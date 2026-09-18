package com.yourname.netforge.domain.tunnel

import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.domain.payload.PayloadEngine
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class HttpConnectTunnel(
    private val config: Config,
    private val socketProtector: ((Socket) -> Boolean)?,
    private val logger: (String, String) -> Unit,
    private val bytesIn: AtomicLong,
    private val bytesOut: AtomicLong
) : AutoCloseable {

    private var socket: Socket? = null
    private val running = AtomicBoolean(false)

    fun connect() {
        val targetHost = config.payload.proxyHost.ifBlank { config.host }
        val targetPort = if (config.payload.proxyHost.isNotBlank()) config.payload.proxyPort else config.port

        logger("INFO", "HTTP CONNECT: Opening socket to $targetHost:$targetPort")
        val s = Socket()
        socketProtector?.invoke(s)
        s.tcpNoDelay = true
        s.soTimeout = 15000
        s.connect(InetSocketAddress(targetHost, targetPort), 10000)
        socket = s

        val out: OutputStream = s.getOutputStream()
        val inStream: InputStream = s.getInputStream()

        // Generate injected HTTP payload
        val payloadBytes = if (config.payload.rawPayload.isNotBlank()) {
            PayloadEngine.process(config.payload.rawPayload, config, isTls = false)
        } else {
            ("CONNECT ${config.host}:${config.port} HTTP/1.1\r\n" +
             "Host: ${config.host}:${config.port}\r\n" +
             "Proxy-Connection: Keep-Alive\r\n\r\n").toByteArray(Charsets.UTF_8)
        }

        logger("INFO", "HTTP CONNECT: Sending payload (${payloadBytes.size} bytes)")
        out.write(payloadBytes)
        out.flush()
        bytesOut.addAndGet(payloadBytes.size.toLong())

        // Read HTTP status line
        val responseBuffer = ByteArray(1024)
        val read = inStream.read(responseBuffer)
        if (read <= 0) {
            throw IllegalStateException("Empty response from proxy server")
        }
        bytesIn.addAndGet(read.toLong())
        val responseStr = String(responseBuffer, 0, read, Charsets.UTF_8)
        val statusLine = responseStr.lines().firstOrNull() ?: ""
        logger("INFO", "HTTP CONNECT: Received << $statusLine")

        if (!statusLine.contains("200")) {
            logger("WARN", "HTTP CONNECT: Unexpected status response: $statusLine")
        } else {
            logger("SUCCESS", "HTTP CONNECT: Proxy chain established")
        }

        s.soTimeout = 0
        running.set(true)
    }

    override fun close() {
        running.set(false)
        try {
            socket?.close()
            socket = null
            logger("INFO", "HTTP CONNECT: Socket closed")
        } catch (_: Exception) {}
    }
}
