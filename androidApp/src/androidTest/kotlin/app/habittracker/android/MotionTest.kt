package app.habittracker.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Dp
import app.habittracker.android.ui.calendar.DayPanel
import app.habittracker.android.ui.common.HabitRow
import app.habittracker.android.ui.theme.HabitTheme
import app.habittracker.domain.model.Category
import app.habittracker.domain.model.CategoryGroup
import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.ScheduleVersion
import app.habittracker.domain.usecase.DayDetail
import app.habittracker.domain.usecase.DayHabitItem
import app.habittracker.domain.usecase.TodayHabitItem
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import org.junit.Assert.assertTrue

/** Check-off motion must settle back to rest and only play for real changes. */
class MotionTest {
    @get:Rule val rule = createComposeRule()

    private val health = Category("c1", "Health", "green", 0)
    private val read = Habit(
        id = "h1", name = "Read", iconKey = "book", category = health, createdOn = LocalDate(2026, 10, 1),
        schedules = listOf(ScheduleVersion(LocalDate(2026, 10, 1), DayOfWeek.entries.toSet())), archivePeriods = emptyList(),
    )

    private fun width(description: String): Dp = rule.onNodeWithContentDescription(description).getBoundsInRoot().let { it.right - it.left }

    private fun assertSameSize(expected: Dp, actual: Dp, what: String) =
        assertTrue("$what: expected $expected, was $actual", abs(expected.value - actual.value) < 0.5f)

    @Test fun quickRecheckLeavesButtonAtRestSize() {
        var done by mutableStateOf(false)
        rule.setContent { HabitTheme { HabitRow(TodayHabitItem(read, done, streak = 0), onToggle = {}) } }
        val rest = width("Mark done: Read")

        rule.mainClock.autoAdvance = false
        done = true
        rule.mainClock.advanceTimeBy(60) // mid-bounce
        done = false                    // corrected the mis-tap
        rule.mainClock.advanceTimeBy(3_000)

        assertSameSize(rest, width("Mark done: Read"), "button after check + quick uncheck")
    }

    @Test fun switchingDaysDoesNotBounceUntouchedRows() {
        fun day(date: LocalDate, done: Boolean) = DayDetail(
            date = date,
            status = if (done) DayStatus.Perfect(1) else DayStatus.Missed(1),
            isEditable = false,
            groups = listOf(CategoryGroup(health, listOf(DayHabitItem(read, done)))),
        )
        val missed = day(LocalDate(2026, 10, 12), done = false)
        val doneDay = day(LocalDate(2026, 10, 13), done = true)
        var detail by mutableStateOf(missed)
        rule.setContent { HabitTheme { DayPanel(detail, onEvent = {}) } }
        val rest = width("Mark done: Read")

        rule.mainClock.autoAdvance = false
        detail = doneDay // browsing to another day, nothing toggled
        rule.mainClock.advanceTimeBy(60)

        assertSameSize(rest, width("Mark not done: Read"), "row on a newly selected day")
    }
}
