package app.habittracker.android.ui.common

import app.habittracker.domain.model.DomainError

/** Short human-readable message for a [DomainError]; shared by every screen that surfaces one. */
fun domainErrorMessage(error: DomainError): String = when (error) {
    DomainError.EditWindowClosed -> "You can only change today and yesterday."
    DomainError.NotDue -> "That habit isn't scheduled for this day."
    DomainError.InvalidName -> "Names need 1 to 40 characters."
    DomainError.NoDaysSelected -> "Pick at least one day."
    DomainError.UnknownIcon -> "That icon isn't available."
    DomainError.UnknownColor -> "That color isn't available."
    DomainError.CategoryNotFound -> "That category no longer exists."
    DomainError.HabitNotFound -> "That habit no longer exists."
    DomainError.CategoryNotEmpty -> "Move or delete this category's habits first."
}

/** Replaces any snackbar currently shown with [message]. */
suspend fun androidx.compose.material3.SnackbarHostState.showReplacing(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message)
}
