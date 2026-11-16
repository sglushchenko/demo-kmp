package pet.shared.presentation.detail

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pet.shared.domain.error.Outcome
import pet.shared.domain.usecase.GetCharacterUseCase
import pet.shared.presentation.model.CharacterUi
import pet.shared.presentation.model.ErrorUi
import pet.shared.presentation.model.toUi

sealed interface CharacterDetailUiState {
    data object Loading : CharacterDetailUiState
    data class Content(val character: CharacterUi) : CharacterDetailUiState
    data class Error(val error: ErrorUi) : CharacterDetailUiState
}

class CharacterDetailViewModel(
    private val characterId: Int,
    private val getCharacter: GetCharacterUseCase,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _state = MutableStateFlow<CharacterDetailUiState>(CharacterDetailUiState.Loading)
    val state: StateFlow<CharacterDetailUiState> = _state

    init {
        load()
    }

    fun retry() {
        _state.value = CharacterDetailUiState.Loading
        load()
    }

    fun close() = scope.cancel()

    private fun load() {
        scope.launch {
            _state.value = when (val result = getCharacter(characterId)) {
                is Outcome.Success -> CharacterDetailUiState.Content(result.value.toUi())
                is Outcome.Failure -> CharacterDetailUiState.Error(result.error.toUi())
            }
        }
    }
}
