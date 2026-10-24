package app.lifelog.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lifelog.R
import app.lifelog.designsystem.format.DateTimeFormats
import app.lifelog.domain.model.Occurrence

/** A list row for an occurrence (agenda, search): color bar, title, time and location. */
@Composable
fun OccurrenceRow(
    occurrence: Occurrence,
    color: Color,
    formats: DateTimeFormats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingDate: Boolean = false,
) {
    val event = occurrence.event
    val time = occurrenceTimeText(occurrence, formats)
    val dateText = if (leadingDate) formats.shortWeekdayDayMonth.format(occurrence.start) else null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(40.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(dateText, time).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            event.location?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** "09:00 – 10:00", "All day" or "All day · 6 Oct – 8 Oct". */
@Composable
fun occurrenceTimeText(occurrence: Occurrence, formats: DateTimeFormats): String =
    formats.timeRange(occurrence) ?: listOfNotNull(
        stringResource(R.string.calendar_all_day),
        formats.allDayRange(occurrence),
    ).joinToString(" · ")

/** Compact colored chip used in month cells and the all-day row. */
@Composable
fun EventChip(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = contentColorOn(color),
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}
