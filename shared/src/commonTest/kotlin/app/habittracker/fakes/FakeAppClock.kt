package app.habittracker.fakes

import app.habittracker.domain.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

class FakeAppClock(start: LocalDate) : AppClock {
    private val state = MutableStateFlow(start)
    override fun today(): LocalDate = state.value
    override val todayFlow: Flow<LocalDate> = state
    fun setToday(d: LocalDate) { state.value = d }
}
