package app.habittracker.di

import app.habittracker.data.db.HabitDatabase
import app.habittracker.data.inMemoryDb
import app.habittracker.data.time.DayChangeSource
import app.habittracker.domain.repository.CategoryRepository
import app.habittracker.domain.repository.CompletionRepository
import app.habittracker.domain.repository.PreferencesRepository
import app.habittracker.domain.usecase.ObserveTodaySort
import app.habittracker.domain.usecase.SetTodaySort
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import app.habittracker.domain.usecase.ArchiveHabit
import app.habittracker.domain.usecase.CreateCategory
import app.habittracker.domain.usecase.CreateHabit
import app.habittracker.domain.usecase.DeleteCategory
import app.habittracker.domain.usecase.DeleteHabit
import app.habittracker.domain.usecase.GetHabit
import app.habittracker.domain.usecase.ObserveCategories
import app.habittracker.domain.usecase.ObserveDayDetail
import app.habittracker.domain.usecase.ObserveHabitDetail
import app.habittracker.domain.usecase.ObserveHabits
import app.habittracker.domain.usecase.ObserveMonthCalendar
import app.habittracker.domain.usecase.ObserveTodayDashboard
import app.habittracker.domain.usecase.SeedDefaultCategoriesIfEmpty
import app.habittracker.domain.usecase.ToggleCompletion
import app.habittracker.domain.usecase.UnarchiveHabit
import app.habittracker.domain.usecase.UpdateCategory
import app.habittracker.domain.usecase.UpdateHabit
import app.habittracker.presentation.calendar.CalendarViewModel
import app.habittracker.presentation.categories.CategoryListViewModel
import app.habittracker.presentation.dashboard.DashboardViewModel
import app.habittracker.presentation.detail.HabitDetailViewModel
import app.habittracker.presentation.form.HabitFormViewModel
import app.habittracker.presentation.habits.HabitListViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import app.habittracker.domain.reminders.ReminderScheduler
import app.habittracker.domain.reminders.ReminderSync
import app.habittracker.domain.usecase.MarkDoneFromReminder
import app.habittracker.fakes.FakeReminderScheduler
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull

class KoinGraphTest {
    /** Stands in for the platform module: in-memory database and a no-op day-change source. */
    private val testPlatformModule: Module = module {
        single<HabitDatabase> { inMemoryDb() }
        single<DayChangeSource> { object : DayChangeSource { override val changes: Flow<Unit> = emptyFlow() } }
        single<CoroutineDispatcher>(named("io")) { Dispatchers.Unconfined }
        single<ReminderScheduler> { FakeReminderScheduler() }
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
        assertNotNull(koin.get<PreferencesRepository>())
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
        assertNotNull(koin.get<GetHabit>())
        assertNotNull(koin.get<ObserveCategories>())
        assertNotNull(koin.get<ObserveTodaySort>())
        assertNotNull(koin.get<SetTodaySort>())
        assertNotNull(koin.get<ReminderSync>())
        assertNotNull(koin.get<MarkDoneFromReminder>())
        assertNotNull(koin.get<ObserveDayDetail>())
        assertNotNull(koin.get<ObserveHabitDetail>())
        assertNotNull(koin.get<ObserveHabits>())
        assertNotNull(koin.get<ObserveMonthCalendar>())
        assertNotNull(koin.get<ObserveTodayDashboard>())
        assertNotNull(koin.get<DashboardViewModel>())
        assertNotNull(koin.get<CalendarViewModel>())
        assertNotNull(koin.get<HabitListViewModel>())
        assertNotNull(koin.get<CategoryListViewModel>())
        assertNotNull(koin.get<HabitDetailViewModel> { parametersOf("some-id") })
        assertNotNull(koin.get<HabitFormViewModel> { parametersOf(*arrayOf<Any?>(null)) })
        assertNotNull(koin.get<HabitFormViewModel> { parametersOf("some-id") })
    }

    @Test
    fun fixedClockModuleOverridesToday() {
        val koin = koinApplication {
            allowOverride(true)
            modules(sharedModule, testPlatformModule, fixedClockModule(LocalDate(2026, 10, 14)))
        }.koin
        assertEquals(LocalDate(2026, 10, 14), koin.get<AppClock>().today())
    }
}
