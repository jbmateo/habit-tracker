package app.habittracker.domain.usecase

import app.cash.turbine.test
import app.habittracker.HEALTH
import app.habittracker.MON_WED_FRI
import app.habittracker.date
import app.habittracker.domain.model.ArchivePeriod
import app.habittracker.domain.model.Category
import app.habittracker.domain.model.Completion
import app.habittracker.domain.model.DayStatus
import app.habittracker.fakes.FakeAppClock
import app.habittracker.fakes.FakeCategoryRepository
import app.habittracker.fakes.FakeCompletionRepository
import app.habittracker.fakes.FakeHabitRepository
import app.habittracker.fakes.FakeStore
import app.habittracker.habit
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueryUseCasesTest {
    // 2026-01-14 is a Wednesday.
    private val store = FakeStore().apply { categories.value = listOf(HEALTH) }
    private val habits = FakeHabitRepository(store)
    private val categories = FakeCategoryRepository(store)
    private val completions = FakeCompletionRepository(store)
    private val clock = FakeAppClock(date("2026-01-14"))

    private val dashboard = ObserveTodayDashboard(habits, completions, clock)
    private val calendar = ObserveMonthCalendar(habits, completions, clock)
    private val dayDetail = ObserveDayDetail(habits, completions, clock)
    private val overview = ObserveHabits(habits)
    private val habitDetail = ObserveHabitDetail(habits, completions, clock)
    private val observeCategories = ObserveCategories(categories)
    private val getHabit = GetHabit(habits)

    private val jan = YearMonth(2026, 1)

    private fun done(habitId: String, vararg dates: String) {
        store.completions.value += dates.map { Completion(habitId, date(it)) }
    }

    // Dashboard

    @Test fun dashboardListsOnlyDueActiveHabitsGroupedByCategorySortOrder() = runTest {
        val mind = Category("c-mind", "Mind", "blue", sortOrder = 1)
        store.categories.value = listOf(HEALTH, mind)
        store.habits.value = listOf(
            habit("read", category = mind),
            habit("run", category = HEALTH),
            habit("tue-only", days = setOf(DayOfWeek.TUESDAY)),
            habit("old", archived = listOf(ArchivePeriod(date("2026-01-10"), null))),
        )
        dashboard().test {
            val d = awaitItem()
            assertEquals(listOf(HEALTH, mind), d.groups.map { it.category })
            assertEquals(listOf("run"), d.groups[0].items.map { it.habit.id })
            assertEquals(listOf("read"), d.groups[1].items.map { it.habit.id })
            assertEquals(2, d.due)
            assertTrue(d.hasAnyHabits)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun dashboardCountsAndPerfect() = runTest {
        store.habits.value = listOf(habit("a"), habit("b"))
        done("a", "2026-01-14")
        dashboard().test {
            val first = awaitItem()
            assertEquals(1, first.done)
            assertEquals(2, first.due)
            assertFalse(first.isPerfect)
            assertEquals(date("2026-01-14"), first.date)
            done("b", "2026-01-14")
            val second = awaitItem()
            assertEquals(2, second.done)
            assertTrue(second.isPerfect)
            assertTrue(second.groups.single().items.all { it.isDone })
            assertEquals(1, second.groups.single().items.first().streak)
            // 2026-01-14 is a Wednesday; habits created on the default 2026-01-05 (Monday).
            // This week: Mon 12, Tue 13 missed (2 habits × 2 days), today both done → 2/6.
            assertEquals(2.0 / 6, second.week.thisWeek)
            assertEquals(0.0, second.week.lastWeek)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun dashboardHasNoHabitsWhenEmpty() = runTest {
        dashboard().test {
            val d = awaitItem()
            assertFalse(d.hasAnyHabits)
            assertTrue(d.groups.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun dashboardReemitsOnDayChange() = runTest {
        store.habits.value = listOf(habit("a"))
        done("a", "2026-01-14")
        dashboard().test {
            assertEquals(1, awaitItem().done)
            clock.setToday(date("2026-01-15"))
            val next = awaitItem()
            assertEquals(date("2026-01-15"), next.date)
            assertEquals(0, next.done)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Calendar

    @Test fun calendarMonthHasAllDaysAndStatuses() = runTest {
        store.habits.value = listOf(habit("a", created = "2020-01-01"))
        calendar(YearMonth(2028, 2)).test {
            val m = awaitItem()
            assertEquals(29, m.days.size)
            assertEquals(date("2028-02-01"), m.days.first().date)
            assertEquals(date("2028-02-29"), m.days.last().date)
            assertTrue(m.days.all { it.status == DayStatus.Future })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun calendarSummary() = runTest {
        store.habits.value = listOf(habit("a", created = "2026-01-05"))
        done("a", "2026-01-05", "2026-01-06", "2026-01-07", "2026-01-09", "2026-01-10", "2026-01-11",
            "2026-01-12", "2026-01-13", "2026-01-14")
        calendar(jan).test {
            val m = awaitItem()
            assertEquals(jan, m.month)
            assertEquals(9, m.perfectDays)
            assertEquals(6, m.longestPerfectRun)
            assertEquals(0.9, m.completionRate)
            assertEquals(DayStatus.NoHabits, m.days[0].status)
            assertEquals(DayStatus.Missed(1), m.days[7].status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun calendarRateIsNullWithNothingDue() = runTest {
        calendar(jan).test {
            assertNull(awaitItem().completionRate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun calendarFutureDaysAreFuture() = runTest {
        store.habits.value = listOf(habit("a", created = "2026-01-05"))
        calendar(jan).test {
            val m = awaitItem()
            assertTrue(m.days.filter { it.date > date("2026-01-14") }.all { it.status == DayStatus.Future })
            assertEquals(DayStatus.InProgress(0, 1), m.days[13].status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Day detail

    @Test fun dayDetailEditableOnlyTodayAndYesterday() = runTest {
        store.habits.value = listOf(habit("a", created = "2026-01-05"))
        done("a", "2026-01-13")
        val expected = mapOf("2026-01-14" to true, "2026-01-13" to true, "2026-01-12" to false)
        for ((d, editable) in expected) {
            dayDetail(date(d)).test {
                val detail = awaitItem()
                assertEquals(editable, detail.isEditable, d)
                assertEquals(date(d), detail.date)
                cancelAndIgnoreRemainingEvents()
            }
        }
        dayDetail(date("2026-01-13")).test {
            val detail = awaitItem()
            assertEquals(DayStatus.Perfect(1), detail.status)
            assertTrue(detail.groups.single().items.single().isDone)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Habits overview

    @Test fun habitsOverviewSplitsArchived() = runTest {
        store.habits.value = listOf(
            habit("a"),
            habit("b", created = "2026-01-06", archived = listOf(ArchivePeriod(date("2026-01-10"), null))),
        )
        overview().test {
            val o = awaitItem()
            assertEquals(listOf("a"), o.active.flatMap { it.items }.map { it.id })
            assertEquals(listOf("b"), o.archived.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Habit detail

    @Test fun habitDetailStatesForMonth() = runTest {
        store.habits.value = listOf(habit("a", created = "2026-01-05", days = MON_WED_FRI))
        done("a", "2026-01-05", "2026-01-09")
        habitDetail("a", jan).test {
            val d = assertNotNull(awaitItem())
            assertEquals(31, d.days.size)
            val states = d.days.toMap()
            assertEquals(HabitDayState.NotDue, states[date("2026-01-01")]) // before creation
            assertEquals(HabitDayState.Done, states[date("2026-01-05")])
            assertEquals(HabitDayState.NotDue, states[date("2026-01-06")]) // Tuesday
            assertEquals(HabitDayState.Missed, states[date("2026-01-07")])
            assertEquals(HabitDayState.Done, states[date("2026-01-09")])
            assertEquals(HabitDayState.Missed, states[date("2026-01-12")])
            assertEquals(HabitDayState.Pending, states[date("2026-01-14")])
            assertEquals(HabitDayState.Future, states[date("2026-01-16")])
            assertEquals(HabitDayState.Future, states[date("2026-01-17")])
            assertEquals(jan, d.month)
            assertEquals(0, d.stats.currentStreak)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun habitDetailNullAfterDelete() = runTest {
        store.habits.value = listOf(habit("a"))
        habitDetail("a", jan).test {
            assertNotNull(awaitItem())
            store.habits.value = emptyList()
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Categories and single habit

    @Test fun categoriesAreObserved() = runTest {
        observeCategories().test {
            assertEquals(listOf(HEALTH), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getHabitReadsOnce() = runTest {
        store.habits.value = listOf(habit("a"))
        assertEquals("a", getHabit("a")?.id)
        assertNull(getHabit("zzz"))
    }
}
