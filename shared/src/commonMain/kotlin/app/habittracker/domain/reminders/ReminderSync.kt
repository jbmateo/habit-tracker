package app.habittracker.domain.reminders

import app.habittracker.domain.model.Completion
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.toIndex
import app.habittracker.domain.repository.CompletionRepository
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime

/**
 * Keeps the platform's pending notifications equal to [planReminders] for the current habits and check-ins.
 * Scheduler failures are reported to [log] and never stop the sync.
 */
class ReminderSync(
    private val habits: HabitRepository,
    private val completions: CompletionRepository,
    private val clock: AppClock,
    private val scheduler: ReminderScheduler,
    private val log: (String) -> Unit = {},
    private val now: () -> LocalDateTime,
) {
    /** Re-plans whenever habits, check-ins or the day change, for as long as [scope] lives. */
    fun start(scope: CoroutineScope): Job =
        combine(habits.observeAll(), completions.observeAll(), clock.todayFlow) { h, c, _ -> h to c }
            .conflate()
            .onEach { (h, c) -> schedule(h, c) }
            .launchIn(scope)

    /** One-shot re-plan (app start, device boot, after "Mark done"). */
    suspend fun syncNow() = schedule(habits.observeAll().first(), completions.observeAll().first())

    private suspend fun schedule(habits: List<Habit>, completions: List<Completion>) {
        try {
            scheduler.replaceAll(planReminders(habits, completions.toIndex(), now()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log("Reminder scheduling failed: ${e.message}")
        }
    }
}
