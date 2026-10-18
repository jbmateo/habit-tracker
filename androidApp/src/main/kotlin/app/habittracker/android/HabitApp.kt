package app.habittracker.android

import android.app.Application
import app.habittracker.di.initKoin
import kotlinx.datetime.LocalDate
import org.koin.android.ext.koin.androidContext

open class HabitApp : Application() {
    /** Pins "today"; only the instrumented-test application overrides this. */
    protected open val fixedToday: LocalDate? = null

    override fun onCreate() {
        super.onCreate()
        initKoin(fixedToday) { androidContext(this@HabitApp) }
    }
}
