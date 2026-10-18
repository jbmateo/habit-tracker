package app.habittracker.di

import app.habittracker.data.db.HabitDatabase
import app.habittracker.data.inMemoryDb
import app.habittracker.data.time.DayChangeSource
import app.habittracker.domain.repository.CategoryRepository
import app.habittracker.domain.repository.CompletionRepository
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull

class KoinGraphTest {
    /** Stands in for the platform module: in-memory database and a no-op day-change source. */
    private val testPlatformModule: Module = module {
        single<HabitDatabase> { inMemoryDb() }
        single<DayChangeSource> { object : DayChangeSource { override val changes: Flow<Unit> = emptyFlow() } }
        single<CoroutineDispatcher>(named("io")) { Dispatchers.Unconfined }
    }

    @Test
    fun everySharedDefinitionResolves() {
        val koin = koinApplication {
            allowOverride(true)
            modules(sharedModule, testPlatformModule)
        }.koin
        assertNotNull(koin.get<HabitDatabase>())
        assertNotNull(koin.get<HabitRepository>())
        assertNotNull(koin.get<CategoryRepository>())
        assertNotNull(koin.get<CompletionRepository>())
        assertNotNull(koin.get<AppClock>())
    }
}
