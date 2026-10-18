package app.habittracker.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.time.AppClock
import app.habittracker.domain.usecase.ArchiveHabit
import app.habittracker.domain.usecase.DeleteHabit
import app.habittracker.domain.usecase.HabitDetail
import app.habittracker.domain.usecase.ObserveHabitDetail
import app.habittracker.domain.usecase.UnarchiveHabit
import app.habittracker.presentation.calendar.habitStreakBands
import app.habittracker.presentation.leadingBlanks
import app.habittracker.presentation.toYearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModel(
    private val habitId: String,
    private val observe: ObserveHabitDetail,
    private val archive: ArchiveHabit,
    private val unarchive: UnarchiveHabit,
    private val delete: DeleteHabit,
    private val clock: AppClock,
) : ViewModel() {
    private val effectChannel = Channel<HabitDetailEffect>(Channel.BUFFERED)
    val effects: Flow<HabitDetailEffect> = effectChannel.receiveAsFlow()

    // Mutated only from onEvent, which callers invoke on the Main dispatcher; the read-then-write
    // in onEvent is therefore not atomic and relies on that single-threaded access.
    private val month = MutableStateFlow(clock.today().toYearMonth())
    private val confirmingDelete = MutableStateFlow(false)

    /**
     * Each [HabitDetail] carries its own month, so state never pairs one month's grid with another
     * month's header; the previous month stays on screen until the next one arrives.
     */
    private val detail: Flow<HabitDetail?> = month.flatMapLatest { observe(habitId, it) }

    val uiState: StateFlow<HabitDetailUiState> =
        combine(detail, confirmingDelete, clock.todayFlow) { detail, confirming, today ->
            if (detail == null) {
                HabitDetailUiState.Deleted
            } else {
                HabitDetailUiState.Content(
                    detail = detail,
                    leadingBlanks = detail.month.leadingBlanks,
                    canGoNext = detail.month < today.toYearMonth(),
                    confirmingDelete = confirming,
                    bands = habitStreakBands(detail.days),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState.Loading)

    fun onEvent(event: HabitDetailEvent) {
        when (event) {
            HabitDetailEvent.PrevMonth -> month.value = month.value.minus(1, DateTimeUnit.MONTH)
            HabitDetailEvent.NextMonth -> if (month.value < clock.today().toYearMonth()) {
                month.value = month.value.plus(1, DateTimeUnit.MONTH)
            }
            HabitDetailEvent.EditClicked -> send(HabitDetailEffect.OpenEdit(habitId))
            HabitDetailEvent.ArchiveToggled -> run {
                val archived = (uiState.value as? HabitDetailUiState.Content)?.detail?.habit?.isArchived
                    ?: return@run
                perform { if (archived) unarchive(habitId) else archive(habitId) }
            }
            HabitDetailEvent.DeleteClicked -> confirmingDelete.value = true
            HabitDetailEvent.DeleteCancelled -> confirmingDelete.value = false
            HabitDetailEvent.DeleteConfirmed -> {
                confirmingDelete.value = false
                perform(onSuccess = HabitDetailEffect.Close) { delete(habitId) }
            }
        }
    }

    private fun send(effect: HabitDetailEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    private fun perform(onSuccess: HabitDetailEffect? = null, block: suspend () -> Outcome<Unit>) {
        viewModelScope.launch {
            try {
                when (val outcome = block()) {
                    is Outcome.Failure -> effectChannel.send(HabitDetailEffect.ShowDomainError(outcome.error))
                    is Outcome.Success -> onSuccess?.let { effectChannel.send(it) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                effectChannel.send(HabitDetailEffect.ShowError(e.message ?: "Something went wrong"))
            }
        }
    }
}
