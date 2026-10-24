package app.lifelog.feature.event.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifelog.R
import app.lifelog.designsystem.component.ColorDot
import app.lifelog.designsystem.component.ConfirmDialog
import app.lifelog.designsystem.component.DatePickerModal
import app.lifelog.designsystem.component.SingleChoiceDialog
import app.lifelog.designsystem.component.TimePickerModal
import app.lifelog.designsystem.format.DateTimeFormats
import app.lifelog.designsystem.format.rememberDateTimeFormats
import app.lifelog.designsystem.icon.LifeLogIcons
import app.lifelog.domain.usecase.EventValidationError
import app.lifelog.feature.event.REMINDER_OPTIONS
import app.lifelog.feature.event.reminderLabel
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun EventEditorRoute(
    onClose: () -> Unit,
    onManageCategories: () -> Unit,
    viewModel: EventEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedEventId) {
        if (state.savedEventId != null) onClose()
    }
    EventEditorScreen(
        state = state,
        actions = EditorActions(
            onClose = onClose,
            onSave = viewModel::onSave,
            onTitleChange = viewModel::onTitleChange,
            onAllDayChange = viewModel::onAllDayChange,
            onStartDateChange = viewModel::onStartDateChange,
            onStartTimeChange = viewModel::onStartTimeChange,
            onEndDateChange = viewModel::onEndDateChange,
            onEndTimeChange = viewModel::onEndTimeChange,
            onLocationChange = viewModel::onLocationChange,
            onNotesChange = viewModel::onNotesChange,
            onCategoryChange = viewModel::onCategoryChange,
            onReminderChange = viewModel::onReminderChange,
            onManageCategories = onManageCategories,
        ),
    )
}

class EditorActions(
    val onClose: () -> Unit,
    val onSave: () -> Unit,
    val onTitleChange: (String) -> Unit,
    val onAllDayChange: (Boolean) -> Unit,
    val onStartDateChange: (LocalDate) -> Unit,
    val onStartTimeChange: (LocalTime) -> Unit,
    val onEndDateChange: (LocalDate) -> Unit,
    val onEndTimeChange: (LocalTime) -> Unit,
    val onLocationChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onCategoryChange: (Long?) -> Unit,
    val onReminderChange: (Int?) -> Unit,
    val onManageCategories: () -> Unit,
)

private enum class EditorDialog { NONE, START_DATE, START_TIME, END_DATE, END_TIME, CATEGORY, REMINDER, DISCARD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(state: EventEditorUiState, actions: EditorActions) {
    var dialog by rememberSaveable { mutableStateOf(EditorDialog.NONE) }
    val requestClose = { if (state.isDirty) dialog = EditorDialog.DISCARD else actions.onClose() }
    BackHandler(enabled = state.isDirty) { dialog = EditorDialog.DISCARD }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.calendar_new_event else R.string.editor_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(LifeLogIcons.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    TextButton(onClick = actions.onSave, enabled = state.draft != null && !state.isSaving) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        val draft = state.draft
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            when {
                state.notFound -> Text(
                    stringResource(R.string.event_not_found),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                draft == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> EditorForm(state, draft, actions, onOpenDialog = { dialog = it })
            }
        }
    }

