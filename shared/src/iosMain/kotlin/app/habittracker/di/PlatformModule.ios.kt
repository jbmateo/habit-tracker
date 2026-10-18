package app.habittracker.di

import app.habittracker.data.db.DriverFactory
import app.habittracker.data.time.DayChangeSource
import app.habittracker.data.time.IosDayChangeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import app.habittracker.domain.reminders.ReminderScheduler
import app.habittracker.reminders.IosReminderScheduler
import app.habittracker.reminders.UserNotificationCenterGateway
import org.koin.dsl.module

actual val platformModule = module {
    single { DriverFactory() }
    single<DayChangeSource> { IosDayChangeSource() }
    single<CoroutineDispatcher>(IoQualifier) { Dispatchers.IO }
    single<ReminderScheduler> { IosReminderScheduler(UserNotificationCenterGateway()) }
}
