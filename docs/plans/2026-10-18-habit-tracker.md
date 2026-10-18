# Habit Tracker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an offline-first habit tracker. All logic and ViewModels are shared in Kotlin Multiplatform, with native SwiftUI (iOS) and Jetpack Compose (Android) UIs.

**Architecture:** A single `shared` KMP module layered as `domain` (pure Kotlin), `data` (SQLDelight), `presentation` (shared ViewModels exposing `StateFlow<UiState>`), and `di` (Koin). `androidApp` and `iosApp` only render state and forward events. Swift consumes Kotlin Flows and sealed types through SKIE.

**Tech Stack:** Kotlin 2.4.20, SKIE 0.10.15, SQLDelight 2.4.1, Koin 4.2.2, kotlinx-datetime 0.8.0, kotlinx-coroutines 1.11.0, AndroidX Lifecycle ViewModel 2.11.0 (KMP), Turbine 1.2.1, AGP 9.4.1, Jetpack Compose + Material 3, SwiftUI (iOS 17), XcodeGen.

**Spec:** `docs/design/2026-10-18-habit-tracker-design.md`. Read it before starting any task. §-references below point into it.

## Global Constraints

- Base package: `app.habittracker` (shared). Android applicationId `app.habittracker.android`, iOS bundle id `app.habittracker.ios`.
- Targets: `androidTarget` via the `com.android.kotlin.multiplatform.library` plugin, `iosArm64`, `iosSimulatorArm64`. iOS deployment target 17.0. Android minSdk 26, compile/target SDK 36.
- AGP 9 has built-in Kotlin: do **not** apply `org.jetbrains.kotlin.android` to `androidApp`.
- All versions live in `gradle/libs.versions.toml` and match the Tech Stack line. If a pinned combination fails to build, use the newest working one and note why in a TOML comment.
- Files under `shared/.../domain/` import only `kotlin.*`, `kotlinx.coroutines.*`, `kotlinx.datetime.*`, and `app.habittracker.domain.*`. Files under `presentation/` never import `app.habittracker.data.*`.
- All day logic uses `kotlinx.datetime.LocalDate`, stored as ISO `TEXT`. "Now" comes only from `kotlin.time.Clock` via `AppClock`. Never call `Clock.System` outside `SystemAppClock`.
- Weeks start on Monday. `daysMask`: bit0 = Monday … bit6 = Sunday. 127 = every day. Never 0.
- Habit and category names: trimmed, 1–40 characters.
- Expected failures return `Outcome.Failure(DomainError)`. They never throw.
- Unit test commands: `./gradlew :shared:testAndroidHostTest` (JVM host) and `./gradlew :shared:iosSimulatorArm64Test`. `./gradlew :shared:allTests` runs both.
- Every commit message ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **App left open past midnight:** the Today screen must switch to the new date (new due list, progress reset, streak recalculated) without a relaunch. Test in Task 12.
2. **Double-tapping a check button:** two fast toggles must leave the habit not done, with no crash from a primary-key conflict. Test in Task 7 (`toggle` is atomic) and Task 9.
3. **Toggling yesterday after changing the schedule today:** yesterday is judged by the *old* schedule, so a habit due yesterday under the old days can still be toggled. Test in Task 9.
4. **Deleting a category that only has archived habits:** must be refused with `CategoryNotEmpty`, because archived history still references it. Test in Task 9.
5. **Unknown icon or color key in the DB** (renamed or removed key): renders a fallback instead of crashing. `IconKeys.resolve`/`ColorKeys.resolve` are tested in Task 6, and the platforms render only resolved keys (Tasks 17, 19).

---

## File structure

```
habit-tracker/
├── settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew
├── shared/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/app/habittracker/
│       │   ├── domain/model/        Category.kt, Habit.kt (+ScheduleVersion, ArchivePeriod), Completion.kt,
│       │   │                        DayStatus.kt, Stats.kt (StreakInfo, HabitStats), Outcome.kt (+DomainError),
│       │   │                        IconKeys.kt, ColorKeys.kt, Grouping.kt (CategoryGroup)
│       │   ├── domain/logic/        DayStatusCalculator.kt, OverallStreak.kt, HabitStatsCalculator.kt,
│       │   │                        EditWindow.kt, Validation.kt
│       │   ├── domain/repository/   HabitRepository.kt, CategoryRepository.kt, CompletionRepository.kt
│       │   ├── domain/time/         AppClock.kt
│       │   ├── domain/usecase/      one file per use case (Tasks 9–10)
│       │   ├── data/db/             DriverFactory.kt (expect), DaysMask.kt
│       │   ├── data/repository/     SqlHabitRepository.kt, SqlCategoryRepository.kt, SqlCompletionRepository.kt
│       │   ├── data/time/           SystemAppClock.kt, DayChangeSource.kt (interface)
│       │   ├── presentation/<screen>/  <Screen>ViewModel.kt, <Screen>Contract.kt (UiState/Event/Effect)
│       │   └── di/                  Modules.kt, InitKoin.kt
│       ├── commonMain/sqldelight/app/habittracker/data/db/  Category.sq, Habit.sq, HabitSchedule.sq,
│       │                                                     HabitArchivePeriod.sq, Completion.sq
│       ├── androidMain/…            DriverFactory.android.kt, AndroidDayChangeSource.kt, PlatformModule.android.kt
│       ├── iosMain/…                DriverFactory.ios.kt, IosDayChangeSource.kt, PlatformModule.ios.kt, KoinHelper.kt
│       ├── commonTest/…             domain + use case + ViewModel tests, fakes/ (Fake*Repository, FakeAppClock), TestData.kt
│       └── androidHostTest/…        ArchitectureTest.kt, data/ (SQLDelight tests on JdbcSqliteDriver)
├── androidApp/                      src/main/kotlin/app/habittracker/android/{HabitApp.kt, MainActivity.kt, ui/…}
└── iosApp/                          project.yml (XcodeGen), HabitTracker/{App, Bridge, Theme, Screens}/, HabitTrackerUITests/
```

---

### Task 1: Project scaffold, toolchain gate, architecture test

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, Gradle wrapper, `.gitignore`, `shared/build.gradle.kts`, `shared/src/commonMain/kotlin/app/habittracker/Placeholder.kt` (deleted in Task 2), `androidApp/build.gradle.kts`, `androidApp/src/main/AndroidManifest.xml`, `androidApp/src/main/kotlin/app/habittracker/android/MainActivity.kt` (shows a "Habit Tracker" text)
- Test: `shared/src/androidHostTest/kotlin/app/habittracker/ArchitectureTest.kt`, `shared/src/commonTest/kotlin/app/habittracker/SmokeTest.kt`

**Interfaces:**
- Produces: a buildable `:shared` (KMP: android + iosArm64 + iosSimulatorArm64, framework base name `Shared`, static, SKIE applied, SQLDelight plugin configured with database `HabitDatabase` in package `app.habittracker.data.db`, `generateAsync = false`, `verifyMigrations = true`), and `:androidApp` depending on it.

