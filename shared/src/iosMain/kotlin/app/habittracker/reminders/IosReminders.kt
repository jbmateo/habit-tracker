package app.habittracker.reminders

import app.habittracker.domain.model.Outcome
import app.habittracker.domain.reminders.ReminderSync
import app.habittracker.domain.usecase.MarkDoneFromReminder
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatformTools
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNUserNotificationCenter

/** Entry points the Swift app uses for reminders. */
object IosReminders {
    /** Registers the reminder category with its "Mark done" action (no app launch needed). */
    fun registerCategory() {
        val markDone = UNNotificationAction.actionWithIdentifier(MARK_DONE_ACTION, title = "Mark done", options = 0u)
        val category = UNNotificationCategory.categoryWithIdentifier(
            REMINDER_CATEGORY,
            actions = listOf(markDone),
            intentIdentifiers = emptyList<String>(),
            options = 0u,
        )
        UNUserNotificationCenter.currentNotificationCenter().setNotificationCategories(setOf(category))
    }

    /**
     * Handles "Mark done" for [habitId] on [isoDate]. Returns true on success; on failure posts
     * "Couldn't mark <habitName>. Open the app." and returns false.
     */
    suspend fun markDone(habitId: String, isoDate: String, habitName: String): Boolean {
        val koin = KoinPlatformTools.defaultContext().get()
        val ok = runCatching { koin.get<MarkDoneFromReminder>()(habitId, LocalDate.parse(isoDate)) is Outcome.Success }
            .getOrDefault(false)
        if (ok) {
            koin.get<ReminderSync>().syncNow()
        } else {
            UserNotificationCenterGateway().postNow("Couldn't mark $habitName. Open the app.", "")
        }
        return ok
    }
}
