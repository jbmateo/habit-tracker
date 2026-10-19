import SwiftUI

/// A habit's icon as a solid circle in its category color, with a glyph that stays readable on that color.
struct CategoryIcon: View {
    let iconKey: String
    let colorKey: String
    @ScaledMetric(relativeTo: .title3) var size: CGFloat = 34

    var body: some View {
        Image(systemName: HabitIcons.symbol(iconKey))
            .font(.system(size: size * 0.48, weight: .semibold))
            .foregroundStyle(Palette.onColor(colorKey))
            .frame(width: size, height: size)
            .background(Circle().fill(Palette.color(colorKey)))
            .accessibilityHidden(true)
    }
}

extension View {
    /// The redesign's card surface: `Theme.card` at the theme radius.
    func themeCard(padding: CGFloat = 12) -> some View {
        self.padding(padding)
            .background(Theme.card, in: RoundedRectangle(cornerRadius: Theme.cornerRadius))
    }
}
