import XCTest

/// Every test launches on a fresh database (`-uiTestResetDB` deletes `habits.db` before Koin starts).
final class SmokeUITests: XCTestCase {
    override func setUp() {
        continueAfterFailure = false
    }

    @MainActor
    private func launchFresh() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestResetDB"]
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
        XCTAssertTrue(app.buttons["Mark not done: Read"].exists)
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
}
