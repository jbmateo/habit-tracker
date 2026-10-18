import Foundation
import Shared

/// "Every day" or Monday-first names joined with " · ", e.g. "Mon · Wed · Fri".
func scheduleSummary(_ days: Set<Kotlinx_datetimeDayOfWeek>) -> String {
    if days.count == 7 { return "Every day" }
    let names = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
    return days.map { Int($0.ordinal) }.sorted().map { names[$0] }.joined(separator: " · ")
}

/// " · ⏰ 7:00" in the device's time format, or "" when there is no reminder.
func reminderSuffix(_ time: Kotlinx_datetimeLocalTime?) -> String {
    guard let time, let date = Calendar.current.date(from: DateComponents(hour: Int(time.hour), minute: Int(time.minute))) else {
        return ""
    }
    return " · ⏰ " + date.formatted(date: .omitted, time: .shortened)
}
