package com.soundicons.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.soundicons.app.R
import com.soundicons.app.data.model.SoundIcon
import com.soundicons.app.data.model.WidgetMapping
import com.soundicons.app.service.AUDIO_EXTRA_LOOP
import com.soundicons.app.service.AUDIO_EXTRA_NAME
import com.soundicons.app.service.AUDIO_EXTRA_TRIM_END_MS
import com.soundicons.app.service.AUDIO_EXTRA_TRIM_START_MS
import com.soundicons.app.service.AUDIO_EXTRA_URI
import com.soundicons.app.service.AUDIO_EXTRA_VIBRATE
import com.soundicons.app.service.AUDIO_EXTRA_VIBRATE_MS
import com.soundicons.app.service.AUDIO_EXTRA_VOLUME
import com.soundicons.app.service.AUDIO_EXTRA_WIDGET_ID

private const val TAG = "WidgetRemoteViewsHelper"

/**
 * Builds RemoteViews for both widget providers.
 *
 * Rectangular sizing:
 *   - Reads OPTION_APPWIDGET_MAX_WIDTH and OPTION_APPWIDGET_MAX_HEIGHT
 *     as separate values (not collapsed to a single min-side).
 *   - Converts both to pixels and passes widthPx + heightPx to
 *     [WidgetBitmapHelper.load] so the bitmap fills the full widget
 *     rectangle at any aspect ratio.
 *   - For ICON_AND_NAME: the image height is reduced by an estimate of
 *     the label height so the bitmap doesn't overlap the label area.
 *
 * Text scaling:
 *   - Label text size = 9sp + proportional bonus based on widget height.
 *   - Clamped to 9sp–16sp so it never becomes illegibly tiny or huge.
 */
object WidgetRemoteViewsHelper {

    fun build(
        context:          Context,
        appWidgetId:      Int,
        icon:             SoundIcon,
        mapping:          WidgetMapping,
        options:          Bundle,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val displayMode = mapping.displayMode
        val layoutId    = if (displayMode == WidgetMapping.DisplayMode.ICON_ONLY)
            R.layout.widget_icon_only
        else
            R.layout.widget_icon_and_name

        val views = RemoteViews(context.packageName, layoutId)

        // ── Read actual widget dimensions ────────────────────────────────────
        // On most launchers:
        //   MIN = portrait size,  MAX = landscape size (or same for square)
        // We use MAX so the bitmap is sized for the largest the widget will be.
        // coerceAtLeast(40) prevents degenerate 0-size bitmaps.
        val minWDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,  57)
        val minHDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 57)
        val maxWDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,  minWDp)
        val maxHDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHDp)

        val widthDp  = maxWDp.coerceAtLeast(minWDp).coerceAtLeast(40)
        val heightDp = maxHDp.coerceAtLeast(minHDp).coerceAtLeast(40)

        val widthPx  = WidgetBitmapHelper.dpToPx(context, widthDp)
        val heightPx = WidgetBitmapHelper.dpToPx(context, heightDp)

        // ── Compute image area (subtract label height for ICON_AND_NAME) ─────
        //
        // The layout has the image filling weight=1 above the label.
        // We estimate the label height so the bitmap matches its actual
        // render area and doesn't appear stretched.
        //
        // Text size ranges from ~9sp to ~16sp; we use the midpoint ~13sp
        // for the estimate, plus 4dp top margin + 1dp bottom padding.
        // This is approximate — the system does the real layout — but it
        // keeps the bitmap aspect ratio close to the visual image area.
        val imageBitmapWidthPx:  Int
        val imageBitmapHeightPx: Int

        if (displayMode == WidgetMapping.DisplayMode.ICON_AND_NAME) {
            val textSp          = (9f + (heightDp - 57f) / 30f).coerceIn(9f, 16f)
            val labelHeightDp   = (textSp * 1.4f + 6f).toInt()   // line height + margins
            val imageHeightDp   = (heightDp - labelHeightDp).coerceAtLeast(40)
            imageBitmapWidthPx  = widthPx
            imageBitmapHeightPx = WidgetBitmapHelper.dpToPx(context, imageHeightDp)
        } else {
            imageBitmapWidthPx  = widthPx
            imageBitmapHeightPx = heightPx
        }

        // ── Icon bitmap — rectangular ────────────────────────────────────────
        val bitmap = WidgetBitmapHelper.load(
            context, icon,
            imageBitmapWidthPx,
            imageBitmapHeightPx
        )
        views.setImageViewBitmap(R.id.widget_icon_image, bitmap)

        // ── Label (ICON_AND_NAME only) ───────────────────────────────────────
        if (displayMode == WidgetMapping.DisplayMode.ICON_AND_NAME) {
            views.setTextViewText(R.id.widget_icon_label, icon.name)
            views.setViewVisibility(R.id.widget_icon_label, View.VISIBLE)

            // Scale label text with widget height; clamped to readable range
            val textSp = (9f + (heightDp - 57f) / 30f).coerceIn(9f, 16f)
            views.setTextViewTextSize(
                R.id.widget_icon_label,
                android.util.TypedValue.COMPLEX_UNIT_SP,
                textSp
            )
        } else {
            views.setTextViewText(R.id.widget_icon_label, "")
        }

        // ── Tap PendingIntent → play sound ───────────────────────────────────
        val volume = (mapping.volumePercent / 100f).coerceIn(0f, 1f)
        val clickIntent = Intent(context, WidgetClickReceiver::class.java).apply {
            action = ACTION_WIDGET_CLICK
            data   = Uri.parse("soundicons://widget/$appWidgetId")
            putExtra(AUDIO_EXTRA_WIDGET_ID,     appWidgetId)
            putExtra(AUDIO_EXTRA_URI,           icon.audioUri)
            putExtra(AUDIO_EXTRA_NAME,          icon.name)
            putExtra(AUDIO_EXTRA_VOLUME,        volume)
            putExtra(AUDIO_EXTRA_LOOP,          icon.loopAudio)
            putExtra(AUDIO_EXTRA_TRIM_START_MS, icon.trimStartMs)
            putExtra(AUDIO_EXTRA_TRIM_END_MS,   icon.trimEndMs)
            putExtra(AUDIO_EXTRA_VIBRATE,       icon.vibrationEnabled)
            putExtra(AUDIO_EXTRA_VIBRATE_MS,    icon.vibrationDurationMs)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        return views
    }

    /** Placeholder shown when no icon is configured yet */
    fun buildPlaceholder(context: Context, displayMode: String): RemoteViews {
        val layoutId = if (displayMode == WidgetMapping.DisplayMode.ICON_ONLY)
            R.layout.widget_icon_only
        else
            R.layout.widget_icon_and_name

        val views = RemoteViews(context.packageName, layoutId)

        // Use a 1:1 placeholder bitmap for the initial unset state
        val sizePx = WidgetBitmapHelper.dpToPx(context, 80)
        val bitmap = WidgetBitmapHelper.load(
            context,
            SoundIcon(id = -1, name = "♪", audioUri = "", color = 0xFF6650A4.toInt()),
            sizePx   // square overload — placeholder is always square
        )
        views.setImageViewBitmap(R.id.widget_icon_image, bitmap)

        if (displayMode == WidgetMapping.DisplayMode.ICON_AND_NAME) {
            views.setTextViewText(R.id.widget_icon_label, "Sound Icon")
            views.setViewVisibility(R.id.widget_icon_label, View.VISIBLE)
        } else {
            views.setTextViewText(R.id.widget_icon_label, "")
        }
        return views
    }
}
