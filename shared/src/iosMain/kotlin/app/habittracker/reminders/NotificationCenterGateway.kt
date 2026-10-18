package app.habittracker.reminders

import app.habittracker.domain.reminders.PlannedReminder
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/** The parts of UNUserNotificationCenter the reminder scheduler needs, so it can be tested with a fake. */
interface NotificationCenterGateway {
    /** Pending request ids mapped to the fingerprint stored with each request ("" when none). */
    suspend fun pendingFingerprints(): Map<String, String>
    fun add(reminder: PlannedReminder)
    fun remove(ids: List<String>)
    fun postNow(title: String, body: String)
}

internal const val REMINDER_CATEGORY = "HABIT_REMINDER"
internal const val MARK_DONE_ACTION = "MARK_DONE"

class UserNotificationCenterGateway(
    private val center: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter(),
) : NotificationCenterGateway {
    override suspend fun pendingFingerprints(): Map<String, String> = suspendCancellableCoroutine { cont ->
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val map = requests.orEmpty().filterIsInstance<UNNotificationRequest>().associate { request ->
                request.identifier to (request.content.userInfo["fingerprint"] as? String ?: "")
            }
            cont.resume(map)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun add(reminder: PlannedReminder) {
        val content = UNMutableNotificationContent().apply {
            setTitle(reminder.title)
            setBody(reminder.body)
            setSound(UNNotificationSound.defaultSound)
            setCategoryIdentifier(REMINDER_CATEGORY)
            setUserInfo(
                mapOf(
                    "habitId" to reminder.habitId,
                    "habitName" to reminder.habitName,
                    "date" to reminder.date.toString(),
                    "fingerprint" to reminder.fingerprint,
                ),
            )
        }
        val components = NSDateComponents().apply {
            year = reminder.date.year.toLong()
            month = reminder.date.month.ordinal.toLong() + 1
            day = reminder.date.day.toLong()
            hour = reminder.time.hour.toLong()
            minute = reminder.time.minute.toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(reminder.id, content, trigger), null)
    }

    override fun remove(ids: List<String>) {
        if (ids.isNotEmpty()) center.removePendingNotificationRequestsWithIdentifiers(ids)
    }

    override fun postNow(title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
        }
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier("habit-notice", content, null), null)
    }
}
