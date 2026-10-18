package app.habittracker.presentation.dashboard

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.usecase.TodayDashboard

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    /** No active habits exist at all. */
    data object NoHabits : DashboardUiState

    /** Habits exist; empty [TodayDashboard.groups] means nothing is due today. */
    data class Content(val dashboard: TodayDashboard) : DashboardUiState
}

sealed interface DashboardEvent {
    data class ToggleHabit(val id: String) : DashboardEvent
    data object AddHabitClicked : DashboardEvent
    data class HabitClicked(val id: String) : DashboardEvent
}

sealed interface DashboardEffect {
    data object Celebrate : DashboardEffect
    data object OpenCreateHabit : DashboardEffect
    data class OpenHabit(val id: String) : DashboardEffect
    data class ShowDomainError(val error: DomainError) : DashboardEffect
    data class ShowError(val message: String) : DashboardEffect
}
