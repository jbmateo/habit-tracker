package app.habittracker.domain.usecase

import app.habittracker.domain.repository.CategoryRepository

class SeedDefaultCategoriesIfEmpty(private val categories: CategoryRepository) {
    suspend operator fun invoke() {
        if (!categories.isEmpty()) return
        DEFAULTS.forEach { (name, color) -> categories.create(name, color) }
    }

    private companion object {
        val DEFAULTS = listOf(
            "Health" to "green",
            "Fitness" to "orange",
            "Learning" to "blue",
            "Mindfulness" to "purple",
            "Productivity" to "teal",
        )
    }
}
