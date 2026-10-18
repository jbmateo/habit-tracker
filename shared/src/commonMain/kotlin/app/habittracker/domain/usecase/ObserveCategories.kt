package app.habittracker.domain.usecase

import app.habittracker.domain.model.Category
import app.habittracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class ObserveCategories(private val categories: CategoryRepository) {
    operator fun invoke(): Flow<List<Category>> = categories.observeAll()
}
