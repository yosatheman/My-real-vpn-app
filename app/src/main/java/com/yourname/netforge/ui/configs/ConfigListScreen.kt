package com.yourname.netforge.ui.configs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.netforge.domain.model.Config
import com.yourname.netforge.ui.components.ConfigCard
import com.yourname.netforge.ui.theme.DarkCardBorder
import com.yourname.netforge.ui.theme.TextMutedDark
import com.yourname.netforge.ui.theme.VioletPrimary

@Composable
fun ConfigListScreen(
    viewModel: ConfigListViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val configs by viewModel.configs.collectAsState()
    var showImportDialog by remember { mutableStateOf(false) }
    var configToRename by remember { mutableStateOf<Config?>(null) }
    var newRenameName by remember { mutableStateOf("") }
    var configToDelete by remember { mutableStateOf<Config?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary FAB: Create New in Editor
                SmallFloatingActionButton(
                    onClick = { onNavigateToEditor(null) },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("fab_create_config")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Create Config")
                }

                // Primary FAB: Import .nf file
                ExtendedFloatingActionButton(
                    onClick = { showImportDialog = true },
                    icon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    text = { Text("Import .nf") },
                    containerColor = VioletPrimary,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.testTag("fab_import_config")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configurations",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (configs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No configurations found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Import a .nf file or create one from scratch",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMutedDark
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(configs, key = { it.id }) { config ->
                        ConfigCard(
                            config = config,
                            onSelect = { viewModel.setActive(config.id) },
                            onEdit = { onNavigateToEditor(config.id) },
                            onDelete = { configToDelete = config },
                            onRename = {
                                configToRename = config
                                newRenameName = config.name
                            }
                        )
                    }
                }
            }
        }
    }

    // Import Dialog
    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImportSuccess = { newConfig ->
                viewModel.importConfig(newConfig)
                showImportDialog = false
            }
        )
    }

    // Rename Dialog
    if (configToRename != null) {
        AlertDialog(
            onDismissRequest = { configToRename = null },
            title = { Text("Rename Config") },
            text = {
                OutlinedTextField(
                    value = newRenameName,
                    onValueChange = { newRenameName = it },
                    label = { Text("Config Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rename_config")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRenameName.isNotBlank()) {
                            viewModel.renameConfig(configToRename!!.id, newRenameName.trim())
                        }
                        configToRename = null
                    },
                    modifier = Modifier.testTag("btn_confirm_rename")
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { configToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (configToDelete != null) {
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            title = { Text("Delete Config?") },
            text = {
                Text("Are you sure you want to permanently delete \"${configToDelete!!.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConfig(configToDelete!!.id)
                        configToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { configToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
