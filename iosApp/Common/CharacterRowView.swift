import SwiftUI

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
