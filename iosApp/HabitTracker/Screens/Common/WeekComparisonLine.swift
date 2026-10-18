import Shared
import SwiftUI

/// "This week 86% · Last week 71%  ↑ 15 pts"; hidden when this week has no data yet.
struct WeekComparisonLine: View {
    let week: WeekComparison

    var body: some View {
        if let thisWeek = percent(week.thisWeek) {
            let lastWeek = percent(week.lastWeek)
            let summary = "This week \(thisWeek)%" + (lastWeek.map { " · Last week \($0)%" } ?? "")
            HStack(spacing: 8) {
                Text(summary).foregroundStyle(.secondary)
                if let lastWeek { trend(thisWeek - lastWeek) }
            }
            .font(.subheadline)
            .accessibilityElement(children: .combine)
        }
    }

    @ViewBuilder
    private func trend(_ delta: Int) -> some View {
        if delta > 0 {
            Text("↑ \(delta) pts").fontWeight(.semibold).foregroundStyle(.green)
        } else if delta < 0 {
            Text("↓ \(-delta) pts").fontWeight(.semibold).foregroundStyle(.red)
        } else {
            Text("→ same").fontWeight(.semibold).foregroundStyle(.secondary)
        }
    }

    private func percent(_ rate: KotlinDouble?) -> Int? {
        rate.map { Int(($0.doubleValue * 100).rounded()) }
    }
}
