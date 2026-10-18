import XCTest
import Shared
@testable import HabitTracker

final class GridAndContrastTests: XCTestCase {
    /// October 2026 starts on a Thursday: 3 blanks, 31 days. Every cell needs a distinct identity.
    func testMonthGridCellsHaveUniqueIdsAndCoverBlanksAndDays() {
        let cells = MonthGridCell.build(leadingBlanks: 3, itemCount: 31)
        XCTAssertEqual(cells.count, 34)
        XCTAssertEqual(Set(cells.map(\.id)).count, 34)
        XCTAssertEqual(cells.filter { if case .blank = $0 { true } else { false } }.count, 3)
        XCTAssertEqual(cells.compactMap { if case .day(let i) = $0 { i } else { nil } }, Array(0..<31))
    }

    func testOnColorContrastsWithEveryFillInBothAppearances() {
        for key in ColorKeys.shared.all {
            for dark in [false, true] {
                let ratio = Palette.contrastRatio(forKey: key, dark: dark)
                XCTAssertGreaterThanOrEqual(ratio, 3, "\(key) dark=\(dark) contrast \(ratio)")
            }
        }
    }
}
