package com.focuspro.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.focuspro.app.FocusProApplication
import com.focuspro.app.MainActivity
import com.focuspro.app.R
import com.focuspro.app.data.local.entity.WebsiteVisitEntity
import com.focuspro.app.util.DnsPacketParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class LocalVpnBlockerService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        startForeground(NOTIFICATION_ID, buildNotification())

        val builder = Builder()
            .setSession("FOCUS PRO Local DNS Blocker")
            .addAddress("10.0.0.2", 32)
            .addRoute("10.0.0.1", 32)
            .addDnsServer("10.0.0.1")
            .setMtu(1500)

        vpnInterface = builder.establish()
        val pfd = vpnInterface ?: run {
            stopSelf()
            return
        }

        val app = applicationContext as? FocusProApplication
        serviceScope.launch {
            app?.preferencesManager?.setVpnBlockerActive(true)
        }

        vpnJob = serviceScope.launch {
            val input = FileInputStream(pfd.fileDescriptor)
            val output = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteArray(32767)
            val upstreamAddress = InetAddress.getByName("8.8.8.8")

            while (vpnInterface != null) {
                val length = runCatching { input.read(buffer) }.getOrDefault(-1)
                if (length <= 0) continue

                val parsed = DnsPacketParser.parseIpv4UdpDnsQuery(buffer, length) ?: continue
                val domain = parsed.domain
                val rules = app?.database?.websiteDao()?.getRulesSync().orEmpty()
                val now = System.currentTimeMillis()

                val isAllowed = rules.any {
                    it.isAllowListed && (domain == it.domain || domain.endsWith(".${it.domain}"))
                }
                val isBlocked = !isAllowed && rules.any {
                    it.isBlocked &&
                        (it.blockUntilMillis == 0L || it.blockUntilMillis > now) &&
                        (domain == it.domain || domain.endsWith(".${it.domain}"))
                }

                app?.database?.websiteDao()?.insertVisit(
                    WebsiteVisitEntity(
                        domain = domain,
                        wasBlocked = isBlocked,
                        timestampMillis = now
                    )
                )

                if (isBlocked) {
                    val nxResponse = DnsPacketParser.buildNxDomainResponse(buffer, parsed)
                    output.write(nxResponse)
                } else {
                    forwardDnsToUpstream(buffer, parsed, upstreamAddress, output)
                }
            }
        }
    }

    private fun forwardDnsToUpstream(
        requestPacket: ByteArray,
        parsed: com.focuspro.app.util.ParsedDnsPacket,
        upstreamAddress: InetAddress,
        tunOutput: FileOutputStream
    ) {
        runCatching {
            DatagramSocket().use { socket ->
                protect(socket)
                socket.soTimeout = 2500
                val dnsPayload = ByteArray(parsed.dnsPayloadLength)
                System.arraycopy(
                    requestPacket,
                    parsed.dnsPayloadOffset,
                    dnsPayload,
                    0,
                    parsed.dnsPayloadLength
                )
                val outPacket = DatagramPacket(dnsPayload, dnsPayload.size, upstreamAddress, 53)
                socket.send(outPacket)

                val respBuf = ByteArray(4096)
                val inPacket = DatagramPacket(respBuf, respBuf.size)
                socket.receive(inPacket)

                val wrapped = DnsPacketParser.buildUdpResponseFromUpstream(
                    requestPacket,
                    parsed,
                    respBuf,
                    inPacket.length
                )
                tunOutput.write(wrapped)
            }
        }
    }

    private fun stopVpn() {
        vpnJob?.cancel()
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        val app = applicationContext as? FocusProApplication
        serviceScope.launch {
            app?.preferencesManager?.setVpnBlockerActive(false)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, FocusProApplication.CHANNEL_VPN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FOCUS PRO Website Blocker")
            .setContentText("Local DNS filtering active • HTTPS traffic is never decrypted")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_STOP = "com.focuspro.app.ACTION_STOP_VPN"
        private const val NOTIFICATION_ID = 4102
    }
}
