package app.habittracker.domain.usecase

import app.habittracker.domain.model.TodaySort
import app.habittracker.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveTodaySort(private val prefs: PreferencesRepository) {
    operator fun invoke(): Flow<TodaySort> = prefs.observeTodaySort()
}
