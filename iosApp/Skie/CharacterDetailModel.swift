import SwiftUI
import Shared

@MainActor
final class CharacterDetailModel: ObservableObject {
    @Published private(set) var state: DetailViewState = .loading

    private let vm: CharacterDetailViewModel

    init(characterId: Int, container: AppContainer) {
        vm = container.makeCharacterDetailViewModel(characterId: Int32(characterId))
    }

    func observe() async {
        for await value in vm.state {
            state = Self.map(value)
        }
    }

    func retry() { vm.retry() }

    deinit { vm.close() }

    private static func map(_ state: CharacterDetailUiState) -> DetailViewState {
        switch onEnum(of: state) {
        case .loading: return .loading
        case .error(let failure): return .error(ErrorInfo(failure.error))
        case .content(let content): return .content(CharacterRow(content.character))
        }
    }
}
