import SwiftUI
import Shared

struct DashboardView: View {
    let onAddHabit: () -> Void
    let onOpenHabit: (String) -> Void

    var body: some View {
        Hosted(make: { KoinHelper.shared.dashboard() }, state: { $0.uiState }) { host in
            DashboardContent(host: host, onAddHabit: onAddHabit, onOpenHabit: onOpenHabit)
        }
    }
}

private struct DashboardContent: View {
    let host: ViewModelHost<DashboardViewModel, DashboardUiState>
    let onAddHabit: () -> Void
    let onOpenHabit: (String) -> Void

    @State private var celebrations = 0
    @State private var errorMessage: String?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Theme.canvas.ignoresSafeArea())
            .navigationTitle("Today")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if let sort = currentSort {
                    ToolbarItem(placement: .topBarTrailing) { sortMenu(current: sort) }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        host.vm.onEvent(event: DashboardEventAddHabitClicked.shared)
                    } label: {
                        Label("Add habit", systemImage: "plus")
                    }
                }
            }
            // Haptics only when the done count goes up, i.e. a toggle really succeeded: a light tap per habit,
            // success when it completes the day.
            .sensoryFeedback(trigger: progress) { old, new in
                guard let old, let new, new.done > old.done else { return nil }
                return new.done == new.due ? .success : .impact(weight: .light)
            }
            .errorAlert($errorMessage)
            .task { host.start() }
            .task {
                for await effect in host.vm.effects {
                    switch onEnum(of: effect) {
                    case .celebrate: celebrations += 1
                    case .openCreateHabit: onAddHabit()
                    case .openHabit(let e): onOpenHabit(e.id)
                    case .showDomainError(let e): errorMessage = domainErrorMessage(e.error)
                    case .showError(let e): errorMessage = e.message
                    }
                }
            }
    }

    /// The remembered sort; `nil` until habits exist, so the menu only shows when there is a list to sort.
    private var currentSort: TodaySort? {
        if case .content(let content) = onEnum(of: host.state) { content.sort } else { nil }
    }

    private static let sortOptions: [(TodaySort, String)] = [
        (.category, "Category"), (.name, "Name A–Z"), (.streak, "Streak"), (.toDoFirst, "To do first"),
    ]

    private func sortMenu(current: TodaySort) -> some View {
        Menu {
            Picker("Sort by", selection: Binding(
                get: { current },
                set: { host.vm.onEvent(event: DashboardEventSortSelected(sort: $0)) }
            )) {
                ForEach(Self.sortOptions, id: \.0) { option in
                    Text(option.1).tag(option.0)
                }
            }
        } label: {
            Label("Sort habits", systemImage: "arrow.up.arrow.down")
        }
    }

    private struct Progress: Equatable { let done: Int; let due: Int }

    /// Today's progress; `nil` until the dashboard has loaded, so the first load never counts as a toggle.
    private var progress: Progress? {
        if case .content(let content) = onEnum(of: host.state) {
            Progress(done: Int(content.dashboard.done), due: Int(content.dashboard.due))
        } else {
            nil
        }
    }

    @ViewBuilder
    private var content: some View {
        switch onEnum(of: host.state) {
        case .loading:
            Color.clear
        case .noHabits:
            emptyState(
                title: "No habits yet",
                message: "Build your routine one day at a time.",
                cta: "Create your first habit"
            )
        case .content(let content):
            let items = kotlinList(content.items, as: TodayHabitItem.self)
            dashboard(content.dashboard, items: items, milestone: content.milestone?.intValue)
                .overlay(alignment: .top) {
                    if !reduceMotion {
                        ConfettiView(
                            trigger: celebrations,
                            colors: [Theme.flame, Theme.gold, Theme.accent] + items.map { Palette.color($0.habit.category.colorKey) }
                        )
                    }
                }
        }
    }

    private func dashboard(_ dashboard: TodayDashboard, items: [TodayHabitItem], milestone: Int?) -> some View {
        List {
            Section {
                HeroStreakCard(
                    dashboard: dashboard,
                    dateText: formatted(dashboard.date),
                    pulseTrigger: reduceMotion ? celebrations : 0
                )
                if let milestone {
                    MilestoneCard(milestone: milestone) {
                        withAnimation(.easeOut(duration: 0.25)) {
                            host.vm.onEvent(event: DashboardEventDismissMilestone.shared)
                        }
                    }
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }
            }
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
            .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))

            if dashboard.groups.isEmpty {
                emptyState(title: "Nothing due today", message: "Enjoy the day off.", cta: nil)
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
            }
            if !items.isEmpty {
                Section {
                    ForEach(items, id: \.habit.id) { item in
                        HabitRow(
                            habit: item.habit,
                            isDone: item.isDone,
                            streak: Int(item.streak),
                            onToggle: { host.vm.onEvent(event: DashboardEventToggleHabit(id: item.habit.id)) },
                            onOpen: { host.vm.onEvent(event: DashboardEventHabitClicked(id: item.habit.id)) }
                        )
                        .listRowBackground(Theme.card)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(Theme.canvas)
        .animation(.easeOut(duration: 0.25), value: milestone)
    }

    private func emptyState(title: String, message: String, cta: String?) -> some View {
        VStack(spacing: 12) {
            Text(title).font(.title3.weight(.semibold))
            Text(message).font(.subheadline).foregroundStyle(.secondary)
            if let cta {
                Button(cta) { host.vm.onEvent(event: DashboardEventAddHabitClicked.shared) }
                    .buttonStyle(.borderedProminent)
            }
        }
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity, maxHeight: cta == nil ? nil : .infinity)
        .padding(32)
    }

    private func formatted(_ date: Kotlinx_datetimeLocalDate) -> String {
        let components = DateComponents(year: Int(date.year), month: Int(date.month.ordinal) + 1, day: Int(date.day))
        guard let value = Calendar.current.date(from: components) else { return "" }
        return value.formatted(.dateTime.weekday(.wide).day().month(.wide))
    }
}
