package com.yourname.netforge.ui.configs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.netforge.data.repo.ConfigRepository
import com.yourname.netforge.domain.model.Config
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigListViewModel(private val repository: ConfigRepository) : ViewModel() {

    val configs: StateFlow<List<Config>> = repository.configsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        viewModelScope.launch {
            repository.seedDefaultsIfEmpty()
        }
    }

    fun setActive(id: Long) {
        viewModelScope.launch {
            repository.setActiveConfig(id)
        }
    }

    fun renameConfig(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renameConfig(id, newName)
        }
    }

    fun deleteConfig(id: Long) {
        viewModelScope.launch {
            repository.deleteConfig(id)
        }
    }

    fun importConfig(config: Config) {
        viewModelScope.launch {
            repository.saveConfig(config)
        }
    }
}
