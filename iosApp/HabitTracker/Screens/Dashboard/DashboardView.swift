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

    var body: some View {
        content
            .navigationTitle("Today")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        host.vm.onEvent(event: DashboardEventAddHabitClicked.shared)
                    } label: {
                        Label("Add habit", systemImage: "plus")
                    }
                }
            }
            // Success haptic only when the done count goes up, i.e. a toggle really succeeded.
            .sensoryFeedback(trigger: doneCount) { old, new in
                guard let old, let new, new > old else { return nil }
                return .success
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

    /// Habits done today; `nil` until the dashboard has loaded, so the first load never counts as a toggle.
    private var doneCount: Int? {
        if case .content(let content) = onEnum(of: host.state) { Int(content.dashboard.done) } else { nil }
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
            dashboard(content.dashboard)
        }
    }

    private func dashboard(_ dashboard: TodayDashboard) -> some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 16) {
                    Text(formatted(dashboard.date)).font(.title2.bold())
                    HStack(spacing: 16) {
                        ProgressRing(done: Int(dashboard.done), due: Int(dashboard.due), celebrateTrigger: celebrations)
                        StreakCard(streak: dashboard.streak)
                    }
                    if dashboard.isPerfect {
                        Text("Perfect day!").font(.headline)
                    }
                }
                .padding(.vertical, 4)
            }
            .listRowSeparator(.hidden)

            if dashboard.groups.isEmpty {
                emptyState(title: "Nothing due today", message: "Enjoy the day off.", cta: nil)
                    .listRowSeparator(.hidden)
            }
            ForEach(dashboard.groups, id: \.category.id) { group in
                groupSection(group)
            }
        }
        .listStyle(.insetGrouped)
    }

    private func groupSection(_ group: CategoryGroup<TodayHabitItem>) -> some View {
        Section {
            ForEach(kotlinList(group.items, as: TodayHabitItem.self), id: \.habit.id) { item in
                HabitRow(
                    habit: item.habit,
                    isDone: item.isDone,
                    streak: Int(item.streak),
                    onToggle: { host.vm.onEvent(event: DashboardEventToggleHabit(id: item.habit.id)) },
                    onOpen: { host.vm.onEvent(event: DashboardEventHabitClicked(id: item.habit.id)) }
                )
            }
        } header: {
            CategoryHeader(category: group.category)
        }
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
