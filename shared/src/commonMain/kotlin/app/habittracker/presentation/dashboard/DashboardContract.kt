package app.habittracker.presentation.dashboard

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.TodaySort
import app.habittracker.domain.usecase.TodayDashboard
import app.habittracker.domain.usecase.TodayHabitItem

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    /** No active habits exist at all. */
    data object NoHabits : DashboardUiState

    /**
     * Habits exist; empty [TodayDashboard.groups] means nothing is due today.
     * @property items today's habits as one list, ordered by [sort].
     * @property milestone a streak milestone (7, 30, 100 or 365) to show until dismissed, or null.
     */
    data class Content(
        val dashboard: TodayDashboard,
        val items: List<TodayHabitItem>,
        val sort: TodaySort,
        val milestone: Int? = null,
    ) : DashboardUiState
}

sealed interface DashboardEvent {
    data class ToggleHabit(val id: String) : DashboardEvent
    data object AddHabitClicked : DashboardEvent
    data class HabitClicked(val id: String) : DashboardEvent
    data class SortSelected(val sort: TodaySort) : DashboardEvent
    data object DismissMilestone : DashboardEvent
}

sealed interface DashboardEffect {
    data object Celebrate : DashboardEffect
    data object OpenCreateHabit : DashboardEffect
    data class OpenHabit(val id: String) : DashboardEffect
    data class ShowDomainError(val error: DomainError) : DashboardEffect
    data class ShowError(val message: String) : DashboardEffect
}
