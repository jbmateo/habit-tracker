import SwiftUI
import Shared

struct HabitRow: View {
    let item: TodayHabitItem
    let onToggle: () -> Void
    var onOpen: (() -> Void)?

    private var color: Color { Palette.color(item.habit.category.colorKey) }

    var body: some View {
        HStack(spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: HabitIcons.symbol(item.habit.iconKey))
                    .font(.title3)
                    .foregroundStyle(color)
                    .frame(width: 28)
                Text(item.habit.name).font(.body)
                Spacer(minLength: 8)
                if item.streak > 0 {
                    Text("🔥 \(item.streak)").font(.subheadline.weight(.medium)).monospacedDigit()
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { onOpen?() }

            Button(action: onToggle) {
                Image(systemName: "checkmark")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(item.isDone ? Color.white : Color.secondary)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(item.isDone ? color : Color.clear))
                    .overlay(Circle().strokeBorder(item.isDone ? color : Color.secondary.opacity(0.4), lineWidth: 1.5))
            }
            .buttonStyle(.plain)
            .accessibilityLabel((item.isDone ? "Mark not done: " : "Mark done: ") + item.habit.name)
        }
        .padding(.vertical, 2)
    }
}
