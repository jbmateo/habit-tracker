package app.habittracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.habittracker.data.db.HabitDatabase
import app.habittracker.domain.model.TodaySort
import app.habittracker.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlPreferencesRepository(
    private val db: HabitDatabase,
    private val io: CoroutineDispatcher,
) : PreferencesRepository {
    private val q = db.preferenceQueries

    override fun observeTodaySort(): Flow<TodaySort> =
        q.selectValue(TODAY_SORT).asFlow().mapToOneOrNull(io).map(::parseSort).distinctUntilChanged()

    override suspend fun setTodaySort(sort: TodaySort) = withContext(io) {
        q.upsert(TODAY_SORT, sort.name)
        Unit
    }

    /** One-shot read of the stored Today sort. */
    suspend fun todaySortNow(): TodaySort = withContext(io) { parseSort(q.selectValue(TODAY_SORT).executeAsOneOrNull()) }

    private fun parseSort(stored: String?): TodaySort =
        TodaySort.entries.firstOrNull { it.name == stored } ?: TodaySort.Category

    private companion object {
        const val TODAY_SORT = "todaySort"
    }
}
