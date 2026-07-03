package com.soundicons.app.viewmodel

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundicons.app.data.model.SoundIcon
import com.soundicons.app.data.model.WidgetMapping
import com.soundicons.app.data.repository.SoundIconRepository
import com.soundicons.app.data.repository.WidgetRepository
import com.soundicons.app.widget.SoundWidgetIconAndName
import com.soundicons.app.widget.SoundWidgetIconOnly
import com.soundicons.app.widget.WidgetRemoteViewsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    private val soundIconRepo: SoundIconRepository,
    private val widgetRepo:    WidgetRepository
) : ViewModel() {

    val allIcons: StateFlow<List<SoundIcon>> = soundIconRepo.getAllIcons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Saves the mapping and immediately renders the widget.
     * No widgetSize parameter — the launcher owns sizing.
     */
    fun saveWidget(
        context:      Context,
        appWidgetId:  Int,
        soundIcon:    SoundIcon,
        volumePercent: Int    = 100,
        displayMode:  String  = WidgetMapping.DisplayMode.ICON_AND_NAME
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                widgetRepo.saveMapping(appWidgetId, soundIcon.id, volumePercent, displayMode)
                val manager = AppWidgetManager.getInstance(context)
                val mapping = widgetRepo.getMapping(appWidgetId) ?: return@launch
                val options = manager.getAppWidgetOptions(appWidgetId)
                val views   = WidgetRemoteViewsHelper.build(
                    context, appWidgetId, soundIcon, mapping, options, manager
                )
                manager.updateAppWidget(appWidgetId, views)
                Log.d("WidgetConfigVM", "Widget $appWidgetId saved → '${soundIcon.name}' ($displayMode)")
            } catch (e: Exception) {
                Log.e("WidgetConfigVM", "Failed to save widget $appWidgetId", e)
            }
        }
    }
}
