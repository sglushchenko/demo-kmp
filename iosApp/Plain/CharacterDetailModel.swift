import SwiftUI
import Shared

@MainActor
final class CharacterDetailModel: ObservableObject {
    @Published private(set) var state: DetailViewState = .loading

    private let vm: CharacterDetailViewModel

    init(characterId: Int) {
        vm = AppContainer.shared.makeCharacterDetailViewModel(characterId: Int32(characterId))
    }

    func observe() async {
        do {
            try await vm.state.collect(collector: Collector<CharacterDetailUiState> { [weak self] value in
                Task { @MainActor in self?.state = Self.map(value) }
            })
        } catch {
            // Cancellation ends the subscription.
        }
    }

    func retry() { vm.retry() }

    deinit { vm.close() }

    private static func map(_ state: CharacterDetailUiState) -> DetailViewState {
        if let content = state as? CharacterDetailUiStateContent {
            return .content(CharacterRow(content.character))
        } else if let failure = state as? CharacterDetailUiStateError {
            return .error(ErrorInfo(failure.error))
        }
        return .loading
    }
}
