package app.habittracker.di

import app.habittracker.data.db.HabitDatabase
import app.habittracker.data.inMemoryDb
import app.habittracker.data.time.DayChangeSource
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
        assertNotNull(koin.get<ToggleCompletion>())
        assertNotNull(koin.get<CreateHabit>())
        assertNotNull(koin.get<UpdateHabit>())
        assertNotNull(koin.get<ArchiveHabit>())
        assertNotNull(koin.get<UnarchiveHabit>())
        assertNotNull(koin.get<DeleteHabit>())
        assertNotNull(koin.get<CreateCategory>())
        assertNotNull(koin.get<UpdateCategory>())
        assertNotNull(koin.get<DeleteCategory>())
        assertNotNull(koin.get<SeedDefaultCategoriesIfEmpty>())
    }
}
