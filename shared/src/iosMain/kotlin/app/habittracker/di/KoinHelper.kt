package app.habittracker.di

import app.habittracker.presentation.calendar.CalendarViewModel
import app.habittracker.presentation.categories.CategoryListViewModel
import app.habittracker.presentation.dashboard.DashboardViewModel
import app.habittracker.presentation.habits.HabitListViewModel
import org.koin.mp.KoinPlatformTools

/** Entry point for the iOS app. Per-screen handle factories are added alongside each ViewModel. */
object KoinHelper {
    /** Starts Koin once; safe to call repeatedly. */
    fun start() {
        if (KoinPlatformTools.defaultContext().getOrNull() == null) initKoin()
    }

    fun dashboard(): ViewModelHandle<DashboardViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<DashboardViewModel>() }

    fun calendar(): ViewModelHandle<CalendarViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<CalendarViewModel>() }

    fun habitList(): ViewModelHandle<HabitListViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<HabitListViewModel>() }

    fun categoryList(): ViewModelHandle<CategoryListViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<CategoryListViewModel>() }
}
