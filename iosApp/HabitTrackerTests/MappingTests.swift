import SwiftUI
import UIKit
import XCTest
import Shared
@testable import HabitTracker

final class MappingTests: XCTestCase {
    func testEveryIconKeyMapsToLoadableSymbol() {
        XCTAssertFalse(IconKeys.shared.all.isEmpty)
        for key in IconKeys.shared.all {
            let symbol = HabitIcons.symbol(key)
            XCTAssertNotNil(UIImage(systemName: symbol), "\(key) -> \(symbol) is not a valid SF Symbol")
        }
    }

    func testEveryIconKeyMapsToItsOwnSymbolExceptFallback() {
        let fallback = HabitIcons.symbol("definitely-not-a-key")
        for key in IconKeys.shared.all where key != IconKeys.shared.FALLBACK {
            XCTAssertNotEqual(HabitIcons.symbol(key), fallback, "\(key) maps to the fallback symbol")
        }
    }

    func testUnknownIconFallsBackToChecklist() {
        XCTAssertEqual(HabitIcons.symbol("definitely-not-a-key"), "checklist")
    }

    func testEveryColorKeyHasDistinctLightAndDark() {
        XCTAssertFalse(ColorKeys.shared.all.isEmpty)
        for key in ColorKeys.shared.all {
            XCTAssertNotEqual(resolved(key, .light), resolved(key, .dark), "\(key) has identical light/dark colors")
        }
    }

    func testUnknownColorResolvesToGray() {
        XCTAssertEqual(resolved("mauve", .light), resolved("gray", .light))
        XCTAssertEqual(resolved("mauve", .dark), resolved("gray", .dark))
    }

    private func resolved(_ key: String, _ style: UIUserInterfaceStyle) -> [CGFloat] {
        let ui = UIColor(Palette.color(key)).resolvedColor(with: UITraitCollection(userInterfaceStyle: style))
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        ui.getRed(&r, green: &g, blue: &b, alpha: &a)
        return [r, g, b, a]
    }
}
