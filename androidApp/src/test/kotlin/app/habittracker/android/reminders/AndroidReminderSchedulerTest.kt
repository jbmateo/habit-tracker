package app.habittracker.android.reminders

import app.habittracker.domain.reminders.PlannedReminder
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidReminderSchedulerTest {
    private class FakeSink : AlarmSink {
        val set = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        override fun set(reminder: PlannedReminder) { set += reminder.id }
        override fun cancel(id: String) { cancelled += id }
    }

    private class MemoryStore(var map: Map<String, String>) : ScheduledReminderStore {
        override fun load() = map
        override fun save(scheduled: Map<String, String>) { map = scheduled }
    }

    private fun r(habit: String, day: Int, hour: Int = 9) =
        PlannedReminder(habit, habit, "run", LocalDate(2026, 10, day), LocalTime(hour, 0), streak = 0)

    @Test fun replaceAllSetsNewAndCancelsRemoved() = runTest {
        val sink = FakeSink()
        val store = MemoryStore(mapOf(r("a", 14).id to r("a", 14).fingerprint, r("b", 15).id to r("b", 15).fingerprint))
        AndroidReminderScheduler(sink, store).replaceAll(listOf(r("a", 14), r("c", 16)))
        assertEquals(listOf(r("b", 15).id), sink.cancelled)
        assertEquals(listOf(r("c", 16).id), sink.set)
        assertEquals(setOf(r("a", 14).id, r("c", 16).id), store.map.keys)
    }

    @Test fun changedTimeIsRescheduled() = runTest {
        val sink = FakeSink()
        val store = MemoryStore(mapOf(r("a", 14).id to r("a", 14).fingerprint))
        AndroidReminderScheduler(sink, store).replaceAll(listOf(r("a", 14, hour = 20)))
        assertEquals(listOf(r("a", 14).id), sink.cancelled)
        assertEquals(listOf(r("a", 14).id), sink.set)
        assertEquals(r("a", 14, hour = 20).fingerprint, store.map.getValue(r("a", 14).id))
    }
}
