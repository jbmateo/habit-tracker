package app.habittracker.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.habittracker.domain.logic.validateName
import app.habittracker.domain.model.Category
import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.IconKeys
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.usecase.CreateHabit
import app.habittracker.domain.usecase.GetHabit
import app.habittracker.domain.usecase.ObserveCategories
import app.habittracker.domain.usecase.UpdateHabit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/** @param habitId the habit to edit, or `null` to create a new one. */
class HabitFormViewModel(
    private val habitId: String?,
    private val getHabit: GetHabit,
    observeCategories: ObserveCategories,
    private val create: CreateHabit,
    private val update: UpdateHabit,
) : ViewModel() {
    private val effectChannel = Channel<HabitFormEffect>(Channel.BUFFERED)
    val effects: Flow<HabitFormEffect> = effectChannel.receiveAsFlow()

    private data class Draft(
        val name: String = "",
        val categoryId: String? = null,
        val iconKey: String = IconKeys.FALLBACK,
        val days: Set<DayOfWeek> = ALL_DAYS,
        val nameTouched: Boolean = false,
        val saveAttempted: Boolean = false,
        val isSaving: Boolean = false,
        val reminderEnabled: Boolean = false,
        val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    )

    // Read-then-write in onEvent relies on callers using the Main dispatcher (no lock).
    private val draft = MutableStateFlow(Draft())
    private val categories = MutableStateFlow<List<Category>?>(null)
    private val pending = MutableStateFlow(habitId != null) // edit mode: habit not loaded yet
    private val loadedDays = MutableStateFlow<Set<DayOfWeek>?>(null)

    val uiState: StateFlow<HabitFormUiState> =
        combine(draft, categories, pending, loadedDays) { _, _, _, _ -> render() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), render())

    init {
        viewModelScope.launch {
            observeCategories().collect { categories.value = it }
        }
        if (habitId != null) viewModelScope.launch { load(habitId) }
    }

    private suspend fun load(id: String) {
        val habit: Habit? = try {
            getHabit(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            effectChannel.send(HabitFormEffect.ShowError(e.message ?: "Something went wrong"))
            effectChannel.send(HabitFormEffect.Dismiss)
            return
        }
        if (habit == null) {
            effectChannel.send(HabitFormEffect.ShowDomainError(DomainError.HabitNotFound))
            effectChannel.send(HabitFormEffect.Dismiss)
            return
        }
        val days = habit.currentSchedule.days
        draft.update {
            it.copy(
                name = habit.name,
                categoryId = habit.category.id,
                iconKey = IconKeys.resolve(habit.iconKey),
                days = days,
                reminderEnabled = habit.reminderTime != null,
                reminderTime = habit.reminderTime ?: DEFAULT_REMINDER_TIME,
            )
        }
        loadedDays.value = days
        pending.value = false
    }

    private fun render(): HabitFormUiState {
        val d = draft.value
        val list = categories.value
        val loading = list == null || pending.value
        val all = list.orEmpty()
        // A selection that is gone from the list falls back to the first category.
        val categoryId = d.categoryId?.takeIf { id -> all.any { it.id == id } } ?: all.firstOrNull()?.id
        val nameResult = validateName(d.name)
        val nameError = (nameResult as? Outcome.Failure)?.error?.takeIf { d.nameTouched || d.saveAttempted }
        val original = loadedDays.value
        return HabitFormUiState(
            isEdit = habitId != null,
            isLoading = loading,
            name = d.name,
            categoryId = categoryId,
            iconKey = d.iconKey,
            days = d.days,
            categories = all,
            iconKeys = IconKeys.all,
            nameError = nameError,
            canSave = nameResult is Outcome.Success && d.days.isNotEmpty() && categoryId != null &&
                !d.isSaving && !loading,
            showScheduleNote = habitId != null && original != null && d.days != original,
            isSaving = d.isSaving,
            reminderEnabled = d.reminderEnabled,
            reminderTime = d.reminderTime,
        )
    }

    fun onEvent(event: HabitFormEvent) {
        when (event) {
            is HabitFormEvent.NameChanged ->
                draft.update { it.copy(name = event.v, nameTouched = it.nameTouched || event.v.isNotBlank()) }
            is HabitFormEvent.CategorySelected -> draft.update { it.copy(categoryId = event.id) }
            is HabitFormEvent.IconSelected -> draft.update { it.copy(iconKey = event.key) }
            is HabitFormEvent.DayToggled -> draft.update {
                it.copy(days = if (event.day in it.days) it.days - event.day else it.days + event.day)
            }
            HabitFormEvent.EveryDayToggled -> draft.update {
                it.copy(days = if (it.days == ALL_DAYS) setOf(DayOfWeek.MONDAY) else ALL_DAYS)
            }
            HabitFormEvent.ReminderToggled -> draft.update { it.copy(reminderEnabled = !it.reminderEnabled) }
            is HabitFormEvent.ReminderTimeChanged -> draft.update { it.copy(reminderTime = event.time) }
            HabitFormEvent.Save -> save()
        }
    }

    private fun save() {
        val state = render()
        if (state.isSaving || state.isLoading) return
        val categoryId = state.categoryId
        if (!state.canSave || categoryId == null) {
            draft.update { it.copy(saveAttempted = true) }
            return
        }
        draft.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val result: Outcome<*> = if (habitId == null) {
                    create(state.name, state.iconKey, categoryId, state.days, state.reminderTimeOrNull())
                } else {
                    update(habitId, state.name, state.iconKey, categoryId, state.days, state.reminderTimeOrNull())
                }
                when (result) {
                    is Outcome.Success -> effectChannel.send(HabitFormEffect.Dismiss)
                    is Outcome.Failure -> {
                        draft.update { it.copy(isSaving = false, saveAttempted = true) }
                        if (result.error != DomainError.InvalidName) {
                            effectChannel.send(HabitFormEffect.ShowDomainError(result.error))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                draft.update { it.copy(isSaving = false) }
                effectChannel.send(HabitFormEffect.ShowError(e.message ?: "Something went wrong"))
            }
        }
    }

    private companion object {
        val ALL_DAYS: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    }
}

private fun HabitFormUiState.reminderTimeOrNull(): LocalTime? = if (reminderEnabled) reminderTime else null
