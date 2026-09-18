package com.yourname.netforge.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.yourname.netforge.data.db.NetForgeDatabase
import com.yourname.netforge.data.repo.ConfigRepository
import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.domain.model.TunnelState
import com.yourname.netforge.domain.model.TunnelStatus
import com.yourname.netforge.domain.tunnel.TunnelEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class NetForgeVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelEngine: TunnelEngine? = null
    private var activeConfig: Config? = null

    companion object {
        const val ACTION_CONNECT = "com.yourname.netforge.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.yourname.netforge.ACTION_DISCONNECT"
        const val EXTRA_CONFIG_ID = "extra_config_id"

        data class LogEntry(
            val timestamp: String,
            val level: String,
            val message: String
        )

        private val _serviceState = MutableStateFlow(TunnelState())
        val serviceState: StateFlow<TunnelState> = _serviceState.asStateFlow()

        private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
        val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

        fun addLog(level: String, message: String) {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
            val entry = LogEntry(
                timestamp = sdf.format(Date()),
                level = level,
                message = message
            )
            val current = _logs.value.toMutableList()
            if (current.size > 500) {
                current.removeAt(0)
            }
            current.add(entry)
            _logs.value = current
        }

        fun clearLogs() {
            _logs.value = emptyList()
        }

        fun startVpn(context: Context, configId: Long) {
            val intent = Intent(context, NetForgeVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_CONFIG_ID, configId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopVpn(context: Context) {
            val intent = Intent(context, NetForgeVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        TunnelNotification.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val configId = intent.getLongExtra(EXTRA_CONFIG_ID, -1L)
                connectTunnel(configId)
            }
            ACTION_DISCONNECT -> {
                disconnectTunnel()
            }
        }
        return START_NOT_STICKY
    }

    private fun connectTunnel(configId: Long) {
        serviceScope.launch {
            try {
                val db = NetForgeDatabase.getDatabase(applicationContext)
                val repo = ConfigRepository(db.configDao())
                val config = if (configId != -1L) {
                    repo.getConfigById(configId)
                } else {
                    repo.getActiveConfig()
                }

                if (config == null) {
                    addLog("ERROR", "No active config found to connect")
                    stopSelf()
                    return@launch
                }

                activeConfig = config
                addLog("INFO", "Preparing VPN tunnel for config: ${config.name}")

                // Establish VPN Interface
                val builder = Builder()
                    .setSession(config.name)
                    .setMtu(config.mtu)
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)

                // Configure DNS
                if (config.dns.primary.isNotBlank()) {
                    try { builder.addDnsServer(config.dns.primary) } catch (_: Exception) {}
                }
                if (config.dns.secondary.isNotBlank()) {
                    try { builder.addDnsServer(config.dns.secondary) } catch (_: Exception) {}
                }

                vpnInterface = builder.establish()
                addLog("INFO", "VPN Tun interface established (MTU ${config.mtu})")

                // Start Foreground Service
                val initialNotification = TunnelNotification.buildNotification(
                    this@NetForgeVpnService,
                    TunnelState(
                        status = TunnelStatus.CONNECTING,
                        activeConfigName = config.name,
                        activeNode = "${config.host}:${config.port}"
                    )
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        TunnelNotification.NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                    )
                } else {
                    startForeground(TunnelNotification.NOTIFICATION_ID, initialNotification)
                }

                // Initialize Tunnel Engine with socket protector
                val engine = TunnelEngine(socketProtector = { socket -> protect(socket) })
                engine.logCallback = object : TunnelEngine.LogCallback {
                    override fun log(level: String, message: String) {
                        addLog(level, message)
                    }
                }

                tunnelEngine = engine

                // Sync state with service flow and notification updates
                serviceScope.launch {
                    engine.tunnelState.collect { state ->
                        _serviceState.value = state
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(
                            TunnelNotification.NOTIFICATION_ID,
                            TunnelNotification.buildNotification(this@NetForgeVpnService, state)
                        )
                    }
                }

                engine.start(config, serviceScope)

            } catch (e: Exception) {
                addLog("ERROR", "Failed to start VPN: ${e.localizedMessage}")
                disconnectTunnel()
            }
        }
    }

    private fun disconnectTunnel() {
        serviceScope.launch {
            addLog("INFO", "Tearing down VPN session…")
            tunnelEngine?.stop()
            tunnelEngine = null

            try {
                vpnInterface?.close()
            } catch (_: Exception) {}
            vpnInterface = null

            // Wipe config from memory on disconnect
            activeConfig = null

            _serviceState.value = TunnelState(status = TunnelStatus.DISCONNECTED)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            addLog("INFO", "VPN interface closed and sensitive memory wiped.")
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        tunnelEngine?.stop()
        try { vpnInterface?.close() } catch (_: Exception) {}
        activeConfig = null
        super.onDestroy()
    }
}
