enum DetailViewState {
    case loading
    case content(CharacterRow)
    case error(ErrorInfo)
}
