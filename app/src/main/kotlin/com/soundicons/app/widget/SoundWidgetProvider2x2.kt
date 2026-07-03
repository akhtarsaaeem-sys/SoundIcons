package com.soundicons.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Legacy 2×2 provider — delegates to new rendering system.
 */
class SoundWidgetProvider2x2 : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val repo = WidgetEntryPoint.resolve(context).widgetRepository()
        appWidgetIds.forEach { widgetId ->
            scope.launch {
                val icon    = repo.getSoundIconForWidget(widgetId) ?: return@launch
                val mapping = repo.getMapping(widgetId) ?: return@launch
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val views   = WidgetRemoteViewsHelper.build(
                    context, widgetId, icon, mapping, options, appWidgetManager
                )
                appWidgetManager.updateAppWidget(widgetId, views)
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
}
