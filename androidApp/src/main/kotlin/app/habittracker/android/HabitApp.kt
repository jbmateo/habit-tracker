package app.habittracker.android

import android.app.Application
import app.habittracker.android.reminders.AndroidReminderScheduler
import app.habittracker.android.reminders.NotificationShade
import app.habittracker.android.reminders.PrefsReminderStore
import app.habittracker.android.reminders.RealAlarmSink
import app.habittracker.android.reminders.ensureReminderChannel
import app.habittracker.di.initKoin
import app.habittracker.domain.reminders.ReminderScheduler
import app.habittracker.domain.reminders.ReminderSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.LocalDate
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

open class HabitApp : Application() {
    /** Pins "today"; only the instrumented-test application overrides this. */
    protected open val fixedToday: LocalDate? = null

    // Re-plans make up to 60 AlarmManager calls; keep them off the main thread.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ensureReminderChannel(this)
        val koin = initKoin(fixedToday) {
            androidContext(this@HabitApp)
            modules(module {
                single<ReminderScheduler> { AndroidReminderScheduler(RealAlarmSink(get()), PrefsReminderStore(get()), NotificationShade(get())) }
            })
        }.koin
        koin.get<ReminderSync>().start(appScope)
    }
}
