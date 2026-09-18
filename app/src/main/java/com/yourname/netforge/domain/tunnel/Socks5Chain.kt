package com.yourname.netforge.domain.tunnel

import com.yourname.netforge.domain.model.Config
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class Socks5Chain(
    private val config: Config,
    private val socketProtector: ((Socket) -> Boolean)?,
    private val logger: (String, String) -> Unit,
    private val bytesIn: AtomicLong,
    private val bytesOut: AtomicLong
) : AutoCloseable {

    private var socket: Socket? = null
    private val running = AtomicBoolean(false)

    fun connect() {
        val proxyHost = config.payload.proxyHost.ifBlank { config.host }
        val proxyPort = if (config.payload.proxyHost.isNotBlank()) config.payload.proxyPort else 1080
        val destHost = config.host
        val destPort = config.port

        logger("INFO", "SOCKS5: Connecting to proxy $proxyHost:$proxyPort")
        val s = Socket()
        socketProtector?.invoke(s)
        s.connect(InetSocketAddress(proxyHost, proxyPort), 10000)
        socket = s

        val out: OutputStream = s.getOutputStream()
        val inStream: InputStream = s.getInputStream()

        // 1. Initial greeting: Version 5, 1 method: 0x00 (No Auth)
        val greeting = byteArrayOf(0x05, 0x01, 0x00)
        out.write(greeting)
        out.flush()
        bytesOut.addAndGet(3)

        val greetingResp = ByteArray(2)
        val r1 = inStream.read(greetingResp)
        if (r1 < 2 || greetingResp[0] != 0x05.toByte()) {
            throw IllegalStateException("SOCKS5: Invalid proxy response version")
        }
        bytesIn.addAndGet(r1.toLong())

        // 2. Send CONNECT command
        val hostBytes = destHost.toByteArray(Charsets.US_ASCII)
        val cmd = ByteArray(4 + 1 + hostBytes.size + 2)
        cmd[0] = 0x05 // Ver
        cmd[1] = 0x01 // CMD CONNECT
        cmd[2] = 0x00 // RSV
        cmd[3] = 0x03 // ATYP: Domain name
        cmd[4] = hostBytes.size.toByte()
        System.arraycopy(hostBytes, 0, cmd, 5, hostBytes.size)
        cmd[cmd.size - 2] = (destPort shr 8).toByte()
        cmd[cmd.size - 1] = (destPort and 0xFF).toByte()

        logger("INFO", "SOCKS5: Requesting CONNECT to $destHost:$destPort")
        out.write(cmd)
        out.flush()
        bytesOut.addAndGet(cmd.size.toLong())

        val cmdResp = ByteArray(10)
        val r2 = inStream.read(cmdResp)
        if (r2 < 4 || cmdResp[1] != 0x00.toByte()) {
            val errCode = if (r2 >= 2) cmdResp[1].toInt() else -1
            throw IllegalStateException("SOCKS5: Connection failed with code $errCode")
        }
        bytesIn.addAndGet(r2.toLong())

        logger("SUCCESS", "SOCKS5: Proxy chain established to $destHost:$destPort")
        running.set(true)
    }

    override fun close() {
        running.set(false)
        try {
            socket?.close()
            socket = null
            logger("INFO", "SOCKS5: Socket closed")
        } catch (_: Exception) {}
    }
}
