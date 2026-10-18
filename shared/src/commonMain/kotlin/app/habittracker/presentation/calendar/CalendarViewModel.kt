package app.habittracker.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.time.AppClock
import app.habittracker.domain.usecase.DayDetail
import app.habittracker.domain.usecase.MonthCalendar
import app.habittracker.domain.usecase.ObserveDayDetail
import app.habittracker.domain.usecase.ObserveMonthCalendar
import app.habittracker.domain.usecase.ToggleCompletion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val observeMonth: ObserveMonthCalendar,
    private val observeDay: ObserveDayDetail,
    private val toggle: ToggleCompletion,
    private val clock: AppClock,
) : ViewModel() {
    private val effectChannel = Channel<CalendarEffect>(Channel.BUFFERED)
    val effects: Flow<CalendarEffect> = effectChannel.receiveAsFlow()

    // Mutated only from onEvent, which callers invoke on the Main dispatcher; the read-then-write
    // in onEvent is therefore not atomic and relies on that single-threaded access.
    private val month = MutableStateFlow(currentMonth())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    /** Pairs each month with its calendar so state never shows a month next to another month's days. */
    private val monthCalendar: Flow<Pair<YearMonth, MonthCalendar?>> = month.flatMapLatest { m ->
        observeMonth(m).map<MonthCalendar, Pair<YearMonth, MonthCalendar?>> { m to it }
            .onStart { emit(m to null) }
    }

    private val selectedDetail: Flow<DayDetail?> = selectedDate.flatMapLatest { date ->
        if (date == null) flowOf(null) else observeDay(date).map<DayDetail, DayDetail?> { it }.onStart { emit(null) }
    }

    val uiState: StateFlow<CalendarUiState> =
        combine(monthCalendar, selectedDetail, clock.todayFlow) { (m, calendar), selected, today ->
            CalendarUiState(
                month = m,
                leadingBlanks = m.firstDay.dayOfWeek.isoDayNumber - 1,
                calendar = calendar,
                selected = selected,
                canGoNext = m < today.toYearMonth(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    fun onEvent(event: CalendarEvent) {
        when (event) {
            CalendarEvent.PrevMonth -> {
                month.value = month.value.minus(1, DateTimeUnit.MONTH)
                selectedDate.value = null
            }
            CalendarEvent.NextMonth -> if (month.value < clock.today().toYearMonth()) {
                month.value = month.value.plus(1, DateTimeUnit.MONTH)
                selectedDate.value = null
            }
            is CalendarEvent.SelectDay -> if (event.date <= clock.today()) selectedDate.value = event.date
            CalendarEvent.ClearSelection -> selectedDate.value = null
            is CalendarEvent.ToggleHabit -> viewModelScope.launch {
                try {
                    val outcome = toggle(event.id, event.date)
                    if (outcome is Outcome.Failure) {
                        effectChannel.send(CalendarEffect.ShowDomainError(outcome.error))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    effectChannel.send(CalendarEffect.ShowError(e.message ?: "Something went wrong"))
                }
            }
        }
    }

    private fun currentMonth(): YearMonth = clock.today().toYearMonth()

    private fun initialState(): CalendarUiState {
        val m = month.value
        return CalendarUiState(m, m.firstDay.dayOfWeek.isoDayNumber - 1, null, null, canGoNext = false)
    }
}

private fun LocalDate.toYearMonth(): YearMonth = YearMonth(year, month)
