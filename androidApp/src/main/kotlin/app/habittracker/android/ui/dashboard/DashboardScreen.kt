package app.habittracker.android.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.habittracker.android.ui.common.HabitRow
import app.habittracker.android.ui.common.ProgressRing
import app.habittracker.android.ui.common.StreakCard
import app.habittracker.android.ui.common.WeekComparisonLine
import app.habittracker.android.ui.common.domainErrorMessage
import app.habittracker.android.ui.common.showReplacing
import app.habittracker.domain.model.TodaySort
import app.habittracker.presentation.dashboard.DashboardEffect
import app.habittracker.presentation.dashboard.DashboardEvent
import app.habittracker.presentation.dashboard.DashboardUiState
import app.habittracker.presentation.dashboard.DashboardViewModel
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    snackbar: SnackbarHostState,
    onAddHabit: () -> Unit,
    onOpenHabit: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var celebrations by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val currentOnAddHabit by rememberUpdatedState(onAddHabit)
    val currentOnOpenHabit by rememberUpdatedState(onOpenHabit)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DashboardEffect.Celebrate -> celebrations++
                DashboardEffect.OpenCreateHabit -> currentOnAddHabit()
                is DashboardEffect.OpenHabit -> currentOnOpenHabit(effect.id)
                is DashboardEffect.ShowDomainError -> snackbar.showReplacing(scope, domainErrorMessage(effect.error))
                is DashboardEffect.ShowError -> snackbar.showReplacing(scope, effect.message)
            }
        }
    }

    Column(modifier.fillMaxSize().padding(contentPadding)) {
        when (val s = state) {
            DashboardUiState.Loading -> Unit
            DashboardUiState.NoHabits -> EmptyState(
                title = "No habits yet",
                body = "Build your routine one day at a time.",
                cta = "Create your first habit",
                onCta = { viewModel.onEvent(DashboardEvent.AddHabitClicked) },
            )
            is DashboardUiState.Content -> DashboardContent(s, celebrations, viewModel::onEvent)
        }
    }
}

@Composable
private fun DashboardContent(state: DashboardUiState.Content, celebrations: Int, onEvent: (DashboardEvent) -> Unit) {
    val dashboard = state.dashboard
    val haptic = LocalHapticFeedback.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDate(dashboard.date),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    SortMenu(state.sort, onSelect = { onEvent(DashboardEvent.SortSelected(it)) })
                    IconButton(onClick = { onEvent(DashboardEvent.AddHabitClicked) }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add habit")
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProgressRing(dashboard.done, dashboard.due, celebrations)
                    StreakCard(dashboard.streak, Modifier.weight(1f))
                }
                WeekComparisonLine(dashboard.week, Modifier.padding(top = 12.dp))
                if (dashboard.isPerfect) {
                    Text("Perfect day!", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        if (dashboard.groups.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing due today",
                    body = "Enjoy the day off.",
                    cta = null,
                    onCta = {},
                    modifier = Modifier.padding(top = 32.dp),
                )
            }
        }
        if (state.items.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                ) {
                    Column {
                        state.items.forEachIndexed { index, item ->
                            key(item.habit.id) {
                                HabitRow(
                                    item = item,
                                    onToggle = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onEvent(DashboardEvent.ToggleHabit(item.habit.id))
                                    },
                                    onOpen = { onEvent(DashboardEvent.HabitClicked(item.habit.id)) },
                                )
                            }
                            if (index < state.items.lastIndex) {
                                HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val sortLabels = linkedMapOf(
    TodaySort.Category to "Category",
    TodaySort.Name to "Name A–Z",
    TodaySort.Streak to "Streak",
    TodaySort.ToDoFirst to "To do first",
)

@Composable
private fun SortMenu(current: TodaySort, onSelect: (TodaySort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort habits, ${sortLabels.getValue(current)}")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            sortLabels.forEach { (sort, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { open = false; onSelect(sort) },
                    trailingIcon = { if (sort == current) Icon(Icons.Rounded.Check, contentDescription = "Selected") },
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String, cta: String?, onCta: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (cta != null) Button(onClick = onCta) { Text(cta) }
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")

private fun formatDate(date: LocalDate): String =
    java.time.LocalDate.of(date.year, date.month.ordinal + 1, date.day).format(dateFormatter)
