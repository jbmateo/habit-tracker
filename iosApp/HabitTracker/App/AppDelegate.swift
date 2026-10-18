import Shared
import UIKit
import UserNotifications

/// Receives reminder notifications: "Mark done" checks the habit off without opening the app.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let info = response.notification.request.content.userInfo
        guard response.actionIdentifier == "MARK_DONE",
              let habitId = info["habitId"] as? String,
              let date = info["date"] as? String else {
            completionHandler()
            return
        }
        let name = info["habitName"] as? String ?? "habit"
        nonisolated(unsafe) let done = completionHandler
        Task { @MainActor in
            KoinHelper.shared.start(resetDatabase: false, fixedToday: nil)
            _ = try? await IosReminders.shared.markDone(habitId: habitId, isoDate: date, habitName: name)
            done()
        }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}
