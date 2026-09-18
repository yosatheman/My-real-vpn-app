package com.yourname.netforge.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import com.yourname.netforge.service.NetForgeVpnService
import kotlinx.coroutines.flow.StateFlow

class LogsViewModel : ViewModel() {

    val logs: StateFlow<List<NetForgeVpnService.Companion.LogEntry>> = NetForgeVpnService.logs

    fun clearLogs() {
        NetForgeVpnService.clearLogs()
    }

    fun copyLogsToClipboard(context: Context) {
        val allLogs = logs.value.joinToString(separator = "\n") {
            "[${it.timestamp}] [${it.level}] ${it.message}"
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("NetForge Logs", allLogs)
        clipboard.setPrimaryClip(clip)
    }
}
