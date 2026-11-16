package pet.shared.domain.repository

import kotlinx.coroutines.flow.StateFlow
import pet.shared.domain.error.Outcome
import pet.shared.domain.model.Character
import pet.shared.domain.model.CharactersSnapshot

/** Single source of truth for characters: loaded data lives here, so every screen observes the same state. */
interface CharacterRepository {
    val characters: StateFlow<CharactersSnapshot>

    /** Loads the next page and appends it. A call made while another load is running is ignored. */
    suspend fun loadNextPage(): Outcome<Unit>

    /** Reloads from the first page. On failure the previously loaded data is kept. */
    suspend fun refresh(): Outcome<Unit>

    /** Served from the loaded data when possible, otherwise fetched. */
    suspend fun getCharacter(id: Int): Outcome<Character>
}
