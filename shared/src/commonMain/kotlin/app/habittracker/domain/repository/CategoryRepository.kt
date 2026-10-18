package app.habittracker.domain.repository

import app.habittracker.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    /** Ordered by sortOrder. */
    fun observeAll(): Flow<List<Category>>
    suspend fun get(id: String): Category?

    /** Returns the new id; sortOrder = max + 1. */
    suspend fun create(name: String, colorKey: String): String
    suspend fun update(id: String, name: String, colorKey: String)
    suspend fun delete(id: String)

    /** Counts active and archived habits. */
    suspend fun habitCount(id: String): Long
    suspend fun isEmpty(): Boolean
}
