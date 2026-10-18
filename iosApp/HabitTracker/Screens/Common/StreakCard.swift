import SwiftUI
import Shared

struct StreakCard: View {
    let streak: StreakInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("🔥 \(streak.current)").font(.title2.bold())
            Text("day streak").font(.footnote)
            Text("Best \(streak.best)").font(.footnote).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.fill.tertiary, in: RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }
}
