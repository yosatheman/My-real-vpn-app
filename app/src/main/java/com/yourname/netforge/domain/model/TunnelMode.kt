package com.yourname.netforge.domain.model

enum class TunnelMode(val id: String, val displayName: String) {
    SSH("ssh", "SSH Tunnel"),
    HTTP_CONNECT("http_connect", "HTTP CONNECT (Proxy Chain)"),
    SNI("sni", "SNI Fronting (TLS Direct)"),
    WEBSOCKET("websocket", "WebSocket Upgrade"),
    SOCKS5("socks5", "SOCKS5 Proxy Chain"),
    DIRECT("direct", "Direct Raw TCP");

    companion object {
        fun fromString(value: String): TunnelMode {
            return entries.firstOrNull {
                it.id.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
            } ?: SSH
        }
    }
}
