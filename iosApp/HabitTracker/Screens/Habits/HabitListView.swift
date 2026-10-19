import SwiftUI
import Shared

struct HabitListView: View {
    let onAddHabit: () -> Void
    let onOpenHabit: (String) -> Void
    let onOpenCategories: () -> Void

    var body: some View {
        Hosted(make: { KoinHelper.shared.habitList() }, state: { $0.uiState }) { host in
            HabitListContent(host: host, onAddHabit: onAddHabit, onOpenHabit: onOpenHabit, onOpenCategories: onOpenCategories)
        }
    }
}

private struct HabitListContent: View {
    let host: ViewModelHost<HabitListViewModel, HabitListUiState>
    let onAddHabit: () -> Void
    let onOpenHabit: (String) -> Void
    let onOpenCategories: () -> Void

    @State private var archivedExpanded = false

    var body: some View {
        content
            .navigationTitle("Habits")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        host.vm.onEvent(event: HabitListEventAddHabitClicked.shared)
                    } label: {
                        Label("Add habit", systemImage: "plus")
                    }
                }
            }
            .task { host.start() }
            .task {
                for await effect in host.vm.effects {
                    switch onEnum(of: effect) {
                    case .openHabit(let e): onOpenHabit(e.id)
                    case .openCreateHabit: onAddHabit()
                    case .openCategories: onOpenCategories()
                    }
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        switch onEnum(of: host.state) {
        case .loading:
            Color.clear
        case .content(let content):
            list(content.overview)
        }
    }

    private func list(_ overview: HabitsOverview) -> some View {
        let archived = kotlinList(overview.archived, as: Habit_.self)
        return List {
            if overview.active.isEmpty {
                Text("No active habits.").foregroundStyle(.secondary)
            }
            ForEach(overview.active, id: \.category.id) { group in
                Section {
                    ForEach(kotlinList(group.items, as: Habit_.self), id: \.id) { habit in
                        habitRow(habit).listRowBackground(Theme.card)
                    }
                } header: {
                    CategoryHeader(category: group.category)
                }
            }
            Section {
                Button("Manage categories") {
                    host.vm.onEvent(event: HabitListEventManageCategoriesClicked.shared)
                }
                .listRowBackground(Theme.card)
            }
            if !archived.isEmpty {
                Section {
                    DisclosureGroup("Archived (\(archived.count))", isExpanded: $archivedExpanded) {
                        ForEach(archived, id: \.id) { habit in
                            habitRow(habit)
                        }
                    }
                    .listRowBackground(Theme.card)
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(Theme.canvas)
    }

    private func habitRow(_ habit: Habit_) -> some View {
        Button {
            host.vm.onEvent(event: HabitListEventHabitClicked(id: habit.id))
        } label: {
            HStack(spacing: 12) {
                CategoryIcon(iconKey: habit.iconKey, colorKey: habit.category.colorKey)
                VStack(alignment: .leading, spacing: 2) {
                    Text(habit.name).font(.body)
                    Text(scheduleSummary(habit.currentSchedule.days) + reminderSuffix(habit.reminderTime)).font(.footnote).foregroundStyle(.secondary)
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(habit.name)
        .accessibilityValue(scheduleSummary(habit.currentSchedule.days) + reminderSuffix(habit.reminderTime))
        .accessibilityAddTraits(.isButton)
    }
}