- [ ] **Step 1: Toolchain gate.** Run `xcodebuild -version`. If it's below Xcode 26, STOP and ask the user to update Xcode (spec §3 prerequisite). Run `brew install gradle xcodegen` if either is missing.
- [ ] **Step 2: Generate the scaffold.** Create the files listed above. Run `gradle wrapper` with the Gradle version AGP 9.4.1 requires. Add the dependencies to `commonMain`: coroutines-core, kotlinx-datetime, sqldelight runtime + coroutines-extensions, koin-core, koin-core-viewmodel, lifecycle-viewmodel. Add to `commonTest`: kotlin-test, coroutines-test, turbine. Add to `androidHostTest`: sqldelight `sqlite-driver`. Add the android-driver to `androidMain` and the native-driver to `iosMain`. `.gitignore`: `build/`, `.gradle/`, `local.properties`, `*.xcodeproj` (generated by XcodeGen), `xcuserdata/`, `.idea/`.
- [ ] **Step 3: Write the tests.**

```kotlin
// SmokeTest.kt (commonTest)
class SmokeTest { @Test fun runs() = assertEquals(2, 1 + 1) }

// ArchitectureTest.kt (androidHostTest) — scans shared/src/commonMain/kotlin/app/habittracker
class ArchitectureTest {
    @Test fun domainImportsOnlyAllowedPackages()      // every `import` line in domain/** starts with one of
        // "kotlin.", "kotlinx.coroutines.", "kotlinx.datetime.", "app.habittracker.domain."; failure message lists file:line
    @Test fun presentationDoesNotImportData()         // no import in presentation/** starts with "app.habittracker.data."
    @Test fun scannerFindsSourceRoot()                 // the root directory exists (guards against a wrong path silently passing)
}
```
Resolve the root from `File(System.getProperty("user.dir"))` (Gradle runs host tests in `shared/`). If `domain/` or `presentation/` doesn't exist yet, those two tests pass vacuously, but `scannerFindsSourceRoot` must still pass.

- [ ] **Step 4: Verify.** Run `./gradlew :shared:allTests :androidApp:assembleDebug`. Expected: BUILD SUCCESSFUL, with SmokeTest passing on the host and the iOS simulator. If the version pins conflict, apply the fallback rule from Global Constraints.
- [ ] **Step 5: Commit.** `git add -A && git commit -m "chore: scaffold KMP project with architecture test"`

---

### Task 2: Core domain model and `isDueOn`

**Files:**
- Create: `domain/model/Category.kt`, `domain/model/Habit.kt`, `domain/model/Completion.kt`
- Delete: `Placeholder.kt`
- Test: `commonTest/.../domain/model/HabitDueTest.kt`, `commonTest/.../TestData.kt`

**Interfaces:**
- Produces:
```kotlin
data class Category(val id: String, val name: String, val colorKey: String, val sortOrder: Int)
data class ScheduleVersion(val effectiveFrom: LocalDate, val days: Set<DayOfWeek>)
data class ArchivePeriod(val from: LocalDate, val toExclusive: LocalDate?)   // null = still archived
data class Habit(
    val id: String, val name: String, val iconKey: String, val category: Category,
    val createdOn: LocalDate, val schedules: List<ScheduleVersion>, val archivePeriods: List<ArchivePeriod>,
) {
    val isArchived: Boolean                       // any period with toExclusive == null
    val currentSchedule: ScheduleVersion          // latest effectiveFrom
    fun scheduleOn(date: LocalDate): ScheduleVersion?   // greatest effectiveFrom <= date, else null
    fun isArchivedOn(date: LocalDate): Boolean    // from <= date < (toExclusive ?: +∞)
    fun isDueOn(date: LocalDate): Boolean         // date >= createdOn && !isArchivedOn(date) && date.dayOfWeek in scheduleOn(date).days
}
data class Completion(val habitId: String, val date: LocalDate)
typealias CompletionIndex = Map<LocalDate, Set<String>>   // date -> habitIds done
fun List<Completion>.toIndex(): CompletionIndex
```
- `TestData.kt` (commonTest) helpers used by every later test: `fun date(s: String): LocalDate`, `val HEALTH: Category`, `fun habit(id: String = "h1", created: String = "2026-01-05", days: Set<DayOfWeek> = DayOfWeek.entries.toSet(), category: Category = HEALTH, schedules: List<ScheduleVersion>? = null, archived: List<ArchivePeriod> = emptyList(), name: String = id, iconKey: String = "run"): Habit`, `val MON_WED_FRI: Set<DayOfWeek>`. (2026-01-05 is a Monday.)

- [ ] **Step 1: Write the failing tests** in `HabitDueTest`:
  - `notDueBeforeCreatedOn`: `habit(created="2026-01-05").isDueOn(date("2026-01-04")) == false`, and `true` on `2026-01-05`.
  - `weekdayScheduleRespected`: with MON_WED_FRI, due on 2026-01-05 (Mon) and 2026-01-07 (Wed), not due on 2026-01-06 (Tue).
  - `scheduleHistoryUsesVersionInEffect`: versions `[2026-01-05: MON_WED_FRI, 2026-01-12: {TUESDAY}]` → Wed 2026-01-07 due, Wed 2026-01-14 not due, Tue 2026-01-13 due.
  - `archivedPeriodIsNotDue`: period `(2026-01-10, 2026-01-15)` → not due on 01-10 or 01-14, due on 01-15 and 01-09.
  - `openArchivePeriodIsNotDueForever`: period `(2026-01-10, null)` → not due on 2027-01-01, and `isArchived == true`.
  - `completionIndexGroupsByDate`: two completions on the same date → one key with both ids.
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "*HabitDueTest*"`. Expected: FAIL (unresolved references).
- [ ] **Step 3: Implement** the Interfaces block in the listed files.
- [ ] **Step 4: Run** the same command plus `ArchitectureTest`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(domain): habit model with schedule history and archive periods`

---

### Task 3: Day status

**Files:**
- Create: `domain/model/DayStatus.kt`, `domain/logic/DayStatusCalculator.kt`
- Test: `commonTest/.../domain/logic/DayStatusTest.kt`

**Interfaces:**
- Consumes: `Habit.isDueOn`, `CompletionIndex` (Task 2)
- Produces:
```kotlin
sealed interface DayStatus {
    data object Future : DayStatus
    data object NoHabits : DayStatus
    data class Perfect(val due: Int) : DayStatus
    data class InProgress(val done: Int, val due: Int) : DayStatus   // today only
    data class Partial(val done: Int, val due: Int) : DayStatus
    data class Missed(val due: Int) : DayStatus
}
fun dueHabits(habits: List<Habit>, date: LocalDate): List<Habit>
fun dayStatus(date: LocalDate, today: LocalDate, habits: List<Habit>, completions: CompletionIndex): DayStatus
```
Precedence, checked in order (spec §6): Future → NoHabits → Perfect → InProgress → Partial → Missed. `done` counts only habits that are due **and** completed on that date.

- [ ] **Step 1: Write the failing tests:** `futureDay`, `noHabitsDueIsNoHabits` (including for today), `allDoneIsPerfect` (including today → `Perfect`, not InProgress), `todayIncompleteIsInProgress(done=1,due=2)`, `pastSomeDoneIsPartial(1,2)`, `pastNoneDoneIsMissed(2)`, `completionOnNonDueDayIgnored` (habit due Mon only, completion on Tue, other habit due Tue undone → `Missed(1)`).
- [ ] **Step 2: Run** `--tests "*DayStatusTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(domain): day status calculation`

---

### Task 4: Overall streak

**Files:**
- Create: `domain/model/Stats.kt` (StreakInfo here, HabitStats in Task 5), `domain/logic/OverallStreak.kt`
- Test: `commonTest/.../domain/logic/OverallStreakTest.kt`

