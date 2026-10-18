package app.habittracker.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.habittracker.domain.model.ColorKeys
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.usecase.CreateCategory
import app.habittracker.domain.usecase.DeleteCategory
import app.habittracker.domain.usecase.ObserveCategories
import app.habittracker.domain.usecase.UpdateCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryListViewModel(
    private val observe: ObserveCategories,
    private val create: CreateCategory,
    private val update: UpdateCategory,
    private val delete: DeleteCategory,
) : ViewModel() {
    private val effectChannel = Channel<CategoryListEffect>(Channel.BUFFERED)
    val effects: Flow<CategoryListEffect> = effectChannel.receiveAsFlow()

    // Local editor state. Read-then-write in onEvent and in the launched save relies on the Main
    // dispatcher serialising access (no lock); isSaving de-duplicates a double-tapped Save.
    private val editor = MutableStateFlow<CategoryEditor?>(null)

    val uiState: StateFlow<CategoryListUiState> =
        combine(observe(), editor) { categories, editor -> CategoryListUiState(categories, editor) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CategoryListUiState(emptyList(), null),
            )

    fun onEvent(event: CategoryListEvent) {
        when (event) {
            CategoryListEvent.AddClicked -> editor.value = CategoryEditor(null, "", DEFAULT_COLOR, null)
            is CategoryListEvent.EditClicked -> viewModelScope.launch {
                val category = observe().first().firstOrNull { it.id == event.id } ?: return@launch
                editor.value = CategoryEditor(category.id, category.name, ColorKeys.resolve(category.colorKey), null)
            }
            // Editing clears the stale save error.
            is CategoryListEvent.NameChanged -> editor.update { it?.copy(name = event.v, error = null) }
            is CategoryListEvent.ColorChanged -> editor.update { it?.copy(colorKey = event.key, error = null) }
            CategoryListEvent.DismissEditor -> editor.value = null
            CategoryListEvent.SaveEditor -> save()
            is CategoryListEvent.DeleteClicked -> viewModelScope.launch {
                guarded { outcome(delete(event.id)) }
            }
        }
    }

    private fun save() {
        val current = editor.value ?: return
        if (current.isSaving) return
        editor.value = current.copy(isSaving = true)
        viewModelScope.launch {
            guarded {
                val result = if (current.id == null) {
                    when (val r = create(current.name, current.colorKey)) {
                        is Outcome.Success -> Outcome.Success(Unit)
                        is Outcome.Failure -> r
                    }
                } else {
                    update(current.id, current.name, current.colorKey)
                }
                when (result) {
                    is Outcome.Success -> editor.value = null
                    is Outcome.Failure -> editor.update { it?.copy(error = result.error, isSaving = false) }
                }
            }
            editor.update { it?.copy(isSaving = false) }
        }
    }

    private fun outcome(result: Outcome<Unit>) {
        if (result is Outcome.Failure) effectChannel.trySend(CategoryListEffect.ShowDomainError(result.error))
    }

    private suspend fun guarded(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            effectChannel.send(CategoryListEffect.ShowError(e.message ?: "Something went wrong"))
        }
    }

    private companion object {
        const val DEFAULT_COLOR = "blue"
    }
}
