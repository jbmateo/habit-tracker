package app.habittracker.domain.usecase

import app.habittracker.domain.model.MilestoneSeen
import app.habittracker.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** What the Today screen has already celebrated: the day of the last confetti and the last dismissed milestone. */
class CelebrationMemory(private val prefs: PreferencesRepository) {
    fun celebratedOn(): Flow<LocalDate?> = prefs.observeCelebratedOn()
    fun milestoneSeen(): Flow<MilestoneSeen?> = prefs.observeMilestoneSeen()
    suspend fun markCelebrated(date: LocalDate) = prefs.setCelebratedOn(date)
    suspend fun dismissMilestone(seen: MilestoneSeen) = prefs.setMilestoneSeen(seen)
}