**Interfaces:**
- Consumes: `dayStatus` (Task 3)
- Produces:
```kotlin
data class StreakInfo(val current: Int, val best: Int)
fun overallStreak(habits: List<Habit>, completions: CompletionIndex, today: LocalDate): StreakInfo
fun longestPerfectRun(statuses: List<DayStatus>): Int   // reused by the calendar month summary (Task 10)
```
Algorithm (spec §6): the range runs from `min(createdOn)` to `today`. Each `Perfect` adds +1. `NoHabits` and `InProgress` are skipped (they neither break nor extend a run). `Partial` and `Missed` reset the run. `current` walks backwards from today and stops at the first Partial/Missed. `best` = `longestPerfectRun` over the full range. With no habits, the result is `StreakInfo(0, 0)`.

- [ ] **Step 1: Write the failing tests:**
  - `noHabitsIsZero`
  - `threePerfectDaysThenTodayInProgress` → current 3
  - `todayPerfectCounts` → current 4
  - `noHabitsGapDoesNotBreak` (Mon/Wed/Fri habit, all done, today = Fri) → current 3
  - `missedDayStops` → current counts only after the miss, and best is the earlier longer run
  - `habitCreatedTodayDoesNotBreakPastPerfectDays` (old habit perfect for 5 days, new habit created today and not done) → current 5
  - `archivingMidStreakKeepsStreak` (archived habit stops being due, remaining habit all done) → run unbroken
  - `scheduleChangeMidStreak` (judged per the version in effect)
  - `acrossYearBoundaryAndLeapDay` (2027-12-30 → 2028-01-02 all perfect → 4; 2028-02-28 → 2028-03-01 → 3)
- [ ] **Step 2: Run** `--tests "*OverallStreakTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(domain): overall perfect-day streak`

---

### Task 5: Per-habit stats

**Files:**
- Modify: `domain/model/Stats.kt`
- Create: `domain/logic/HabitStatsCalculator.kt`
- Test: `commonTest/.../domain/logic/HabitStatsTest.kt`

**Interfaces:**
- Produces:
```kotlin
data class HabitStats(val currentStreak: Int, val bestStreak: Int, val completionRate: Double?)  // rate in 0.0..1.0
fun habitStats(habit: Habit, doneDates: Set<LocalDate>, today: LocalDate): HabitStats
```
Only days with `habit.isDueOn(d)` and `createdOn ≤ d ≤ today` count. Today undone is skipped (no break, no count). A past due day that wasn't done breaks the streak. `completionRate = doneDueDays / countedDueDays`, where today counts only if done. If nothing is counted, the rate is `null`.

- [ ] **Step 1: Write the failing tests:**
  - `skipsNonDueDays` (Mon/Wed/Fri done 4× in a row across a weekend → 4)
  - `todayUndoneDoesNotBreak`
  - `pastMissBreaksCurrentButBestKeepsLongest` (best 5, current 2)
  - `completionRateExcludesTodayUnlessDone` (3 of 4 past days done, today undone → 0.75; today done → 0.8)
  - `createdTodayUndoneRateIsNull`
  - `archivedGapIsSkipped`
- [ ] **Step 2: Run** `--tests "*HabitStatsTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(domain): per-habit streak and completion rate`

---

### Task 6: Domain contracts: Outcome, validation, edit window, keys, clock, repositories

**Files:**
- Create: `domain/model/Outcome.kt`, `domain/model/IconKeys.kt`, `domain/model/ColorKeys.kt`, `domain/model/Grouping.kt`, `domain/logic/Validation.kt`, `domain/logic/EditWindow.kt`, `domain/time/AppClock.kt`, `domain/repository/{HabitRepository,CategoryRepository,CompletionRepository}.kt`
- Create (commonTest): `fakes/FakeAppClock.kt`, `fakes/FakeHabitRepository.kt`, `fakes/FakeCategoryRepository.kt`, `fakes/FakeCompletionRepository.kt`
- Test: `commonTest/.../domain/logic/ValidationTest.kt`, `commonTest/.../domain/model/KeysTest.kt`

**Interfaces:**
- Produces:
```kotlin
sealed interface Outcome<out T> { data class Success<T>(val value: T) : Outcome<T>; data class Failure(val error: DomainError) : Outcome<Nothing> }
sealed interface DomainError {
    data object EditWindowClosed; data object NotDue; data object InvalidName; data object NoDaysSelected
    data object UnknownIcon; data object UnknownColor; data object CategoryNotFound; data object HabitNotFound; data object CategoryNotEmpty
}   // each `: DomainError`

object IconKeys { val all: List<String>; const val FALLBACK = "checklist"; fun resolve(key: String): String }  // unknown -> FALLBACK
object ColorKeys { val all: List<String>; const val FALLBACK = "gray"; fun resolve(key: String): String }
// IconKeys.all (exact, in order): run, walk, gym, bike, swim, yoga, meditate, water, nutrition, food, sleep, pill,
//   heart, book, study, code, language, journal, music, art, pray, sun, clean, money, no_phone, call, people,
//   plant, pet, checklist, timer, target, star, smile, no_smoking, home
// ColorKeys.all (exact, in order): red, orange, amber, green, teal, blue, indigo, purple, pink, gray

data class CategoryGroup<T>(val category: Category, val items: List<T>)
fun <T> List<T>.groupByCategory(category: (T) -> Category): List<CategoryGroup<T>>  // groups ordered by sortOrder, then name; item order preserved

fun validateName(raw: String): Outcome<String>          // trimmed; 1..40 chars else InvalidName
fun validateDays(days: Set<DayOfWeek>): Outcome<Set<DayOfWeek>>   // empty -> NoDaysSelected
fun isEditable(date: LocalDate, today: LocalDate): Boolean        // date == today || date == today - 1 day

interface AppClock { fun today(): LocalDate; val todayFlow: Flow<LocalDate> }

interface HabitRepository {
    fun observeAll(): Flow<List<Habit>>                  // active + archived, fully hydrated
    fun observe(id: String): Flow<Habit?>
    suspend fun get(id: String): Habit?
    suspend fun create(name: String, iconKey: String, categoryId: String, createdOn: LocalDate, days: Set<DayOfWeek>): String  // returns id; inserts schedule effectiveFrom = createdOn
    suspend fun updateDetails(id: String, name: String, iconKey: String, categoryId: String)
    suspend fun upsertSchedule(id: String, effectiveFrom: LocalDate, days: Set<DayOfWeek>)
    suspend fun openArchivePeriod(id: String, from: LocalDate)
    suspend fun closeArchivePeriod(id: String, from: LocalDate, toExclusive: LocalDate)
    suspend fun deleteArchivePeriod(id: String, from: LocalDate)
    suspend fun delete(id: String)
}
interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>               // ordered by sortOrder
    suspend fun get(id: String): Category?
    suspend fun create(name: String, colorKey: String): String   // sortOrder = max + 1
    suspend fun update(id: String, name: String, colorKey: String)
    suspend fun delete(id: String)
    suspend fun habitCount(id: String): Long             // active + archived
    suspend fun isEmpty(): Boolean
}
interface CompletionRepository {
    fun observeAll(): Flow<List<Completion>>
    fun observeForHabit(habitId: String): Flow<Set<LocalDate>>
    suspend fun toggle(habitId: String, date: LocalDate): Boolean   // atomic; returns isDone after toggle
}
```
- Fakes: in-memory implementations backed by `MutableStateFlow`. `FakeAppClock(start: LocalDate)` has `fun setToday(d: LocalDate)`, which updates both `today()` and `todayFlow`.

