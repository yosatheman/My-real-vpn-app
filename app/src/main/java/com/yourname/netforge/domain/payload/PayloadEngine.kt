package com.yourname.netforge.domain.payload

import com.yourname.netforge.crypto.AesGcm
import com.yourname.netforge.domain.model.Config
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger

object PayloadEngine {

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0",
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:125.0) Gecko/20100101 Firefox/125.0",
        "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.118 Mobile Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
        "Mozilla/5.0 (Linux; Android 14; SM-A546B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    )

    private val uaIndex = AtomicInteger(0)
    private val secureRandom = SecureRandom()
    private const val ALNUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    fun generateRandomAlnum(length: Int = 16): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(ALNUM[secureRandom.nextInt(ALNUM.length)])
        }
        return sb.toString()
    }

    private fun nextUserAgent(): String {
        val idx = Math.abs(uaIndex.getAndIncrement()) % userAgents.size
        return userAgents[idx]
    }

    /**
     * Replaces placeholders in order of longest placeholder tag first.
     */
    fun process(
        template: String,
        config: Config,
        isTls: Boolean = true,
        frontHostOverride: String? = null
    ): ByteArray {
        val host = config.host
        val port = config.port.toString()
        val frontHost = frontHostOverride?.ifBlank { null }
            ?: config.payload.sni.ifBlank { null }
            ?: host
        val hostPort = "$host:$port"
        val realHost = host
        val sshUser = config.payload.sshUser
        val sshPass = config.payload.sshPass
        val protocol = if (isTls) "https" else "http"
        val randomStr = generateRandomAlnum(16)
        val ua = nextUserAgent()

        // Longest first replacement
        val replacements = listOf(
            "[front_host]" to frontHost,
            "[host_port]" to hostPort,
            "[real_host]" to realHost,
            "[protocol]" to protocol,
            "[ssh_user]" to sshUser,
            "[ssh_pass]" to sshPass,
            "[random]" to randomStr,
            "[crlf]" to "\r\n",
            "[host]" to host,
            "[port]" to port,
            "[ua]" to ua,
            "[cr]" to "\r",
            "[lf]" to "\n"
        )

        var result = template
        for ((tag, value) in replacements) {
            result = result.replace(tag, value)
        }

        return result.toByteArray(Charsets.UTF_8)
    }

    /**
     * Preview processing without consuming secrets/state permanently.
     */
    fun preview(template: String, config: Config): String {
        val bytes = process(template, config, isTls = true)
        return String(bytes, Charsets.UTF_8)
    }
}
