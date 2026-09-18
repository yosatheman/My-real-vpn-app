package com.yourname.netforge.domain.tunnel

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.yourname.netforge.domain.model.Config
import java.net.Socket
import java.util.concurrent.atomic.AtomicLong

class SshTunnel(
    private val config: Config,
    private val socketProtector: ((Socket) -> Boolean)?,
    private val logger: (String, String) -> Unit,
    private val bytesIn: AtomicLong,
    private val bytesOut: AtomicLong
) : AutoCloseable {

    private var session: Session? = null

    fun connect() {
        val host = config.host
        val port = config.port
        val username = config.payload.sshUser.ifBlank { "root" }
        val password = config.payload.sshPass

        logger("INFO", "SSH: Connecting to $username@$host:$port")
        val jsch = JSch()
        val sess = jsch.getSession(username, host, port)
        sess.setPassword(password)

        val properties = java.util.Properties()
        properties["StrictHostKeyChecking"] = "no"
        properties["PreferredAuthentications"] = "password,keyboard-interactive"
        sess.setConfig(properties)
        sess.timeout = 15000

        // In JSch, connect establishes the SSH handshake
        sess.connect(15000)
        session = sess

        logger("SUCCESS", "SSH: Authentication successful, session active")
        bytesOut.addAndGet(256)
        bytesIn.addAndGet(512)
    }

    override fun close() {
        try {
            session?.disconnect()
            session = null
            logger("INFO", "SSH: Session disconnected")
        } catch (_: Exception) {}
    }
}
