package com.yourname.netforge.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.netforge.data.repo.ConfigRepository
import com.yourname.netforge.domain.model.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConfigDetailViewModel(
    private val repository: ConfigRepository,
    private val configId: Long
) : ViewModel() {

    private val _config = MutableStateFlow<Config?>(null)
    val config: StateFlow<Config?> = _config.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _config.value = repository.getConfigById(configId)
        }
    }

    fun setActive() {
        viewModelScope.launch {
            repository.setActiveConfig(configId)
            loadConfig()
        }
    }
}
