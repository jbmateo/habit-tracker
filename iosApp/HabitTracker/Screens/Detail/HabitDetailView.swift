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

    /// Idempotent: latches only after actually dismissing, so a close request that cannot act yet can be retried.
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
                Image(systemName: HabitIcons.symbol(habit.iconKey))
                    .font(.system(size: iconSize * 0.7))
                    .foregroundStyle(color)
                    .frame(width: iconSize)
                VStack(alignment: .leading, spacing: 2) {
                    Text(habit.name).font(.title2.bold()).foregroundStyle(color)
                    Text(scheduleSummary(habit.currentSchedule.days) + (habit.isArchived ? " · Archived" : ""))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            HStack(spacing: 8) {
                StatTile(value: "🔥 \(state.detail.stats.currentStreak)", label: "Current streak")
                StatTile(value: "\(state.detail.stats.bestStreak)", label: "Best streak")
                StatTile(
                    value: state.detail.stats.completionRate.map { "\(Int(($0.doubleValue * 100).rounded()))%" } ?? "—",
                    label: "Completion"
                )
            }
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
                HabitDayCell(day: cell.day, state: cell.state, color: color, onColor: Palette.onColor(habit.category.colorKey))
            }
            HStack(spacing: 8) {
                Button("Edit") { onEvent(HabitDetailEventEditClicked.shared) }
                Button(habit.isArchived ? "Unarchive" : "Archive") { onEvent(HabitDetailEventArchiveToggled.shared) }
                Button("Delete", role: .destructive) { onEvent(HabitDetailEventDeleteClicked.shared) }
            }
            .buttonStyle(.bordered)
            .padding(.bottom, 24)
        }
    }
}

private struct HabitDayCell: View {
    let day: Int
    let state: HabitDayState
    let color: Color
    let onColor: Color

    var body: some View {
        Text("\(day)")
            .font(.footnote.weight(.medium))
            .monospacedDigit()
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background { shape }
            .padding(3)
            .opacity(state == .notDue || state == .future ? 0.4 : 1)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Day \(day), \(describe)")
    }

    @ViewBuilder
    private var shape: some View {
        switch state {
        case .done: Circle().fill(color)
        case .missed: Circle().fill(Color.red.opacity(0.2))
        case .pending: Circle().strokeBorder(color, lineWidth: 1.5)
        case .notDue, .future: Color.clear
        }
    }

    private var foreground: Color {
        switch state {
        case .done: onColor
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
