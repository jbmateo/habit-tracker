package app.habittracker.data

import app.cash.turbine.test
import app.habittracker.data.repository.SqlPreferencesRepository
import app.habittracker.domain.model.TodaySort
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
