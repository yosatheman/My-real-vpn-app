package com.yourname.netforge.domain.tunnel

import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.domain.payload.PayloadEngine
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class DirectTunnel(
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

        logger("INFO", "Direct TCP: Opening socket to $host:$port")
        val s = Socket()
        socketProtector?.invoke(s)
        s.tcpNoDelay = true
        s.connect(InetSocketAddress(host, port), 10000)
        socket = s

        if (config.payload.rawPayload.isNotBlank()) {
            val payload = PayloadEngine.process(config.payload.rawPayload, config, isTls = false)
            val out: OutputStream = s.getOutputStream()
            out.write(payload)
            out.flush()
            bytesOut.addAndGet(payload.size.toLong())
            logger("INFO", "Direct TCP: Transmitted ${payload.size} payload bytes")
        }

        logger("SUCCESS", "Direct TCP: Socket stream active")
        running.set(true)
    }

    override fun close() {
        running.set(false)
        try {
            socket?.close()
            socket = null
            logger("INFO", "Direct TCP: Socket closed")
        } catch (_: Exception) {}
    }
}
