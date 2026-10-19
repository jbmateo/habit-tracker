package app.habittracker.domain.repository

import app.habittracker.domain.model.MilestoneSeen
import app.habittracker.domain.model.TodaySort
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Small on-device user preferences. */
interface PreferencesRepository {
    /** Emits the remembered Today sort; [TodaySort.Category] until one is chosen. */
    fun observeTodaySort(): Flow<TodaySort>
    suspend fun setTodaySort(sort: TodaySort)

    /** The last day the all-done confetti played; null if never. */
    fun observeCelebratedOn(): Flow<LocalDate?>
    suspend fun setCelebratedOn(date: LocalDate)

    /** The last dismissed streak milestone; null if none, or if the stored value can't be read. */
    fun observeMilestoneSeen(): Flow<MilestoneSeen?>
    suspend fun setMilestoneSeen(seen: MilestoneSeen)
}
