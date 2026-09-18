package com.yourname.netforge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.netforge.data.prefs.SecurePrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val securePrefs: SecurePrefs) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = securePrefs.isDarkTheme.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val isAutoConnect: StateFlow<Boolean> = securePrefs.isAutoConnect.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val isVerboseLogs: StateFlow<Boolean> = securePrefs.isVerboseLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val isCustomDns: StateFlow<Boolean> = securePrefs.isCustomDns.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val primaryDns: StateFlow<String> = securePrefs.primaryDns.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "1.1.1.1"
    )

    val secondaryDns: StateFlow<String> = securePrefs.secondaryDns.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "8.8.8.8"
    )

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { securePrefs.setDarkTheme(enabled) }
    }

    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch { securePrefs.setAutoConnect(enabled) }
    }

    fun setVerboseLogs(enabled: Boolean) {
        viewModelScope.launch { securePrefs.setVerboseLogs(enabled) }
    }

    fun setCustomDns(enabled: Boolean, primary: String, secondary: String) {
        viewModelScope.launch { securePrefs.setCustomDns(enabled, primary, secondary) }
    }
}
