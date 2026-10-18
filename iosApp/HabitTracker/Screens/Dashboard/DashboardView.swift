import SwiftUI
import Shared

struct DashboardView: View {
    let onAddHabit: () -> Void
    let onOpenHabit: (String) -> Void

    @State private var host = ViewModelHost(KoinHelper.shared.dashboard()) { $0.uiState }
    @State private var celebrations = 0
    @State private var toggles = 0
    @State private var errorMessage: String?

    var body: some View {
        content
            .navigationTitle("Today")
            .navigationBarTitleDisplayMode(.inline)
            .sensoryFeedback(.success, trigger: toggles)
            .alert("Something went wrong", isPresented: errorPresented) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(errorMessage ?? "")
            }
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

    private var errorPresented: Binding<Bool> {
        Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })
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
            ForEach(group.items as? [TodayHabitItem] ?? [], id: \.habit.id) { item in
                HabitRow(
                    item: item,
                    onToggle: {
                        toggles += 1
                        host.vm.onEvent(event: DashboardEventToggleHabit(id: item.habit.id))
                    },
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

private func domainErrorMessage(_ error: DomainError) -> String {
    switch onEnum(of: error) {
    case .editWindowClosed: "You can only change today and yesterday."
    case .notDue: "That habit isn't scheduled for this day."
    case .invalidName: "Names need 1 to 40 characters."
    case .noDaysSelected: "Pick at least one day."
    case .unknownIcon: "That icon isn't available."
    case .unknownColor: "That color isn't available."
    case .categoryNotFound: "That category no longer exists."
    case .habitNotFound: "That habit no longer exists."
    case .categoryNotEmpty: "Move or delete this category's habits first."
    }
}
