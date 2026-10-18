package app.habittracker.fakes

import app.habittracker.domain.model.Category
import app.habittracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeCategoryRepository(private val store: FakeStore) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> =
        store.categories.map { list -> list.sortedBy { it.sortOrder } }

    override suspend fun get(id: String): Category? = store.categories.value.firstOrNull { it.id == id }

    override suspend fun create(name: String, colorKey: String): String {
        store.checkWrite()
        val id = store.newId("c")
        val order = (store.categories.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
        store.categories.value += Category(id, name, colorKey, order)
        return id
    }

    override suspend fun update(id: String, name: String, colorKey: String) {
        store.checkWrite()
        store.categories.value = store.categories.value.map {
            if (it.id == id) it.copy(name = name, colorKey = colorKey) else it
        }
        val updated = get(id) ?: return
        store.habits.value = store.habits.value.map {
            if (it.category.id == id) it.copy(category = updated) else it
        }
    }

    override suspend fun delete(id: String) {
        store.checkWrite()
        store.categories.value = store.categories.value.filterNot { it.id == id }
    }

    override suspend fun habitCount(id: String): Long =
        store.habits.value.count { it.category.id == id }.toLong()

    override suspend fun isEmpty(): Boolean = store.categories.value.isEmpty()
}
