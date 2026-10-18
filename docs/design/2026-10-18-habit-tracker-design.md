# Habit Tracker — Design Spec

**Date:** 2026-10-18
**Status:** Draft for review

## 1. Goal

An offline-first habit tracker built with Kotlin Multiplatform. It has two equal purposes:

1. **A daily-use app on the user's iPhone.** The iOS experience must be reliable and feel native.
2. **A clean reference for KMP + Clean Architecture + MVVM.** Layering and shared-code boundaries should be easy to read and enforced by tests.

Android ships from the same codebase and gets the same features.

### Success criteria

- Daily check-in on iPhone takes seconds: open the app, tap the habits, done.
- Streaks and calendar marks are always correct under the rules in §5, verified by unit tests.
- All data lives on the device. The app works with no network at all.
- `domain` has no framework dependencies, and an architecture test enforces this.
- All business and presentation logic is shared. Platform code only renders state and forwards events.

## 2. Scope

### In v1
- **Today dashboard:** progress for today, overall streak (current and best), today's habits grouped by category, toggles.
- **Create and edit habits:** name, category, icon, schedule (every day or chosen weekdays).
- **Categories:** each has a name and color. Seeded with defaults and fully editable.
- **Calendar:** month view with Perfect (🔥), Partial, Missed, and No-habits markers. Tap a day to see its breakdown.
- **Habit detail:** per-habit current and best streak, completion %, and a mini calendar.
- **Archive and unarchive habits** (history kept), and **delete** (history erased).
- **Edits allowed for today and yesterday only.**

### Out of v1
Reminders and notifications, reordering, sync/backup/accounts, widgets, count-based habits (e.g. 3/8 glasses), "X times per week" schedules, stats charts.

## 3. Platforms & stack

| Concern | Choice |
|---|---|
| Shared code | Kotlin Multiplatform, single `shared` module |
| Targets | `androidTarget`, `iosArm64`, `iosSimulatorArm64` |
| iOS UI | SwiftUI, iOS 17+ (`@Observable`, `NavigationStack`) |
| Android UI | Jetpack Compose + Material 3, minSdk 26 |
| Shared ViewModels | `androidx.lifecycle:lifecycle-viewmodel` (KMP) |
| Swift interop | SKIE (Flows → `AsyncSequence`, sealed → Swift enums, suspend → async) |
| Persistence | SQLDelight 2.x (native driver on iOS, Android driver on Android) |
| DI | Koin 4.x |
| Dates | kotlinx-datetime |
| Testing | kotlin.test, kotlinx-coroutines-test, Turbine; Compose UI test; XCUITest |
| Build | Gradle with version catalog (`gradle/libs.versions.toml`) |

### Pinned versions (verified against Maven on 2026-10-18)

| Library | Version | Note |
|---|---|---|
| Kotlin | 2.4.20 | SKIE 0.10.15 release notes explicitly add 2.4.20 support |
| SKIE | 0.10.15 | |
| SQLDelight | 2.4.1 | |
| Koin | 4.2.2 | |
| kotlinx-datetime | 0.8.0 | |
| kotlinx-coroutines | 1.11.0 | |
| AndroidX Lifecycle (ViewModel, KMP) | 2.11.0 | |
| Turbine | 1.2.1 | |
| Android Gradle Plugin | 9.4.1 | Confirm compatibility with Kotlin 2.4.20 during scaffolding and fall back to the newest compatible AGP if needed |

Compose BOM / Compose compiler: the Compose compiler ships with Kotlin 2.4.20 (Kotlin Compose Gradle plugin), and the newest stable Compose BOM is chosen at scaffold time.

### Environment prerequisite
The machine has **Xcode 15.4**, which is too old for Kotlin 2.4 / SKIE. **The user updates Xcode to the current release before the first iOS build.** macOS 15.7 supports it. JDK, Android SDK, and Android Studio are already installed.

## 4. Architecture

