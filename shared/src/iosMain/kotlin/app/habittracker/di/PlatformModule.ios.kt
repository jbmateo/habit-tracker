package app.habittracker.di

import app.habittracker.data.db.DriverFactory
import app.habittracker.data.time.DayChangeSource
import app.habittracker.data.time.IosDayChangeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module

actual val platformModule = module {
    single { DriverFactory() }
    single<DayChangeSource> { IosDayChangeSource() }
    single<CoroutineDispatcher>(IoQualifier) { Dispatchers.IO }
}
