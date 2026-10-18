package app.habittracker.android.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.habittracker.android.ui.common.CategoryHeader
import app.habittracker.android.ui.common.HabitRow
import app.habittracker.android.ui.common.MonthGrid
import app.habittracker.android.ui.common.MonthHeader
import app.habittracker.android.ui.common.StatTile
import app.habittracker.android.ui.common.StreakBandBackground
import app.habittracker.android.ui.common.domainErrorMessage
import app.habittracker.android.ui.common.showReplacing
import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.usecase.CalendarDay
import app.habittracker.domain.usecase.DayDetail
import app.habittracker.domain.usecase.MonthCalendar
import app.habittracker.domain.usecase.TodayHabitItem
import app.habittracker.presentation.calendar.CalendarEffect
import app.habittracker.presentation.calendar.CalendarEvent
import app.habittracker.presentation.calendar.CalendarUiState
import app.habittracker.presentation.calendar.CalendarViewModel
import app.habittracker.presentation.calendar.StreakBand
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun CalendarScreen(
    snackbar: SnackbarHostState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CalendarEffect.ShowDomainError -> snackbar.showReplacing(scope, domainErrorMessage(effect.error))
                is CalendarEffect.ShowError -> snackbar.showReplacing(scope, effect.message)
            }
        }
    }

    CalendarContent(state, viewModel::onEvent, modifier.fillMaxSize().padding(contentPadding))
}

@Composable
private fun CalendarContent(state: CalendarUiState, onEvent: (CalendarEvent) -> Unit, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        item {
            MonthHeader(
                month = state.month,
                canGoNext = state.canGoNext,
                onPrev = { onEvent(CalendarEvent.PrevMonth) },
                onNext = { onEvent(CalendarEvent.NextMonth) },
            )
        }
        val calendar = state.calendar ?: return@LazyColumn
        item {
            MonthGrid(
                month = state.month,
                leadingBlanks = state.leadingBlanks,
                cells = calendar.days.mapIndexed { i, day ->
                    @Composable {
                        val band = state.bands.getOrElse(i) { StreakBand.None }
                        DayCell(day, band, selected = state.selected?.date == day.date) {
                            onEvent(
                                if (state.selected?.date == day.date) CalendarEvent.ClearSelection
                                else CalendarEvent.SelectDay(day.date),
                            )
                        }
                    }
                },
            )
        }
        item { MonthSummary(calendar, Modifier.padding(top = 12.dp)) }
        state.selected?.let { detail -> item { DayPanel(detail, onEvent, Modifier.padding(top = 12.dp)) } }
    }
}

@Composable
private fun DayCell(day: CalendarDay, band: StreakBand, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        StreakBandBackground(band, colorKey = "orange")
        DayCellContent(day, selected, onClick)
    }
}

@Composable
private fun DayCellContent(day: CalendarDay, selected: Boolean, onClick: () -> Unit) {
    val status = day.status
    val primary = MaterialTheme.colorScheme.primary
    val border = when {
        selected -> BorderStroke(2.dp, primary)
        status is DayStatus.InProgress -> BorderStroke(1.dp, primary)
        else -> null
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = border,
        modifier = Modifier
            .padding(2.dp)
            .fillMaxSize()
            .alpha(if (status is DayStatus.Future) 0.4f else 1f)
            .semantics { contentDescription = "Day ${day.date.day}, ${describe(status)}" },
    ) {
        Column(
            Modifier.clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("${day.date.day}", style = MaterialTheme.typography.labelLarge)
            Box(Modifier.padding(bottom = 4.dp).size(16.dp), contentAlignment = Alignment.Center) { Marker(status) }
        }
    }
}

@Composable
private fun Marker(status: DayStatus) {
    when (status) {
        is DayStatus.Perfect -> Text("🔥", fontSize = 12.sp)
        is DayStatus.Partial -> MiniRing(status.done, status.due)
        is DayStatus.InProgress -> MiniRing(status.done, status.due)
        is DayStatus.Missed -> Box(
            Modifier.size(6.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.5f), CircleShape),
        )
        DayStatus.NoHabits, DayStatus.Future -> Unit
    }
}

@Composable
private fun MiniRing(done: Int, due: Int) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val color = MaterialTheme.colorScheme.primary
    val fraction = if (due == 0) 0f else done.toFloat() / due
    Canvas(Modifier.size(14.dp)) {
        val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        val inset = stroke.width / 2
        val arc = Size(size.width - stroke.width, size.height - stroke.width)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = stroke)
        drawArc(color, -90f, 360f * fraction, false, Offset(inset, inset), arc, style = stroke)
    }
}

private fun describe(status: DayStatus): String = when (status) {
    DayStatus.Future -> "upcoming"
    DayStatus.NoHabits -> "no habits"
    is DayStatus.Perfect -> "perfect"
    is DayStatus.InProgress -> "${status.done} of ${status.due} done, in progress"
    is DayStatus.Partial -> "${status.done} of ${status.due} done"
    is DayStatus.Missed -> "missed"
}

@Composable
private fun MonthSummary(calendar: MonthCalendar, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("${calendar.perfectDays}", "Perfect days", "orange", Modifier.weight(1f))
        StatTile(
            calendar.completionRate?.let { "${(it * 100).roundToInt()}%" } ?: "—",
            "Completion",
            "blue",
            Modifier.weight(1f),
        )
        StatTile("${calendar.longestPerfectRun}", "Longest run", "purple", Modifier.weight(1f))
    }
}

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")

@Composable
private fun DayPanel(detail: DayDetail, onEvent: (CalendarEvent) -> Unit, modifier: Modifier = Modifier) {
    val date = remember(detail.date) {
        java.time.LocalDate.of(detail.date.year, detail.date.month.ordinal + 1, detail.date.day).format(dayFormatter)
    }
    val pendingDay = detail.status is DayStatus.InProgress || detail.status is DayStatus.Future
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(date, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            if (detail.groups.isEmpty()) {
                Text(
                    "No habits due.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            detail.groups.forEach { group ->
                CategoryHeader(group.category)
                group.items.forEach { item ->
                    HabitRow(
                        item = TodayHabitItem(item.habit, item.isDone, streak = 0),
                        onToggle = { onEvent(CalendarEvent.ToggleHabit(item.habit.id, detail.date)) },
                        toggleEnabled = detail.isEditable,
                        status = when {
                            item.isDone -> "Done"
                            pendingDay -> null
                            else -> "Missed"
                        },
                    )
                }
            }
            if (!detail.isEditable && detail.groups.isNotEmpty()) {
                Text(
                    "Only today and yesterday can be changed.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}
