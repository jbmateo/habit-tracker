import SwiftUI
import UserNotifications
import Shared

/// Create (`habitId == nil`) or edit form, presented in a sheet. Its ViewModel is created when the sheet content
/// appears and cleared when the sheet goes away, because the host lives in this view's state.
struct HabitFormView: View {
    let habitId: String?

    var body: some View {
        Hosted(make: { KoinHelper.shared.habitForm(id: habitId) }, state: { $0.uiState }) { host in
            HabitFormContent(host: host)
        }
    }
}

private let scheduleNote = "Schedule changes apply from today. Past days keep their old schedule."
private let reminderHint = "Only on scheduled days. Skipped if you've already done it."
private let deniedNote = "Notifications are off for Habits. Turn them on in Settings."

private struct HabitFormContent: View {
    let host: ViewModelHost<HabitFormViewModel, HabitFormUiState>

    @Environment(\.dismiss) private var dismiss
    @State private var errorMessage: String?
    /// Local copy of the name: typing must not wait on the ViewModel round trip, or fast input drops characters.
    /// It is filled from the ViewModel only once, when an edited habit finishes loading.
    @State private var name = ""
    @State private var notificationsDenied = false

    private var state: HabitFormUiState { host.state }
    private var inputsEnabled: Bool { !state.isLoading && !state.isSaving }

    /// The ViewModel's LocalTime as a Date for DatePicker (only hour and minute matter).
    private var reminderDate: Binding<Date> {
        Binding(
            get: {
                let t = state.reminderTime
                return Calendar.current.date(from: DateComponents(hour: Int(t.hour), minute: Int(t.minute))) ?? Date()
            },
            set: { date in
                let parts = Calendar.current.dateComponents([.hour, .minute], from: date)
                send(HabitFormEventReminderTimeChanged(
                    time: Kotlinx_datetimeLocalTime(hour: Int32(parts.hour ?? 9), minute: Int32(parts.minute ?? 0), second: 0, nanosecond: 0)
                ))
            }
        )
    }

    /// Asks once; afterwards reflects the current setting so the denied note can point to Settings.
    private func requestNotificationPermission() async {
        let center = UNUserNotificationCenter.current()
        _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
        let settings = await center.notificationSettings()
        notificationsDenied = settings.authorizationStatus == .denied
    }

    var body: some View {
        Form {
            Section {
                TextField("Name", text: $name)
                    .disabled(!inputsEnabled)
                    .onChange(of: name) { host.vm.onEvent(event: HabitFormEventNameChanged(v: name)) }
                if let error = state.nameError {
                    Text(domainErrorMessage(error)).font(.footnote).foregroundStyle(.red)
                }
            }
            Section("Category") { CategoryChips(state: state, onEvent: send).disabled(!inputsEnabled) }
            Section {
                DayChips(state: state, onEvent: send).disabled(!inputsEnabled)
                if state.showScheduleNote {
                    Text(scheduleNote).font(.footnote).foregroundStyle(.secondary)
                }
            } header: {
                Text("Days")
            }
            Section("Reminder") {
                Toggle("Remind me", isOn: Binding(
                    get: { state.reminderEnabled },
                    set: { on in
                        send(HabitFormEventReminderToggled.shared)
                        if on { Task { await requestNotificationPermission() } }
                    }
                ))
                .disabled(!inputsEnabled)
                if state.reminderEnabled {
                    DatePicker("Reminder time", selection: reminderDate, displayedComponents: .hourAndMinute)
                        .disabled(!inputsEnabled)
                    Text(reminderHint).font(.footnote).foregroundStyle(.secondary)
                    if notificationsDenied {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(deniedNote).font(.footnote).foregroundStyle(.red)
                            Button("Open Settings") {
                                if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                            }
                            .font(.footnote)
                        }
                    }
                }
            }
            Section("Icon") { IconGrid(state: state, onEvent: send).disabled(!inputsEnabled) }
        }
        .navigationTitle(state.isEdit ? "Edit habit" : "New habit")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { host.vm.onEvent(event: HabitFormEventSave.shared) }
                    .disabled(!state.canSave || !inputsEnabled)
            }
        }
        .interactiveDismissDisabled(state.isSaving)
        .onChange(of: state.isLoading, initial: true) { _, loading in
            if !loading && name.isEmpty { name = state.name }
        }
        .errorAlert($errorMessage)
        .task { host.start() }
        .task {
            for await effect in host.vm.effects {
                switch onEnum(of: effect) {
                case .dismiss: dismiss()
                case .showDomainError(let e): errorMessage = domainErrorMessage(e.error)
                case .showError(let e): errorMessage = e.message
                }
            }
        }
    }

    private func send(_ event: HabitFormEvent) {
        host.vm.onEvent(event: event)
    }
}

