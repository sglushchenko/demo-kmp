package pet.shared.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pet.shared.data.mapper.toDomain
import pet.shared.data.remote.CharacterApi
import pet.shared.data.remote.randomRequestId
import pet.shared.data.remote.safeApiCall
import pet.shared.domain.error.DomainError
import pet.shared.domain.error.Outcome
import pet.shared.domain.error.map
import pet.shared.domain.model.Character
import pet.shared.domain.model.CharactersSnapshot
import pet.shared.domain.repository.CharacterRepository

private const val FIRST_PAGE = 1

/** In-memory cache backed by the network. One instance serves the whole app. */
internal class CharacterRepositoryImpl(
    private val api: CharacterApi,
    private val newRequestId: () -> String = ::randomRequestId,
) : CharacterRepository {

    private val state = MutableStateFlow(CharactersSnapshot.EMPTY)
    override val characters: StateFlow<CharactersSnapshot> = state.asStateFlow()

    private val loadLock = Mutex()
    private var nextPage = FIRST_PAGE

    override suspend fun loadNextPage(): Outcome<Unit> {
        // Skip instead of queueing, so fast scrolling cannot request the same page twice.
        if (!loadLock.tryLock()) return Outcome.Success(Unit)
        try {
            val current = state.value
            if (nextPage > FIRST_PAGE && !current.hasMore) return Outcome.Success(Unit)

            return when (val result = fetchPage(nextPage)) {
                is Outcome.Success -> {
                    val page = result.value
                    state.value = CharactersSnapshot(
                        items = (current.items + page.items).distinctBy { it.id },
                        hasMore = page.hasMore,
                    )
                    nextPage++
                    Outcome.Success(Unit)
                }
                is Outcome.Failure ->
                    // The API answers 404 for a page past the end: that means "no more data", not an error.
                    if (result.error is DomainError.NotFound && nextPage > FIRST_PAGE) {
                        state.value = current.copy(hasMore = false)
                        Outcome.Success(Unit)
                    } else {
                        result
                    }
            }
        } finally {
            loadLock.unlock()
        }
    }

    override suspend fun refresh(): Outcome<Unit> = loadLock.withLock {
        when (val result = fetchPage(FIRST_PAGE)) {
            is Outcome.Success -> {
                state.value = CharactersSnapshot(result.value.items.distinctBy { it.id }, result.value.hasMore)
                nextPage = FIRST_PAGE + 1
                Outcome.Success(Unit)
            }
            is Outcome.Failure -> result // keep whatever is already loaded
        }
    }

    override suspend fun getCharacter(id: Int): Outcome<Character> {
        state.value.items.firstOrNull { it.id == id }?.let { return Outcome.Success(it) }
        return safeApiCall(newRequestId) { requestId -> api.getCharacter(id, requestId) }
            .map { it.toDomain() }
    }

    private suspend fun fetchPage(page: Int): Outcome<PageResult> =
        safeApiCall(newRequestId) { requestId -> api.getPage(page, requestId) }
            .map { dto -> PageResult(dto.results.map { it.toDomain() }, hasMore = dto.info.next != null) }

    private class PageResult(val items: List<Character>, val hasMore: Boolean)
}
