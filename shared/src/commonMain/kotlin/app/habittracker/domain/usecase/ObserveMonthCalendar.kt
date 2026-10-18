package app.habittracker.domain.usecase

import app.habittracker.domain.logic.dayStatus
import app.habittracker.domain.logic.longestPerfectRun
import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.model.toIndex
import app.habittracker.domain.repository.CompletionRepository
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus

data class CalendarDay(val date: LocalDate, val status: DayStatus)

data class MonthCalendar(
    val month: YearMonth,
    val days: List<CalendarDay>,
    val perfectDays: Int,
    val completionRate: Double?,
    val longestPerfectRun: Int,
)

class ObserveMonthCalendar(
    private val habits: HabitRepository,
    private val completions: CompletionRepository,
    private val clock: AppClock,
) {
    operator fun invoke(month: YearMonth): Flow<MonthCalendar> =
        combine(habits.observeAll(), completions.observeAll(), clock.todayFlow) { allHabits, allCompletions, today ->
            val index = allCompletions.toIndex()
            val first = month.firstDay
            val dates = generateSequence(first) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= month.lastDay }
            val days = dates.map { CalendarDay(it, dayStatus(it, today, allHabits, index)) }.toList()

            var done = 0
            var due = 0
            for ((date, status) in days) {
                if (date > today) continue
                when (status) {
                    is DayStatus.Perfect -> { done += status.due; due += status.due }
                    is DayStatus.Partial -> if (date < today) { done += status.done; due += status.due }
                    is DayStatus.Missed -> if (date < today) due += status.due
                    is DayStatus.InProgress, DayStatus.NoHabits, DayStatus.Future -> Unit
                }
            }
            val statuses = days.map { it.status }
            MonthCalendar(
                month = month,
                days = days,
                perfectDays = statuses.count { it is DayStatus.Perfect },
                completionRate = if (due == 0) null else done.toDouble() / due,
                longestPerfectRun = longestPerfectRun(statuses),
            )
        }
}
