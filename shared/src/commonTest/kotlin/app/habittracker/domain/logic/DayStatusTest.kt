package app.habittracker.domain.logic

import app.habittracker.date
import app.habittracker.domain.model.DayStatus
import app.habittracker.habit
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals

class DayStatusTest {
    private val today = date("2026-01-07") // Wednesday
    private val past = date("2026-01-06") // Tuesday

    @Test
    fun futureDay() {
        val status = dayStatus(date("2026-01-08"), today, listOf(habit()), emptyMap())
        assertEquals(DayStatus.Future, status)
    }

    @Test
    fun noHabitsDueIsNoHabits() {
        val h = habit(days = setOf(DayOfWeek.MONDAY))
        assertEquals(DayStatus.NoHabits, dayStatus(past, today, listOf(h), emptyMap()))
        assertEquals(DayStatus.NoHabits, dayStatus(today, today, listOf(h), emptyMap()))
    }

    @Test
    fun allDoneIsPerfect() {
        val habits = listOf(habit("a"), habit("b"))
        val done = mapOf(past to setOf("a", "b"), today to setOf("a", "b"))
        assertEquals(DayStatus.Perfect(2), dayStatus(past, today, habits, done))
        assertEquals(DayStatus.Perfect(2), dayStatus(today, today, habits, done))
    }

    @Test
    fun todayIncompleteIsInProgress() {
        val habits = listOf(habit("a"), habit("b"))
        val status = dayStatus(today, today, habits, mapOf(today to setOf("a")))
        assertEquals(DayStatus.InProgress(done = 1, due = 2), status)
    }

    @Test
    fun pastSomeDoneIsPartial() {
        val habits = listOf(habit("a"), habit("b"))
        val status = dayStatus(past, today, habits, mapOf(past to setOf("a")))
        assertEquals(DayStatus.Partial(done = 1, due = 2), status)
    }

    @Test
    fun pastNoneDoneIsMissed() {
        val habits = listOf(habit("a"), habit("b"))
        assertEquals(DayStatus.Missed(2), dayStatus(past, today, habits, emptyMap()))
    }

    @Test
    fun completionOnNonDueDayIgnored() {
        val monOnly = habit("a", days = setOf(DayOfWeek.MONDAY))
        val tueOnly = habit("b", days = setOf(DayOfWeek.TUESDAY))
        val status = dayStatus(past, today, listOf(monOnly, tueOnly), mapOf(past to setOf("a")))
        assertEquals(DayStatus.Missed(1), status)
    }
}
