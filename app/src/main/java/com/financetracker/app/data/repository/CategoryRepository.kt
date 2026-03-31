package com.financetracker.app.data.repository

import com.financetracker.app.data.db.dao.CategoryDao
import com.financetracker.app.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao
) {
    fun getAll(): Flow<List<CategoryEntity>> = dao.getAll()

    suspend fun getByName(name: String): CategoryEntity? = dao.getByName(name)

    suspend fun insert(category: CategoryEntity): Long = dao.insert(category)

    suspend fun update(category: CategoryEntity) = dao.update(category)

    suspend fun delete(category: CategoryEntity) = dao.delete(category)
}
