package app.habittracker.domain.logic

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

fun isEditable(date: LocalDate, today: LocalDate): Boolean =
    date == today || date == today.minus(DatePeriod(days = 1))
