package app.habittracker.presentation.habits

import app.habittracker.domain.usecase.HabitsOverview

sealed interface HabitListUiState {
    data object Loading : HabitListUiState
    data class Content(val overview: HabitsOverview) : HabitListUiState
}

sealed interface HabitListEvent {
    data class HabitClicked(val id: String) : HabitListEvent
    data object AddHabitClicked : HabitListEvent
    data object ManageCategoriesClicked : HabitListEvent
}

sealed interface HabitListEffect {
    data class OpenHabit(val id: String) : HabitListEffect
    data object OpenCreateHabit : HabitListEffect
    data object OpenCategories : HabitListEffect
}
