package app.habittracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.time.AppClock
import app.habittracker.domain.usecase.CelebrationMemory
import app.habittracker.domain.usecase.ObserveTodayDashboard
import app.habittracker.domain.usecase.ToggleCompletion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import app.habittracker.domain.usecase.SetTodaySort
import app.habittracker.domain.usecase.ObserveTodaySort
import app.habittracker.domain.logic.pendingMilestone
import app.habittracker.domain.logic.sortTodayItems
import app.habittracker.domain.model.MilestoneSeen
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
    observeSort: ObserveTodaySort,
    private val setSort: SetTodaySort,
    private val celebrations: CelebrationMemory,
    private val clock: AppClock,
) : ViewModel() {
    private val effectChannel = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = effectChannel.receiveAsFlow()

    // The previous emission of the current subscription; the celebrated day itself is stored, so it survives reopening.
    private var previous: Pair<LocalDate, Boolean>? = null

    val uiState: StateFlow<DashboardUiState> = observe()
        .onStart { previous = null }
        .map { dashboard ->
            val prev = previous
            previous = dashboard.date to dashboard.isPerfect
            val becamePerfect = dashboard.isPerfect && prev != null && prev.first == dashboard.date && !prev.second
            if (becamePerfect && celebrations.celebratedOn().first() != dashboard.date) {
                celebrations.markCelebrated(dashboard.date)
                effectChannel.trySend(DashboardEffect.Celebrate)
            }
            dashboard
        }
        .combine(observeSort()) { dashboard, sort -> dashboard to sort }
        .combine(celebrations.milestoneSeen()) { (dashboard, sort), seen ->
            if (dashboard.hasAnyHabits) {
                DashboardUiState.Content(
                    dashboard,
                    sortTodayItems(dashboard.groups, sort),
                    sort,
                    milestone = pendingMilestone(dashboard.streak.current, dashboard.streakRunStart, seen),
                )
            } else {
                DashboardUiState.NoHabits
            }
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
            DashboardEvent.DismissMilestone -> {
                val content = uiState.value as? DashboardUiState.Content ?: return
                val milestone = content.milestone ?: return
                val runStart = content.dashboard.streakRunStart ?: return
                viewModelScope.launch { celebrations.dismissMilestone(MilestoneSeen(runStart, milestone)) }
            }
            is DashboardEvent.SortSelected -> viewModelScope.launch {
                try {
                    setSort(event.sort)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    effectChannel.send(DashboardEffect.ShowError(e.message ?: "Something went wrong"))
                }
            }
        }
    }
}
