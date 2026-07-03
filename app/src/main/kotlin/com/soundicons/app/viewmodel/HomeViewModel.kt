package com.soundicons.app.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundicons.app.data.model.Category
import com.soundicons.app.data.model.SoundIcon
import com.soundicons.app.data.model.WidgetMapping
import com.soundicons.app.data.repository.CategoryRepository
import com.soundicons.app.data.repository.SoundIconRepository
import com.soundicons.app.data.repository.WidgetRepository
import com.soundicons.app.settings.AppSettings
import com.soundicons.app.util.AudioPlayerManager
import com.soundicons.app.util.BackupUtil
import com.soundicons.app.widget.SoundWidgetIconAndName
import com.soundicons.app.widget.SoundWidgetIconOnly
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FilterMode { ALL, FAVORITES, CATEGORY }

data class HomeUiState(
    val icons:           List<SoundIcon>    = emptyList(),
    val categories:      List<String>       = emptyList(),
    val categoryObjects: List<Category>     = emptyList(),
    val widgetMappings:  List<WidgetMapping> = emptyList(),
    val selectedCategory: String?           = null,
    val searchQuery:     String             = "",
    val filterMode:      FilterMode         = FilterMode.ALL,
    val isLoading:       Boolean            = true,
    val masterVolume:    Float              = 1f,
    val snackbarMessage: String?            = null,
    val autoStopPrevious: Boolean           = true
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soundRepo:      SoundIconRepository,
    private val widgetRepo:     WidgetRepository,
    private val categoryRepo:   CategoryRepository,
    private val appSettings:    AppSettings,
    val audioPlayer:            AudioPlayerManager
) : ViewModel() {

    private val _searchQuery      = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _filterMode       = MutableStateFlow(FilterMode.ALL)
    private val _masterVolume     = MutableStateFlow(1f)
    private val _snackbar         = MutableStateFlow<String?>(null)

    private val _icons: Flow<List<SoundIcon>> = combine(
        _searchQuery.debounce(150), _selectedCategory, _filterMode
    ) { q, cat, mode -> Triple(q, cat, mode) }.flatMapLatest { (q, cat, mode) ->
        when {
            q.isNotBlank()             -> soundRepo.searchIcons(q)
            mode == FilterMode.FAVORITES -> soundRepo.getFavorites()
            cat != null                -> soundRepo.getIconsByCategory(cat)
            else                       -> soundRepo.getAllIcons()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _icons,
        soundRepo.getAllCategories(),
        categoryRepo.getAll(),
        widgetRepo.observeAll(),
        _selectedCategory,
        _searchQuery,
        _filterMode,
        _masterVolume,
        _snackbar,
        appSettings.autoStopPrevious
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState(
            icons            = args[0] as List<SoundIcon>,
            categories       = args[1] as List<String>,
            categoryObjects  = args[2] as List<Category>,
            widgetMappings   = args[3] as List<WidgetMapping>,
            selectedCategory = args[4] as String?,
            searchQuery      = args[5] as String,
            filterMode       = args[6] as FilterMode,
            masterVolume     = args[7] as Float,
            isLoading        = false,
            snackbarMessage  = args[8] as String?,
            autoStopPrevious = args[9] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    // ── Audio ─────────────────────────────────────────────────────────────────

    fun playSound(icon: SoundIcon) =
        audioPlayer.play(icon.id, icon.audioUri, icon.volume * _masterVolume.value,
            icon.loopAudio, icon.trimStartMs, icon.trimEndMs)

    fun stopSound()               = audioPlayer.stop()
    fun setMasterVolume(v: Float) { _masterVolume.value = v; audioPlayer.setVolume(v) }

    // ── Filters ───────────────────────────────────────────────────────────────

    fun setSearchQuery(q: String)  { _searchQuery.value = q }
    fun setCategory(cat: String?)  { _selectedCategory.value = cat; _filterMode.value = if (cat != null) FilterMode.CATEGORY else FilterMode.ALL }
    fun setFilterMode(m: FilterMode) { _filterMode.value = m; if (m != FilterMode.CATEGORY) _selectedCategory.value = null }

    // ── Favorites ─────────────────────────────────────────────────────────────

    fun toggleFavorite(icon: SoundIcon) {
        viewModelScope.launch {
            soundRepo.setFavorite(icon.id, !icon.isFavorite)
        }
    }

    // ── Categories ────────────────────────────────────────────────────────────

    fun createCategory(name: String) {
        viewModelScope.launch { categoryRepo.insert(Category(name = name.trim())) }
    }
    fun renameCategory(category: Category, newName: String) {
        viewModelScope.launch { categoryRepo.update(category.copy(name = newName.trim())) }
    }
    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryRepo.delete(category) }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun deleteIcon(icon: SoundIcon) {
        viewModelScope.launch {
            try {
                soundRepo.deleteIcon(icon)
                SoundWidgetIconOnly.refreshAll(context)
                SoundWidgetIconAndName.refreshAll(context)
                _snackbar.value = "\"${icon.name}\" deleted"
            } catch (e: Exception) {
                Log.e("HomeVM", "Delete failed", e); _snackbar.value = "Delete failed"
            }
        }
    }

    fun onIconSaved(soundIconId: Long) { SoundWidgetIconOnly.refreshAll(context)
        SoundWidgetIconAndName.refreshAll(context) }

    // ── Backup / Restore (Feature 6) ──────────────────────────────────────────

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val icons   = soundRepo.getAllIconsSnapshot()
                val success = BackupUtil.exportToUri(context, uri, icons)
                _snackbar.value = if (success) "Backup exported!" else "Export failed"
            }.onFailure { _snackbar.value = "Export error: ${it.message}" }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val icons = BackupUtil.importFromUri(context, uri)
                if (icons != null) {
                    soundRepo.importIcons(icons.map { it.copy(id = 0) })
                    _snackbar.value = "Imported ${icons.size} icons!"
                } else {
                    _snackbar.value = "Import failed — invalid or corrupt file"
                }
            }.onFailure { _snackbar.value = "Import error: ${it.message}" }
        }
    }

    // ── Drag-and-drop reorder (Feature 3) ───────────────────────────────────────

    /**
     * Called after the user drops an icon into a new position.
     * [fromIndex] and [toIndex] are positions in the *currently visible* list.
     * We reassign sortOrder values so the new order is persisted to Room.
     */
    fun reorderIcons(fromIndex: Int, toIndex: Int) {
        val current = uiState.value.icons.toMutableList()
        if (fromIndex == toIndex) return
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        // Move the item
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)

        // Assign new sortOrder values based on neighbours
        viewModelScope.launch {
            // Give each icon a sortOrder spaced 1000 apart so future insertions
            // can slot in without requiring a full rewrite of all rows.
            current.forEachIndexed { index, icon ->
                val newOrder = (index + 1).toLong() * 1000L
                if (icon.sortOrder != newOrder) {
                    soundRepo.updateSortOrder(icon.id, newOrder)
                }
            }
        }
    }

    fun clearSnackbar() { _snackbar.value = null }

    override fun onCleared() { super.onCleared(); audioPlayer.release() }
}
