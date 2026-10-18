import SwiftUI
import Shared

struct CalendarView: View {
    var body: some View {
        Hosted(make: { KoinHelper.shared.calendar() }, state: { $0.uiState }) { host in
            CalendarContent(host: host)
        }
    }
}

private struct CalendarContent: View {
    let host: ViewModelHost<CalendarViewModel, CalendarUiState>

    @State private var errorMessage: String?

    private var state: CalendarUiState { host.state }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                MonthHeader(
                    month: state.month,
                    canGoNext: state.canGoNext,
                    onPrev: { host.vm.onEvent(event: CalendarEventPrevMonth.shared) },
                    onNext: { host.vm.onEvent(event: CalendarEventNextMonth.shared) }
                )
                if let calendar = state.calendar {
                    MonthGrid(
                        month: state.month,
                        leadingBlanks: Int(state.leadingBlanks),
                        items: kotlinList(calendar.days, as: CalendarDay.self)
                    ) { day in
                        DayCell(day: day, band: band(for: day), selected: state.selected?.date == day.date) {
                            host.vm.onEvent(
                                event: state.selected?.date == day.date
                                    ? CalendarEventClearSelection.shared
                                    : CalendarEventSelectDay(date: day.date)
                            )
                        }
                    }
                    MonthSummary(calendar: calendar)
                }
                if let detail = state.selected {
                    DayPanel(detail: detail) { id in
                        host.vm.onEvent(event: CalendarEventToggleHabit(id: id, date: detail.date))
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .navigationTitle("Calendar")
        .navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(trigger: selectedDone) { old, new in
            // Same day, more habits done: a toggle really succeeded.
            guard let old, let new, old.date == new.date, new.done > old.done else { return nil }
            return .success
        }
        .errorAlert($errorMessage)
        .task { host.start() }
        .task {
            for await effect in host.vm.effects {
                switch onEnum(of: effect) {
                case .showDomainError(let e): errorMessage = domainErrorMessage(e.error)
                case .showError(let e): errorMessage = e.message
                }
            }
        }
    }

    /// Streak-band position for [day]; `state.bands` is aligned with `calendar.days` (day N at index N-1).
    private func band(for day: CalendarDay) -> StreakBand {
        let bands = kotlinList(state.bands, as: StreakBand.self)
        let index = Int(day.date.day) - 1
        return bands.indices.contains(index) ? bands[index] : .none
    }

    private struct SelectedDone: Equatable {
        let date: Kotlinx_datetimeLocalDate
        let done: Int
    }

    private var selectedDone: SelectedDone? {
        guard let detail = state.selected else { return nil }
        let done = detail.groups.reduce(0) { total, group in
            total + kotlinList(group.items, as: DayHabitItem.self).filter(\.isDone).count
        }
        return SelectedDone(date: detail.date, done: done)
    }
}

private struct DayCell: View {
    let day: CalendarDay
    let band: StreakBand
    let selected: Bool
    let onTap: () -> Void

    @ScaledMetric(relativeTo: .caption) private var markerSize = 14

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 1) {
                Text("\(day.date.day)").font(.callout).monospacedDigit()
                marker.frame(width: markerSize, height: markerSize)
                    .padding(.bottom, 4)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .strokeBorder(border, lineWidth: selected ? 2 : 1)
            )
            .padding(2)
            .background(StreakBandBackground(band: band, colorKey: "orange"))
            .contentShape(RoundedRectangle(cornerRadius: 10))
            .opacity(isFuture ? 0.4 : 1)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Day \(day.date.day), \(describe)")
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
    }

    private var isFuture: Bool {
        if case .future = onEnum(of: day.status) { true } else { false }
    }

    private var isToday: Bool {
        if case .inProgress = onEnum(of: day.status) { true } else { false }
    }

    private var border: Color {
        if selected { Color.accentColor } else if isToday { Color.accentColor.opacity(0.7) } else { Color.clear }
    }

    @ViewBuilder
    private var marker: some View {
        switch onEnum(of: day.status) {
        case .perfect: Text("🔥").font(.system(size: markerSize * 0.9))
        case .partial(let s): MiniRing(done: Int(s.done), due: Int(s.due))
        case .inProgress(let s): MiniRing(done: Int(s.done), due: Int(s.due))
        case .missed: Circle().fill(Color.red.opacity(0.5)).frame(width: markerSize * 0.45, height: markerSize * 0.45)
        case .noHabits, .future: EmptyView()
        }
    }

    private var describe: String {
        switch onEnum(of: day.status) {
        case .future: "upcoming"
        case .noHabits: "no habits"
        case .perfect: "perfect"
        case .inProgress(let s): "\(s.done) of \(s.due) done, today"
        case .partial(let s): "\(s.done) of \(s.due) done"
        case .missed: "missed"
        }
    }
}

private struct MiniRing: View {
    let done: Int
    let due: Int

    var body: some View {
        ZStack {
            Circle().stroke(Color.secondary.opacity(0.3), lineWidth: 2.5)
            Circle()
                .trim(from: 0, to: due == 0 ? 0 : Double(done) / Double(due))
                .stroke(Color.accentColor, style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
        .padding(1)
    }
}

private struct MonthSummary: View {
    let calendar: MonthCalendar

    var body: some View {
        HStack(spacing: 8) {
            StatTile(value: "\(calendar.perfectDays)", label: "Perfect days")
            StatTile(
                value: calendar.completionRate.map { "\(Int(($0.doubleValue * 100).rounded()))%" } ?? "—",
                label: "Completion"
            )
            StatTile(value: "\(calendar.longestPerfectRun)", label: "Longest run")
        }
    }
}

private struct DayPanel: View {
    let detail: DayDetail
    let onToggle: (String) -> Void

    private var title: String {
        let c = DateComponents(year: Int(detail.date.year), month: Int(detail.date.month.ordinal) + 1, day: Int(detail.date.day))
        return Calendar.current.date(from: c)?.formatted(.dateTime.weekday(.wide).day().month(.wide)) ?? ""
    }

    private var pendingDay: Bool {
        switch onEnum(of: detail.status) {
        case .inProgress, .future: true
        default: false
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.headline)
            if detail.groups.isEmpty {
                Text("No habits due.").font(.subheadline).foregroundStyle(.secondary)
            }
            ForEach(detail.groups, id: \.category.id) { group in
                CategoryHeader(category: group.category)
                ForEach(kotlinList(group.items, as: DayHabitItem.self), id: \.habit.id) { item in
                    HabitRow(
                        habit: item.habit,
                        isDone: item.isDone,
                        status: item.isDone ? "Done" : (pendingDay ? nil : "Missed"),
                        toggleEnabled: detail.isEditable,
                        onToggle: { onToggle(item.habit.id) }
                    )
                }
            }
            if !detail.isEditable && !detail.groups.isEmpty {
                Text("Only today and yesterday can be changed.").font(.footnote).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(.fill.quaternary, in: RoundedRectangle(cornerRadius: 16))
    }
}
