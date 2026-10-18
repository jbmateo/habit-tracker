package app.habittracker.domain.model

object ColorKeys {
    const val FALLBACK = "gray"

    val all: List<String> = listOf(
        "red", "orange", "amber", "green", "teal", "blue", "indigo", "purple", "pink", "gray",
    )

    fun resolve(key: String): String = if (key in all) key else FALLBACK
}
