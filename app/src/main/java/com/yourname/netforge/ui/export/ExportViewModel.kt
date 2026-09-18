package com.yourname.netforge.ui.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.netforge.crypto.AesGcm
import com.yourname.netforge.data.file.NfFileWriter
import com.yourname.netforge.data.repo.ConfigRepository
import com.yourname.netforge.domain.model.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExportViewModel(
    private val repository: ConfigRepository,
    private val configId: Long
) : ViewModel() {

    private val _config = MutableStateFlow<Config?>(null)
    val config: StateFlow<Config?> = _config.asStateFlow()

    var passphrase = MutableStateFlow("")
    var confirmPassphrase = MutableStateFlow("")

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

    init {
        viewModelScope.launch {
            _config.value = repository.getConfigById(configId)
        }
    }

    fun exportToUri(context: Context, uri: Uri, onSuccess: () -> Unit) {
        val conf = _config.value ?: return
        if (passphrase.value != confirmPassphrase.value) {
            _exportResult.value = "Passphrases do not match"
            return
        }
        if (passphrase.value.length < 6) {
            _exportResult.value = "Passphrase must be at least 6 characters"
            return
        }

        _isExporting.value = true
        _exportResult.value = null

        viewModelScope.launch {
            try {
                val passChars = passphrase.value.toCharArray()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        NfFileWriter.writeEncryptedNf(conf, passChars, outStream)
                    } ?: throw IllegalStateException("Cannot open output stream for selected file")
                }
                AesGcm.wipe(passChars)
                _isExporting.value = false
                _exportResult.value = "SUCCESS"
                onSuccess()
            } catch (e: Exception) {
                _isExporting.value = false
                _exportResult.value = e.localizedMessage ?: "Failed to export config"
            }
        }
    }
}
