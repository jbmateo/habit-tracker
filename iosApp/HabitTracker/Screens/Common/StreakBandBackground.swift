import Shared
import SwiftUI

/// Tinted band behind consecutive streak days on a month grid, in the palette color `colorKey`;
/// rounded only on the band's outer ends. Use as the background of a full-size grid cell.
struct StreakBandBackground: View {
    let band: StreakBand
    let colorKey: String
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        if band != .none {
            GeometryReader { geo in
                let radius = (geo.size.height - 6) / 2
                let roundStart = band == .single || band == .start
                let roundEnd = band == .single || band == .end
                UnevenRoundedRectangle(
                    topLeadingRadius: roundStart ? radius : 0,
                    bottomLeadingRadius: roundStart ? radius : 0,
                    bottomTrailingRadius: roundEnd ? radius : 0,
                    topTrailingRadius: roundEnd ? radius : 0
                )
                .fill(Palette.color(colorKey).opacity(colorScheme == .dark ? 0.55 : 0.25))
                .padding(.vertical, 3)
                .padding(.leading, roundStart ? 3 : 0)
                .padding(.trailing, roundEnd ? 3 : 0)
            }
        }
    }
}
