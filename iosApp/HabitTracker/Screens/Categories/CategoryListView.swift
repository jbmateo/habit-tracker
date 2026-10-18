import SwiftUI
import Shared

struct CategoryListView: View {
    var body: some View {
        Hosted(make: { KoinHelper.shared.categoryList() }, state: { $0.uiState }) { host in
            CategoryListContent(host: host)
        }
    }
}

private struct CategoryListContent: View {
    let host: ViewModelHost<CategoryListViewModel, CategoryListUiState>

    @State private var errorMessage: String?
    @ScaledMetric(relativeTo: .body) private var dotSize = 24

    private var editorPresented: Binding<Bool> {
        Binding(
            get: { host.state.editor != nil },
            set: { if !$0 { host.vm.onEvent(event: CategoryListEventDismissEditor.shared) } }
        )
    }

    var body: some View {
        List(kotlinList(host.state.categories, as: Category_.self), id: \.id) { category in
            Button {
                host.vm.onEvent(event: CategoryListEventEditClicked(id: category.id))
            } label: {
                HStack(spacing: 12) {
                    Circle().fill(Palette.color(category.colorKey)).frame(width: dotSize, height: dotSize)
                    Text(category.name).font(.body)
                    Spacer(minLength: 8)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .swipeActions(edge: .trailing) {
                Button(role: .destructive) {
                    host.vm.onEvent(event: CategoryListEventDeleteClicked(id: category.id))
                } label: {
                    Label("Delete \(category.name)", systemImage: "trash")
                }
            }
            .accessibilityAction(named: "Delete") {
                host.vm.onEvent(event: CategoryListEventDeleteClicked(id: category.id))
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Categories")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    host.vm.onEvent(event: CategoryListEventAddClicked.shared)
                } label: {
                    Label("Add category", systemImage: "plus")
                }
            }
        }
        .sheet(isPresented: editorPresented) {
            if let editor = host.state.editor {
                CategoryEditorSheet(editor: editor, onEvent: { host.vm.onEvent(event: $0) })
            }
        }
        .errorAlert($errorMessage)
        .task { host.start() }
        .task {
            for await effect in host.vm.effects {
                switch onEnum(of: effect) {
                case .showDomainError(let e): errorMessage = domainErrorMessage(e.error)
                case .showError(let e): errorMessage = e.message
                }
            }
        }
    }
}

private struct CategoryEditorSheet: View {
    let editor: CategoryEditor
    let onEvent: (CategoryListEvent) -> Void

    /// Local copy of the name: typing must not wait on the ViewModel round trip, or fast input drops characters.
    @State private var name: String
    @ScaledMetric(relativeTo: .body) private var swatch = 32

    init(editor: CategoryEditor, onEvent: @escaping (CategoryListEvent) -> Void) {
        self.editor = editor
        self.onEvent = onEvent
        _name = State(initialValue: editor.name)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Name", text: $name)
                        .onChange(of: name) { onEvent(CategoryListEventNameChanged(v: name)) }
                    if let error = editor.error {
                        Text(domainErrorMessage(error)).font(.footnote).foregroundStyle(.red)
                    }
                }
                Section("Color") {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: swatch + 8), spacing: 12)], spacing: 12) {
                        ForEach(kotlinList(ColorKeys.shared.all, as: String.self), id: \.self) { key in
                            Button {
                                onEvent(CategoryListEventColorChanged(key: key))
                            } label: {
                                Circle()
                                    .fill(Palette.color(key))
                                    .frame(width: swatch, height: swatch)
                                    .overlay {
                                        if key == editor.colorKey {
                                            Circle().strokeBorder(Color.primary, lineWidth: 3)
                                        }
                                    }
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Color \(key)")
                            .accessibilityAddTraits(key == editor.colorKey ? .isSelected : [])
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle(editor.id == nil ? "New category" : "Edit category")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onEvent(CategoryListEventDismissEditor.shared) }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { onEvent(CategoryListEventSaveEditor.shared) }
                        .disabled(editor.isSaving || name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
