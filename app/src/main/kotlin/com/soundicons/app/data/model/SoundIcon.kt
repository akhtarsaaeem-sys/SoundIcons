package com.soundicons.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Core data model for a sound icon.
 *
 * DB version history:
 *  v1 → v2: added widget_mappings table
 *  v2 → v3: added trimStartMs, trimEndMs, isFavorite, categoryId,
 *            croppedImageUri, vibrationEnabled, vibrationDurationMs
 *  v3 → v4: added sortOrder for drag-and-drop reordering
 */
@Entity(tableName = "sound_icons")
data class SoundIcon(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    /** Original image URI selected from gallery */
    val imageUri: String? = null,

    /** Cropped/processed image URI (used by widgets and in-app display) */
    @ColumnInfo(name = "cropped_image_uri")
    val croppedImageUri: String? = null,

    val audioUri: String,

    val color: Int,

    /** Optional free-text category (legacy) */
    val category: String? = null,

    /** FK to Category table (null = uncategorised) */
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    /** Legacy position field kept for compat */
    val position: Int = 0,

    /**
     * User-defined sort order for drag-and-drop reordering.
     * Lower = appears earlier in the grid.
     * Defaults to a large timestamp so new items appear at the end.
     */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Long = System.currentTimeMillis(),

    val createdAt: Long = System.currentTimeMillis(),

    /** Per-sound volume 0.0–1.0 */
    val volume: Float = 1.0f,

    val loopAudio: Boolean = false,

    /** Trim start position in milliseconds (0 = beginning) */
    @ColumnInfo(name = "trim_start_ms")
    val trimStartMs: Long = 0L,

    /** Trim end position in milliseconds (0 = play to end) */
    @ColumnInfo(name = "trim_end_ms")
    val trimEndMs: Long = 0L,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "vibration_enabled")
    val vibrationEnabled: Boolean = false,

    @ColumnInfo(name = "vibration_duration_ms")
    val vibrationDurationMs: Long = 100L
)
