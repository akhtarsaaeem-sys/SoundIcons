package com.soundicons.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundicons.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let { uri -> viewModel.exportBackup(uri) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { uri -> viewModel.importBackup(uri) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Theme ─────────────────────────────────────────────────────
            SettingsSectionHeader("Appearance")
            Card(shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(4.dp)) {
                    Text("Theme", style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    Row(
                        Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (value, label) ->
                            FilterChip(
                                selected = uiState.themeMode == value,
                                onClick  = { viewModel.setTheme(value) },
                                label    = { Text(label) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Audio behaviour ───────────────────────────────────────────
            SettingsSectionHeader("Audio Behaviour")
            Card(shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(4.dp)) {
                    SettingsToggleRow(
                        title    = "Auto-stop previous sound",
                        subtitle = "When a new widget is tapped, stop the current playback",
                        checked  = uiState.autoStopPrevious,
                        onToggle = viewModel::setAutoStopPrevious
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SettingsToggleRow(
                        title    = "Haptic feedback",
                        subtitle = "Vibrate briefly when the app plays a sound",
                        checked  = uiState.hapticFeedback,
                        onToggle = viewModel::setHapticFeedback
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Backup & Restore ──────────────────────────────────────────
            SettingsSectionHeader("Backup & Restore")
            Card(shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Export your sounds, categories, and widget settings to a JSON file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick  = { exportLauncher.launch("soundicons_backup.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Upload, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export")
                        }
                        Button(
                            onClick  = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import")
                        }
                    }

                    uiState.backupMessage?.let { msg ->
                        Text(msg, style = MaterialTheme.typography.labelMedium,
                            color = if (msg.contains("error", ignoreCase = true) || msg.contains("failed", ignoreCase = true))
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Widget defaults ───────────────────────────────────────────
            SettingsSectionHeader("Widget Defaults")
            Card(shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(4.dp)) {
                    SettingsToggleRow(
                        title    = "Default: loop audio",
                        subtitle = "New sound icons default to looping",
                        checked  = uiState.defaultLoop,
                        onToggle = viewModel::setDefaultLoop
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Sound Icons v2.0", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
