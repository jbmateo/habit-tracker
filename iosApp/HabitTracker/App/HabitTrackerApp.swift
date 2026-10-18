import SwiftUI
import Shared

@main
struct HabitTrackerApp: App {
    init() {
        KoinHelper.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            RootTabView()
        }
    }
}
