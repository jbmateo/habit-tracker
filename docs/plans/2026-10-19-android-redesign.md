# Android redesign implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Android app the iPhone redesign's identity and delight: the indigo hero Today, check-off motion and haptics, once-a-day confetti and milestone cards, plus a restyle of the other screens.

**Architecture:**
- A `HabitColors` token set, exposed via `CompositionLocal`, sits beside a Material 3 `ColorScheme` re-pointed at those tokens.
- Today gets new Compose components that read the shared ViewModel's existing `Celebrate`, `Content.milestone` and `DismissMilestone`.
- Other screens move onto the canvas and card tokens.
- No shared Kotlin changes.

**Tech Stack:** Jetpack Compose (Compose BOM already in use), Material 3, Kotlin 2.4.20, JUnit via `kotlin.test` for JVM tests, and the Compose UI test (`createAndroidComposeRule`) for instrumented tests.

**Spec:** `docs/design/2026-10-19-android-redesign-design.md`, which builds on `docs/design/2026-10-19-ui-redesign-design.md`.

## Global Constraints

- Work on `main`, and end each commit with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Token hex pairs, light / dark:

  | Token | Light | Dark |
  |---|---|---|
  | canvas | `#F4F3F8` | `#13121C` |
  | card | `#FFFFFF` | `#1E1C2B` |
  | hero | `#3C3489` | `#2A2466` |
  | heroText | `#EEEDFE` | `#EEEDFE` |
  | heroSubtext | `#CECBF6` | `#CECBF6` |
  | heroTrack | `#534AB7` | `#3C3489` |
  | flame | `#EF9F27` | `#EF9F27` |
  | gold | `#FFB224` | `#FFCA16` |
  | accent | `#3C3489` | `#AFA9EC` |

- Card radius is 20dp. Display numbers use `FontFamily.SansSerif` with `FontWeight.Black`: 44sp for the hero streak, 22sp for stat tiles.
- Flame tiers: 0 none, 1–6 small (28dp), 7–29 medium (36dp), 30–99 large (44dp), 100+ legendary (44dp plus a gold inner flame).
- Copy is exact:
  - Progress: "All done today ✨", "N of M done today", "day streak · best N".
  - Milestone titles: "7-day streak", "30-day streak", "100-day streak", "365-day streak".
  - Milestone bodies: "One week strong", "A month of perfect days", "100 days. Legendary.", "A whole year".
  - TalkBack: "Milestone: <title>. <body>" and "Dismiss milestone".
- Content descriptions the tests rely on must survive: "N of M habits done", "Mark done: <name>", "Mark not done: <name>", "Add habit", and the tab labels "Today", "Calendar" and "Habits".
- Animations count as off when `Settings.Global.ANIMATOR_DURATION_SCALE == 0`. When off: no confetti (a 0.4s gold hero outline pulse instead), no spring, and fades only.
- The Detail screen's Delete button stays red.
- Manual checks run on AVD `Pixel_8a`. Instrumented tests wipe and uninstall the app, so reinstall and re-seed the sample data afterwards (Task 4).
- Commands:
  - `./gradlew :androidApp:testDebugUnitTest`
  - `./gradlew :androidApp:assembleDebug`
  - `./gradlew :androidApp:connectedDebugAndroidTest`, which needs the emulator running.
  - Screenshots: `adb emu screenrecord screenshot <file>`, because plain `screencap` returns a blank image on this emulator.

## Review Focus

These are the five failure modes most likely to reach the user. Each line names the test that pins it.

1. **TalkBack reading the check button and the confetti.** Confetti must not be focusable, and each row must still read "Mark done: <name>". Pinned by `SmokeTest.completingOnlyHabitShowsAllDone`, which finds the button by that description after the restyle.
2. **"Remove animations" on (scale 0).** No confetti, no spring. Pinned by `ThemeTest.animationsDisabledAtZeroScale`, plus a manual check.
3. **Dark-mode contrast of hero text.** heroText and heroSubtext on hero must reach at least 4.5:1. Pinned by `ThemeTest.heroTextContrast`.
4. **A milestone card for a very long streak (365+).** It shows the legendary flame and the "A whole year" copy. Pinned by `ThemeTest.flameTierBoundaries` (100 → legendary) and `milestoneCopy`.
5. **Rotating, or leaving Today during the confetti.** No crash and no stuck pieces. The confetti is keyed on a trigger counter and stops at 1.8s. Checked by hand in Task 2.

---

### Task 1: Theme tokens, flame tiers, milestone copy and the animations-off check