private struct CategoryChips: View {
    let state: HabitFormUiState
    let onEvent: (HabitFormEvent) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(kotlinList(state.categories, as: Category_.self), id: \.id) { category in
                    let selected = category.id == state.categoryId
                    Button {
                        onEvent(HabitFormEventCategorySelected(id: category.id))
                    } label: {
                        HStack(spacing: 6) {
                            Circle().fill(Palette.color(category.colorKey)).frame(width: 10, height: 10)
                            Text(category.name).font(.subheadline)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(selected ? Color.accentColor.opacity(0.2) : Color.secondary.opacity(0.12), in: Capsule())
                        .overlay(Capsule().strokeBorder(selected ? Color.accentColor : Color.clear, lineWidth: 1.5))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(selected ? .isSelected : [])
                }
            }
            .padding(.vertical, 2)
        }
    }
}

private struct DayChips: View {
    let state: HabitFormUiState
    let onEvent: (HabitFormEvent) -> Void

    @ScaledMetric(relativeTo: .body) private var chipSize = 36

    private static let days: [(Kotlinx_datetimeDayOfWeek, String, String)] = [
        (.monday, "M", "Monday"), (.tuesday, "T", "Tuesday"), (.wednesday, "W", "Wednesday"),
        (.thursday, "T", "Thursday"), (.friday, "F", "Friday"), (.saturday, "S", "Saturday"), (.sunday, "S", "Sunday"),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 4) {
                ForEach(Self.days.indices, id: \.self) { index in
                    let (day, letter, name) = Self.days[index]
                    let selected = state.days.contains(day)
                    Button {
                        onEvent(HabitFormEventDayToggled(day: day))
                    } label: {
                        Text(letter)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(selected ? Color.white : Color.primary)
                            .frame(width: chipSize, height: chipSize)
                            .background(Circle().fill(selected ? Color.accentColor : Color.secondary.opacity(0.15)))
                    }
                    .buttonStyle(.plain)
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel(name)
                    .accessibilityAddTraits(selected ? .isSelected : [])
                }
            }
            Toggle("Every day", isOn: Binding(
                get: { state.days.count == 7 },
                set: { _ in onEvent(HabitFormEventEveryDayToggled.shared) }
            ))
        }
    }
}

private struct IconGrid: View {
    let state: HabitFormUiState
    let onEvent: (HabitFormEvent) -> Void

    @ScaledMetric(relativeTo: .body) private var cell = 44

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: cell), spacing: 4)], spacing: 4) {
            ForEach(kotlinList(state.iconKeys, as: String.self), id: \.self) { key in
                let selected = key == state.iconKey
                Button {
                    onEvent(HabitFormEventIconSelected(key: key))
                } label: {
                    Image(systemName: HabitIcons.symbol(key))
                        .font(.title3)
                        .foregroundStyle(selected ? Color.accentColor : Color.primary)
                        .frame(width: cell, height: cell)
                        .background(Circle().fill(selected ? Color.accentColor.opacity(0.2) : Color.clear))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Icon \(key)")
                .accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
    }
}
