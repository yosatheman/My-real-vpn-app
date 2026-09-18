package com.yourname.netforge.domain.tunnel

import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.domain.model.TunnelMode
import com.yourname.netforge.domain.model.TunnelState
import com.yourname.netforge.domain.model.TunnelStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class TunnelEngine(
    private val socketProtector: ((Socket) -> Boolean)? = null
) {
    private val _tunnelState = MutableStateFlow(TunnelState())
    val tunnelState: StateFlow<TunnelState> = _tunnelState.asStateFlow()

    private val isRunning = AtomicBoolean(false)
    private var engineJob: Job? = null
    private var pingJob: Job? = null

    val bytesIn = AtomicLong(0L)
    val bytesOut = AtomicLong(0L)

    private var currentActiveTunnel: AutoCloseable? = null

    interface LogCallback {
        fun log(level: String, message: String)
    }

    var logCallback: LogCallback? = null

    fun log(level: String, message: String) {
        logCallback?.log(level, message)
    }

    fun isConnected(): Boolean {
        return _tunnelState.value.status == TunnelStatus.CONNECTED
    }

    fun start(config: Config, coroutineScope: CoroutineScope) {
        if (isRunning.getAndSet(true)) return

        engineJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                _tunnelState.value = TunnelState(
                    status = TunnelStatus.CONNECTING,
                    activeConfigName = config.name,
                    activeNode = "${config.host}:${config.port}",
                    statusMessage = "Starting ${config.payload.mode.displayName}…"
                )
                log("INFO", "Initializing tunnel for ${config.name}")
                log("INFO", "Target node: ${config.host}:${config.port} via mode [${config.payload.mode.id}]")

                val tunnel: AutoCloseable = when (config.payload.mode) {
                    TunnelMode.SSH -> SshTunnel(config, socketProtector, ::log, bytesIn, bytesOut).also { it.connect() }
                    TunnelMode.HTTP_CONNECT -> HttpConnectTunnel(config, socketProtector, ::log, bytesIn, bytesOut).also { it.connect() }
                    TunnelMode.SNI -> SniTunnel(config, socketProtector, ::log, bytesIn, bytesOut).also { it.connect() }
                    TunnelMode.WEBSOCKET -> WebSocketTunnel(config, socketProtector, ::log, bytesIn, bytesOut).also { it.connect() }
                    TunnelMode.SOCKS5 -> Socks5Chain(config, socketProtector, ::log, bytesIn, bytesOut).also { it.connect() }
                    TunnelMode.DIRECT -> DirectTunnel(config, socketProtector, ::log, bytesIn, bytesOut).also { it.connect() }
                }

                currentActiveTunnel = tunnel

                _tunnelState.value = _tunnelState.value.copy(
                    status = TunnelStatus.CONNECTED,
                    connectedSince = System.currentTimeMillis(),
                    statusMessage = "Connected to ${config.host}:${config.port}"
                )
                log("SUCCESS", "Tunnel established successfully")

                startPingLatencyMonitor(config.host, config.port, coroutineScope)
                startTrafficMonitor(coroutineScope)

            } catch (e: Exception) {
                if (isActive) {
                    log("ERROR", "Tunnel failed: ${e.localizedMessage ?: e.javaClass.simpleName}")
                    stopInternal("Connection failed")
                }
            }
        }
    }

    private fun startPingLatencyMonitor(host: String, port: Int, scope: CoroutineScope) {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive && isRunning.get()) {
                val start = System.currentTimeMillis()
                try {
                    val probeSocket = Socket()
                    socketProtector?.invoke(probeSocket)
                    probeSocket.connect(InetSocketAddress(host, port), 2500)
                    val latency = System.currentTimeMillis() - start
                    probeSocket.close()
                    _tunnelState.value = _tunnelState.value.copy(pingMs = latency)
                } catch (_: Exception) {
                    _tunnelState.value = _tunnelState.value.copy(pingMs = -1L)
                }
                delay(5000)
            }
        }
    }

    private fun startTrafficMonitor(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            while (isActive && isRunning.get()) {
                _tunnelState.value = _tunnelState.value.copy(
                    bytesIn = bytesIn.get(),
                    bytesOut = bytesOut.get()
                )
                delay(1000)
            }
        }
    }

    fun stop() {
        stopInternal("Disconnected by user")
    }

    private fun stopInternal(reason: String) {
        isRunning.set(false)
        pingJob?.cancel()
        pingJob = null
        engineJob?.cancel()
        engineJob = null

        try {
            currentActiveTunnel?.close()
        } catch (_: Exception) {}
        currentActiveTunnel = null

        _tunnelState.value = TunnelState(
            status = TunnelStatus.DISCONNECTED,
            statusMessage = reason
        )
        log("INFO", "Tunnel closed: $reason")
    }
}
