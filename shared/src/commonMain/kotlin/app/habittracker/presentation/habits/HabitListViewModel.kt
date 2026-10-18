package app.habittracker.presentation.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.habittracker.domain.usecase.ObserveHabits
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

class HabitListViewModel(observe: ObserveHabits) : ViewModel() {
    private val effectChannel = Channel<HabitListEffect>(Channel.BUFFERED)
    val effects: Flow<HabitListEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<HabitListUiState> = observe()
        .map<_, HabitListUiState> { HabitListUiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitListUiState.Loading)

    fun onEvent(event: HabitListEvent) {
        when (event) {
            is HabitListEvent.HabitClicked -> effectChannel.trySend(HabitListEffect.OpenHabit(event.id))
            HabitListEvent.AddHabitClicked -> effectChannel.trySend(HabitListEffect.OpenCreateHabit)
            HabitListEvent.ManageCategoriesClicked -> effectChannel.trySend(HabitListEffect.OpenCategories)
        }
    }
}
