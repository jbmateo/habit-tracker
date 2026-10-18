package app.habittracker.presentation.detail

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.usecase.HabitDetail

sealed interface HabitDetailUiState {
    data object Loading : HabitDetailUiState

    /** The habit no longer exists; the screen should close. */
    data object Deleted : HabitDetailUiState

    /**
     * @property leadingBlanks empty grid cells before the 1st, Monday-first (Monday = 0).
     * @property canGoNext `true` only when the shown month is before the current month.
     */
    data class Content(
        val detail: HabitDetail,
        val leadingBlanks: Int,
        val canGoNext: Boolean,
        val confirmingDelete: Boolean,
    ) : HabitDetailUiState
}

sealed interface HabitDetailEvent {
    data object PrevMonth : HabitDetailEvent
    data object NextMonth : HabitDetailEvent
    data object EditClicked : HabitDetailEvent
    data object ArchiveToggled : HabitDetailEvent
    data object DeleteClicked : HabitDetailEvent
    data object DeleteConfirmed : HabitDetailEvent
    data object DeleteCancelled : HabitDetailEvent
}

sealed interface HabitDetailEffect {
    data class OpenEdit(val id: String) : HabitDetailEffect
    data object Close : HabitDetailEffect
    data class ShowDomainError(val error: DomainError) : HabitDetailEffect
    data class ShowError(val message: String) : HabitDetailEffect
}
