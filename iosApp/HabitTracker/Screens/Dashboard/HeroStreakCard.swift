import Shared
import SwiftUI

/// Today's indigo hero: date, the perfect-day streak with its flame, today's progress and the week comparison.
/// Bumping `pulseTrigger` flashes a gold outline (the Reduce Motion stand-in for confetti).
struct HeroStreakCard: View {
    let dashboard: TodayDashboard
    let dateText: String
    let pulseTrigger: Int

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var pulse = false

    private var done: Int { Int(dashboard.done) }
    private var due: Int { Int(dashboard.due) }
    private var streak: Int { Int(dashboard.streak.current) }
    private var fraction: CGFloat { due == 0 ? 0 : CGFloat(done) / CGFloat(due) }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(dateText)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Theme.heroSubtext)

            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 0) {
                    Text("\(streak)")
                        .font(.system(size: 44, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .contentTransition(reduceMotion ? .identity : .numericText())
                        .foregroundStyle(Theme.heroText)
                    Text("day streak · best \(dashboard.streak.best)")
                        .font(.footnote)
                        .foregroundStyle(Theme.heroSubtext)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(streak) day streak, best \(dashboard.streak.best)")
                Spacer()
                FlameView(streak: streak)
            }

            if due > 0 {
                progressBar
                Text(dashboard.isPerfect ? "All done today ✨" : "\(done) of \(due) done today")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(dashboard.isPerfect ? Theme.gold : Theme.heroText)
            }

            WeekComparisonLine(week: dashboard.week, onHero: true)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: Theme.cornerRadius).fill(Theme.hero))
        .overlay(
            RoundedRectangle(cornerRadius: Theme.cornerRadius)
                .strokeBorder(Theme.gold, lineWidth: 3)
                .opacity(pulse ? 1 : 0)
        )
        .animation(reduceMotion ? .easeInOut(duration: 0.2) : .spring(response: 0.45, dampingFraction: 0.8), value: done)
        .onChange(of: pulseTrigger) {
            pulse = true
            withAnimation(.easeOut(duration: 0.4)) { pulse = false }
        }
    }

    /// Flame fill on the indigo track; gold once everything is done. Exposed to VoiceOver as "N of M habits done".
    private var progressBar: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(Theme.heroTrack)
                Capsule()
                    .fill(dashboard.isPerfect ? Theme.gold : Theme.flame)
                    .frame(width: max(fraction == 0 ? 0 : 10, proxy.size.width * fraction))
            }
        }
        .frame(height: 10)
        .accessibilityElement()
        .accessibilityLabel("\(done) of \(due) habits done")
    }
}
