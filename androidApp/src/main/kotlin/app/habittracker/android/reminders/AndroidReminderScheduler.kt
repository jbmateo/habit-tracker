package app.habittracker.android.reminders

import app.habittracker.domain.reminders.PlannedReminder
import app.habittracker.domain.reminders.ReminderScheduler

/** Sets and cancels one alarm per reminder id. */
interface AlarmSink {
    fun set(reminder: PlannedReminder)
    fun cancel(id: String)
}

/** Remembers which reminders were scheduled (id → fingerprint); AlarmManager can't list its own alarms. */
interface ScheduledReminderStore {
    fun load(): Map<String, String>
    fun save(scheduled: Map<String, String>)
}

/**
 * Keeps alarms equal to the planned reminders. Every wanted alarm is set again on each pass,
 * because a reboot or force-stop wipes alarms without touching the store; setting an existing id
 * replaces it. The store only tells us which ids to cancel.
 */
class AndroidReminderScheduler(
    private val alarms: AlarmSink,
    private val store: ScheduledReminderStore,
) : ReminderScheduler {
    override suspend fun replaceAll(reminders: List<PlannedReminder>) {
        val wanted = reminders.map { it.id }.toSet()
        store.load().keys.filter { it !in wanted }.forEach(alarms::cancel)
        reminders.forEach(alarms::set)
        store.save(reminders.associate { it.id to it.fingerprint })
    }
}
