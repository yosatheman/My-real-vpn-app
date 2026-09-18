package com.yourname.netforge.domain.tunnel

import android.util.Base64
import com.yourname.netforge.crypto.AesGcm
import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.domain.payload.PayloadEngine
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

class WebSocketTunnel(
    private val config: Config,
    private val socketProtector: ((Socket) -> Boolean)?,
    private val logger: (String, String) -> Unit,
    private val bytesIn: AtomicLong,
    private val bytesOut: AtomicLong
) : AutoCloseable {

    private var socket: Socket? = null
    private val running = AtomicBoolean(false)

    fun connect() {
        val host = config.host
        val port = config.port
        val sniHost = config.payload.sni.ifBlank { host }
        val isTls = port == 443 || config.payload.sni.isNotBlank()

        logger("INFO", "WebSocket: Connecting to $host:$port (TLS: $isTls)")

        val rawSocket = Socket()
        socketProtector?.invoke(rawSocket)
        rawSocket.connect(InetSocketAddress(host, port), 10000)

        val activeSocket: Socket = if (isTls) {
            val sslContext = SSLContext.getInstance("TLS").apply { init(null, null, null) }
            val ssl = sslContext.socketFactory.createSocket(rawSocket, host, port, true) as SSLSocket
            try {
                val params = ssl.sslParameters
                params.serverNames = listOf(SNIHostName(sniHost))
                ssl.sslParameters = params
            } catch (_: Exception) {}
            ssl.startHandshake()
            ssl
        } else {
            rawSocket
        }
        socket = activeSocket

        val out: OutputStream = activeSocket.getOutputStream()
        val inStream: InputStream = activeSocket.getInputStream()

        // Sec-WebSocket-Key: 16 random bytes base64 encoded
        val nonce = AesGcm.generateRandomBytes(16)
        val wsKey = Base64.encodeToString(nonce, Base64.NO_WRAP)

        val requestHeader = buildString {
            appendLine("GET / HTTP/1.1")
            appendLine("Host: $sniHost")
            appendLine("Upgrade: websocket")
            appendLine("Connection: Upgrade")
            appendLine("Sec-WebSocket-Key: $wsKey")
            appendLine("Sec-WebSocket-Version: 13")
            if (config.payload.rawPayload.isNotBlank()) {
                // If custom payload contains extra headers
                val custom = String(PayloadEngine.process(config.payload.rawPayload, config, isTls), Charsets.UTF_8)
                if (!custom.startsWith("GET /")) {
                    appendLine(custom)
                }
            }
            appendLine()
        }.toByteArray(Charsets.UTF_8)

        logger("INFO", "WebSocket: Transmitting Upgrade handshake (${requestHeader.size} bytes)")
        out.write(requestHeader)
        out.flush()
        bytesOut.addAndGet(requestHeader.size.toLong())

        val buffer = ByteArray(2048)
        val read = inStream.read(buffer)
        if (read <= 0) throw IllegalStateException("WebSocket: Remote server closed handshake stream")
        bytesIn.addAndGet(read.toLong())

        val response = String(buffer, 0, read, Charsets.UTF_8)
        val statusLine = response.lines().firstOrNull() ?: ""
        logger("INFO", "WebSocket: Received << $statusLine")

        if (statusLine.contains("101")) {
            logger("SUCCESS", "WebSocket: 101 Switching Protocols verified. Channel active.")
        } else {
            logger("WARN", "WebSocket: Received status: $statusLine")
        }

        running.set(true)
    }

    override fun close() {
        running.set(false)
        try {
            socket?.close()
            socket = null
            logger("INFO", "WebSocket: Tunnel closed")
        } catch (_: Exception) {}
    }
}
