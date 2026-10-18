package app.habittracker.android.ui.common

import androidx.compose.material3.SnackbarHostState
import app.habittracker.domain.model.DomainError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

/**
 * Replaces any snackbar currently shown with [message]. Launches in [scope] so the caller (typically an effect
 * collector) never suspends while the snackbar is visible.
 */
fun SnackbarHostState.showReplacing(scope: CoroutineScope, message: String) {
    scope.launch {
        currentSnackbarData?.dismiss()
        showSnackbar(message)
    }
}
