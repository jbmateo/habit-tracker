package app.habittracker.android.ui.form

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.habittracker.android.ui.common.formatReminderTime
import app.habittracker.presentation.form.HabitFormEvent
import app.habittracker.presentation.form.HabitFormUiState
import kotlinx.datetime.LocalTime

const val REMINDER_HINT = "Only on scheduled days. Skipped if you've already done it."
const val REMINDER_DENIED_NOTE = "Notifications are off for Habits. Turn them on in Settings."

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
internal fun ReminderSection(state: HabitFormUiState, enabled: Boolean, onEvent: (HabitFormEvent) -> Unit) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    var picking by remember { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    // Back from system Settings: re-read the permission.
    LifecycleResumeEffect(Unit) {
        allowed = notificationsAllowed(context)
        onPauseOrDispose { }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Remind me", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = state.reminderEnabled,
                onCheckedChange = { on ->
                    onEvent(HabitFormEvent.ReminderToggled)
                    if (on && !allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = "Remind me" },
            )
        }
        if (state.reminderEnabled) {
            OutlinedButton(
                onClick = { picking = true },
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = "Reminder time" },
            ) { Text(formatReminderTime(context, state.reminderTime)) }
            Text(REMINDER_HINT, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!allowed) {
                Text(REMINDER_DENIED_NOTE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }) { Text("Open Settings") }
            }
        }
    }

    if (picking) {
        ReminderTimeDialog(
            initial = state.reminderTime,
            is24Hour = DateFormat.is24HourFormat(context),
            onConfirm = { time ->
                picking = false
                onEvent(HabitFormEvent.ReminderTimeChanged(time))
            },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initial: LocalTime, is24Hour: Boolean, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder time") },
        text = { TimePicker(picker, Modifier.padding(top = 8.dp)) },
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime(picker.hour, picker.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
