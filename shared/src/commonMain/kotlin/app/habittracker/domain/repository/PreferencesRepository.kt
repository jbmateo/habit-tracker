package app.habittracker.domain.repository

import app.habittracker.domain.model.TodaySort
import kotlinx.coroutines.flow.Flow

/** Small on-device user preferences. */
interface PreferencesRepository {
    /** Emits the remembered Today sort; [TodaySort.Category] until one is chosen. */
    fun observeTodaySort(): Flow<TodaySort>
    suspend fun setTodaySort(sort: TodaySort)
}
