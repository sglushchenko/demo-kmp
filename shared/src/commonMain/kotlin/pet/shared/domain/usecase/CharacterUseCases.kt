package pet.shared.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import pet.shared.domain.error.Outcome
import pet.shared.domain.model.Character
import pet.shared.domain.model.CharactersSnapshot
import pet.shared.domain.repository.CharacterRepository

class ObserveCharactersUseCase(private val repository: CharacterRepository) {
    operator fun invoke(): StateFlow<CharactersSnapshot> = repository.characters
}

class LoadNextCharactersPageUseCase(private val repository: CharacterRepository) {
    suspend operator fun invoke(): Outcome<Unit> = repository.loadNextPage()
}

class RefreshCharactersUseCase(private val repository: CharacterRepository) {
    suspend operator fun invoke(): Outcome<Unit> = repository.refresh()
}

class GetCharacterUseCase(private val repository: CharacterRepository) {
    suspend operator fun invoke(id: Int): Outcome<Character> = repository.getCharacter(id)
}
