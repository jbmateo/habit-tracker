package app.habittracker.di

import app.habittracker.data.db.deleteHabitsDatabase
import app.habittracker.presentation.calendar.CalendarViewModel
import app.habittracker.presentation.categories.CategoryListViewModel
import app.habittracker.presentation.dashboard.DashboardViewModel
import app.habittracker.presentation.detail.HabitDetailViewModel
import app.habittracker.presentation.form.HabitFormViewModel
import app.habittracker.presentation.habits.HabitListViewModel
import kotlinx.datetime.LocalDate
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatformTools

/** Entry point for the iOS app. Per-screen handle factories are added alongside each ViewModel. */
object KoinHelper {
    /**
     * Starts Koin once; safe to call repeatedly. [resetDatabase] deletes the database file first (UI tests); it is
     * ignored when Koin is already running, because the database is then already open. [fixedToday] (ISO date, UI
     * tests only) pins "today" so tests don't depend on when they run.
     */
    fun start(resetDatabase: Boolean = false, fixedToday: String? = null) {
        if (KoinPlatformTools.defaultContext().getOrNull() != null) return
        if (resetDatabase) deleteHabitsDatabase()
        initKoin(fixedToday = fixedToday?.let(LocalDate::parse))
    }

    fun dashboard(): ViewModelHandle<DashboardViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<DashboardViewModel>() }

    fun calendar(): ViewModelHandle<CalendarViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<CalendarViewModel>() }

    fun habitList(): ViewModelHandle<HabitListViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<HabitListViewModel>() }

    fun categoryList(): ViewModelHandle<CategoryListViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<CategoryListViewModel>() }

    fun habitDetail(id: String): ViewModelHandle<HabitDetailViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<HabitDetailViewModel> { parametersOf(id) } }

    fun habitForm(id: String?): ViewModelHandle<HabitFormViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<HabitFormViewModel> { parametersOf(*arrayOf<Any?>(id)) } }
}
