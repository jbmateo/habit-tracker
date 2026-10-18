package app.habittracker.presentation

import app.cash.turbine.test
import app.habittracker.HEALTH
import app.habittracker.date
import app.habittracker.domain.model.ArchivePeriod
import app.habittracker.domain.usecase.ObserveHabits
import app.habittracker.fakes.FakeHabitRepository
import app.habittracker.fakes.FakeStore
import app.habittracker.habit
import app.habittracker.presentation.habits.HabitListEffect
import app.habittracker.presentation.habits.HabitListEvent
import app.habittracker.presentation.habits.HabitListUiState
import app.habittracker.presentation.habits.HabitListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class HabitListViewModelTest {
    private val store = FakeStore().apply { categories.value = listOf(HEALTH) }
    private val habits = FakeHabitRepository(store)

    private fun vm() = HabitListViewModel(ObserveHabits(habits))

    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test fun listGroupsActiveAndArchived() = runTest {
        store.habits.value = listOf(
            habit("a"),
            habit("b", archived = listOf(ArchivePeriod(date("2026-01-10"), null))),
        )
        vm().uiState.test {
            assertEquals(HabitListUiState.Loading, awaitItem())
            val overview = assertIs<HabitListUiState.Content>(awaitItem()).overview
            assertEquals(listOf("a"), overview.active.single().items.map { it.id })
            assertEquals(HEALTH, overview.active.single().category)
            assertEquals(listOf("b"), overview.archived.map { it.id })
        }
    }

    @Test fun eventsEmitNavigationEffects() = runTest {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(HabitListEvent.HabitClicked("a"))
            vm.onEvent(HabitListEvent.AddHabitClicked)
            vm.onEvent(HabitListEvent.ManageCategoriesClicked)
            assertEquals(HabitListEffect.OpenHabit("a"), awaitItem())
            assertEquals(HabitListEffect.OpenCreateHabit, awaitItem())
            assertEquals(HabitListEffect.OpenCategories, awaitItem())
        }
    }
}
