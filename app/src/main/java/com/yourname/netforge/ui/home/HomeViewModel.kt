package com.yourname.netforge.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.netforge.data.repo.ConfigRepository
import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.domain.model.TunnelState
import com.yourname.netforge.domain.model.TunnelStatus
import com.yourname.netforge.service.NetForgeVpnService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ConfigRepository) : ViewModel() {

    val tunnelState: StateFlow<TunnelState> = NetForgeVpnService.serviceState

    val activeConfig: StateFlow<Config?> = repository.activeConfigFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    fun toggleConnect(context: Context, onVpnPermissionRequired: () -> Unit) {
        val currentState = tunnelState.value.status
        if (currentState == TunnelStatus.CONNECTED || currentState == TunnelStatus.CONNECTING) {
            NetForgeVpnService.stopVpn(context)
        } else {
            val config = activeConfig.value
            if (config != null) {
                NetForgeVpnService.startVpn(context, config.id)
            } else {
                NetForgeVpnService.addLog("WARN", "Please select or create a config before connecting")
            }
        }
    }
}
