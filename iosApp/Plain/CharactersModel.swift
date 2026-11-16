import SwiftUI
import Shared

// Interop variant: plain Objective-C export. Compare with Skie/CharactersModel.swift.

@MainActor
final class CharactersModel: ObservableObject {
    @Published private(set) var state: ViewState = .loading
    let variant = "Obj-C"

    private let vm = AppContainer.shared.makeCharactersViewModel()

    /// Runs for as long as the calling Task; cancelling it cancels the Kotlin `collect`.
    func observe() async {
        for await value in vm.state.stream(of: CharactersUiState.self) {
            state = Self.map(value)
        }
    }

    func loadNext() { vm.loadNext() }
    func retry() { vm.retry() }
    func refresh() { vm.refresh() }

    // ARC does not release Kotlin coroutine scopes.
    deinit { vm.close() }

    // Sealed interfaces arrive as classes, so there is no exhaustiveness check.
    private static func map(_ state: CharactersUiState) -> ViewState {
        if let content = state as? CharactersUiStateContent {
            return .content(
                content.items.map(CharacterRow.init),
                canLoadMore: content.canLoadMore,
                loadingMore: content.isLoadingMore,
                error: content.error.map(ErrorInfo.init)
            )
        } else if let failure = state as? CharactersUiStateError {
            return .error(ErrorInfo(failure.error))
        }
        return .loading
    }
}

extension CharacterRow {
    init(_ ui: CharacterUi) {
        let status: RowStatus =
            ui.status == CharacterStatus.alive ? .alive :
            ui.status == CharacterStatus.dead ? .dead : .unknown
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
