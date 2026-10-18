import SwiftUI
import Shared

@main
struct HabitTrackerApp: App {
    init() {
        // UI tests pass `-uiTestResetDB` to start from an empty database.
        KoinHelper.shared.start(resetDatabase: ProcessInfo.processInfo.arguments.contains("-uiTestResetDB"))
    }

    var body: some Scene {
        WindowGroup {
            RootTabView()
        }
    }
}
