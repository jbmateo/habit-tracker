package app.habittracker.domain.logic

import app.habittracker.domain.model.CategoryGroup
import app.habittracker.domain.model.TodaySort
import app.habittracker.domain.usecase.TodayHabitItem

/** Flattens today's category groups into one list ordered by [sort]. All sorts are stable. */
fun sortTodayItems(groups: List<CategoryGroup<TodayHabitItem>>, sort: TodaySort): List<TodayHabitItem> {
    val byCategory = groups.flatMap { it.items }
    val byName = compareBy<TodayHabitItem> { it.habit.name.lowercase() }
    return when (sort) {
        TodaySort.Category -> byCategory
        TodaySort.Name -> byCategory.sortedWith(byName)
        TodaySort.Streak -> byCategory.sortedWith(compareByDescending<TodayHabitItem> { it.streak }.then(byName))
        TodaySort.ToDoFirst -> byCategory.sortedBy { it.isDone }
    }
}
