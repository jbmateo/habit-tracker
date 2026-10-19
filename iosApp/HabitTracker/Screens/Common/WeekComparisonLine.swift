import Shared
import SwiftUI

/// "This week 86% · Last week 71%  ↑ 15 pts"; hidden when this week has no data yet.
struct WeekComparisonLine: View {
    let week: WeekComparison
    /// Drawn on the indigo hero card: light text, gold for an improvement.
    var onHero = false

    var body: some View {
        if let thisWeek = percent(week.thisWeek) {
            let lastWeek = percent(week.lastWeek)
            let summary = "This week \(thisWeek)%" + (lastWeek.map { " · Last week \($0)%" } ?? "")
            HStack(spacing: 8) {
                Text(summary).foregroundStyle(onHero ? Theme.heroSubtext : Color.secondary)
                if let lastWeek { trend(thisWeek - lastWeek) }
            }
            .font(.subheadline)
            .accessibilityElement(children: .combine)
        }
    }

    @ViewBuilder
    private func trend(_ delta: Int) -> some View {
        if delta > 0 {
            Text("↑ \(delta) pts").fontWeight(.semibold).foregroundStyle(onHero ? Theme.gold : Color.green)
        } else if delta < 0 {
            Text("↓ \(-delta) pts").fontWeight(.semibold).foregroundStyle(onHero ? Theme.heroText : Color.red)
        } else {
            Text("→ same").fontWeight(.semibold).foregroundStyle(onHero ? Theme.heroSubtext : Color.secondary)
        }
    }

    private func percent(_ rate: KotlinDouble?) -> Int? {
        rate.map { Int(($0.doubleValue * 100).rounded()) }
    }
}
