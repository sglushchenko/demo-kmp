package pet.shared.data.remote

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import pet.shared.domain.error.DomainError
import pet.shared.domain.error.Outcome

@OptIn(ExperimentalUuidApi::class)
internal fun randomRequestId(): String = Uuid.random().toString()

/** Runs [block] with a fresh request id and turns any failure into [Outcome.Failure]; cancellation is rethrown. */
internal suspend fun <T> safeApiCall(
    newRequestId: () -> String,
    block: suspend (requestId: String) -> T,
): Outcome<T> {
    val requestId = newRequestId()
    return try {
        Outcome.Success(block(requestId))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Outcome.Failure(e.toDomainError(requestId))
    }
}

internal fun Exception.toDomainError(requestId: String): DomainError = when (this) {
    is ClientRequestException ->
        if (response.status == HttpStatusCode.NotFound) DomainError.NotFound(requestId)
        else DomainError.Client(response.status.value, requestId)

    is ServerResponseException -> DomainError.Server(response.status.value, requestId)

    // Timeouts extend IOException, so they must be checked first.
    is HttpRequestTimeoutException,
    is ConnectTimeoutException,
    is SocketTimeoutException -> DomainError.Timeout(requestId)

    is SerializationException,
    is ContentConvertException,
    is NoTransformationFoundException -> DomainError.InvalidResponse(requestId)

    is IOException -> DomainError.NoConnection(requestId)

    else -> DomainError.Unknown(message, requestId)
}
