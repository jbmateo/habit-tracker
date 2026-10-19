package app.habittracker.presentation

import app.cash.turbine.test
import app.habittracker.HEALTH
import app.habittracker.date
import app.habittracker.domain.model.Completion
import app.habittracker.domain.model.MilestoneSeen
import app.habittracker.domain.usecase.CelebrationMemory
import app.habittracker.domain.usecase.ObserveTodayDashboard
import app.habittracker.domain.usecase.ToggleCompletion
import app.habittracker.fakes.FakeAppClock
import app.habittracker.fakes.FakeCompletionRepository
import app.habittracker.fakes.FakeHabitRepository
import app.habittracker.fakes.FakeStore
import app.habittracker.habit
import app.habittracker.presentation.dashboard.DashboardEffect
import app.habittracker.presentation.dashboard.DashboardEvent
import app.habittracker.presentation.dashboard.DashboardUiState
import app.habittracker.presentation.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import app.habittracker.fakes.FakePreferencesRepository
import app.habittracker.domain.usecase.ObserveTodaySort
import app.habittracker.domain.usecase.SetTodaySort
import app.habittracker.domain.model.TodaySort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val store = FakeStore().apply { categories.value = listOf(HEALTH) }
    private val habits = FakeHabitRepository(store)
    private val completions = FakeCompletionRepository(store)
    private val clock = FakeAppClock(date("2026-01-14"))
    private val prefs = FakePreferencesRepository()

    private fun vm() = DashboardViewModel(
        ObserveTodayDashboard(habits, completions, clock),
        ToggleCompletion(habits, completions, clock),
        ObserveTodaySort(prefs),
        SetTodaySort(prefs),
        CelebrationMemory(prefs),
        clock,
    )

    /** Perfect days for habit "a" on the [n] days ending yesterday (today is 2026-01-14). */
    private fun perfectDaysEndingYesterday(n: Int) {
        store.completions.value = (1..n).map { Completion("a", date("2026-01-14").minus(DatePeriod(days = it))) }
    }

    private fun milestone(s: DashboardUiState) = assertIs<DashboardUiState.Content>(s).milestone

    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun content(s: DashboardUiState) = assertIs<DashboardUiState.Content>(s).dashboard

    @Test fun loadingThenContent() = runTest {
        store.habits.value = listOf(habit("a"))
        vm().uiState.test {
            assertEquals(DashboardUiState.Loading, awaitItem())
            val d = content(awaitItem())
            assertEquals(1, d.due)
            assertEquals(0, d.done)
        }
    }

    @Test fun noHabitsState() = runTest {
        vm().uiState.test {
            assertEquals(DashboardUiState.Loading, awaitItem())
            assertEquals(DashboardUiState.NoHabits, awaitItem())
        }
    }

    @Test fun toggleUpdatesCounts() = runTest {
        store.habits.value = listOf(habit("a"), habit("b"))
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            assertEquals(0, content(awaitItem()).done)
            vm.onEvent(DashboardEvent.ToggleHabit("a"))
            assertEquals(1, content(awaitItem()).done)
        }
    }

    @Test fun celebrateOnceWhenDayBecomesPerfect() = runTest {
        store.habits.value = listOf(habit("a"))
        val vm = vm()
        val effects = mutableListOf<DashboardEffect>()
        backgroundScope.launch { vm.effects.toList(effects) }
        vm.uiState.test {
            awaitItem(); awaitItem()
            vm.onEvent(DashboardEvent.ToggleHabit("a"))
            assertEquals(true, content(awaitItem()).isPerfect)
            vm.onEvent(DashboardEvent.ToggleHabit("a"))
            assertEquals(false, content(awaitItem()).isPerfect)
            vm.onEvent(DashboardEvent.ToggleHabit("a"))
            assertEquals(true, content(awaitItem()).isPerfect)
        }
        advanceUntilIdle()
        assertEquals(listOf<DashboardEffect>(DashboardEffect.Celebrate), effects)
    }

    @Test fun noCelebrateWhenAlreadyPerfectOnLoad() = runTest {
        store.habits.value = listOf(habit("a"))
        store.completions.value = listOf(Completion("a", date("2026-01-14")))
        val vm = vm()
        val effects = mutableListOf<DashboardEffect>()
        backgroundScope.launch { vm.effects.toList(effects) }
        vm.uiState.test {
            awaitItem()
            assertEquals(true, content(awaitItem()).isPerfect)
        }
        advanceUntilIdle()
        assertTrue(effects.isEmpty())
    }

    @Test fun rollsOverAtMidnight() = runTest {
        store.habits.value = listOf(habit("a"))
        store.completions.value = listOf(Completion("a", date("2026-01-14")))
        vm().uiState.test {
            awaitItem()
            assertEquals(1, content(awaitItem()).done)
            clock.setToday(date("2026-01-15"))
            val d = content(awaitItem())
            assertEquals(date("2026-01-15"), d.date)
            assertEquals(0, d.done)
        }
    }

    @Test fun navigationEventsEmitEffects() = runTest {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(DashboardEvent.AddHabitClicked)
            vm.onEvent(DashboardEvent.HabitClicked("a"))
            assertEquals(DashboardEffect.OpenCreateHabit, awaitItem())
            assertEquals(DashboardEffect.OpenHabit("a"), awaitItem())
        }
    }

    @Test fun domainFailureEmitsShowDomainError() = runTest {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(DashboardEvent.ToggleHabit("missing"))
            assertEquals(DashboardEffect.ShowDomainError(app.habittracker.domain.model.DomainError.HabitNotFound), awaitItem())
        }
    }

    @Test fun unexpectedExceptionEmitsShowError() = runTest {
        store.habits.value = listOf(habit("a"))
        val vm = vm()
        vm.effects.test {
            store.failNextWrite = IllegalStateException("disk full")
            vm.onEvent(DashboardEvent.ToggleHabit("a"))
            assertEquals(DashboardEffect.ShowError("disk full"), awaitItem())
        }
    }

    @Test fun itemsFollowRememberedSortAndSortSelectedPersists() = runTest {
        store.habits.value = listOf(habit("b", name = "Bike"), habit("a", name = "apple"))
        prefs.todaySort.value = TodaySort.Name
        val vm = vm()
        vm.uiState.test {
            awaitItem() // Loading
            var s = assertIs<DashboardUiState.Content>(awaitItem())
            assertEquals(TodaySort.Name, s.sort)
            assertEquals(listOf("apple", "Bike"), s.items.map { it.habit.name })
            vm.onEvent(DashboardEvent.SortSelected(TodaySort.Category))
            s = assertIs<DashboardUiState.Content>(awaitItem())
            assertEquals(TodaySort.Category, s.sort)
            assertEquals(s.dashboard.groups.flatMap { it.items }, s.items)
        }
        assertEquals(TodaySort.Category, prefs.todaySort.value)
    }

    @Test fun celebrateOncePerDayAcrossReopen() = runTest {
        store.habits.value = listOf(habit("a"))
        val first = vm()
        val firstEffects = mutableListOf<DashboardEffect>()
        backgroundScope.launch { first.effects.toList(firstEffects) }
        first.uiState.test {
            awaitItem(); awaitItem()
            first.onEvent(DashboardEvent.ToggleHabit("a"))
            assertEquals(true, content(awaitItem()).isPerfect)
        }
        advanceUntilIdle()
        assertEquals(listOf<DashboardEffect>(DashboardEffect.Celebrate), firstEffects)
        assertEquals(date("2026-01-14"), prefs.celebratedOn.value)

        val second = vm()
        val secondEffects = mutableListOf<DashboardEffect>()
        backgroundScope.launch { second.effects.toList(secondEffects) }
        second.uiState.test {
            awaitItem(); awaitItem()
            second.onEvent(DashboardEvent.ToggleHabit("a"))
            assertEquals(false, content(awaitItem()).isPerfect)
            second.onEvent(DashboardEvent.ToggleHabit("a"))
            assertEquals(true, content(awaitItem()).isPerfect)
        }
        advanceUntilIdle()
        assertTrue(secondEffects.isEmpty())
    }

    @Test fun celebrateFiresNextDayAfterCelebratingYesterday() = runTest {
        store.habits.value = listOf(habit("a"))
        prefs.celebratedOn.value = date("2026-01-13")
        val vm = vm()
        val effects = mutableListOf<DashboardEffect>()
        backgroundScope.launch { vm.effects.toList(effects) }
        vm.uiState.test {
            awaitItem(); awaitItem()
            vm.onEvent(DashboardEvent.ToggleHabit("a"))
            assertEquals(true, content(awaitItem()).isPerfect)
        }
        advanceUntilIdle()
        assertEquals(listOf<DashboardEffect>(DashboardEffect.Celebrate), effects)
    }

    @Test fun milestoneShownAtSevenDayStreak() = runTest {
        store.habits.value = listOf(habit("a", created = "2026-01-01"))
        perfectDaysEndingYesterday(7)
        vm().uiState.test {
            awaitItem()
            assertEquals(7, milestone(awaitItem()))
        }
    }

    @Test fun noMilestoneUnderSeven() = runTest {
        store.habits.value = listOf(habit("a", created = "2026-01-01"))
        perfectDaysEndingYesterday(6)
        vm().uiState.test {
            awaitItem()
            assertNull(milestone(awaitItem()))
        }
    }

    @Test fun dismissStoresTheShownMilestone() = runTest {
        store.habits.value = listOf(habit("a", created = "2025-11-01"))
        perfectDaysEndingYesterday(40)
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            assertEquals(30, milestone(awaitItem()))
            vm.onEvent(DashboardEvent.DismissMilestone)
            assertNull(milestone(awaitItem()))
        }
        assertEquals(MilestoneSeen(date("2025-12-05"), 30), prefs.milestoneSeen.value)
    }
}
