package app.habittracker.domain.logic

import app.habittracker.MON_WED_FRI
import app.habittracker.date
import app.habittracker.domain.model.Completion
import app.habittracker.domain.model.WeekComparison
import app.habittracker.domain.model.toIndex
import app.habittracker.habit
import kotlin.test.Test
import kotlin.test.assertEquals

class WeekComparisonTest {
    private fun done(vararg pairs: Pair<String, String>) = pairs.map { (id, d) -> Completion(id, date(d)) }.toIndex()

    @Test fun thisWeekIsMondayToTodayAndLastWeekIsFullPreviousWeek() {
        // Today Wed 14 Oct 2026. This week: Mon 12, Tue 13 done; today not done yet → 2/2.
        // Last week Mon 5 – Sun 11: done on 3 of 7 days → 3/7.
        val h = habit("a", created = "2026-10-01")
        val c = done("a" to "2026-10-12", "a" to "2026-10-13", "a" to "2026-10-05", "a" to "2026-10-07", "a" to "2026-10-11")
        assertEquals(WeekComparison(1.0, 3.0 / 7), weekComparison(listOf(h), c, date("2026-10-14")))
    }

    @Test fun todayCountsOnlyWhenDone() {
        val h = habit("a", created = "2026-10-01")
        // Today is Monday 12: undone → no data this week; done → 1/1.
        assertEquals(null, weekComparison(listOf(h), done(), date("2026-10-12")).thisWeek)
        assertEquals(1.0, weekComparison(listOf(h), done("a" to "2026-10-12"), date("2026-10-12")).thisWeek)
    }

    @Test fun sundayClosesTheWeek() {
        // Today Sun 18: this week is Mon 12 – Sun 18; past days missed count, today undone does not.
        val h = habit("a", created = "2026-10-01")
        val c = done("a" to "2026-10-12", "a" to "2026-10-13", "a" to "2026-10-14")
        assertEquals(3.0 / 6, weekComparison(listOf(h), c, date("2026-10-18")).thisWeek)
    }

    @Test fun lastWeekWithoutHabitsIsNull() {
        val h = habit("a", created = "2026-10-12")
        assertEquals(null, weekComparison(listOf(h), done("a" to "2026-10-12"), date("2026-10-13")).lastWeek)
    }

    @Test fun unscheduledDaysDoNotCount() {
        // Mon/Wed/Fri habit; today Fri 16: Mon done, Wed missed, today undone → 1/2.
        val h = habit("a", created = "2026-10-01", days = MON_WED_FRI)
        assertEquals(0.5, weekComparison(listOf(h), done("a" to "2026-10-12"), date("2026-10-16")).thisWeek)
    }
}
