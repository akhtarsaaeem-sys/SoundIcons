package com.soundicons.app.data.db

import androidx.room.*
import com.soundicons.app.data.model.SoundIcon
import com.soundicons.app.data.model.WidgetMapping
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: WidgetMapping)

    @Update
    suspend fun update(mapping: WidgetMapping)

    @Query("SELECT * FROM widget_mappings WHERE app_widget_id = :appWidgetId")
    suspend fun getByWidgetId(appWidgetId: Int): WidgetMapping?

    @Query("DELETE FROM widget_mappings WHERE app_widget_id = :appWidgetId")
    suspend fun deleteByWidgetId(appWidgetId: Int)

    @Query("SELECT app_widget_id FROM widget_mappings WHERE sound_icon_id = :soundIconId")
    suspend fun getWidgetIdsForIcon(soundIconId: Long): List<Int>

    @Query("""
        SELECT si.*
        FROM sound_icons si
        INNER JOIN widget_mappings wm ON si.id = wm.sound_icon_id
        WHERE wm.app_widget_id = :appWidgetId
        LIMIT 1
    """)
    suspend fun getSoundIconForWidget(appWidgetId: Int): SoundIcon?

    @Query("SELECT * FROM widget_mappings")
    fun observeAll(): Flow<List<WidgetMapping>>

    @Query("SELECT * FROM widget_mappings WHERE app_widget_id = :appWidgetId")
    fun observeMapping(appWidgetId: Int): Flow<WidgetMapping?>
}
