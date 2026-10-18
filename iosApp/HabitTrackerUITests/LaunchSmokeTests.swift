import XCTest

final class LaunchSmokeTests: XCTestCase {
    @MainActor
    func testLaunchShowsThreeTabs() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.tabBars.buttons["Today"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.tabBars.buttons["Calendar"].exists)
        XCTAssertTrue(app.tabBars.buttons["Habits"].exists)
    }
}
