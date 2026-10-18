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
import app.habittracker.domain.usecase.ArchiveHabit
import app.habittracker.domain.usecase.CreateCategory
import app.habittracker.domain.usecase.CreateHabit
import app.habittracker.domain.usecase.DeleteCategory
import app.habittracker.domain.usecase.DeleteHabit
import app.habittracker.domain.usecase.SeedDefaultCategoriesIfEmpty
import app.habittracker.domain.usecase.ToggleCompletion
import app.habittracker.domain.usecase.UnarchiveHabit
import app.habittracker.domain.usecase.UpdateCategory
import app.habittracker.domain.usecase.UpdateHabit
import kotlinx.datetime.TimeZone
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

/** Dispatcher qualifier for blocking database work. */
val IoQualifier = named("io")

/** Provides DriverFactory, DayChangeSource and the `named("io")` CoroutineDispatcher. */
expect val platformModule: Module

/** ViewModels are added here as later tasks land. */
val sharedModule: Module = module {
    single<HabitDatabase> { HabitDatabase(get<DriverFactory>().create()) }
    single<HabitRepository> { SqlHabitRepository(get(), get(IoQualifier)) }
    single<CategoryRepository> { SqlCategoryRepository(get(), get(IoQualifier)) }
    single<CompletionRepository> { SqlCompletionRepository(get(), get(IoQualifier)) }
    single<AppClock> { SystemAppClock(Clock.System, { TimeZone.currentSystemDefault() }, get()) }

    factory { ToggleCompletion(get(), get(), get()) }
    factory { CreateHabit(get(), get(), get()) }
    factory { UpdateHabit(get(), get(), get()) }
    factory { ArchiveHabit(get(), get()) }
    factory { UnarchiveHabit(get(), get()) }
    factory { DeleteHabit(get()) }
    factory { CreateCategory(get()) }
    factory { UpdateCategory(get()) }
    factory { DeleteCategory(get()) }
    factory { SeedDefaultCategoriesIfEmpty(get()) }
}
