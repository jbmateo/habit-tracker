import SwiftUI
import Shared

/// Short human-readable message for a `DomainError`; shared by every screen that surfaces one.
func domainErrorMessage(_ error: DomainError) -> String {
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

extension View {
    /// Shows [message] in an alert while it is non-nil; clearing it dismisses the alert.
    func errorAlert(_ message: Binding<String?>) -> some View {
        alert(
            "Something went wrong",
            isPresented: Binding(get: { message.wrappedValue != nil }, set: { if !$0 { message.wrappedValue = nil } })
        ) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(message.wrappedValue ?? "")
        }
    }
}
