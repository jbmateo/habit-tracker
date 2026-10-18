package app.habittracker.domain.reminders

import app.habittracker.HEALTH
import app.habittracker.date
import app.habittracker.domain.model.ArchivePeriod
import app.habittracker.fakes.FakeAppClock
import app.habittracker.fakes.FakeCompletionRepository
import app.habittracker.fakes.FakeHabitRepository
import app.habittracker.fakes.FakeReminderScheduler
import app.habittracker.fakes.FakeStore
import app.habittracker.habit
import app.habittracker.domain.model.Habit
import app.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSyncTest {
    private val store = FakeStore().apply {
        categories.value = listOf(HEALTH)
        habits.value = listOf(habit("a", name = "Run", created = "2026-10-01", reminder = "09:00"))
    }
    private val habits = FakeHabitRepository(store)
    private val completions = FakeCompletionRepository(store)
    private val clock = FakeAppClock(date("2026-10-14"))
    private var now = LocalDateTime(2026, 10, 14, 8, 0)
    private val scheduler = FakeReminderScheduler()
    private val sync = ReminderSync(habits, completions, clock, scheduler) { now }

    private fun TestScope.started() { sync.start(backgroundScope); runCurrent() }
    private fun ids() = scheduler.scheduled.map { it.id }

    @Test fun initialPlanIsScheduled() = runTest {
        started()
        assertEquals(7, scheduler.scheduled.size)
        assertEquals("habit:a@2026-10-14", ids().first())
    }

    @Test fun checkingOffRemovesTodaysReminder() = runTest {
        started()
        completions.toggle("a", date("2026-10-14")); runCurrent()
        assertTrue("habit:a@2026-10-14" !in ids())
    }

    @Test fun uncheckingRestoresIt() = runTest {
        started()
        completions.toggle("a", date("2026-10-14")); runCurrent()
        completions.toggle("a", date("2026-10-14")); runCurrent()
        assertTrue("habit:a@2026-10-14" in ids())
    }

    @Test fun editingTimeReplans() = runTest {
        started()
        habits.update("a", "Run", "run", HEALTH.id, schedule = null, reminderTime = LocalTime(20, 15)); runCurrent()
        assertTrue(scheduler.scheduled.all { it.time == LocalTime(20, 15) })
    }

    @Test fun dayChangeMovesWindow() = runTest {
        started()
        now = LocalDateTime(2026, 10, 15, 0, 1)
        clock.setToday(date("2026-10-15")); runCurrent()
        assertEquals("habit:a@2026-10-15", ids().first())
        assertEquals("habit:a@2026-10-21", ids().last())
    }

    @Test fun deletingHabitRemovesItsReminders() = runTest {
        started()
        habits.delete("a"); runCurrent()
        assertEquals(emptyList(), scheduler.scheduled)
    }

    @Test fun unarchiveRestoresReminders() = runTest {
        started()
        habits.openArchivePeriod("a", date("2026-10-14")); runCurrent()
        assertEquals(emptyList(), scheduler.scheduled)
        habits.deleteArchivePeriod("a", date("2026-10-14")); runCurrent()
        assertEquals(7, scheduler.scheduled.size)
    }

    @Test fun schedulerFailureIsSwallowedAndRetried() = runTest {
        scheduler.failNext = IllegalStateException("notifications off")
        started()
        assertEquals(emptyList(), scheduler.scheduled)
        completions.toggle("a", date("2026-10-15")); runCurrent()
        assertEquals(2, scheduler.calls)
        assertEquals(6, scheduler.scheduled.size)
    }

    @Test fun syncNowPlansOnce() = runTest {
        sync.syncNow()
        assertEquals(7, scheduler.scheduled.size)
    }

    @Test fun deliveredNotificationsOfSettledRemindersAreCleared() = runTest {
        started()
        assertEquals(setOf("habit:a@2026-10-13", "habit:a@2026-10-14"), scheduler.keptDelivered)
        completions.toggle("a", date("2026-10-14")); runCurrent()
        assertEquals(setOf("habit:a@2026-10-13"), scheduler.keptDelivered)
    }

    @Test fun concurrentSyncsNeverOverlap() = runTest {
        val slow = object : ReminderScheduler {
            var inFlight = 0
            var maxInFlight = 0
            override suspend fun replaceAll(reminders: List<PlannedReminder>) {
                inFlight++
                maxInFlight = maxOf(maxInFlight, inFlight)
                delay(100)
                inFlight--
            }
            override suspend fun clearDelivered(keep: Set<String>) = Unit
        }
        val sync = ReminderSync(habits, completions, clock, slow) { now }
        launch { sync.syncNow() }
        launch { sync.syncNow() }
        advanceUntilIdle()
        assertEquals(1, slow.maxInFlight)
    }

    @Test fun readFailureIsLoggedNotThrown() = runTest {
        val logged = mutableListOf<String>()
        val broken = object : HabitRepository by habits {
            override fun observeAll(): Flow<List<Habit>> = flow { throw IllegalStateException("db closed") }
        }
        ReminderSync(broken, completions, clock, scheduler, log = { logged += it }) { now }.syncNow()
        assertTrue(logged.single().contains("db closed"))
    }
}
