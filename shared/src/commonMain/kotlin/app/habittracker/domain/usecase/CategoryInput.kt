package app.habittracker.domain.usecase

import app.habittracker.domain.logic.validateName
import app.habittracker.domain.model.ColorKeys
import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome

/** Returns the trimmed name, or the first failure (name, then color). */
internal fun validateCategoryInput(name: String, colorKey: String): Outcome<String> {
    val valid = when (val r = validateName(name)) {
        is Outcome.Failure -> return r
        is Outcome.Success -> r.value
    }
    if (colorKey !in ColorKeys.all) return Outcome.Failure(DomainError.UnknownColor)
    return Outcome.Success(valid)
}
