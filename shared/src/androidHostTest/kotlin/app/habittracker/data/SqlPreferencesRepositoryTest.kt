package app.habittracker.data

import app.cash.turbine.test
import app.habittracker.data.repository.SqlPreferencesRepository
import app.habittracker.domain.model.MilestoneSeen
import app.habittracker.domain.model.TodaySort
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlPreferencesRepositoryTest {
    @Test fun todaySortDefaultsToCategoryAndRemembersChoice() = runTest {
        val db = inMemoryDb()
        val prefs = SqlPreferencesRepository(db, StandardTestDispatcher(testScheduler))
        prefs.observeTodaySort().test {
            assertEquals(TodaySort.Category, awaitItem())
            prefs.setTodaySort(TodaySort.Streak)
            assertEquals(TodaySort.Streak, awaitItem())
            prefs.setTodaySort(TodaySort.Name)
            assertEquals(TodaySort.Name, awaitItem())
        }
        // A fresh repository over the same database reads the stored value.
        assertEquals(TodaySort.Name, SqlPreferencesRepository(db, StandardTestDispatcher(testScheduler)).todaySortNow())
    }

    @Test fun unknownStoredValueFallsBackToCategory() = runTest {
        val db = inMemoryDb()
        db.preferenceQueries.upsert("todaySort", "Rainbow")
        assertEquals(TodaySort.Category, SqlPreferencesRepository(db, StandardTestDispatcher(testScheduler)).todaySortNow())
    }

    @Test fun celebratedOnRoundTrips() = runTest {
        val prefs = SqlPreferencesRepository(inMemoryDb(), StandardTestDispatcher(testScheduler))
        assertNull(prefs.observeCelebratedOn().first())
        prefs.setCelebratedOn(LocalDate(2026, 10, 19))
        assertEquals(LocalDate(2026, 10, 19), prefs.observeCelebratedOn().first())
    }

    @Test fun milestoneSeenRoundTrips() = runTest {
        val db = inMemoryDb()
        val prefs = SqlPreferencesRepository(db, StandardTestDispatcher(testScheduler))
        val seen = MilestoneSeen(LocalDate(2026, 10, 11), 7)
        prefs.setMilestoneSeen(seen)
        assertEquals("2026-10-11:7", db.preferenceQueries.selectValue("milestone_seen").executeAsOne())
        assertEquals(seen, prefs.observeMilestoneSeen().first())
    }

    @Test fun garbageMilestoneSeenReadsAsNull() = runTest {
        val db = inMemoryDb()
        val prefs = SqlPreferencesRepository(db, StandardTestDispatcher(testScheduler))
        db.preferenceQueries.upsert("milestone_seen", "nope")
        assertNull(prefs.observeMilestoneSeen().first())
        db.preferenceQueries.upsert("milestone_seen", "2026-10-11:x")
        assertNull(prefs.observeMilestoneSeen().first())
    }
}
