import SwiftUI

/// A one-time streak milestone (7, 30, 100, 365 days) shown under the hero until dismissed.
struct MilestoneCard: View {
    let milestone: Int
    let onDismiss: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            FlameView(streak: milestone)
            VStack(alignment: .leading, spacing: 2) {
                Text(MilestoneCopy.title(milestone))
                    .font(.system(.headline, design: .rounded))
                    .foregroundStyle(Theme.gold)
                Text(MilestoneCopy.body(milestone))
                    .font(.subheadline)
                    .foregroundStyle(Theme.heroText)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Milestone: \(MilestoneCopy.title(milestone)). \(MilestoneCopy.body(milestone))")
            Spacer(minLength: 8)
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.footnote.weight(.bold))
                    .foregroundStyle(Theme.heroSubtext)
                    .frame(width: 32, height: 32)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Dismiss milestone")
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: Theme.cornerRadius).fill(Theme.hero))
        .overlay(RoundedRectangle(cornerRadius: Theme.cornerRadius).strokeBorder(Theme.gold, lineWidth: 1.5))
    }
}
