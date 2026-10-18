package app.habittracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.habittracker.data.db.HabitDatabase
import app.habittracker.domain.model.Category
import app.habittracker.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SqlCategoryRepository(
    private val db: HabitDatabase,
    private val io: CoroutineDispatcher,
) : CategoryRepository {
    private val q = db.categoryQueries

    override fun observeAll(): Flow<List<Category>> =
        q.selectAll(::toCategory).asFlow().mapToList(io)

    override suspend fun get(id: String): Category? = withContext(io) {
        q.selectById(id, ::toCategory).executeAsOneOrNull()
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(name: String, colorKey: String): String = withContext(io) {
        val id = Uuid.random().toString()
        db.transaction {
            val next = (q.maxSortOrder().executeAsOne().MAX ?: -1L) + 1
            q.insert(id, name, colorKey, next)
        }
        id
    }

    override suspend fun update(id: String, name: String, colorKey: String) {
        withContext(io) {
            q.update(name, colorKey, id)
        }
    }

    override suspend fun delete(id: String) {
        withContext(io) {
            q.deleteById(id)
        }
    }

    override suspend fun habitCount(id: String): Long = withContext(io) {
        db.habitQueries.countByCategory(id).executeAsOne()
    }

    override suspend fun isEmpty(): Boolean = withContext(io) {
        q.count().executeAsOne() == 0L
    }

    private fun toCategory(id: String, name: String, colorKey: String, sortOrder: Long) =
        Category(id, name, colorKey, sortOrder.toInt())
}
