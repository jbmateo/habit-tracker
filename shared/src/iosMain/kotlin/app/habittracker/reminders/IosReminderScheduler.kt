package app.habittracker.reminders

import app.habittracker.domain.reminders.PlannedReminder
import app.habittracker.domain.reminders.ReminderScheduler

/** Keeps the app's pending notifications (ids starting with "habit:") equal to the planned reminders. */
class IosReminderScheduler(private val gateway: NotificationCenterGateway) : ReminderScheduler {
    override suspend fun replaceAll(reminders: List<PlannedReminder>) {
        val pending = gateway.pendingFingerprints().filterKeys { it.startsWith("habit:") }
        val wanted = reminders.associateBy { it.id }
        val stale = pending.filter { (id, fingerprint) -> wanted[id]?.fingerprint != fingerprint }.keys
        gateway.remove(stale.toList())
        reminders.filter { it.id !in pending || it.id in stale }.forEach(gateway::add)
    }

    override suspend fun clearDelivered(keep: Set<String>) {
        gateway.removeDelivered(gateway.deliveredIds().filter { it.startsWith("habit:") && it !in keep })
    }
}
