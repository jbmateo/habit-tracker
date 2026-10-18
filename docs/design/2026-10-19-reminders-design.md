# Habit Reminders — Design Spec

**Date:** 2026-10-19
**Status:** Draft for review
**Builds on:** `2026-10-18-habit-tracker-design.md` (v1, merged)

## 1. Goal

Optional, per-habit local reminders that nudge only when a habit is still open, and let the user check it off straight from the notification. Fully offline; no server.

### Success criteria
- A habit can have at most one reminder time; it fires only on the habit's scheduled days.
- No reminder fires for a habit already done that day.
- Tapping **Mark done** on the notification records the check-in without opening the app — including when the app isn't running — and never un-checks a done habit.
- Android and iOS behave identically; all rules live in shared, unit-tested code.
- Notification failures never crash the app or block saving a habit.

## 2. Scope

**In:** one optional reminder time per habit; skip-if-done; "Mark done" notification action; permission prompt on first use; denied-permission note with "Open Settings"; reminder shown on habit detail and Habits list rows.

**Out:** multiple times per habit, a daily summary nudge, snooze, custom sounds, reminders for past days.

## 3. Data

Migration `2.sqm` → schema v3:

```sql
ALTER TABLE Habit ADD COLUMN reminderTime TEXT;   -- "HH:mm" local time; NULL = no reminder
```

- Commit generated `3.db`; `verifySqlDelightMigration` must pass.
- Domain `Habit` gains `reminderTime: LocalTime?` (kotlinx-datetime). Not versioned — a change only affects future reminders.
- `CreateHabit(name, iconKey, categoryId, days, reminderTime: LocalTime?)` and `UpdateHabit(id, …, reminderTime: LocalTime?)`; `HabitRepository.create`/`update` persist it inside their existing single transaction.

## 4. Planner (shared, pure)

```kotlin
data class PlannedReminder(
    val habitId: String,
    val habitName: String,
    val iconKey: String,
    val date: LocalDate,
    val time: LocalTime,
    val streak: Int,
) { val id: String get() = "habit:$habitId@$date" }

const val REMINDER_WINDOW_DAYS = 7
const val MAX_PLANNED_REMINDERS = 60

fun planReminders(
    habits: List<Habit>,
    completions: CompletionIndex,
    now: LocalDateTime,
    days: Int = REMINDER_WINDOW_DAYS,
): List<PlannedReminder>
```

For each date `D` in `now.date .. now.date + (days − 1)` and each habit with `reminderTime != null`, plan a reminder iff **all** hold:
1. `habit.isDueOn(D)` (schedule history, archive periods, `createdOn` already handled there);
2. the habit is **not** done on `D`;
3. `LocalDateTime(D, reminderTime) > now`.

Result sorted by `(date, time, habitName)`; truncated to the first `MAX_PLANNED_REMINDERS` (earliest kept — iOS allows 64 pending). `streak` = the habit's current streak (`habitStats(...).currentStreak`) at planning time.

**Notification text** (shared, so both platforms match):
- title: `"<emoji> <habit name>"` — emoji from `ReminderEmoji.forIconKey(iconKey)`, a small map over `IconKeys.all`, fallback `"✅"`.
- body: `streak > 0` → `"Time for your habit — keep your 🔥 <streak>-day streak going!"`, else `"Time for your habit"`.
- action label: `"Mark done"`.

## 5. Sync (shared)

```kotlin
interface ReminderScheduler {
    /** Make pending reminders exactly [reminders]: add missing ids, remove ids no longer present. */
    suspend fun replaceAll(reminders: List<PlannedReminder>)
}

class ReminderSync(habits: HabitRepository, completions: CompletionRepository, clock: AppClock,
                   scheduler: ReminderScheduler, now: () -> LocalDateTime) {
    fun start(scope: CoroutineScope)   // collect combine(habits, completions, clock.todayFlow) → plan → replaceAll
    suspend fun syncNow()              // one-shot (app start, boot, after "Mark done")
}
```
- Started once at app start (Android `HabitApp`, iOS `KoinHelper.start`), in an app-lifetime scope.
- Re-plans on any habit/completion change and on day change; `conflate()` so bursts collapse.
- Exceptions from the scheduler are caught and logged; never propagated.

## 6. Mark done from a notification

