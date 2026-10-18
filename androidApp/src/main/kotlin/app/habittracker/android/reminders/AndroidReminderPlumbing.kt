package app.habittracker.android.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import app.habittracker.domain.reminders.PlannedReminder
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

const val REMINDER_CHANNEL_ID = "habit_reminders"
internal const val EXTRA_HABIT_ID = "habitId"
internal const val EXTRA_HABIT_NAME = "habitName"
internal const val EXTRA_DATE = "date"
internal const val EXTRA_TITLE = "title"
internal const val EXTRA_BODY = "body"

/** Reminder notifications use (tag = reminder id, id = this), so two reminders never share a notification slot. */
internal const val REMINDER_NOTIFICATION_ID = 1
internal const val REMINDER_TAG_PREFIX = "habit:"
/** The "Couldn't mark" notice uses its own tag so clearing settled reminders leaves it alone. */
internal const val NOTICE_TAG_PREFIX = "notice:"

fun ensureReminderChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(REMINDER_CHANNEL_ID, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT),
    )
}

/** PendingIntent request code; intents are told apart by their data URI, so a hash collision is harmless. */
internal fun requestCodeFor(reminderId: String): Int = reminderId.hashCode()

/** Broadcast that fires a reminder; the data URI makes each reminder's intent distinct for cancelling. */
internal fun reminderIntent(context: Context, reminderId: String): Intent =
    Intent(context, ReminderReceiver::class.java).setData(Uri.parse("habit://reminder/$reminderId"))

class RealAlarmSink(private val context: Context) : AlarmSink {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun set(reminder: PlannedReminder) {
        val intent = reminderIntent(context, reminder.id)
            .putExtra(EXTRA_HABIT_ID, reminder.habitId)
            .putExtra(EXTRA_HABIT_NAME, reminder.habitName)
            .putExtra(EXTRA_DATE, reminder.date.toString())
            .putExtra(EXTRA_TITLE, reminder.title)
            .putExtra(EXTRA_BODY, reminder.body)
        val pending = PendingIntent.getBroadcast(
            context, requestCodeFor(reminder.id), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = LocalDateTime(reminder.date, reminder.time).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    override fun cancel(id: String) {
        val pending = PendingIntent.getBroadcast(
            context, requestCodeFor(id), reminderIntent(context, id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarms.cancel(pending)
        pending.cancel()
    }
}

class PrefsReminderStore(context: Context) : ScheduledReminderStore {
    private val prefs = context.getSharedPreferences("scheduled_reminders", Context.MODE_PRIVATE)

    override fun load(): Map<String, String> = prefs.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()

    override fun save(scheduled: Map<String, String>) {
        prefs.edit {
            clear()
            scheduled.forEach { (id, fingerprint) -> putString(id, fingerprint) }
        }
    }
}

/** POST_NOTIFICATIONS exists only from API 33; below that it always reads as denied, so only the app-level switch counts. */
fun mayPostNotifications(sdkInt: Int, permissionGranted: Boolean, appNotificationsEnabled: Boolean): Boolean =
    appNotificationsEnabled && (sdkInt < Build.VERSION_CODES.TIRAMISU || permissionGranted)

/** Reminder notifications are posted with their reminder id as the tag, so they can be found and dismissed here. */
class NotificationShade(context: Context) : DeliveredReminders {
    private val manager = context.getSystemService(NotificationManager::class.java)

    override fun activeIds(): Set<String> =
        manager.activeNotifications.mapNotNull { it.tag }.filter { it.startsWith(REMINDER_TAG_PREFIX) }.toSet()

    override fun dismiss(id: String) = manager.cancel(id, REMINDER_NOTIFICATION_ID)
}
