package app.habittracker.domain.repository

import app.habittracker.domain.model.Habit
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

interface HabitRepository {
    /** Active and archived habits, fully hydrated. */
    fun observeAll(): Flow<List<Habit>>
    fun observe(id: String): Flow<Habit?>
    suspend fun get(id: String): Habit?

    /** Returns the new id; inserts a schedule with effectiveFrom = [createdOn]. */
    suspend fun create(name: String, iconKey: String, categoryId: String, createdOn: LocalDate, days: Set<DayOfWeek>): String
    suspend fun updateDetails(id: String, name: String, iconKey: String, categoryId: String)
    suspend fun upsertSchedule(id: String, effectiveFrom: LocalDate, days: Set<DayOfWeek>)
    suspend fun openArchivePeriod(id: String, from: LocalDate)
    suspend fun closeArchivePeriod(id: String, from: LocalDate, toExclusive: LocalDate)
    suspend fun deleteArchivePeriod(id: String, from: LocalDate)
    suspend fun delete(id: String)
}
