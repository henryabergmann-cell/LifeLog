package app.lifelog.feature.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifelog.R
import app.lifelog.designsystem.component.DatePickerModal
import app.lifelog.designsystem.format.DateTimeFormats
import app.lifelog.designsystem.format.rememberDateTimeFormats
import app.lifelog.designsystem.icon.LifeLogIcons
import app.lifelog.domain.model.JournalEntry
import app.lifelog.domain.model.Mood
import app.lifelog.feature.premium.PremiumTeaser
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onOpenPremium: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose { viewModel.onPause() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_journal), modifier = Modifier.semantics { heading() }) })
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                !state.isPremium && state.entries.isEmpty() -> PremiumTeaser(
                    icon = LifeLogIcons.Journal,
                    title = stringResource(R.string.journal_teaser_title),
                    description = stringResource(R.string.journal_teaser_description),
                    previewDescription = stringResource(R.string.journal_teaser_preview_description),
                    onUnlock = onOpenPremium,
                ) { JournalPreview(state.today) }
                else -> JournalContent(
                    state = state,
                    noteText = viewModel.noteText,
                    onMoodSelected = viewModel::onMoodSelected,
                    onNoteChange = viewModel::onNoteChange,
                    onDateSelected = viewModel::onDateSelected,
                    onPreviousDay = viewModel::onPreviousDay,
                    onNextDay = viewModel::onNextDay,
                    onOpenPremium = onOpenPremium,
                )
            }
        }
    }
}

@Composable
private fun JournalContent(
    state: JournalUiState,
    noteText: String,
    onMoodSelected: (Mood) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var pickDate by rememberSaveable { mutableStateOf(false) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item(key = "editor") {
            if (state.isReadOnly) {
                ReadOnlyNotice(onOpenPremium)
            } else {
                EntryEditor(
                    state = state,
                    noteText = noteText,
                    formats = formats,
                    onMoodSelected = onMoodSelected,
                    onNoteChange = onNoteChange,
                    onPreviousDay = onPreviousDay,
                    onNextDay = onNextDay,
                    onPickDate = { pickDate = true },
                )
            }
        }
        item(key = "timeline-header") {
            Text(
                text = stringResource(R.string.journal_timeline),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                    .semantics { heading() },
            )
        }
        if (state.entries.isEmpty()) {
            item(key = "timeline-empty") {
                Text(
                    text = stringResource(R.string.journal_timeline_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        items(state.entries, key = { it.date.toEpochDay() }) { entry ->
            JournalEntryRow(
                entry = entry,
                isSelected = entry.date == state.selectedDate && !state.isReadOnly,
                formats = formats,
                onClick = if (state.isReadOnly) {
                    null
                } else {
                    {
                        onDateSelected(entry.date)
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                },
            )
        }
    }

    if (pickDate) {
        DatePickerModal(initial = state.selectedDate, onConfirm = onDateSelected, onDismiss = { pickDate = false })
    }
}

@Composable
private fun EntryEditor(
    state: JournalUiState,
    noteText: String,
    formats: DateTimeFormats,
    onMoodSelected: (Mood) -> Unit,
    onNoteChange: (String) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousDay) {
                    Icon(LifeLogIcons.ChevronLeft, contentDescription = stringResource(R.string.calendar_previous_day))
                }
                val dateText = formats.weekdayDayMonth.format(state.selectedDate)
                TextButton(onClick = onPickDate, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.selectedDate == state.today) {
                            stringResource(R.string.calendar_today_prefix, dateText)
                        } else {
                            dateText
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                IconButton(onClick = onNextDay, enabled = state.canGoForward) {
                    Icon(LifeLogIcons.ChevronRight, contentDescription = stringResource(R.string.calendar_next_day))
                }
            }

            Text(stringResource(R.string.journal_mood_question), style = MaterialTheme.typography.labelLarge)
            MoodSelector(selected = state.mood, onSelect = onMoodSelected)

            val status = when {
                state.hasUnsavedChanges -> stringResource(R.string.journal_saving)
                state.mood != null || noteText.isNotBlank() -> stringResource(R.string.journal_saved)
                else -> ""
            }
            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.journal_note)) },
                placeholder = { Text(stringResource(R.string.journal_note_hint)) },
                minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                supportingText = {
                    Row {
                        Text(status, modifier = Modifier.weight(1f))
                        Text("${noteText.length} / ${JournalEntry.MAX_NOTE_LENGTH}")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MoodSelector(selected: Mood?, onSelect: (Mood) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        Mood.entries.forEach { mood ->
            val isSelected = mood == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(mood) }
                    .padding(vertical = 6.dp),
            ) {
                Text(mood.emoji, fontSize = 28.sp, modifier = Modifier.clearAndSetSemantics {})
                Text(
                    stringResource(mood.label),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun JournalEntryRow(
    entry: JournalEntry,
    isSelected: Boolean,
    formats: DateTimeFormats,
    onClick: (() -> Unit)?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // The mood label below is read instead of the emoji's name.
        Text(entry.mood?.emoji ?: "·", fontSize = 26.sp, modifier = Modifier.clearAndSetSemantics {})
        Column(Modifier.weight(1f)) {
            Text(formats.weekdayDayMonth.format(entry.date), style = MaterialTheme.typography.titleSmall)
            entry.mood?.let {
                Text(stringResource(it.label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (entry.note.isNotBlank()) {
                Text(entry.note, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ReadOnlyNotice(onOpenPremium: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.journal_read_only), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onOpenPremium) { Text(stringResource(R.string.premium_learn_more)) }
        }
    }
}

/** Sample entries for the free-tier teaser. */
@Composable
private fun JournalPreview(today: LocalDate) {
    val formats = rememberDateTimeFormats()
    val samples = listOf(
        JournalEntry(today.minusDays(1), Mood.GREAT, stringResource(R.string.journal_sample_1)),
        JournalEntry(today.minusDays(2), Mood.GOOD, stringResource(R.string.journal_sample_2)),
        JournalEntry(today.minusDays(3), Mood.OKAY, stringResource(R.string.journal_sample_3)),
    )
    Column { samples.forEach { JournalEntryRow(it, isSelected = false, formats = formats, onClick = null) } }
}
