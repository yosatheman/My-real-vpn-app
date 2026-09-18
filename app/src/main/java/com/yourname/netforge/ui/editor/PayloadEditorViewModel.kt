package com.yourname.netforge.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.netforge.data.repo.ConfigRepository
import com.yourname.netforge.domain.model.*
import com.yourname.netforge.domain.payload.PayloadEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PayloadEditorViewModel(
    private val repository: ConfigRepository,
    private val existingConfigId: Long?
) : ViewModel() {

    var configId: Long = existingConfigId ?: 0L
    var name = MutableStateFlow("Custom Payload Config")
    var host = MutableStateFlow("1.2.3.4")
    var port = MutableStateFlow("443")
    var mode = MutableStateFlow(TunnelMode.SSH)
    var sni = MutableStateFlow("")
    var rawPayload = MutableStateFlow("GET / HTTP/1.1[crlf]Host: [host][crlf][crlf]")
    var sshUser = MutableStateFlow("")
    var sshPass = MutableStateFlow("")
    var proxyHost = MutableStateFlow("")
    var proxyPort = MutableStateFlow("8080")
    var primaryDns = MutableStateFlow("1.1.1.1")
    var secondaryDns = MutableStateFlow("8.8.8.8")
    var keepalive = MutableStateFlow("60")
    var mtu = MutableStateFlow("1500")
    var udp = MutableStateFlow(false)

    private val _livePreview = MutableStateFlow("")
    val livePreview: StateFlow<String> = _livePreview.asStateFlow()

    init {
        if (existingConfigId != null && existingConfigId > 0) {
            viewModelScope.launch {
                val existing = repository.getConfigById(existingConfigId)
                if (existing != null) {
                    name.value = existing.name
                    host.value = existing.host
                    port.value = existing.port.toString()
                    mode.value = existing.payload.mode
                    sni.value = existing.payload.sni
                    rawPayload.value = existing.payload.rawPayload
                    sshUser.value = existing.payload.sshUser
                    sshPass.value = existing.payload.sshPass
                    proxyHost.value = existing.payload.proxyHost
                    proxyPort.value = existing.payload.proxyPort.toString()
                    primaryDns.value = existing.dns.primary
                    secondaryDns.value = existing.dns.secondary
                    keepalive.value = existing.keepalive.toString()
                    mtu.value = existing.mtu.toString()
                    udp.value = existing.udp
                    updatePreview()
                }
            }
        } else {
            updatePreview()
        }
    }

    fun insertPlaceholder(tag: String) {
        rawPayload.value = rawPayload.value + tag
        updatePreview()
    }

    fun updatePreview() {
        val dummyConfig = Config(
            name = name.value,
            host = host.value.ifBlank { "1.2.3.4" },
            port = port.value.toIntOrNull() ?: 443,
            payload = Payload(
                mode = mode.value,
                sni = sni.value,
                rawPayload = rawPayload.value,
                sshUser = sshUser.value,
                sshPass = sshPass.value,
                proxyHost = proxyHost.value,
                proxyPort = proxyPort.value.toIntOrNull() ?: 8080
            )
        )
        _livePreview.value = PayloadEngine.preview(rawPayload.value, dummyConfig)
    }

    fun saveConfig(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val config = Config(
                id = configId,
                name = name.value.ifBlank { "Custom Config" },
                host = host.value.trim().ifBlank { "127.0.0.1" },
                port = port.value.trim().toIntOrNull() ?: 443,
                payload = Payload(
                    mode = mode.value,
                    sni = sni.value.trim(),
                    rawPayload = rawPayload.value,
                    sshUser = sshUser.value.trim(),
                    sshPass = sshPass.value,
                    proxyHost = proxyHost.value.trim(),
                    proxyPort = proxyPort.value.trim().toIntOrNull() ?: 8080
                ),
                dns = DnsConfig(
                    primary = primaryDns.value.trim().ifBlank { "1.1.1.1" },
                    secondary = secondaryDns.value.trim().ifBlank { "8.8.8.8" },
                    customEnabled = true
                ),
                keepalive = keepalive.value.toIntOrNull() ?: 60,
                mtu = mtu.value.toIntOrNull() ?: 1500,
                udp = udp.value
            )
            val savedId = repository.saveConfig(config)
            onSuccess(savedId)
        }
    }
}
