package app.habittracker.data.time

import kotlinx.coroutines.flow.Flow

/** Platform signals that the current day may have changed (midnight, clock/timezone change, app foregrounded). */
interface DayChangeSource {
    val changes: Flow<Unit>
}
