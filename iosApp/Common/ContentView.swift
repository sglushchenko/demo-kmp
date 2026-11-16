import SwiftUI
import Shared

struct ContentView: View {
    @StateObject private var model: CharactersModel
    private let container: AppContainer

    init(container: AppContainer) {
        self.container = container
        _model = StateObject(wrappedValue: CharactersModel(container: container))
    }

    var body: some View {
        NavigationStack {
            Group {
                switch model.state {
                case .loading:
                    ProgressView("Loading...")
                case .error(let error):
                    ErrorView(error: error) { model.retry() }
                case .content(let rows, let canLoadMore, let loadingMore, let error):
                    List {
                        Section(footer: footer) {
                            ForEach(rows) { row in
                                NavigationLink(value: row.id) { CharacterRowView(row: row) }
                                    .onAppear {
                                        if canLoadMore, row.id == rows.last?.id { model.loadNext() }
                                    }
                            }
                        }
                        if loadingMore {
                            HStack { Spacer(); ProgressView(); Spacer() }
                        }
                        if let error {
                            VStack(alignment: .leading, spacing: 6) {
                                Text(error.message).font(.footnote).foregroundStyle(.red)
                                if error.isRetryable {
                                    Button("Try again") { model.retry() }
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Rick & Morty")
            .navigationDestination(for: Int.self) { CharacterDetailView(characterId: $0, container: container) }
            .toolbar {
                Button { model.refresh() } label: { Image(systemName: "arrow.clockwise") }
            }
        }
        // Cancelled when the view disappears; re-subscribing re-delivers the current state.
        .task { await model.observe() }
    }

    private var footer: some View {
        Text("Shared Kotlin on \(container.platform) | \(model.variant) | session \(container.sessionId.prefix(8))")
    }
}
