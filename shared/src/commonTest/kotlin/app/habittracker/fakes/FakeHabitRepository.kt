package app.habittracker.fakes

import app.habittracker.domain.model.ArchivePeriod
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.ScheduleVersion
import app.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class FakeHabitRepository(private val store: FakeStore) : HabitRepository {
    override fun observeAll(): Flow<List<Habit>> =
        store.habits.map { list -> list.sortedWith(compareBy({ it.createdOn }, { it.name })) }

    override fun observe(id: String): Flow<Habit?> =
        store.habits.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun get(id: String): Habit? = store.habits.value.firstOrNull { it.id == id }

    override suspend fun create(
        name: String,
        iconKey: String,
        categoryId: String,
        createdOn: LocalDate,
        days: Set<DayOfWeek>,
    ): String {
        store.checkWrite()
        val category = store.categories.value.first { it.id == categoryId }
        val id = store.newId("h")
        store.habits.value += Habit(
            id = id,
            name = name,
            iconKey = iconKey,
            category = category,
            createdOn = createdOn,
            schedules = listOf(ScheduleVersion(createdOn, days)),
            archivePeriods = emptyList(),
        )
        return id
    }

    override suspend fun update(id: String, name: String, iconKey: String, categoryId: String, schedule: ScheduleVersion?) {
        store.checkWrite()
        val category = store.categories.value.first { it.id == categoryId }
        modify(id) { h ->
            val schedules = if (schedule == null) h.schedules else
                h.schedules.filterNot { it.effectiveFrom == schedule.effectiveFrom } + schedule
            h.copy(name = name, iconKey = iconKey, category = category, schedules = schedules)
        }
    }

    override suspend fun upsertSchedule(id: String, effectiveFrom: LocalDate, days: Set<DayOfWeek>) {
        store.checkWrite()
        modify(id) { h ->
            h.copy(schedules = h.schedules.filterNot { it.effectiveFrom == effectiveFrom } +
                ScheduleVersion(effectiveFrom, days))
        }
    }

    override suspend fun openArchivePeriod(id: String, from: LocalDate) {
        store.checkWrite()
        modify(id) { it.copy(archivePeriods = it.archivePeriods + ArchivePeriod(from, null)) }
    }

    override suspend fun closeArchivePeriod(id: String, from: LocalDate, toExclusive: LocalDate) {
        store.checkWrite()
        modify(id) { h ->
            h.copy(archivePeriods = h.archivePeriods.map {
                if (it.from == from) it.copy(toExclusive = toExclusive) else it
            })
        }
    }

    override suspend fun deleteArchivePeriod(id: String, from: LocalDate) {
        store.checkWrite()
        modify(id) { h -> h.copy(archivePeriods = h.archivePeriods.filterNot { it.from == from }) }
    }

    override suspend fun delete(id: String) {
        store.checkWrite()
        store.habits.value = store.habits.value.filterNot { it.id == id }
        store.completions.value = store.completions.value.filterNot { it.habitId == id }
    }

    private fun modify(id: String, change: (Habit) -> Habit) {
        store.habits.value = store.habits.value.map { if (it.id == id) change(it) else it }
    }
}