- [ ] **Step 1: Write the failing tests:**
  - `ValidationTest`: `"  Run  "` → Success("Run"); `"   "` → InvalidName; 40 chars ok; 41 chars → InvalidName; empty days → NoDaysSelected.
  - `isEditable`: today ✓, yesterday ✓, 2 days ago ✗, tomorrow ✗, Jan 1 vs Dec 31 ✓.
  - `KeysTest`: `IconKeys.resolve("rocket") == "checklist"`, `resolve("run") == "run"`, `ColorKeys.resolve("mauve") == "gray"`, `IconKeys.all.size == 36` with no duplicates, `ColorKeys.all.size == 10`.
- [ ] **Step 2: Run** `--tests "*ValidationTest*" --tests "*KeysTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** the contracts and fakes.
- [ ] **Step 4: Run** the tests plus `ArchitectureTest`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(domain): outcomes, validation, keys, clock and repository contracts`

---

### Task 7: SQLDelight schema, drivers, repository implementations

**Files:**
- Create: the five `.sq` files (DDL exactly as spec §5, with `PRIMARY KEY`s and `ON DELETE CASCADE` as written), `data/db/DriverFactory.kt` (`expect class DriverFactory { fun create(): SqlDriver }`), `androidMain/.../DriverFactory.android.kt`, `iosMain/.../DriverFactory.ios.kt`, `data/db/DaysMask.kt`, `data/repository/Sql{Habit,Category,Completion}Repository.kt`
- Test (androidHostTest): `data/DaysMaskTest.kt`, `data/SqlRepositoriesTest.kt`, `data/TestDriver.kt`

**Interfaces:**
- Consumes: repository interfaces (Task 6)
- Produces: `object DaysMask { fun encode(days: Set<DayOfWeek>): Int; fun decode(mask: Int): Set<DayOfWeek> }`; `class SqlHabitRepository(db: HabitDatabase, io: CoroutineDispatcher)`, and likewise for the others. `DriverFactory` (Android takes a `Context`). Both platforms enable foreign keys: Android via the `AndroidSqliteDriver.Callback.onOpen { db.setForeignKeyConstraintsEnabled(true) }`, and iOS via `DatabaseConfiguration.Extended(foreignKeyConstraints = true)`. DB file: `habits.db`.
- `TestDriver.kt`: `fun inMemoryDb(): HabitDatabase` using `JdbcSqliteDriver(IN_MEMORY, Properties().apply { put("foreign_keys", "true") })` + `HabitDatabase.Schema.create`.
- Hydration: `observeAll` combines three query flows (habits joined with category, all schedules, all archive periods) into `List<Habit>`, ordered by `createdOn`, then `name`. Queries use `asFlow().mapToList(io)`.
- `toggle` runs in a `transactionWithResult`: if the row exists, delete it and return false; otherwise insert it and return true.
- `upsertSchedule` uses `INSERT OR REPLACE`.

- [ ] **Step 1: Write the failing tests:**
  - `DaysMaskTest`: `encode(all) == 127`, `encode({MONDAY}) == 1`, `encode({SUNDAY}) == 64`, round-trip for MON_WED_FRI (= 21).
  - `SqlRepositoriesTest` (`runTest` + Turbine):
    - `createHabitHydratesScheduleAndCategory`
    - `upsertScheduleSameDayReplaces` (two upserts on the same date → 1 version)
    - `scheduleHistoryOrdered`
    - `archivePeriodsRoundTrip` (open, close, delete)
    - `toggleTwiceEndsNotDone` (true then false, no exception)
    - `concurrentTogglesAreConsistent` (two `async { toggle }` → final state not done, row count 0)
    - `deleteHabitCascades` (schedules, periods, and completions gone)
    - `categoryDeleteWithHabitFailsOnForeignKey` (raw delete throws, proving FKs are on)
    - `habitCountIncludesArchived`
    - `observeAllEmitsOnToggle`
- [ ] **Step 2: Run** `--tests "*DaysMaskTest*" --tests "*SqlRepositoriesTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** the schema, queries, drivers, and repositories.
- [ ] **Step 4: Run** the tests. Also run `./gradlew :shared:verifySqlDelightMigration :shared:compileKotlinIosSimulatorArm64` (with no `.sqm` files yet, this checks the schema exports). Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(data): SQLDelight schema and repositories`

---

### Task 8: Clock and DI wiring

**Files:**
- Create: `data/time/DayChangeSource.kt` (`interface DayChangeSource { val changes: Flow<Unit> }`), `data/time/SystemAppClock.kt`, `androidMain/.../AndroidDayChangeSource.kt`, `iosMain/.../IosDayChangeSource.kt`, `di/Modules.kt`, `di/InitKoin.kt`, `androidMain/.../PlatformModule.android.kt`, `iosMain/.../PlatformModule.ios.kt`
- Test: `commonTest/.../data/time/SystemAppClockTest.kt`, `androidHostTest/.../di/KoinGraphTest.kt`

**Interfaces:**
- Produces:
  - `class SystemAppClock(clock: Clock, timeZone: () -> TimeZone, changes: DayChangeSource) : AppClock`. `todayFlow = changes.changes.onStart { emit(Unit) }.map { today() }.distinctUntilChanged()`.
  - Android source: a `BroadcastReceiver` for `ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED`, and `ACTION_TIMEZONE_CHANGED`, merged with `ProcessLifecycleOwner` `ON_RESUME`, built with `callbackFlow`.
  - iOS source: `NSNotificationCenter` observers for `NSCalendarDayChangedNotification`, `UIApplicationSignificantTimeChangeNotification`, and `UIApplicationWillEnterForegroundNotification`.
  - `expect val platformModule: Module`, which provides `DriverFactory`, `DayChangeSource`, and `named("io")` dispatcher.
  - `val sharedModule: Module`, which provides `HabitDatabase`, the repositories (`single`), `AppClock`, all use cases (`factory`), and all ViewModels (`viewModelOf`/`viewModel { (id: String?) -> … }`). ViewModels are added to this module as Tasks 12–16 land.
  - `fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication`. (Task 9 adds the startup seed call.)

