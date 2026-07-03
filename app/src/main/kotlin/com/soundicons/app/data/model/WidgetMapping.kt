package com.soundicons.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Maps an Android AppWidget ID to a [SoundIcon].
 *
 * displayMode: controls which provider class the widget was placed from:
 *   ICON_ONLY     → SoundWidgetIconOnly
 *   ICON_AND_NAME → SoundWidgetIconAndName
 *
 * widgetSize column REMOVED in v5 — the launcher controls all sizing.
 * volumePercent: per-widget volume 0–100.
 */
@Entity(
    tableName = "widget_mappings",
    foreignKeys = [
        ForeignKey(
            entity        = SoundIcon::class,
            parentColumns = ["id"],
            childColumns  = ["sound_icon_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sound_icon_id"])]
)
data class WidgetMapping(
    @PrimaryKey
    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int,

    @ColumnInfo(name = "sound_icon_id")
    val soundIconId: Long,

    /** Per-widget volume 0–100 */
    @ColumnInfo(name = "volume_percent")
    val volumePercent: Int = 100,

    /**
     * Display style — determines which AppWidgetProvider class rendered this widget.
     * Used only for display; the provider class already encodes the style.
     */
    @ColumnInfo(name = "display_mode")
    val displayMode: String = DisplayMode.ICON_AND_NAME
) {
    object DisplayMode {
        const val ICON_ONLY     = "ICON_ONLY"
        const val ICON_AND_NAME = "ICON_AND_NAME"
    }
}
