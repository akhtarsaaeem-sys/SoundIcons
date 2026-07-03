package com.soundicons.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundicons.app.data.repository.SoundIconRepository
import com.soundicons.app.settings.AppSettings
import com.soundicons.app.util.BackupUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode:       String  = "SYSTEM",
    val autoStopPrevious: Boolean = true,
    val hapticFeedback:  Boolean = true,
    val defaultLoop:     Boolean = false,
    val backupMessage:   String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
    private val soundRepo:   SoundIconRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        appSettings.darkTheme,
        appSettings.autoStopPrevious,
        appSettings.hapticFeedback,
        appSettings.defaultLoop
    ) { theme, autoStop, haptic, loop ->
        SettingsUiState(themeMode = theme, autoStopPrevious = autoStop, hapticFeedback = haptic, defaultLoop = loop)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _backupMessage = MutableStateFlow<String?>(null)

    fun setTheme(value: String)           { viewModelScope.launch { appSettings.setDarkTheme(value) } }
    fun setAutoStopPrevious(v: Boolean)   { viewModelScope.launch { appSettings.setAutoStopPrevious(v) } }
    fun setHapticFeedback(v: Boolean)     { viewModelScope.launch { appSettings.setHapticFeedback(v) } }
    fun setDefaultLoop(v: Boolean)        { viewModelScope.launch { appSettings.setDefaultLoop(v) } }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val icons   = soundRepo.getAllIconsSnapshot()
                val success = BackupUtil.exportToUri(context, uri, icons)
                _backupMessage.value = if (success) "✓ Backup exported successfully" else "Export failed"
            }.onFailure { _backupMessage.value = "Export error: ${it.message}" }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val icons = BackupUtil.importFromUri(context, uri)
                if (icons != null) {
                    soundRepo.importIcons(icons.map { it.copy(id = 0) })
                    _backupMessage.value = "✓ Imported ${icons.size} sounds"
                } else {
                    _backupMessage.value = "Import failed — invalid file"
                }
            }.onFailure { _backupMessage.value = "Import error: ${it.message}" }
        }
    }
}
