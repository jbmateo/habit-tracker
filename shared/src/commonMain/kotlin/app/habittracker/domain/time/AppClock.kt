package app.habittracker.domain.time

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface AppClock {
    fun today(): LocalDate
    val todayFlow: Flow<LocalDate>
}
