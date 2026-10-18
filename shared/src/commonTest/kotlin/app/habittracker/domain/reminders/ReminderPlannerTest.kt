package app.habittracker.domain.reminders

import app.habittracker.MON_WED_FRI
import app.habittracker.date
import app.habittracker.domain.model.ArchivePeriod
import app.habittracker.domain.model.Completion
import app.habittracker.domain.model.IconKeys
import app.habittracker.domain.model.toIndex
import app.habittracker.habit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderPlannerTest {
    // Wednesday 14 Oct 2026, 08:00. Window: 14–20 Oct.
    private val now = LocalDateTime(2026, 10, 14, 8, 0)
    private fun done(vararg pairs: Pair<String, String>) = pairs.map { (id, d) -> Completion(id, date(d)) }.toIndex()

    @Test fun onlyHabitsWithReminderTime() {
        val plan = planReminders(listOf(habit("a", created = "2026-10-01"), habit("b", created = "2026-10-01", reminder = "09:00")), done(), now)
        assertTrue(plan.all { it.habitId == "b" })
        assertEquals(7, plan.size)
    }

    @Test fun scheduledDaysOnly() {
        val h = habit("a", created = "2026-10-01", days = MON_WED_FRI, reminder = "09:00")
        assertEquals(listOf("2026-10-14", "2026-10-16", "2026-10-19"), planReminders(listOf(h), done(), now).map { it.date.toString() })
    }

    @Test fun pastTimeTodaySkippedTomorrowKept() {
        val h = habit("a", created = "2026-10-01", reminder = "07:00")
        assertEquals(date("2026-10-15"), planReminders(listOf(h), done(), now).first().date)
    }

    @Test fun doneDaysSkipped() {
        val h = habit("a", created = "2026-10-01", reminder = "09:00")
        val dates = planReminders(listOf(h), done("a" to "2026-10-15"), now).map { it.date.toString() }
        assertTrue("2026-10-15" !in dates)
        assertTrue("2026-10-14" in dates)
    }

    @Test fun archivedDaysSkipped() {
        val h = habit("a", created = "2026-10-01", reminder = "09:00", archived = listOf(ArchivePeriod(date("2026-10-16"), null)))
        assertEquals(listOf("2026-10-14", "2026-10-15"), planReminders(listOf(h), done(), now).map { it.date.toString() })
    }

    @Test fun notBeforeCreatedOn() {
        val h = habit("a", created = "2026-10-17", reminder = "09:00")
        assertEquals(date("2026-10-17"), planReminders(listOf(h), done(), now).first().date)
    }

    @Test fun sortedByDateThenTimeThenName() {
        val plan = planReminders(
            listOf(
                habit("z", name = "Zen", created = "2026-10-01", reminder = "09:00"),
                habit("a", name = "Art", created = "2026-10-01", reminder = "09:00"),
                habit("e", name = "Early", created = "2026-10-01", reminder = "08:30"),
            ),
            done(), now,
        )
        assertEquals(listOf("Early", "Art", "Zen"), plan.take(3).map { it.habitName })
    }

    @Test fun cappedAtSixtyKeepingEarliest() {
        val habits = (1..10).map { habit("h$it", name = "H$it", created = "2026-10-01", reminder = "09:00") }
        val plan = planReminders(habits, done(), now)
        assertEquals(MAX_PLANNED_REMINDERS, plan.size)
        assertTrue(plan.all { it.date <= date("2026-10-19") })
        assertEquals(date("2026-10-14"), plan.first().date)
    }

    @Test fun idTitleAndBody() {
        val h = habit("a", name = "Run", iconKey = "run", created = "2026-10-01", reminder = "09:00")
        // Done 11–13 Oct → current streak 3 (today undone doesn't break it).
        val r = planReminders(listOf(h), done("a" to "2026-10-11", "a" to "2026-10-12", "a" to "2026-10-13"), now).first()
        assertEquals("habit:a@2026-10-14", r.id)
        assertEquals(LocalTime(9, 0), r.time)
        assertEquals("🏃 Run", r.title)
        assertEquals("Time for your habit — keep your 🔥 3-day streak going!", r.body)
        assertEquals("Time for your habit", r.copy(streak = 0).body)
    }

    @Test fun emojiFallback() {
        assertEquals("✅", ReminderEmoji.forIconKey("rocket"))
        IconKeys.all.forEach { assertTrue(ReminderEmoji.forIconKey(it).isNotBlank(), "no emoji for $it") }
        assertEquals("🏃", ReminderEmoji.forIconKey("run"))
    }
}
