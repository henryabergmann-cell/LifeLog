package app.lifelog.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.lifelog.R
import app.lifelog.domain.premium.PremiumOverride
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Debug builds only. The release source set has an empty version of this composable. */
@Composable
fun DeveloperOptionsSection(viewModel: DeveloperOptionsViewModel = hiltViewModel()) {
    val forced by viewModel.forcedPremium.collectAsStateWithLifecycle()
    Column {
        HorizontalDivider()
        ListItem(
            overlineContent = { Text(stringResource(R.string.developer_options), color = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(stringResource(R.string.developer_simulate_premium)) },
            supportingContent = { Text(stringResource(R.string.developer_simulate_premium_summary)) },
            trailingContent = { Switch(checked = forced, onCheckedChange = null) },
            modifier = Modifier.toggleable(value = forced, role = Role.Switch, onValueChange = viewModel::setForcedPremium),
        )
    }
}

@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    private val premiumOverride: PremiumOverride,
) : ViewModel() {

    val forcedPremium: StateFlow<Boolean> =
        premiumOverride.isForcedPremium.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setForcedPremium(enabled: Boolean) {
        viewModelScope.launch { premiumOverride.setForcedPremium(enabled) }
    }
}
