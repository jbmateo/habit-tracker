import SwiftUI
import Shared

@main
struct HabitTrackerApp: App {
    init() {
        #if DEBUG
        // UI tests pass `-uiTestResetDB` to start from an empty database (Debug builds only).
        let reset = ProcessInfo.processInfo.arguments.contains("-uiTestResetDB")
        #else
        let reset = false
        #endif
        KoinHelper.shared.start(resetDatabase: reset)
    }

    var body: some Scene {
        WindowGroup {
            RootTabView()
        }
    }
}
