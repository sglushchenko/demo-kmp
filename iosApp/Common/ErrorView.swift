import SwiftUI

struct ErrorView: View {
    let error: ErrorInfo
    let retry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle").font(.largeTitle).foregroundStyle(.orange)
            Text(error.message).multilineTextAlignment(.center)
            Text("Request ID: \(error.requestId)")
                .font(.caption2.monospaced())
                .foregroundStyle(.secondary)
                .textSelection(.enabled)
            if error.isRetryable {
                Button("Retry", action: retry).buttonStyle(.borderedProminent)
            }
        }
        .padding()
    }
}
