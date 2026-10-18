# Habit Reminders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Optional per-habit local reminders that skip habits already done and offer a "Mark done" action, on Android and iOS.

**Architecture:** Shared Kotlin owns the rules: a pure `planReminders` computes the next 7 days of reminders; `ReminderSync` re-plans on every habit/completion/day change and hands the list to a platform `ReminderScheduler` (iOS: Kotlin `iosMain` over `UNUserNotificationCenter`; Android: `androidApp` over `AlarmManager`). "Mark done" calls the shared `MarkDoneFromReminder`.

**Tech Stack:** Kotlin 2.4.20 KMP, SQLDelight 2.4.1, Koin 4.2.2, kotlinx-datetime 0.8.0, SwiftUI (iOS 17), Jetpack Compose + Material 3.

**Spec:** `docs/design/2026-10-19-reminders-design.md` (read it before any task; § refs point there). Base app spec: `docs/design/2026-10-18-habit-tracker-design.md`.

## Global Constraints

- Packages under `app.habittracker`; `domain/` imports only `kotlin.*`, `kotlinx.coroutines.*`, `kotlinx.datetime.*`, `app.habittracker.domain.*` (ArchitectureTest enforces).
- Reminder time stored as `"HH:mm"` TEXT in `Habit.reminderTime`, NULL = none; domain type `LocalTime?`.
- Planner constants: `REMINDER_WINDOW_DAYS = 7`, `MAX_PLANNED_REMINDERS = 60`; reminder id `"habit:$habitId@$date"`.
- Copy (exact): title `"<emoji> <name>"`; body `"Time for your habit — keep your 🔥 <n>-day streak going!"` / `"Time for your habit"`; action `"Mark done"`; form hint `"Only on scheduled days. Skipped if you've already done it."`; denied note `"Notifications are off for Habits. Turn them on in Settings."`; failure `"Couldn't mark <name>. Open the app."`; form toggle `"Remind me"`; default time 09:00.
- Notification failures are caught and logged; they never crash or block saving.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Work on `main` (user's choice).
- iOS verification on simulator "iPhone 17 Pro" (UDID `81E64689-FC28-47D8-9976-7E9C8F4214A3`, iOS 27.0); Android on AVD `Pixel_8a`. iOS UI tests wipe the simulator DB — re-seed sample data afterwards (see task 8).
- Commands: `./gradlew :shared:allTests`, `./gradlew :shared:verifySqlDelightMigration`, `./gradlew :androidApp:testDebugUnitTest :androidApp:assembleDebug`, `./gradlew :androidApp:connectedDebugAndroidTest`, `cd iosApp && xcodegen && xcodebuild test -scheme HabitTracker -destination 'platform=iOS Simulator,id=81E64689-FC28-47D8-9976-7E9C8F4214A3'`.

## Review Focus

1. **A stale notification tapped days later** ("Mark done" for a date now outside the 2-day edit window) → no crash, quiet failure notice. Test in Task 3 (`markDoneOutsideWindowFails`).
2. **Habit deleted while its notification is still showing**, then "Mark done" → `HabitNotFound`, no crash. Test in Task 3 (`markDoneDeletedHabitFails`).
3. **Reminder time edited to earlier than now** → nothing for today, tomorrow still planned. Test in Task 2 (`pastTimeTodaySkippedTomorrowKept`).
4. **Archived habit with a reminder** → no reminders while archived, they return after unarchive. Tests in Task 2 (`archivedDaysSkipped`) and Task 3 (`unarchiveRestoresReminders`).
5. **Scheduler throws** (permission revoked, OS error) → sync keeps running and the next change re-plans. Test in Task 3 (`schedulerFailureIsSwallowedAndRetried`).

---

## File structure

```
shared/src/commonMain/sqldelight/app/habittracker/data/db/2.sqm        (new) ALTER TABLE Habit ADD COLUMN reminderTime TEXT
shared/src/commonMain/sqldelight/databases/3.db                          (generated, committed)
shared/src/commonMain/kotlin/app/habittracker/
  domain/model/Habit.kt                    + reminderTime: LocalTime?
  domain/reminders/PlannedReminder.kt      PlannedReminder, constants, ReminderEmoji, reminderTitle/Body
  domain/reminders/ReminderPlanner.kt      planReminders()
  domain/reminders/ReminderScheduler.kt    interface
  domain/reminders/ReminderSync.kt         ReminderSync
  domain/usecase/MarkDoneFromReminder.kt
  data/repository/SqlHabitRepository.kt    persist reminderTime
  di/Modules.kt                            register sync + use case; `nowLocal` provider
shared/src/iosMain/kotlin/app/habittracker/reminders/
  NotificationCenterGateway.kt             thin interface over UNUserNotificationCenter (+ real impl)
  IosReminderScheduler.kt
  IosReminders.kt                          category registration, markDone entry used by Swift
androidApp/src/main/kotlin/app/habittracker/android/reminders/
  ReminderChannel.kt, AndroidReminderScheduler.kt, ScheduledIdStore.kt,
  ReminderReceiver.kt, MarkDoneReceiver.kt, BootReceiver.kt
iosApp/HabitTracker/App/AppDelegate.swift  notification delegate
UI: form reminder section + subtitles on both platforms
```

---

### Task 1: Store a reminder time per habit

**Files:**
- Create: `shared/src/commonMain/sqldelight/app/habittracker/data/db/2.sqm`, generated `shared/src/commonMain/sqldelight/databases/3.db`
- Modify: `Habit.sq` (column + include `reminderTime` in `selectAllWithCategory`, `insert`, `updateDetails`), `domain/model/Habit.kt`, `domain/repository/HabitRepository.kt`, `data/repository/SqlHabitRepository.kt`, `domain/usecase/CreateHabit.kt`, `domain/usecase/UpdateHabit.kt`, `presentation/form/HabitFormViewModel.kt` (pass-through only: `null` for now), test fakes `FakeHabitRepository.kt`, `TestData.kt`
- Test: `androidHostTest/.../data/SqlRepositoriesTest.kt`, `commonTest/.../domain/usecase/CommandUseCasesTest.kt`

**Interfaces:**
- Produces: `Habit.reminderTime: LocalTime?` (last constructor param, default `null`); `HabitRepository.create(name, iconKey, categoryId, createdOn, days, reminderTime: LocalTime?)`; `HabitRepository.update(id, name, iconKey, categoryId, schedule: ScheduleVersion?, reminderTime: LocalTime?)`; `CreateHabit(name, iconKey, categoryId, days, reminderTime: LocalTime? = null)`; `UpdateHabit(id, name, iconKey, categoryId, days, reminderTime: LocalTime? = null)`; `TestData.habit(..., reminder: String? = null)` (parses `"HH:mm"`).

- [ ] **Step 1: Write failing tests**
  - `SqlRepositoriesTest.reminderTimeRoundTrips`: create with `LocalTime(7, 30)` → `get(id)!!.reminderTime == LocalTime(7, 30)`; `update(..., reminderTime = null)` → `null`.
  - `CommandUseCasesTest.createAndUpdateCarryReminderTime`: `CreateHabit(..., reminderTime = LocalTime(21, 0))` then `UpdateHabit(..., reminderTime = LocalTime(6, 45))` → stored `06:45`.
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "*SqlRepositoriesTest*" --tests "*CommandUseCasesTest*"` → FAIL (unresolved `reminderTime`).
- [ ] **Step 3: Implement.** `2.sqm`: `ALTER TABLE Habit ADD COLUMN reminderTime TEXT;` (also add the column to the `CREATE TABLE` in `Habit.sq`). Store via `LocalTime.toString()` truncated to `HH:mm` (`"%02d:%02d"`), parse with `LocalTime.parse`. Then `./gradlew :shared:generateCommonMainHabitDatabaseSchema` → `3.db`.
- [ ] **Step 4: Run** `./gradlew :shared:allTests :shared:verifySqlDelightMigration :androidApp:assembleDebug` → PASS; `xcodebuild build` for iOS still compiles (Swift callers unchanged thanks to defaults).
- [ ] **Step 5: Commit** `feat(reminders): store an optional reminder time per habit`

---

### Task 2: Reminder planner (pure)

**Files:**
- Create: `shared/src/commonMain/kotlin/app/habittracker/domain/reminders/PlannedReminder.kt`, `.../domain/reminders/ReminderPlanner.kt`
- Test: `shared/src/commonTest/kotlin/app/habittracker/domain/reminders/ReminderPlannerTest.kt`

**Interfaces:**
- Consumes: `Habit.isDueOn`, `Habit.reminderTime`, `CompletionIndex`, `habitStats(habit, doneDates, today).currentStreak`.
- Produces (spec §4, exactly):
```kotlin
data class PlannedReminder(val habitId: String, val habitName: String, val iconKey: String,
                           val date: LocalDate, val time: LocalTime, val streak: Int) {
    val id: String get() = "habit:$habitId@$date"
    val title: String get()   // "<ReminderEmoji.forIconKey(iconKey)> <habitName>"
    val body: String get()    // streak > 0 ? "Time for your habit — keep your 🔥 $streak-day streak going!" : "Time for your habit"
}
const val REMINDER_WINDOW_DAYS = 7
const val MAX_PLANNED_REMINDERS = 60
object ReminderEmoji { fun forIconKey(key: String): String }   // map over IconKeys.all, fallback "✅"
fun planReminders(habits: List<Habit>, completions: CompletionIndex, now: LocalDateTime,
                  days: Int = REMINDER_WINDOW_DAYS): List<PlannedReminder>
```
  Emoji map (exact): run 🏃, walk 🚶, gym 🏋️, bike 🚴, swim 🏊, yoga 🧘, meditate 🧘, water 💧, nutrition 🥕, food 🍽️, sleep 😴, pill 💊, heart ❤️, book 📖, study 🎓, code 💻, language 🗣️, journal ✍️, music 🎵, art 🎨, pray 🙏, sun ☀️, clean 🧹, money 💰, no_phone 📵, call 📞, people 👥, plant 🌱, pet 🐾, checklist ✅, timer ⏱️, target 🎯, star ⭐, smile 🙂, no_smoking 🚭, home 🏠.

- [ ] **Step 1: Write failing tests** (`now = LocalDateTime(2026, 10, 14, 8, 0)`, Wednesday):
  - `onlyHabitsWithReminderTime`
  - `scheduledDaysOnly` (Mon/Wed/Fri habit at 09:00 → dates 14, 16, 19 in the window 14–20)
  - `pastTimeTodaySkippedTomorrowKept` (reminder 07:00, now 08:00 → first is the 15th) *(Review Focus 3)*
  - `doneDaysSkipped` (done on the 15th → no entry for the 15th)
  - `archivedDaysSkipped` (open archive period from the 16th → nothing from the 16th) *(Review Focus 4)*
  - `notBeforeCreatedOn`
  - `sortedByDateThenTimeThenName`
  - `cappedAtSixtyKeepingEarliest` (10 daily habits → 60 entries, last date ≤ the 19th)
  - `idTitleAndBody` (id `"habit:a@2026-10-15"`; title `"🏃 Run"`; body with streak 3 → `"Time for your habit — keep your 🔥 3-day streak going!"`; streak 0 → `"Time for your habit"`)
  - `emojiFallback` (`ReminderEmoji.forIconKey("rocket") == "✅"`; every `IconKeys.all` key mapped)
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "*ReminderPlannerTest*"` → FAIL.
- [ ] **Step 3: Implement** per spec §4.
- [ ] **Step 4: Run** the same + `ArchitectureTest` → PASS.
- [ ] **Step 5: Commit** `feat(reminders): planner for the next 7 days of reminders`

---

### Task 3: Sync, scheduler contract, Mark-done use case, DI

**Files:**
- Create: `domain/reminders/ReminderScheduler.kt`, `domain/reminders/ReminderSync.kt`, `domain/usecase/MarkDoneFromReminder.kt`; test fake `commonTest/.../fakes/FakeReminderScheduler.kt`
- Modify: `di/Modules.kt` (register `ReminderSync` single, `MarkDoneFromReminder` factory, `named("nowLocal")` provider `() -> LocalDateTime` from `Clock.System` + `TimeZone.currentSystemDefault()`), `androidHostTest/.../di/KoinGraphTest.kt` (test platform module binds `FakeReminderScheduler`)
- Test: `commonTest/.../domain/reminders/ReminderSyncTest.kt`, `commonTest/.../domain/usecase/MarkDoneFromReminderTest.kt`

**Interfaces:**
```kotlin
interface ReminderScheduler { suspend fun replaceAll(reminders: List<PlannedReminder>) }
class ReminderSync(habits: HabitRepository, completions: CompletionRepository, clock: AppClock,
                   scheduler: ReminderScheduler, now: () -> LocalDateTime) {
    fun start(scope: CoroutineScope): Job       // combine(observeAll, observeAll, todayFlow).conflate() → plan → replaceAll; catch+log
    suspend fun syncNow()                        // read current lists once, plan, replaceAll; catch+log
}
class MarkDoneFromReminder(habits: HabitRepository, completions: CompletionRepository, toggle: ToggleCompletion) {
    suspend operator fun invoke(habitId: String, date: LocalDate): Outcome<Unit>
}
```
- `ReminderScheduler` is bound by each platform (Tasks 5 and 7), **not** in `sharedModule`.
- `FakeReminderScheduler`: records the last list; `var failNext: Throwable?`.

- [ ] **Step 1: Write failing tests**
  - Sync (fake repos, `FakeAppClock`, `now` lambda over a mutable value):
    - `initialPlanIsScheduled`
    - `checkingOffRemovesTodaysReminder`
    - `uncheckingRestoresIt`
    - `editingTimeReplans`
    - `dayChangeMovesWindow`
    - `deletingHabitRemovesItsReminders`
    - `unarchiveRestoresReminders` *(Review Focus 4)*
    - `schedulerFailureIsSwallowedAndRetried` — `failNext` set, then a completion change → second call succeeds, sync job still active *(Review Focus 5)*
  - Mark done:
    - `markDoneMarksUndoneHabit`
    - `markDoneOnAlreadyDoneHabitKeepsItDone` (never un-checks)
    - `markDoneOutsideWindowFails` → `Failure(EditWindowClosed)` *(Review Focus 1)*
    - `markDoneDeletedHabitFails` → `Failure(HabitNotFound)` *(Review Focus 2)*
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "*ReminderSyncTest*" --tests "*MarkDoneFromReminderTest*"` → FAIL.
- [ ] **Step 3: Implement**; register in `Modules.kt`; extend `KoinGraphTest` to resolve `ReminderSync` and `MarkDoneFromReminder`.
- [ ] **Step 4: Run** `./gradlew :shared:allTests` → PASS.
- [ ] **Step 5: Commit** `feat(reminders): sync, scheduler contract and mark-done use case`

---

### Task 4: Habit form reminder state

**Files:**
- Modify: `presentation/form/HabitFormContract.kt`, `presentation/form/HabitFormViewModel.kt`
- Test: `commonTest/.../presentation/HabitFormViewModelTest.kt`

**Interfaces:**
- Produces: `HabitFormUiState.reminderEnabled: Boolean`, `HabitFormUiState.reminderTime: LocalTime` (default `LocalTime(9, 0)`); events `HabitFormEvent.ReminderToggled`, `HabitFormEvent.ReminderTimeChanged(val time: LocalTime)`. Save passes `reminderTime = if (reminderEnabled) reminderTime else null`. Edit loads `reminderEnabled = habit.reminderTime != null`, `reminderTime = habit.reminderTime ?: 09:00`. Inputs disabled while loading/saving as today.

- [ ] **Step 1: Write failing tests:** `newHabitReminderOffAtNine`, `toggleAndTimeSavedOnCreate` (toggle on, 21:30 → stored 21:30), `toggleOffSavesNull`, `editLoadsExistingReminder`.
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "*HabitFormViewModelTest*"` → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :shared:allTests` → PASS.
- [ ] **Step 5: Commit** `feat(reminders): reminder toggle and time in the habit form state`

---

### Task 5: iOS scheduler and "Mark done" plumbing

**Files:**
- Create: `shared/src/iosMain/kotlin/app/habittracker/reminders/NotificationCenterGateway.kt` (interface `NotificationCenterGateway { suspend fun pendingIds(): List<String>; fun add(id, title, body, date, time, habitId); fun remove(ids: List<String>) }` + `UserNotificationCenterGateway` over `UNUserNotificationCenter.currentNotificationCenter()`), `IosReminderScheduler.kt` (implements `ReminderScheduler` via the gateway; only ids starting `"habit:"`; adds missing, removes absent), `IosReminders.kt` (`object IosReminders { fun registerCategory(); suspend fun markDone(habitId: String, isoDate: String): Boolean }`)
- Modify: `shared/src/iosMain/.../di/PlatformModule.ios.kt` (bind `ReminderScheduler`), `KoinHelper.start(...)` (after `initKoin`, `registerCategory()` and `get<ReminderSync>().start(appScope)` with a `MainScope()`), `iosApp/HabitTracker/App/AppDelegate.swift` (new), `HabitTrackerApp.swift` (`@UIApplicationDelegateAdaptor`), `iosApp/project.yml` if needed
- Test: `shared/src/iosTest/kotlin/app/habittracker/reminders/IosReminderSchedulerTest.kt` (fake gateway)

**Interfaces:**
- Notification request: id = `PlannedReminder.id`; content title/body from `PlannedReminder`; `categoryIdentifier = "HABIT_REMINDER"`; `userInfo = ["habitId": …, "date": "yyyy-MM-dd"]`; trigger `UNCalendarNotificationTrigger(dateMatching: year/month/day/hour/minute, repeats: false)`.
- Category `HABIT_REMINDER` with action id `"MARK_DONE"`, title `"Mark done"`, options `[]` (no foreground).
- `AppDelegate` is `UNUserNotificationCenterDelegate`: `MARK_DONE` → `KoinHelper.start()` then `IosReminders.markDone`; on `false` post an immediate notification `"Couldn't mark <name>. Open the app."` (name = the reminder's title without the emoji). `willPresent` → `[.banner, .sound]`.

- [ ] **Step 1: Write failing test** `IosReminderSchedulerTest`: `replaceAllAddsMissingAndRemovesStale` (pending `habit:a@…14`, `habit:b@…15`, `other:x`; plan `a@14`, `c@16` → removes `b@15`, adds `c@16`, leaves `other:x`).
- [ ] **Step 2: Run** `./gradlew :shared:iosSimulatorArm64Test --tests "*IosReminderSchedulerTest*"` → FAIL.
- [ ] **Step 3: Implement** the Kotlin side, then Swift `AppDelegate` wiring.
- [ ] **Step 4: Run** `./gradlew :shared:allTests` and `xcodebuild build` → PASS. Manual (iPhone 17 Pro): allow notifications, set a habit reminder 2 min ahead via the DB or Task 6 UI → banner arrives; "Mark done" from it → habit done in the app.
- [ ] **Step 5: Commit** `feat(reminders): iOS notification scheduling and Mark done action`

---

### Task 6: iOS reminder UI

**Files:**
- Modify: `iosApp/HabitTracker/Screens/Form/HabitFormView.swift` (Reminder section), `Screens/Detail/HabitDetailView.swift` and `Screens/Habits/HabitListView.swift` (subtitle `" · ⏰ <time>"` using `Date.FormatStyle` `.hour().minute()`), `HabitTrackerUITests/SmokeUITests.swift`
- Permission: on first toggle-on, `UNUserNotificationCenter.requestAuthorization([.alert, .sound, .badge])`; if status is `.denied`, show the denied note and an "Open Settings" button (`UIApplication.openSettingsURLString`).

- [ ] **Step 1: Write failing UI test** `testReminderToggleShowsTimePicker` (launch with `-uiTestResetDB -uiTestToday 2026-10-14`, open create form, tap "Remind me" → a date picker element exists and the hint text `"Only on scheduled days. Skipped if you've already done it."` is shown).
- [ ] **Step 2: Run** `xcodebuild test … -only-testing:HabitTrackerUITests/SmokeUITests/testReminderToggleShowsTimePicker` → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** full `xcodebuild test …` → PASS; screenshot form + detail subtitle on iPhone 17 Pro.
- [ ] **Step 5: Commit** `feat(reminders): iOS reminder controls and display`

---

### Task 7: Android scheduler, receivers and UI

**Files:**
- Create in `androidApp/src/main/kotlin/app/habittracker/android/reminders/`: `ReminderChannel.kt` (`ensureReminderChannel(context)`, id `"habit_reminders"`, name `"Habit reminders"`), `ScheduledIdStore.kt` (SharedPreferences-backed `Set<String>`), `AndroidReminderScheduler.kt` (`ReminderScheduler`; `setExactAndAllowWhileIdle` when `canScheduleExactAlarms()`, else `setAndAllowWhileIdle`; request code = `id.hashCode()`; extras `habitId`, `date`, `title`, `body`), `ReminderReceiver.kt` (posts notification with "Mark done" action), `MarkDoneReceiver.kt` (`goAsync()`, ensures Koin via `(applicationContext as HabitApp)`, runs `MarkDoneFromReminder`, cancels notification, `syncNow()`; on failure posts `"Couldn't mark <name>. Open the app."`), `BootReceiver.kt` (`syncNow()`)
- Modify: `AndroidManifest.xml` (receivers; permissions `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`), `HabitApp.kt` (channel, bind scheduler module via `initKoin { modules(...) }`, start `ReminderSync` in an app scope), form UI `ui/form/HabitFormSheet.kt` (Reminder section with Material 3 `TimePicker` dialog; request `POST_NOTIFICATIONS` on first toggle-on with `rememberLauncherForActivityResult`; denied note + Open Settings), `ui/detail/HabitDetailScreen.kt`, `ui/habits/HabitListScreen.kt` (subtitle `" · ⏰ <time>"` via `DateFormat.getTimeFormat`)
- Test: `androidApp/src/test/.../reminders/AndroidReminderSchedulerTest.kt` (bookkeeping with a fake alarm sink), `androidApp/src/androidTest/.../SmokeTest.kt` (+ `reminderToggleShowsTimeField`)

**Interfaces:**
- `AndroidReminderScheduler(context, alarms: AlarmSink = RealAlarmSink(context), store: ScheduledIdStore)`, where `interface AlarmSink { fun set(id: String, atMillis: Long, extras: Bundle, exact: Boolean); fun cancel(id: String) }` so the unit test can fake it.

- [ ] **Step 1: Write failing tests:** `AndroidReminderSchedulerTest.replaceAllSetsNewAndCancelsRemoved` (store has `a@14`, `b@15`; plan `a@14`, `c@16` → cancel `b@15`, set `c@16`, store = {`a@14`, `c@16`}); `SmokeTest.reminderToggleShowsTimeField` (tap "Remind me" → hint text shown and the time button reads "9:00 AM" or "09:00" per locale — assert the hint).
- [ ] **Step 2: Run** `./gradlew :androidApp:testDebugUnitTest --tests "*AndroidReminderSchedulerTest*"` → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest` → PASS. Manual on Pixel_8a: reminder 2 min ahead → notification; "Mark done" with the app swiped away → done; check off beforehand → no notification.
- [ ] **Step 5: Commit** `feat(reminders): Android alarms, notification actions and reminder controls`

---

### Task 8: End-to-end verification and docs

- [ ] **Step 1:** Run every command in Global Constraints → all PASS.
- [ ] **Step 2:** Manual on both platforms (spec §9 manual list), with screenshots: arrival, Mark done, suppression when already done.
- [ ] **Step 3:** README: add a "Reminders" bullet under features and a note on notification/exact-alarm permissions.
- [ ] **Step 4:** Re-seed the iPhone 17 Pro sample data (3 habits, completions Oct 1–18 as before) since UI tests wipe it.
- [ ] **Step 5: Commit** `docs: reminders in README`
