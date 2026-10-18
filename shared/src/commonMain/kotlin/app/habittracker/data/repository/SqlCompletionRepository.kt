package app.habittracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.habittracker.data.db.HabitDatabase
import app.habittracker.domain.model.Completion
import app.habittracker.domain.repository.CompletionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class SqlCompletionRepository(
    private val db: HabitDatabase,
    private val io: CoroutineDispatcher,
) : CompletionRepository {
    private val q = db.completionQueries

    override fun observeAll(): Flow<List<Completion>> =
        q.selectAll { habitId, date -> Completion(habitId, LocalDate.parse(date)) }.asFlow().mapToList(io)

    override fun observeForHabit(habitId: String): Flow<Set<LocalDate>> =
        q.selectDatesForHabit(habitId).asFlow().mapToList(io).map { dates -> dates.map(LocalDate::parse).toSet() }

    override suspend fun toggle(habitId: String, date: LocalDate): Boolean = withContext(io) {
        val key = date.toString()
        db.transactionWithResult {
            if (q.exists(habitId, key).executeAsOne() > 0) {
                q.delete(habitId, key)
                false
            } else {
                q.insert(habitId, key)
                true
            }
        }
    }
}
