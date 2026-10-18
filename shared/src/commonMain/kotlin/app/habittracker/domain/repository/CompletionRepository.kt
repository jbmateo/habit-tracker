package app.habittracker.domain.repository

import app.habittracker.domain.model.Completion
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface CompletionRepository {
    fun observeAll(): Flow<List<Completion>>
    fun observeForHabit(habitId: String): Flow<Set<LocalDate>>

    /** Atomic; returns whether the habit is done on [date] after the toggle. */
    suspend fun toggle(habitId: String, date: LocalDate): Boolean
}
