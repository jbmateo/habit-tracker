package app.habittracker

import app.habittracker.domain.model.ArchivePeriod
import app.habittracker.domain.model.Category
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.ScheduleVersion
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

fun date(s: String): LocalDate = LocalDate.parse(s)

val HEALTH = Category(id = "c-health", name = "Health", colorKey = "green", sortOrder = 0)

val MON_WED_FRI: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

fun habit(
    id: String = "h1",
    created: String = "2026-01-05",
    days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    category: Category = HEALTH,
    schedules: List<ScheduleVersion>? = null,
    archived: List<ArchivePeriod> = emptyList(),
    name: String = id,
    iconKey: String = "run",
    reminder: String? = null,
): Habit = Habit(
    id = id,
    name = name,
    iconKey = iconKey,
    category = category,
    createdOn = date(created),
    schedules = schedules ?: listOf(ScheduleVersion(date(created), days)),
    archivePeriods = archived,
    reminderTime = reminder?.let(kotlinx.datetime.LocalTime::parse),
)
