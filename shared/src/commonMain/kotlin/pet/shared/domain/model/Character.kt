package pet.shared.domain.model

import kotlin.time.Instant

data class Character(
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

enum class Gender { FEMALE, MALE, GENDERLESS, UNKNOWN }

/** Everything loaded so far plus whether another page can be requested. */
data class CharactersSnapshot(
    val items: List<Character>,
    val hasMore: Boolean,
) {
    companion object {
        val EMPTY = CharactersSnapshot(items = emptyList(), hasMore = true)
    }
}
