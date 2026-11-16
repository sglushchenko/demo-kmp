package pet.shared.di

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertIs
import pet.shared.domain.model.CharactersSnapshot
import pet.shared.presentation.characters.CharactersUiState
import pet.shared.testutil.FakeCharacterRepository
import pet.shared.testutil.testCharacter

class AppContainerTest {

    private fun containerWith(vararg ids: Int) = AppContainer(
        FakeCharacterRepository(CharactersSnapshot(ids.map(::testCharacter), hasMore = true)),
    )

    private fun AppContainer.visibleIds(): List<Int> {
        val viewModel = makeCharactersViewModel()
        val state = assertIs<CharactersUiState.Content>(viewModel.state.value)
        viewModel.close()
        return state.items.map { it.id }
    }

    @Test
    fun viewModels_readFromTheInjectedRepository() {
        assertEquals(listOf(1, 2, 3), containerWith(1, 2, 3).visibleIds())
    }

    @Test
    fun containers_doNotShareData() {
        val first = containerWith(1, 2)
        val second = containerWith(7)

        assertEquals(listOf(1, 2), first.visibleIds())
        assertEquals(listOf(7), second.visibleIds())
    }

    @Test
    fun eachContainerIsItsOwnSession() {
        assertNotEquals(containerWith().sessionId, containerWith().sessionId)
    }
}