- [ ] **Step 1: Write the failing tests:** `SystemAppClockTest.todayFlowEmitsOnChangeOnlyWhenDateDiffers` (a controllable clock at 23:59 then 00:01 the next day; three change signals → emissions [d1, d2]). Add `KoinGraphTest.verify` using Koin's `Module.verify()` with Android `Context` as an extra type (or `checkModules` with a mocked platform module that uses `inMemoryDb()`).
- [ ] **Step 2: Run** `--tests "*SystemAppClockTest*" --tests "*KoinGraphTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the tests plus `./gradlew :shared:compileKotlinIosSimulatorArm64`. Expected: PASS.
- [ ] **Step 5: Commit** `feat: app clock with day-change signals and Koin modules`

> Tasks 9 and 10 add use cases. Register each one in `sharedModule` in the same task and keep `KoinGraphTest` green.

---

### Task 9: Command use cases

**Files:**
- Create in `domain/usecase/`: `ToggleCompletion.kt`, `CreateHabit.kt`, `UpdateHabit.kt`, `ArchiveHabit.kt`, `UnarchiveHabit.kt`, `DeleteHabit.kt`, `CreateCategory.kt`, `UpdateCategory.kt`, `DeleteCategory.kt`, `SeedDefaultCategoriesIfEmpty.kt`
- Test: `commonTest/.../domain/usecase/CommandUseCasesTest.kt` (with fakes + `FakeAppClock`)

**Interfaces:** each class is `operator fun invoke` and takes repositories and `AppClock` in its constructor.
```kotlin
ToggleCompletion:   suspend (habitId: String, date: LocalDate): Outcome<Boolean>
CreateHabit:        suspend (name: String, iconKey: String, categoryId: String, days: Set<DayOfWeek>): Outcome<String>
UpdateHabit:        suspend (id: String, name: String, iconKey: String, categoryId: String, days: Set<DayOfWeek>): Outcome<Unit>
ArchiveHabit / UnarchiveHabit / DeleteHabit: suspend (id: String): Outcome<Unit>
CreateCategory:     suspend (name: String, colorKey: String): Outcome<String>
UpdateCategory:     suspend (id: String, name: String, colorKey: String): Outcome<Unit>
DeleteCategory:     suspend (id: String): Outcome<Unit>
SeedDefaultCategoriesIfEmpty: suspend (): Unit
```
Rules:
- **Toggle:** check order is HabitNotFound → EditWindowClosed (`isEditable`) → NotDue (`habit.isDueOn(date)`) → `repo.toggle`.
- **Create/Update:** validate name, then days, then `iconKey ∈ IconKeys.all` (UnknownIcon), then category exists (CategoryNotFound). Create uses `createdOn = today`. Update calls `upsertSchedule(today)` **only if** the days differ from `currentSchedule.days`.
- **Archive:** if already archived, no-op Success. Otherwise `openArchivePeriod(today)`.
- **Unarchive:** find the open period. If `from == today`, call `deleteArchivePeriod`; otherwise `closeArchivePeriod(from, today)`. If not archived, no-op Success.
- **DeleteCategory:** if `habitCount > 0`, return CategoryNotEmpty.
- **Category create/update:** validate the name, then `colorKey ∈ ColorKeys.all` (UnknownColor).
- **Seed:** if `isEmpty()`, create in order: Health/green, Fitness/orange, Learning/blue, Mindfulness/purple, Productivity/teal.

- [ ] **Step 1: Write the failing tests:**
  - Toggle:
    - `toggleToday`
    - `toggleYesterdayAllowed`
    - `toggleTwoDaysAgoEditWindowClosed`
    - `toggleNonDueDayNotDue`
    - `toggleUnknownHabit`
    - `doubleToggleEndsNotDone`
    - `toggleYesterdayAfterTodayScheduleChange`: habit is Mon-only, today is Tue. The schedule changes today to Tue-only. Toggling yesterday (Mon) → Success(true), and toggling today (Tue) works.
  - Create/Update:
    - `createValidatesNameDaysIconCategory` (one assertion per error)
    - `createSetsCreatedOnToday`
    - `updateSameDaysNoNewScheduleVersion`
    - `updateChangedDaysAddsVersionFromToday`
    - `updateTwiceSameDayReplacesVersion`
  - Archive/Unarchive:
    - `archiveThenUnarchiveNextDayLeavesClosedPeriod`
    - `archiveAndUnarchiveSameDayLeavesNoPeriod`
    - `archiveTwiceIsNoOp`
  - Categories:
    - `deleteCategoryWithOnlyArchivedHabitFails` → CategoryNotEmpty
    - `deleteEmptyCategory`
    - `categoryUnknownColor`
  - Seed:
    - `seedOnlyWhenEmpty` (second call adds nothing; names and colors exact)
- [ ] **Step 2: Run** `--tests "*CommandUseCasesTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** and register everything in `sharedModule`. Make `initKoin` run `SeedDefaultCategoriesIfEmpty` once (`runBlocking`) after the graph starts.
- [ ] **Step 4: Run** the tests plus `KoinGraphTest`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(domain): command use cases`

---

### Task 10: Query use cases

**Files:**
- Create in `domain/usecase/`: `ObserveTodayDashboard.kt`, `ObserveMonthCalendar.kt`, `ObserveDayDetail.kt`, `ObserveHabits.kt`, `ObserveHabitDetail.kt`, `ObserveCategories.kt`, `GetHabit.kt`
- Test: `commonTest/.../domain/usecase/QueryUseCasesTest.kt`

**Interfaces:** each takes repositories + `AppClock`, combines repository flows with `clock.todayFlow` (so they re-emit at midnight), and calls the Task 3–5 functions. (These cover the spec's `CalculateOverallStreak`/`CalculateHabitStats`; those stay pure functions rather than separate classes.)
```kotlin
data class TodayHabitItem(val habit: Habit, val isDone: Boolean, val streak: Int)
data class TodayDashboard(val date: LocalDate, val done: Int, val due: Int, val streak: StreakInfo,
                          val isPerfect: Boolean, val groups: List<CategoryGroup<TodayHabitItem>>, val hasAnyHabits: Boolean)
ObserveTodayDashboard: () -> Flow<TodayDashboard>

data class CalendarDay(val date: LocalDate, val status: DayStatus)
data class MonthCalendar(val month: YearMonth, val days: List<CalendarDay>, val perfectDays: Int,
                         val completionRate: Double?, val longestPerfectRun: Int)
ObserveMonthCalendar: (month: YearMonth) -> Flow<MonthCalendar>
// completionRate = Σdone / Σdue over days ≤ today in the month, where today counts only if Perfect; null if Σdue == 0

data class DayHabitItem(val habit: Habit, val isDone: Boolean)
data class DayDetail(val date: LocalDate, val status: DayStatus, val isEditable: Boolean, val groups: List<CategoryGroup<DayHabitItem>>)
ObserveDayDetail: (date: LocalDate) -> Flow<DayDetail>

data class HabitsOverview(val active: List<CategoryGroup<Habit>>, val archived: List<Habit>)
ObserveHabits: () -> Flow<HabitsOverview>

enum class HabitDayState { Done, Missed, Pending, NotDue, Future }    // Pending = today, due, not done
data class HabitDetail(val habit: Habit, val stats: HabitStats, val month: YearMonth, val days: List<Pair<LocalDate, HabitDayState>>)
ObserveHabitDetail: (id: String, month: YearMonth) -> Flow<HabitDetail?>   // null when deleted

