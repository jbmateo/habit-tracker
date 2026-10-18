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

    @ScaledMetric(relativeTo: .title3) private var iconWidth = 28
    @ScaledMetric(relativeTo: .body) private var checkSize = 36

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
                    .foregroundStyle(isDone ? Color.white : Color.secondary)
                    .frame(width: checkSize, height: checkSize)
                    .background(Circle().fill(isDone ? color : Color.clear))
                    .overlay(Circle().strokeBorder(isDone ? color : Color.secondary.opacity(0.4), lineWidth: 1.5))
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .disabled(!toggleEnabled)
            .opacity(toggleEnabled ? 1 : 0.4)
            .accessibilityLabel((isDone ? "Mark not done: " : "Mark done: ") + habit.name)
        }
        .padding(.vertical, 2)
    }

    private var label: some View {
        HStack(spacing: 12) {
            Image(systemName: HabitIcons.symbol(habit.iconKey))
                .font(.title3)
                .foregroundStyle(color)
                .frame(width: iconWidth)
            VStack(alignment: .leading, spacing: 2) {
                Text(habit.name).font(.body)
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
