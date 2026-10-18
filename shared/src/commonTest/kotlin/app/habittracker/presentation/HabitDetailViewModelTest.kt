package app.habittracker.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.habittracker.HEALTH
import app.habittracker.date
import app.habittracker.domain.usecase.ArchiveHabit
import app.habittracker.domain.usecase.DeleteHabit
import app.habittracker.domain.usecase.ObserveHabitDetail
import app.habittracker.domain.usecase.UnarchiveHabit
import app.habittracker.fakes.FakeAppClock
import app.habittracker.fakes.FakeCompletionRepository
import app.habittracker.fakes.FakeHabitRepository
import app.habittracker.fakes.FakeStore
import app.habittracker.habit
import app.habittracker.presentation.detail.HabitDetailEffect
import app.habittracker.presentation.detail.HabitDetailEvent
import app.habittracker.presentation.detail.HabitDetailUiState
import app.habittracker.presentation.detail.HabitDetailViewModel
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModelTest {
    private val store = FakeStore().apply {
        categories.value = listOf(HEALTH)
        habits.value = listOf(habit("a", created = "2026-10-01"))
    }
    private val habits = FakeHabitRepository(store)
    private val completions = FakeCompletionRepository(store)
    private val clock = FakeAppClock(date("2026-10-18"))

    private fun vm() = HabitDetailViewModel(
        "a",
        ObserveHabitDetail(habits, completions, clock),
        ArchiveHabit(habits, clock),
        UnarchiveHabit(habits, clock),
        DeleteHabit(habits),
        clock,
    )

    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private suspend fun ReceiveTurbine<HabitDetailUiState>.content(
        month: YearMonth,
        until: (HabitDetailUiState.Content) -> Boolean = { true },
    ): HabitDetailUiState.Content {
        while (true) {
            val s = awaitItem()
            if (s is HabitDetailUiState.Content && s.detail.month == month && until(s)) return s
        }
    }

    @Test fun showsStatsAndMonth() = runTest {
        vm().uiState.test {
            assertEquals(HabitDetailUiState.Loading, awaitItem())
            val s = content(YearMonth(2026, 10))
            assertEquals("a", s.detail.habit.id)
            assertEquals(31, s.detail.days.size)
            assertEquals(3, s.leadingBlanks) // 1 Oct 2026 is a Thursday
            assertEquals(false, s.canGoNext)
            assertEquals(false, s.confirmingDelete)
        }
    }

    @Test fun archiveToggleFlipsState() = runTest {
        val vm = vm()
        vm.uiState.test {
            content(YearMonth(2026, 10))
            vm.onEvent(HabitDetailEvent.ArchiveToggled)
            assertEquals(true, content(YearMonth(2026, 10)) { it.detail.habit.isArchived }.detail.habit.isArchived)
            vm.onEvent(HabitDetailEvent.ArchiveToggled)
            assertEquals(false, content(YearMonth(2026, 10)) { !it.detail.habit.isArchived }.detail.habit.isArchived)
        }
    }

    @Test fun deleteRequiresConfirmationThenCloses() = runTest {
        val vm = vm()
        vm.uiState.test {
            content(YearMonth(2026, 10))
            vm.onEvent(HabitDetailEvent.DeleteClicked)
            content(YearMonth(2026, 10)) { it.confirmingDelete }
            assertEquals(1, store.habits.value.size)
            vm.effects.test {
                vm.onEvent(HabitDetailEvent.DeleteConfirmed)
                assertEquals(HabitDetailEffect.Close, awaitItem())
            }
            var s = awaitItem()
            while (s !is HabitDetailUiState.Deleted) s = awaitItem()
            assertEquals(0, store.habits.value.size)
        }
    }

    @Test fun cancelDeleteKeepsHabit() = runTest {
        val vm = vm()
        vm.uiState.test {
            content(YearMonth(2026, 10))
            vm.onEvent(HabitDetailEvent.DeleteClicked)
            content(YearMonth(2026, 10)) { it.confirmingDelete }
            vm.onEvent(HabitDetailEvent.DeleteCancelled)
            val s = content(YearMonth(2026, 10)) { !it.confirmingDelete }
            assertEquals(false, s.confirmingDelete)
            assertEquals(1, store.habits.value.size)
        }
    }

    @Test fun editClickedEmitsOpenEdit() = runTest {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(HabitDetailEvent.EditClicked)
            assertEquals(HabitDetailEffect.OpenEdit("a"), awaitItem())
        }
    }

    @Test fun monthNavigationBounded() = runTest {
        val vm = vm()
        vm.uiState.test {
            content(YearMonth(2026, 10))
            vm.onEvent(HabitDetailEvent.NextMonth)
            expectNoEvents()
            vm.onEvent(HabitDetailEvent.PrevMonth)
            val sep = content(YearMonth(2026, 9))
            assertEquals(1, sep.leadingBlanks) // 1 Sep 2026 is a Tuesday
            assertEquals(30, sep.detail.days.size)
            assertEquals(true, sep.canGoNext)
            vm.onEvent(HabitDetailEvent.NextMonth)
            assertEquals(false, content(YearMonth(2026, 10)).canGoNext)
        }
    }

    @Test fun deletedExternallyShowsDeleted() = runTest {
        vm().uiState.test {
            content(YearMonth(2026, 10))
            store.habits.value = emptyList()
            var s = awaitItem()
            while (s !is HabitDetailUiState.Deleted) s = awaitItem()
            assertIs<HabitDetailUiState.Deleted>(s)
        }
    }

    @Test fun unexpectedExceptionEmitsShowError() = runTest {
        val vm = vm()
        vm.uiState.test {
            content(YearMonth(2026, 10))
            vm.effects.test {
                store.failNextWrite = IllegalStateException("disk full")
                vm.onEvent(HabitDetailEvent.ArchiveToggled)
                assertEquals(HabitDetailEffect.ShowError("disk full"), awaitItem())
            }
        }
    }
}
