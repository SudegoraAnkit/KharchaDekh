package com.ankitsudegora.data

import kotlinx.coroutines.flow.Flow

interface ICategoryRepository {
    val allCategories: Flow<List<Category>>
    suspend fun getAllCategoriesList(): List<Category>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}
