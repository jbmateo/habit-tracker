import SwiftUI
import Shared

/// Creates a `ViewModelHost` exactly once per view identity, then hands it to [content].
///
/// SwiftUI may rebuild a view's `@State` initial value on every parent re-render, so a Kotlin ViewModel must never
/// be constructed in a property initializer. Here the host is created in `onAppear` only while `nil`, and it is
/// released (clearing the ViewModel) when SwiftUI drops this view's state: a popped destination or a dismissed sheet.
struct Hosted<VM: AnyObject, State, Content: View>: View {
    let make: () -> ViewModelHandle<VM>
    let state: (VM) -> SkieSwiftStateFlow<State>
    @ViewBuilder let content: (ViewModelHost<VM, State>) -> Content

    @SwiftUI.State private var host: ViewModelHost<VM, State>?

    var body: some View {
        ZStack {
            if let host {
                content(host)
            }
        }
        .onAppear {
            if host == nil { host = ViewModelHost(make(), state: state) }
        }
    }
}
