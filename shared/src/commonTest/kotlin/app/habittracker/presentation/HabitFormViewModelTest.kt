package app.habittracker.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.habittracker.HEALTH
import app.habittracker.MON_WED_FRI
import app.habittracker.date
import app.habittracker.domain.model.Category
import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.IconKeys
import app.habittracker.domain.usecase.CreateHabit
import app.habittracker.domain.usecase.GetHabit
import app.habittracker.domain.usecase.ObserveCategories
import app.habittracker.domain.usecase.UpdateHabit
import app.habittracker.fakes.FakeAppClock
import app.habittracker.fakes.FakeCategoryRepository
import app.habittracker.fakes.FakeHabitRepository
import app.habittracker.fakes.FakeStore
import app.habittracker.habit
import app.habittracker.presentation.form.HabitFormEffect
import app.habittracker.presentation.form.HabitFormEvent
import app.habittracker.presentation.form.HabitFormUiState
import app.habittracker.presentation.form.HabitFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HabitFormViewModelTest {
    private val store = FakeStore().apply { categories.value = listOf(HEALTH) }
    private val habits = FakeHabitRepository(store)
    private val categories = FakeCategoryRepository(store)
    private val clock = FakeAppClock(date("2026-10-18"))

    private fun vm(id: String? = null) = HabitFormViewModel(
        id,
        GetHabit(habits),
        ObserveCategories(categories),
        CreateHabit(habits, categories, clock),
        UpdateHabit(habits, categories, clock),
    )

    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private suspend fun ReceiveTurbine<HabitFormUiState>.awaitLoaded(): HabitFormUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    @Test fun newHabitDefaults() = runTest {
        vm().uiState.test {
            assertTrue(awaitItem().isLoading)
            val s = awaitLoaded()
            assertEquals("", s.name)
            assertEquals(HEALTH.id, s.categoryId)
            assertEquals("checklist", s.iconKey)
            assertEquals(DayOfWeek.entries.toSet(), s.days)
            assertEquals(IconKeys.all, s.iconKeys)
            assertEquals(listOf(HEALTH), s.categories)
            assertFalse(s.isEdit)
            assertFalse(s.canSave)
            assertNull(s.nameError)
            assertFalse(s.showScheduleNote)
        }
    }

    @Test fun editLoadsExisting() = runTest {
        store.habits.value = listOf(habit("a", name = "Run", iconKey = "run", days = MON_WED_FRI))
        vm("a").uiState.test {
            val s = awaitLoaded()
            assertTrue(s.isEdit)
            assertEquals("Run", s.name)
            assertEquals("run", s.iconKey)
            assertEquals(HEALTH.id, s.categoryId)
            assertEquals(MON_WED_FRI, s.days)
            assertTrue(s.canSave)
            assertFalse(s.showScheduleNote)
        }
    }

    @Test fun editResolvesRemovedIconKeyToFallback() = runTest {
        store.habits.value = listOf(habit("a", name = "Run", iconKey = "removed-icon", days = MON_WED_FRI))
        vm("a").uiState.test {
            val s = awaitLoaded()
            assertEquals(IconKeys.FALLBACK, s.iconKey)
            assertTrue(s.canSave)
        }
    }

    @Test fun missingHabitEmitsNotFoundThenDismiss() = runTest {
        vm("nope").effects.test {
            assertEquals(HabitFormEffect.ShowDomainError(DomainError.HabitNotFound), awaitItem())
            assertEquals(HabitFormEffect.Dismiss, awaitItem())
        }
    }

    @Test fun blankNameCannotSave() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(HabitFormEvent.NameChanged("   "))
            // Blank edits do not count as touched: no error yet, and no state change at all.
            vm.onEvent(HabitFormEvent.Save)
            val s = awaitItem()
            assertFalse(s.canSave)
            assertEquals(DomainError.InvalidName, s.nameError)
            vm.onEvent(HabitFormEvent.NameChanged("Read"))
            val ok = awaitItem()
            assertTrue(ok.canSave)
            assertNull(ok.nameError)
            vm.onEvent(HabitFormEvent.NameChanged(""))
            assertEquals(DomainError.InvalidName, awaitItem().nameError)
        }
        assertTrue(store.habits.value.isEmpty())
    }

    @Test fun noDaysCannotSave() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(HabitFormEvent.NameChanged("Read"))
            assertTrue(awaitItem().canSave)
            DayOfWeek.entries.forEach { vm.onEvent(HabitFormEvent.DayToggled(it)) }
            var s = awaitItem()
            while (s.days.isNotEmpty()) s = awaitItem()
            assertFalse(s.canSave)
            vm.onEvent(HabitFormEvent.EveryDayToggled)
            assertEquals(DayOfWeek.entries.toSet(), awaitItem().days)
            vm.onEvent(HabitFormEvent.EveryDayToggled)
            assertEquals(setOf(DayOfWeek.MONDAY), awaitItem().days)
        }
    }

    @Test fun scheduleNoteOnlyWhenDaysChangedInEdit() = runTest {
        store.habits.value = listOf(habit("a", days = MON_WED_FRI))
        val vm = vm("a")
        vm.uiState.test {
            assertFalse(awaitLoaded().showScheduleNote)
            vm.onEvent(HabitFormEvent.DayToggled(DayOfWeek.TUESDAY))
            assertTrue(awaitItem().showScheduleNote)
            vm.onEvent(HabitFormEvent.DayToggled(DayOfWeek.TUESDAY))
            assertFalse(awaitItem().showScheduleNote)
        }
        val newVm = vm()
        newVm.uiState.test {
            awaitLoaded()
            newVm.onEvent(HabitFormEvent.DayToggled(DayOfWeek.TUESDAY))
            assertFalse(awaitItem().showScheduleNote)
        }
    }

    @Test fun selectedCategorySurvivesAndFallsBackWhenRemoved() = runTest {
        val other = Category("c-other", "Other", "blue", 1)
        store.categories.value = listOf(HEALTH, other)
        val vm = vm()
        vm.uiState.test {
            assertEquals(HEALTH.id, awaitLoaded().categoryId)
            vm.onEvent(HabitFormEvent.CategorySelected(other.id))
            assertEquals(other.id, awaitItem().categoryId)
            store.categories.value = listOf(HEALTH)
            var s = awaitItem()
            while (s.categories.size != 1) s = awaitItem()
            assertEquals(HEALTH.id, s.categoryId)
        }
    }

    @Test fun saveCreateDismisses() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(HabitFormEvent.NameChanged("  Read  "))
            awaitItem()
            vm.onEvent(HabitFormEvent.IconSelected("book"))
            awaitItem()
            vm.onEvent(HabitFormEvent.Save)
            assertTrue(awaitItem().isSaving)
            cancelAndIgnoreRemainingEvents()
        }
        vm.effects.test { assertEquals(HabitFormEffect.Dismiss, awaitItem()) }
        val created = store.habits.value.single()
        assertEquals("Read", created.name)
        assertEquals("book", created.iconKey)
        assertEquals(DayOfWeek.entries.toSet(), created.currentSchedule.days)
    }

    @Test fun saveUpdateDismisses() = runTest {
        store.habits.value = listOf(habit("a", name = "Run", days = MON_WED_FRI))
        val vm = vm("a")
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(HabitFormEvent.NameChanged("Jog"))
            awaitItem()
            vm.onEvent(HabitFormEvent.Save)
            assertTrue(awaitItem().isSaving)
            cancelAndIgnoreRemainingEvents()
        }
        vm.effects.test { assertEquals(HabitFormEffect.Dismiss, awaitItem()) }
        assertEquals("Jog", store.habits.value.single().name)
    }

    @Test fun doubleTapSaveCreatesOnce() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(HabitFormEvent.NameChanged("Read"))
            awaitItem()
            vm.onEvent(HabitFormEvent.Save)
            vm.onEvent(HabitFormEvent.Save)
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()
        assertEquals(1, store.habits.value.size)
    }

    @Test fun saveFailureKeepsFormAndShowsError() = runTest {
        store.habits.value = listOf(habit("a", name = "Run"))
        val vm = vm("a")
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(HabitFormEvent.NameChanged("Jog"))
            awaitItem()
            store.habits.value = emptyList() // deleted behind the form's back
            vm.onEvent(HabitFormEvent.Save)
            advanceUntilIdle() // saving is set and reset within one idle pass, so the state flow emits nothing new
            val s = vm.uiState.value
            assertFalse(s.isSaving)
            assertEquals("Jog", s.name)
            assertTrue(s.canSave)
            vm.effects.test {
                assertEquals(HabitFormEffect.ShowDomainError(DomainError.HabitNotFound), awaitItem())
            }
        }
    }

    @Test fun unexpectedExceptionEmitsShowErrorAndResetsSaving() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(HabitFormEvent.NameChanged("Read"))
            awaitItem()
            store.failNextWrite = IllegalStateException("disk full")
            vm.onEvent(HabitFormEvent.Save)
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isSaving)
            vm.effects.test { assertEquals(HabitFormEffect.ShowError("disk full"), awaitItem()) }
        }
    }

    @Test fun newHabitReminderOffAtNine() = runTest {
        vm().uiState.test {
            val s = awaitLoaded()
            assertEquals(false, s.reminderEnabled)
            assertEquals(kotlinx.datetime.LocalTime(9, 0), s.reminderTime)
        }
    }

    private suspend fun saveNew(vm: HabitFormViewModel, events: List<HabitFormEvent>) {
        vm.uiState.test {
            awaitLoaded()
            vm.onEvent(HabitFormEvent.NameChanged("Read")); awaitItem()
            events.forEach { vm.onEvent(it); awaitItem() }
            vm.onEvent(HabitFormEvent.Save)
            cancelAndIgnoreRemainingEvents()
        }
        vm.effects.test { assertEquals(HabitFormEffect.Dismiss, awaitItem()) }
    }

    @Test fun toggleAndTimeSavedOnCreate() = runTest {
        saveNew(vm(), listOf(HabitFormEvent.ReminderToggled, HabitFormEvent.ReminderTimeChanged(kotlinx.datetime.LocalTime(21, 30))))
        assertEquals(kotlinx.datetime.LocalTime(21, 30), store.habits.value.single().reminderTime)
    }

    @Test fun toggleOffSavesNull() = runTest {
        saveNew(vm(), listOf(
            HabitFormEvent.ReminderToggled,
            HabitFormEvent.ReminderTimeChanged(kotlinx.datetime.LocalTime(7, 0)),
            HabitFormEvent.ReminderToggled,
        ))
        assertEquals(null, store.habits.value.single().reminderTime)
    }

    @Test fun editLoadsExistingReminder() = runTest {
        store.habits.value = listOf(habit("a", name = "Run", reminder = "06:45"))
        vm("a").uiState.test {
            val s = awaitLoaded()
            assertEquals(true, s.reminderEnabled)
            assertEquals(kotlinx.datetime.LocalTime(6, 45), s.reminderTime)
        }
    }
}
