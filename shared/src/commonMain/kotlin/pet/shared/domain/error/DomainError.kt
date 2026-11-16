package pet.shared.domain.error

/** Failures the domain reports, in business terms. [requestId] identifies the failed operation in logs. */
internal sealed interface DomainError {
    val requestId: String

    /** The device is offline or the service could not be reached. */
    data class NoConnection(override val requestId: String) : DomainError

    data class Timeout(override val requestId: String) : DomainError

    /** The requested item does not exist. */
    data class NotFound(override val requestId: String) : DomainError

    /** The service failed on its side; trying again later may help. */
    data class ServerFailure(override val requestId: String) : DomainError

    /** The service refused the request; repeating it unchanged will not help. */
    data class RequestRejected(override val requestId: String) : DomainError

    /** The answer arrived but could not be understood. */
    data class InvalidResponse(override val requestId: String) : DomainError

    data class Unknown(val details: String?, override val requestId: String) : DomainError
}
