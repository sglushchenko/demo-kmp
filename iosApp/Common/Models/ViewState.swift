enum ViewState {
    case loading
    /// `error` is non-blocking (for example a failed next page); the list stays visible.
    case content([CharacterRow], canLoadMore: Bool, loadingMore: Bool, error: ErrorInfo?)
    case error(ErrorInfo)
}
