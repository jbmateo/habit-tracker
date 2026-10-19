import SwiftUI
import Shared

/// One habit: icon, name, optional streak, and a check button. The check always toggles; the rest of the row is a
/// separate button that opens the habit when [onOpen] is given.
struct HabitRow: View {
    let habit: Habit_
    let isDone: Bool
    var streak = 0
    /// Secondary text under the name (e.g. "Done" / "Missed" in the calendar's day panel).
    var status: String?
    var toggleEnabled = true
    let onToggle: () -> Void
    var onOpen: (() -> Void)?

    @ScaledMetric(relativeTo: .title3) private var iconSize = 34
    @ScaledMetric(relativeTo: .body) private var checkSize = 36
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    /// Bumped on each check-off to play the spring; un-checking never bounces.
    @State private var bounces = 0

    private var color: Color { Palette.color(habit.category.colorKey) }

    var body: some View {
        HStack(spacing: 12) {
            if let onOpen {
                Button(action: onOpen) { label }
                    .buttonStyle(.plain)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(habit.name)
                    .accessibilityValue(accessibilityValue)
                    .accessibilityAddTraits(.isButton)
            } else {
                label
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(habit.name)
                    .accessibilityValue(accessibilityValue)
            }

            Button(action: onToggle) {
                Image(systemName: "checkmark")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(isDone ? Palette.onColor(habit.category.colorKey) : Color.secondary)
                    .frame(width: checkSize, height: checkSize)
                    // The fill grows out from the centre (a plain fade with Reduce Motion on).
                    .background(
                        Circle().fill(color)
                            .scaleEffect(isDone || reduceMotion ? 1 : 0.01)
                            .opacity(isDone ? 1 : 0)
                    )
                    .overlay(Circle().strokeBorder(isDone ? color : Color.secondary.opacity(0.4), lineWidth: 1.5))
                    .contentShape(Circle())
                    .keyframeAnimator(initialValue: 1.0, trigger: bounces) { content, scale in
                        content.scaleEffect(scale)
                    } keyframes: { _ in
                        SpringKeyframe(1.25, duration: 0.12)
                        SpringKeyframe(1.0, duration: 0.23, spring: .bouncy)
                    }
                    .animation(reduceMotion ? .easeInOut(duration: 0.2) : .spring(response: 0.3, dampingFraction: 0.7), value: isDone)
            }
            .buttonStyle(.plain)
            .disabled(!toggleEnabled)
            .opacity(toggleEnabled ? 1 : 0.4)
            .accessibilityLabel((isDone ? "Mark not done: " : "Mark done: ") + habit.name)
        }
        .padding(.vertical, 2)
        .onChange(of: isDone) { _, done in
            if done && !reduceMotion { bounces += 1 }
        }
    }

    private var label: some View {
        HStack(spacing: 12) {
            Image(systemName: HabitIcons.symbol(habit.iconKey))
                .font(.body.weight(.semibold))
                .foregroundStyle(Palette.onColor(habit.category.colorKey))
                .frame(width: iconSize, height: iconSize)
                .background(Circle().fill(color))
            VStack(alignment: .leading, spacing: 2) {
                Text(habit.name)
                    .font(.body)
                    .strikethrough(isDone && status == nil, color: .secondary)
                    .foregroundStyle(isDone && status == nil ? Color.secondary : Color.primary)
                    .animation(.easeInOut(duration: 0.2), value: isDone)
                if let status {
                    Text(status).font(.footnote).foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 8)
            if streak > 0 {
                Text("🔥 \(streak)").font(.subheadline.weight(.medium)).monospacedDigit()
            }
        }
        .contentShape(Rectangle())
    }

    private var accessibilityValue: String {
        [status, streak > 0 ? "\(streak) day streak" : nil].compactMap { $0 }.joined(separator: ", ")
    }
}
