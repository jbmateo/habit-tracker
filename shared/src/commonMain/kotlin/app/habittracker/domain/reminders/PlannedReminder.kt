package app.habittracker.domain.reminders

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

const val REMINDER_WINDOW_DAYS = 7

/** iOS allows 64 pending notifications; stay safely below. */
const val MAX_PLANNED_REMINDERS = 60

/** One notification to deliver: [habitName]'s reminder on [date] at [time]. */
data class PlannedReminder(
    val habitId: String,
    val habitName: String,
    val iconKey: String,
    val date: LocalDate,
    val time: LocalTime,
    val streak: Int,
) {
    /** Stable per habit and day, so re-planning only adds and removes differences. */
    val id: String get() = "habit:$habitId@$date"

    val title: String get() = "${ReminderEmoji.forIconKey(iconKey)} $habitName"

    val body: String
        get() = if (streak > 0) "Time for your habit — keep your 🔥 $streak-day streak going!" else "Time for your habit"

    /** Changes whenever what the user would see changes, so schedulers can replace an outdated notification. */
    val fingerprint: String get() = "$time|$title|$body"
}

/** Emoji shown in reminder titles, one per habit icon key. */
object ReminderEmoji {
    private val byKey = mapOf(
        "run" to "🏃", "walk" to "🚶", "gym" to "🏋️", "bike" to "🚴", "swim" to "🏊", "yoga" to "🧘",
        "meditate" to "🧘", "water" to "💧", "nutrition" to "🥕", "food" to "🍽️", "sleep" to "😴", "pill" to "💊",
        "heart" to "❤️", "book" to "📖", "study" to "🎓", "code" to "💻", "language" to "🗣️", "journal" to "✍️",
        "music" to "🎵", "art" to "🎨", "pray" to "🙏", "sun" to "☀️", "clean" to "🧹", "money" to "💰",
        "no_phone" to "📵", "call" to "📞", "people" to "👥", "plant" to "🌱", "pet" to "🐾", "checklist" to "✅",
        "timer" to "⏱️", "target" to "🎯", "star" to "⭐", "smile" to "🙂", "no_smoking" to "🚭", "home" to "🏠",
    )

    fun forIconKey(key: String): String = byKey[key] ?: "✅"
}
