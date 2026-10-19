import SwiftUI
import Shared

struct HabitDetailView: View {
    let habitId: String
    let onEdit: (String) -> Void

    var body: some View {
        Hosted(make: { KoinHelper.shared.habitDetail(id: habitId) }, state: { $0.uiState }) { host in
            HabitDetailContent(host: host, onEdit: onEdit)
        }
    }
}

private struct HabitDetailContent: View {
    let host: ViewModelHost<HabitDetailViewModel, HabitDetailUiState>
    let onEdit: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    /// Both the `Close` effect and the `Deleted` state ask to close; the screen must pop only once.
    @State private var closed = false
    @State private var errorMessage: String?
    @State private var showDeleteAlert = false

    var body: some View {
        content
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .errorAlert($errorMessage)
            .alert("Delete habit?", isPresented: $showDeleteAlert) {
                Button("Delete", role: .destructive) { host.vm.onEvent(event: HabitDetailEventDeleteConfirmed.shared) }
                Button("Cancel", role: .cancel) { host.vm.onEvent(event: HabitDetailEventDeleteCancelled.shared) }
            } message: {
                Text("This erases all history for this habit.")
            }
            .onChange(of: confirmingDelete, initial: true) { _, confirming in showDeleteAlert = confirming }
            .onChange(of: isDeleted, initial: true) { _, deleted in if deleted { close() } }
            .task { host.start() }
            .task {
                for await effect in host.vm.effects {
                    switch onEnum(of: effect) {
                    case .close: close()
                    case .openEdit(let e): onEdit(e.id)
                    case .showDomainError(let e): errorMessage = domainErrorMessage(e.error)
                    case .showError(let e): errorMessage = e.message
                    }
                }
            }
    }

    /// Idempotent: the first call dismisses and sets the latch, so repeated close requests (e.g. a delete plus its
    /// close effect) dismiss only once.
    private func close() {
        guard !closed else { return }
        dismiss()
        closed = true
    }

    private var isDeleted: Bool {
        if case .deleted = onEnum(of: host.state) { true } else { false }
    }

    private var confirmingDelete: Bool {
        if case .content(let c) = onEnum(of: host.state) { c.confirmingDelete } else { false }
    }

    private var title: String {
        if case .content(let c) = onEnum(of: host.state) { c.detail.habit.name } else { "" }
    }

    @ViewBuilder
    private var content: some View {
        switch onEnum(of: host.state) {
        case .loading, .deleted:
            Color.clear
        case .content(let content):
            ScrollView { DetailBody(state: content, onEvent: { host.vm.onEvent(event: $0) }).padding(16) }
                .background(Theme.canvas.ignoresSafeArea())
        }
    }
}

private struct DetailBody: View {
    let state: HabitDetailUiStateContent
    let onEvent: (HabitDetailEvent) -> Void

    @ScaledMetric(relativeTo: .largeTitle) private var iconSize = 40

    private var habit: Habit_ { state.detail.habit }
    private var color: Color { Palette.color(habit.category.colorKey) }

    private struct DayCellData {
        let day: Int
        let state: HabitDayState
    }

    /// Kotlin `List<Pair<LocalDate, HabitDayState>>` as plain values; the enum is bridged by SKIE.
    /// Streak-band position for day-of-month `day`; `state.bands` is aligned with `detail.days`.
    private func band(forDay day: Int) -> StreakBand {
        let bands = kotlinList(state.bands, as: StreakBand.self)
        return bands.indices.contains(day - 1) ? bands[day - 1] : .none
    }

    private var dayCells: [DayCellData] {
        let pairs = kotlinList(state.detail.days, as: KotlinPair<Kotlinx_datetimeLocalDate, __HabitDayState>.self)
        let cells = pairs.compactMap { pair -> DayCellData? in
            guard let date = pair.first, let raw = pair.second else { return nil }
            return DayCellData(day: Int(date.day), state: raw as HabitDayState)
        }
        if cells.count != pairs.count { assertionFailure("Habit day cells missing date or state") }
        return cells
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                CategoryIcon(iconKey: habit.iconKey, colorKey: habit.category.colorKey, size: iconSize)
                VStack(alignment: .leading, spacing: 2) {
                    Text(habit.name).font(.system(.title2, design: .rounded).weight(.bold))
                    Text(scheduleSummary(habit.currentSchedule.days) + reminderSuffix(habit.reminderTime) + (habit.isArchived ? " · Archived" : ""))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            HStack(spacing: 8) {
                StatTile(value: "🔥 \(state.detail.stats.currentStreak)", label: "Current streak", colorKey: "orange")
                StatTile(value: "\(state.detail.stats.bestStreak)", label: "Best streak", colorKey: "purple")
                StatTile(
                    value: state.detail.stats.completionRate.map { "\(Int(($0.doubleValue * 100).rounded()))%" } ?? "—",
                    label: "Completion",
                    colorKey: completionColorKey(state.detail.stats.completionRate)
                )
            }
            VStack(spacing: 8) {
                MonthHeader(
                    month: state.detail.month,
                    canGoNext: state.canGoNext,
                    onPrev: { onEvent(HabitDetailEventPrevMonth.shared) },
                    onNext: { onEvent(HabitDetailEventNextMonth.shared) }
                )
                MonthGrid(
                    month: state.detail.month,
                    leadingBlanks: Int(state.leadingBlanks),
                    items: dayCells
                ) { cell in
                    HabitDayCell(day: cell.day, state: cell.state, band: band(forDay: cell.day), colorKey: habit.category.colorKey, color: color)
                }
            }
            .themeCard(padding: 8)
            HStack(spacing: 8) {
                Button("Edit") { onEvent(HabitDetailEventEditClicked.shared) }
                Button(habit.isArchived ? "Unarchive" : "Archive") { onEvent(HabitDetailEventArchiveToggled.shared) }
                Button("Delete", role: .destructive) { onEvent(HabitDetailEventDeleteClicked.shared) }
                    .tint(.red)
            }
            .buttonStyle(.bordered)
            .padding(.bottom, 24)
        }
    }
}

private struct HabitDayCell: View {
    let day: Int
    let state: HabitDayState
    let band: StreakBand
    let colorKey: String
    let color: Color

    var body: some View {
        Text("\(day)")
            .font(.footnote.weight(.medium))
            .monospacedDigit()
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background { shape }
            .padding(3)
            .background(StreakBandBackground(band: band, colorKey: colorKey))
            .opacity(state == .notDue || state == .future ? 0.4 : 1)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Day \(day), \(describe)")
    }

    @ViewBuilder
    private var shape: some View {
        switch state {
        case .missed: Circle().fill(Color.red.opacity(0.2))
        case .pending: Circle().strokeBorder(color, lineWidth: 1.5)
        case .done, .notDue, .future: Color.clear
        }
    }

    private var foreground: Color {
        switch state {
        case .missed: .red
        default: .primary
        }
    }

    private var describe: String {
        switch state {
        case .done: "done"
        case .missed: "missed"
        case .pending: "pending"
        case .notDue: "not scheduled"
        case .future: "upcoming"
        }
    }
}
