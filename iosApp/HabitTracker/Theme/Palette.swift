import SwiftUI
import UIKit
import Shared

/// Category colors (spec §5): light/dark hex pairs, resolved from a stored color key.
enum Palette {
    private static let pairs: [String: (light: UInt32, dark: UInt32)] = [
        "red": (0xE5484D, 0xFF6369),
        "orange": (0xF76B15, 0xFF8B3E),
        "amber": (0xFFB224, 0xFFCA16),
        "green": (0x30A46C, 0x3DD68C),
        "teal": (0x12A594, 0x0BD8B6),
        "blue": (0x0090FF, 0x70B8FF),
        "indigo": (0x3E63DD, 0x849DFF),
        "purple": (0x8E4EC6, 0xBF7AF0),
        "pink": (0xD6409F, 0xFF8DCC),
        "gray": (0x8B8D98, 0xB0B4BA),
    ]

    /// Dynamic light/dark color for [key]; unknown keys resolve to gray through the shared `ColorKeys`.
    static func color(_ key: String) -> Color {
        let pair = pairs[ColorKeys.shared.resolve(key: key)] ?? pairs["gray"]!
        return Color(UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? pair.dark : pair.light)
        })
    }
}

private extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}
