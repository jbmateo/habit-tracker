import SwiftUI

/// Three-tab shell. Each tab owns a `NavigationStack`; Task 20 fills in Calendar, Habits and the form.
struct RootTabView: View {
    private enum Tab: Hashable { case today, calendar, habits }

    /// Form sheet request: create (`habitId == nil`) or edit. `openId` makes each opening distinct.
    private struct FormSheet: Identifiable {
        let habitId: String?
        let id: Int
    }

    @State private var selection: Tab = .today
    @State private var formSheet: FormSheet?
    @State private var openCount = 0

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack {
                DashboardView(onAddHabit: { openForm(nil) }, onOpenHabit: { _ in })
            }
            .tabItem { Label("Today", systemImage: "checkmark.circle") }
            .tag(Tab.today)

            NavigationStack {
                PlaceholderView(title: "Calendar")
            }
            .tabItem { Label("Calendar", systemImage: "calendar") }
            .tag(Tab.calendar)

            NavigationStack {
                PlaceholderView(title: "Habits")
            }
            .tabItem { Label("Habits", systemImage: "list.bullet") }
            .tag(Tab.habits)
        }
        .sheet(item: $formSheet) { sheet in
            NavigationStack {
                PlaceholderView(title: sheet.habitId == nil ? "New habit" : "Edit habit")
            }
        }
    }

    private func openForm(_ habitId: String?) {
        openCount += 1
        formSheet = FormSheet(habitId: habitId, id: openCount)
    }
}

private struct PlaceholderView: View {
    let title: String

    var body: some View {
        ContentUnavailableView(title, systemImage: "hammer", description: Text("Coming soon."))
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
    }
}
