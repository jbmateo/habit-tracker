package app.habittracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.time.AppClock
import app.habittracker.domain.usecase.ObserveTodayDashboard
import app.habittracker.domain.usecase.ToggleCompletion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class DashboardViewModel(
    observe: ObserveTodayDashboard,
    private val toggle: ToggleCompletion,
    private val clock: AppClock,
) : ViewModel() {
    private val effectChannel = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = effectChannel.receiveAsFlow()

    // In-memory only: dates already celebrated, and the previous emission of the current subscription.
    private val celebrated = mutableSetOf<LocalDate>()
    private var previous: Pair<LocalDate, Boolean>? = null

    val uiState: StateFlow<DashboardUiState> = observe()
        .onStart { previous = null }
        .map { dashboard ->
            val prev = previous
            previous = dashboard.date to dashboard.isPerfect
            if (dashboard.isPerfect && prev != null && prev.first == dashboard.date && !prev.second &&
                celebrated.add(dashboard.date)
            ) {
                effectChannel.trySend(DashboardEffect.Celebrate)
            }
            if (dashboard.hasAnyHabits) DashboardUiState.Content(dashboard) else DashboardUiState.NoHabits
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.ToggleHabit -> viewModelScope.launch {
                try {
                    val outcome = toggle(event.id, clock.today())
                    if (outcome is Outcome.Failure) {
                        effectChannel.send(DashboardEffect.ShowDomainError(outcome.error))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    effectChannel.send(DashboardEffect.ShowError(e.message ?: "Something went wrong"))
                }
            }
            DashboardEvent.AddHabitClicked -> effectChannel.trySend(DashboardEffect.OpenCreateHabit)
            is DashboardEvent.HabitClicked -> effectChannel.trySend(DashboardEffect.OpenHabit(event.id))
        }
    }
}
