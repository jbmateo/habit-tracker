package app.habittracker.data

import app.habittracker.data.db.DaysMask
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals

class DaysMaskTest {
    @Test fun encodeAllIs127() = assertEquals(127, DaysMask.encode(DayOfWeek.entries.toSet()))
    @Test fun encodeMonday() = assertEquals(1, DaysMask.encode(setOf(DayOfWeek.MONDAY)))
    @Test fun encodeSunday() = assertEquals(64, DaysMask.encode(setOf(DayOfWeek.SUNDAY)))
    @Test fun roundTripMonWedFri() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals(21, DaysMask.encode(days))
        assertEquals(days, DaysMask.decode(21))
    }
}
