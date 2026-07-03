package com.soundicons.app.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundicons.app.data.model.SoundIcon
import com.soundicons.app.data.repository.CategoryRepository
import com.soundicons.app.data.repository.SoundIconRepository
import com.soundicons.app.data.repository.WidgetRepository
import com.soundicons.app.util.ImageCropUtil
import com.soundicons.app.util.CropShape
import com.soundicons.app.util.UriUtil
import com.soundicons.app.widget.SoundWidgetIconAndName
import com.soundicons.app.widget.SoundWidgetIconOnly
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.soundicons.app.util.AudioPlayerManager
import javax.inject.Inject

data class EditorUiState(
    val name: String                  = "",
    val audioUri: String              = "",
    val imageUri: String?             = null,
    val croppedImageUri: String?      = null,
    val category: String              = "",
    val volume: Float                 = 1f,
    val loopAudio: Boolean            = false,
    // Feature 2: Trim
    val trimStartMs: Long             = 0L,
    val trimEndMs: Long               = 0L,
    val audioDurationMs: Long         = 0L,
    // Feature 1: Crop
    val cropShape: CropShape          = CropShape.ROUNDED_SQUARE,
    val showCropSheet: Boolean        = false,
    // Feature 9: Favorites
    val isFavorite: Boolean           = false,
    // Feature 11: Vibration
    val vibrationEnabled: Boolean     = false,
    val vibrationDurationMs: Long     = 100L,
    val isEditMode: Boolean           = false,
    val isSaving: Boolean             = false,
    val savedSuccessfully: Boolean    = false,
    val nameError: String?            = null,
    val audioError: String?           = null,
    val existingCategories: List<String> = emptyList()
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soundIconRepository: SoundIconRepository,
    private val categoryRepository:  CategoryRepository,
    private val widgetRepository:    WidgetRepository,
    savedStateHandle: SavedStateHandle,
    val audioPlayer: AudioPlayerManager          // public so EditorScreen can observe playback
) : ViewModel() {

    private val iconId: Long? = savedStateHandle.get<Long>("iconId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val allCategories = soundIconRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            soundIconRepository.getAllCategories().collect { cats ->
                _uiState.value = _uiState.value.copy(existingCategories = cats)
            }
        }
        iconId?.let { loadIcon(it) }
    }

    private fun loadIcon(id: Long) {
        viewModelScope.launch {
            soundIconRepository.getIconById(id)?.let { icon ->
                _uiState.value = _uiState.value.copy(
                    name              = icon.name,
                    audioUri          = icon.audioUri,
                    imageUri          = icon.imageUri,
                    croppedImageUri   = icon.croppedImageUri,
                    category          = icon.category ?: "",
                    volume            = icon.volume,
                    loopAudio         = icon.loopAudio,
                    trimStartMs       = icon.trimStartMs,
                    trimEndMs         = icon.trimEndMs,
                    isFavorite        = icon.isFavorite,
                    vibrationEnabled  = icon.vibrationEnabled,
                    vibrationDurationMs = icon.vibrationDurationMs,
                    isEditMode        = true
                )
                // Load audio duration
                if (icon.audioUri.isNotBlank()) loadAudioDuration(icon.audioUri)
            }
        }
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setName(name: String)           { _uiState.value = _uiState.value.copy(name = name, nameError = null) }
    fun setCategory(cat: String)        { _uiState.value = _uiState.value.copy(category = cat) }
    fun setVolume(v: Float)             { _uiState.value = _uiState.value.copy(volume = v) }
    fun setLoop(loop: Boolean)          { _uiState.value = _uiState.value.copy(loopAudio = loop) }
    fun setFavorite(fav: Boolean)       { _uiState.value = _uiState.value.copy(isFavorite = fav) }
    fun setVibration(enabled: Boolean)  { _uiState.value = _uiState.value.copy(vibrationEnabled = enabled) }
    fun setVibrationDuration(ms: Long)  { _uiState.value = _uiState.value.copy(vibrationDurationMs = ms) }
    fun setTrimStart(ms: Long)          { _uiState.value = _uiState.value.copy(trimStartMs = ms) }
    fun setTrimEnd(ms: Long)            { _uiState.value = _uiState.value.copy(trimEndMs = ms) }
    fun setCropShape(shape: CropShape)  { _uiState.value = _uiState.value.copy(cropShape = shape) }
    fun showCropSheet(show: Boolean)    { _uiState.value = _uiState.value.copy(showCropSheet = show) }

    fun setAudioUri(uri: Uri) {
        UriUtil.persistReadPermission(context, uri)
        _uiState.value = _uiState.value.copy(audioUri = uri.toString(), audioError = null,
            trimStartMs = 0L, trimEndMs = 0L)
        loadAudioDuration(uri.toString())
    }

    fun setImageUri(uri: Uri?) {
        if (uri != null) UriUtil.persistReadPermission(context, uri)
        _uiState.value = _uiState.value.copy(imageUri = uri?.toString(), croppedImageUri = null)
    }

    fun removeImage() {
        _uiState.value = _uiState.value.copy(imageUri = null, croppedImageUri = null)
    }

    /** Feature 1: Called by ImageCropSheet after it renders and saves the crop. */
    fun setCroppedImagePath(path: String) {
        _uiState.value = _uiState.value.copy(
            croppedImageUri = path,
            showCropSheet   = false
        )
    }

    /** Feature 1: Kept for backward compat — now unused (sheet handles saving). */
    fun applyCrop() = Unit

    private fun loadAudioDuration(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mmr = android.media.MediaMetadataRetriever()
                mmr.setDataSource(context, Uri.parse(uriString))
                val dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                mmr.release()
                _uiState.value = _uiState.value.copy(audioDurationMs = dur,
                    trimEndMs = if (_uiState.value.trimEndMs == 0L) dur else _uiState.value.trimEndMs)
            } catch (e: Exception) {
                Log.w("EditorVM", "Could not get audio duration: ${e.message}")
            }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) { _uiState.value = state.copy(nameError = "Name is required"); return }
        if (state.audioUri.isBlank()) { _uiState.value = state.copy(audioError = "Please select an audio file"); return }

        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val icon = SoundIcon(
                    id                  = iconId ?: 0L,
                    name                = state.name.trim(),
                    audioUri            = state.audioUri,
                    imageUri            = state.imageUri,
                    croppedImageUri     = state.croppedImageUri,
                    color               = 0xFF6650A4.toInt(), // fixed default — color picker removed
                    category            = state.category.trim().takeIf { it.isNotEmpty() },
                    volume              = state.volume,
                    loopAudio           = state.loopAudio,
                    trimStartMs         = state.trimStartMs,
                    trimEndMs           = if (state.trimEndMs >= state.audioDurationMs) 0L else state.trimEndMs,
                    isFavorite          = state.isFavorite,
                    vibrationEnabled    = state.vibrationEnabled,
                    vibrationDurationMs = state.vibrationDurationMs
                )
                val savedId = if (state.isEditMode) {
                    soundIconRepository.updateIcon(icon); iconId ?: 0L
                } else {
                    soundIconRepository.insertIcon(icon)
                }
                // Refresh any bound widgets
                withContext(Dispatchers.IO) {
                    SoundWidgetIconOnly.refreshAll(context)
                SoundWidgetIconAndName.refreshAll(context)
                }
                _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
            } catch (e: Exception) {
                Log.e("EditorVM", "Save failed", e)
                _uiState.value = _uiState.value.copy(isSaving = false, audioError = "Save failed: ${e.localizedMessage}")
            }
        }
    }
}
