package app.habittracker.presentation.calendar

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.usecase.DayDetail
import app.habittracker.domain.usecase.MonthCalendar
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/**
 * @property leadingBlanks empty grid cells before the 1st, Monday-first (Monday = 0).
 * @property calendar `null` while the month is loading.
 * @property selected detail of the selected day, or `null` when nothing is selected (or it is loading).
 * @property canGoNext `true` only when [month] is before the current month.
 * @property bands streak-band position per day, aligned with `calendar.days`; empty while loading.
 */
data class CalendarUiState(
    val month: YearMonth,
    val leadingBlanks: Int,
    val calendar: MonthCalendar?,
    val selected: DayDetail?,
    val canGoNext: Boolean,
    val bands: List<StreakBand> = emptyList(),
)

sealed interface CalendarEvent {
    data object PrevMonth : CalendarEvent
    data object NextMonth : CalendarEvent
    data class SelectDay(val date: LocalDate) : CalendarEvent
    data object ClearSelection : CalendarEvent
    data class ToggleHabit(val id: String, val date: LocalDate) : CalendarEvent
}

sealed interface CalendarEffect {
    data class ShowDomainError(val error: DomainError) : CalendarEffect
    data class ShowError(val message: String) : CalendarEffect
}
