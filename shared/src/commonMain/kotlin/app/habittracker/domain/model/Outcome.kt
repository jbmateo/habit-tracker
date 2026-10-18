package app.habittracker.domain.model

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}

sealed interface DomainError {
    data object EditWindowClosed : DomainError
    data object NotDue : DomainError
    data object InvalidName : DomainError
    data object NoDaysSelected : DomainError
    data object UnknownIcon : DomainError
    data object UnknownColor : DomainError
    data object CategoryNotFound : DomainError
    data object HabitNotFound : DomainError
    data object CategoryNotEmpty : DomainError
}
