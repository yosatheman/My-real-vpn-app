package com.yourname.netforge.domain.tunnel

import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.domain.payload.PayloadEngine
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.*

class SniTunnel(
    private val config: Config,
    private val socketProtector: ((Socket) -> Boolean)?,
    private val logger: (String, String) -> Unit,
    private val bytesIn: AtomicLong,
    private val bytesOut: AtomicLong
) : AutoCloseable {

    private var sslSocket: SSLSocket? = null
    private val running = AtomicBoolean(false)

    fun connect() {
        val host = config.host
        val port = config.port
        val sniHost = config.payload.sni.ifBlank { host }

        logger("INFO", "SNI Tunnel: Connecting to $host:$port (SNI: $sniHost)")

        val rawSocket = Socket()
        socketProtector?.invoke(rawSocket)
        rawSocket.connect(InetSocketAddress(host, port), 10000)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }), null)

        val sslFactory = sslContext.socketFactory
        val ssl = sslFactory.createSocket(rawSocket, host, port, true) as SSLSocket

        val sslParams = ssl.sslParameters
        try {
            val sniServerName = SNIHostName(sniHost)
            sslParams.serverNames = listOf(sniServerName)
        } catch (_: Exception) {
            logger("WARN", "SNI: Could not configure SNIHostName for $sniHost")
        }
        ssl.sslParameters = sslParams
        ssl.startHandshake()
        sslSocket = ssl

        logger("SUCCESS", "SNI: TLS handshake complete with SNI $sniHost")

        if (config.payload.rawPayload.isNotBlank()) {
            val payload = PayloadEngine.process(config.payload.rawPayload, config, isTls = true)
            val out = ssl.outputStream
            out.write(payload)
            out.flush()
            bytesOut.addAndGet(payload.size.toLong())
            logger("INFO", "SNI: Injected payload (${payload.size} bytes) sent over TLS")
        }

        running.set(true)
    }

    override fun close() {
        running.set(false)
        try {
            sslSocket?.close()
            sslSocket = null
            logger("INFO", "SNI: SSL socket closed")
        } catch (_: Exception) {}
    }
}
