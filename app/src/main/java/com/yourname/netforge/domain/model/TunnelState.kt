package com.yourname.netforge.domain.model

enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING
}

data class TunnelState(
    val status: TunnelStatus = TunnelStatus.DISCONNECTED,
    val activeConfigName: String = "",
    val activeNode: String = "",
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val pingMs: Long = -1L,
    val connectedSince: Long = 0L,
    val statusMessage: String = "Ready to connect"
)
