package com.soundicons.app.data.repository

import com.soundicons.app.data.db.WidgetMappingDao
import com.soundicons.app.data.model.SoundIcon
import com.soundicons.app.data.model.WidgetMapping
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(private val dao: WidgetMappingDao) {

    suspend fun saveMapping(
        appWidgetId:  Int,
        soundIconId:  Long,
        volumePercent: Int    = 100,
        displayMode:  String  = WidgetMapping.DisplayMode.ICON_AND_NAME
    ) = dao.insert(WidgetMapping(appWidgetId, soundIconId, volumePercent, displayMode))

    suspend fun updateMapping(mapping: WidgetMapping)                   = dao.update(mapping)
    suspend fun getSoundIconForWidget(appWidgetId: Int): SoundIcon?     = dao.getSoundIconForWidget(appWidgetId)
    suspend fun getMapping(appWidgetId: Int): WidgetMapping?            = dao.getByWidgetId(appWidgetId)
    suspend fun removeMapping(appWidgetId: Int)                         = dao.deleteByWidgetId(appWidgetId)
    suspend fun getWidgetIdsForIcon(soundIconId: Long): List<Int>       = dao.getWidgetIdsForIcon(soundIconId)
    fun observeAll(): Flow<List<WidgetMapping>>                         = dao.observeAll()
}
