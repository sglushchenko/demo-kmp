package pet.shared.testutil

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pet.shared.domain.error.Outcome
import pet.shared.domain.model.CartoonCharacter
import pet.shared.domain.model.CharactersSnapshot
import pet.shared.domain.repository.CharacterRepository

/** Hand-written fake repository for ViewModel tests. */
internal class FakeCharacterRepository(initial: CharactersSnapshot = CharactersSnapshot.EMPTY) : CharacterRepository {
    val snapshot = MutableStateFlow(initial)
    override val characters: StateFlow<CharactersSnapshot> = snapshot

    var loadNextCalls = 0
    var refreshCalls = 0
    var onLoadNext: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
    var onRefresh: suspend () -> Outcome<Unit> = { Outcome.Success(Unit) }
    var onGetCharacter: suspend (Int) -> Outcome<CartoonCharacter> = { Outcome.Success(testCharacter(it)) }

    override suspend fun loadNextPage(): Outcome<Unit> {
        loadNextCalls++
        return onLoadNext()
    }

    override suspend fun refresh(): Outcome<Unit> {
        refreshCalls++
        return onRefresh()
    }

    override suspend fun getCharacter(id: Int): Outcome<CartoonCharacter> = onGetCharacter(id)
}
