package app.habittracker.android.ui.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.habittracker.android.ui.common.CategoryHeader
import app.habittracker.android.ui.common.reminderSuffix
import app.habittracker.android.ui.common.scheduleSummary
import app.habittracker.android.ui.icons.habitIcon
import app.habittracker.android.ui.theme.categoryColor
import app.habittracker.domain.model.Habit
import app.habittracker.domain.usecase.HabitsOverview
import app.habittracker.presentation.habits.HabitListEffect
import app.habittracker.presentation.habits.HabitListEvent
import app.habittracker.presentation.habits.HabitListUiState
import app.habittracker.presentation.habits.HabitListViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HabitListScreen(
    onOpenHabit: (String) -> Unit,
    onAddHabit: () -> Unit,
    onOpenCategories: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: HabitListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOpenHabit by rememberUpdatedState(onOpenHabit)
    val currentAddHabit by rememberUpdatedState(onAddHabit)
    val currentOpenCategories by rememberUpdatedState(onOpenCategories)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HabitListEffect.OpenHabit -> currentOpenHabit(effect.id)
                HabitListEffect.OpenCreateHabit -> currentAddHabit()
                HabitListEffect.OpenCategories -> currentOpenCategories()
            }
        }
    }

    Column(modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Habits",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.onEvent(HabitListEvent.AddHabitClicked) }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add habit")
            }
        }
        when (val s = state) {
            HabitListUiState.Loading -> Unit
            is HabitListUiState.Content -> HabitListContent(s.overview, viewModel::onEvent)
        }
    }
}

@Composable
private fun HabitListContent(overview: HabitsOverview, onEvent: (HabitListEvent) -> Unit) {
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (overview.active.isEmpty()) {
            item {
                Text(
                    "No active habits.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        overview.active.forEach { group ->
            item(key = "header-${group.category.id}") { CategoryHeader(group.category) }
            items(group.items, key = { it.id }) { habit ->
                HabitListRow(habit) { onEvent(HabitListEvent.HabitClicked(habit.id)) }
            }
        }
        item(key = "categories") {
            TextButton(
                onClick = { onEvent(HabitListEvent.ManageCategoriesClicked) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            ) { Text("Manage categories") }
        }
        if (overview.archived.isNotEmpty()) {
            item(key = "archived-header") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { archivedExpanded = !archivedExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Archived (${overview.archived.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (archivedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (archivedExpanded) "Collapse archived" else "Expand archived",
                    )
                }
            }
            if (archivedExpanded) {
                items(overview.archived, key = { "archived-${it.id}" }) { habit ->
                    HabitListRow(habit) { onEvent(HabitListEvent.HabitClicked(habit.id)) }
                }
            }
        }
    }
}

@Composable
private fun HabitListRow(habit: Habit, onClick: () -> Unit) {
    val color = categoryColor(habit.category.colorKey, isSystemInDarkTheme())
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(habitIcon(habit.iconKey), contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(habit.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                scheduleSummary(habit.currentSchedule.days) + reminderSuffix(LocalContext.current, habit.reminderTime),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
