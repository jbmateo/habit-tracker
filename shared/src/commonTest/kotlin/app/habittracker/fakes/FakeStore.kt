package app.habittracker.fakes

import app.habittracker.domain.model.Category
import app.habittracker.domain.model.Completion
import app.habittracker.domain.model.Habit
import kotlinx.coroutines.flow.MutableStateFlow

/** Shared in-memory state behind the three fake repositories. */
class FakeStore {
    val categories = MutableStateFlow<List<Category>>(emptyList())
    val habits = MutableStateFlow<List<Habit>>(emptyList())
    val completions = MutableStateFlow<List<Completion>>(emptyList())

    /** The next mutating repository call throws this, then it is cleared. */
    var failNextWrite: Throwable? = null

    private var nextId = 1

    fun newId(prefix: String): String = "$prefix-${nextId++}"

    fun checkWrite() {
        failNextWrite?.let { failNextWrite = null; throw it }
    }
}
