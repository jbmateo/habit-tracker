package app.habittracker.domain.model

object IconKeys {
    const val FALLBACK = "checklist"

    val all: List<String> = listOf(
        "run", "walk", "gym", "bike", "swim", "yoga", "meditate", "water", "nutrition", "food",
        "sleep", "pill", "heart", "book", "study", "code", "language", "journal", "music", "art",
        "pray", "sun", "clean", "money", "no_phone", "call", "people", "plant", "pet", "checklist",
        "timer", "target", "star", "smile", "no_smoking", "home",
    )

    fun resolve(key: String): String = if (key in all) key else FALLBACK
}
