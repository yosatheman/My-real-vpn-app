package com.yourname.netforge.domain.model

data class DnsConfig(
    val primary: String = "1.1.1.1",
    val secondary: String = "8.8.8.8",
    val customEnabled: Boolean = false
)
