package app.habittracker.domain.logic

import app.habittracker.date
import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationTest {
    @Test fun nameIsTrimmed() = assertEquals(Outcome.Success("Run"), validateName("  Run  "))

    @Test fun blankNameIsInvalid() =
        assertEquals(Outcome.Failure(DomainError.InvalidName), validateName("   "))

    @Test fun nameOfFortyCharsIsValid() =
        assertEquals(Outcome.Success("a".repeat(40)), validateName("a".repeat(40)))

    @Test fun nameOfFortyOneCharsIsInvalid() =
        assertEquals(Outcome.Failure(DomainError.InvalidName), validateName("a".repeat(41)))

    @Test fun emptyDaysAreRejected() =
        assertEquals(Outcome.Failure(DomainError.NoDaysSelected), validateDays(emptySet()))

    @Test fun nonEmptyDaysAreAccepted() {
        val days = setOf(DayOfWeek.MONDAY)
        assertEquals(Outcome.Success(days), validateDays(days))
    }

    @Test fun editWindowIsTodayAndYesterday() {
        val today = date("2026-03-10")
        assertTrue(isEditable(date("2026-03-10"), today))
        assertTrue(isEditable(date("2026-03-09"), today))
        assertFalse(isEditable(date("2026-03-08"), today))
        assertFalse(isEditable(date("2026-03-11"), today))
    }

    @Test fun editWindowCrossesYearBoundary() =
        assertTrue(isEditable(date("2025-12-31"), date("2026-01-01")))
}
