package pet.shared.presentation.characters

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pet.shared.domain.error.DomainError
import pet.shared.domain.error.Outcome
import pet.shared.domain.model.CharactersSnapshot
import pet.shared.domain.usecase.LoadNextCharactersPageUseCase
import pet.shared.domain.usecase.ObserveCharactersUseCase
import pet.shared.domain.usecase.RefreshCharactersUseCase
import pet.shared.presentation.model.CharacterUi
import pet.shared.presentation.model.ErrorUi
import pet.shared.presentation.model.toUi

sealed interface CharactersUiState {
    data object Loading : CharactersUiState

    /** [error] is a non-blocking failure (for example while loading the next page); the list stays visible. */
    data class Content(
        val items: List<CharacterUi>,
        val canLoadMore: Boolean,
        val isLoadingMore: Boolean,
        val error: ErrorUi?,
    ) : CharactersUiState

    /** Blocking failure: nothing could be loaded at all. */
    data class Error(val error: ErrorUi) : CharactersUiState
}

/**
 * Presents the shared repository data. It owns no data itself, so creating another instance
 * (for another screen or UI toolkit) shows what is already loaded without hitting the network again.
 */
class CharactersViewModel(
    private val observeCharacters: ObserveCharactersUseCase,
    private val loadNextPage: LoadNextCharactersPageUseCase,
    private val refreshCharacters: RefreshCharactersUseCase,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private data class LoadState(val isLoading: Boolean, val error: DomainError? = null)

    private enum class LoadKind { NEXT_PAGE, REFRESH }

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val snapshots: StateFlow<CharactersSnapshot> = observeCharacters()
    private val loadState = MutableStateFlow(LoadState(isLoading = snapshots.value.items.isEmpty()))
    private var loadJob: Job? = null
    private var lastLoad = LoadKind.NEXT_PAGE

    val state: StateFlow<CharactersUiState> =
        combine(snapshots, loadState) { snapshot, load -> reduce(snapshot, load) }
            .stateIn(scope, SharingStarted.Eagerly, reduce(snapshots.value, loadState.value))

    init {
        if (snapshots.value.items.isEmpty()) loadNext()
    }

    fun loadNext() {
        val snapshot = snapshots.value
        if (snapshot.items.isNotEmpty() && !snapshot.hasMore) return
        launchLoad(LoadKind.NEXT_PAGE) { loadNextPage() }
    }

    fun refresh() = launchLoad(LoadKind.REFRESH) { refreshCharacters() }

    /** Repeats the operation that was started last, so a failed refresh is refreshed again rather than paging. */
    fun retry() = when (lastLoad) {
        LoadKind.NEXT_PAGE -> loadNext()
        LoadKind.REFRESH -> refresh()
    }

    fun close() = scope.cancel()

    // A load that is already running wins; extra calls are ignored.
    private fun launchLoad(kind: LoadKind, block: suspend () -> Outcome<Unit>) {
        if (loadJob?.isActive == true) return
        lastLoad = kind
        loadState.value = LoadState(isLoading = true)
        loadJob = scope.launch {
            loadState.value = when (val result = block()) {
                is Outcome.Success -> LoadState(isLoading = false)
                is Outcome.Failure -> LoadState(isLoading = false, error = result.error)
            }
        }
    }

    private fun reduce(snapshot: CharactersSnapshot, load: LoadState): CharactersUiState = when {
        snapshot.items.isEmpty() && load.error != null -> CharactersUiState.Error(load.error.toUi())
        snapshot.items.isEmpty() && load.isLoading -> CharactersUiState.Loading
        else -> CharactersUiState.Content(
            items = snapshot.items.map { it.toUi() },
            canLoadMore = snapshot.hasMore,
            isLoadingMore = load.isLoading,
            error = load.error?.toUi(),
        )
    }
}
