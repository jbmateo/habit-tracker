package app.habittracker.data.time

import app.habittracker.domain.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate

/** A clock pinned to one date, so UI tests behave the same whenever they run (e.g. across midnight). */
class FixedAppClock(private val date: LocalDate) : AppClock {
    override fun today(): LocalDate = date
    override val todayFlow: Flow<LocalDate> = flowOf(date)
}
