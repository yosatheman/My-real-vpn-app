package com.yourname.netforge.domain

import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.domain.model.Payload
import com.yourname.netforge.domain.model.TunnelMode
import com.yourname.netforge.domain.payload.PayloadEngine
import org.junit.Assert.*
import org.junit.Test

class PayloadEngineTest {

    @Test
    fun testLongestFirstRulePreservesHostPort() {
        val config = Config(
            host = "myserver.org",
            port = 8443,
            payload = Payload(
                mode = TunnelMode.HTTP_CONNECT,
                rawPayload = "CONNECT [host_port] HTTP/1.1[crlf]Host: [host]:[port][crlf][crlf]"
            )
        )

        val processed = String(PayloadEngine.process(config.payload.rawPayload, config), Charsets.UTF_8)
        assertEquals("CONNECT myserver.org:8443 HTTP/1.1\r\nHost: myserver.org:8443\r\n\r\n", processed)
    }

    @Test
    fun testPlaceholderSubstitution() {
        val config = Config(
            host = "api.backend.net",
            port = 443,
            payload = Payload(
                mode = TunnelMode.SNI,
                sni = "fronting.cdn.com",
                sshUser = "admin",
                sshPass = "secret123",
                rawPayload = "GET / HTTP/1.1[crlf]Host: [front_host][crlf]Real: [real_host][crlf]Auth: [ssh_user]:[ssh_pass][crlf][crlf]"
            )
        )

        val processed = String(PayloadEngine.process(config.payload.rawPayload, config, isTls = true), Charsets.UTF_8)
        assertTrue(processed.contains("Host: fronting.cdn.com\r\n"))
        assertTrue(processed.contains("Real: api.backend.net\r\n"))
        assertTrue(processed.contains("Auth: admin:secret123\r\n"))
    }

    @Test
    fun testUserAgentInjection() {
        val config = Config(
            host = "edge.com",
            port = 80,
            payload = Payload(
                mode = TunnelMode.DIRECT,
                rawPayload = "GET / HTTP/1.1[crlf]User-Agent: [ua][crlf][crlf]"
            )
        )

        val processed = String(PayloadEngine.process(config.payload.rawPayload, config), Charsets.UTF_8)
        assertFalse(processed.contains("[ua]"))
        assertTrue(processed.contains("User-Agent: Mozilla/5.0"))
    }

    @Test
    fun testRandomHexReplacement() {
        val config = Config(
            host = "example.com",
            port = 443,
            payload = Payload(
                rawPayload = "Token: [random][crlf]"
            )
        )

        val processed1 = String(PayloadEngine.process(config.payload.rawPayload, config), Charsets.UTF_8)
        val processed2 = String(PayloadEngine.process(config.payload.rawPayload, config), Charsets.UTF_8)

        assertFalse(processed1.contains("[random]"))
        assertFalse(processed2.contains("[random]"))
        // Consecutive calls should generate random strings
        assertNotEquals(processed1, processed2)
    }
}
