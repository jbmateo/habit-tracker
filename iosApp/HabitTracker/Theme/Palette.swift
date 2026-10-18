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

    /// Readable foreground (black or white) for text or glyphs drawn on `color(key)`, chosen per appearance by
    /// whichever has the higher WCAG contrast against that fill.
    static func onColor(_ key: String) -> Color {
        let pair = pairs[ColorKeys.shared.resolve(key: key)] ?? pairs["gray"]!
        return Color(UIColor { traits in
            let fill = traits.userInterfaceStyle == .dark ? pair.dark : pair.light
            return onIsBlack(fill) ? .black : .white
        })
    }

    /// WCAG contrast ratio between the fill for [key] and its `onColor`, for the given appearance.
    static func contrastRatio(forKey key: String, dark: Bool) -> Double {
        let pair = pairs[ColorKeys.shared.resolve(key: key)] ?? pairs["gray"]!
        let fill = luminance(dark ? pair.dark : pair.light)
        let on = onIsBlack(dark ? pair.dark : pair.light) ? 0.0 : 1.0
        return (max(fill, on) + 0.05) / (min(fill, on) + 0.05)
    }

    private static func onIsBlack(_ hex: UInt32) -> Bool {
        let fill = luminance(hex)
        let againstBlack = (fill + 0.05) / 0.05
        let againstWhite = 1.05 / (fill + 0.05)
        return againstBlack >= againstWhite
    }

    /// WCAG relative luminance of an sRGB hex color.
    private static func luminance(_ hex: UInt32) -> Double {
        func channel(_ shift: UInt32) -> Double {
            let c = Double((hex >> shift) & 0xFF) / 255
            return c <= 0.03928 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
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
