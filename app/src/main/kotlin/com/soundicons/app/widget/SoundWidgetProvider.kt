package com.soundicons.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "SoundWidgetProvider"

/**
 * Legacy AppWidgetProvider — kept so widgets placed before this update continue working.
 * All rendering now delegates to [WidgetRemoteViewsHelper] and [WidgetBitmapHelper].
 * No layout switching, no fixed size categories.
 */
class SoundWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val repo = WidgetEntryPoint.resolve(context).widgetRepository()
        appWidgetIds.forEach { widgetId ->
            scope.launch {
                val icon    = repo.getSoundIconForWidget(widgetId)
                val mapping = repo.getMapping(widgetId)
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                if (icon != null && mapping != null) {
                    val views = WidgetRemoteViewsHelper.build(
                        context, widgetId, icon, mapping, options, appWidgetManager
                    )
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val repo = WidgetEntryPoint.resolve(context).widgetRepository()
        scope.launch {
            val icon    = repo.getSoundIconForWidget(appWidgetId) ?: return@launch
            val mapping = repo.getMapping(appWidgetId) ?: return@launch
            val views   = WidgetRemoteViewsHelper.build(
                context, appWidgetId, icon, mapping, newOptions, appWidgetManager
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val repo = WidgetEntryPoint.resolve(context).widgetRepository()
        appWidgetIds.forEach { widgetId -> scope.launch { repo.removeMapping(widgetId) } }
    }

    companion object {
        fun refreshAll(context: Context) {
            SoundWidgetIconOnly.refreshAll(context)
            SoundWidgetIconAndName.refreshAll(context)
        }
    }
}
