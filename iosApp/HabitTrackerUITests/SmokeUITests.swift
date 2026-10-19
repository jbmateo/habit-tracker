import XCTest

/// Every test launches on a fresh database (`-uiTestResetDB` deletes `habits.db` before Koin starts).
final class SmokeUITests: XCTestCase {
    override func setUp() {
        continueAfterFailure = false
    }

    @MainActor
    private func launchFresh() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestResetDB", "-uiTestToday", "2026-10-14"]
        app.launch()
        return app
    }

    @MainActor
    private func createHabit(named name: String, in app: XCUIApplication, via button: String) {
        let add = app.buttons[button]
        XCTAssertTrue(add.waitForExistence(timeout: 10), "\(button) button missing")
        add.tap()
        let field = app.textFields["Name"]
        XCTAssertTrue(field.waitForExistence(timeout: 10), "form name field missing")
        field.tap()
        field.typeText(name)
        app.buttons["Save"].tap()
        XCTAssertTrue(field.waitForNonExistence(timeout: 10), "form sheet did not dismiss")
    }

    @MainActor
    func testCreateHabitThenToggleUpdatesProgress() {
        let app = launchFresh()
        createHabit(named: "Read", in: app, via: "Create your first habit")

        let check = app.buttons["Mark done: Read"]
        XCTAssertTrue(check.waitForExistence(timeout: 10))
        XCTAssertTrue(app.descendants(matching: .any)["0 of 1 habits done"].exists)

        check.tap()

        // The ring draws "1/1" and exposes it as one accessibility element labelled "1 of 1 habits done".
        XCTAssertTrue(app.descendants(matching: .any)["1 of 1 habits done"].waitForExistence(timeout: 10))
        // "Today" is pinned by -uiTestToday, so the header never depends on when the test runs.
        XCTAssertTrue(app.staticTexts["Wednesday, October 14"].exists)
        XCTAssertTrue(app.buttons["Mark not done: Read"].exists)
    }

    @MainActor
    func testCompletingOnlyHabitShowsAllDone() {
        let app = launchFresh()
        createHabit(named: "Read", in: app, via: "Create your first habit")

        let check = app.buttons["Mark done: Read"]
        XCTAssertTrue(check.waitForExistence(timeout: 10))
        check.tap()

        XCTAssertTrue(app.staticTexts["All done today ✨"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testArchiveThenDeleteFromDetail() {
        let app = launchFresh()
        createHabit(named: "Stretch", in: app, via: "Create your first habit")

        app.buttons["Habits"].tap()
        let row = app.buttons["Stretch"]
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        row.tap()

        let archive = app.buttons["Archive"]
        XCTAssertTrue(archive.waitForExistence(timeout: 10))
        archive.tap()
        XCTAssertTrue(app.buttons["Unarchive"].waitForExistence(timeout: 10))

        app.buttons["Delete"].tap()
        XCTAssertTrue(app.staticTexts["This erases all history for this habit."].waitForExistence(timeout: 10))
        app.alerts.buttons["Delete"].tap()

        // Deleting closes the detail screen; the habit list is empty again.
        XCTAssertTrue(app.buttons["Add habit"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons["Stretch"].exists)
    }

    /// Regression: leading blanks and day cells once shared grid ids, hiding days 1..<leadingBlanks.
    @MainActor
    func testCalendarShowsFirstDayOfMonth() {
        let app = launchFresh()
        app.buttons["Calendar"].tap()
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Day 1,'")).firstMatch
            .waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Day 3,'")).firstMatch.exists)
    }

    @MainActor
    func testReminderToggleShowsTimePicker() {
        let app = launchFresh()
        let add = app.buttons["Create your first habit"]
        XCTAssertTrue(add.waitForExistence(timeout: 10))
        add.tap()
        let toggle = app.switches["Remind me"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 10), "reminder toggle missing")
        XCTAssertFalse(app.datePickers.firstMatch.exists)
        toggle.switches.firstMatch.swipeRight()
        // First use asks for notification permission; accept it if the system alert appears.
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let allow = springboard.buttons["Allow"]
        if allow.waitForExistence(timeout: 5) { allow.tap() }
        // The hint only shows once the reminder is on.
        XCTAssertTrue(
            app.staticTexts["Only on scheduled days. Skipped if you've already done it."].waitForExistence(timeout: 10),
            "reminder did not switch on"
        )
        XCTAssertTrue(app.datePickers.firstMatch.exists, "time picker missing\n\(app.debugDescription)")
    }
}
