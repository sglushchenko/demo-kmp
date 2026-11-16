import SwiftUI

// Swift-native types the UI works with. Each interop variant (Plain / SKIE) maps the Kotlin types
// into these in its own model files; that mapping code is where the interop cost shows up.

enum RowStatus {
    case alive, dead, unknown

    var color: Color {
        switch self {
        case .alive: return .green
        case .dead: return .red
        case .unknown: return .gray
        }
    }
}

struct CharacterRow: Identifiable {
    let id: Int
    let name: String
    let status: RowStatus
    let statusLabel: String
    let species: String
    let gender: String
    let origin: String
    let imageURL: URL?
    let episodes: Int
    let created: Date
}

struct ErrorInfo {
    let message: String
    let requestId: String
    let isRetryable: Bool
}

enum ViewState {
    case loading
    /// `error` is a non-blocking failure (for example loading the next page); the list stays visible.
    case content([CharacterRow], canLoadMore: Bool, loadingMore: Bool, error: ErrorInfo?)
    case error(ErrorInfo)
}

enum DetailViewState {
    case loading
    case content(CharacterRow)
    case error(ErrorInfo)
}
