# UI redesign: personality and delight (iOS first)

Date: 2026-10-19 · Status: approved in conversation, awaiting spec review

## 1. Goal

Make the app more interesting to open every day, through two things:

- **Personality:** a bolder visual identity, built around a "bold hero" Today screen.
- **Delight:** the app reacts when you check things off, finish the day, or reach a streak milestone.

This pass covers the iPhone app (SwiftUI). The Android app keeps its current look; a later pass brings it in line. The celebration rules live in shared Kotlin, so that pass can reuse them.

Success criteria:

- Every screen picks up the new identity, in light and dark mode.
- Finishing the last habit of the day bursts confetti over the hero card, once per day.
- A streak milestone (7, 30, 100, 365 days) shows a one-time card on Today until you dismiss it.
- All motion respects Reduce Motion.
- Android builds and its tests stay green with no visual change.

## 2. Out of scope

- Android visuals (only the shared ViewModel changes under it).
- Per-habit milestones, sounds, a year heat map, and new statistics.
- Changes to navigation or features.

## 3. Visual identity (iOS)

**Theme tokens.** A new `iosApp/HabitTracker/Theme/Theme.swift` sits next to `Palette.swift`. Each token is a light/dark pair:

| Token | Light | Dark | Use |
|---|---|---|---|
| canvas | `#F4F3F8` | `#13121C` | Screen background |
| card | `#FFFFFF` | `#1E1C2B` | Rows, tiles, sections |
| hero | `#3C3489` | `#2A2466` | Today streak card |
| heroText | `#EEEDFE` | `#EEEDFE` | Primary text on hero |
| heroSubtext | `#CECBF6` | `#CECBF6` | Secondary text on hero |
| heroTrack | `#534AB7` | `#3C3489` | Progress bar track |
| flame | `#EF9F27` | `#EF9F27` | Flame, progress fill |
| gold | `#FFB224` | `#FFCA16` | All done, milestone |

- Cards use a 20pt corner radius with no border. Category colors (`Palette`) stay as they are.
- Contrast: heroText and heroSubtext on hero must reach WCAG 4.5:1 in both modes. A unit test enforces this.

**Type.** SF Rounded (`.fontDesign(.rounded)`) for display numbers and titles: 44pt for the hero streak and 22pt for screen titles. Body text stays in the default SF font.

**Today (direction B):**

1. **Hero streak card** (replaces the current `StreakCard` and `ProgressRing` header):
   - Date, a big streak number with "day streak", and a flame sized by tier (§3 Flame tiers).
   - A progress bar plus "N of M done today".
   - The week-over-week line, in heroSubtext.
2. **Milestone card**, when present (§4.2).
3. **Habit rows** on card surfaces:
   - A solid icon circle in the category color with an `onColor` glyph.
   - Name, then the streak count.
   - A check button that fills in the category color when done. Done rows get strikethrough text.
   - Sorting is unchanged.

**Flame tiers** (the overall perfect-day streak):

| Streak | Tier |
|---|---|
| 0 | Muted outline |
| 1–6 | Small (28pt) |
| 7–29 | Medium (36pt) |
| 30–99 | Large (44pt) |
| 100+ | Large with a gold inner glow layer |

**Other screens.** Calendar, Habit detail, Habits and Categories adopt the canvas, cards, rounded display numbers and solid category icon circles:

- Stat tiles keep their meaning colors (orange, purple, completion green/amber/red/gray) in the new card style.
- Calendar streak bands are unchanged.
- The tab bar tint becomes hero indigo.

## 4. Celebration rules (shared Kotlin)

### 4.1 All done (confetti)

`DashboardEffect.Celebrate` already exists and fires when today goes from not perfect to perfect within one subscription. This pass changes it in two ways:

- The "already celebrated today" date moves from memory to the preferences store (key `celebrated_on`, ISO date). Reopening the app, or un-checking and re-checking, never fires a second celebration on the same day.
- The rules that it never fires on first load and only fires for today stay as they are.

