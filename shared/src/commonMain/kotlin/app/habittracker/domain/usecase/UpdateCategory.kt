package app.habittracker.domain.usecase

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.repository.CategoryRepository

class UpdateCategory(private val categories: CategoryRepository) {
    suspend operator fun invoke(id: String, name: String, colorKey: String): Outcome<Unit> {
        val valid = when (val r = validateCategoryInput(name, colorKey)) {
            is Outcome.Failure -> return r
            is Outcome.Success -> r.value
        }
        categories.get(id) ?: return Outcome.Failure(DomainError.CategoryNotFound)
        categories.update(id, valid, colorKey)
        return Outcome.Success(Unit)
    }
}
