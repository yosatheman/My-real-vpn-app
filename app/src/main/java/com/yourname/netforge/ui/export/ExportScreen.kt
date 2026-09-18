package com.yourname.netforge.ui.export

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.netforge.ui.components.PasswordField
import com.yourname.netforge.ui.components.StrengthMeter
import com.yourname.netforge.ui.theme.DarkCardBorder
import com.yourname.netforge.ui.theme.StatusGreen
import com.yourname.netforge.ui.theme.TextMutedDark
import com.yourname.netforge.ui.theme.VioletPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val passphrase by viewModel.passphrase.collectAsState()
    val confirmPassphrase by viewModel.confirmPassphrase.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()

    var showSuccessDialog by remember { mutableStateOf(false) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(context, uri) {
                showSuccessDialog = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Export Config") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("export_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Config summary card
            if (config != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TARGET CONFIGURATION",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = VioletPrimary
                        )
                        Text(
                            text = config!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${config!!.payload.mode.displayName} • ${config!!.host}:${config!!.port}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMutedDark
                        )
                    }
                }
            }

            // Security Info Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = VioletPrimary.copy(alpha = 0.1f),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VioletPrimary.copy(alpha = 0.3f)))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = VioletPrimary)
                    Column {
                        Text(
                            text = "Argon2id + AES-256-GCM Protection",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your config payload is encrypted with 64MB Argon2id key derivation and GCM authentication before saving.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMutedDark
                        )
                    }
                }
            }

            // Passphrase Fields
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordField(
                    value = passphrase,
                    onValueChange = { viewModel.passphrase.value = it },
                    label = "Export Passphrase",
                    placeholder = "Choose a strong passphrase"
                )

                StrengthMeter(password = passphrase)

                PasswordField(
                    value = confirmPassphrase,
                    onValueChange = { viewModel.confirmPassphrase.value = it },
                    label = "Confirm Passphrase",
                    placeholder = "Re-enter passphrase",
                    isError = confirmPassphrase.isNotBlank() && confirmPassphrase != passphrase,
                    errorMessage = "Passphrases do not match"
                )
            }

            if (exportResult != null && exportResult != "SUCCESS") {
                Text(
                    text = exportResult!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            val defaultFileName = (config?.name?.replace(" ", "_") ?: "config") + ".nf"

            Button(
                onClick = {
                    if (passphrase.isBlank() || passphrase != confirmPassphrase) {
                        Toast.makeText(context, "Please enter matching passphrases", Toast.LENGTH_SHORT).show()
                    } else if (passphrase.length < 6) {
                        Toast.makeText(context, "Passphrase must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    } else {
                        createDocLauncher.launch(defaultFileName)
                    }
                },
                enabled = !isExporting && passphrase.isNotBlank() && passphrase == confirmPassphrase && passphrase.length >= 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_export_save"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Encrypted .nf")
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateBack()
            },
            title = { Text("Export Complete") },
            text = { Text("Your configuration was successfully encrypted and exported as .nf.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("btn_export_done")
                ) {
                    Text("Done")
                }
            }
        )
    }
}
