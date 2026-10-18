package app.habittracker.fakes

import app.habittracker.domain.model.TodaySort
import app.habittracker.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePreferencesRepository(initial: TodaySort = TodaySort.Category) : PreferencesRepository {
    val todaySort = MutableStateFlow(initial)
    override fun observeTodaySort(): Flow<TodaySort> = todaySort
    override suspend fun setTodaySort(sort: TodaySort) { todaySort.value = sort }
}
