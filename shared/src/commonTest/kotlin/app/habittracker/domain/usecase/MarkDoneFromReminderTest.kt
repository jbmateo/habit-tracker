package app.habittracker.domain.usecase

import app.habittracker.HEALTH
import app.habittracker.date
import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import app.habittracker.fakes.FakeAppClock
import app.habittracker.fakes.FakeCompletionRepository
import app.habittracker.fakes.FakeHabitRepository
import app.habittracker.fakes.FakeStore
import app.habittracker.habit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkDoneFromReminderTest {
    private val store = FakeStore().apply {
        categories.value = listOf(HEALTH)
        habits.value = listOf(habit("a", created = "2026-10-01"))
    }
    private val habits = FakeHabitRepository(store)
    private val completions = FakeCompletionRepository(store)
    private val clock = FakeAppClock(date("2026-10-14"))
    private val markDone = MarkDoneFromReminder(habits, completions, ToggleCompletion(habits, completions, clock))

    private suspend fun doneDates() = completions.observeForHabit("a").first()

    @Test fun markDoneMarksUndoneHabit() = runTest {
        assertEquals(Outcome.Success(Unit), markDone("a", date("2026-10-14")))
        assertTrue(date("2026-10-14") in doneDates())
    }

    @Test fun markDoneOnAlreadyDoneHabitKeepsItDone() = runTest {
        completions.toggle("a", date("2026-10-14"))
        assertEquals(Outcome.Success(Unit), markDone("a", date("2026-10-14")))
        assertTrue(date("2026-10-14") in doneDates())
    }

    @Test fun markDoneOutsideWindowFails() = runTest {
        assertEquals(Outcome.Failure(DomainError.EditWindowClosed), markDone("a", date("2026-10-11")))
    }

    @Test fun markDoneDeletedHabitFails() = runTest {
        habits.delete("a")
        assertEquals(Outcome.Failure(DomainError.HabitNotFound), markDone("a", date("2026-10-14")))
    }
}
