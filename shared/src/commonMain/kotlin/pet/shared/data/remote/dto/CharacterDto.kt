package pet.shared.data.remote.dto

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire format of the Rick and Morty API. */
@Serializable
internal enum class CharacterStatusDto {
    @SerialName("Alive") ALIVE,
    @SerialName("Dead") DEAD,
    @SerialName("unknown") UNKNOWN,
}

@Serializable
internal enum class GenderDto {
    @SerialName("Female") FEMALE,
    @SerialName("Male") MALE,
    @SerialName("Genderless") GENDERLESS,
    @SerialName("unknown") UNKNOWN,
}

@Serializable
internal data class PlaceDto(val name: String, val url: String = "")

@Serializable
internal data class CharacterDto(
    val id: Int,
    val name: String,
    // Defaults make `coerceInputValues` fall back to UNKNOWN if the server adds a new value.
    val status: CharacterStatusDto = CharacterStatusDto.UNKNOWN,
    val species: String,
    val gender: GenderDto = GenderDto.UNKNOWN,
    val origin: PlaceDto,
    val image: String,
    val episode: List<String>,
    val created: Instant,
)

@Serializable
internal data class PageInfoDto(val count: Int, val pages: Int, val next: String? = null)

@Serializable
internal data class CharacterPageDto(val info: PageInfoDto, val results: List<CharacterDto>)
