package app.habittracker.domain.usecase

import app.habittracker.domain.model.Outcome
import app.habittracker.domain.repository.CategoryRepository

class CreateCategory(private val categories: CategoryRepository) {
    suspend operator fun invoke(name: String, colorKey: String): Outcome<String> {
        val valid = when (val r = validateCategoryInput(name, colorKey)) {
            is Outcome.Failure -> return r
            is Outcome.Success -> r.value
        }
        return Outcome.Success(categories.create(valid, colorKey))
    }
}