```
habit-tracker/
├── shared/
│   └── src/commonMain/kotlin/<pkg>/
│       ├── domain/
│       │   ├── model/        Habit, Category, Schedule, Weekday set, DayStatus, StreakInfo, HabitStats
│       │   ├── repository/   HabitRepository, CategoryRepository, CompletionRepository (interfaces)
│       │   ├── usecase/      one class per use case (see §6)
│       │   └── time/         AppClock (today(): LocalDate, observe day changes)
│       ├── data/
│       │   ├── db/           SQLDelight .sq files, migrations (.sqm)
│       │   ├── repository/   implementations + row↔model mappers
│       │   └── seed/         default categories
│       ├── presentation/
│       │   └── <screen>/     XViewModel, XUiState, XEvent, XEffect
│       └── di/               Koin modules; expect/actual SqlDriver factory
│   ├── src/androidMain/      AndroidSqliteDriver, Android day-change source
│   ├── src/iosMain/          NativeSqliteDriver, iOS day-change source, KoinHelper for Swift
│   └── src/commonTest/ ...
├── androidApp/               Compose UI, Navigation Compose, icon/color mapping
└── iosApp/                   SwiftUI, Xcode project embedding the shared framework, icon/color mapping
```

### Dependency rule
`presentation → domain ← data`. `domain` declares the repository interfaces and `data` implements them. ViewModels depend only on use cases. `di` is the only package that knows all the layers.

**Enforced by an architecture test** in `commonTest` (a source-scanning test over `shared/src/commonMain`):
- files under `domain/` import only `kotlin.*`, `kotlinx.coroutines.*`, `kotlinx.datetime.*`, and `domain` itself;
- files under `presentation/` never import `data`.

### Data flow (MVVM, unidirectional)
1. The UI sends an `Event` to the ViewModel through `onEvent`.
2. The ViewModel calls a use case in `viewModelScope`.
3. The use case validates the input and writes through a repository (in a SQLDelight transaction).
4. SQLDelight query `Flow`s emit, and the use cases map them to domain models.
5. The ViewModel `combine`s the flows into `XUiState` and exposes it as a `StateFlow` (`stateIn(WhileSubscribed(5s))`).
6. The UI renders the state. One-off events (dismiss sheet, error message) arrive on `effects: Flow<XEffect>` (a `Channel`).

No screen refreshes manually. Toggling a habit on Today updates the Calendar and the Habit detail screens automatically.

### iOS bridge
- `KoinHelper` (iosMain) starts Koin and exposes factory functions for each ViewModel.
- A small generic Swift wrapper (`@Observable final class ViewModelHost<VM, State>`) holds a ViewModel, iterates `uiState` with SKIE's `AsyncSequence` in a `Task`, publishes `state`, and calls `vm.clear()` on deinit.
- Sealed `UiState` and `Effect` types become exhaustive Swift enums through SKIE.

## 5. Data model

All dates are **local calendar dates** (`LocalDate`) stored as ISO `TEXT` (`"2026-10-18"`), which sorts correctly. No timestamps are used for day logic.

```sql
CREATE TABLE Category (
  id         TEXT NOT NULL PRIMARY KEY,   -- UUID
  name       TEXT NOT NULL,
  colorKey   TEXT NOT NULL,               -- palette key, e.g. "green"
  sortOrder  INTEGER NOT NULL
);

CREATE TABLE Habit (
  id          TEXT NOT NULL PRIMARY KEY,
  name        TEXT NOT NULL,
  iconKey     TEXT NOT NULL,              -- e.g. "run"
  categoryId  TEXT NOT NULL REFERENCES Category(id),
  createdOn   TEXT NOT NULL               -- LocalDate
);

CREATE TABLE HabitArchivePeriod (
  habitId   TEXT NOT NULL REFERENCES Habit(id) ON DELETE CASCADE,
  fromDate  TEXT NOT NULL,                -- first day not due (inclusive)
  toDate    TEXT,                         -- first day due again (exclusive); NULL = currently archived
  PRIMARY KEY (habitId, fromDate)
);

CREATE TABLE HabitSchedule (
  habitId        TEXT NOT NULL REFERENCES Habit(id) ON DELETE CASCADE,
  effectiveFrom  TEXT NOT NULL,           -- LocalDate
  daysMask       INTEGER NOT NULL,        -- bit0=Mon … bit6=Sun; 127 = every day; never 0
  PRIMARY KEY (habitId, effectiveFrom)
);

CREATE TABLE Completion (
  habitId  TEXT NOT NULL REFERENCES Habit(id) ON DELETE CASCADE,
  date     TEXT NOT NULL,                 -- LocalDate
  PRIMARY KEY (habitId, date)
);
```

