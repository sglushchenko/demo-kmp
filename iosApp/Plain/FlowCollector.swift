import Shared

/// Without SKIE a Kotlin `Flow<T>` arrives as an untyped `Kotlinx_coroutines_coreFlow`.
/// You implement `FlowCollector` yourself and cast the `Any?` you receive back to the expected type.
final class Collector<T>: Kotlinx_coroutines_coreFlowCollector {
    private let onValue: (T) -> Void

    init(_ onValue: @escaping (T) -> Void) { self.onValue = onValue }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        if let typed = value as? T { onValue(typed) }
        completionHandler(nil)
    }
}
