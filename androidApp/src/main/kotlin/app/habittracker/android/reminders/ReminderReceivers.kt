package app.habittracker.android.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

private fun canNotify(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
    PendingIntent.FLAG_IMMUTABLE,
)

/** Posts the reminder notification with a "Mark done" action. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!canNotify(context)) return
        val reminderId = intent.data?.lastPathSegment ?: return
        val notificationId = notificationIdFor(reminderId)
        val markDone = Intent(context, MarkDoneReceiver::class.java)
            .setData(intent.data)
            .putExtras(intent)
            .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        val markDonePending = PendingIntent.getBroadcast(
            context, notificationId, markDone, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

/** Checks the habit off from the notification, even when the app isn't running. */
class MarkDoneReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val date = intent.getStringExtra(EXTRA_DATE)?.let(LocalDate::parse) ?: return
        val name = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "habit"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val pending = goAsync()
        receiverScope.launch {
            try {
                val ok = runCatching { get<MarkDoneFromReminder>()(habitId, date) is Outcome.Success }.getOrDefault(false)
                val manager = NotificationManagerCompat.from(context)
                manager.cancel(notificationId)
                if (ok) {
                    get<ReminderSync>().syncNow()
                } else if (canNotify(context)) {
                    val notice = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Couldn't mark $name. Open the app.")
                        .setContentIntent(openAppIntent(context))
                        .setAutoCancel(true)
                        .build()
                    @Suppress("MissingPermission") // checked by canNotify
                    manager.notify(notificationId, notice)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are forgotten on reboot; re-plan them. */
class BootReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
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
