package app.habittracker.android.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.habittracker.android.ui.common.domainErrorMessage
import app.habittracker.android.ui.common.showReplacing
import app.habittracker.android.ui.icons.habitIcon
import app.habittracker.android.ui.theme.categoryColor
import app.habittracker.presentation.form.HabitFormEffect
import app.habittracker.presentation.form.HabitFormEvent
import app.habittracker.presentation.form.HabitFormUiState
import app.habittracker.presentation.form.HabitFormViewModel
import kotlinx.datetime.DayOfWeek
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private val dayLabels = listOf(
    DayOfWeek.MONDAY to ("M" to "Monday"),
    DayOfWeek.TUESDAY to ("T" to "Tuesday"),
    DayOfWeek.WEDNESDAY to ("W" to "Wednesday"),
    DayOfWeek.THURSDAY to ("T" to "Thursday"),
    DayOfWeek.FRIDAY to ("F" to "Friday"),
    DayOfWeek.SATURDAY to ("S" to "Saturday"),
    DayOfWeek.SUNDAY to ("S" to "Sunday"),
)

const val SCHEDULE_NOTE = "Schedule changes apply from today. Past days keep their old schedule."

/**
 * Create ([habitId] == null) or edit form, meant to be the content of a modal sheet.
 * [sheetKey] must differ for every sheet open so a reopened sheet never reuses a stale ViewModel.
 */
@Composable
fun HabitFormSheet(
    habitId: String?,
    sheetKey: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitFormViewModel = koinViewModel(key = "form-$sheetKey") { parametersOf(habitId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The sheet is a separate window, so it hosts its own snackbar above the app's.
    val snackbar = remember { SnackbarHostState() }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HabitFormEffect.Dismiss -> currentOnDismiss()
                is HabitFormEffect.ShowDomainError -> snackbar.showReplacing(domainErrorMessage(effect.error))
                is HabitFormEffect.ShowError -> snackbar.showReplacing(effect.message)
            }
        }
    }

    Box(modifier.fillMaxWidth().fillMaxHeight(0.9f).navigationBarsPadding()) {
        FormBody(state, viewModel::onEvent)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormBody(state: HabitFormUiState, onEvent: (HabitFormEvent) -> Unit) {
    val inputsEnabled = !state.isLoading && !state.isSaving
    Column(Modifier.fillMaxWidth()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            fullWidth {
                Text(
                    if (state.isEdit) "Edit habit" else "New habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            fullWidth { NameField(state, inputsEnabled, onEvent) }
            fullWidth { SectionLabel("Category") }
            fullWidth { CategoryRow(state, inputsEnabled, onEvent) }
            fullWidth { SectionLabel("Days") }
            fullWidth { DaysRow(state, inputsEnabled, onEvent) }
            if (state.showScheduleNote) {
                fullWidth {
                    Text(
                        SCHEDULE_NOTE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            fullWidth { SectionLabel("Icon") }
            items(state.iconKeys, key = { it }) { key -> IconCell(key, key == state.iconKey, inputsEnabled, onEvent) }
        }
        Button(
            onClick = { onEvent(HabitFormEvent.Save) },
            enabled = state.canSave && inputsEnabled,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) { Text("Save") }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun NameField(state: HabitFormUiState, enabled: Boolean, onEvent: (HabitFormEvent) -> Unit) {
    OutlinedTextField(
        value = state.name,
        onValueChange = { onEvent(HabitFormEvent.NameChanged(it)) },
        label = { Text("Name") },
        singleLine = true,
        enabled = enabled,
        isError = state.nameError != null,
        supportingText = state.nameError?.let { error -> { Text(domainErrorMessage(error)) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CategoryRow(state: HabitFormUiState, enabled: Boolean, onEvent: (HabitFormEvent) -> Unit) {
    val dark = isSystemInDarkTheme()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.categories, key = { it.id }) { category ->
            val color = categoryColor(category.colorKey, dark)
            FilterChip(
                selected = category.id == state.categoryId,
                onClick = { onEvent(HabitFormEvent.CategorySelected(category.id)) },
                enabled = enabled,
                label = { Text(category.name) },
                leadingIcon = {
                    Box(Modifier.size(10.dp).background(color, CircleShape))
                },
            )
        }
    }
}

@Composable
private fun DaysRow(state: HabitFormUiState, enabled: Boolean, onEvent: (HabitFormEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            dayLabels.forEach { (day, names) ->
                FilterChip(
                    selected = day in state.days,
                    onClick = { onEvent(HabitFormEvent.DayToggled(day)) },
                    enabled = enabled,
                    label = { Text(names.first, Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f).semantics { contentDescription = names.second },
                )
            }
        }
        FilterChip(
            selected = state.days.size == 7,
            onClick = { onEvent(HabitFormEvent.EveryDayToggled) },
            enabled = enabled,
            label = { Text("Every day") },
        )
    }
}

@Composable
private fun IconCell(key: String, selected: Boolean, enabled: Boolean, onEvent: (HabitFormEvent) -> Unit) {
    IconButton(
        onClick = { onEvent(HabitFormEvent.IconSelected(key)) },
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
        ),
        modifier = Modifier.semantics {
            contentDescription = "Icon $key"
            this.selected = selected
        },
    ) {
        Icon(habitIcon(key), contentDescription = null, modifier = Modifier.size(24.dp))
    }
}
