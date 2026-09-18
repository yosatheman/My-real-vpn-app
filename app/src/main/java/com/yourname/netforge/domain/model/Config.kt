package com.yourname.netforge.domain.model

data class Config(
    val id: Long = 0L,
    val name: String = "Untitled Config",
    val version: Int = 1,
    val host: String = "1.2.3.4",
    val port: Int = 443,
    val payload: Payload = Payload(),
    val dns: DnsConfig = DnsConfig(),
    val keepalive: Int = 60,
    val mtu: Int = 1500,
    val udp: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
) {
    fun toIniString(): String {
        return buildString {
            appendLine("# NetForge config v$version")
            appendLine("[meta]")
            appendLine("name = $name")
            appendLine("version = $version")
            appendLine()
            appendLine("[server]")
            appendLine("host = $host")
            appendLine("port = $port")
            appendLine()
            appendLine("[payload]")
            appendLine("mode = ${payload.mode.id}")
            if (payload.sni.isNotBlank()) appendLine("sni = ${payload.sni}")
            appendLine("payload = ${payload.rawPayload}")
            if (payload.sshUser.isNotBlank()) appendLine("ssh_user = ${payload.sshUser}")
            if (payload.sshPass.isNotBlank()) appendLine("ssh_pass = ${payload.sshPass}")
            if (payload.proxyHost.isNotBlank()) appendLine("proxy_host = ${payload.proxyHost}")
            if (payload.proxyPort != 8080) appendLine("proxy_port = ${payload.proxyPort}")
            appendLine()
            appendLine("[dns]")
            appendLine("primary = ${dns.primary}")
            appendLine("secondary = ${dns.secondary}")
            appendLine()
            appendLine("[advanced]")
            appendLine("keepalive = $keepalive")
            appendLine("mtu = $mtu")
            appendLine("udp = $udp")
        }
    }

    companion object {
        fun fromIniString(ini: String): Config {
            var name = "Imported Config"
            var version = 1
            var host = "127.0.0.1"
            var port = 443
            var mode = TunnelMode.SSH
            var sni = ""
            var rawPayload = "GET / HTTP/1.1[crlf]Host: [host][crlf][crlf]"
            var sshUser = ""
            var sshPass = ""
            var proxyHost = ""
            var proxyPort = 8080
            var dnsPrimary = "1.1.1.1"
            var dnsSecondary = "8.8.8.8"
            var keepalive = 60
            var mtu = 1500
            var udp = false

            var currentSection = ""
            ini.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("#") || trimmed.startsWith(";") || trimmed.isEmpty()) {
                    return@forEach
                }
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentSection = trimmed.substring(1, trimmed.length - 1).trim().lowercase()
                    return@forEach
                }
                val eqIdx = trimmed.indexOf('=')
                if (eqIdx != -1) {
                    val key = trimmed.substring(0, eqIdx).trim().lowercase()
                    val value = trimmed.substring(eqIdx + 1).trim()
                    when (currentSection) {
                        "meta" -> when (key) {
                            "name" -> name = value
                            "version" -> version = value.toIntOrNull() ?: 1
                        }
                        "server" -> when (key) {
                            "host" -> host = value
                            "port" -> port = value.toIntOrNull() ?: 443
                        }
                        "payload" -> when (key) {
                            "mode" -> mode = TunnelMode.fromString(value)
                            "sni" -> sni = value
                            "payload" -> rawPayload = value
                            "ssh_user" -> sshUser = value
                            "ssh_pass" -> sshPass = value
                            "proxy_host" -> proxyHost = value
                            "proxy_port" -> proxyPort = value.toIntOrNull() ?: 8080
                        }
                        "dns" -> when (key) {
                            "primary" -> dnsPrimary = value
                            "secondary" -> dnsSecondary = value
                        }
                        "advanced" -> when (key) {
                            "keepalive" -> keepalive = value.toIntOrNull() ?: 60
                            "mtu" -> mtu = value.toIntOrNull() ?: 1500
                            "udp" -> udp = value.toBoolean()
                        }
                    }
                }
            }
            return Config(
                name = name,
                version = version,
                host = host,
                port = port,
                payload = Payload(
                    mode = mode,
                    sni = sni,
                    rawPayload = rawPayload,
                    sshUser = sshUser,
                    sshPass = sshPass,
                    proxyHost = proxyHost,
                    proxyPort = proxyPort
                ),
                dns = DnsConfig(primary = dnsPrimary, secondary = dnsSecondary, customEnabled = true),
                keepalive = keepalive,
                mtu = mtu,
                udp = udp
            )
        }
    }
}
