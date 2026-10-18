package app.habittracker.domain.reminders

/** Platform notification scheduling. Each platform binds one implementation. */
interface ReminderScheduler {
    /** Make pending reminders exactly [reminders]: add missing ids, remove ids no longer present. */
    suspend fun replaceAll(reminders: List<PlannedReminder>)

    /** Remove already-delivered reminder notifications whose id isn't in [keep] (done, deleted or stale). */
    suspend fun clearDelivered(keep: Set<String>)
}
