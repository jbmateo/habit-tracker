package app.habittracker.domain.model

data class CategoryGroup<T>(val category: Category, val items: List<T>)

/** Groups ordered by category sortOrder, then name; item order within a group is preserved. */
fun <T> List<T>.groupByCategory(category: (T) -> Category): List<CategoryGroup<T>> =
    groupBy { category(it) }
        .map { (cat, items) -> CategoryGroup(cat, items) }
        .sortedWith(compareBy({ it.category.sortOrder }, { it.category.name }))
