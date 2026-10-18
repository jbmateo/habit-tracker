package app.habittracker.domain.usecase

import app.habittracker.HEALTH
import app.habittracker.MON_WED_FRI
import app.habittracker.date
import app.habittracker.domain.model.ArchivePeriod
import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.model.ScheduleVersion
import app.habittracker.fakes.FakeAppClock
import app.habittracker.fakes.FakeCategoryRepository
import app.habittracker.fakes.FakeCompletionRepository
import app.habittracker.fakes.FakeHabitRepository
import app.habittracker.fakes.FakeStore
import app.habittracker.habit
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandUseCasesTest {
    // 2026-01-14 is a Wednesday.
    private val store = FakeStore().apply { categories.value = listOf(HEALTH) }
    private val habits = FakeHabitRepository(store)
    private val categories = FakeCategoryRepository(store)
    private val completions = FakeCompletionRepository(store)
    private val clock = FakeAppClock(date("2026-01-14"))

    private val toggle = ToggleCompletion(habits, completions, clock)
    private val createHabit = CreateHabit(habits, categories, clock)
    private val updateHabit = UpdateHabit(habits, categories, clock)
    private val archive = ArchiveHabit(habits, clock)
    private val unarchive = UnarchiveHabit(habits, clock)
    private val createCategory = CreateCategory(categories)
    private val updateCategory = UpdateCategory(categories)
    private val deleteCategory = DeleteCategory(categories)
    private val seed = SeedDefaultCategoriesIfEmpty(categories)

    private fun fail(error: DomainError) = Outcome.Failure(error)
    private fun addHabit(h: app.habittracker.domain.model.Habit = habit()) { store.habits.value += h }

    // Toggle

    @Test fun toggleToday() = runTest {
        addHabit()
        assertEquals(Outcome.Success(true), toggle("h1", date("2026-01-14")))
    }

    @Test fun toggleYesterdayAllowed() = runTest {
        addHabit()
        assertEquals(Outcome.Success(true), toggle("h1", date("2026-01-13")))
    }

    @Test fun toggleTwoDaysAgoEditWindowClosed() = runTest {
        addHabit()
        assertEquals(fail(DomainError.EditWindowClosed), toggle("h1", date("2026-01-12")))
    }

    @Test fun toggleNonDueDayNotDue() = runTest {
        addHabit(habit(days = MON_WED_FRI))
        // Tuesday the 13th is yesterday and not scheduled.
        assertEquals(fail(DomainError.NotDue), toggle("h1", date("2026-01-13")))
    }

    @Test fun toggleUnknownHabit() = runTest {
        assertEquals(fail(DomainError.HabitNotFound), toggle("nope", date("2026-01-14")))
    }

    @Test fun doubleToggleEndsNotDone() = runTest {
        addHabit()
        toggle("h1", date("2026-01-14"))
        assertEquals(Outcome.Success(false), toggle("h1", date("2026-01-14")))
    }

    @Test fun toggleYesterdayAfterTodayScheduleChange() = runTest {
                clock.setToday(date("2026-01-13")) // Tuesday
        addHabit(habit(days = setOf(DayOfWeek.MONDAY), created = "2026-01-05"))
        assertEquals(Outcome.Success(Unit), updateHabit("h1", "h1", "run", HEALTH.id, setOf(DayOfWeek.TUESDAY)))
        // Yesterday (Mon) still follows the old schedule.
        assertEquals(Outcome.Success(true), toggle("h1", date("2026-01-12")))
        // Today (Tue) follows the new one.
        assertEquals(Outcome.Success(true), toggle("h1", date("2026-01-13")))
    }

    // Create / update

    @Test fun createValidatesNameDaysIconCategory() = runTest {
        val days = setOf(DayOfWeek.MONDAY)
        assertEquals(fail(DomainError.InvalidName), createHabit("  ", "run", HEALTH.id, days))
        assertEquals(fail(DomainError.NoDaysSelected), createHabit("Run", "run", HEALTH.id, emptySet()))
        assertEquals(fail(DomainError.UnknownIcon), createHabit("Run", "bogus", HEALTH.id, days))
        assertEquals(fail(DomainError.CategoryNotFound), createHabit("Run", "run", "missing", days))
        assertTrue(store.habits.value.isEmpty())
    }

    @Test fun createSetsCreatedOnToday() = runTest {
        val id = (createHabit(" Run ", "run", HEALTH.id, MON_WED_FRI) as Outcome.Success).value
        val h = habits.get(id)!!
        assertEquals(date("2026-01-14"), h.createdOn)
        assertEquals("Run", h.name)
        assertEquals(MON_WED_FRI, h.currentSchedule.days)
    }

    @Test fun updateSameDaysNoNewScheduleVersion() = runTest {
        addHabit(habit(days = MON_WED_FRI))
        updateHabit("h1", "Renamed", "gym", HEALTH.id, MON_WED_FRI)
        val h = habits.get("h1")!!
        assertEquals(1, h.schedules.size)
        assertEquals("Renamed", h.name)
        assertEquals("gym", h.iconKey)
    }

    @Test fun updateChangedDaysAddsVersionFromToday() = runTest {
        addHabit(habit(days = MON_WED_FRI))
        updateHabit("h1", "h1", "run", HEALTH.id, setOf(DayOfWeek.TUESDAY))
        val h = habits.get("h1")!!
        assertEquals(2, h.schedules.size)
        assertEquals(ScheduleVersion(date("2026-01-14"), setOf(DayOfWeek.TUESDAY)), h.currentSchedule)
    }

    @Test fun updateTwiceSameDayReplacesVersion() = runTest {
        addHabit(habit(days = MON_WED_FRI))
        updateHabit("h1", "h1", "run", HEALTH.id, setOf(DayOfWeek.TUESDAY))
        updateHabit("h1", "h1", "run", HEALTH.id, setOf(DayOfWeek.THURSDAY))
        val h = habits.get("h1")!!
        assertEquals(2, h.schedules.size)
        assertEquals(setOf(DayOfWeek.THURSDAY), h.currentSchedule.days)
    }

    // Archive / unarchive

    @Test fun archiveThenUnarchiveNextDayLeavesClosedPeriod() = runTest {
        addHabit()
        archive("h1")
        clock.setToday(date("2026-01-15"))
        unarchive("h1")
        assertEquals(
            listOf(ArchivePeriod(date("2026-01-14"), date("2026-01-15"))),
            habits.get("h1")!!.archivePeriods,
        )
    }

    @Test fun archiveAndUnarchiveSameDayLeavesNoPeriod() = runTest {
        addHabit()
        archive("h1")
        unarchive("h1")
        assertTrue(habits.get("h1")!!.archivePeriods.isEmpty())
    }

    @Test fun archiveTwiceIsNoOp() = runTest {
        addHabit()
        archive("h1")
        clock.setToday(date("2026-01-15"))
        assertEquals(Outcome.Success(Unit), archive("h1"))
        assertEquals(listOf(ArchivePeriod(date("2026-01-14"), null)), habits.get("h1")!!.archivePeriods)
    }

    // Categories

    @Test fun deleteCategoryWithOnlyArchivedHabitFails() = runTest {
        addHabit(habit(archived = listOf(ArchivePeriod(date("2026-01-10"), null))))
        assertEquals(fail(DomainError.CategoryNotEmpty), deleteCategory(HEALTH.id))
        assertEquals(listOf(HEALTH), store.categories.value)
    }

    @Test fun deleteEmptyCategory() = runTest {
        assertEquals(Outcome.Success(Unit), deleteCategory(HEALTH.id))
        assertTrue(store.categories.value.isEmpty())
    }

    @Test fun categoryUnknownColor() = runTest {
        assertEquals(fail(DomainError.UnknownColor), createCategory("Sleep", "neon"))
        assertEquals(fail(DomainError.UnknownColor), updateCategory(HEALTH.id, "Sleep", "neon"))
        assertEquals(fail(DomainError.InvalidName), createCategory("", "green"))
    }

    // Seed

    @Test fun seedOnlyWhenEmpty() = runTest {
        store.categories.value = emptyList()
        seed()
        val expected = listOf(
            "Health" to "green", "Fitness" to "orange", "Learning" to "blue",
            "Mindfulness" to "purple", "Productivity" to "teal",
        )
        assertEquals(expected, store.categories.value.sortedBy { it.sortOrder }
            .map { it.name to it.colorKey })
        seed()
        assertEquals(5, store.categories.value.size)
    }

    @Test fun createAndUpdateCarryReminderTime() = runTest {
        val id = (createHabit("Read", "book", HEALTH.id, MON_WED_FRI, reminderTime = kotlinx.datetime.LocalTime(21, 0)) as Outcome.Success).value
        assertEquals(kotlinx.datetime.LocalTime(21, 0), habits.get(id)!!.reminderTime)
        updateHabit(id, "Read", "book", HEALTH.id, MON_WED_FRI, reminderTime = kotlinx.datetime.LocalTime(6, 45))
        assertEquals(kotlinx.datetime.LocalTime(6, 45), habits.get(id)!!.reminderTime)
    }
}
