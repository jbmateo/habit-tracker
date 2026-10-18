package app.habittracker.domain.usecase

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.repository.CategoryRepository

class DeleteCategory(private val categories: CategoryRepository) {
    suspend operator fun invoke(id: String): Outcome<Unit> {
        categories.get(id) ?: return Outcome.Failure(DomainError.CategoryNotFound)
        if (categories.habitCount(id) > 0) return Outcome.Failure(DomainError.CategoryNotEmpty)
        categories.delete(id)
        return Outcome.Success(Unit)
    }
}
