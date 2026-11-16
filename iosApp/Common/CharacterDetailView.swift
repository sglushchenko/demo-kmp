import SwiftUI
import Shared

struct CharacterDetailView: View {
    @StateObject private var model: CharacterDetailModel

    init(characterId: Int, container: AppContainer) {
        _model = StateObject(wrappedValue: CharacterDetailModel(characterId: characterId, container: container))
    }

    var body: some View {
        Group {
            switch model.state {
            case .loading:
                ProgressView("Loading...")
            case .error(let error):
                ErrorView(error: error) { model.retry() }
            case .content(let row):
                ScrollView {
                    VStack(spacing: 16) {
                        AsyncImage(url: row.imageURL) { image in
                            image.resizable().scaledToFit()
                        } placeholder: {
                            Color.gray.opacity(0.2).aspectRatio(1, contentMode: .fit)
                        }
                        .clipShape(RoundedRectangle(cornerRadius: 16))

                        VStack(alignment: .leading, spacing: 8) {
                            detail("Status", row.statusLabel)
                            detail("Species", row.species)
                            detail("Gender", row.gender)
                            detail("Origin", row.origin)
                            detail("Episodes", "\(row.episodes)")
                            detail("Created", row.created.formatted(date: .long, time: .omitted))
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding()
                }
                .navigationTitle(row.name)
            }
        }
        .task { await model.observe() }
    }

    private func detail(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title).foregroundStyle(.secondary)
            Spacer()
            Text(value)
        }
    }
}
