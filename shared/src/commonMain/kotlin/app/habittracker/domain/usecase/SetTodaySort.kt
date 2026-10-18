package app.habittracker.domain.usecase

import app.habittracker.domain.model.TodaySort
import app.habittracker.domain.repository.PreferencesRepository

class SetTodaySort(private val prefs: PreferencesRepository) {
    suspend operator fun invoke(sort: TodaySort) = prefs.setTodaySort(sort)
}
