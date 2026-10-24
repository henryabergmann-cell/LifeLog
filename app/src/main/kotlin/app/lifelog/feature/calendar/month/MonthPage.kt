package app.lifelog.feature.calendar.month

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lifelog.R
import app.lifelog.designsystem.component.ColorDot
import app.lifelog.designsystem.component.EventChip
import app.lifelog.designsystem.component.eventColor
import app.lifelog.designsystem.format.DateTimeFormats
import app.lifelog.domain.model.Category
import app.lifelog.domain.model.Occurrence
import app.lifelog.feature.calendar.CalendarPeriods
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Six-week month grid. Tapping a day opens it in the day view. */
@Composable
fun MonthPage(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate,
    occurrences: List<Occurrence>,
    categories: Map<Long, Category>,
    formats: DateTimeFormats,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = remember(month, firstDayOfWeek) { CalendarPeriods.monthGrid(month, firstDayOfWeek) }
    val byDate = remember(days, occurrences) {
        days.associateWith { date -> occurrences.filter { it.overlaps(date) } }
    }
    // With large fonts there is no room for titles; cells then show colored dots.
    val compact = LocalDensity.current.fontScale > 1.3f

    Column(modifier.fillMaxSize()) {
        // Each cell announces its full date, so the weekday header is hidden from TalkBack.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clearAndSetSemantics {},
        ) {
            days.take(7).forEach { day ->
                Text(
                    text = formats.shortWeekday.format(day),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        days.chunked(7).forEach { week ->
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                week.forEach { date ->
                    MonthCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        occurrences = byDate[date].orEmpty(),
                        categories = categories,
                        compact = compact,
                        formats = formats,
                        onClick = { onDayClick(date) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    occurrences: List<Occurrence>,
    categories: Map<Long, Category>,
    compact: Boolean,
    formats: DateTimeFormats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateText = formats.weekdayDayMonth.format(date)
    val countText = if (occurrences.isEmpty()) {
        stringResource(R.string.calendar_day_no_appointments, dateText)
    } else {
        pluralStringResource(R.plurals.calendar_day_appointments, occurrences.size, dateText, occurrences.size)
    }
    val description = if (isToday) stringResource(R.string.calendar_today_prefix, countText) else countText
    val openLabel = stringResource(R.string.calendar_open_day)
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier
            .border(0.5.dp, colors.outlineVariant)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = openLabel) { onClick(); true }
            }
            .padding(2.dp),
    ) {
        val density = LocalDensity.current
        val chipHeight = with(density) { MaterialTheme.typography.labelSmall.lineHeight.toDp() } + 4.dp
        val capacity = ((maxHeight - DAY_NUMBER_SIZE - 2.dp) / chipHeight).toInt().coerceAtLeast(0)

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(DAY_NUMBER_SIZE)
                    .background(if (isToday) colors.primary else colors.surface.copy(alpha = 0f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        isToday -> colors.onPrimary
                        inMonth -> colors.onSurface
                        else -> colors.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                )
            }
            when {
                occurrences.isEmpty() -> Unit
                compact || capacity == 0 -> Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    occurrences.take(3).forEach { ColorDot(eventColor(it.event.categoryId, categories), size = 6.dp) }
                }
                else -> {
                    val shown = if (occurrences.size > capacity) capacity - 1 else occurrences.size
                    occurrences.take(shown).forEach {
                        EventChip(title = it.event.title, color = eventColor(it.event.categoryId, categories))
                    }
                    if (shown < occurrences.size) {
                        Text(
                            text = stringResource(R.string.calendar_more_count, occurrences.size - shown),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

private val DAY_NUMBER_SIZE = 24.dp
