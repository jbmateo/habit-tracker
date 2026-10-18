package app.habittracker.fakes

import app.habittracker.domain.model.Completion
import app.habittracker.domain.repository.CompletionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class FakeCompletionRepository(private val store: FakeStore) : CompletionRepository {
    override fun observeAll(): Flow<List<Completion>> = store.completions

    override fun observeForHabit(habitId: String): Flow<Set<LocalDate>> =
        store.completions.map { list -> list.filter { it.habitId == habitId }.map { it.date }.toSet() }

    override suspend fun toggle(habitId: String, date: LocalDate): Boolean {
        store.checkWrite()
        val c = Completion(habitId, date)
        val done = c !in store.completions.value
        store.completions.value = if (done) store.completions.value + c else store.completions.value - c
        return done
    }
}