    val draft = state.draft ?: return
    when (dialog) {
        EditorDialog.NONE -> Unit
        EditorDialog.START_DATE -> DatePickerModal(draft.startDate, actions.onStartDateChange) { dialog = EditorDialog.NONE }
        EditorDialog.END_DATE -> DatePickerModal(draft.endDate, actions.onEndDateChange) { dialog = EditorDialog.NONE }
        EditorDialog.START_TIME -> TimePickerModal(draft.start.toLocalTime(), actions.onStartTimeChange) { dialog = EditorDialog.NONE }
        EditorDialog.END_TIME -> TimePickerModal(draft.end.toLocalTime(), actions.onEndTimeChange) { dialog = EditorDialog.NONE }
        EditorDialog.CATEGORY -> SingleChoiceDialog(
            title = stringResource(R.string.event_category),
            options = listOf<Long?>(null) + state.categories.map { it.id },
            selected = draft.categoryId,
            label = { id -> state.categories.firstOrNull { it.id == id }?.name ?: stringResource(R.string.event_no_category) },
            onSelect = {
                actions.onCategoryChange(it)
                dialog = EditorDialog.NONE
            },
            onDismiss = { dialog = EditorDialog.NONE },
            extraButton = {
                TextButton(onClick = {
                    dialog = EditorDialog.NONE
                    actions.onManageCategories()
                }) { Text(stringResource(R.string.editor_manage_categories)) }
            },
        )
        EditorDialog.REMINDER -> SingleChoiceDialog(
            title = stringResource(R.string.event_reminder),
            options = listOf<Int?>(null) + REMINDER_OPTIONS,
            selected = draft.reminderMinutes.firstOrNull(),
            label = { it?.let { minutes -> reminderLabel(minutes, draft.allDay) } ?: stringResource(R.string.reminder_none) },
            onSelect = {
                actions.onReminderChange(it)
                dialog = EditorDialog.NONE
            },
            onDismiss = { dialog = EditorDialog.NONE },
        )
        EditorDialog.DISCARD -> ConfirmDialog(
            title = stringResource(R.string.editor_discard_title),
            message = stringResource(R.string.editor_discard_message),
            confirmLabel = stringResource(R.string.action_discard),
            dismissLabel = stringResource(R.string.action_keep_editing),
            onConfirm = actions.onClose,
            onDismiss = { dialog = EditorDialog.NONE },
        )
    }
}

@Composable
private fun EditorForm(
    state: EventEditorUiState,
    draft: EventDraft,
    actions: EditorActions,
    onOpenDialog: (EditorDialog) -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val titleError = state.validationError == EventValidationError.TITLE_MISSING
    // New appointments start in the title field; only once, not after rotation.
    val titleFocus = remember { FocusRequester() }
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (state.isNew && !autoFocused && draft.title.isEmpty()) {
            autoFocused = true
            titleFocus.requestFocus()
        }
    }
    val endError = state.validationError == EventValidationError.END_BEFORE_START

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = draft.title,
            onValueChange = actions.onTitleChange,
            label = { Text(stringResource(R.string.editor_field_title)) },
            isError = titleError,
            supportingText = if (titleError) {
                { Text(stringResource(R.string.editor_error_title)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(titleFocus),
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.calendar_all_day)) },
            trailingContent = { Switch(checked = draft.allDay, onCheckedChange = null) },
            modifier = Modifier.toggleable(value = draft.allDay, role = Role.Switch, onValueChange = actions.onAllDayChange),
        )

        DateTimeRow(
            label = stringResource(R.string.editor_starts),
            date = draft.startDate,
            time = draft.start.toLocalTime().takeUnless { draft.allDay },
            formats = formats,
            dateDescription = R.string.editor_start_date,
            timeDescription = R.string.editor_start_time,
            onDateClick = { onOpenDialog(EditorDialog.START_DATE) },
            onTimeClick = { onOpenDialog(EditorDialog.START_TIME) },
        )
        DateTimeRow(
            label = stringResource(R.string.editor_ends),
            date = draft.endDate,
            time = draft.end.toLocalTime().takeUnless { draft.allDay },
            formats = formats,
            dateDescription = R.string.editor_end_date,
            timeDescription = R.string.editor_end_time,
            onDateClick = { onOpenDialog(EditorDialog.END_DATE) },
            onTimeClick = { onOpenDialog(EditorDialog.END_TIME) },
            isError = endError,
        )
        if (endError) {
            Text(
                text = stringResource(R.string.editor_error_end),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        state.foreignZone?.let {
            Text(
                text = stringResource(R.string.event_time_zone, it.id),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        OutlinedTextField(
            value = draft.location,
            onValueChange = actions.onLocationChange,
            label = { Text(stringResource(R.string.event_location)) },
            leadingIcon = { Icon(LifeLogIcons.Place, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        val category = state.categories.firstOrNull { it.id == draft.categoryId }
        ListItem(
            leadingContent = { Icon(LifeLogIcons.Label, contentDescription = null) },
            overlineContent = { Text(stringResource(R.string.event_category)) },
            headlineContent = { Text(category?.name ?: stringResource(R.string.event_no_category)) },
            trailingContent = category?.let { { ColorDot(Color(it.colorArgb), size = 16.dp) } },
            modifier = Modifier.clickable(role = Role.Button) { onOpenDialog(EditorDialog.CATEGORY) },
        )

        ListItem(
            leadingContent = { Icon(LifeLogIcons.Notifications, contentDescription = null) },
            overlineContent = { Text(stringResource(R.string.event_reminder)) },
            headlineContent = {
                Text(
                    draft.reminderMinutes.firstOrNull()?.let { reminderLabel(it, draft.allDay) }
                        ?: stringResource(R.string.reminder_none),
                )
            },
            supportingContent = (draft.reminderMinutes.size - 1).takeIf { it > 0 }?.let { extra ->
                { Text(stringResource(R.string.editor_more_reminders, extra)) }
            },
            modifier = Modifier.clickable(role = Role.Button) { onOpenDialog(EditorDialog.REMINDER) },
        )

        OutlinedTextField(
            value = draft.notes,
            onValueChange = actions.onNotesChange,
            label = { Text(stringResource(R.string.event_notes)) },
            leadingIcon = { Icon(LifeLogIcons.Notes, contentDescription = null) },
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    date: LocalDate,
    time: LocalTime?,
    formats: DateTimeFormats,
    dateDescription: Int,
    timeDescription: Int,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    isError: Boolean = false,
) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        FlowRow(horizontalArrangement = Arrangement.End) {
            val dateText = formats.shortWeekdayDayMonth.format(date)
            val dateLabel = stringResource(dateDescription, formats.fullDate.format(date))
            TextButton(onClick = onDateClick, modifier = Modifier.semantics { contentDescription = dateLabel }) {
                Text(dateText, color = color)
            }
            if (time != null) {
                val timeText = formats.time(time)
                val timeLabel = stringResource(timeDescription, timeText)
                TextButton(onClick = onTimeClick, modifier = Modifier.semantics { contentDescription = timeLabel }) {
                    Text(timeText, color = color)
                }
            }
        }
    }
}