**Files:**
- Modify: `androidApp/src/main/kotlin/app/habittracker/android/ui/theme/Theme.kt`
- Modify: `androidApp/src/main/kotlin/app/habittracker/android/ui/theme/Palette.kt` (make `luminance` internal and add `contrastRatio`)
- Create: `androidApp/src/main/kotlin/app/habittracker/android/ui/common/Flame.kt`
- Test: `androidApp/src/test/kotlin/app/habittracker/android/ThemeTest.kt`

**Interfaces:**
- Produces:
  - `@Immutable data class HabitColors(val canvas: Color, val card: Color, val hero: Color, val heroText: Color, val heroSubtext: Color, val heroTrack: Color, val flame: Color, val gold: Color, val accent: Color)`.
  - `val LightHabitColors` and `val DarkHabitColors`, plus `val LocalHabitColors`.
  - `object HabitTheme { val colors: HabitColors @Composable get() }`.
  - `HabitTheme(darkTheme, content)` (existing name, kept) now provides `LocalHabitColors` and a `ColorScheme` with `primary = accent`, `background = canvas`, `surface = card` and `surfaceContainer = card`.
  - `val CardShape = RoundedCornerShape(20.dp)`.
  - `val DisplayFont = FontFamily.SansSerif`.
  - `fun contrastRatio(a: Color, b: Color): Double`.
  - `enum class FlameTier { None, Small, Medium, Large, Legendary }`, with `companion fun forStreak(streak: Int): FlameTier` and `val size: Dp`.
  - `@Composable fun Flame(streak: Int, modifier: Modifier = Modifier)`, drawn with a flame icon (`Icons.Rounded.LocalFireDepartment`), plus a smaller gold overlay at Legendary.
  - `object MilestoneCopy { fun title(m: Int): String; fun body(m: Int): String }`.
  - `fun animationsDisabled(animatorDurationScale: Float): Boolean`, plus `@Composable fun rememberAnimationsDisabled(): Boolean`, which reads `Settings.Global.ANIMATOR_DURATION_SCALE` with default 1f.

- [ ] **Step 1: Write the failing tests** in `ThemeTest`:
  - `flameTierBoundaries`: 0→None, 6→Small, 7→Medium, 29→Medium, 30→Large, 99→Large, 100→Legendary.
  - `milestoneCopy`: all four titles and bodies, exactly as in Global Constraints.
  - `heroTextContrast`: `contrastRatio(heroText, hero) >= 4.5` and `contrastRatio(heroSubtext, hero) >= 4.5` for `LightHabitColors` and `DarkHabitColors`.
  - `animationsDisabledAtZeroScale`: `animationsDisabled(0f)` is true, and `animationsDisabled(1f)` and `animationsDisabled(0.5f)` are false.
- [ ] **Step 2: Run** `./gradlew :androidApp:testDebugUnitTest --tests "*ThemeTest*"`. Expected: compile FAIL (unresolved `FlameTier`).
- [ ] **Step 3: Implement** everything in the Interfaces block. `HabitTheme` keeps its signature, so `MainActivity` and the tests are unchanged.
- [ ] **Step 4: Run** `./gradlew :androidApp:testDebugUnitTest`. Expected: PASS, including the existing `PaletteContrastTest`.
- [ ] **Step 5: Commit** `feat(android-redesign): theme tokens, flame tiers and milestone copy`

### Task 2: Today redesign with motion

**Files:**
- Create in `androidApp/src/main/kotlin/app/habittracker/android/ui/dashboard/`:
  - `HeroStreakCard.kt`
  - `MilestoneCard.kt`
  - `Confetti.kt`
- Create: `androidApp/src/main/kotlin/app/habittracker/android/ui/common/CategoryIcon.kt`
- Modify:
  - `ui/dashboard/DashboardScreen.kt`
  - `ui/common/HabitRow.kt`
  - `ui/common/WeekComparisonLine.kt` (add `onHero: Boolean = false`)
- Delete: `ui/common/ProgressRing.kt` and `ui/common/StreakCard.kt`, once `grep` shows no users.
- Test: `androidApp/src/androidTest/kotlin/app/habittracker/android/SmokeTest.kt`

**Interfaces:**
- Consumes:
  - Task 1's tokens, `Flame` and `MilestoneCopy`.
  - From shared code: `DashboardUiState.Content.milestone: Int?`, `DashboardEvent.DismissMilestone` and `DashboardEffect.Celebrate`.