Foreign keys are enabled on both drivers (`PRAGMA foreign_keys = ON`).

### Rules
- **Completion presence = done.** Toggling inserts or deletes the row.
- **Schedule in effect on day D** = the `HabitSchedule` row with the greatest `effectiveFrom ≤ D`. Creating a habit inserts a schedule with `effectiveFrom = createdOn`.
- **Editing a schedule** inserts a row with `effectiveFrom = today`, or replaces it if one already exists for today. Past days keep their old schedule. Editing the name, icon, or category changes nothing historical (those fields aren't versioned).
- **Archive** inserts `(habitId, today, NULL)`. If an open period already exists, this is a no-op. Days before today keep their history and status.
- **Unarchive** sets `toDate = today` on the open period. If `fromDate == today`, the row is deleted instead. The days in the gap stay **not due**, so they never turn into misses.
- **Archived habit** = one with an open period (`toDate IS NULL`).
- The habit is **not due on D** if any period has `fromDate ≤ D < coalesce(toDate, +∞)`.
- **Delete** removes the habit and cascades its schedules and completions. The UI asks for confirmation.
- **Categories:** deletion is allowed only when no habits (active or archived) reference the category. Otherwise the use case returns `CategoryNotEmpty`.
- **Seed:** on first launch (empty `Category` table), insert Health (green), Fitness (orange), Learning (blue), Mindfulness (purple), Productivity (teal).

### Palette & icons
- `colorKey`: about 10 curated keys (`red, orange, amber, green, teal, blue, indigo, purple, pink, gray`). Each platform maps every key to a light and a dark variant.
- `iconKey`: a curated set of 30–40 keys (`run, water, book, meditate, sleep, gym, walk, code, pray, journal, pill, apple, …`). iOS maps them to SF Symbols and Android to Material Symbols. The shared module stores and validates only the key list (`IconKeys.all`).
- An unknown key (e.g. from a future version) falls back to a generic icon or `gray` on render.

## 6. Domain logic

### Model
- `Habit(id, name, iconKey, category: Category, createdOn, schedules: List<ScheduleVersion>, archivePeriods: List<ArchivePeriod>)`
- `ScheduleVersion(effectiveFrom: LocalDate, days: Set<DayOfWeek>)`
- `fun Habit.isDueOn(date: LocalDate): Boolean`, which is true when:
  1. `date ≥ createdOn`, and
  2. no archive period covers `date`, and
  3. `date.dayOfWeek ∈ scheduleOn(date).days`.

### Day status
For day D with `due = habits due on D` and `done = due ∩ completed on D`:

| Status | Condition |
|---|---|
| `Future` | D > today |
| `NoHabits` | `due` is empty |
| `Perfect` | `done == due` |
| `InProgress` | D == today and not Perfect |
| `Partial(done, due)` | 0 < done < due, D < today |
| `Missed` | done == 0, D < today |

Completions on days when a habit isn't due are kept in storage and ignored in every calculation.

### Overall streak
`current`: iterate D from today backwards to the earliest `createdOn`:
- `Perfect` → +1
- `InProgress` (today only) and `NoHabits` → skip
- `Partial` / `Missed` → stop

`best`: the longest run over all history using the same rules (NoHabits days neither break nor extend a run). If there are no habits, both are 0.

### Per-habit stats
For a single habit, considering only days it is due:
- `current` streak: walk back from today. Done → +1. Today not done → skip. Not done (past) → stop.
- `best` streak: the longest run of consecutive due days that are done.
- `completionRate` = done-due-days ÷ due-days for days ≤ today (today counts only if done). If there are no due days, it's `null` and shown as "—".

### Edit window
`ToggleCompletion(habitId, date)`:
- `date ∉ {today, today − 1}` → `Failure(EditWindowClosed)`
- habit not due on `date` → `Failure(NotDue)`
- otherwise insert or delete the completion → `Success(isDoneNow)`

The rule lives in the use case, so the UI cannot bypass it.

### Validation (`CreateHabit` / `UpdateHabit`)
- Name trimmed, 1–40 characters → otherwise `InvalidName`.
- At least one weekday → otherwise `NoDaysSelected`.
- `iconKey ∈ IconKeys.all`, and the category exists.

### Use cases
`ObserveTodayDashboard`, `ObserveMonthCalendar(yearMonth)`, `ObserveDayDetail(date)`, `ObserveHabits` (active + archived), `ObserveHabitDetail(id)`, `CalculateOverallStreak`, `CalculateHabitStats`, `ToggleCompletion`, `CreateHabit`, `UpdateHabit`, `ArchiveHabit`, `UnarchiveHabit`, `DeleteHabit`, `ObserveCategories`, `CreateCategory`, `UpdateCategory`, `DeleteCategory`, `SeedDefaultCategoriesIfEmpty`.

Expected failures use a sealed `DomainError` inside a `Result`-like `Outcome<T>` type. Unexpected exceptions (DB I/O) propagate to the ViewModel, which emits an error effect.

### Time
`AppClock` provides `today(): LocalDate` (from `Clock` + `TimeZone.currentSystemDefault()`) and `todayFlow: Flow<LocalDate>`, which re-emits when the date changes:
- iOS: `NSCalendarDayChangedNotification` / `UIApplication.significantTimeChangeNotification`, plus app foreground.
- Android: `ACTION_DATE_CHANGED` / `ACTION_TIMEZONE_CHANGED`, plus `ON_RESUME`.

The ViewModels build their state from `todayFlow`, so an app left open past midnight rolls over correctly. Tests use a `FakeAppClock`.

**Performance:** queries load only the range needed (a month, or history from the earliest `createdOn` for streaks). Streak walks are O(days × habits) in memory. That's trivial for years of data, so v1 has no caching.

## 7. Screens

Navigation: a bottom bar with three tabs (**Today**, **Calendar**, **Habits**). Create and edit is a modal sheet. Habit detail is pushed onto the current tab's stack.

Each screen has `XViewModel` with `uiState: StateFlow<XUiState>`, `onEvent(XEvent)`, and `effects: Flow<XEffect>`.

### 7.1 Today — `DashboardViewModel`
- Header: date, progress ring (`done / due`), streak card (🔥 current, best).
- Due habits grouped by category (header in the category color). Each row shows the icon, name, per-habit 🔥 current streak, and a check button.
- The first time today becomes Perfect while the screen is visible, it emits `Effect.Celebrate` once per day.
- Empty states: no habits at all (CTA "Create your first habit"), or nothing due today.
- Events: `ToggleHabit(id)`, `AddHabitClicked`, `HabitClicked(id)`.

### 7.2 Calendar — `CalendarViewModel`
- Month grid (weeks start on Monday) with ‹ ›, initialized to the current month. Markers:
  - `Perfect` 🔥
  - `Partial` ring showing the fraction
  - `Missed` muted red dot
  - `InProgress` today outline plus ring
  - `NoHabits` and `Future` blank (Future dimmed)
- Month summary: Perfect days, the completion rate for the month (done ÷ due over days ≤ today), and the longest Perfect run inside the month.
- Selecting a day shows a panel listing that day's due habits with done or missed status. Toggles are enabled only when the date is today or yesterday.
- Events: `PrevMonth`, `NextMonth`, `SelectDay(date)`, `ToggleHabit(id, date)`.

### 7.3 Habits — `HabitListViewModel`
- Active habits grouped by category, each showing a schedule summary ("Every day" / "Mon · Wed · Fri").
- A collapsible "Archived" section.
- An entry point to **Manage categories** (`CategoryListViewModel`: create, rename, recolor, delete-if-empty).
- Events: `HabitClicked(id)`, `AddHabitClicked`, `ManageCategoriesClicked`.

### 7.4 Habit detail — `HabitDetailViewModel`
- Icon and name in the category color, current and best streak, completion %.
- Mini month calendar for this habit with done, missed, and not-due states, plus ‹ ›.
- Actions: Edit (opens the form), Archive/Unarchive, Delete (confirmation: "This erases all history for this habit").

### 7.5 Habit form — `HabitFormViewModel(habitId: String?)`
- Fields: name, category picker, icon grid, weekday chips (M T W T F S S) with an "Every day" toggle.
- Live validation in shared code, with Save enabled only when the form is valid.
- In edit mode, if the days changed, the form shows: "Schedule changes apply from today. Past days keep their old schedule."
- Effects: `Dismiss`, `ShowError(DomainError)`.

### Platform rendering
- **iOS:** `TabView`, `NavigationStack`, `.sheet`, SF Symbols, `sensoryFeedback` on toggle, Dynamic Type supported.
- **Android:** `Scaffold` + `NavigationBar`, Navigation Compose, `ModalBottomSheet`, Material Symbols, haptic on toggle.
- Both support light and dark mode using the palette mapping in §5.

### Error handling
- Domain failures (`EditWindowClosed`, `NotDue`, `InvalidName`, `NoDaysSelected`, `CategoryNotEmpty`) are shown inline or as a short message.
- Unexpected DB errors emit `Effect.ShowError`, shown as a snackbar or alert. Writes are transactional, so the state never ends up half-applied.

## 8. Testing

Development is TDD, with most of the effort on shared code.

| Layer | What | How |
|---|---|---|
| Domain | `isDueOn` with schedule history and archive periods; every `DayStatus`; overall and per-habit streaks; completion rate; edit window; validation | `commonTest`, `FakeAppClock`, plain models |
| Data | schedule-at-date query, archive-period queries, cascades, FK enforcement, seed runs once, mappers | SQLDelight in-memory driver (JVM test) |
| Presentation | `UiState` sequences and effects per event | fake repositories, `kotlinx-coroutines-test`, Turbine |
| Architecture | domain and presentation import rules (§4) | source-scanning test |
| Android UI | smoke: launch, toggle updates the progress ring | Compose UI test |
| iOS UI | smoke: launch, toggle updates the progress ring | XCUITest |

Required domain edge cases: NoHabits gaps inside a streak, archiving mid-streak, unarchive (the gap stays not-due), a schedule change mid-streak, a habit created today, today in progress vs. complete, month and year boundaries, Feb 29, completions on non-due days ignored.

`./gradlew :shared:allTests` runs the shared tests on the JVM/Android and iOS simulator targets.

## 9. Risks

- **Toolchain:** Kotlin 2.4.20 + SKIE 0.10.15 + AGP 9.4.1 must be verified together at scaffold time. If they conflict, use the newest compatible combination and record it in `libs.versions.toml`.
- **Swift/Kotlin lifecycle:** the ViewModel must be cleared when the SwiftUI view goes away, or it leaks. `ViewModelHost` covers this, and its lifecycle is checked manually in the simulator.
- **Timezone travel:** dates are local, so moving east or west can make "today" jump. Accepted, because this is how users think about days.
