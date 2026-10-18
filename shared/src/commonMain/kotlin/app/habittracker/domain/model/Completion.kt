package app.habittracker.domain.model

import kotlinx.datetime.LocalDate

data class Completion(val habitId: String, val date: LocalDate)

/** date -> ids of habits completed on that date. */
typealias CompletionIndex = Map<LocalDate, Set<String>>

fun List<Completion>.toIndex(): CompletionIndex =
    groupBy({ it.date }, { it.habitId }).mapValues { it.value.toSet() }
