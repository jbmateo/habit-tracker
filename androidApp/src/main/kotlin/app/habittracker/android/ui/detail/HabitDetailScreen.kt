package app.habittracker.android.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.habittracker.android.ui.common.MonthGrid
import app.habittracker.android.ui.common.MonthHeader
import app.habittracker.android.ui.common.domainErrorMessage
import app.habittracker.android.ui.common.scheduleSummary
import app.habittracker.android.ui.common.showReplacing
import app.habittracker.android.ui.icons.habitIcon
import app.habittracker.android.ui.theme.categoryColor
import app.habittracker.android.ui.theme.categoryOnColor
import app.habittracker.domain.usecase.HabitDayState
import app.habittracker.presentation.detail.HabitDetailEffect
import app.habittracker.presentation.detail.HabitDetailEvent
import app.habittracker.presentation.detail.HabitDetailUiState
import app.habittracker.presentation.detail.HabitDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

/**
 * @param onClose pops this destination; must be idempotent, since the VM may signal it through both the
 * `Close` effect and the `Deleted` state.
 */
@Composable
fun HabitDetailScreen(
    habitId: String,
    snackbar: SnackbarHostState,
    onClose: () -> Unit,
    onEdit: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: HabitDetailViewModel = koinViewModel(key = "detail-$habitId") { parametersOf(habitId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val currentOnClose by rememberUpdatedState(onClose)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val close = { currentOnClose() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HabitDetailEffect.Close -> close()
                is HabitDetailEffect.OpenEdit -> currentOnEdit(effect.id)
                is HabitDetailEffect.ShowDomainError -> snackbar.showReplacing(scope, domainErrorMessage(effect.error))
                is HabitDetailEffect.ShowError -> snackbar.showReplacing(scope, effect.message)
            }
        }
    }
    LaunchedEffect(state is HabitDetailUiState.Deleted) {
        if (state is HabitDetailUiState.Deleted) close()
    }

    Column(modifier.fillMaxSize().padding(contentPadding)) {
        IconButton(onClick = { close() }, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        (state as? HabitDetailUiState.Content)?.let { DetailContent(it, viewModel::onEvent) }
    }
}

@Composable
private fun DetailContent(state: HabitDetailUiState.Content, onEvent: (HabitDetailEvent) -> Unit) {
    val detail = state.detail
    val habit = detail.habit
    val dark = isSystemInDarkTheme()
    val color = categoryColor(habit.category.colorKey, dark)
    val onColor = categoryOnColor(habit.category.colorKey, dark)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(habitIcon(habit.iconKey), contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
            Column {
                Text(habit.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
                val subtitle = scheduleSummary(habit.currentSchedule.days) + if (habit.isArchived) " · Archived" else ""
                Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("🔥 ${detail.stats.currentStreak}", "Current streak", Modifier.weight(1f))
            StatTile("${detail.stats.bestStreak}", "Best streak", Modifier.weight(1f))
            StatTile(detail.stats.completionRate?.let { "${(it * 100).roundToInt()}%" } ?: "—", "Completion", Modifier.weight(1f))
        }
        MonthHeader(
            month = detail.month,
            canGoNext = state.canGoNext,
            onPrev = { onEvent(HabitDetailEvent.PrevMonth) },
            onNext = { onEvent(HabitDetailEvent.NextMonth) },
        )
        MonthGrid(
            month = detail.month,
            leadingBlanks = state.leadingBlanks,
            cells = detail.days.map { (date, dayState) -> @Composable { HabitDayCell(date.day, dayState, color, onColor) } },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
            OutlinedButton(onClick = { onEvent(HabitDetailEvent.EditClicked) }) { Text("Edit") }
            OutlinedButton(onClick = { onEvent(HabitDetailEvent.ArchiveToggled) }) {
                Text(if (habit.isArchived) "Unarchive" else "Archive")
            }
            OutlinedButton(
                onClick = { onEvent(HabitDetailEvent.DeleteClicked) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
        }
    }

    if (state.confirmingDelete) {
        AlertDialog(
            onDismissRequest = { onEvent(HabitDetailEvent.DeleteCancelled) },
            title = { Text("Delete habit?") },
            text = { Text("This erases all history for this habit.") },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(HabitDetailEvent.DeleteConfirmed) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { onEvent(HabitDetailEvent.DeleteCancelled) }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HabitDayCell(day: Int, state: HabitDayState, color: Color, onColor: Color) {
    val description = when (state) {
        HabitDayState.Done -> "done"
        HabitDayState.Missed -> "missed"
        HabitDayState.Pending -> "pending"
        HabitDayState.NotDue -> "not scheduled"
        HabitDayState.Future -> "upcoming"
    }
    val dim = state == HabitDayState.NotDue || state == HabitDayState.Future
    Box(
        Modifier
            .padding(4.dp)
            .fillMaxSize()
            .alpha(if (dim) 0.4f else 1f)
            .then(
                when (state) {
                    HabitDayState.Done -> Modifier.background(color, CircleShape)
                    HabitDayState.Missed -> Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), CircleShape)
                    HabitDayState.Pending -> Modifier.border(1.5.dp, color, CircleShape)
                    HabitDayState.NotDue, HabitDayState.Future -> Modifier
                },
            )
            .semantics { contentDescription = "Day $day, $description" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$day",
            style = MaterialTheme.typography.labelMedium,
            color = when (state) {
                HabitDayState.Done -> onColor
                HabitDayState.Missed -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
