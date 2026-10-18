package app.habittracker.fakes

import app.habittracker.domain.reminders.PlannedReminder
import app.habittracker.domain.reminders.ReminderScheduler

class FakeReminderScheduler : ReminderScheduler {
    var scheduled: List<PlannedReminder> = emptyList()
        private set
    var calls = 0
        private set
    var failNext: Throwable? = null

    override suspend fun replaceAll(reminders: List<PlannedReminder>) {
        calls++
        failNext?.let { failNext = null; throw it }
        scheduled = reminders
    }
}
