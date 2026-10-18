package app.habittracker.di

import app.habittracker.data.time.FixedAppClock
import app.habittracker.domain.time.AppClock
import app.habittracker.domain.usecase.SeedDefaultCategoriesIfEmpty
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Starts the app's dependency graph. [fixedToday] pins "today" for UI tests; apps pass it only from
 * debug/test builds, so release builds always follow the real clock.
 */
fun initKoin(fixedToday: LocalDate? = null, appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        allowOverride(true)
        modules(listOfNotNull(platformModule, sharedModule, fixedToday?.let(::fixedClockModule)))
    }.also { app -> runBlocking { app.koin.get<SeedDefaultCategoriesIfEmpty>()() } }

/** Overrides [AppClock] with one pinned to [date]. */
fun fixedClockModule(date: LocalDate): Module = module {
    single<AppClock> { FixedAppClock(date) }
}
