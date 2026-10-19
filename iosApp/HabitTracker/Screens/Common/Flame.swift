import SwiftUI

/// How big the streak flame is drawn: it grows with the perfect-day streak (spec §3).
enum FlameTier: Equatable {
    case none, small, medium, large, legendary

    static func forStreak(_ streak: Int) -> FlameTier {
        switch streak {
        case ..<1: .none
        case 1..<7: .small
        case 7..<30: .medium
        case 30..<100: .large
        default: .legendary
        }
    }

    var pointSize: CGFloat {
        switch self {
        case .none, .small: 28
        case .medium: 36
        case .large, .legendary: 44
        }
    }
}

/// The streak flame: an outline with no streak, a filled flame otherwise, with a gold core from 100 days.
struct FlameView: View {
    let streak: Int

    var body: some View {
        let tier = FlameTier.forStreak(streak)
        ZStack {
            Image(systemName: tier == .none ? "flame" : "flame.fill")
                .font(.system(size: tier.pointSize))
                .foregroundStyle(tier == .none ? Theme.heroSubtext : Theme.flame)
            if tier == .legendary {
                Image(systemName: "flame.fill")
                    .font(.system(size: tier.pointSize * 0.5))
                    .foregroundStyle(Theme.gold)
                    .offset(y: tier.pointSize * 0.12)
            }
        }
        .accessibilityHidden(true)
    }
}

/// Copy for the milestone card (spec §5).
enum MilestoneCopy {
    static func title(_ milestone: Int) -> String { "\(milestone)-day streak" }

    static func body(_ milestone: Int) -> String {
        switch milestone {
        case 7: "One week strong"
        case 30: "A month of perfect days"
        case 100: "100 days. Legendary."
        default: "A whole year"
        }
    }
}
