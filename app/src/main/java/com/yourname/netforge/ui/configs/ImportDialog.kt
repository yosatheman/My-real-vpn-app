package com.yourname.netforge.ui.configs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.netforge.crypto.AesGcm
import com.yourname.netforge.data.file.NfFileReader
import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.ui.components.PasswordField
import com.yourname.netforge.ui.theme.VioletPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImportSuccess: (Config) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var passphrase by remember { mutableStateOf("") }
    var isDecrypting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = uri.lastPathSegment ?: "config.nf"
            errorMessage = null
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isDecrypting) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, tint = VioletPrimary)
                Text("Import Encrypted .nf")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // File selection
                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_select_file"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = selectedFileName ?: "Select .nf File")
                }

                PasswordField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        errorMessage = null
                    },
                    label = "Decryption Passphrase",
                    placeholder = "Enter file passphrase",
                    isError = errorMessage != null,
                    errorMessage = errorMessage
                )

                if (isDecrypting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Deriving Argon2id key & decrypting…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUri == null) {
                        errorMessage = "Please select a .nf file first"
                        return@Button
                    }
                    isDecrypting = true
                    errorMessage = null

                    coroutineScope.launch {
                        try {
                            val uri = selectedUri!!
                            val passChars = passphrase.toCharArray()
                            val config = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri).use { stream ->
                                    if (stream == null) throw IllegalStateException("Cannot open file stream")
                                    NfFileReader.readEncryptedNf(stream, passChars)
                                }
                            }
                            AesGcm.wipe(passChars)
                            isDecrypting = false
                            onImportSuccess(config)
                        } catch (e: Exception) {
                            isDecrypting = false
                            errorMessage = e.localizedMessage ?: "Decryption failed (bad tag or invalid key)"
                        }
                    }
                },
                enabled = !isDecrypting && selectedUri != null,
                modifier = Modifier.testTag("btn_confirm_import")
            ) {
                Text("Decrypt & Import")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDecrypting,
                modifier = Modifier.testTag("btn_cancel_import")
            ) {
                Text("Cancel")
            }
        }
    )
}
