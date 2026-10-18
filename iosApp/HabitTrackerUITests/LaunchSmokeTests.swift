import XCTest

final class LaunchSmokeTests: XCTestCase {
    @MainActor
    func testLaunchShowsThreeTabs() {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestResetDB", "-uiTestToday", "2026-10-14"]
        app.launch()
        // On this device class the TabView renders as a floating pill rather than a tab bar, so query buttons.
        XCTAssertTrue(app.buttons["Today"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["Calendar"].exists)
        XCTAssertTrue(app.buttons["Habits"].exists)
    }
}
