import SwiftUI
import Shared

@main
struct HabitTrackerApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    init() {
        #if DEBUG
        // UI tests pass `-uiTestResetDB` to start from an empty database and `-uiTestToday <yyyy-MM-dd>`
        // to pin "today" (Debug builds only).
        let arguments = ProcessInfo.processInfo.arguments
        let reset = arguments.contains("-uiTestResetDB")
        let fixedToday = arguments.firstIndex(of: "-uiTestToday").flatMap { i in
            arguments.indices.contains(i + 1) ? arguments[i + 1] : nil
        }
        #else
        let reset = false
        let fixedToday: String? = nil
        #endif
        KoinHelper.shared.start(resetDatabase: reset, fixedToday: fixedToday)
    }

    var body: some Scene {
        WindowGroup {
            RootTabView()
        }
        .onChange(of: scenePhase) { _, phase in
            // Back from Settings with notifications turned on: requests added while they were off were dropped.
            if phase == .active { Task { try? await KoinHelper.shared.syncReminders() } }
        }
    }
}