ObserveCategories: () -> Flow<List<Category>>
GetHabit: suspend (id: String) -> Habit?        // one-shot read for the edit form
```
- [ ] **Step 1: Write the failing tests:**
  - `dashboardListsOnlyDueActiveHabitsGroupedByCategorySortOrder`
  - `dashboardCountsAndPerfect`
  - `dashboardReemitsOnDayChange` (`FakeAppClock.setToday` → new date, done resets to 0)
  - `calendarMonthHasAllDaysAndStatuses` (Feb 2028 has 29 days)
  - `calendarSummary` (perfectDays, rate, longestPerfectRun)
  - `calendarFutureDaysAreFuture`
  - `dayDetailEditableOnlyTodayAndYesterday`
  - `habitsOverviewSplitsArchived`
  - `habitDetailStatesForMonth` (Done/Missed/Pending/NotDue/Future)
  - `habitDetailNullAfterDelete`
- [ ] **Step 2: Run** `--tests "*QueryUseCasesTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** and register in `sharedModule`.
- [ ] **Step 4: Run** the tests plus `KoinGraphTest`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(domain): query use cases`

---

> **ViewModel conventions (Tasks 11–16).** Each screen has `<Screen>Contract.kt`, with a sealed `UiState` (or a data class where noted), a sealed `Event`, and a sealed `Effect`, plus `<Screen>ViewModel : androidx.lifecycle.ViewModel` exposing:
> `val uiState: StateFlow<UiState>` (`stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loading)`), `val effects: Flow<Effect>` (from `Channel(Channel.BUFFERED).receiveAsFlow()`), and `fun onEvent(event: Event)`.
> Unexpected exceptions from use cases → `Effect.ShowError(message: String)`. `Outcome.Failure` → `Effect.ShowDomainError(error: DomainError)` unless the screen shows the error inline.
> Tests use the fakes from Task 6, `StandardTestDispatcher` + `Dispatchers.setMain`, and Turbine. Register each VM in `sharedModule` and keep `KoinGraphTest` green.

### Task 11: iOS ViewModel bridge (Kotlin side)

**Files:**
- Create: `iosMain/.../KoinHelper.kt`
- Test: `iosTest/.../ViewModelHandleTest.kt`

**Interfaces:**
```kotlin
class ViewModelHandle<VM : ViewModel>(val viewModel: VM, private val store: ViewModelStore) { fun clear() }  // store.clear() → onCleared
object KoinHelper {
    fun start()                      // initKoin() with platformModule; idempotent
    fun dashboard(): ViewModelHandle<DashboardViewModel>
    fun calendar(): ViewModelHandle<CalendarViewModel>
    fun habitList(): ViewModelHandle<HabitListViewModel>
    fun categoryList(): ViewModelHandle<CategoryListViewModel>
    fun habitDetail(id: String): ViewModelHandle<HabitDetailViewModel>
    fun habitForm(id: String?): ViewModelHandle<HabitFormViewModel>
}
```
Each handle gets its own `ViewModelStore`, created through `ViewModelProvider.create(store, viewModelFactory { initializer { koin.get<VM> { parametersOf(...) } } })`. **This task is done in two halves:** write `ViewModelHandle`, `start()`, and the test now. Add each factory function in the task that creates that ViewModel (Tasks 12–16).

- [ ] **Step 1: Write the failing test:** `clearInvokesOnCleared`, using a test-only `ViewModel` whose `onCleared` sets a flag and whose `viewModelScope` job is cancelled after `clear()`.
- [ ] **Step 2: Run** `./gradlew :shared:iosSimulatorArm64Test --tests "*ViewModelHandleTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(ios-bridge): ViewModel handles with explicit clearing`

### Task 12: DashboardViewModel

**Files:** `presentation/dashboard/{DashboardContract,DashboardViewModel}.kt`; test `commonTest/.../presentation/DashboardViewModelTest.kt`. Add `KoinHelper.dashboard()`.

**Interfaces:**
```kotlin
sealed interface DashboardUiState { data object Loading; data object NoHabits; data class Content(val dashboard: TodayDashboard) }
sealed interface DashboardEvent { data class ToggleHabit(val id: String); data object AddHabitClicked; data class HabitClicked(val id: String) }
sealed interface DashboardEffect { data object Celebrate; data object OpenCreateHabit; data class OpenHabit(val id: String); data class ShowDomainError(val error: DomainError); data class ShowError(val message: String) }
class DashboardViewModel(observe: ObserveTodayDashboard, toggle: ToggleCompletion, clock: AppClock) : ViewModel()
```
`NoHabits` when `!hasAnyHabits`. `Content` with empty groups means nothing is due today. `Celebrate` is emitted when, for the same `date`, the previous emitted dashboard had `isPerfect == false` and the new one has `isPerfect == true`, at most once per date (tracked in memory).

- [ ] **Step 1: Write the failing tests:**
  - `loadingThenContent`
  - `noHabitsState`
  - `toggleUpdatesCounts`
  - `celebrateOnceWhenDayBecomesPerfect` (untoggle and retoggle → still one Celebrate)
  - `noCelebrateWhenAlreadyPerfectOnLoad`
  - `rollsOverAtMidnight` (`FakeAppClock.setToday(next)` → Content.date == next, done == 0) *(Review Focus 1)*
  - `navigationEventsEmitEffects`
- [ ] **Step 2: Run** `--tests "*DashboardViewModelTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(presentation): dashboard view model`

### Task 13: CalendarViewModel

**Files:** `presentation/calendar/…`; test `CalendarViewModelTest.kt`. Add `KoinHelper.calendar()`.

**Interfaces:**
```kotlin
data class CalendarUiState(val month: YearMonth, val leadingBlanks: Int,     // Monday-first: firstDay.dayOfWeek.isoDayNumber - 1
                           val calendar: MonthCalendar?, val selected: DayDetail?, val canGoNext: Boolean)  // canGoNext = month < current month
sealed interface CalendarEvent { data object PrevMonth; data object NextMonth; data class SelectDay(val date: LocalDate); data object ClearSelection; data class ToggleHabit(val id: String, val date: LocalDate) }
sealed interface CalendarEffect { data class ShowDomainError(val error: DomainError); data class ShowError(val message: String) }
class CalendarViewModel(observeMonth: ObserveMonthCalendar, observeDay: ObserveDayDetail, toggle: ToggleCompletion, clock: AppClock) : ViewModel()
```
The initial month is the current one. `NextMonth` past the current month is ignored. Selecting a `Future` day is ignored.

- [ ] **Step 1: Write the failing tests:**
  - `startsAtCurrentMonthWithLeadingBlanks` (Oct 2026 starts on Thu → 3)
  - `prevNextNavigation`
  - `cannotGoPastCurrentMonth`
  - `selectDayShowsDetail`
  - `toggleOldDayEmitsEditWindowClosed`
  - `toggleUpdatesMonthStatus`
- [ ] **Step 2: Run** `--tests "*CalendarViewModelTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(presentation): calendar view model`

### Task 14: HabitListViewModel and CategoryListViewModel

**Files:** `presentation/habits/…`, `presentation/categories/…`; tests `HabitListViewModelTest.kt`, `CategoryListViewModelTest.kt`. Add the KoinHelper factories.

**Interfaces:**
```kotlin
sealed interface HabitListUiState { data object Loading; data class Content(val overview: HabitsOverview) }
sealed interface HabitListEvent { data class HabitClicked(val id: String); data object AddHabitClicked; data object ManageCategoriesClicked }
sealed interface HabitListEffect { data class OpenHabit(val id: String); data object OpenCreateHabit; data object OpenCategories }

data class CategoryListUiState(val categories: List<Category>, val editor: CategoryEditor?)
data class CategoryEditor(val id: String?, val name: String, val colorKey: String, val error: DomainError?)  // id null = new
sealed interface CategoryListEvent { data object AddClicked; data class EditClicked(val id: String); data class NameChanged(val v: String)
    data class ColorChanged(val key: String); data object SaveEditor; data object DismissEditor; data class DeleteClicked(val id: String) }
sealed interface CategoryListEffect { data class ShowDomainError(val error: DomainError); data class ShowError(val message: String) }
```
A new category editor defaults to `colorKey = "blue"`. Save errors stay inline in `editor.error`. Delete errors (CategoryNotEmpty) go out as an effect.

- [ ] **Step 1: Write the failing tests:**
  - `listGroupsActiveAndArchived`
  - `eventsEmitNavigationEffects`
  - `addCategorySaves`
  - `invalidNameShowsInlineError`
  - `deleteNonEmptyEmitsCategoryNotEmpty`
- [ ] **Step 2: Run** `--tests "*HabitListViewModelTest*" --tests "*CategoryListViewModelTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(presentation): habit list and category list view models`

### Task 15: HabitDetailViewModel

**Files:** `presentation/detail/…`; test `HabitDetailViewModelTest.kt`. Add `KoinHelper.habitDetail(id)`.

