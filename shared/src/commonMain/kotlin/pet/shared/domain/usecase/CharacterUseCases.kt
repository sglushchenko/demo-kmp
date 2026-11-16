package pet.shared.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import pet.shared.domain.error.Outcome
import pet.shared.domain.model.CartoonCharacter
import pet.shared.domain.model.CharactersSnapshot
import pet.shared.domain.repository.CharacterRepository

internal class ObserveCharactersUseCase(private val repository: CharacterRepository) {
    operator fun invoke(): StateFlow<CharactersSnapshot> = repository.characters
}

internal class LoadNextCharactersPageUseCase(private val repository: CharacterRepository) {
    suspend operator fun invoke(): Outcome<Unit> = repository.loadNextPage()
}

internal class RefreshCharactersUseCase(private val repository: CharacterRepository) {
    suspend operator fun invoke(): Outcome<Unit> = repository.refresh()
}

internal class GetCharacterUseCase(private val repository: CharacterRepository) {
    suspend operator fun invoke(id: Int): Outcome<CartoonCharacter> = repository.getCharacter(id)
}
