package app.habittracker.android.reminders

import app.habittracker.domain.reminders.PlannedReminder
import app.habittracker.domain.reminders.ReminderScheduler

/** Sets and cancels one alarm per reminder id. */
interface AlarmSink {
    fun set(reminder: PlannedReminder)
    fun cancel(id: String)
}

/** Remembers which reminders are scheduled (id → fingerprint); AlarmManager can't list its own alarms. */
interface ScheduledReminderStore {
    fun load(): Map<String, String>
    fun save(scheduled: Map<String, String>)
}

/** Keeps alarms equal to the planned reminders: cancels removed or changed ones, sets new or changed ones. */
class AndroidReminderScheduler(
    private val alarms: AlarmSink,
    private val store: ScheduledReminderStore,
) : ReminderScheduler {
    override suspend fun replaceAll(reminders: List<PlannedReminder>) {
        val scheduled = store.load()
        val wanted = reminders.associateBy { it.id }
        val stale = scheduled.filter { (id, fingerprint) -> wanted[id]?.fingerprint != fingerprint }.keys
        stale.forEach(alarms::cancel)
        reminders.filter { it.id !in scheduled || it.id in stale }.forEach(alarms::set)
        store.save(reminders.associate { it.id to it.fingerprint })
    }
}
