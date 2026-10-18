package app.habittracker.data

import app.cash.turbine.test
import app.habittracker.data.repository.SqlCategoryRepository
import app.habittracker.data.repository.SqlCompletionRepository
import app.habittracker.data.repository.SqlHabitRepository
import app.habittracker.domain.model.ArchivePeriod
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlRepositoriesTest {
    private val d1 = LocalDate(2026, 10, 1)
    private val d2 = LocalDate(2026, 10, 5)
    private val d3 = LocalDate(2026, 10, 9)
    private val mwf = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

    private class Fixture(val testScope: kotlinx.coroutines.test.TestScope) {
        val db = inMemoryDb()
        val io = StandardTestDispatcher(testScope.testScheduler)
        val habits = SqlHabitRepository(db, io)
        val categories = SqlCategoryRepository(db, io)
        val completions = SqlCompletionRepository(db, io)
    }

    private suspend fun Fixture.newHabit(name: String = "Run", createdOn: LocalDate = d1, days: Set<DayOfWeek> = mwf): Pair<String, String> {
        val cat = categories.create("Fitness", "orange")
        return cat to habits.create(name, "run", cat, createdOn, days)
    }

    @Test fun createHabitHydratesScheduleAndCategory() = runTest {
        val f = Fixture(this)
        val (cat, id) = f.newHabit()
        val h = assertNotNull(f.habits.get(id))
        assertEquals("Run", h.name)
        assertEquals("run", h.iconKey)
        assertEquals(d1, h.createdOn)
        assertEquals(cat, h.category.id)
        assertEquals("Fitness", h.category.name)
        assertEquals(1, h.schedules.size)
        assertEquals(d1, h.currentSchedule.effectiveFrom)
        assertEquals(mwf, h.currentSchedule.days)
        assertTrue(h.archivePeriods.isEmpty())
    }

    @Test fun upsertScheduleSameDayReplaces() = runTest {
        val f = Fixture(this)
        val (_, id) = f.newHabit()
        f.habits.upsertSchedule(id, d2, setOf(DayOfWeek.MONDAY))
        f.habits.upsertSchedule(id, d2, setOf(DayOfWeek.TUESDAY))
        val h = assertNotNull(f.habits.get(id))
        assertEquals(2, h.schedules.size)
        assertEquals(setOf(DayOfWeek.TUESDAY), h.scheduleOn(d2)?.days)
    }

    @Test fun scheduleHistoryOrdered() = runTest {
        val f = Fixture(this)
        val (_, id) = f.newHabit()
        f.habits.upsertSchedule(id, d3, setOf(DayOfWeek.SUNDAY))
        f.habits.upsertSchedule(id, d2, setOf(DayOfWeek.SATURDAY))
        val h = assertNotNull(f.habits.get(id))
        assertEquals(listOf(d1, d2, d3), h.schedules.map { it.effectiveFrom })
    }

    @Test fun archivePeriodsRoundTrip() = runTest {
        val f = Fixture(this)
        val (_, id) = f.newHabit()
        f.habits.openArchivePeriod(id, d2)
        assertEquals(listOf(ArchivePeriod(d2, null)), f.habits.get(id)!!.archivePeriods)
        assertTrue(f.habits.get(id)!!.isArchived)
        f.habits.closeArchivePeriod(id, d2, d3)
        assertEquals(listOf(ArchivePeriod(d2, d3)), f.habits.get(id)!!.archivePeriods)
        assertFalse(f.habits.get(id)!!.isArchived)
        f.habits.deleteArchivePeriod(id, d2)
        assertTrue(f.habits.get(id)!!.archivePeriods.isEmpty())
    }

    @Test fun toggleTwiceEndsNotDone() = runTest {
        val f = Fixture(this)
        val (_, id) = f.newHabit()
        assertTrue(f.completions.toggle(id, d1))
        assertFalse(f.completions.toggle(id, d1))
    }

    @Test fun concurrentTogglesAreConsistent() = runTest {
        val f = Fixture(this)
        val (_, id) = f.newHabit()
        val results = listOf(async { f.completions.toggle(id, d1) }, async { f.completions.toggle(id, d1) }).awaitAll()
        assertEquals(setOf(true, false), results.toSet())
        assertEquals(0, f.db.completionQueries.selectAll().executeAsList().size)
    }

    @Test fun deleteHabitCascades() = runTest {
        val f = Fixture(this)
        val (_, id) = f.newHabit()
        f.habits.openArchivePeriod(id, d2)
        f.completions.toggle(id, d1)
        f.habits.delete(id)
        assertNull(f.habits.get(id))
        assertEquals(0, f.db.habitScheduleQueries.selectAll().executeAsList().size)
        assertEquals(0, f.db.habitArchivePeriodQueries.selectAll().executeAsList().size)
        assertEquals(0, f.db.completionQueries.selectAll().executeAsList().size)
    }

    @Test fun categoryDeleteWithHabitFailsOnForeignKey() = runTest {
        val f = Fixture(this)
        val (cat, _) = f.newHabit()
        assertFailsWith<Exception> { f.categories.delete(cat) }
    }

    @Test fun habitCountIncludesArchived() = runTest {
        val f = Fixture(this)
        val (cat, id) = f.newHabit()
        f.habits.openArchivePeriod(id, d2)
        assertEquals(1L, f.categories.habitCount(cat))
    }

    @Test fun observeAllEmitsOnToggle() = runTest {
        val f = Fixture(this)
        val (_, id) = f.newHabit()
        f.completions.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            f.completions.toggle(id, d1)
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun observeAllHydratesConsistentlyOnCreate() = runTest {
        val f = Fixture(this)
        val cat = f.categories.create("Fitness", "orange")
        f.habits.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            f.habits.create("A", "run", cat, d1, mwf)
            f.habits.create("B", "run", cat, d1, mwf)
            var last = awaitItem()
            while (last.size < 2) {
                assertTrue(last.all { it.schedules.isNotEmpty() })
                last = awaitItem()
            }
            assertTrue(last.all { it.schedules.isNotEmpty() })
            assertEquals(setOf("A", "B"), last.map { it.name }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun observeAllAfterDeleteNeverEmitsScheduleless() = runTest {
        val f = Fixture(this)
        val (_, id) = f.newHabit()
        f.habits.observeAll().test {
            var item = awaitItem()
            assertTrue(item.all { it.schedules.isNotEmpty() })
            f.habits.delete(id)
            while (item.isNotEmpty()) {
                item = awaitItem()
                assertTrue(item.all { it.schedules.isNotEmpty() })
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun observeSingleHabitHydrates() = runTest {
        val f = Fixture(this)
        val (_, id) = f.newHabit()
        f.habits.observe(id).test {
            val h = assertNotNull(awaitItem())
            assertEquals(1, h.schedules.size)
            f.habits.delete(id)
            var item = awaitItem()
            while (item != null) item = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
