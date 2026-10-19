package app.habittracker.android.ui.categories

import app.habittracker.android.ui.theme.HabitTheme
import app.habittracker.android.ui.theme.CardShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import app.habittracker.android.ui.theme.categoryColor
import app.habittracker.domain.model.Category
import app.habittracker.domain.model.ColorKeys
import app.habittracker.presentation.categories.CategoryEditor
import app.habittracker.presentation.categories.CategoryListEffect
import app.habittracker.presentation.categories.CategoryListEvent
import app.habittracker.presentation.categories.CategoryListViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CategoryListScreen(
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: CategoryListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CategoryListEffect.ShowDomainError -> snackbar.showReplacing(scope, domainErrorMessage(effect.error))
                is CategoryListEffect.ShowError -> snackbar.showReplacing(scope, effect.message)
            }
        }
    }

    Column(modifier.fillMaxSize().background(HabitTheme.colors.canvas).padding(contentPadding)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text(
                "Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.onEvent(CategoryListEvent.AddClicked) }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add category")
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(state.categories, key = { it.id }) { category ->
                CategoryRow(
                    category,
                    onEdit = { viewModel.onEvent(CategoryListEvent.EditClicked(category.id)) },
                    onDelete = { viewModel.onEvent(CategoryListEvent.DeleteClicked(category.id)) },
                )
            }
        }
    }

    state.editor?.let { editor -> EditorDialog(editor, viewModel::onEvent) }
}

@Composable
private fun CategoryRow(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(HabitTheme.colors.card)
            .clickable(onClick = onEdit)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ColorDot(category.colorKey)
        Text(category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete ${category.name}")
        }
    }
}

@Composable
private fun ColorDot(colorKey: String, modifier: Modifier = Modifier) {
    Row(modifier.size(24.dp).background(categoryColor(colorKey, isSystemInDarkTheme()), CircleShape)) {}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorDialog(editor: CategoryEditor, onEvent: (CategoryListEvent) -> Unit) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    AlertDialog(
        onDismissRequest = { currentOnEvent(CategoryListEvent.DismissEditor) },
        title = { Text(if (editor.id == null) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { onEvent(CategoryListEvent.NameChanged(it)) },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = editor.error != null,
                    supportingText = editor.error?.let { error -> { Text(domainErrorMessage(error)) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorKeys.all.forEach { key ->
                        val selected = key == editor.colorKey
                        ColorDot(
                            key,
                            Modifier
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier,
                                )
                                .clickable { onEvent(CategoryListEvent.ColorChanged(key)) }
                                .semantics {
                                    contentDescription = "Color $key"
                                    this.selected = selected
                                },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(CategoryListEvent.SaveEditor) },
                enabled = !editor.isSaving && editor.name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { onEvent(CategoryListEvent.DismissEditor) }) { Text("Cancel") } },
    )
}
