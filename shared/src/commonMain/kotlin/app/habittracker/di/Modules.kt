package app.habittracker.di

import app.habittracker.data.db.DriverFactory
import app.habittracker.data.db.HabitDatabase
import app.habittracker.data.repository.SqlCategoryRepository
import app.habittracker.data.repository.SqlCompletionRepository
import app.habittracker.data.repository.SqlHabitRepository
import app.habittracker.data.time.SystemAppClock
import app.habittracker.domain.repository.CategoryRepository
import app.habittracker.domain.repository.CompletionRepository
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import kotlinx.datetime.TimeZone
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

/** Dispatcher qualifier for blocking database work. */
val IoQualifier = named("io")

/** Provides DriverFactory, DayChangeSource and the `named("io")` CoroutineDispatcher. */
expect val platformModule: Module

/** Use cases (`factory`) and ViewModels are added here as later tasks land. */
val sharedModule: Module = module {
    single<HabitDatabase> { HabitDatabase(get<DriverFactory>().create()) }
    single<HabitRepository> { SqlHabitRepository(get(), get(IoQualifier)) }
    single<CategoryRepository> { SqlCategoryRepository(get(), get(IoQualifier)) }
    single<CompletionRepository> { SqlCompletionRepository(get(), get(IoQualifier)) }
    single<AppClock> { SystemAppClock(Clock.System, { TimeZone.currentSystemDefault() }, get()) }
}
