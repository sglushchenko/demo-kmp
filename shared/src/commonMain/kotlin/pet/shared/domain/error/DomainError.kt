package pet.shared.domain.error

/**
 * Every failure the domain can report. Data-layer exceptions are translated into these,
 * so upper layers never depend on Ktor or any other transport detail.
 *
 * [requestId] is the correlation id sent to the server as `X-Request-Id`; it makes a failure traceable.
 */
sealed interface DomainError {
    val requestId: String

    /** The device is offline or the host could not be reached. */
    data class NoConnection(override val requestId: String) : DomainError

    data class Timeout(override val requestId: String) : DomainError

    /** HTTP 404. */
    data class NotFound(override val requestId: String) : DomainError

    /** HTTP 5xx. */
    data class Server(val statusCode: Int, override val requestId: String) : DomainError

    /** Any other HTTP 4xx. */
    data class Client(val statusCode: Int, override val requestId: String) : DomainError

    /** The response arrived but could not be decoded. */
    data class InvalidResponse(override val requestId: String) : DomainError

    data class Unknown(val details: String?, override val requestId: String) : DomainError
}
