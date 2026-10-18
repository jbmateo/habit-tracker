package app.habittracker.data.time

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class SystemAppClockTest {
    private class MutableClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    private class ManualChanges : DayChangeSource {
        val signals = MutableSharedFlow<Unit>()
        override val changes: Flow<Unit> = signals
    }

    @Test
    fun todayFlowEmitsOnChangeOnlyWhenDateDiffers() = runTest {
        val clock = MutableClock(Instant.parse("2026-10-18T23:59:00Z"))
        val changes = ManualChanges()
        val appClock = SystemAppClock(clock, { TimeZone.UTC }, changes)

        appClock.todayFlow.test {
            assertEquals(LocalDate(2026, 10, 18), awaitItem())
            changes.signals.emit(Unit) // same day: suppressed
            clock.now = Instant.parse("2026-10-19T00:01:00Z")
            changes.signals.emit(Unit)
            assertEquals(LocalDate(2026, 10, 19), awaitItem())
            changes.signals.emit(Unit) // same day again: suppressed
            expectNoEvents()
        }
    }

    @Test
    fun todayUsesCurrentTimeZone() {
        val clock = MutableClock(Instant.parse("2026-10-18T23:30:00Z"))
        var zone: TimeZone = TimeZone.UTC
        val appClock = SystemAppClock(clock, { zone }, ManualChanges())
        assertEquals(LocalDate(2026, 10, 18), appClock.today())
        zone = TimeZone.of("Asia/Tokyo")
        assertEquals(LocalDate(2026, 10, 19), appClock.today())
    }
}
