import SwiftUI
import Shared

// VARIANT A: plain Kotlin/Native -> Objective-C export, no extra tooling.

@MainActor
final class CharactersModel: ObservableObject {
    @Published private(set) var state: ViewState = .loading
    let variant = "Obj-C"

    // Kotlin default arguments are invisible to Swift, so the container exposes explicit factories.
    private let vm = AppContainer.shared.makeCharactersViewModel()

    /// The subscription lives exactly as long as the calling Task (SwiftUI `.task`).
    /// Cancelling the Task cancels the Kotlin `collect`.
    func observe() async {
        do {
            try await vm.state.collect(collector: Collector<CharactersUiState> { [weak self] value in
                Task { @MainActor in self?.state = Self.map(value) }
            })
        } catch {
            // Cancellation is the normal way this ends; anything else would be a bridging failure.
        }
    }

    func loadNext() { vm.loadNext() }
    func retry() { vm.retry() }
    func refresh() { vm.refresh() }

    // Closes the Kotlin CoroutineScope when the Swift model goes away.
    deinit { vm.close() }

    // A Kotlin sealed interface arrives as a protocol plus classes: `as?` casts, no exhaustiveness check.
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
        // The Kotlin enum is a class: compare against its static instances.
        let status: RowStatus =
            ui.status == CharacterStatus.alive ? .alive :
            ui.status == CharacterStatus.dead ? .dead : .unknown
        self.init(
            id: Int(ui.id),                                   // Int32 -> Int
            name: ui.name,
            status: status,
            statusLabel: ui.statusLabel,
            species: ui.species,
            gender: ui.genderLabel,
            origin: ui.origin,
            imageURL: URL(string: ui.imageUrl),
            episodes: Int(ui.episodeCount),
            created: Date(timeIntervalSince1970: TimeInterval(ui.createdAtEpochMillis) / 1000)  // Int64 millis -> Date
        )
    }
}

extension ErrorInfo {
    init(_ ui: ErrorUi) {
        self.init(message: ui.message, requestId: ui.requestId, isRetryable: ui.isRetryable)
    }
}
