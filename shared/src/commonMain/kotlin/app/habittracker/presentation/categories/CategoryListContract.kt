package app.habittracker.presentation.categories

import app.habittracker.domain.model.Category
import app.habittracker.domain.model.DomainError

/** @property editor the open create/edit form, or `null` when closed. */
data class CategoryListUiState(val categories: List<Category>, val editor: CategoryEditor?)

/** @property id `null` while creating a new category. @property error last save failure, shown inline. */
data class CategoryEditor(val id: String?, val name: String, val colorKey: String, val error: DomainError?)

sealed interface CategoryListEvent {
    data object AddClicked : CategoryListEvent
    data class EditClicked(val id: String) : CategoryListEvent
    data class NameChanged(val v: String) : CategoryListEvent
    data class ColorChanged(val key: String) : CategoryListEvent
    data object SaveEditor : CategoryListEvent
    data object DismissEditor : CategoryListEvent
    data class DeleteClicked(val id: String) : CategoryListEvent
}

sealed interface CategoryListEffect {
    data class ShowDomainError(val error: DomainError) : CategoryListEffect
    data class ShowError(val message: String) : CategoryListEffect
}
