package app.lifelog

import app.cash.turbine.test
import app.lifelog.domain.model.ThemeMode
import app.lifelog.fakes.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository()

    @Test
    fun `exposes the theme from settings and follows changes`() = runTest {
        val viewModel = MainViewModel(settingsRepository)

        viewModel.uiState.test {
            val first = awaitItem().let { if (it == MainUiState.Loading) awaitItem() else it }
            assertEquals(MainUiState.Ready(ThemeMode.SYSTEM, dynamicColor = true), first)

            settingsRepository.update { it.copy(themeMode = ThemeMode.DARK, dynamicColor = false) }

            assertEquals(MainUiState.Ready(ThemeMode.DARK, dynamicColor = false), awaitItem())
        }
    }
}
