import Shared

/// "Every day" or Monday-first names joined with " · ", e.g. "Mon · Wed · Fri".
func scheduleSummary(_ days: Set<Kotlinx_datetimeDayOfWeek>) -> String {
    if days.count == 7 { return "Every day" }
    let names = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
    return days.map { Int($0.ordinal) }.sorted().map { names[$0] }.joined(separator: " · ")
}
