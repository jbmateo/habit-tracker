package app.habittracker.presentation

import app.cash.turbine.test
import app.habittracker.HEALTH
import app.habittracker.date
import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.model.DomainError
import app.habittracker.domain.usecase.ObserveDayDetail
import app.habittracker.domain.usecase.ObserveMonthCalendar
import app.habittracker.domain.usecase.ToggleCompletion
import app.habittracker.fakes.FakeAppClock
import app.habittracker.fakes.FakeCompletionRepository
import app.habittracker.fakes.FakeHabitRepository
import app.habittracker.fakes.FakeStore
import app.habittracker.habit
import app.habittracker.presentation.calendar.CalendarEffect
import app.habittracker.presentation.calendar.CalendarEvent
import app.habittracker.presentation.calendar.CalendarUiState
import app.habittracker.presentation.calendar.CalendarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.YearMonth
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val store = FakeStore().apply { categories.value = listOf(HEALTH) }
    private val habits = FakeHabitRepository(store)
    private val completions = FakeCompletionRepository(store)
    private val clock = FakeAppClock(date("2026-10-18"))

    private fun vm() = CalendarViewModel(
        ObserveMonthCalendar(habits, completions, clock),
        ObserveDayDetail(habits, completions, clock),
        ToggleCompletion(habits, completions, clock),
        clock,
    )

    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    /** Skips items until the calendar for [month] has loaded. */
    private suspend fun app.cash.turbine.ReceiveTurbine<CalendarUiState>.loaded(month: YearMonth): CalendarUiState {
        while (true) {
            val s = awaitItem()
            if (s.month == month && s.calendar?.month == month) return s
        }
    }

    @Test fun startsAtCurrentMonthWithLeadingBlanks() = runTest {
        vm().uiState.test {
            val loading = awaitItem()
            assertEquals(YearMonth(2026, 10), loading.month)
            assertNull(loading.calendar)
            val s = loaded(YearMonth(2026, 10))
            assertEquals(3, s.leadingBlanks) // 1 Oct 2026 is a Thursday
            assertEquals(31, s.calendar!!.days.size)
            assertNull(s.selected)
            assertEquals(false, s.canGoNext)
        }
    }

    @Test fun prevNextNavigation() = runTest {
        val vm = vm()
        vm.uiState.test {
            loaded(YearMonth(2026, 10))
            vm.onEvent(CalendarEvent.PrevMonth)
            val sep = loaded(YearMonth(2026, 9))
            assertEquals(1, sep.leadingBlanks) // 1 Sep 2026 is a Tuesday
            assertEquals(true, sep.canGoNext)
            vm.onEvent(CalendarEvent.NextMonth)
            val oct = loaded(YearMonth(2026, 10))
            assertEquals(false, oct.canGoNext)
        }
    }

    @Test fun cannotGoPastCurrentMonth() = runTest {
        val vm = vm()
        vm.uiState.test {
            loaded(YearMonth(2026, 10))
            vm.onEvent(CalendarEvent.NextMonth)
            expectNoEvents()
            assertEquals(YearMonth(2026, 10), vm.uiState.value.month)
        }
    }

    @Test fun selectDayShowsDetail() = runTest {
        store.habits.value = listOf(habit("a", created = "2026-10-01"))
        val vm = vm()
        vm.uiState.test {
            loaded(YearMonth(2026, 10))
            vm.onEvent(CalendarEvent.SelectDay(date("2026-10-17")))
            var s = awaitItem()
            while (s.selected == null) s = awaitItem()
            assertEquals(date("2026-10-17"), s.selected.date)
            assertEquals(true, s.selected.isEditable)
            vm.onEvent(CalendarEvent.ClearSelection)
            var c = awaitItem()
            while (c.selected != null) c = awaitItem()
            assertNull(c.selected)
        }
    }

    @Test fun selectFutureDayIsIgnored() = runTest {
        val vm = vm()
        vm.uiState.test {
            loaded(YearMonth(2026, 10))
            vm.onEvent(CalendarEvent.SelectDay(date("2026-10-19")))
            expectNoEvents()
            assertNull(vm.uiState.value.selected)
        }
    }

    @Test fun toggleOldDayEmitsEditWindowClosed() = runTest {
        store.habits.value = listOf(habit("a", created = "2026-10-01"))
        val vm = vm()
        vm.effects.test {
            vm.onEvent(CalendarEvent.ToggleHabit("a", date("2026-10-10")))
            assertEquals(CalendarEffect.ShowDomainError(DomainError.EditWindowClosed), awaitItem())
        }
    }

    @Test fun toggleUpdatesMonthStatus() = runTest {
        store.habits.value = listOf(habit("a", created = "2026-10-01"))
        val vm = vm()
        vm.uiState.test {
            loaded(YearMonth(2026, 10))
            vm.onEvent(CalendarEvent.ToggleHabit("a", date("2026-10-17")))
            var s = awaitItem()
            while (s.calendar?.days?.first { it.date == date("2026-10-17") }?.status !is DayStatus.Perfect) s = awaitItem()
            assertEquals(1, s.calendar.perfectDays)
        }
    }

    @Test fun unexpectedExceptionEmitsShowError() = runTest {
        store.habits.value = listOf(habit("a", created = "2026-10-01"))
        val vm = vm()
        vm.effects.test {
            store.failNextWrite = IllegalStateException("disk full")
            vm.onEvent(CalendarEvent.ToggleHabit("a", date("2026-10-17")))
            assertEquals(CalendarEffect.ShowError("disk full"), awaitItem())
        }
    }
}
