import SwiftUI
import UIKit

/// Raw light/dark hex pairs behind `Theme` (spec §3), kept separate so tests can check their contrast.
enum ThemeHex {
    typealias Pair = (light: UInt32, dark: UInt32)
    static let canvas: Pair = (0xF4F3F8, 0x13121C)
    static let card: Pair = (0xFFFFFF, 0x1E1C2B)
    static let hero: Pair = (0x3C3489, 0x2A2466)
    static let heroText: Pair = (0xEEEDFE, 0xEEEDFE)
    static let heroSubtext: Pair = (0xCECBF6, 0xCECBF6)
    static let heroTrack: Pair = (0x534AB7, 0x3C3489)
    static let flame: Pair = (0xEF9F27, 0xEF9F27)
    static let gold: Pair = (0xFFB224, 0xFFCA16)
    /// Tint for controls on the canvas: hero indigo, lifted in dark mode so it stays legible on dark bars.
    static let accent: Pair = (0x3C3489, 0xAFA9EC)
}

/// The app's visual identity: a cool canvas, white cards and a deep indigo hero, in light and dark.
enum Theme {
    static let canvas = dynamic(ThemeHex.canvas)
    static let card = dynamic(ThemeHex.card)
    static let hero = dynamic(ThemeHex.hero)
    static let heroText = dynamic(ThemeHex.heroText)
    static let heroSubtext = dynamic(ThemeHex.heroSubtext)
    static let heroTrack = dynamic(ThemeHex.heroTrack)
    static let flame = dynamic(ThemeHex.flame)
    static let gold = dynamic(ThemeHex.gold)
    static let accent = dynamic(ThemeHex.accent)

    static let cornerRadius: CGFloat = 20

    private static func dynamic(_ pair: ThemeHex.Pair) -> Color {
        Color(UIColor { traits in UIColor(hex: traits.userInterfaceStyle == .dark ? pair.dark : pair.light) })
    }

    /// WCAG contrast ratio between two sRGB hex colors.
    static func contrastRatio(_ a: UInt32, _ b: UInt32) -> Double {
        let la = luminance(a), lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /// WCAG relative luminance of an sRGB hex color.
    static func luminance(_ hex: UInt32) -> Double {
        func channel(_ shift: UInt32) -> Double {
            let c = Double((hex >> shift) & 0xFF) / 255
            return c <= 0.03928 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
