package pet.shared.data.mapper

import pet.shared.data.remote.dto.CharacterDto
import pet.shared.data.remote.dto.CharacterStatusDto
import pet.shared.data.remote.dto.GenderDto
import pet.shared.domain.model.CartoonCharacter
import pet.shared.domain.model.CharacterStatus
import pet.shared.domain.model.Gender

internal fun CharacterDto.toDomain() = CartoonCharacter(
    id = id,
    name = name,
    status = status.toDomain(),
    species = species,
    gender = gender.toDomain(),
    origin = origin.name,
    imageUrl = image,
    episodeCount = episode.size,
    createdAt = created,
)

internal fun CharacterStatusDto.toDomain() = when (this) {
    CharacterStatusDto.ALIVE -> CharacterStatus.ALIVE
    CharacterStatusDto.DEAD -> CharacterStatus.DEAD
    CharacterStatusDto.UNKNOWN -> CharacterStatus.UNKNOWN
}

internal fun GenderDto.toDomain() = when (this) {
    GenderDto.FEMALE -> Gender.FEMALE
    GenderDto.MALE -> Gender.MALE
    GenderDto.GENDERLESS -> Gender.GENDERLESS
    GenderDto.UNKNOWN -> Gender.UNKNOWN
}
