package com.yourname.netforge.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.netforge.domain.model.TunnelMode
import com.yourname.netforge.ui.components.PasswordField
import com.yourname.netforge.ui.theme.DarkCardBorder
import com.yourname.netforge.ui.theme.TextMutedDark
import com.yourname.netforge.ui.theme.VioletPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadEditorScreen(
    viewModel: PayloadEditorViewModel,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name by viewModel.name.collectAsState()
    val host by viewModel.host.collectAsState()
    val port by viewModel.port.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val sni by viewModel.sni.collectAsState()
    val rawPayload by viewModel.rawPayload.collectAsState()
    val sshUser by viewModel.sshUser.collectAsState()
    val sshPass by viewModel.sshPass.collectAsState()
    val proxyHost by viewModel.proxyHost.collectAsState()
    val proxyPort by viewModel.proxyPort.collectAsState()
    val livePreview by viewModel.livePreview.collectAsState()

    var modeMenuExpanded by remember { mutableStateOf(false) }

    val placeholders = listOf(
        "[host_port]",
        "[front_host]",
        "[real_host]",
        "[crlf]",
        "[host]",
        "[port]",
        "[random]",
        "[ua]",
        "[protocol]",
        "[ssh_user]",
        "[ssh_pass]",
        "[cr]",
        "[lf]"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Payload & Config Editor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveConfig { onSaved() } },
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Config", tint = VioletPrimary)
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Config Name
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.name.value = it },
                label = { Text("Config Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_config_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Server Node Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        viewModel.host.value = it
                        viewModel.updatePreview()
                    },
                    label = { Text("Server Host") },
                    modifier = Modifier
                        .weight(2f)
                        .testTag("input_server_host"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        viewModel.port.value = it
                        viewModel.updatePreview()
                    },
                    label = { Text("Port") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_server_port"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Mode Selector
            ExposedDropdownMenuBox(
                expanded = modeMenuExpanded,
                onExpandedChange = { modeMenuExpanded = !modeMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = mode.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tunnel Mode") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("input_tunnel_mode"),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = modeMenuExpanded,
                    onDismissRequest = { modeMenuExpanded = false }
                ) {
                    TunnelMode.entries.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.displayName) },
                            onClick = {
                                viewModel.mode.value = m
                                modeMenuExpanded = false
                                viewModel.updatePreview()
                            }
                        )
                    }
                }
            }

            // Conditional Mode Fields
            if (mode == TunnelMode.SNI || mode == TunnelMode.WEBSOCKET || mode == TunnelMode.SSH) {
                OutlinedTextField(
                    value = sni,
                    onValueChange = {
                        viewModel.sni.value = it
                        viewModel.updatePreview()
                    },
                    label = { Text("SNI / Fronting Host (optional)") },
                    placeholder = { Text("e.g. cdn.example.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sni"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (mode == TunnelMode.HTTP_CONNECT || mode == TunnelMode.SOCKS5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = proxyHost,
                        onValueChange = { viewModel.proxyHost.value = it },
                        label = { Text("Proxy Host") },
                        modifier = Modifier
                            .weight(2f)
                            .testTag("input_proxy_host"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = proxyPort,
                        onValueChange = { viewModel.proxyPort.value = it },
                        label = { Text("Proxy Port") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_proxy_port"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            if (mode == TunnelMode.SSH) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = sshUser,
                        onValueChange = {
                            viewModel.sshUser.value = it
                            viewModel.updatePreview()
                        },
                        label = { Text("SSH Username") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_ssh_user"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    PasswordField(
                        value = sshPass,
                        onValueChange = {
                            viewModel.sshPass.value = it
                            viewModel.updatePreview()
                        },
                        label = "SSH Password",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Injected Payload String
            Text(
                text = "Payload Pattern",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Placeholder chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tap placeholder to insert:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMutedDark
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    placeholders.forEach { tag ->
                        AssistChip(
                            onClick = { viewModel.insertPlaceholder(tag) },
                            label = { Text(tag, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = rawPayload,
                onValueChange = {
                    viewModel.rawPayload.value = it
                    viewModel.updatePreview()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("input_raw_payload"),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            )

            // Live Preview Box
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LIVE RESOLVED PREVIEW",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = VioletPrimary
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = livePreview.ifBlank { "(Empty payload)" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .testTag("payload_live_preview"),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.saveConfig { onSaved() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_config"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
            ) {
                Text("Save Configuration")
            }
        }
    }
}
