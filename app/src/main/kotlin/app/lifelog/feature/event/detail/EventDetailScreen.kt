package app.lifelog.feature.event.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifelog.R
import app.lifelog.designsystem.component.ColorDot
import app.lifelog.designsystem.component.occurrenceTimeText
import app.lifelog.designsystem.format.rememberDateTimeFormats
import app.lifelog.designsystem.icon.LifeLogIcons
import app.lifelog.feature.event.reminderLabel
import androidx.compose.ui.graphics.Color

@Composable
fun EventDetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Close once the event is gone; the undo snackbar is shown by the app shell.
    LaunchedEffect(state) {
        if (state is EventDetailUiState.Gone && (state as EventDetailUiState.Gone).deletedHere) onBack()
    }
    EventDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onDuplicate = onDuplicate,
        onDelete = viewModel::onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    state: EventDetailUiState,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    val ready = state as? EventDetailUiState.Ready
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(LifeLogIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (ready != null) {
                        IconButton(onClick = { onEdit(ready.event.id) }) {
                            Icon(LifeLogIcons.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { onDuplicate(ready.event.id) }) {
                            Icon(LifeLogIcons.Copy, contentDescription = stringResource(R.string.action_duplicate))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(LifeLogIcons.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state) {
                EventDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is EventDetailUiState.Gone -> Text(
                    text = stringResource(R.string.event_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                is EventDetailUiState.Ready -> EventDetails(state)
            }
        }
    }
}

@Composable
private fun EventDetails(state: EventDetailUiState.Ready) {
    val formats = rememberDateTimeFormats()
    val event = state.event
    val occurrence = state.occurrence
    // Multi-day ranges are part of the time line below ("All day · 6 Oct – 8 Oct").
    val dateText = formats.fullDate.format(occurrence.start)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        DetailRow(LifeLogIcons.Schedule) {
            Text(dateText, style = MaterialTheme.typography.bodyLarge)
            Text(
                occurrenceTimeText(occurrence, formats),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.foreignZone?.let {
                Text(
                    stringResource(R.string.event_time_zone, it.id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        event.location?.let { DetailRow(LifeLogIcons.Place) { Text(it, style = MaterialTheme.typography.bodyLarge) } }
        state.category?.let { category ->
            DetailRow(LifeLogIcons.Label) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(Color(category.colorArgb))
                    Text(category.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (event.reminderMinutes.isNotEmpty()) {
            DetailRow(LifeLogIcons.Notifications) {
                event.reminderMinutes.forEach { Text(reminderLabel(it, occurrence.isAllDay), style = MaterialTheme.typography.bodyLarge) }
            }
        }
        event.notes?.let { DetailRow(LifeLogIcons.Notes) { Text(it, style = MaterialTheme.typography.bodyLarge) } }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) { content() }
    }
}
