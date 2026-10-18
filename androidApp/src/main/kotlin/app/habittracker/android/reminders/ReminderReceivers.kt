package app.habittracker.android.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.habittracker.android.MainActivity
import app.habittracker.android.R
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.reminders.ReminderSync
import app.habittracker.domain.usecase.MarkDoneFromReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private fun canNotify(context: Context): Boolean = mayPostNotifications(
    sdkInt = Build.VERSION.SDK_INT,
    permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
    appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
)

private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
    PendingIntent.FLAG_IMMUTABLE,
)

/** Posts the reminder notification with a "Mark done" action, then re-plans so the window keeps moving. */
class ReminderReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        receiverScope.launch {
            try {
                post(context, intent)
                // Without this, someone who stops opening the app gets 7 days of reminders and then none.
                get<ReminderSync>().syncNow()
            } finally {
                pending.finish()
            }
        }
    }

    private fun post(context: Context, intent: Intent) {
        if (!canNotify(context)) return
        val reminderId = intent.data?.lastPathSegment ?: return
        val markDone = Intent(context, MarkDoneReceiver::class.java)
            .setData(intent.data)
            .putExtras(intent)
        val markDonePending = PendingIntent.getBroadcast(
            context, requestCodeFor(reminderId), markDone, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(intent.getStringExtra(EXTRA_TITLE))
            .setContentText(intent.getStringExtra(EXTRA_BODY))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .addAction(0, "Mark done", markDonePending)
            .build()
        @Suppress("MissingPermission") // checked by canNotify
        NotificationManagerCompat.from(context).notify(reminderId, REMINDER_NOTIFICATION_ID, notification)
    }
}

/** Checks the habit off from the notification, even when the app isn't running. */
class MarkDoneReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val date = intent.getStringExtra(EXTRA_DATE)?.let(LocalDate::parse) ?: return
        val name = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "habit"
        val reminderId = intent.data?.lastPathSegment ?: return
        val pending = goAsync()
        receiverScope.launch {
            try {
                val ok = runCatching { get<MarkDoneFromReminder>()(habitId, date) is Outcome.Success }.getOrDefault(false)
                val manager = NotificationManagerCompat.from(context)
                manager.cancel(reminderId, REMINDER_NOTIFICATION_ID)
                if (ok) {
                    get<ReminderSync>().syncNow()
                } else if (canNotify(context)) {
                    val notice = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Couldn't mark $name. Open the app.")
                        .setContentIntent(openAppIntent(context))
                        .setAutoCancel(true)
                        .setSilent(true)
                        .build()
                    @Suppress("MissingPermission") // checked by canNotify
                    manager.notify(NOTICE_TAG_PREFIX + reminderId, REMINDER_NOTIFICATION_ID, notice)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

private val replanActions = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED)

/** Alarms are forgotten on reboot and hold instants from the old zone after a zone or clock change; re-plan them. */
class BootReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in replanActions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                get<ReminderSync>().syncNow()
            } finally {
                pending.finish()
            }
        }
    }
}
