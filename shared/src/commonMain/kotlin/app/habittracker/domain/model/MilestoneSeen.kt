package app.habittracker.domain.model

import kotlinx.datetime.LocalDate

/** The highest streak milestone dismissed for the perfect-day run that started on [runStart]. */
data class MilestoneSeen(val runStart: LocalDate, val milestone: Int)
