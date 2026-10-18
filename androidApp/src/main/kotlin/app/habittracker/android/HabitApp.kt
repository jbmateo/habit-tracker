package app.habittracker.android

import android.app.Application
import app.habittracker.di.initKoin
import org.koin.android.ext.koin.androidContext

class HabitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@HabitApp) }
    }
}
