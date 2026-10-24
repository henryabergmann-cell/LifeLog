package app.lifelog.feature.calendar.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.lifelog.R
import app.lifelog.designsystem.component.OccurrenceRow
import app.lifelog.designsystem.component.eventColor
import app.lifelog.designsystem.format.DateTimeFormats
import app.lifelog.domain.model.Occurrence
import app.lifelog.feature.calendar.AgendaDay
import app.lifelog.feature.calendar.CalendarUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.time.LocalDate

/** Days with appointments as a scrolling list, extendable in both directions. */
@Composable
fun AgendaList(
    state: CalendarUiState.Ready,
    formats: DateTimeFormats,
    onEventClick: (Occurrence) -> Unit,
    onLoadEarlier: () -> Unit,
    onLoadLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = state.agendaDays
    val listState = rememberLazyListState()
    val currentOnLoadLater by rememberUpdatedState(onLoadLater)

    // Scroll to the selected date when the agenda opens and when "Today" is pressed.
    LaunchedEffect(state.selectedDate) {
        listState.scrollToItem(indexOfFirstDayOnOrAfter(days, state.selectedDate))
    }
    // Load further ahead when the end of the list comes into view.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 2
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { currentOnLoadLater() }
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        item(key = "earlier") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onLoadEarlier) { Text(stringResource(R.string.agenda_show_earlier)) }
            }
        }
        if (days.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.agenda_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        days.forEach { day ->
            item(key = "header-${day.date}") {
                AgendaHeader(day.date, isToday = day.date == state.today, formats = formats)
            }
            items(day.occurrences, key = { "${day.date}-${it.key}" }) { occurrence ->
                OccurrenceRow(
                    occurrence = occurrence,
                    color = eventColor(occurrence.event.categoryId, state.categories),
                    formats = formats,
                    onClick = { onEventClick(occurrence) },
                )
            }
        }
        item(key = "later") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onLoadLater) { Text(stringResource(R.string.agenda_show_later)) }
            }
        }
    }
}

@Composable
private fun AgendaHeader(date: LocalDate, isToday: Boolean, formats: DateTimeFormats) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
    ) {
        Text(
            text = formats.weekdayDayMonth.format(date),
            style = MaterialTheme.typography.titleSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (isToday) {
            Text(
                text = stringResource(R.string.calendar_today),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** List index of the header of the first day on or after [date] (index 0 is the "earlier" button). */
private fun indexOfFirstDayOnOrAfter(days: List<AgendaDay>, date: LocalDate): Int {
    var index = 1
    for (day in days) {
        if (!day.date.isBefore(date)) return index
        index += 1 + day.occurrences.size
    }
    return if (days.isEmpty()) 0 else index
}