- Produces:
  - `@Composable fun HeroStreakCard(dashboard: TodayDashboard, pulseTrigger: Int, modifier: Modifier = Modifier)`. The progress bar carries `contentDescription = "N of M habits done"`.
  - `@Composable fun MilestoneCard(milestone: Int, onDismiss: () -> Unit, modifier: Modifier = Modifier)`.
  - `@Composable fun Confetti(trigger: Int, colors: List<Color>, modifier: Modifier = Modifier)`.
  - `@Composable fun CategoryIcon(iconKey: String, colorKey: String, size: Dp = 34.dp)`.

- [ ] **Step 1: Write the failing test** `SmokeTest.completingOnlyHabitShowsAllDone`:
  1. Create "Read" via "Create your first habit".
  2. Click the `contentDescription` "Mark done: Read".
  3. `rule.waitUntil(5_000)` for the node with text "All done today ✨", then `assertIsDisplayed()`.
- [ ] **Step 2: Run** `./gradlew :androidApp:connectedDebugAndroidTest`. Expected: the new test FAILS and the other two pass.
- [ ] **Step 3: Implement** spec §3 (Today) and §4:
  - **Hero:** date, streak number (`AnimatedContent`, slide up, no slide when animations are off), `Flame`, an animated progress bar (gold when `isPerfect`), the subtitle copy and `WeekComparisonLine(onHero = true)`.
  - **Gold outline pulse:** an `Animatable` alpha, 0 → 1 → 0 over about 0.65s, keyed on `pulseTrigger`. Pass `pulseTrigger = if (animationsOff) celebrations else 0`.
  - **Milestone card:** under the hero, `AnimatedVisibility` (fade only), dismissed with `DismissMilestone`.
  - **Rows:**
    - Card background with a `CategoryIcon`.
    - When done, the name gets strikethrough and muted color.
    - The check fill scales from the center.
    - On each change to done, a spring bounce (1 → 1.25 → 1 via `Animatable`) unless animations are off.
  - **Haptics:** `ContextClick` per check-off, and `Confirm` when done reaches due (replacing the current `LongPress`).
  - **Confetti:** overlay on the Today content, skipped when animations are off, colors = flame, gold, accent plus today's category colors. It has no semantics (`clearAndSetSemantics {}`).
  - **Screen background:** canvas.
- [ ] **Step 4: Run** `./gradlew :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest`. Expected: PASS (3 instrumented tests).
- [ ] **Step 5: Check by hand on Pixel_8a,** with the sample data re-seeded:
  - The 7-day card shows and dismisses.
  - Confetti is captured mid-flight via `adb emu screenrecord screenshot`.
  - Light and dark mode.
  - `adb shell settings put global animator_duration_scale 0`: no confetti and a gold pulse. Restore the scale to 1 afterwards.
  - Rotating mid-confetti causes no crash.
- [ ] **Step 6: Commit** `feat(android-redesign): bold hero Today with check-off motion, confetti and milestones`

### Task 3: Restyle Calendar, Detail, Habits and Categories

**Files:**
- Modify:
  - `ui/calendar/CalendarScreen.kt`
  - `ui/detail/HabitDetailScreen.kt`
  - `ui/habits/HabitListScreen.kt`
  - `ui/categories/CategoryListScreen.kt`
  - `ui/common/StatTile.kt`
  - `ui/common/CategoryHeader.kt`
  - `ui/form/HabitFormSheet.kt` (colors only)

**Interfaces:**
- Consumes: Task 1's tokens and `CardShape`; Task 2's `CategoryIcon`.
- Produces: no new API.

- [ ] **Step 1: Restyle** per spec §3, "Other screens":
  - Canvas backgrounds; month header and grid in one `card`/`CardShape` surface.
  - `StatTile` numbers in `DisplayFont` Black at 22sp, keeping their meaning colors, on a card base.
  - `CategoryIcon` in the Habits list and the Detail header.
  - Rows on card surfaces; category headers in bold.
  - Delete keeps a red tint (`colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)` or equivalent).
- [ ] **Step 2: Run** `./gradlew :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest`. Expected: PASS.
- [ ] **Step 3: Take light and dark screenshots** of Calendar, Detail, Habits and Categories on Pixel_8a, and check for no clipped text and readable contrast.
- [ ] **Step 4: Commit** `feat(android-redesign): restyle calendar, detail, habits and categories`

### Task 4: Verification and docs

- [ ] **Step 1: Run every command** in Global Constraints, plus `./gradlew :shared:allTests`. Expected: all PASS.
- [ ] **Step 2: README.** Replace "(iOS). Android keeps the previous look for now." with "(iOS and Android)."
- [ ] **Step 3: Reinstall and re-seed the Pixel_8a sample data:** 3 habits, completions Oct 1–18, Morning run reminder at 07:00, and both celebration preferences cleared.
- [ ] **Step 4: Commit** `docs: Android redesign in README`
