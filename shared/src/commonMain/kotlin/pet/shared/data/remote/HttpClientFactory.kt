package pet.shared.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal const val DEFAULT_BASE_URL = "https://rickandmortyapi.com/api"
internal const val REQUEST_ID_HEADER = "X-Request-Id"

internal val apiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/** Shared by production and tests (tests pass a mock engine). */
internal fun HttpClientConfig<*>.configureClient() {
    // Non-2xx responses throw, so every HTTP failure goes through one error-mapping path.
    expectSuccess = true
    install(ContentNegotiation) { json(apiJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 15_000
    }
}

/** The engine is chosen per platform from the classpath (Darwin on iOS). */
internal fun createHttpClient(): HttpClient = HttpClient { configureClient() }
