package com.soundicons.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.soundicons.app.service.*

private const val TAG = "WidgetClickReceiver"
const val ACTION_WIDGET_CLICK = "com.soundicons.app.ACTION_WIDGET_CLICK"

class WidgetClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WIDGET_CLICK) return

        val widgetId   = intent.getIntExtra(AUDIO_EXTRA_WIDGET_ID, -1)
        val audioUri   = intent.getStringExtra(AUDIO_EXTRA_URI)
        val name       = intent.getStringExtra(AUDIO_EXTRA_NAME) ?: "Sound"
        val volume     = intent.getFloatExtra(AUDIO_EXTRA_VOLUME, 1f)
        val loop       = intent.getBooleanExtra(AUDIO_EXTRA_LOOP, false)
        val trimStart  = intent.getLongExtra(AUDIO_EXTRA_TRIM_START_MS, 0L)
        val trimEnd    = intent.getLongExtra(AUDIO_EXTRA_TRIM_END_MS, 0L)
        val vibrate    = intent.getBooleanExtra(AUDIO_EXTRA_VIBRATE, false)
        val vibrateMs  = intent.getLongExtra(AUDIO_EXTRA_VIBRATE_MS, 100L)

        if (audioUri.isNullOrBlank()) {
            Log.w(TAG, "Widget $widgetId tapped but has no audio URI")
            return
        }

        Log.d(TAG, "Widget $widgetId → '$name'")

        val svc = AudioPlaybackService.buildPlayIntent(
            context     = context,
            widgetId    = widgetId,
            audioUri    = audioUri,
            name        = name,
            volume      = volume,
            loop        = loop,
            trimStartMs = trimStart,
            trimEndMs   = trimEnd,
            vibrate     = vibrate,
            vibrateMs   = vibrateMs
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc)
        } else {
            context.startService(svc)
        }
    }
}