```kotlin
class MarkDoneFromReminder(habits: HabitRepository, completions: CompletionRepository, toggle: ToggleCompletion) {
    suspend operator fun invoke(habitId: String, date: LocalDate): Outcome<Unit>
}
```
- Already done → `Success` with no change (never un-checks).
- Otherwise delegates to `ToggleCompletion` (same edit-window and due rules) and maps its result.
- Failure (e.g. `EditWindowClosed`, `HabitNotFound`) → platform posts a quiet notification: `"Couldn't mark <name>. Open the app."`

## 7. Platforms

### iOS
- `IosReminderScheduler` over `UNUserNotificationCenter`: one non-repeating `UNCalendarNotificationTrigger` per reminder (year/month/day/hour/minute), request id = `PlannedReminder.id`; only manages ids with prefix `habit:`. `userInfo` carries `habitId` and ISO `date`.
- Category `HABIT_REMINDER` with action `MARK_DONE` ("Mark done", no foreground launch).
- `AppDelegate` (`UIApplicationDelegateAdaptor`) is the `UNUserNotificationCenterDelegate`: `MARK_DONE` → `KoinHelper.start()` then `MarkDoneFromReminder`; default tap → app opens on Today; `willPresent` shows a banner while foregrounded.
- Permission: requested (`.alert, .sound, .badge`) the first time the form's reminder toggle is switched on.

### Android
- Channel `habit_reminders` ("Habit reminders", default importance), created at app start.
- `AndroidReminderScheduler`: `AlarmManager.setExactAndAllowWhileIdle` per reminder, `PendingIntent` request code from the id's hash, extras `habitId`, `date`, title, body. Keeps the set of scheduled ids in `SharedPreferences` to cancel removed ones. If exact alarms aren't permitted (Android 12+), falls back to `setAndAllowWhileIdle`.
- `ReminderReceiver` posts the notification with a **Mark done** action → `MarkDoneReceiver`, which ensures Koin is started (`goAsync()`), runs `MarkDoneFromReminder`, cancels the notification, and triggers `syncNow()`.
- `BootReceiver` (`BOOT_COMPLETED`) → `syncNow()`.
- Permissions: `POST_NOTIFICATIONS` (13+) requested on first reminder toggle; `SCHEDULE_EXACT_ALARM` declared; `RECEIVE_BOOT_COMPLETED`.

## 8. UI

**Habit form** — new "Reminder" section below Days:
- Toggle "Remind me" (default off). On → time picker (default 09:00): iOS `DatePicker(.hourAndMinute)`, Android Material 3 `TimePicker` in a dialog.
- Hint: "Only on scheduled days. Skipped if you've already done it."
- Permission denied → note "Notifications are off for Habits. Turn them on in Settings." with "Open Settings". The reminder time is still saved.
- `HabitFormUiState` gains `reminderEnabled: Boolean`, `reminderTime: LocalTime`; events `ReminderToggled`, `ReminderTimeChanged(LocalTime)`. Saved as `reminderTime` when enabled, `null` otherwise.

**Habit detail** and **Habits list** subtitle: append `" · ⏰ 7:00"` (device-locale time format) when a reminder is set.

## 9. Testing

Shared (`commonTest`, fakes + `FakeAppClock`):
- `planReminders`: due days only (incl. schedule history), archived/pre-creation skipped, done days skipped, today before/after the time, 7-day window, 60 cap keeps earliest, stable ids, streak in text.
- `ReminderSync` with a fake scheduler: check-off removes today's, un-check restores, time edit re-plans, day change shifts window, delete removes, scheduler exception swallowed.
- `MarkDoneFromReminder`: not done → done; done → unchanged; outside edit window → failure.
- `HabitFormViewModel`: reminder toggle/time saved and loaded; off → `null`.
- Data: `reminderTime` round-trip; `verifySqlDelightMigration`.

Platform:
- Android: scheduler bookkeeping unit test (adds/cancels by id); Compose UI test that the toggle reveals the time field.
- iOS: `IosReminderScheduler` against a fake notification-center protocol (adds/removes ids); UI test that the toggle reveals the time picker.

Manual (iPhone 17 Pro simulator, Pixel 8a emulator): reminder 2 minutes ahead arrives; "Mark done" checks it off in the app; checking off beforehand suppresses it; app killed → "Mark done" still works.

## 10. Risks
- **iOS pending-notification limit (64):** capped at 60 by the planner.
- **Android exact-alarm restrictions:** fallback to inexact alarms (may drift a few minutes).
- **Process death on Android:** `MarkDoneReceiver` uses `goAsync()` and starts Koin itself.
- **Time-zone change:** reminders are planned in local time; the existing day-change signal also fires on time-zone change and re-plans.
