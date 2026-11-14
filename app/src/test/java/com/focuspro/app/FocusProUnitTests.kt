package com.focuspro.app

import com.focuspro.app.util.DnsPacketParser
import com.focuspro.app.util.ProductivityCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusProUnitTests {

    @Test
    fun productivityScore_maxValues_returns100() {
        val result = ProductivityCalculator.calculate(
            focusMinutes = 180,
            dailyTargetMinutes = 180,
            completedTasks = 5,
            totalTasks = 5,
            blockedAttempts = 0
        )
        assertEquals(100, result.totalScore)
        assertEquals(45, result.focusPoints)
        assertEquals(35, result.taskPoints)
        assertEquals(20, result.disciplinePoints)
    }

    @Test
    fun productivityScore_penalizesBlockedAttempts_clampedAtZero() {
        val result = ProductivityCalculator.calculate(
            focusMinutes = 0,
            dailyTargetMinutes = 120,
            completedTasks = 0,
            totalTasks = 4,
            blockedAttempts = 15
        )
        assertEquals(0, result.disciplinePoints)
        assertEquals(0, result.totalScore)
    }

    @Test
    fun dnsPacketParser_parsesValidIpv4UdpQuery() {
        // Minimal IPv4 (20 bytes) + UDP (8 bytes, dst port 53) + DNS header (12 bytes) + "youtube.com"
        val domainLabels = byteArrayOf(
            7, 'y'.code.toByte(), 'o'.code.toByte(), 'u'.code.toByte(),
            't'.code.toByte(), 'u'.code.toByte(), 'b'.code.toByte(), 'e'.code.toByte(),
            3, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            0
        )
        val packet = ByteArray(20 + 8 + 12 + domainLabels.size)
        packet[0] = 0x45 // IPv4, IHL=5 (20 bytes)
        packet[9] = 17   // UDP
        packet[22] = 0x00
        packet[23] = 53  // Dest port 53
        packet[28] = 0x12
        packet[29] = 0x34 // Transaction ID
        System.arraycopy(domainLabels, 0, packet, 40, domainLabels.size)

        val parsed = DnsPacketParser.parseIpv4UdpDnsQuery(packet, packet.size)
        assertNotNull(parsed)
        assertEquals("youtube.com", parsed?.domain)

        val nx = DnsPacketParser.buildNxDomainResponse(packet, parsed!!)
        assertTrue((nx[parsed.dnsPayloadOffset + 2].toInt() and 0x80) != 0) // QR=1
        assertEquals(3, nx[parsed.dnsPayloadOffset + 3].toInt() and 0x0F)   // RCODE=3 (NXDOMAIN)
    }

    @Test
    fun dnsPacketParser_ignoresNonUdpPacket() {
        val packet = ByteArray(40)
        packet[0] = 0x45
        packet[9] = 6 // TCP
        assertNull(DnsPacketParser.parseIpv4UdpDnsQuery(packet, packet.size))
    }
}
