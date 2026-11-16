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
 * Composition root. The app creates one instance and passes it to every screen, so all screens
 * share one repository and therefore the same data.
 */
class AppContainer internal constructor(private val repository: CharacterRepository) {

    constructor() : this(CharacterRepositoryImpl(CharacterApi(createHttpClient())))

    @OptIn(ExperimentalUuidApi::class)
    val sessionId: String = Uuid.random().toString()

    val platform: String get() = platformName()

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
