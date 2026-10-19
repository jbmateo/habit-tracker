# Android redesign: bold hero and celebrations

Date: 2026-10-19 · Status: approved in conversation, awaiting spec review
Builds on: `docs/design/2026-10-19-ui-redesign-design.md` (the iOS redesign). Everything not restated here, including palette values, copy, flame tiers and celebration rules, comes from that spec.

## 1. Goal

Bring the iPhone redesign's identity and delight to the Android app (Jetpack Compose).

- **Same identity:** the same palette, hero card, flame tiers, milestone card, confetti and copy.
- **Android-native details:** Material 3 controls stay, including the navigation bar, time picker, switches and dialogs. They're themed through Material's color roles.

The shared celebration rules (`Celebrate`, `Content.milestone`, `DismissMilestone`) already exist. This is UI work only.

Success criteria:

- Every Android screen picks up the identity, in light and dark mode.
- Finishing the last habit of the day bursts confetti over the hero card, once per day.
- The milestone card shows and dismisses.
- With "Remove animations" on, there is no confetti or spring.
- The existing tests and the new ones pass.

## 2. Out of scope

- Shared Kotlin changes.
- A bundled rounded font.
- New features.
- Changes to the form sheet's structure. It only picks up the new colors.

## 3. Identity (Android)

**Theme:**

- `ui/theme/Theme.kt` defines `HabitColors` (canvas, card, hero, heroText, heroSubtext, heroTrack, flame, gold, accent). The light and dark values are the hex pairs from the iOS spec §3, plus accent `#3C3489`/`#AFA9EC`.
- `HabitColors` is exposed as `LocalHabitColors`, read through `HabitTheme.colors`.
- Material 3's `ColorScheme` is re-pointed at the tokens:
  - `primary` = accent
  - `background` = canvas
  - `surface` = card
  - `surfaceContainer` = card
  - The `on*` colors are chosen for contrast.
- Card radius is 20dp.

**Type:** Android has no system rounded face, so display numbers use `FontFamily.SansSerif` with `FontWeight.Black`: 44sp for the hero streak and 22sp for stat tiles.

**Today:**

- `HeroStreakCard` shows the date, the streak number, "day streak · best N" and a `Flame` at the streak's tier.
- Below that, a progress bar and either "N of M done today" or "All done today ✨" (gold), then `WeekComparisonLine(onHero = true)`.
- `MilestoneCard` appears under the hero when `milestone != null`.
- `HabitRow`:
  - A solid category icon circle (`CategoryIcon`).
  - Strikethrough and muted text when done.
  - The check button fills in the category color.
- `ProgressRing` and `StreakCard` are removed once unused.

**Other screens:**

- Calendar, Detail, Habits and Categories use the canvas background, with cards at 20dp.
- On Calendar and Detail, the month header and grid sit in one card.
- Stat tiles use the display numbers in their meaning colors.
- Streak bands are unchanged.
- The Detail screen's Delete button stays red.

**Labels the tests rely on** must survive:

- "N of M habits done" (on the hero progress bar)
- "Mark done: <name>" and "Mark not done: <name>"
- The tab labels, and the date text "Wednesday, 14 October" in tests

## 4. Motion and feedback (Android)

**Check-off:**

- The button scale springs 1 → 1.25 → 1 (`Animatable`, `spring(dampingRatio = 0.5f)`).
- The fill grows from the center, and the strikethrough fades in.
- Haptics: `HapticFeedbackType.ContextClick` per check-off, and `HapticFeedbackType.Confirm` when the done count reaches due.

**Hero:** the progress width animates, and the streak number uses `AnimatedContent` with a vertical slide.

**Confetti:**

- `Confetti(trigger: Int, colors)`: about 60 pieces over 1.8s on a `Canvas`, driven by `withFrameNanos`.
- Takes no touches (no pointer input), and is hidden from TalkBack (`clearAndSetSemantics {}`).
- Fired by `DashboardEffect.Celebrate`, which already increments a counter in `DashboardScreen`.

**Animations off** (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`, exposed as `animationsDisabled(scale: Float): Boolean`):

- No confetti; the hero shows a 0.4s gold outline pulse instead.
- No spring, opacity changes only.
- The streak number changes without sliding.

**TalkBack:** the milestone card reads "Milestone: <title>. <body>". The × reads "Dismiss milestone".

## 5. Testing

**JVM unit tests (`androidApp/src/test`):**

- `FlameTier.forStreak` boundaries: 0, 6, 7, 29, 30, 99, 100.
- `MilestoneCopy` titles and bodies.
- heroText and heroSubtext on hero reach at least 4.5:1, light and dark.
- `animationsDisabled(0f) == true`, `animationsDisabled(1f) == false`.

**Compose instrumented test:** `SmokeTest.completingOnlyHabitShowsAllDone` checks that after checking off the only habit, "All done today ✨" is displayed. The existing two tests keep passing.

**By hand (Pixel 8a):**

- Light and dark Today with the 7-day card.
- Dismissing the card.
- Confetti captured mid-flight.
- Calendar, Detail, Habits and Categories in both modes.
- "Remove animations" on: the gold pulse and no confetti.
