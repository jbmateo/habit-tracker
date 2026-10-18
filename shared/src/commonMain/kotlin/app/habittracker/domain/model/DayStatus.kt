package app.habittracker.domain.model

sealed interface DayStatus {
    data object Future : DayStatus
    data object NoHabits : DayStatus
    data class Perfect(val due: Int) : DayStatus
    data class InProgress(val done: Int, val due: Int) : DayStatus
    data class Partial(val done: Int, val due: Int) : DayStatus
    data class Missed(val due: Int) : DayStatus
}
