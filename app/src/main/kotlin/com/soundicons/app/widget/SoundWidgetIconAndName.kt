package com.soundicons.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "SoundWidgetIconAndName"

/**
 * AppWidgetProvider for the "Icon + Name" style.
 *
 * A LinearLayout (vertical) with:
 *   - ImageView (weight=1, fills remaining space)
 *   - TextView  (wrap_content, always visible)
 *
 * The name label is ALWAYS visible — it is never set to GONE.
 * Resizing the widget simply gives more space to the image; the label
 * stays at the bottom and scales its text size proportionally.
 */
class SoundWidgetIconAndName : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate: ${appWidgetIds.size} widget(s)")
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
                } else {
                    appWidgetManager.updateAppWidget(
                        widgetId,
                        WidgetRemoteViewsHelper.buildPlaceholder(
                            context,
                            com.soundicons.app.data.model.WidgetMapping.DisplayMode.ICON_AND_NAME
                        )
                    )
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
        Log.d(TAG, "onAppWidgetOptionsChanged: widget $appWidgetId resized")
        val repo = WidgetEntryPoint.resolve(context).widgetRepository()
        scope.launch {
            val icon    = repo.getSoundIconForWidget(appWidgetId)
            val mapping = repo.getMapping(appWidgetId)
            if (icon != null && mapping != null) {
                val views = WidgetRemoteViewsHelper.build(
                    context, appWidgetId, icon, mapping, newOptions, appWidgetManager
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val repo = WidgetEntryPoint.resolve(context).widgetRepository()
        appWidgetIds.forEach { widgetId ->
            scope.launch { repo.removeMapping(widgetId) }
        }
    }

    override fun onEnabled(context: Context)  = Unit
    override fun onDisabled(context: Context) = Unit

    companion object {
        fun refresh(context: Context, appWidgetId: Int) {
            val repo    = WidgetEntryPoint.resolve(context).widgetRepository()
            val manager = AppWidgetManager.getInstance(context)
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val icon    = repo.getSoundIconForWidget(appWidgetId) ?: return@launch
                val mapping = repo.getMapping(appWidgetId) ?: return@launch
                val options = manager.getAppWidgetOptions(appWidgetId)
                val views   = WidgetRemoteViewsHelper.build(
                    context, appWidgetId, icon, mapping, options, manager
                )
                manager.updateAppWidget(appWidgetId, views)
            }
        }

        fun refreshAll(context: Context) {
            val repo    = WidgetEntryPoint.resolve(context).widgetRepository()
            val manager = AppWidgetManager.getInstance(context)
            val ids     = manager.getAppWidgetIds(
                ComponentName(context, SoundWidgetIconAndName::class.java)
            )
            ids.forEach { refresh(context, it) }
        }
    }
}
