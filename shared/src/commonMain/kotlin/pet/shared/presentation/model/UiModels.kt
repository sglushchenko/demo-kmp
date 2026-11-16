package pet.shared.presentation.model

import pet.shared.domain.error.DomainError
import pet.shared.domain.model.Character
import pet.shared.domain.model.CharacterStatus
import pet.shared.domain.model.Gender

/** What the UI needs to draw a character: display strings are prepared here, not in each UI toolkit. */
data class CharacterUi(
    val id: Int,
    val name: String,
    val status: CharacterStatus,
    val statusLabel: String,
    val species: String,
    val genderLabel: String,
    val origin: String,
    val imageUrl: String,
    val episodeCount: Int,
    val createdAtEpochMillis: Long,
)

/** A user-facing error. [requestId] helps support trace the failing request. */
data class ErrorUi(
    val message: String,
    val requestId: String,
    val isRetryable: Boolean,
)

internal fun Character.toUi() = CharacterUi(
    id = id,
    name = name,
    status = status,
    statusLabel = when (status) {
        CharacterStatus.ALIVE -> "Alive"
        CharacterStatus.DEAD -> "Dead"
        CharacterStatus.UNKNOWN -> "Unknown"
    },
    species = species,
    genderLabel = when (gender) {
        Gender.FEMALE -> "Female"
        Gender.MALE -> "Male"
        Gender.GENDERLESS -> "Genderless"
        Gender.UNKNOWN -> "Unknown"
    },
    origin = origin,
    imageUrl = imageUrl,
    episodeCount = episodeCount,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
)

internal fun DomainError.toUi(): ErrorUi = when (this) {
    is DomainError.NoConnection ->
        ErrorUi("No internet connection. Check your network and try again.", requestId, isRetryable = true)
    is DomainError.Timeout ->
        ErrorUi("The request timed out. Please try again.", requestId, isRetryable = true)
    is DomainError.NotFound ->
        ErrorUi("The requested item was not found.", requestId, isRetryable = false)
    is DomainError.ServerFailure ->
        ErrorUi("The server is having problems. Please try again later.", requestId, isRetryable = true)
    is DomainError.RequestRejected ->
        ErrorUi("The request was rejected.", requestId, isRetryable = false)
    is DomainError.InvalidResponse ->
        ErrorUi("The server sent a response the app could not understand.", requestId, isRetryable = false)
    is DomainError.Unknown ->
        ErrorUi("Something went wrong.", requestId, isRetryable = true)
}
