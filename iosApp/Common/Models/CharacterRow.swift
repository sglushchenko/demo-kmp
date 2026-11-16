import Foundation

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
