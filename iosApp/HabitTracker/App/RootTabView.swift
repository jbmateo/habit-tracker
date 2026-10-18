import SwiftUI

/// Three-tab shell. Each tab owns a `NavigationStack` with a typed route path; create/edit is a modal sheet.
struct RootTabView: View {
    private enum Tab: Hashable { case today, calendar, habits }

    /// Pushed destinations, shared by the Today and Habits stacks.
    private enum Route: Hashable {
        case detail(String)
        case categories
    }

    /// Form sheet request: create (`habitId == nil`) or edit. `id` makes each opening distinct, so a reopened sheet
    /// always gets a fresh ViewModel.
    private struct FormSheet: Identifiable {
        let habitId: String?
        let id: Int
    }

    @State private var selection: Tab = .today
    @State private var todayPath: [Route] = []
    @State private var habitsPath: [Route] = []
    @State private var formSheet: FormSheet?
    @State private var openCount = 0

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack(path: $todayPath) {
                DashboardView(
                    onAddHabit: { openForm(nil) },
                    onOpenHabit: { todayPath.append(.detail($0)) }
                )
                .navigationDestination(for: Route.self) { destination($0) }
            }
            .tabItem { Label("Today", systemImage: "checkmark.circle") }
            .tag(Tab.today)

            NavigationStack {
                CalendarView()
            }
            .tabItem { Label("Calendar", systemImage: "calendar") }
            .tag(Tab.calendar)

            NavigationStack(path: $habitsPath) {
                HabitListView(
                    onAddHabit: { openForm(nil) },
                    onOpenHabit: { habitsPath.append(.detail($0)) },
                    onOpenCategories: { habitsPath.append(.categories) }
                )
                .navigationDestination(for: Route.self) { destination($0) }
            }
            .tabItem { Label("Habits", systemImage: "list.bullet") }
            .tag(Tab.habits)
        }
        .sheet(item: $formSheet) { sheet in
            NavigationStack {
                HabitFormView(habitId: sheet.habitId)
            }
        }
    }

    @ViewBuilder
    private func destination(_ route: Route) -> some View {
        switch route {
        case .detail(let id): HabitDetailView(habitId: id, onEdit: { openForm($0) })
        case .categories: CategoryListView()
        }
    }

    private func openForm(_ habitId: String?) {
        openCount += 1
        formSheet = FormSheet(habitId: habitId, id: openCount)
    }
}
