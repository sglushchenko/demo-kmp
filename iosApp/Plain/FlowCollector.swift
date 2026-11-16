import Shared

/// Collects an untyped Kotlin Flow (no SKIE): values arrive as `Any?` and are cast to `T`.
final class Collector<T>: Kotlinx_coroutines_coreFlowCollector {
    private let onValue: (T) -> Void

    init(_ onValue: @escaping (T) -> Void) { self.onValue = onValue }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        if let typed = value as? T { onValue(typed) }
        completionHandler(nil)
    }
}
