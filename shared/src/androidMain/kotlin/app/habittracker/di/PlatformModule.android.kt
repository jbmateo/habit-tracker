package app.habittracker.di

import app.habittracker.data.db.DriverFactory
import app.habittracker.data.time.AndroidDayChangeSource
import app.habittracker.data.time.DayChangeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { DriverFactory(androidContext()) }
    single<DayChangeSource> { AndroidDayChangeSource(androidContext()) }
    single<CoroutineDispatcher>(IoQualifier) { Dispatchers.IO }
}
