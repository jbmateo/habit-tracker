package app.habittracker.di

import app.habittracker.presentation.dashboard.DashboardViewModel
import org.koin.mp.KoinPlatformTools

/** Entry point for the iOS app. Per-screen handle factories are added alongside each ViewModel. */
object KoinHelper {
    /** Starts Koin once; safe to call repeatedly. */
    fun start() {
        if (KoinPlatformTools.defaultContext().getOrNull() == null) initKoin()
    }

    fun dashboard(): ViewModelHandle<DashboardViewModel> =
        handle { KoinPlatformTools.defaultContext().get().get<DashboardViewModel>() }
}
