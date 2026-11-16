package pet.shared.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import pet.shared.domain.error.DomainError
import pet.shared.domain.error.Outcome
import pet.shared.domain.usecase.GetCharacterUseCase
import pet.shared.presentation.detail.CharacterDetailUiState
import pet.shared.presentation.detail.CharacterDetailViewModel
import pet.shared.testutil.FakeCharacterRepository
import pet.shared.testutil.TEST_REQUEST_ID
import pet.shared.testutil.testCharacter

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterDetailViewModelTest {

    private fun detailViewModel(repository: FakeCharacterRepository, id: Int) = CharacterDetailViewModel(
        characterId = id,
        getCharacter = GetCharacterUseCase(repository),
        dispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun success_exposesTheCharacter() {
        val vm = detailViewModel(FakeCharacterRepository(), id = 5)
        val content = assertIs<CharacterDetailUiState.Content>(vm.state.value)
        assertEquals(5, content.character.id)
        assertEquals("Alive", content.character.statusLabel)
        assertEquals(1_509_821_326_250, content.character.createdAtEpochMillis)
        vm.close()
    }

    @Test
    fun notFound_isNotRetryableAndRetryReloads() {
        val repository = FakeCharacterRepository()
        repository.onGetCharacter = { Outcome.Failure(DomainError.NotFound(TEST_REQUEST_ID)) }
        val vm = detailViewModel(repository, id = 999)

        val error = assertIs<CharacterDetailUiState.Error>(vm.state.value).error
        assertFalse(error.isRetryable)

        repository.onGetCharacter = { Outcome.Success(testCharacter(it)) }
        vm.retry()
        assertIs<CharacterDetailUiState.Content>(vm.state.value)
        vm.close()
    }
}
