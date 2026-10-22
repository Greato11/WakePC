package com.example.wakepc

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress

/** Builds and sends a Wake-on-LAN "magic packet" (6 x 0xFF followed by the MAC repeated 16 times). */
object WolSender {

    fun parseMac(text: String): ByteArray? {
        val hex = text.replace(Regex("[^0-9A-Fa-f]"), "")
        if (hex.length != 12) return null
        return ByteArray(6) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    fun formatMac(mac: ByteArray): String =
        mac.joinToString(":") { "%02X".format(it.toInt() and 0xFF) }

    fun buildPacket(mac: ByteArray): ByteArray {
        val packet = ByteArray(6 + 16 * 6)
        for (i in 0 until 6) packet[i] = 0xFF.toByte()
        for (i in 1..16) System.arraycopy(mac, 0, packet, i * 6, 6)
        return packet
    }

    /** Blocking. Call from a background thread. */
    fun send(context: Context, device: Device): Result<Unit> = runCatching {
        val mac = parseMac(device.mac) ?: error("Invalid MAC address")
        val packet = buildPacket(mac)

        val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
        val network: Network? = cm?.activeNetwork

        val targets = LinkedHashSet<InetAddress>()
        if (device.address.isNotBlank()) {
            targets += InetAddress.getByName(device.address.trim())
        } else {
            targets += InetAddress.getByName("255.255.255.255")
            subnetBroadcast(cm, network)?.let { targets += it }
        }

        var sent = 0
        var lastError: Exception? = null
        DatagramSocket().use { socket ->
            socket.broadcast = true
            try {
                network?.bindSocket(socket)
            } catch (_: Exception) {
                // fall back to the default route
            }
            for (target in targets) {
                try {
                    socket.send(DatagramPacket(packet, packet.size, target, device.port))
                    sent++
                } catch (e: Exception) {
                    lastError = e
                }
            }
        }
        if (sent == 0) throw lastError ?: IOException("No network")
    }

    /** e.g. 192.168.1.37/24 -> 192.168.1.255 (more reliable than 255.255.255.255 on many routers). */
    private fun subnetBroadcast(cm: ConnectivityManager?, network: Network?): InetAddress? {
        if (cm == null || network == null) return null
        val props = cm.getLinkProperties(network) ?: return null
        for (la in props.linkAddresses) {
            val addr = la.address
            if (addr is Inet4Address && !addr.isLoopbackAddress) {
                val b = addr.address
                val ip = ((b[0].toInt() and 0xFF) shl 24) or
                    ((b[1].toInt() and 0xFF) shl 16) or
                    ((b[2].toInt() and 0xFF) shl 8) or
                    (b[3].toInt() and 0xFF)
                val prefix = la.prefixLength
                val mask = if (prefix <= 0) 0 else -1 shl (32 - prefix)
                val bc = ip or mask.inv()
                return InetAddress.getByAddress(
                    byteArrayOf(
                        (bc ushr 24).toByte(), (bc ushr 16).toByte(),
                        (bc ushr 8).toByte(), bc.toByte(),
                    )
                )
            }
        }
        return null
    }
}
