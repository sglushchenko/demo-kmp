import SwiftUI
import Shared

struct ContentView: View {
    @StateObject private var model = CharactersModel()

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
            .navigationDestination(for: Int.self) { CharacterDetailView(characterId: $0) }
            .toolbar {
                Button { model.refresh() } label: { Image(systemName: "arrow.clockwise") }
            }
        }
        // `.task` starts when the view appears and is cancelled automatically when it disappears.
        // On re-appear it subscribes again and the StateFlow immediately re-delivers the current value.
        .task { await model.observe() }
    }

    private var footer: some View {
        Text("Shared Kotlin on \(AppContainer.shared.platform) | \(model.variant) | session \(AppContainer.shared.sessionId.prefix(8))")
    }
}

struct CharacterRowView: View {
    let row: CharacterRow

    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: row.imageURL) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                Color.gray.opacity(0.2)
            }
            .frame(width: 64, height: 64)
            .clipShape(RoundedRectangle(cornerRadius: 10))

            VStack(alignment: .leading, spacing: 3) {
                Text(row.name).font(.headline)
                HStack(spacing: 6) {
                    Circle().fill(row.status.color).frame(width: 8, height: 8)
                    Text("\(row.statusLabel) | \(row.species)").font(.subheadline)
                }
                Text("Origin: \(row.origin)").font(.caption).foregroundStyle(.secondary)
            }
        }
    }
}
