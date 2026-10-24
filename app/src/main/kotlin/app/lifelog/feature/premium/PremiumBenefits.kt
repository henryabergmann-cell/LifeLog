package app.lifelog.feature.premium

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.lifelog.R
import app.lifelog.designsystem.icon.LifeLogIcons

/** One thing Premium adds, shown on the Premium screen. */
data class PremiumBenefit(
    val icon: ImageVector,
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
)

val PREMIUM_BENEFITS: List<PremiumBenefit> by lazy {
    listOf(
        PremiumBenefit(LifeLogIcons.Journal, R.string.premium_benefit_journal, R.string.premium_benefit_journal_desc),
        PremiumBenefit(LifeLogIcons.Insights, R.string.premium_benefit_insights, R.string.premium_benefit_insights_desc),
        PremiumBenefit(LifeLogIcons.Palette, R.string.premium_benefit_categories, R.string.premium_benefit_categories_desc),
        PremiumBenefit(LifeLogIcons.Notifications, R.string.premium_benefit_reminders, R.string.premium_benefit_reminders_desc),
        PremiumBenefit(LifeLogIcons.Widgets, R.string.premium_benefit_widgets, R.string.premium_benefit_widgets_desc),
        PremiumBenefit(LifeLogIcons.Sync, R.string.premium_benefit_sync, R.string.premium_benefit_sync_desc),
        PremiumBenefit(LifeLogIcons.Backup, R.string.premium_benefit_backup, R.string.premium_benefit_backup_desc),
        PremiumBenefit(LifeLogIcons.Star, R.string.premium_benefit_no_ads, R.string.premium_benefit_no_ads_desc),
    )
}

@Composable
fun PremiumBenefitRow(benefit: PremiumBenefit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        ) {
            Icon(benefit.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(benefit.title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(benefit.description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
