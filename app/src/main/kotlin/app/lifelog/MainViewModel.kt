package app.lifelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifelog.domain.model.ThemeMode
import app.lifelog.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = settingsRepository.settings
        .map { MainUiState.Ready(themeMode = it.themeMode, dynamicColor = it.dynamicColor) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState.Loading)
}

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(
        val themeMode: ThemeMode,
        val dynamicColor: Boolean,
    ) : MainUiState
}
