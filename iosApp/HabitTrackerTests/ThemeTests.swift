import XCTest
@testable import HabitTracker

final class ThemeTests: XCTestCase {
    func testFlameTierBoundaries() {
        let expected: [(Int, FlameTier)] = [
            (0, .none), (6, .small), (7, .medium), (29, .medium), (30, .large), (99, .large), (100, .legendary),
        ]
        for (streak, tier) in expected {
            XCTAssertEqual(FlameTier.forStreak(streak), tier, "streak \(streak)")
        }
    }

    func testMilestoneCopy() {
        XCTAssertEqual(MilestoneCopy.title(7), "7-day streak")
        XCTAssertEqual(MilestoneCopy.title(30), "30-day streak")
        XCTAssertEqual(MilestoneCopy.title(100), "100-day streak")
        XCTAssertEqual(MilestoneCopy.title(365), "365-day streak")
        XCTAssertEqual(MilestoneCopy.body(7), "One week strong")
        XCTAssertEqual(MilestoneCopy.body(30), "A month of perfect days")
        XCTAssertEqual(MilestoneCopy.body(100), "100 days. Legendary.")
        XCTAssertEqual(MilestoneCopy.body(365), "A whole year")
    }

    func testHeroTextContrast() {
        for hero in [ThemeHex.hero.light, ThemeHex.hero.dark] {
            for text in [ThemeHex.heroText.light, ThemeHex.heroSubtext.light] {
                let ratio = Theme.contrastRatio(text, hero)
                XCTAssertGreaterThanOrEqual(ratio, 4.5, String(format: "text %06X on %06X: %.2f", text, hero, ratio))
            }
        }
    }
}
