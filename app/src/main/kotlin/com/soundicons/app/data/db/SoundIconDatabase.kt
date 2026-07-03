package com.soundicons.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.soundicons.app.data.model.Category
import com.soundicons.app.data.model.SoundIcon
import com.soundicons.app.data.model.WidgetMapping

@Database(
    entities     = [SoundIcon::class, WidgetMapping::class, Category::class],
    version      = 5,
    exportSchema = false
)
abstract class SoundIconDatabase : RoomDatabase() {
    abstract fun soundIconDao(): SoundIconDao
    abstract fun widgetMappingDao(): WidgetMappingDao
    abstract fun categoryDao(): CategoryDao

    companion object {

        /** v3→v4: adds sort_order to sound_icons */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE sound_icons ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE sound_icons SET sort_order = createdAt")
            }
        }

        /**
         * v4→v5: removes widget_size column from widget_mappings.
         *
         * SQLite cannot DROP COLUMN before API 35, so we recreate the table
         * without widget_size and copy the data across.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create new table without widget_size
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS widget_mappings_new (
                        app_widget_id INTEGER NOT NULL PRIMARY KEY,
                        sound_icon_id INTEGER NOT NULL,
                        volume_percent INTEGER NOT NULL DEFAULT 100,
                        display_mode TEXT NOT NULL DEFAULT 'ICON_AND_NAME',
                        FOREIGN KEY (sound_icon_id) REFERENCES sound_icons(id)
                        ON DELETE CASCADE
                    )
                """.trimIndent())

                // 2. Copy existing rows (drop widget_size silently)
                db.execSQL("""
                    INSERT INTO widget_mappings_new
                        (app_widget_id, sound_icon_id, volume_percent, display_mode)
                    SELECT app_widget_id, sound_icon_id, volume_percent, display_mode
                    FROM widget_mappings
                """.trimIndent())

                // 3. Drop old table and rename
                db.execSQL("DROP TABLE widget_mappings")
                db.execSQL("ALTER TABLE widget_mappings_new RENAME TO widget_mappings")

                // 4. Recreate index
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_widget_mappings_sound_icon_id " +
                    "ON widget_mappings(sound_icon_id)"
                )
            }
        }
    }
}
