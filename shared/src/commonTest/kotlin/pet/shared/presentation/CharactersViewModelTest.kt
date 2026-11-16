package pet.shared.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import pet.shared.domain.error.DomainError
import pet.shared.domain.error.Outcome
import pet.shared.domain.model.CharactersSnapshot
import pet.shared.domain.usecase.LoadNextCharactersPageUseCase
import pet.shared.domain.usecase.ObserveCharactersUseCase
import pet.shared.domain.usecase.RefreshCharactersUseCase
import pet.shared.presentation.characters.CharactersUiState
import pet.shared.presentation.characters.CharactersViewModel
import pet.shared.testutil.FakeCharacterRepository
import pet.shared.testutil.TEST_REQUEST_ID
import pet.shared.testutil.testCharacter

@OptIn(ExperimentalCoroutinesApi::class)
class CharactersViewModelTest {

    private fun viewModel(repository: FakeCharacterRepository) = CharactersViewModel(
        observeCharacters = ObserveCharactersUseCase(repository),
        loadNextPage = LoadNextCharactersPageUseCase(repository),
        refreshCharacters = RefreshCharactersUseCase(repository),
        dispatcher = UnconfinedTestDispatcher(),
    )

    private fun FakeCharacterRepository.loadsSuccessfully(ids: IntRange, hasMore: Boolean = true) {
        onLoadNext = {
            snapshot.value = CharactersSnapshot((snapshot.value.items + ids.map(::testCharacter)), hasMore)
            Outcome.Success(Unit)
        }
    }

    @Test
    fun load_showsLoadingThenContent() {
        val repository = FakeCharacterRepository()
        val gate = CompletableDeferred<Unit>()
        repository.onLoadNext = {
            gate.await()
            repository.snapshot.value = CharactersSnapshot(listOf(testCharacter(1), testCharacter(2)), hasMore = true)
            Outcome.Success(Unit)
        }

        val vm = viewModel(repository)
        assertEquals(CharactersUiState.Loading, vm.state.value)

        gate.complete(Unit)
        val content = assertIs<CharactersUiState.Content>(vm.state.value)
        assertEquals(listOf(1, 2), content.items.map { it.id })
        assertFalse(content.isLoadingMore)
        assertNull(content.error)
        vm.close()
    }

    @Test
    fun load_failureWithNoData_isBlockingErrorAndRetryRecovers() {
        val repository = FakeCharacterRepository()
        repository.onLoadNext = { Outcome.Failure(DomainError.NoConnection(TEST_REQUEST_ID)) }

        val vm = viewModel(repository)
        val error = assertIs<CharactersUiState.Error>(vm.state.value).error
        assertTrue(error.isRetryable)
        assertEquals(TEST_REQUEST_ID, error.requestId)

        repository.loadsSuccessfully(1..2)
        vm.retry()
        assertIs<CharactersUiState.Content>(vm.state.value)
        vm.close()
    }

    @Test
    fun load_failureWithData_keepsListAndShowsNonBlockingError() {
        val repository = FakeCharacterRepository()
        repository.loadsSuccessfully(1..3)
        val vm = viewModel(repository)

        repository.onLoadNext = { Outcome.Failure(DomainError.ServerFailure(TEST_REQUEST_ID)) }
        vm.loadNext()

        val content = assertIs<CharactersUiState.Content>(vm.state.value)
        assertEquals(3, content.items.size)
        assertEquals(TEST_REQUEST_ID, content.error?.requestId)
        vm.close()
    }

    @Test
    fun load_nonRetryableError_isFlagged() {
        val repository = FakeCharacterRepository()
        repository.onLoadNext = { Outcome.Failure(DomainError.InvalidResponse(TEST_REQUEST_ID)) }

        val vm = viewModel(repository)
        assertFalse(assertIs<CharactersUiState.Error>(vm.state.value).error.isRetryable)
        vm.close()
    }

