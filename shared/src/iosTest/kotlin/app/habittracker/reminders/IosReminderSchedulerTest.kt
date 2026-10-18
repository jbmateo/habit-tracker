package app.habittracker.reminders

import app.habittracker.domain.reminders.PlannedReminder
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class IosReminderSchedulerTest {
    private class FakeGateway(initial: Map<String, String>) : NotificationCenterGateway {
        val pending = initial.toMutableMap()
        val added = mutableListOf<String>()
        val removed = mutableListOf<String>()
        override suspend fun pendingFingerprints(): Map<String, String> = pending.toMap()
        override fun add(reminder: PlannedReminder) { added += reminder.id; pending[reminder.id] = reminder.fingerprint }
        override fun remove(ids: List<String>) { removed += ids; ids.forEach(pending::remove) }
        override fun postNow(title: String, body: String) = Unit
    }

    private fun r(habit: String, day: Int, hour: Int = 9) =
        PlannedReminder(habit, habit, "run", LocalDate(2026, 10, day), LocalTime(hour, 0), streak = 0)

    @Test fun replaceAllAddsMissingAndRemovesStale() = runTest {
        val gateway = FakeGateway(mapOf(r("a", 14).id to r("a", 14).fingerprint, r("b", 15).id to r("b", 15).fingerprint, "other:x" to "x"))
        IosReminderScheduler(gateway).replaceAll(listOf(r("a", 14), r("c", 16)))
        assertEquals(listOf(r("b", 15).id), gateway.removed)
        assertEquals(listOf(r("c", 16).id), gateway.added)
        assertEquals(setOf(r("a", 14).id, r("c", 16).id, "other:x"), gateway.pending.keys)
    }

    @Test fun changedTimeIsReplaced() = runTest {
        val gateway = FakeGateway(mapOf(r("a", 14).id to r("a", 14).fingerprint))
        IosReminderScheduler(gateway).replaceAll(listOf(r("a", 14, hour = 20)))
        assertEquals(listOf(r("a", 14).id), gateway.removed)
        assertEquals(listOf(r("a", 14).id), gateway.added)
        assertEquals(r("a", 14, hour = 20).fingerprint, gateway.pending.getValue(r("a", 14).id))
    }
}
