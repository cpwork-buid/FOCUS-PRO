package com.focuspro.app.util

import java.nio.ByteBuffer

data class ParsedDnsPacket(
    val domain: String,
    val ipHeaderLen: Int,
    val udpHeaderOffset: Int,
    val dnsPayloadOffset: Int,
    val dnsPayloadLength: Int,
    val transactionId: Short
)

object DnsPacketParser {

    /**
     * Parses an IPv4 UDP DNS query packet from TUN interface.
     * Returns null if packet is not IPv4, not UDP, or not destination port 53.
     */
    fun parseIpv4UdpDnsQuery(packet: ByteArray, length: Int): ParsedDnsPacket? {
        if (length < 28) return null
        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 4) return null

        val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
        if (ipHeaderLen < 20 || length < ipHeaderLen + 8) return null

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null // 17 = UDP

        val udpOffset = ipHeaderLen
        val destPort = ((packet[udpOffset + 2].toInt() and 0xFF) shl 8) or
            (packet[udpOffset + 3].toInt() and 0xFF)
        if (destPort != 53) return null

        val dnsOffset = udpOffset + 8
        val dnsLen = length - dnsOffset
        if (dnsLen < 12) return null

        val transactionId = (((packet[dnsOffset].toInt() and 0xFF) shl 8) or
            (packet[dnsOffset + 1].toInt() and 0xFF)).toShort()

        val domain = extractDomainName(packet, dnsOffset + 12, length) ?: return null
        return ParsedDnsPacket(
            domain = domain.lowercase(),
            ipHeaderLen = ipHeaderLen,
            udpHeaderOffset = udpOffset,
            dnsPayloadOffset = dnsOffset,
            dnsPayloadLength = dnsLen,
            transactionId = transactionId
        )
    }

    fun extractDomainName(packet: ByteArray, startOffset: Int, maxLength: Int): String? {
        var pos = startOffset
        val parts = mutableListOf<String>()
        while (pos < maxLength) {
            val labelLen = packet[pos].toInt() and 0xFF
            if (labelLen == 0) break
            if ((labelLen and 0xC0) == 0xC0) break // compression pointer
            pos++
            if (pos + labelLen > maxLength) return null
            parts.add(String(packet, pos, labelLen, Charsets.US_ASCII))
            pos += labelLen
        }
        return if (parts.isEmpty()) null else parts.joinToString(".")
    }

    /**
     * Builds an IPv4+UDP DNS NXDOMAIN response packet so blocked domains fail fast locally
     * without decrypting HTTPS or sending packets externally.
     */
    fun buildNxDomainResponse(requestPacket: ByteArray, parsed: ParsedDnsPacket): ByteArray {
        val dnsOffset = parsed.dnsPayloadOffset
        val dnsLen = parsed.dnsPayloadLength
        val totalLen = parsed.ipHeaderLen + 8 + dnsLen
        val resp = ByteArray(totalLen)

        // Copy IP + UDP + DNS request as template
        System.arraycopy(requestPacket, 0, resp, 0, totalLen)

        // Swap source and destination IPv4 addresses (bytes 12..15 <-> 16..19)
        for (i in 0 until 4) {
            val tmp = resp[12 + i]
            resp[12 + i] = resp[16 + i]
            resp[16 + i] = tmp
        }

        // Swap UDP source and destination ports
        val udp = parsed.udpHeaderOffset
        val srcPort0 = resp[udp]
        val srcPort1 = resp[udp + 1]
        resp[udp] = resp[udp + 2]
        resp[udp + 1] = resp[udp + 3]
        resp[udp + 2] = srcPort0
        resp[udp + 3] = srcPort1

        // Set DNS flags: QR=1 (response), RCODE=3 (NXDOMAIN)
        resp[dnsOffset + 2] = (resp[dnsOffset + 2].toInt() or 0x80).toByte()
        resp[dnsOffset + 3] = ((resp[dnsOffset + 3].toInt() and 0xF0) or 0x03).toByte()
        // Zero answer/authority/additional counts
        for (i in 6 until 12) {
            resp[dnsOffset + i] = 0
        }

        // Zero UDP checksum (optional in IPv4)
        resp[udp + 6] = 0
        resp[udp + 7] = 0

        // Recompute IPv4 header checksum
        resp[10] = 0
        resp[11] = 0
        var sum = 0
        for (i in 0 until parsed.ipHeaderLen step 2) {
            val word = ((resp[i].toInt() and 0xFF) shl 8) or (resp[i + 1].toInt() and 0xFF)
            sum += word
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        resp[10] = (checksum ushr 8).toByte()
        resp[11] = (checksum and 0xFF).toByte()

        return resp
    }

    /**
     * Wraps a raw DNS response payload back into IPv4+UDP headers matching the original query.
     */
    fun buildUdpResponseFromUpstream(
        requestPacket: ByteArray,
        parsed: ParsedDnsPacket,
        dnsResponsePayload: ByteArray,
        dnsResponseLength: Int
    ): ByteArray {
        val ipLen = parsed.ipHeaderLen
        val totalLen = ipLen + 8 + dnsResponseLength
        val resp = ByteArray(totalLen)

        System.arraycopy(requestPacket, 0, resp, 0, ipLen)
        // Update IP total length
        resp[2] = (totalLen ushr 8).toByte()
        resp[3] = (totalLen and 0xFF).toByte()

        // Swap IPv4 src/dst
        for (i in 0 until 4) {
            val tmp = resp[12 + i]
            resp[12 + i] = resp[16 + i]
            resp[16 + i] = tmp
        }

        // UDP header
        val udp = ipLen
        resp[udp] = requestPacket[parsed.udpHeaderOffset + 2]
        resp[udp + 1] = requestPacket[parsed.udpHeaderOffset + 3]
        resp[udp + 2] = requestPacket[parsed.udpHeaderOffset]
        resp[udp + 3] = requestPacket[parsed.udpHeaderOffset + 1]
        val udpLen = 8 + dnsResponseLength
        resp[udp + 4] = (udpLen ushr 8).toByte()
        resp[udp + 5] = (udpLen and 0xFF).toByte()
        resp[udp + 6] = 0
        resp[udp + 7] = 0

        System.arraycopy(dnsResponsePayload, 0, resp, ipLen + 8, dnsResponseLength)

        // Recompute IPv4 header checksum
        resp[10] = 0
        resp[11] = 0
        var sum = 0
        for (i in 0 until ipLen step 2) {
            val word = ((resp[i].toInt() and 0xFF) shl 8) or (resp[i + 1].toInt() and 0xFF)
            sum += word
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        resp[10] = (checksum ushr 8).toByte()
        resp[11] = (checksum and 0xFF).toByte()

        return resp
    }
}
