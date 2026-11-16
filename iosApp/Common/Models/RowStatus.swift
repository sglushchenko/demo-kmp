import SwiftUI

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