**Interfaces:**
```kotlin
sealed interface HabitDetailUiState { data object Loading; data object Deleted; data class Content(val detail: HabitDetail, val leadingBlanks: Int, val canGoNext: Boolean, val confirmingDelete: Boolean) }
sealed interface HabitDetailEvent { data object PrevMonth; data object NextMonth; data object EditClicked; data object ArchiveToggled; data object DeleteClicked; data object DeleteConfirmed; data object DeleteCancelled }
sealed interface HabitDetailEffect { data class OpenEdit(val id: String); data object Close; data class ShowDomainError(val error: DomainError); data class ShowError(val message: String) }
class HabitDetailViewModel(habitId: String, observe: ObserveHabitDetail, archive: ArchiveHabit, unarchive: UnarchiveHabit, delete: DeleteHabit, clock: AppClock) : ViewModel()
```
`ArchiveToggled` calls archive or unarchive based on `habit.isArchived`. `DeleteConfirmed` deletes the habit and then emits `Close`.

- [ ] **Step 1: Write the failing tests:**
  - `showsStatsAndMonth`
  - `archiveToggleFlipsState`
  - `deleteRequiresConfirmationThenCloses`
  - `cancelDeleteKeepsHabit`
  - `monthNavigationBounded`
- [ ] **Step 2: Run** `--tests "*HabitDetailViewModelTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(presentation): habit detail view model`

### Task 16: HabitFormViewModel

**Files:** `presentation/form/…`; test `HabitFormViewModelTest.kt`. Add `KoinHelper.habitForm(id)`.

**Interfaces:**
```kotlin
data class HabitFormUiState(
    val isEdit: Boolean, val isLoading: Boolean, val name: String, val categoryId: String?, val iconKey: String,
    val days: Set<DayOfWeek>, val categories: List<Category>, val iconKeys: List<String>,   // = IconKeys.all
    val nameError: DomainError?, val canSave: Boolean, val showScheduleNote: Boolean, val isSaving: Boolean,
)
sealed interface HabitFormEvent { data class NameChanged(val v: String); data class CategorySelected(val id: String); data class IconSelected(val key: String)
    data class DayToggled(val day: DayOfWeek); data object EveryDayToggled; data object Save }
sealed interface HabitFormEffect { data object Dismiss; data class ShowDomainError(val error: DomainError); data class ShowError(val message: String) }
class HabitFormViewModel(habitId: String?, getHabit: GetHabit, observeCategories: ObserveCategories, create: CreateHabit, update: UpdateHabit) : ViewModel()
```
Defaults for a new habit: name "", the first category, `iconKey = "checklist"`, all 7 days. `canSave` = valid name && days not empty && category set && !isSaving. `nameError` is shown only after the first edit of a non-blank field, or after a Save attempt. `showScheduleNote` = isEdit && days ≠ the loaded habit's current days. `EveryDayToggled` selects all 7 days if not all are selected; otherwise it clears to Monday only.

- [ ] **Step 1: Write the failing tests:**
  - `newHabitDefaults`
  - `editLoadsExisting`
  - `blankNameCannotSave`
  - `noDaysCannotSave`
  - `scheduleNoteOnlyWhenDaysChangedInEdit`
  - `saveCreateDismisses`
  - `saveUpdateDismisses`
  - `saveFailureKeepsFormAndShowsError`
- [ ] **Step 2: Run** `--tests "*HabitFormViewModelTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :shared:allTests`. Expected: PASS (all shared tests on host and the iOS simulator).
- [ ] **Step 5: Commit** `feat(presentation): habit form view model`

---

### Task 17: Android app: theme, palette, icons, navigation shell, Today screen

**Files:**
- Create in `androidApp/src/main/kotlin/app/habittracker/android/`: `HabitApp.kt` (Application; `initKoin { androidContext(this) }`), `MainActivity.kt`, `ui/theme/{Theme,Palette}.kt`, `ui/icons/HabitIcons.kt`, `ui/nav/AppNav.kt`, `ui/dashboard/DashboardScreen.kt`, `ui/common/{CategoryHeader,HabitRow,ProgressRing,StreakCard}.kt`
- Test: `androidApp/src/test/.../MappingTest.kt`

**Interfaces:**
- `fun categoryColor(key: String, dark: Boolean): Color`, which resolves through `ColorKeys.resolve`. Light/dark hex values:
  - red `#E5484D/#FF6369`
  - orange `#F76B15/#FF8B3E`
  - amber `#FFB224/#FFCA16`
  - green `#30A46C/#3DD68C`
  - teal `#12A594/#0BD8B6`
  - blue `#0090FF/#70B8FF`
  - indigo `#3E63DD/#849DFF`
  - purple `#8E4EC6/#BF7AF0`
  - pink `#D6409F/#FF8DCC`
  - gray `#8B8D98/#B0B4BA`
- `fun habitIcon(key: String): ImageVector`, which resolves through `IconKeys.resolve` and uses `material-icons-extended` Rounded icons named after the Material column of the icon table below.
- Navigation: `Scaffold` + `NavigationBar` with three tabs (Today `today`, Calendar `calendar`, Habits `habits`), plus `habit/{id}`, `categories`, and a `ModalBottomSheet` for the form (state hoisted in `AppNav`). ViewModels come from `koinViewModel()` / `koinViewModel { parametersOf(id) }`. Effects are collected in `LaunchedEffect`. Errors show in a `SnackbarHost`.
- Today screen per spec §7.1. Toggle triggers `HapticFeedbackType.LongPress`. `Celebrate` triggers a short scale animation of the progress ring.

**Icon table** (key → SF Symbol / Material):

| key | SF Symbol | Material |
|---|---|---|
| run | figure.run | directions_run |
| walk | figure.walk | directions_walk |
| gym | dumbbell.fill | fitness_center |
| bike | bicycle | directions_bike |
| swim | figure.pool.swim | pool |
| yoga | figure.yoga | sports_gymnastics |
| meditate | figure.mind.and.body | self_improvement |
| water | drop.fill | water_drop |
| nutrition | carrot.fill | eco |
| food | fork.knife | restaurant |
| sleep | bed.double.fill | bedtime |
| pill | pills.fill | medication |
| heart | heart.fill | favorite |
| book | book.fill | menu_book |
| study | graduationcap.fill | school |
| code | chevron.left.forwardslash.chevron.right | code |
| language | character.bubble.fill | translate |
| journal | square.and.pencil | edit_note |
| music | music.note | music_note |
| art | paintbrush.fill | brush |
| pray | hands.sparkles.fill | volunteer_activism |
| sun | sun.max.fill | wb_sunny |
| clean | sparkles | cleaning_services |
| money | dollarsign.circle.fill | savings |
| no_phone | iphone.slash | mobile_off |
| call | phone.fill | call |
| people | person.2.fill | group |
| plant | leaf.fill | local_florist |
| pet | pawprint.fill | pets |
| checklist | checklist | checklist |
| timer | timer | timer |
| target | target | track_changes |
| star | star.fill | star |
| smile | face.smiling | mood |
| no_smoking | nosign | smoke_free |
| home | house.fill | home |

