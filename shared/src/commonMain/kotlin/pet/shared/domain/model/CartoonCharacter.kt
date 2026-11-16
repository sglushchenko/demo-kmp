package pet.shared.domain.model

import kotlin.time.Instant

internal data class CartoonCharacter(
    val id: Int,
    val name: String,
    val status: CharacterStatus,
    val species: String,
    val gender: Gender,
    val origin: String,
    val imageUrl: String,
    val episodeCount: Int,
    val createdAt: Instant,
)

enum class CharacterStatus { ALIVE, DEAD, UNKNOWN }

internal enum class Gender { FEMALE, MALE, GENDERLESS, UNKNOWN }

/** Everything loaded so far plus whether another page can be requested. */
internal data class CharactersSnapshot(
    val items: List<CartoonCharacter>,
    val hasMore: Boolean,
) {
    companion object {
        val EMPTY = CharactersSnapshot(items = emptyList(), hasMore = true)
    }
}
