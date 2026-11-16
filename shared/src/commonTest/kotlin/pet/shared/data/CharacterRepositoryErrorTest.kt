package pet.shared.data

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pet.shared.data.remote.REQUEST_ID_HEADER
import pet.shared.domain.error.DomainError
import pet.shared.domain.error.Outcome
import pet.shared.domain.model.CharacterStatus
import pet.shared.testutil.RecordingClient
import pet.shared.testutil.TEST_REQUEST_ID
import pet.shared.testutil.characterJson
import pet.shared.testutil.json
import pet.shared.testutil.pageJson
import pet.shared.testutil.repositoryFor

/** Every failure of every endpoint must surface as a typed [DomainError], never as a raw exception. */
class CharacterRepositoryErrorTest {

    private suspend fun errorOfDetail(handler: RecordingClient): DomainError {
        val result = repositoryFor(handler).getCharacter(1)
        return assertIs<Outcome.Failure>(result).error
    }

    private suspend fun errorOfPage(handler: RecordingClient): DomainError {
        val result = repositoryFor(handler).loadNextPage()
        return assertIs<Outcome.Failure>(result).error
    }

    // --- GET /character/{id} ---

    @Test
    fun getCharacter_404_isNotFound() = runTest {
        val error = errorOfDetail(RecordingClient { json("""{"error":"Character not found"}""", HttpStatusCode.NotFound) })
        assertEquals(DomainError.NotFound(TEST_REQUEST_ID), error)
    }

    @Test
    fun getCharacter_500_isServerFailure() = runTest {
        val error = errorOfDetail(RecordingClient { json("{}", HttpStatusCode.InternalServerError) })
        assertEquals(DomainError.ServerFailure(TEST_REQUEST_ID), error)
    }

    @Test
    fun getCharacter_429_isRequestRejected() = runTest {
        val error = errorOfDetail(RecordingClient { json("{}", HttpStatusCode.TooManyRequests) })
        assertEquals(DomainError.RequestRejected(TEST_REQUEST_ID), error)
    }

    @Test
    fun getCharacter_malformedBody_isInvalidResponse() = runTest {
        val error = errorOfDetail(RecordingClient { json("this is not json") })
        assertEquals(DomainError.InvalidResponse(TEST_REQUEST_ID), error)
    }

    @Test
    fun getCharacter_missingRequiredField_isInvalidResponse() = runTest {
        val error = errorOfDetail(RecordingClient { json("""{"id":1}""") })
        assertEquals(DomainError.InvalidResponse(TEST_REQUEST_ID), error)
    }

    @Test
    fun getCharacter_ioFailure_isNoConnection() = runTest {
        val error = errorOfDetail(RecordingClient { throw IOException("offline") })
        assertEquals(DomainError.NoConnection(TEST_REQUEST_ID), error)
    }

    @Test
    fun getCharacter_timeout_isTimeout() = runTest {
        val error = errorOfDetail(RecordingClient { throw HttpRequestTimeoutException("https://example.com", 1_000L) })
        assertEquals(DomainError.Timeout(TEST_REQUEST_ID), error)
    }

    @Test
    fun getCharacter_unexpectedException_isUnknown() = runTest {
        val error = errorOfDetail(RecordingClient { throw IllegalStateException("boom") })
        assertEquals(DomainError.Unknown("boom", TEST_REQUEST_ID), error)
    }

    // --- GET /character?page={n} ---

    @Test
    fun loadNextPage_404OnFirstPage_isNotFound() = runTest {
        val error = errorOfPage(RecordingClient { json("""{"error":"There is nothing here"}""", HttpStatusCode.NotFound) })
        assertEquals(DomainError.NotFound(TEST_REQUEST_ID), error)
    }

    @Test
    fun loadNextPage_503_isServerFailure() = runTest {
        val error = errorOfPage(RecordingClient { json("{}", HttpStatusCode.ServiceUnavailable) })
        assertEquals(DomainError.ServerFailure(TEST_REQUEST_ID), error)
    }

    @Test
    fun loadNextPage_malformedBody_isInvalidResponse() = runTest {
        val error = errorOfPage(RecordingClient { json("""{"results": "wrong"}""") })
        assertEquals(DomainError.InvalidResponse(TEST_REQUEST_ID), error)
    }

    @Test
    fun loadNextPage_ioFailure_isNoConnection() = runTest {
        val error = errorOfPage(RecordingClient { throw IOException("offline") })
        assertEquals(DomainError.NoConnection(TEST_REQUEST_ID), error)
    }

    // --- Cross-cutting behaviour ---

    @Test
    fun requests_carryTheRequestIdHeader() = runTest {
        val recording = RecordingClient { json(characterJson(1)) }
        repositoryFor(recording).getCharacter(1)
        assertEquals(TEST_REQUEST_ID, recording.requests.single().headers[REQUEST_ID_HEADER])
    }

    @Test
    fun unknownEnumValues_fallBackToUnknown() = runTest {
        val recording = RecordingClient {
            json(
                """{"info":{"count":1,"pages":1,"next":null},"results":[${characterJson(7, status = "Robot", gender = "Other")}]}""",
            )
        }
        val repository = repositoryFor(recording)
        assertIs<Outcome.Success<Unit>>(repository.loadNextPage())
        assertEquals(CharacterStatus.UNKNOWN, repository.characters.value.items.single().status)
    }

    @Test
    fun loadNextPage_success_isNotAnError() = runTest {
        val result = repositoryFor(RecordingClient { json(pageJson(1..3, hasNext = false)) }).loadNextPage()
        assertIs<Outcome.Success<Unit>>(result)
    }
}