    @Test
    fun secondViewModel_reusesLoadedDataWithoutLoadingAgain() {
        val repository = FakeCharacterRepository()
        repository.loadsSuccessfully(1..3)
        val first = viewModel(repository)
        assertEquals(1, repository.loadNextCalls)

        val second = viewModel(repository)

        assertEquals(1, repository.loadNextCalls)
        val content = assertIs<CharactersUiState.Content>(second.state.value)
        assertEquals(3, content.items.size)
        assertFalse(content.isLoadingMore)
        first.close()
        second.close()
    }

    @Test
    fun retry_afterFailedRefresh_refreshesAgainInsteadOfPaging() {
        val repository = FakeCharacterRepository()
        repository.loadsSuccessfully(1..3)
        val vm = viewModel(repository)
        val pageLoadsBeforeRefresh = repository.loadNextCalls

        repository.onRefresh = { Outcome.Failure(DomainError.ServerFailure(TEST_REQUEST_ID)) }
        vm.refresh()
        assertEquals(TEST_REQUEST_ID, assertIs<CharactersUiState.Content>(vm.state.value).error?.requestId)

        repository.onRefresh = { Outcome.Success(Unit) }
        vm.retry()

        assertEquals(2, repository.refreshCalls)
        assertEquals(pageLoadsBeforeRefresh, repository.loadNextCalls)
        assertNull(assertIs<CharactersUiState.Content>(vm.state.value).error)
        vm.close()
    }

    @Test
    fun retry_afterFailedPageLoad_loadsThePageAgainWithoutRefreshing() {
        val repository = FakeCharacterRepository()
        repository.loadsSuccessfully(1..3)
        val vm = viewModel(repository)

        repository.onLoadNext = { Outcome.Failure(DomainError.NoConnection(TEST_REQUEST_ID)) }
        vm.loadNext()
        val failedPageLoads = repository.loadNextCalls

        repository.loadsSuccessfully(4..6)
        vm.retry()

        assertEquals(failedPageLoads + 1, repository.loadNextCalls)
        assertEquals(0, repository.refreshCalls)
        assertEquals(6, assertIs<CharactersUiState.Content>(vm.state.value).items.size)
        vm.close()
    }

    @Test
    fun retry_followsTheMostRecentOperation() {
        val repository = FakeCharacterRepository()
        repository.loadsSuccessfully(1..3)
        val vm = viewModel(repository)

        vm.refresh()
        repository.onLoadNext = { Outcome.Failure(DomainError.Timeout(TEST_REQUEST_ID)) }
        vm.loadNext()
        val pageLoadsBeforeRetry = repository.loadNextCalls
        val refreshesBeforeRetry = repository.refreshCalls

        vm.retry()

        assertEquals(pageLoadsBeforeRetry + 1, repository.loadNextCalls)
        assertEquals(refreshesBeforeRetry, repository.refreshCalls)
        vm.close()
    }

    @Test
    fun load_requestsWhileLoading_doNotStartAnotherLoad() {
        val repository = FakeCharacterRepository()
        val gate = CompletableDeferred<Unit>()
        repository.onLoadNext = {
            gate.await()
            Outcome.Success(Unit)
        }
        val vm = viewModel(repository) // the initial load starts here and waits on the gate

        repeat(3) { vm.loadNext() }
        vm.refresh()
        vm.retry()

        assertEquals(1, repository.loadNextCalls)
        assertEquals(0, repository.refreshCalls)
        gate.complete(Unit)
        vm.close()
    }

    @Test
    fun loadNext_noMoreData_isIgnored() {
        val repository = FakeCharacterRepository()
        repository.loadsSuccessfully(1..3, hasMore = false)
        val vm = viewModel(repository)
        val callsAfterFirstLoad = repository.loadNextCalls

        vm.loadNext()

        assertEquals(callsAfterFirstLoad, repository.loadNextCalls)
        vm.close()
    }

    @Test
    fun refresh_delegatesToTheRepository() {
        val repository = FakeCharacterRepository()
        repository.loadsSuccessfully(1..3)
        val vm = viewModel(repository)

        vm.refresh()

        assertEquals(1, repository.refreshCalls)
        vm.close()
    }
}
