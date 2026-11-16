package pet.shared.di

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import pet.shared.data.remote.CharacterApi
import pet.shared.data.remote.createHttpClient
import pet.shared.data.repository.CharacterRepositoryImpl
import pet.shared.domain.repository.CharacterRepository
import pet.shared.domain.usecase.GetCharacterUseCase
import pet.shared.domain.usecase.LoadNextCharactersPageUseCase
import pet.shared.domain.usecase.ObserveCharactersUseCase
import pet.shared.domain.usecase.RefreshCharactersUseCase
import pet.shared.platform.platformName
import pet.shared.presentation.characters.CharactersViewModel
import pet.shared.presentation.detail.CharacterDetailViewModel

/**
 * Manual dependency injection, the only entry point iOS needs.
 * Swift calls `AppContainer.shared.makeCharactersViewModel()`; no DI framework is required for an app this size.
 */
object AppContainer {
    @OptIn(ExperimentalUuidApi::class)
    val sessionId: String = Uuid.random().toString()

    val platform: String get() = platformName()

    // One repository for the whole app: this is what makes the data independent of any screen.
    private val repository: CharacterRepository by lazy {
        CharacterRepositoryImpl(CharacterApi(createHttpClient()))
    }

    fun makeCharactersViewModel() = CharactersViewModel(
        observeCharacters = ObserveCharactersUseCase(repository),
        loadNextPage = LoadNextCharactersPageUseCase(repository),
        refreshCharacters = RefreshCharactersUseCase(repository),
    )

    fun makeCharacterDetailViewModel(characterId: Int) = CharacterDetailViewModel(
        characterId = characterId,
        getCharacter = GetCharacterUseCase(repository),
    )
}
