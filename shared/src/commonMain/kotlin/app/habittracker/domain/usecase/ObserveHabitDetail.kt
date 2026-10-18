package app.habittracker.domain.usecase

import app.habittracker.domain.logic.habitStats
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.HabitStats
import app.habittracker.domain.repository.CompletionRepository
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus

/** Pending = today, due, not done. */
enum class HabitDayState { Done, Missed, Pending, NotDue, Future }

data class HabitDetail(
    val habit: Habit,
    val stats: HabitStats,
    val month: YearMonth,
    val days: List<Pair<LocalDate, HabitDayState>>,
)

class ObserveHabitDetail(
    private val habits: HabitRepository,
    private val completions: CompletionRepository,
    private val clock: AppClock,
) {
    /** Emits null when the habit no longer exists. */
    operator fun invoke(id: String, month: YearMonth): Flow<HabitDetail?> =
        combine(habits.observe(id), completions.observeForHabit(id), clock.todayFlow) { habit, doneDates, today ->
            habit?.let {
                val dates = generateSequence(month.firstDay) { it.plus(DatePeriod(days = 1)) }
                    .takeWhile { it <= month.lastDay }
                HabitDetail(
                    habit = habit,
                    stats = habitStats(habit, doneDates, today),
                    month = month,
                    days = dates.map { it to stateOf(habit, it, today, doneDates) }.toList(),
                )
            }
        }

    private fun stateOf(habit: Habit, date: LocalDate, today: LocalDate, doneDates: Set<LocalDate>) = when {
        date > today -> HabitDayState.Future
        !habit.isDueOn(date) -> HabitDayState.NotDue
        date in doneDates -> HabitDayState.Done
        date == today -> HabitDayState.Pending
        else -> HabitDayState.Missed
    }
}
