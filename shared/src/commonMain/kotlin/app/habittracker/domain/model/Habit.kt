package app.habittracker.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

data class ScheduleVersion(val effectiveFrom: LocalDate, val days: Set<DayOfWeek>)

/** Archived from [from] (inclusive) until [toExclusive]; null means still archived. */
data class ArchivePeriod(val from: LocalDate, val toExclusive: LocalDate?)

data class Habit(
    val id: String,
    val name: String,
    val iconKey: String,
    val category: Category,
    val createdOn: LocalDate,
    val schedules: List<ScheduleVersion>,
    val archivePeriods: List<ArchivePeriod>,
) {
    val isArchived: Boolean get() = archivePeriods.any { it.toExclusive == null }

    val currentSchedule: ScheduleVersion get() = schedules.maxBy { it.effectiveFrom }

    fun scheduleOn(date: LocalDate): ScheduleVersion? =
        schedules.filter { it.effectiveFrom <= date }.maxByOrNull { it.effectiveFrom }

    fun isArchivedOn(date: LocalDate): Boolean =
        archivePeriods.any { date >= it.from && (it.toExclusive == null || date < it.toExclusive) }

    fun isDueOn(date: LocalDate): Boolean =
        date >= createdOn && !isArchivedOn(date) && scheduleOn(date)?.days?.contains(date.dayOfWeek) == true
}
