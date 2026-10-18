package app.habittracker.data.time

import app.habittracker.domain.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class SystemAppClock(
    private val clock: Clock,
    private val timeZone: () -> TimeZone,
    changes: DayChangeSource,
) : AppClock {
    override fun today(): LocalDate = clock.todayIn(timeZone())

    override val todayFlow: Flow<LocalDate> =
        changes.changes
            .onStart { emit(Unit) }
            .map { today() }
            .distinctUntilChanged()
}
