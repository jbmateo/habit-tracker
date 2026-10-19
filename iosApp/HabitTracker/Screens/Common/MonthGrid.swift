import SwiftUI
import Shared

/// "October 2026".
func monthTitle(_ month: Kotlinx_datetimeYearMonth) -> String {
    let components = DateComponents(year: Int(month.year), month: Int(month.month.ordinal) + 1, day: 1)
    guard let date = Calendar.current.date(from: components) else { return "" }
    return date.formatted(.dateTime.month(.wide).year())
}

/// One slot in the grid. Blanks and days get distinct identities (`id`), otherwise a LazyVGrid treats the two
/// groups' overlapping integer ids as the same cell and drops days.
enum MonthGridCell: Identifiable, Equatable {
    case blank(Int)
    case day(Int)

    var id: String {
        switch self {
        case .blank(let i): "blank-\(i)"
        case .day(let i): "day-\(i)"
        }
    }

    static func build(leadingBlanks: Int, itemCount: Int) -> [MonthGridCell] {
        (0..<max(leadingBlanks, 0)).map(MonthGridCell.blank) + (0..<itemCount).map(MonthGridCell.day)
    }
}

/// Monday-first month grid shared by Calendar and Habit detail. `leadingBlanks` empty cells precede the 1st, as
/// computed by the ViewModel; [cell] draws one day.
struct MonthGrid<Item, Cell: View>: View {
    let month: Kotlinx_datetimeYearMonth
    let leadingBlanks: Int
    let items: [Item]
    @ViewBuilder let cell: (Item) -> Cell

    private static var weekdays: [(short: String, full: String)] {
        [("M", "Monday"), ("T", "Tuesday"), ("W", "Wednesday"), ("T", "Thursday"),
         ("F", "Friday"), ("S", "Saturday"), ("S", "Sunday")]
    }

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 0), count: 7)

    var body: some View {
        VStack(spacing: 4) {
            LazyVGrid(columns: columns, spacing: 0) {
                ForEach(0..<7, id: \.self) { index in
                    Text(Self.weekdays[index].short)
                        .font(.footnote.weight(.medium))
                        .foregroundStyle(.secondary)
                        .accessibilityLabel(Self.weekdays[index].full)
                }
            }
            LazyVGrid(columns: columns, spacing: 0) {
                ForEach(MonthGridCell.build(leadingBlanks: leadingBlanks, itemCount: items.count)) { slot in
                    switch slot {
                    case .blank:
                        Color.clear.aspectRatio(1, contentMode: .fit).accessibilityHidden(true)
                    case .day(let index):
                        cell(items[index]).aspectRatio(1, contentMode: .fit)
                    }
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Calendar for \(monthTitle(month))")
    }
}

/// Month title with previous/next buttons; next is disabled once [canGoNext] is false.
struct MonthHeader: View {
    let month: Kotlinx_datetimeYearMonth
    let canGoNext: Bool
    let onPrev: () -> Void
    let onNext: () -> Void

    var body: some View {
        HStack {
            Button(action: onPrev) { Image(systemName: "chevron.left").padding(8) }
                .accessibilityLabel("Previous month")
            Spacer()
            Text(monthTitle(month)).font(.headline)
            Spacer()
            Button(action: onNext) { Image(systemName: "chevron.right").padding(8) }
                .disabled(!canGoNext)
                .accessibilityLabel("Next month")
        }
    }
}

/// Small value-over-label tile, tinted with the palette color `colorKey`; used by the calendar summary and the
/// habit detail stats.
struct StatTile: View {
    let value: String
    let label: String
    let colorKey: String
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let color = Palette.color(colorKey)
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.system(.title2, design: .rounded).weight(.bold))
                .foregroundStyle(color)
                .minimumScaleFactor(0.7)
                .lineLimit(1)
            Text(label).font(.footnote).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(color.opacity(colorScheme == .dark ? 0.22 : 0.12), in: RoundedRectangle(cornerRadius: 16))
        .background(Theme.card, in: RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }
}

/// Palette key for a completion rate: green ≥ 80%, amber (yellow) 50–79%, red < 50%, gray without data.
func completionColorKey(_ rate: KotlinDouble?) -> String {
    guard let level = CompletionLevelsKt.completionLevel(rate: rate) else { return "gray" }
    switch level {
    case .good: return "green"
    case .fair: return "amber"
    case .low: return "red"
    }
}
