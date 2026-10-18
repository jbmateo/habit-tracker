# UI redesign (bold hero and celebrations) implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the iPhone app a bold "hero" identity, with check-off motion, once-a-day confetti and streak milestone cards, backed by shared, tested celebration rules.

**Architecture:**
- Shared Kotlin gains two stored preferences and pure milestone logic.
- `DashboardViewModel` turns these into a persisted once-per-day `Celebrate` effect and a `milestone` field on its state.
- iOS gets a `Theme` token layer and new Today components (hero card, milestone card, animated rows, confetti), then restyles the other screens.
- Android's UI is untouched.

**Tech Stack:** Kotlin 2.4.20, SQLDelight 2.4.1 (existing `Preference` table, no migration), Koin 4.2.2, SKIE 0.10.15, SwiftUI on iOS 17+ (`Canvas`, `TimelineView`, `sensoryFeedback`, `contentTransition`), XCTest.

**Spec:** `docs/design/2026-10-19-ui-redesign-design.md`

## Global Constraints

- Work on `main` (the user's standing choice). Every commit ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- `domain/` imports only `kotlin.*`, `kotlinx.coroutines.*`, `kotlinx.datetime.*` and `app.habittracker.domain.*`. `ArchitectureTest` enforces this.
- Milestones are exactly `[7, 30, 100, 365]`. Preference keys are `celebrated_on` (ISO date) and `milestone_seen` (`"<runStart ISO>:<milestone>"`).
- Theme hex values come verbatim from spec §3:
  - canvas `#F4F3F8`/`#13121C`, card `#FFFFFF`/`#1E1C2B`, hero `#3C3489`/`#2A2466`
  - heroText `#EEEDFE`, heroSubtext `#CECBF6`, heroTrack `#534AB7`/`#3C3489`
  - flame `#EF9F27`, gold `#FFB224`/`#FFCA16`
- Copy is exact:
  - "All done today ✨" and "N of M done today"
  - Milestone titles "7-day streak", "30-day streak", "100-day streak", "365-day streak"
  - Milestone bodies "One week strong", "A month of perfect days", "100 days. Legendary.", "A whole year"
  - Accessibility labels "Milestone: <title>. <body>" and "Dismiss milestone"
- Labels the UI tests rely on must survive the restyle:
  - `"N of M habits done"` (now on the hero card)
  - `"Mark done: <name>"` and `"Mark not done: <name>"`
  - Tab buttons "Today", "Calendar" and "Habits"
- Every animation has a Reduce Motion path: no confetti, no spring, opacity only.
- iOS checks run on the "iPhone 17 Pro" simulator (UDID `81E64689-FC28-47D8-9976-7E9C8F4214A3`). iOS UI tests wipe its database, so re-seed the sample data afterwards (Task 7).
- Commands:
  - `./gradlew :shared:allTests`
  - `./gradlew :androidApp:testDebugUnitTest :androidApp:assembleDebug :androidApp:connectedDebugAndroidTest`
  - `cd iosApp && xcodegen && xcodebuild test -scheme HabitTracker -destination 'platform=iOS Simulator,id=81E64689-FC28-47D8-9976-7E9C8F4214A3'`. This hangs after printing its results: run it in the background, poll the log for `** TEST SUCCEEDED|FAILED **`, then `pkill -f "xcodebuild test -scheme HabitTracker"`.

## Review Focus

The five failure modes most likely to reach the user, each with the test that pins it:

1. **Midnight rollover while Today is open.** Yesterday's stored `celebrated_on` must not stop today's confetti. Pinned by Task 3's `celebrateFiresNextDayAfterCelebratingYesterday`.
2. **Streak broken today but not yet missed.** The streak counts through yesterday while today is undone, so the run start must not shift by a day and re-show a dismissed milestone. Pinned by Task 2's `runStartWhenTodayNotYetDone`.
3. **Dismissing 7 at a 40-day streak.** The highest milestone (30) is what's showing, so dismissing stores 30. Pinned by Task 3's `dismissStoresTheShownMilestone`.
4. **Corrupt or unparseable `milestone_seen`.** It's treated as nothing seen and never crashes. Pinned by Task 1's `garbageMilestoneSeenReadsAsNull`.
5. **Un-checking the last habit then re-checking it.** No second confetti that day, even across a ViewModel recreation. Pinned by Task 3's `celebrateOncePerDayAcrossReopen`.

---

### Task 1: Celebration preferences storage

**Files:**
- Modify: `shared/src/commonMain/kotlin/app/habittracker/domain/repository/PreferencesRepository.kt`
- Modify: `shared/src/commonMain/kotlin/app/habittracker/data/repository/SqlPreferencesRepository.kt`
- Create: `shared/src/commonMain/kotlin/app/habittracker/domain/model/MilestoneSeen.kt`
- Modify: `shared/src/commonTest/kotlin/app/habittracker/fakes/FakePreferencesRepository.kt`
- Test: `shared/src/androidHostTest/kotlin/app/habittracker/data/SqlPreferencesRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `data class MilestoneSeen(val runStart: LocalDate, val milestone: Int)`
  - On `PreferencesRepository`:
    - `fun observeCelebratedOn(): Flow<LocalDate?>`
    - `suspend fun setCelebratedOn(date: LocalDate)`
    - `fun observeMilestoneSeen(): Flow<MilestoneSeen?>`
    - `suspend fun setMilestoneSeen(seen: MilestoneSeen)`
  - The fake exposes `celebratedOn: MutableStateFlow<LocalDate?>` and `milestoneSeen: MutableStateFlow<MilestoneSeen?>`.

- [ ] **Step 1: Write the failing tests** in `SqlPreferencesRepositoryTest`:
  - `celebratedOnRoundTrips`: null before, then `date("2026-10-19")` after a set.
  - `milestoneSeenRoundTrips`: stores `MilestoneSeen(date("2026-10-11"), 7)`. The raw `Preference` value is `"2026-10-11:7"` and it reads back equal.
  - `garbageMilestoneSeenReadsAsNull`: raw upserts of `"nope"` and `"2026-10-11:x"` both read as `null`.
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "*SqlPreferencesRepositoryTest*"`. Expected: compilation FAIL (unresolved `observeCelebratedOn`).
- [ ] **Step 3: Implement.**
  - Use keys `celebrated_on` and `milestone_seen`.
  - Parse with `LocalDate.parse` and `toIntOrNull`, wrapped in `runCatching`, so a bad value reads as `null`.
  - Follow `observeTodaySort`'s pattern: `asFlow().mapToOneOrNull(io)`, then map, then `distinctUntilChanged()`.
  - Update the fake.
- [ ] **Step 4: Run** `./gradlew :shared:allTests`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(redesign): store celebrated date and seen milestones`

### Task 2: Milestone rules

**Files:**
- Create: `shared/src/commonMain/kotlin/app/habittracker/domain/logic/Milestones.kt`
- Test: `shared/src/commonTest/kotlin/app/habittracker/domain/logic/MilestonesTest.kt`

**Interfaces:**
- Consumes: `MilestoneSeen` (Task 1).
- Produces:
  - `val MILESTONES: List<Int> = listOf(7, 30, 100, 365)`
  - `fun streakRunStart(today: LocalDate, currentStreak: Int, todayDone: Boolean): LocalDate?`
  - `fun pendingMilestone(currentStreak: Int, runStart: LocalDate?, seen: MilestoneSeen?): Int?`

`StreakInfo.current` counts through yesterday while today is undone, and includes today once today is perfect. The run start is `(if todayDone today else today − 1) − (currentStreak − 1)` days, or `null` when `currentStreak == 0`.

- [ ] **Step 1: Write the failing tests:**
  - `runStartWhenTodayDone`: (2026-10-19, 8, true) gives 2026-10-12.
  - `runStartWhenTodayNotYetDone`: (2026-10-19, 8, false) gives 2026-10-11.
  - `noRunWithoutStreak`: (…, 0, …) gives null.
  - `noMilestoneBelowSeven`: (6, run, null) gives null.
  - `milestoneAtSeven`: (7, run, null) gives 7.
  - `highestMilestoneWins`: (40, run, null) gives 30.
  - `dismissedMilestoneStaysHidden`: (8, run, `MilestoneSeen(run, 7)`) gives null.
  - `nextMilestoneAfterDismissedOne`: (30, run, `MilestoneSeen(run, 7)`) gives 30.
  - `rebuiltRunShowsMilestoneAgain`: (7, newRun, `MilestoneSeen(oldRun, 30)`) gives 7.
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "*MilestonesTest*"`. Expected: FAIL (unresolved).
- [ ] **Step 3: Implement both functions** in `Milestones.kt`.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit** `feat(redesign): streak run and milestone rules`

### Task 3: Dashboard celebrations

**Files:**
- Create: `shared/src/commonMain/kotlin/app/habittracker/domain/usecase/CelebrationMemory.kt`
- Modify: `shared/src/commonMain/kotlin/app/habittracker/presentation/dashboard/DashboardContract.kt` and `DashboardViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/app/habittracker/di/Modules.kt`
- Test: `shared/src/commonTest/kotlin/app/habittracker/presentation/DashboardViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 preferences; Task 2's `streakRunStart` and `pendingMilestone`.
- Produces:
  - `class CelebrationMemory(prefs: PreferencesRepository)` with:
    - `fun celebratedOn(): Flow<LocalDate?>`
    - `fun milestoneSeen(): Flow<MilestoneSeen?>`
    - `suspend fun markCelebrated(date: LocalDate)`
    - `suspend fun dismissMilestone(seen: MilestoneSeen)`
  - `DashboardUiState.Content` gains `val milestone: Int? = null`.
  - `DashboardEvent.DismissMilestone : DashboardEvent` (data object).
  - Constructor: `DashboardViewModel(observe, toggle, observeSort, setSort, celebrations: CelebrationMemory, clock)`.

- [ ] **Step 1: Write the failing tests.** Add `CelebrationMemory(prefs)` to `vm()`, then:
  - `celebrateOncePerDayAcrossReopen`:
    1. Check off the only habit and expect one `Celebrate`; `prefs.celebratedOn` becomes today.
    2. Create a new VM, un-check, then re-check. Expect no `Celebrate`.
  - `celebrateFiresNextDayAfterCelebratingYesterday`: with `prefs.celebratedOn = yesterday`, checking off today emits `Celebrate`.
  - The existing celebrate tests (load, transition) still pass.
  - `milestoneShownAtSevenDayStreak`: perfect days for the 7 days ending yesterday gives `milestone == 7`.
  - `dismissStoresTheShownMilestone`:
    1. A 40-day streak shows 30.
    2. `DismissMilestone` sets `prefs.milestoneSeen == MilestoneSeen(runStart, 30)`.
    3. The state's `milestone` becomes null.
  - `noMilestoneUnderSeven`: a 6-day streak gives null.
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "*DashboardViewModelTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement.**
  - Keep the existing transition detection (`previous`), but replace the in-memory `celebrated` set with a check against the latest `celebratedOn` value. On firing, call `markCelebrated(date)`.
  - Combine `celebrations.milestoneSeen()` into the state to compute `milestone` with `pendingMilestone(streak.current, streakRunStart(date, streak.current, isPerfect), seen)`.
  - `DismissMilestone` reads the current Content's dashboard, rebuilds the same `runStart` and milestone, and stores them.
  - Register `factory { CelebrationMemory(get()) }`; `viewModelOf` picks up the new parameter.
- [ ] **Step 4: Run** `./gradlew :shared:allTests :androidApp:testDebugUnitTest :androidApp:assembleDebug`. Expected: PASS, including `KoinGraphTest` and `ArchitectureTest`, with Android compiling unchanged.
- [ ] **Step 5: Commit** `feat(redesign): once-a-day confetti and streak milestones in the dashboard`

### Task 4: iOS theme, flame tiers and milestone copy

**Files:**
- Create: `iosApp/HabitTracker/Theme/Theme.swift`
- Create: `iosApp/HabitTracker/Screens/Common/Flame.swift`
- Test: `iosApp/HabitTrackerTests/ThemeTests.swift`

**Interfaces:**
- Produces:
  - `enum Theme`, with static `Color`s `canvas`, `card`, `hero`, `heroText`, `heroSubtext`, `heroTrack`, `flame` and `gold`, each a dynamic light/dark pair built like `Palette.color`.
  - `static let cornerRadius: CGFloat = 20`
  - `static func contrastRatio(_ a: UInt32, _ b: UInt32) -> Double`, plus `enum ThemeHex` with the raw pairs used by the colors and the test.
  - `enum FlameTier { case none, small, medium, large, legendary }`, with `static func forStreak(_ n: Int) -> FlameTier` and `var pointSize: CGFloat` (none 28, small 28, medium 36, large 44, legendary 44).
  - `struct FlameView: View { let streak: Int }`: `flame.fill` in the flame color; the outline at `.none`; an extra gold inner layer at `.legendary`.
  - `enum MilestoneCopy { static func title(_ m: Int) -> String; static func body(_ m: Int) -> String }`

- [ ] **Step 1: Write the failing tests:**
  - `testFlameTierBoundaries`:
    - 0 → none, 6 → small, 7 → medium, 29 → medium
    - 30 → large, 99 → large, 100 → legendary
  - `testMilestoneCopy`: all 4 titles and bodies match Global Constraints.
  - `testHeroTextContrast`: heroText and heroSubtext against hero reach at least 4.5 in light and dark.
- [ ] **Step 2: Run** the iOS command. Expected: build FAIL (unknown `FlameTier`).
- [ ] **Step 3: Implement** the three types. Move the WCAG luminance helper out of `Palette` into `Theme.contrastRatio`, and have `Palette` call it.
- [ ] **Step 4: Run** the iOS command. Expected: TEST SUCCEEDED.
- [ ] **Step 5: Commit** `feat(redesign): iOS theme tokens, flame tiers and milestone copy`

### Task 5: iOS Today redesign with motion

**Files:**
- Create in `iosApp/HabitTracker/Screens/Dashboard/`:
  - `HeroStreakCard.swift`
  - `MilestoneCard.swift`
  - `ConfettiView.swift`
- Modify:
  - `iosApp/HabitTracker/Screens/Dashboard/DashboardView.swift`
  - `iosApp/HabitTracker/Screens/Common/HabitRow.swift`
- Delete: `ProgressRing.swift` and `StreakCard.swift`, once nothing references them (check with `grep`).
- Test: `iosApp/HabitTrackerUITests/SmokeUITests.swift`

**Interfaces:**
- Consumes:
  - Task 3's `milestone` and `DismissMilestone`.
  - Task 4's `Theme`, `FlameView` and `MilestoneCopy`.
  - The existing `Celebrate` effect.
- Produces:
  - `HeroStreakCard(dashboard: TodayDashboard, celebrateTrigger: Int)`, which carries the accessibility label "N of M habits done".
  - `MilestoneCard(milestone: Int, onDismiss: () -> Void)`
  - `ConfettiView(trigger: Int)`

- [ ] **Step 1: Write the failing UI test** `testCompletingOnlyHabitShowsAllDone`:
  1. Create a habit, as `testCreateHabitThenToggleUpdatesProgress` does.
  2. Tap "Mark done: Read".
  3. Assert `app.staticTexts["All done today ✨"]` exists within 5s.
- [ ] **Step 2: Run** the iOS command. Expected: the new test FAILS and the others pass.
- [ ] **Step 3: Implement** spec §3 (Today) and §5:
  - **Hero card:**
    - Date, a 44pt rounded streak number with `.contentTransition(.numericText())`, and `FlameView`.
    - An animated progress bar: flame fill on the heroTrack, turning gold when `isPerfect`.
    - A subtitle reading "N of M done today", or "All done today ✨" when perfect.
    - `WeekComparisonLine` in heroSubtext.
  - **Milestone card:** shown when `milestone != nil`, with the VoiceOver labels from Global Constraints.
  - **Rows:**
    - Card surface with a solid category icon circle.
    - Strikethrough when done.
    - The check button springs (scale 1.25 → 1), using `.sensoryFeedback(.impact(weight: .light), trigger:)` on check.
    - The final check-off gets `.success`, triggered by `Celebrate`.
  - **Confetti:**
    - About 60 particles over 1.8s, drawn with `Canvas` and `TimelineView(.animation)`.
    - `allowsHitTesting(false)`; fired by `Celebrate`.
    - With `accessibilityReduceMotion`, replace it with a 0.4s gold pulse on the hero.
  - **Screen:** the list background is `Theme.canvas` (`.scrollContentBackground(.hidden)`) and the tab tint is `Theme.hero`.
- [ ] **Step 4: Run** the iOS command. Expected: TEST SUCCEEDED for all UI tests.
- [ ] **Step 5: Check by hand on the iPhone 17 Pro**, with sample data re-seeded:
  - The 7-day milestone card shows and dismisses.
  - Confetti fires on the last check-off.
  - Light and dark screenshots.
  - Reduce Motion on: no confetti, only the gold pulse.
- [ ] **Step 6: Commit** `feat(redesign): bold hero Today with check-off motion, confetti and milestones`

### Task 6: Restyle the other iOS screens

**Files:**
- Modify:
  - `Screens/Calendar/CalendarView.swift`
  - `Screens/Detail/HabitDetailView.swift`
  - `Screens/Habits/HabitListView.swift`
  - `Screens/Categories/CategoryListView.swift`
  - `Screens/Form/HabitFormView.swift`
  - `Screens/Common/CategoryHeader.swift`
  - The stat tile view in `HabitDetailView.swift` and `CalendarView.swift`

**Interfaces:**
- Consumes: Task 4's `Theme`.
- Produces: no new API.

- [ ] **Step 1: Restyle** per spec §3, "Other screens":
  - `Theme.canvas` backgrounds with hidden scroll backgrounds.
  - `Theme.card` sections at a 20pt radius.
  - Rounded display numbers on stat tiles, keeping their meaning colors.
  - Solid category icon circles in the Habits list and Detail header.
  - Calendar streak bands unchanged.
- [ ] **Step 2: Run** the iOS command. Expected: TEST SUCCEEDED, so no label used by a test was lost.
- [ ] **Step 3: Take light and dark screenshots** of Calendar, Detail, Habits and Categories on the iPhone 17 Pro, and check for clipped text and contrast problems.
- [ ] **Step 4: Commit** `feat(redesign): restyle calendar, detail, habits and categories`

### Task 7: Verification and docs

- [ ] **Step 1: Run every command** in Global Constraints. Expected: all PASS. Android's visuals are unchanged and its suites are green.
- [ ] **Step 2: README.** Under Features, add "Bold Today screen with confetti when you finish the day and streak milestones (iOS)". Note that Android keeps the previous look for now.
- [ ] **Step 3: Re-seed the iPhone 17 Pro sample data:** 3 habits, completions for Oct 1–18 as before, and Morning run's reminder at 07:00. The 8-day streak then shows the 7-day milestone card.
- [ ] **Step 4: Commit** `docs: redesign in README`
