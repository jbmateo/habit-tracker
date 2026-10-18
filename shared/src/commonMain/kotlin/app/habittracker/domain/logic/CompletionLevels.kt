package app.habittracker.domain.logic

import app.habittracker.domain.model.CompletionLevel
import kotlin.math.roundToInt

/** Level for a completion [rate] (0.0..1.0), judged on the rounded percent shown to the user; `null` without data. */
fun completionLevel(rate: Double?): CompletionLevel? {
    val percent = rate?.let { (it * 100).roundToInt() } ?: return null
    return when {
        percent >= 80 -> CompletionLevel.Good
        percent >= 50 -> CompletionLevel.Fair
        else -> CompletionLevel.Low
    }
}
