package pet.shared.data

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import pet.shared.domain.error.DomainError
import pet.shared.domain.error.Outcome
import pet.shared.testutil.RecordingClient
import pet.shared.testutil.TEST_REQUEST_ID
import pet.shared.testutil.characterJson
import pet.shared.testutil.json
import pet.shared.testutil.pageJson
import pet.shared.testutil.repositoryFor

class CharacterRepositoryTest {

    private fun pagedHandler(failPage: Int? = null, status: HttpStatusCode = HttpStatusCode.InternalServerError) =
        RecordingClient { request ->
            val page = request.url.parameters["page"]?.toInt() ?: 1
            when {
                page == failPage -> json("{}", status)
                page == 1 -> json(pageJson(1..3, hasNext = true))
                page == 2 -> json(pageJson(4..6, hasNext = false))
                else -> json("""{"error":"There is nothing here"}""", HttpStatusCode.NotFound)
            }
        }

    @Test
    fun loadNextPage_appendsPagesInOrder() = runTest {
        val repository = repositoryFor(pagedHandler())

        repository.loadNextPage()
        assertEquals(listOf(1, 2, 3), repository.characters.value.items.map { it.id })
        assertTrue(repository.characters.value.hasMore)

        repository.loadNextPage()
        assertEquals(listOf(1, 2, 3, 4, 5, 6), repository.characters.value.items.map { it.id })
        assertFalse(repository.characters.value.hasMore)
    }

    @Test
    fun loadNextPage_afterLastPage_doesNotHitTheNetwork() = runTest {
        val recording = pagedHandler()
        val repository = repositoryFor(recording)
        repository.loadNextPage()
        repository.loadNextPage()
        val requestsSoFar = recording.requests.size

        assertIs<Outcome.Success<Unit>>(repository.loadNextPage())
        assertEquals(requestsSoFar, recording.requests.size)
    }

    @Test
    fun pageBeyondTheEnd_404_meansNoMoreData_notAnError() = runTest {
        // The server says "has next" but the following page is missing.
        val recording = RecordingClient { request ->
            if (request.url.parameters["page"] == "1") json(pageJson(1..2, hasNext = true))
            else json("""{"error":"There is nothing here"}""", HttpStatusCode.NotFound)
        }
        val repository = repositoryFor(recording)
        repository.loadNextPage()

        assertIs<Outcome.Success<Unit>>(repository.loadNextPage())
        assertFalse(repository.characters.value.hasMore)
        assertEquals(2, repository.characters.value.items.size)
    }

    @Test
    fun failedPage_keepsLoadedData_andCanBeRetried() = runTest {
        var failing = true
        val recording = RecordingClient { request ->
            val page = request.url.parameters["page"]?.toInt() ?: 1
            if (page == 2 && failing) json("{}", HttpStatusCode.InternalServerError)
            else if (page == 1) json(pageJson(1..3, hasNext = true))
            else json(pageJson(4..6, hasNext = false))
        }
        val repository = repositoryFor(recording)
        repository.loadNextPage()

        val failure = assertIs<Outcome.Failure>(repository.loadNextPage())
        assertEquals(DomainError.ServerFailure(TEST_REQUEST_ID), failure.error)
        assertEquals(3, repository.characters.value.items.size)

        failing = false
        assertIs<Outcome.Success<Unit>>(repository.loadNextPage())
        assertEquals(6, repository.characters.value.items.size)
    }

    @Test
    fun duplicatesAcrossPagesAreIgnored() = runTest {
        val recording = RecordingClient { request ->
            if (request.url.parameters["page"] == "1") json(pageJson(1..3, hasNext = true))
            else json(pageJson(3..5, hasNext = false))
        }
        val repository = repositoryFor(recording)
        repository.loadNextPage()
        repository.loadNextPage()

        assertEquals(listOf(1, 2, 3, 4, 5), repository.characters.value.items.map { it.id })
    }

    @Test
    fun refresh_replacesTheDataAndRestartsPagination() = runTest {
        val repository = repositoryFor(pagedHandler())
        repository.loadNextPage()
        repository.loadNextPage()

        assertIs<Outcome.Success<Unit>>(repository.refresh())
        assertEquals(listOf(1, 2, 3), repository.characters.value.items.map { it.id })
        assertTrue(repository.characters.value.hasMore)

        repository.loadNextPage()
        assertEquals(6, repository.characters.value.items.size)
    }

    @Test
    fun failedRefresh_keepsTheOldData() = runTest {
        var failing = false
        val recording = RecordingClient {
            if (failing) json("{}", HttpStatusCode.ServiceUnavailable) else json(pageJson(1..3, hasNext = true))
        }
        val repository = repositoryFor(recording)
        repository.loadNextPage()

        failing = true
        assertIs<Outcome.Failure>(repository.refresh())
        assertEquals(3, repository.characters.value.items.size)
    }

    @Test
    fun overlappingLoads_runOnlyOneRequest() = runTest {
        val recording = RecordingClient {
            delay(50)
            json(pageJson(1..3, hasNext = true))
        }
        val repository = repositoryFor(recording)

        val first = async { repository.loadNextPage() }
        yield() // let the first call acquire the lock and start its request
        val second = repository.loadNextPage()
        first.await()

        assertIs<Outcome.Success<Unit>>(second)
        assertEquals(1, recording.requests.size)
        assertEquals(3, repository.characters.value.items.size)
    }

    @Test
    fun getCharacter_isServedFromLoadedData() = runTest {
        val recording = pagedHandler()
        val repository = repositoryFor(recording)
        repository.loadNextPage()
        val requestsAfterList = recording.requests.size

        val result = assertIs<Outcome.Success<*>>(repository.getCharacter(2))
        assertEquals(2, (result.value as pet.shared.domain.model.Character).id)
        assertEquals(requestsAfterList, recording.requests.size)
    }

    @Test
    fun getCharacter_unknownId_isFetchedFromTheNetwork() = runTest {
        val recording = RecordingClient { json(characterJson(42)) }
        val repository = repositoryFor(recording)

        val result = assertIs<Outcome.Success<pet.shared.domain.model.Character>>(repository.getCharacter(42))
        assertEquals(42, result.value.id)
        assertEquals(1, recording.requests.size)
        assertTrue(recording.requests.single().url.encodedPath.endsWith("/character/42"))
    }
}
