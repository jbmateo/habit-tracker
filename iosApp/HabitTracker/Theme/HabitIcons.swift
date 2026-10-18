import Shared

/// Habit icon keys (spec §5) mapped to SF Symbols.
enum HabitIcons {
    private static let symbols: [String: String] = [
        "run": "figure.run",
        "walk": "figure.walk",
        "gym": "dumbbell.fill",
        "bike": "bicycle",
        "swim": "figure.pool.swim",
        "yoga": "figure.yoga",
        "meditate": "figure.mind.and.body",
        "water": "drop.fill",
        "nutrition": "carrot.fill",
        "food": "fork.knife",
        "sleep": "bed.double.fill",
        "pill": "pills.fill",
        "heart": "heart.fill",
        "book": "book.fill",
        "study": "graduationcap.fill",
        "code": "chevron.left.forwardslash.chevron.right",
        "language": "character.bubble.fill",
        "journal": "square.and.pencil",
        "music": "music.note",
        "art": "paintbrush.fill",
        "pray": "hands.sparkles.fill",
        "sun": "sun.max.fill",
        "clean": "sparkles",
        "money": "dollarsign.circle.fill",
        "no_phone": "iphone.slash",
        "call": "phone.fill",
        "people": "person.2.fill",
        "plant": "leaf.fill",
        "pet": "pawprint.fill",
        "checklist": "checklist",
        "timer": "timer",
        "target": "target",
        "star": "star.fill",
        "smile": "face.smiling",
        "no_smoking": "nosign",
        "home": "house.fill",
    ]

    /// SF Symbol name for [key]; unknown keys resolve to `checklist` through the shared `IconKeys`.
    static func symbol(_ key: String) -> String {
        symbols[IconKeys.shared.resolve(key: key)] ?? "checklist"
    }
}
