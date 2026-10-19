package app.habittracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.habittracker.data.db.HabitDatabase
import app.habittracker.domain.model.MilestoneSeen
import app.habittracker.domain.model.TodaySort
import app.habittracker.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

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

    override fun observeCelebratedOn(): Flow<LocalDate?> =
        q.selectValue(CELEBRATED_ON).asFlow().mapToOneOrNull(io)
            .map { stored -> stored?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
            .distinctUntilChanged()

    override suspend fun setCelebratedOn(date: LocalDate) = withContext(io) {
        q.upsert(CELEBRATED_ON, date.toString())
        Unit
    }

    override fun observeMilestoneSeen(): Flow<MilestoneSeen?> =
        q.selectValue(MILESTONE_SEEN).asFlow().mapToOneOrNull(io).map(::parseMilestoneSeen).distinctUntilChanged()

    override suspend fun setMilestoneSeen(seen: MilestoneSeen) = withContext(io) {
        q.upsert(MILESTONE_SEEN, "${seen.runStart}:${seen.milestone}")
        Unit
    }

    /** Stored as "<runStart ISO date>:<milestone>". */
    private fun parseMilestoneSeen(stored: String?): MilestoneSeen? {
        val (date, milestone) = stored?.split(':')?.takeIf { it.size == 2 } ?: return null
        val runStart = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        return milestone.toIntOrNull()?.let { MilestoneSeen(runStart, it) }
    }

    /** One-shot read of the stored Today sort. */
    suspend fun todaySortNow(): TodaySort = withContext(io) { parseSort(q.selectValue(TODAY_SORT).executeAsOneOrNull()) }

    private fun parseSort(stored: String?): TodaySort =
        TodaySort.entries.firstOrNull { it.name == stored } ?: TodaySort.Category

    private companion object {
        const val TODAY_SORT = "todaySort"
        const val CELEBRATED_ON = "celebrated_on"
        const val MILESTONE_SEEN = "milestone_seen"
    }
}
