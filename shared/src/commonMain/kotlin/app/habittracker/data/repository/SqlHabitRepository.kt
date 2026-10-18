package app.habittracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.habittracker.data.db.DaysMask
import app.habittracker.data.db.HabitDatabase
import app.habittracker.data.db.HabitArchivePeriod
import app.habittracker.data.db.HabitSchedule
import app.habittracker.data.db.SelectAllWithCategory
import app.habittracker.domain.model.ArchivePeriod
import app.habittracker.domain.model.Category
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.ScheduleVersion
import app.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SqlHabitRepository(
    private val db: HabitDatabase,
    private val io: CoroutineDispatcher,
) : HabitRepository {
    private val habits = db.habitQueries
    private val schedules = db.habitScheduleQueries
    private val periods = db.habitArchivePeriodQueries

    override fun observeAll(): Flow<List<Habit>> =
        combine(
            habits.selectAllWithCategory().asFlow().mapToList(io),
            schedules.selectAll().asFlow().mapToList(io),
            periods.selectAll().asFlow().mapToList(io),
        ) { rows, scheds, pers ->
            val schedByHabit = scheds.groupBy { it.habitId }
            val perByHabit = pers.groupBy { it.habitId }
            rows.map { hydrate(it, schedByHabit[it.id].orEmpty(), perByHabit[it.id].orEmpty()) }
        }

    override fun observe(id: String): Flow<Habit?> = observeAll().map { list -> list.firstOrNull { it.id == id } }

    override suspend fun get(id: String): Habit? = withContext(io) {
        val row = habits.selectOneWithCategory(id).executeAsOneOrNull() ?: return@withContext null
        hydrate(
            SelectAllWithCategory(
                row.id, row.name, row.iconKey, row.createdOn,
                row.categoryId, row.categoryName, row.colorKey, row.sortOrder,
            ),
            schedules.selectForHabit(id).executeAsList(),
            periods.selectForHabit(id).executeAsList(),
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(
        name: String,
        iconKey: String,
        categoryId: String,
        createdOn: LocalDate,
        days: Set<DayOfWeek>,
    ): String = withContext(io) {
        val id = Uuid.random().toString()
        db.transaction {
            habits.insert(id, name, iconKey, categoryId, createdOn.toString())
            schedules.upsert(id, createdOn.toString(), DaysMask.encode(days).toLong())
        }
        id
    }

    override suspend fun updateDetails(id: String, name: String, iconKey: String, categoryId: String) {
        withContext(io) { habits.updateDetails(name, iconKey, categoryId, id) }
    }

    override suspend fun upsertSchedule(id: String, effectiveFrom: LocalDate, days: Set<DayOfWeek>) {
        withContext(io) { schedules.upsert(id, effectiveFrom.toString(), DaysMask.encode(days).toLong()) }
    }

    override suspend fun openArchivePeriod(id: String, from: LocalDate) {
        withContext(io) { periods.insertOpen(id, from.toString()) }
    }

    override suspend fun closeArchivePeriod(id: String, from: LocalDate, toExclusive: LocalDate) {
        withContext(io) { periods.close(toExclusive.toString(), id, from.toString()) }
    }

    override suspend fun deleteArchivePeriod(id: String, from: LocalDate) {
        withContext(io) { periods.deleteOne(id, from.toString()) }
    }

    override suspend fun delete(id: String) {
        withContext(io) { habits.deleteById(id) }
    }

    private fun hydrate(row: SelectAllWithCategory, scheds: List<HabitSchedule>, pers: List<HabitArchivePeriod>) = Habit(
        id = row.id,
        name = row.name,
        iconKey = row.iconKey,
        category = Category(row.categoryId, row.categoryName, row.colorKey, row.sortOrder.toInt()),
        createdOn = LocalDate.parse(row.createdOn),
        schedules = scheds.map { ScheduleVersion(LocalDate.parse(it.effectiveFrom), DaysMask.decode(it.daysMask.toInt())) }
            .sortedBy { it.effectiveFrom },
        archivePeriods = pers.map { ArchivePeriod(LocalDate.parse(it.fromDate), it.toDate?.let(LocalDate::parse)) }
            .sortedBy { it.from },
    )
}
