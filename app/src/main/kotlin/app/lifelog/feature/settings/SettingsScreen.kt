package app.lifelog.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifelog.R
import app.lifelog.designsystem.icon.LifeLogIcons
import app.lifelog.domain.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(premiumManager: PremiumManager) : ViewModel() {
    val isPremium: StateFlow<Boolean> = premiumManager.isPremium
}

/** Settings entry list. M4 adds theme, defaults, language, privacy policy and more. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenCategories: () -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_settings),
                        modifier = Modifier.semantics { heading() },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                leadingContent = { Icon(LifeLogIcons.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(stringResource(R.string.premium_title)) },
                supportingContent = {
                    Text(stringResource(if (isPremium) R.string.settings_premium_active else R.string.settings_premium_summary))
                },
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenPremium),
            )
            ListItem(
                leadingContent = { Icon(LifeLogIcons.Label, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.categories_title)) },
                supportingContent = { Text(stringResource(R.string.settings_categories_summary)) },
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenCategories),
            )
            // Debug builds: "simulate Premium" switch. Release builds: nothing.
            DeveloperOptionsSection()
        }
    }
}
