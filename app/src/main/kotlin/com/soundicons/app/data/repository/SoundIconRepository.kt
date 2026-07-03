package com.soundicons.app.data.repository

import com.soundicons.app.data.db.SoundIconDao
import com.soundicons.app.data.model.SoundIcon
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundIconRepository @Inject constructor(private val dao: SoundIconDao) {

    fun getAllIcons(): Flow<List<SoundIcon>> = dao.getAllIcons()
    fun getFavorites(): Flow<List<SoundIcon>> = dao.getFavorites()
    fun getIconsByCategory(category: String): Flow<List<SoundIcon>> = dao.getIconsByCategory(category)
    fun getIconsByCategoryId(categoryId: Long): Flow<List<SoundIcon>> = dao.getIconsByCategoryId(categoryId)
    fun searchIcons(query: String): Flow<List<SoundIcon>> = dao.searchIcons(query)
    fun getAllCategories(): Flow<List<String>> = dao.getAllCategories()
    suspend fun getIconById(id: Long): SoundIcon? = dao.getIconById(id)
    suspend fun insertIcon(icon: SoundIcon): Long = dao.insertIcon(icon)
    suspend fun updateIcon(icon: SoundIcon) = dao.updateIcon(icon)
    suspend fun deleteIcon(icon: SoundIcon) = dao.deleteIcon(icon)
    suspend fun deleteIconById(id: Long) = dao.deleteIconById(id)
    suspend fun updatePosition(id: Long, position: Int) = dao.updatePosition(id, position)
    suspend fun updateSortOrder(id: Long, sortOrder: Long) = dao.updateSortOrder(id, sortOrder)
    suspend fun setFavorite(id: Long, isFavorite: Boolean) = dao.setFavorite(id, isFavorite)
    suspend fun getAllIconsSnapshot(): List<SoundIcon> = dao.getAllIconsSnapshot()
    suspend fun importIcons(icons: List<SoundIcon>) = dao.insertAll(icons)
    suspend fun getCount(): Int = dao.getCount()
}