- [ ] **Step 1: Write the failing tests:** `MappingTest`: every `IconKeys.all` maps to a non-fallback icon; an unknown key maps to the `checklist` icon; every `ColorKeys.all` key has distinct light and dark colors; `categoryColor("mauve", false) == categoryColor("gray", false)` *(Review Focus 5)*.
- [ ] **Step 2: Run** `./gradlew :androidApp:testDebugUnitTest`. Expected: FAIL.
- [ ] **Step 3: Implement** the theme, mappings, nav shell, and Today screen. The Calendar and Habits tabs show placeholders for now.
- [ ] **Step 4: Verify.** Run `./gradlew :androidApp:testDebugUnitTest :androidApp:assembleDebug`, then install on an emulator (`./gradlew :androidApp:installDebug`). Expected: seeded categories exist; the empty state shows "Create your first habit".
- [ ] **Step 5: Commit** `feat(android): app shell, theme and Today screen`

### Task 18: Android remaining screens and UI smoke test

**Files:**
- Create: `ui/calendar/CalendarScreen.kt`, `ui/habits/HabitListScreen.kt`, `ui/categories/CategoryListScreen.kt`, `ui/detail/HabitDetailScreen.kt`, `ui/form/HabitFormSheet.kt`, `ui/common/MonthGrid.kt` (shared by Calendar and Detail)
- Test: `androidApp/src/androidTest/.../SmokeTest.kt`

**Interfaces:** consumes the VM contracts from Tasks 13–16. Calendar markers per spec §7.2. `MonthGrid(month, leadingBlanks, cells: List<@Composable () -> Unit>)` is Monday-first. The form uses `FilterChip`s for days (labels M T W T F S S), a `LazyVerticalGrid` of icons, a category chip row, and shows the schedule note text exactly as written: "Schedule changes apply from today. Past days keep their old schedule." The delete confirmation text reads exactly: "This erases all history for this habit."

- [ ] **Step 1: Write the failing test:** `SmokeTest.createHabitThenToggleUpdatesProgress` (Compose UI test on a fresh DB: tap +, type "Read", Save, tap the check → the text "1 / 1" is shown).
- [ ] **Step 2: Run** `./gradlew :androidApp:connectedDebugAndroidTest` (emulator running). Expected: FAIL.
- [ ] **Step 3: Implement** the screens.
- [ ] **Step 4: Run** the same command. Expected: PASS. Manually check Calendar markers, archive/unarchive, delete confirmation, category CRUD, and dark mode on the emulator.
- [ ] **Step 5: Commit** `feat(android): calendar, habits, detail, categories and form screens`

### Task 19: iOS app: project, bridge, theme, shell, Today screen

**Files:**
- Create: `iosApp/project.yml` (XcodeGen: app target `HabitTracker`, iOS 17.0, bundle id `app.habittracker.ios`, a pre-build script `cd "$SRCROOT/.." && ./gradlew :shared:embedAndSignAppleFrameworkForXcode`, `FRAMEWORK_SEARCH_PATHS`/`OTHER_LDFLAGS -framework Shared` per the KMP direct-integration docs, and a UI test target `HabitTrackerUITests`)
- Create in `iosApp/HabitTracker/`: `App/HabitTrackerApp.swift` (calls `KoinHelper.shared.start()`), `App/RootTabView.swift`, `Bridge/ViewModelHost.swift`, `Theme/Palette.swift`, `Theme/HabitIcons.swift`, `Screens/Dashboard/DashboardView.swift`, `Screens/Common/{CategoryHeader,HabitRow,ProgressRing,StreakCard}.swift`
- Test: `iosApp/HabitTrackerTests/MappingTests.swift` (unit test target, also declared in `project.yml`)

**Interfaces:**
- `@MainActor @Observable final class ViewModelHost<VM: AnyObject, State>`: takes `ViewModelHandle<VM>` and a closure `(VM) -> SkieSwiftStateFlow<State>`. It exposes `vm` and `state: State`, starts a `Task` iterating the flow, and in `deinit` cancels the task and calls `handle.clear()`. Views own it with `@State`.
- Effects are collected with `.task { for await effect in host.vm.effects { … } }` and a `switch` over the SKIE-generated enum (`onEnum(of:)`).
- `Palette.color(_ key: String) -> Color`, using the same hex pairs as Task 17 as dynamic light/dark colors and resolving through `ColorKeys.shared.resolve`. `HabitIcons.symbol(_ key: String) -> String`, using the SF Symbol column of the Task 17 table and resolving through `IconKeys.shared.resolve`.
- `RootTabView`: a `TabView` with three tabs (Today `checkmark.circle`, Calendar `calendar`, Habits `list.bullet`), each with its own `NavigationStack`. The form opens as a `.sheet`. Toggle triggers `.sensoryFeedback(.success, trigger:)`. `Celebrate` triggers a spring scale on the progress ring.

- [ ] **Step 1: Write the failing tests:** `MappingTests` mirrors Task 17: every icon key maps to an SF Symbol that `UIImage(systemName:)` can load (non-nil); an unknown key → `"checklist"`; `"mauve"` → the gray color.
- [ ] **Step 2: Run** `cd iosApp && xcodegen && xcodebuild test -scheme HabitTracker -destination 'platform=iOS Simulator,name=iPhone 17' -only-testing:HabitTrackerTests`. Expected: FAIL.
- [ ] **Step 3: Implement** the bridge, theme, shell, and Today screen (other tabs show placeholders).
- [ ] **Step 4: Verify.** Run the same command (PASS). Then run the app in the simulator (open the live simulator panel for the user first). Expected: seeded state, empty state CTA, and the ViewModel `onCleared` runs when the screen is dismissed (temporary `println` in `onCleared`, removed before commit).
- [ ] **Step 5: Commit** `feat(ios): Xcode project, ViewModel bridge, theme and Today screen`

### Task 20: iOS remaining screens and UI smoke test

**Files:**
- Create in `iosApp/HabitTracker/Screens/`: `Calendar/CalendarView.swift`, `Habits/HabitListView.swift`, `Categories/CategoryListView.swift`, `Detail/HabitDetailView.swift`, `Form/HabitFormView.swift`, `Common/MonthGrid.swift`
- Test: `iosApp/HabitTrackerUITests/SmokeUITests.swift`

**Interfaces:** consumes the VM contracts from Tasks 13–16. The copy strings, the Monday-first grid, and the markers match Task 18 and spec §7. The app accepts a launch argument `-uiTestResetDB`, which deletes `habits.db` before `KoinHelper.start()`. Implement this through `DriverFactory`/`KoinHelper.start(resetDatabase: Boolean)`.

- [ ] **Step 1: Write the failing test:** `testCreateHabitThenToggleUpdatesProgress` (launch with `-uiTestResetDB`, tap +, type "Read", Save, tap the check → the static text "1 / 1" exists).
- [ ] **Step 2: Run** `xcodebuild test -scheme HabitTracker -destination 'platform=iOS Simulator,name=iPhone 17' -only-testing:HabitTrackerUITests`. Expected: FAIL.
- [ ] **Step 3: Implement** the screens.
- [ ] **Step 4: Run** the same command. Expected: PASS. Manually check in the simulator: Calendar markers, archive/unarchive, delete confirmation, category CRUD, dark mode, Dynamic Type at XXL, and leaving the app open across a simulated date change (Review Focus 1).
- [ ] **Step 5: Commit** `feat(ios): calendar, habits, detail, categories and form screens`

### Task 21: Final verification and README

**Files:** Create `README.md` (what the app is, how to build and test both platforms, the Xcode/JDK prerequisites, and a pointer to the spec).

- [ ] **Step 1: Run** `./gradlew :shared:allTests :androidApp:testDebugUnitTest :androidApp:assembleDebug` and both `xcodebuild test` commands. Expected: everything passes.
- [ ] **Step 2: Write README.md.**
- [ ] **Step 3: Commit** `docs: README with build and test instructions`
