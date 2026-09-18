package com.yourname.netforge.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.netforge.ui.theme.DarkCardBorder
import com.yourname.netforge.ui.theme.StatusGreen
import com.yourname.netforge.ui.theme.TextMutedDark
import com.yourname.netforge.ui.theme.VioletPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDetailScreen(
    viewModel: ConfigDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToExport: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(config?.name ?: "Config Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (config != null) {
                        IconButton(
                            onClick = { onNavigateToExport(config!!.id) },
                            modifier = Modifier.testTag("detail_export_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export .nf")
                        }
                        IconButton(
                            onClick = { onNavigateToEdit(config!!.id) },
                            modifier = Modifier.testTag("detail_edit_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (config == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val item = config!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Set Active Action Bar
                if (!item.isActive) {
                    Button(
                        onClick = { viewModel.setActive() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_set_active_config"),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as Active Config")
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = StatusGreen.copy(alpha = 0.15f),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StatusGreen))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = StatusGreen)
                            Text(
                                text = "Currently Active Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusGreen
                            )
                        }
                    }
                }

                // Section: Server Node
                DetailSection(title = "SERVER NODE") {
                    DetailRow("Host", item.host)
                    DetailRow("Port", item.port.toString())
                }

                // Section: Payload & Tunnel
                DetailSection(title = "TUNNEL & PAYLOAD") {
                    DetailRow("Tunnel Mode", item.payload.mode.displayName)
                    if (item.payload.sni.isNotBlank()) {
                        DetailRow("SNI Fronting", item.payload.sni)
                    }
                    if (item.payload.proxyHost.isNotBlank()) {
                        DetailRow("Proxy Chain", "${item.payload.proxyHost}:${item.payload.proxyPort}")
                    }
                    if (item.payload.sshUser.isNotBlank()) {
                        DetailRow("SSH User", item.payload.sshUser)
                        DetailRow("SSH Pass", "••••••••")
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text("Injected HTTP Payload:", style = MaterialTheme.typography.labelMedium, color = TextMutedDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = item.payload.rawPayload,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Section: DNS
                DetailSection(title = "DNS SERVERS") {
                    DetailRow("Primary DNS", item.dns.primary)
                    DetailRow("Secondary DNS", item.dns.secondary)
                }

                // Section: Advanced
                DetailSection(title = "ADVANCED SETTINGS") {
                    DetailRow("Keepalive", "${item.keepalive} s")
                    DetailRow("MTU", "${item.mtu} bytes")
                    DetailRow("UDP Forwarding", if (item.udp) "Enabled" else "Disabled")
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = VioletPrimary
            )
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextMutedDark)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
