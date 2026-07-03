package com.soundicons.app.data.db

import androidx.room.*
import com.soundicons.app.data.model.SoundIcon
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundIconDao {

    /** All icons ordered by user-defined sortOrder, then creation time */
    @Query("SELECT * FROM sound_icons ORDER BY sort_order ASC, createdAt ASC")
    fun getAllIcons(): Flow<List<SoundIcon>>

    @Query("SELECT * FROM sound_icons WHERE is_favorite = 1 ORDER BY sort_order ASC, createdAt ASC")
    fun getFavorites(): Flow<List<SoundIcon>>

    @Query("SELECT * FROM sound_icons WHERE category = :category ORDER BY sort_order ASC, createdAt ASC")
    fun getIconsByCategory(category: String): Flow<List<SoundIcon>>

    @Query("SELECT * FROM sound_icons WHERE category_id = :categoryId ORDER BY sort_order ASC, createdAt ASC")
    fun getIconsByCategoryId(categoryId: Long): Flow<List<SoundIcon>>

    /**
     * Search by name OR category — both fields are checked so
     * typing "meme" will surface icons in a "Memes" category.
     */
    @Query("""
        SELECT * FROM sound_icons
        WHERE name LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
        ORDER BY sort_order ASC, createdAt ASC
    """)
    fun searchIcons(query: String): Flow<List<SoundIcon>>

    @Query("SELECT DISTINCT category FROM sound_icons WHERE category IS NOT NULL ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM sound_icons WHERE id = :id")
    suspend fun getIconById(id: Long): SoundIcon?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIcon(icon: SoundIcon): Long

    @Update
    suspend fun updateIcon(icon: SoundIcon)

    @Delete
    suspend fun deleteIcon(icon: SoundIcon)

    @Query("DELETE FROM sound_icons WHERE id = :id")
    suspend fun deleteIconById(id: Long)

    @Query("UPDATE sound_icons SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    /** Update sort_order for a single icon — called after every drag-drop reorder */
    @Query("UPDATE sound_icons SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Long)

    @Query("UPDATE sound_icons SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM sound_icons")
    suspend fun getCount(): Int

    @Query("SELECT * FROM sound_icons ORDER BY sort_order ASC, createdAt ASC")
    suspend fun getAllIconsSnapshot(): List<SoundIcon>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(icons: List<SoundIcon>)
}
