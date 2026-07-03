package com.soundicons.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "BootReceiver"

/** Redraws all widgets after device reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON") return

        Log.d(TAG, "Boot complete — refreshing all Sound Icon widgets")
        SoundWidgetIconOnly.refreshAll(context)
        SoundWidgetIconAndName.refreshAll(context)
    }
}
