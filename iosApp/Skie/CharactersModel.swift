import SwiftUI
import Shared

// Interop variant: SKIE. Compare with Plain/CharactersModel.swift.

@MainActor
final class CharactersModel: ObservableObject {
    @Published private(set) var state: ViewState = .loading
    let variant = "SKIE"

    private let vm: CharactersViewModel

    init(container: AppContainer) {
        vm = container.makeCharactersViewModel()
    }

    /// Runs for as long as the calling Task.
    func observe() async {
        for await value in vm.state {
            state = Self.map(value)
        }
    }

    func loadNext() { vm.loadNext() }
    func retry() { vm.retry() }
    func refresh() { vm.refresh() }

    deinit { vm.close() }

    private static func map(_ state: CharactersUiState) -> ViewState {
        switch onEnum(of: state) {
        case .loading:
            return .loading
        case .error(let failure):
            return .error(ErrorInfo(failure.error))
        case .content(let content):
            return .content(
                content.items.map(CharacterRow.init),
                canLoadMore: content.canLoadMore,
                loadingMore: content.isLoadingMore,
                error: content.error.map(ErrorInfo.init)
            )
        }
    }
}

extension CharacterRow {
    init(_ ui: CharacterUi) {
        let status: RowStatus
        switch ui.status {
        case .alive: status = .alive
        case .dead: status = .dead
        case .unknown: status = .unknown
        }
        self.init(
            id: Int(ui.id),
            name: ui.name,
            status: status,
            statusLabel: ui.statusLabel,
            species: ui.species,
            gender: ui.genderLabel,
            origin: ui.origin,
            imageURL: URL(string: ui.imageUrl),
            episodes: Int(ui.episodeCount),
            created: Date(timeIntervalSince1970: TimeInterval(ui.createdAtEpochMillis) / 1000)
        )
    }
}

extension ErrorInfo {
    init(_ ui: ErrorUi) {
        self.init(message: ui.message, requestId: ui.requestId, isRetryable: ui.isRetryable)
    }
}
