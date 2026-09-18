package com.yourname.netforge.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.netforge.ui.theme.DarkCardBorder
import com.yourname.netforge.ui.theme.StatusGreen
import com.yourname.netforge.ui.theme.TextMutedDark
import com.yourname.netforge.ui.theme.VioletPrimary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isAutoConnect by viewModel.isAutoConnect.collectAsState()
    val isVerboseLogs by viewModel.isVerboseLogs.collectAsState()
    val isCustomDns by viewModel.isCustomDns.collectAsState()
    val primaryDns by viewModel.primaryDns.collectAsState()
    val secondaryDns by viewModel.secondaryDns.collectAsState()

    var editingPrimaryDns by remember(primaryDns) { mutableStateOf(primaryDns) }
    var editingSecondaryDns by remember(secondaryDns) { mutableStateOf(secondaryDns) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // General / Appearance
        SettingsSection(title = "GENERAL & APPEARANCE", icon = Icons.Default.Palette) {
            SettingsToggleRow(
                title = "Dark Theme",
                subtitle = "Use dark violet surface interface",
                checked = isDarkTheme,
                onCheckedChange = { viewModel.setDarkTheme(it) },
                tag = "toggle_dark_theme"
            )
            Divider(color = DarkCardBorder)
            SettingsToggleRow(
                title = "Auto-Connect on Boot",
                subtitle = "Connect active configuration automatically",
                checked = isAutoConnect,
                onCheckedChange = { viewModel.setAutoConnect(it) },
                tag = "toggle_auto_connect"
            )
            Divider(color = DarkCardBorder)
            SettingsToggleRow(
                title = "Verbose Logging",
                subtitle = "Record detailed handshake events in logs",
                checked = isVerboseLogs,
                onCheckedChange = { viewModel.setVerboseLogs(it) },
                tag = "toggle_verbose_logs"
            )
        }

        // DNS Configuration
        SettingsSection(title = "DNS CONFIGURATION", icon = Icons.Default.Dns) {
            SettingsToggleRow(
                title = "Override System DNS",
                subtitle = "Route queries through custom secure resolvers",
                checked = isCustomDns,
                onCheckedChange = {
                    viewModel.setCustomDns(it, editingPrimaryDns, editingSecondaryDns)
                },
                tag = "toggle_custom_dns"
            )

            if (isCustomDns) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = editingPrimaryDns,
                    onValueChange = {
                        editingPrimaryDns = it
                        viewModel.setCustomDns(true, it, editingSecondaryDns)
                    },
                    label = { Text("Primary DNS Server") },
                    placeholder = { Text("1.1.1.1") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_primary_dns"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editingSecondaryDns,
                    onValueChange = {
                        editingSecondaryDns = it
                        viewModel.setCustomDns(true, editingPrimaryDns, it)
                    },
                    label = { Text("Secondary DNS Server") },
                    placeholder = { Text("8.8.8.8") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_secondary_dns"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        // About NetForge & Security Architecture
        SettingsSection(title = "ABOUT NETFORGE", icon = Icons.Default.Security) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "1.0.0 (Clean-room build)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedDark
                )
            }

            Divider(color = DarkCardBorder)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Security & Privacy Commitment",
                    style = MaterialTheme.typography.titleMedium,
                    color = VioletPrimary
                )
                Text(
                    text = "• Original clean-room implementation with no borrowed code.\n" +
                           "• No external analytics, trackers, or advertising SDKs.\n" +
                           "• Configs stored with AES-256-GCM via Android Keystore.\n" +
                           "• Sensitive cryptographic keys zeroed in memory after use.\n" +
                           "• .nf export files protected with Argon2id key derivation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedDark,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = VioletPrimary, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = VioletPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMutedDark)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
