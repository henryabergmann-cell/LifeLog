package app.lifelog.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifelog.R
import app.lifelog.designsystem.component.BarChart
import app.lifelog.designsystem.component.ColorDot
import app.lifelog.designsystem.format.DateTimeFormats
import app.lifelog.designsystem.format.rememberDateTimeFormats
import app.lifelog.designsystem.icon.LifeLogIcons
import app.lifelog.domain.stats.Statistics
import app.lifelog.domain.stats.StatsBucket
import app.lifelog.feature.premium.PremiumTeaser
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onOpenPremium: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_insights), modifier = Modifier.semantics { heading() }) })
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                !state.isPremium -> PremiumTeaser(
                    icon = LifeLogIcons.Insights,
                    title = stringResource(R.string.insights_teaser_title),
                    description = stringResource(R.string.insights_teaser_description),
                    previewDescription = stringResource(R.string.insights_teaser_preview_description),
                    onUnlock = onOpenPremium,
                ) { InsightsPreview() }
                else -> InsightsContent(state, viewModel::onRangeSelected)
            }
        }
    }
}

@Composable
private fun InsightsContent(state: InsightsUiState, onRangeSelected: (InsightsRange) -> Unit) {
    val formats = rememberDateTimeFormats()
    val stats = state.statistics
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InsightsRange.entries.forEach { range ->
                FilterChip(
                    selected = state.range == range,
                    onClick = { onRangeSelected(range) },
                    label = { Text(stringResource(range.label)) },
                )
            }
        }
        if (stats == null || stats.isEmpty) {
            Text(
                text = stringResource(R.string.insights_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
            return@Column
        }

        SummaryRow(state, stats)
        PeriodChart(stats, state.range.bucket, formats)
        WeekdayCard(stats, formats)
        CategoryCard(state, stats)
    }
}

@Composable
private fun SummaryRow(state: InsightsUiState, stats: Statistics) {
    val locale = rememberDateTimeFormats().locale
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(stringResource(R.string.insights_appointments), stats.appointmentCount.toString(), Modifier.weight(1f))
        StatTile(
            stringResource(R.string.insights_hours),
            String.format(locale, "%.1f", stats.totalTimedMinutes / 60.0),
            Modifier.weight(1f),
        )
        StatTile(stringResource(R.string.insights_per_week), String.format(locale, "%.1f", state.perWeek), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier.semantics(mergeDescendants = true) {}) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PeriodChart(stats: Statistics, bucket: StatsBucket, formats: DateTimeFormats) {
    val byWeek = bucket == StatsBucket.WEEK
    val title = stringResource(if (byWeek) R.string.insights_per_week_chart else R.string.insights_per_month_chart)
    val itemTemplate = stringResource(if (byWeek) R.string.insights_chart_week_item else R.string.insights_chart_month_item)
    val labels = stats.buckets.map { (if (byWeek) formats.dayMonth else formats.shortMonth).format(it.start) }
    val description = title + ": " + stats.buckets.joinToString("; ") {
        String.format(itemTemplate, (if (byWeek) formats.dayMonth else formats.monthYear).format(it.start), it.count)
    }
    ChartCard(title) {
        BarChart(
            values = stats.buckets.map { it.count },
            labels = labels,
            description = description,
            highlightIndex = stats.buckets.lastIndex,
            labelEvery = when {
                stats.buckets.size > 8 && byWeek -> 3
                stats.buckets.size > 8 -> 2
                else -> 1
            },
        )
    }
}

@Composable
private fun WeekdayCard(stats: Statistics, formats: DateTimeFormats) {
    val locale = formats.locale
    val busiest = stats.weekdayCounts.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first
    val title = stringResource(R.string.insights_weekdays)
    ChartCard(title) {
        BarChart(
            values = stats.weekdayCounts.map { it.second },
            labels = stats.weekdayCounts.map { it.first.getDisplayName(TextStyle.SHORT, locale) },
            description = title + ": " + stats.weekdayCounts.joinToString("; ") {
                "${it.first.getDisplayName(TextStyle.FULL, locale)}: ${it.second}"
            },
            highlightIndex = stats.weekdayCounts.indexOfFirst { it.first == busiest }.takeIf { it >= 0 },
        )
        busiest?.let { day: DayOfWeek ->
            InsightLine(stringResource(R.string.insights_busiest_weekday, day.getDisplayName(TextStyle.FULL, locale)))
        }
        stats.busiestDate?.let { date ->
            InsightLine(
                pluralStringResource(
                    R.plurals.insights_busiest_date,
                    stats.busiestDateCount,
                    formats.weekdayDayMonth.format(date),
                    stats.busiestDateCount,
                ),
            )
        }
        stats.busiestHour?.let { hour ->
            InsightLine(
                stringResource(
                    R.string.insights_busiest_hour,
                    formats.hour.format(LocalTime.of(hour, 0)),
                    formats.hour.format(LocalTime.of((hour + 1) % 24, 0)),
                ),
            )
        }
    }
}

@Composable
private fun CategoryCard(state: InsightsUiState, stats: Statistics) {
    val max = stats.categoryCounts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    ChartCard(stringResource(R.string.insights_categories)) {
        stats.categoryCounts.forEach { entry ->
            val category = entry.categoryId?.let(state.categories::get)
            val color = category?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.outline
            Column(
                Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .padding(vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorDot(color)
                    Text(
                        category?.name ?: stringResource(R.string.event_no_category),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(entry.count.toString(), style = MaterialTheme.typography.labelLarge)
                }
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(entry.count.toFloat() / max)
                        .height(8.dp)
                        .background(color, RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun InsightLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Sample chart for the free-tier teaser. */
@Composable
private fun InsightsPreview() {
    val locale = rememberDateTimeFormats().locale
    val days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.insights_weekdays), style = MaterialTheme.typography.titleSmall)
        BarChart(
            values = listOf(4, 6, 3, 7, 5, 2, 1),
            labels = days.map { it.getDisplayName(TextStyle.SHORT, locale) },
            description = "",
            highlightIndex = 3,
            chartHeight = 100.dp,
        )
        InsightLine(stringResource(R.string.insights_busiest_weekday, DayOfWeek.THURSDAY.getDisplayName(TextStyle.FULL, locale)))
    }
}

private val InsightsRange.label: Int
    get() = when (this) {
        InsightsRange.LAST_4_WEEKS -> R.string.insights_range_4_weeks
        InsightsRange.LAST_12_WEEKS -> R.string.insights_range_12_weeks
        InsightsRange.LAST_12_MONTHS -> R.string.insights_range_12_months
    }
