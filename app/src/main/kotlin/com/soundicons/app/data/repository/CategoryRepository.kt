package com.soundicons.app.data.repository

import com.soundicons.app.data.db.CategoryDao
import com.soundicons.app.data.model.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(private val dao: CategoryDao) {
    fun getAll(): Flow<List<Category>> = dao.getAll()
    suspend fun getById(id: Long): Category? = dao.getById(id)
    suspend fun insert(category: Category): Long = dao.insert(category)
    suspend fun update(category: Category) = dao.update(category)
    suspend fun delete(category: Category) = dao.delete(category)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
