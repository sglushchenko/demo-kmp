import os
import Shared

private let log = Logger(subsystem: Bundle.main.bundleIdentifier ?? "app", category: "kotlin-interop")

/// Receives values of an untyped Kotlin Flow (no SKIE): they arrive as `Any?` and are cast to `T`.
final class Collector<T>: Kotlinx_coroutines_coreFlowCollector {
    private let onValue: (T) -> Void

    init(_ onValue: @escaping (T) -> Void) { self.onValue = onValue }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        if let typed = value as? T { onValue(typed) }
        completionHandler(nil)
    }
}

extension Kotlinx_coroutines_coreFlow {
    /// The flow as an `AsyncStream`: the same shape SKIE generates.
    ///
    /// Values are yielded in emission order and only the newest is kept, matching `StateFlow` semantics.
    /// Cancelling the consumer cancels the Kotlin `collect`.
    func stream<T>(of type: T.Type) -> AsyncStream<T> {
        AsyncStream(bufferingPolicy: .bufferingNewest(1)) { continuation in
            let collecting = Task {
                do {
                    try await self.collect(collector: Collector<T> { continuation.yield($0) })
                } catch where !Task.isCancelled {
                    log.error("Kotlin flow collection failed: \(error.localizedDescription, privacy: .public)")
                } catch {
                    // Cancelled by the consumer.
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in collecting.cancel() }
        }
    }
}
