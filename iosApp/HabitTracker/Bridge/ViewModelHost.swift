import Observation
import Shared

/// Owns a Kotlin ViewModel (through its `ViewModelHandle`) for the lifetime of a SwiftUI view and mirrors its
/// state flow into an observable `state`. Views hold it with `@State`.
///
/// Construction is cheap and starts nothing, because SwiftUI may build (and discard) the `@State` initial value on
/// every parent re-evaluation. Collection begins with `start()`, called from the view's `.task`.
/// `isolated deinit` lets teardown touch main-actor state; it cancels collection and calls `handle.clear()`, which
/// runs the ViewModel's `onCleared` and cancels its `viewModelScope`.
@MainActor @Observable
final class ViewModelHost<VM: AnyObject, State> {
    let vm: VM
    private(set) var state: State

    @ObservationIgnored private let handle: ViewModelHandle<VM>
    @ObservationIgnored private let flow: SkieSwiftStateFlow<State>
    @ObservationIgnored private var task: Task<Void, Never>?

    init(_ handle: ViewModelHandle<VM>, state: (VM) -> SkieSwiftStateFlow<State>) {
        self.handle = handle
        self.vm = handle.viewModel
        self.flow = state(handle.viewModel)
        self.state = flow.value
    }

    /// Starts mirroring the state flow. Safe to call repeatedly.
    func start() {
        guard task == nil else { return }
        let flow = flow
        task = Task { [weak self] in
            for await value in flow {
                self?.state = value
            }
        }
    }

    isolated deinit {
        task?.cancel()
        handle.clear()
    }
}
