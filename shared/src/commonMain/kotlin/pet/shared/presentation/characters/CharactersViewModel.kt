package pet.shared.presentation.characters

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
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

/** Presents the repository data. It owns none, so a new instance reuses what is already loaded. */
class CharactersViewModel internal constructor(
    private val observeCharacters: ObserveCharactersUseCase,
    private val loadNextPage: LoadNextCharactersPageUseCase,
    private val refreshCharacters: RefreshCharactersUseCase,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private enum class LoadKind { NEXT_PAGE, REFRESH }

    private data class LoadState(
        val isLoading: Boolean = false,
        val kind: LoadKind = LoadKind.NEXT_PAGE,
        val error: DomainError? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val snapshots: StateFlow<CharactersSnapshot> = observeCharacters()
    private val loadState = MutableStateFlow(LoadState())

    // Declared before `state` so that its initial value is already Loading.
    init {
        if (snapshots.value.items.isEmpty()) loadNext()
    }

    val state: StateFlow<CharactersUiState> =
        combine(snapshots, loadState) { snapshot, load -> reduce(snapshot, load) }
            .stateIn(scope, SharingStarted.Eagerly, reduce(snapshots.value, loadState.value))

    fun loadNext() {
        val snapshot = snapshots.value
        if (snapshot.items.isNotEmpty() && !snapshot.hasMore) return
        launchLoad(LoadKind.NEXT_PAGE) { loadNextPage() }
    }

    fun refresh() = launchLoad(LoadKind.REFRESH) { refreshCharacters() }

    /** Repeats the last operation that was started. */
    fun retry() = when (loadState.value.kind) {
        LoadKind.NEXT_PAGE -> loadNext()
        LoadKind.REFRESH -> refresh()
    }

    fun close() = scope.cancel()

    // Atomic: only one caller can flip `isLoading`, concurrent calls return without starting anything.
    private fun launchLoad(kind: LoadKind, block: suspend () -> Outcome<Unit>) {
        val previous = loadState.getAndUpdate { if (it.isLoading) it else LoadState(isLoading = true, kind = kind) }
        if (previous.isLoading) return

        scope.launch {
            val error = (block() as? Outcome.Failure)?.error
            loadState.value = LoadState(isLoading = false, kind = kind, error = error)
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