### 4.2 Milestones

`DashboardUiState.Content` gains `milestone: Int?`, one of 7, 30, 100 or 365.

- A **run** is the current perfect-day streak, identified by its start date (the first day of the streak).
- `milestone` is the highest of {7, 30, 100, 365} that is ≤ the current streak and not yet dismissed for this run. When there isn't one, it's `null`.
- The new `DashboardEvent.DismissMilestone` stores `<runStart>:<milestone>` under key `milestone_seen`. Any milestone ≤ that value counts as dismissed for that run.
- A new run (a different start date) starts with nothing dismissed. Rebuilding a broken streak to 7 shows the 7-day card again.
- On first launch after this change, a user with an existing streak sees its highest milestone once (the sample data's 8-day streak shows 7).

### 4.3 Storage

`PreferencesRepository` gains two string preferences, backed by the existing Preference table, so no migration is needed:

- `observeCelebratedOn` / `setCelebratedOn`
- `observeMilestoneSeen` / `setMilestoneSeen`

Writes go through use cases, the same pattern as `SetTodaySort`.

## 5. Motion and feedback (iOS)

**Check-off:**

- The button springs (scale 1.25 → 1, about 0.35s) and the fill grows from the center.
- Strikethrough fades in.
- Haptics: `.sensoryFeedback(.impact(weight: .light))`, or `.success` when the check completes the day.
- Un-checking reverses the animation, with no haptic.

**Hero card:** the progress bar animates its width, and the streak number uses `.contentTransition(.numericText())`.

**Confetti:**

- About 60 pieces in hero, flame, gold and category colors, from the hero card, for 1.8s.
- Drawn with `Canvas` and `TimelineView`, with no dependencies.
- `allowsHitTesting(false)`; stops when the view disappears.
- Fired by `Celebrate`.

**All done:** the progress fill turns gold, and the subtitle reads "All done today ✨" for the rest of the day (derived from `isPerfect`).

**Milestone card:**

- Gold accents on hero indigo, a flame badge at the milestone's tier, a title and a body line.
- An × dismisses it with a fade.
- Copy:

| Milestone | Title | Body |
|---|---|---|
| 7 | 7-day streak | One week strong |
| 30 | 30-day streak | A month of perfect days |
| 100 | 100-day streak | 100 days. Legendary. |
| 365 | 365-day streak | A whole year |

**Reduce Motion:**

- No confetti; the hero does a 0.4s gold pulse instead.
- No spring scale, only opacity fades.
- Numbers change without rolling.

**VoiceOver:** the milestone card reads "Milestone: 30-day streak. A month of perfect days." The × reads "Dismiss milestone".

## 6. Android

The Android app keeps its current UI:

- It already handles `Celebrate` with a progress-ring pulse, which is unchanged.
- It ignores the new `milestone` field.
- `DismissMilestone` has no caller there yet.

## 7. Testing

**Shared (`commonTest`, `DashboardViewModelTest`):**

- celebrateFiresOnLastCheckOff
- celebrateNotOnLoad
- celebrateOncePerDayAcrossReopen (stored date)
- milestoneAtSeven
- highestMilestoneWins
- dismissedMilestoneStaysHidden
- rebuiltRunShowsMilestoneAgain
- noMilestoneBelowSeven

Repository round trips for both new preferences go in the existing repository tests.

**iOS unit (`HabitTrackerTests`):**

- `flameTier` boundaries: 0, 6, 7, 29, 30, 99, 100.
- Milestone copy for each value.
- Hero text contrast in both modes.

**iOS UI (`SmokeUITests`):** checking off the only habit shows "All done today". Existing tests are updated for moved labels.

**Android:** the existing unit and instrumented suites stay green.

**By hand (iPhone 17 Pro):**

- Light and dark screenshots of Today, Calendar, Detail and Habits.
- Confetti on the last check-off.
- The 7-day milestone card with the sample data.
- Reduce Motion on.
