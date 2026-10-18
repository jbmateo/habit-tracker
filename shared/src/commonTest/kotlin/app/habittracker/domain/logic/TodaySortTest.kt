package app.habittracker.domain.logic

import app.habittracker.HEALTH
import app.habittracker.domain.model.CategoryGroup
import app.habittracker.domain.model.TodaySort
import app.habittracker.domain.usecase.TodayHabitItem
import app.habittracker.habit
import kotlin.test.Test
import kotlin.test.assertEquals

class TodaySortTest {
    private val fitness = HEALTH.copy(id = "c-fit", name = "Fitness", sortOrder = 1)

    private fun item(name: String, done: Boolean, streak: Int, category: app.habittracker.domain.model.Category) =
        TodayHabitItem(habit(name, name = name, category = category), done, streak)

    // Category order as delivered by the dashboard: Health (water, gym), Fitness (run).
    private val groups = listOf(
        CategoryGroup(HEALTH, listOf(item("water", done = true, streak = 2, HEALTH), item("Gym", done = false, streak = 9, HEALTH))),
        CategoryGroup(fitness, listOf(item("run", done = false, streak = 4, fitness))),
    )

    private fun names(sort: TodaySort) = sortTodayItems(groups, sort).map { it.habit.name }

    @Test fun categoryKeepsGroupOrder() = assertEquals(listOf("water", "Gym", "run"), names(TodaySort.Category))

    @Test fun nameIsCaseInsensitiveAlphabetical() = assertEquals(listOf("Gym", "run", "water"), names(TodaySort.Name))

    @Test fun streakIsLongestFirst() = assertEquals(listOf("Gym", "run", "water"), names(TodaySort.Streak))

    @Test fun toDoFirstPutsUndoneFirstKeepingCategoryOrder() =
        assertEquals(listOf("Gym", "run", "water"), names(TodaySort.ToDoFirst))

    @Test fun streakTiesFallBackToName() {
        val tied = listOf(CategoryGroup(HEALTH, listOf(item("b", false, 3, HEALTH), item("a", false, 3, HEALTH))))
        assertEquals(listOf("a", "b"), sortTodayItems(tied, TodaySort.Streak).map { it.habit.name })
    }
}
