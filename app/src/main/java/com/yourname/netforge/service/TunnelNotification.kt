package com.yourname.netforge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yourname.netforge.MainActivity
import com.yourname.netforge.R
import com.yourname.netforge.domain.model.TunnelState
import com.yourname.netforge.domain.model.TunnelStatus

object TunnelNotification {
    const val CHANNEL_ID = "netforge_tunnel_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_DISCONNECT = "com.yourname.netforge.ACTION_DISCONNECT"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.tunnel_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.tunnel_notification_desc)
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        context: Context,
        state: TunnelState
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(context, NetForgeVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            context,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (state.status) {
            TunnelStatus.CONNECTED -> "Connected: ${state.activeConfigName.ifBlank { state.activeNode }}"
            TunnelStatus.CONNECTING -> "Connecting to ${state.activeNode}…"
            TunnelStatus.RECONNECTING -> "Reconnecting…"
            TunnelStatus.DISCONNECTING -> "Disconnecting…"
            TunnelStatus.DISCONNECTED -> "Disconnected"
        }

        val formattedUp = formatBytes(state.bytesOut)
        val formattedDown = formatBytes(state.bytesIn)
        val contentDetail = "↑ $formattedUp   ↓ $formattedDown" + if (state.pingMs >= 0) "   ⚡ ${state.pingMs}ms" else ""

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("NetForge - $statusText")
            .setContentText(contentDetail)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(state.status == TunnelStatus.CONNECTED || state.status == TunnelStatus.CONNECTING)
            .setOnlyAlertOnce(true)

        if (state.status == TunnelStatus.CONNECTED || state.status == TunnelStatus.CONNECTING) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                disconnectPendingIntent
            )
        }

        return builder.build()
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
