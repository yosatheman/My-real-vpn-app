package com.yourname.netforge.domain.model

data class Payload(
    val mode: TunnelMode = TunnelMode.SSH,
    val sni: String = "",
    val rawPayload: String = "GET / HTTP/1.1[crlf]Host: [host][crlf][crlf]",
    val sshUser: String = "",
    val sshPass: String = "",
    val proxyHost: String = "",
    val proxyPort: Int = 8080
)
