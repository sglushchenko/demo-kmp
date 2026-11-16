package pet.shared.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import pet.shared.data.remote.dto.CharacterDto
import pet.shared.data.remote.dto.CharacterPageDto

/** Thin wrapper over the HTTP endpoints. Throws; translation to domain errors happens in [safeApiCall]. */
internal class CharacterApi(
    private val client: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    /** GET /character?page={page} */
    suspend fun getPage(page: Int, requestId: String): CharacterPageDto =
        client.get("$baseUrl/character") {
            parameter("page", page)
            header(REQUEST_ID_HEADER, requestId)
        }.body()

    /** GET /character/{id} */
    suspend fun getCharacter(id: Int, requestId: String): CharacterDto =
        client.get("$baseUrl/character/$id") {
            header(REQUEST_ID_HEADER, requestId)
        }.body()
}
