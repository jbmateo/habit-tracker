package app.habittracker.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import kotlinx.datetime.LocalDate

/** Instrumented tests run against [TestHabitApp], whose "today" is fixed. */
class HabitTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, TestHabitApp::class.java.name, context)
}

/** The app with "today" pinned to [TEST_TODAY], so tests behave the same whenever they run. */
class TestHabitApp : HabitApp() {
    override val fixedToday: LocalDate = TEST_TODAY
}

val TEST_TODAY = LocalDate(2026, 10, 14)
