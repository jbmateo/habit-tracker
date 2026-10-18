package app.habittracker.data.db

import kotlinx.datetime.DayOfWeek

/** bit0 = Monday ... bit6 = Sunday. */
object DaysMask {
    fun encode(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl d.ordinal) }

    fun decode(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filter { mask and (1 shl it.ordinal) != 0 }.toSet()
}
