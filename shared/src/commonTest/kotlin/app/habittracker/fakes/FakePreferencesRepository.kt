package app.habittracker.fakes

import app.habittracker.domain.model.MilestoneSeen
import app.habittracker.domain.model.TodaySort
import app.habittracker.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

class FakePreferencesRepository(initial: TodaySort = TodaySort.Category) : PreferencesRepository {
    val todaySort = MutableStateFlow(initial)
    override fun observeTodaySort(): Flow<TodaySort> = todaySort
    override suspend fun setTodaySort(sort: TodaySort) { todaySort.value = sort }

    val celebratedOn = MutableStateFlow<LocalDate?>(null)
    override fun observeCelebratedOn(): Flow<LocalDate?> = celebratedOn
    override suspend fun setCelebratedOn(date: LocalDate) { celebratedOn.value = date }

    val milestoneSeen = MutableStateFlow<MilestoneSeen?>(null)
    override fun observeMilestoneSeen(): Flow<MilestoneSeen?> = milestoneSeen
    override suspend fun setMilestoneSeen(seen: MilestoneSeen) { milestoneSeen.value = seen }
}
