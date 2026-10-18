package app.habittracker.presentation.form

import app.habittracker.domain.model.Category
import app.habittracker.domain.model.DomainError
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * @property isLoading true until the categories (and, when editing, the habit) have loaded.
 * @property nameError shown only after the first non-blank edit or a Save attempt.
 * @property showScheduleNote editing with changed days: the change applies from today onward.
 */
data class HabitFormUiState(
    val isEdit: Boolean,
    val isLoading: Boolean,
    val name: String,
    val categoryId: String?,
    val iconKey: String,
    val days: Set<DayOfWeek>,
    val categories: List<Category>,
    val iconKeys: List<String>,
    val nameError: DomainError?,
    val canSave: Boolean,
    val showScheduleNote: Boolean,
    val isSaving: Boolean,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
)

/** Pre-filled time when a reminder is first switched on. */
val DEFAULT_REMINDER_TIME = LocalTime(9, 0)

sealed interface HabitFormEvent {
    data class NameChanged(val v: String) : HabitFormEvent
    data class CategorySelected(val id: String) : HabitFormEvent
    data class IconSelected(val key: String) : HabitFormEvent
    data class DayToggled(val day: DayOfWeek) : HabitFormEvent
    data object EveryDayToggled : HabitFormEvent
    data object ReminderToggled : HabitFormEvent
    data class ReminderTimeChanged(val time: LocalTime) : HabitFormEvent
    data object Save : HabitFormEvent
}

sealed interface HabitFormEffect {
    data object Dismiss : HabitFormEffect
    data class ShowDomainError(val error: DomainError) : HabitFormEffect
    data class ShowError(val message: String) : HabitFormEffect
}
