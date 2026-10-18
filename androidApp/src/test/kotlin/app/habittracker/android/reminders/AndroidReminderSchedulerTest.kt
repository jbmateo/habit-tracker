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

    private class FakeShade(ids: Set<String>) : DeliveredReminders {
        val active = ids.toMutableSet()
        override fun activeIds(): Set<String> = active.toSet()
        override fun dismiss(id: String) { active -= id }
    }

    private fun r(habit: String, day: Int, hour: Int = 9) =
        PlannedReminder(habit, habit, "run", LocalDate(2026, 10, day), LocalTime(hour, 0), streak = 0)

    @Test fun replaceAllSetsNewAndCancelsRemoved() = runTest {
        val sink = FakeSink()
        val store = MemoryStore(mapOf(r("a", 14).id to r("a", 14).fingerprint, r("b", 15).id to r("b", 15).fingerprint))
        AndroidReminderScheduler(sink, store, FakeShade(emptySet())).replaceAll(listOf(r("a", 14), r("c", 16)))
        assertEquals(listOf(r("b", 15).id), sink.cancelled)
        // Every wanted alarm is (re)set: the store can't tell whether the OS still holds it.
        assertEquals(listOf(r("a", 14).id, r("c", 16).id), sink.set)
        assertEquals(setOf(r("a", 14).id, r("c", 16).id), store.map.keys)
    }

    @Test fun changedTimeIsRescheduled() = runTest {
        val sink = FakeSink()
        val store = MemoryStore(mapOf(r("a", 14).id to r("a", 14).fingerprint))
        AndroidReminderScheduler(sink, store, FakeShade(emptySet())).replaceAll(listOf(r("a", 14, hour = 20)))
        assertEquals(emptyList(), sink.cancelled) // same id: setting it replaces the old alarm
        assertEquals(listOf(r("a", 14).id), sink.set)
        assertEquals(r("a", 14, hour = 20).fingerprint, store.map.getValue(r("a", 14).id))
    }

    @Test fun alarmsLostAfterRebootAreSetAgain() = runTest {
        // A reboot or force-stop wipes alarms but not the store.
        val sink = FakeSink()
        val store = MemoryStore(mapOf(r("a", 14).id to r("a", 14).fingerprint, r("a", 15).id to r("a", 15).fingerprint))
        AndroidReminderScheduler(sink, store, FakeShade(emptySet())).replaceAll(listOf(r("a", 14), r("a", 15)))
        assertEquals(listOf(r("a", 14).id, r("a", 15).id), sink.set)
    }

    @Test fun clearDeliveredDismissesSettledRemindersOnly() = runTest {
        val shade = FakeShade(setOf(r("a", 13).id, r("a", 14).id, r("b", 14).id))
        AndroidReminderScheduler(FakeSink(), MemoryStore(emptyMap()), shade).clearDelivered(keep = setOf(r("a", 14).id))
        assertEquals(setOf(r("a", 14).id), shade.active)
    }

    @Test fun notificationGateByApiLevel() {
        // Below API 33 POST_NOTIFICATIONS doesn't exist and always reads as denied.
        assertEquals(true, mayPostNotifications(sdkInt = 31, permissionGranted = false, appNotificationsEnabled = true))
        assertEquals(false, mayPostNotifications(sdkInt = 31, permissionGranted = false, appNotificationsEnabled = false))
        assertEquals(false, mayPostNotifications(sdkInt = 34, permissionGranted = false, appNotificationsEnabled = true))
        assertEquals(true, mayPostNotifications(sdkInt = 34, permissionGranted = true, appNotificationsEnabled = true))
    }
}
