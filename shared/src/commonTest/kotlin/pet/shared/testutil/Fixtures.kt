package pet.shared.testutil

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.time.Instant
import pet.shared.data.remote.CharacterApi
import pet.shared.data.remote.configureClient
import pet.shared.data.repository.CharacterRepositoryImpl
import pet.shared.domain.model.Character
import pet.shared.domain.model.CharacterStatus
import pet.shared.domain.model.Gender

internal const val TEST_REQUEST_ID = "test-request-id"

internal fun characterJson(id: Int, status: String = "Alive", gender: String = "Male"): String =
    """{"id":$id,"name":"Character $id","status":"$status","species":"Human","type":"","gender":"$gender",""" +
        """"origin":{"name":"Earth","url":""},"location":{"name":"Earth","url":""},""" +
        """"image":"https://example.com/$id.png","episode":["e1","e2"],"url":"","created":"2017-11-04T18:48:46.250Z"}"""

internal fun pageJson(ids: IntRange, hasNext: Boolean): String {
    val next = if (hasNext) "\"https://example.com/next\"" else "null"
    return """{"info":{"count":100,"pages":5,"next":$next,"prev":null},"results":[${ids.joinToString(",") { characterJson(it) }}]}"""
}

internal fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

/** Records every request so tests can assert how often the network was hit. */
internal class RecordingClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
    val requests = mutableListOf<HttpRequestData>()
    val client: HttpClient = HttpClient(
        MockEngine { request ->
            requests += request
            handler(request)
        },
    ) { configureClient() }
}

internal fun repositoryFor(recording: RecordingClient) =
    CharacterRepositoryImpl(CharacterApi(recording.client, baseUrl = "https://example.com/api"), newRequestId = { TEST_REQUEST_ID })

internal fun testCharacter(id: Int) = Character(
    id = id,
    name = "Character $id",
    status = CharacterStatus.ALIVE,
    species = "Human",
    gender = Gender.MALE,
    origin = "Earth",
    imageUrl = "https://example.com/$id.png",
    episodeCount = 2,
    createdAt = Instant.fromEpochMilliseconds(1_509_821_326_250),
)
