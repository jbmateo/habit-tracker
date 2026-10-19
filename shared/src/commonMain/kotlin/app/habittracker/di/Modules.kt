package app.habittracker.di

import app.habittracker.data.db.DriverFactory
import app.habittracker.data.db.HabitDatabase
import app.habittracker.data.repository.SqlCategoryRepository
import app.habittracker.data.repository.SqlCompletionRepository
import app.habittracker.data.repository.SqlPreferencesRepository
import app.habittracker.domain.repository.PreferencesRepository
import app.habittracker.domain.usecase.ObserveTodaySort
import app.habittracker.domain.usecase.SetTodaySort
import app.habittracker.domain.usecase.CelebrationMemory
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
import app.habittracker.presentation.detail.HabitDetailViewModel
import app.habittracker.presentation.categories.CategoryListViewModel
import app.habittracker.presentation.dashboard.DashboardViewModel
import app.habittracker.presentation.form.HabitFormViewModel
import app.habittracker.presentation.habits.HabitListViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDateTime
import app.habittracker.domain.usecase.MarkDoneFromReminder
import app.habittracker.domain.reminders.ReminderSync
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

/** Dispatcher qualifier for blocking database work. */
val IoQualifier = named("io")

/** `() -> LocalDateTime` in the device time zone, used to plan reminders. */
val NowLocalQualifier = named("nowLocal")

/** Provides DriverFactory, DayChangeSource and the `named("io")` CoroutineDispatcher. */
expect val platformModule: Module

/** ViewModels are added here as later tasks land. */
val sharedModule: Module = module {
    single<HabitDatabase> { HabitDatabase(get<DriverFactory>().create()) }
    single<HabitRepository> { SqlHabitRepository(get(), get(IoQualifier)) }
    single<CategoryRepository> { SqlCategoryRepository(get(), get(IoQualifier)) }
    single<CompletionRepository> { SqlCompletionRepository(get(), get(IoQualifier)) }
    single<PreferencesRepository> { SqlPreferencesRepository(get(), get(IoQualifier)) }
    single<AppClock> { SystemAppClock(Clock.System, { TimeZone.currentSystemDefault() }, get()) }
    single(NowLocalQualifier) { { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) } }
    single { ReminderSync(get(), get(), get(), get(), log = ::println, now = get<() -> LocalDateTime>(NowLocalQualifier)) }
    factory { MarkDoneFromReminder(get(), get(), get()) }

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

    factory { GetHabit(get()) }
    factory { ObserveCategories(get()) }
    factory { ObserveTodaySort(get()) }
    factory { SetTodaySort(get()) }
    factory { CelebrationMemory(get()) }
    factory { ObserveDayDetail(get(), get(), get()) }
    factory { ObserveHabitDetail(get(), get(), get()) }
    factory { ObserveHabits(get()) }
    factory { ObserveMonthCalendar(get(), get(), get()) }
    factory { ObserveTodayDashboard(get(), get(), get()) }

    viewModelOf(::DashboardViewModel)
    viewModelOf(::CalendarViewModel)
    viewModelOf(::HabitListViewModel)
    viewModelOf(::CategoryListViewModel)
    viewModel { (id: String) -> HabitDetailViewModel(id, get(), get(), get(), get(), get()) }
    viewModel { (id: String?) -> HabitFormViewModel(id, get(), get(), get(), get()) }
}
