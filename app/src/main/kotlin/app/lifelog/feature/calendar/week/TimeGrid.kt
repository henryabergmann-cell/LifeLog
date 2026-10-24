package app.lifelog.feature.calendar.week

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lifelog.R
import app.lifelog.designsystem.component.EventChip
import app.lifelog.designsystem.component.contentColorOn
import app.lifelog.designsystem.component.eventColor
import app.lifelog.designsystem.component.occurrenceTimeText
import app.lifelog.designsystem.format.DateTimeFormats
import app.lifelog.domain.model.Category
import app.lifelog.domain.model.Occurrence
import app.lifelog.feature.calendar.layout.EventColumnLayout
import app.lifelog.feature.calendar.layout.TimedItem
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val MINUTES_PER_DAY = 24 * 60
private const val MIN_BLOCK_MINUTES = 20
private const val MAX_ALL_DAY_CHIPS = 2

/**
 * Time grid shared by the week view (7 days) and the day view (1 day): day headers, an all-day
 * row, and a scrollable 24-hour area with events laid out side by side when they overlap.
 */
@Composable
fun TimeGrid(
    days: List<LocalDate>,
    today: LocalDate,
    occurrences: List<Occurrence>,
    categories: Map<Long, Category>,
    formats: DateTimeFormats,
    onEventClick: (Occurrence) -> Unit,
    onSlotClick: (date: LocalDate, minuteOfDay: Int) -> Unit,
    onDayHeaderClick: ((LocalDate) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    val hourHeight = 56.dp * fontScale.coerceIn(1f, 1.6f)
    val labelWidth = 48.dp * fontScale.coerceIn(1f, 1.4f)
    val now by rememberCurrentMinute()

    val initialScrollPx = with(LocalDensity.current) {
        val firstHour = if (today in days) (now.hour - 1).coerceAtLeast(0) else 7
        (hourHeight * firstHour).roundToPx()
    }
    val scrollState = rememberScrollState(initialScrollPx)

    Column(modifier.fillMaxSize()) {
        if (onDayHeaderClick != null) {
            DayHeaderRow(days, today, labelWidth, formats, onDayHeaderClick)
        }
        AllDayRow(days, occurrences, categories, labelWidth, formats, onEventClick)
        HorizontalDivider()
        Row(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            HourLabels(labelWidth, hourHeight, formats)
            days.forEach { day ->
                DayColumn(
                    day = day,
                    occurrences = occurrences,
                    categories = categories,
                    hourHeight = hourHeight,
                    nowMinute = if (day == today) now.hour * 60 + now.minute else null,
                    formats = formats,
                    onEventClick = onEventClick,
                    onSlotClick = onSlotClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(hourHeight * 24),
                )
            }
        }
    }
}

@Composable
private fun rememberCurrentMinute() = produceState(LocalTime.now()) {
    while (true) {
        val current = LocalTime.now()
        value = current
        delay((60 - current.second) * 1_000L)
    }
}

@Composable
private fun DayHeaderRow(
    days: List<LocalDate>,
    today: LocalDate,
    labelWidth: Dp,
    formats: DateTimeFormats,
    onDayHeaderClick: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val openLabel = stringResource(R.string.calendar_open_day)
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(labelWidth))
        days.forEach { day ->
            val isToday = day == today
            val description = formats.weekdayDayMonth.format(day).let {
                if (isToday) stringResource(R.string.calendar_today_prefix, it) else it
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable { onDayHeaderClick(day) }
                    .clearAndSetSemantics {
                        contentDescription = description
                        role = Role.Button
                        onClick(label = openLabel) { onDayHeaderClick(day); true }
                    }
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = formats.shortWeekday.format(day),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .size(28.dp)
                        .background(if (isToday) colors.primary else colors.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isToday) colors.onPrimary else colors.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun AllDayRow(
    days: List<LocalDate>,
    occurrences: List<Occurrence>,
    categories: Map<Long, Category>,
    labelWidth: Dp,
    formats: DateTimeFormats,
    onEventClick: (Occurrence) -> Unit,
) {
    val allDay = occurrences.filter { it.isAllDay }
    if (days.none { day -> allDay.any { it.overlaps(day) } }) return

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.calendar_all_day),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(labelWidth)
                .clearAndSetSemantics {},
        )
        days.forEach { day ->
            val forDay = allDay.filter { it.overlaps(day) }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 1.dp),
            ) {
                val shown = if (forDay.size > MAX_ALL_DAY_CHIPS) MAX_ALL_DAY_CHIPS - 1 else forDay.size
                forDay.take(shown).forEach { occurrence ->
                    val description = listOf(occurrence.event.title, occurrenceTimeText(occurrence, formats)).joinToString(", ")
                    EventChip(
                        title = occurrence.event.title,
                        color = eventColor(occurrence.event.categoryId, categories),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .clickable { onEventClick(occurrence) }
                            .clearAndSetSemantics {
                                contentDescription = description
                                role = Role.Button
                                onClick { onEventClick(occurrence); true }
                            },
                    )
                }
                if (shown < forDay.size) {
                    Text(
                        text = stringResource(R.string.calendar_more_count, forDay.size - shown),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HourLabels(labelWidth: Dp, hourHeight: Dp, formats: DateTimeFormats) {
    Column(
        Modifier
            .width(labelWidth)
            .clearAndSetSemantics {},
    ) {
        repeat(24) { hour ->
            Box(Modifier.height(hourHeight).fillMaxWidth()) {
                if (hour > 0) {
                    Text(
                        text = formats.hour.format(LocalTime.of(hour, 0)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-8).dp),
                    )
                }
            }
        }
    }
}

private data class Segment(val occurrence: Occurrence, val startMinute: Int, val endMinute: Int)

@Composable
private fun DayColumn(
    day: LocalDate,
    occurrences: List<Occurrence>,
    categories: Map<Long, Category>,
    hourHeight: Dp,
    nowMinute: Int?,
    formats: DateTimeFormats,
    onEventClick: (Occurrence) -> Unit,
    onSlotClick: (LocalDate, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = remember(day, occurrences) {
        val dayStart = day.atStartOfDay()
        occurrences
            .filter { !it.isAllDay && it.overlaps(day) }
            .map { Segment(it, minutesFrom(dayStart, it.start), minutesFrom(dayStart, it.end)) }
    }
    val placements = remember(segments) {
        EventColumnLayout.layout(segments.map { TimedItem(it.startMinute, it.endMinute) }, MIN_BLOCK_MINUTES)
    }
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val hourHeightPx = with(LocalDensity.current) { hourHeight.toPx() }

    BoxWithConstraints(
        modifier
            .drawBehind {
                drawLine(lineColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1f)
                for (hour in 1 until 24) {
                    val y = hour * hourHeightPx
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
            }
            .pointerInput(day, hourHeightPx) {
                detectTapGestures { offset ->
                    val minute = (offset.y / hourHeightPx * 60).toInt().coerceIn(0, MINUTES_PER_DAY - 1)
                    onSlotClick(day, minute / 30 * 30)
                }
            },
    ) {
        segments.forEachIndexed { index, segment ->
            val placement = placements[index]
            val columnWidth = maxWidth / placement.columnCount
            val duration = maxOf(segment.endMinute - segment.startMinute, MIN_BLOCK_MINUTES)
            EventBlock(
                occurrence = segment.occurrence,
                color = eventColor(segment.occurrence.event.categoryId, categories),
                formats = formats,
                onClick = { onEventClick(segment.occurrence) },
                modifier = Modifier
                    .offset(x = columnWidth * placement.column, y = hourHeight * (segment.startMinute / 60f))
                    .width(columnWidth)
                    .height(hourHeight * (duration / 60f))
                    .padding(1.dp),
            )
        }
        if (nowMinute != null) {
            Box(
                Modifier
                    .offset(y = hourHeight * (nowMinute / 60f))
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.error)
                    .clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun EventBlock(
    occurrence: Occurrence,
    color: androidx.compose.ui.graphics.Color,
    formats: DateTimeFormats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = occurrenceTimeText(occurrence, formats)
    val description = listOfNotNull(occurrence.event.title, time, occurrence.event.location).joinToString(", ")
    val textColor = contentColorOn(color)
    Column(
        modifier
            .background(color, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
            }
            .padding(horizontal = 3.dp, vertical = 1.dp),
    ) {
        Text(
            text = occurrence.event.title,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

private fun minutesFrom(dayStart: LocalDateTime, time: LocalDateTime): Int =
    Duration.between(dayStart, time).toMinutes().coerceIn(0, MINUTES_PER_DAY.toLong()).toInt()
