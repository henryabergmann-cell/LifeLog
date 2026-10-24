package app.lifelog.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifelog.R
import app.lifelog.designsystem.component.CategoryPalette
import app.lifelog.designsystem.component.ColorDot
import app.lifelog.designsystem.component.ConfirmDialog
import app.lifelog.designsystem.component.contentColorOn
import app.lifelog.designsystem.icon.LifeLogIcons

@Composable
fun CategoriesRoute(
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoriesScreen(state, viewModel, onBack, onOpenPremium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesScreen(
    state: CategoriesUiState,
    viewModel: CategoriesViewModel,
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(LifeLogIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAdd) {
                Icon(LifeLogIcons.Add, contentDescription = stringResource(R.string.categories_add))
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!state.limits.isPremium && !state.isLoading) {
                item(key = "limit") {
                    Text(
                        text = stringResource(
                            R.string.categories_free_limit,
                            state.categories.size.coerceAtMost(state.limits.maxCategories),
                            state.limits.maxCategories,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            if (state.categories.isEmpty() && !state.isLoading) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.categories_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.categories, key = { it.id }) { category ->
                ListItem(
                    leadingContent = { ColorDot(Color(category.colorArgb), size = 24.dp) },
                    headlineContent = { Text(category.name) },
                    trailingContent = {
                        androidx.compose.foundation.layout.Row {
                            IconButton(onClick = { viewModel.onEdit(category) }) {
                                Icon(LifeLogIcons.Edit, contentDescription = stringResource(R.string.categories_edit_named, category.name))
                            }
                            IconButton(onClick = { viewModel.onDeleteRequest(category) }) {
                                Icon(LifeLogIcons.Delete, contentDescription = stringResource(R.string.categories_delete_named, category.name))
                            }
                        }
                    },
                )
            }
        }
    }

    state.dialog?.let { dialog ->
        CategoryDialog(
            dialog = dialog,
            onNameChange = viewModel::onNameChange,
            onColorChange = viewModel::onColorChange,
            onSave = viewModel::onSaveDialog,
            onDismiss = viewModel::onDismissDialog,
        )
    }
    state.pendingDelete?.let { category ->
        ConfirmDialog(
            title = stringResource(R.string.categories_delete_title, category.name),
            message = stringResource(R.string.categories_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = viewModel::onConfirmDelete,
            onDismiss = viewModel::onDismissDelete,
        )
    }
    state.limitReached?.let { limit ->
        AlertDialog(
            onDismissRequest = viewModel::onLimitMessageDismissed,
            text = { Text(stringResource(R.string.categories_limit_reached, limit)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onLimitMessageDismissed()
                    onOpenPremium()
                }) { Text(stringResource(R.string.premium_learn_more)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onLimitMessageDismissed) { Text(stringResource(R.string.action_close)) }
            },
        )
    }
}

@Composable
private fun CategoryDialog(
    dialog: CategoryDialogState,
    onNameChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (dialog.isNew) R.string.categories_add else R.string.categories_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = dialog.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.categories_name)) },
                    isError = dialog.nameError,
                    supportingText = if (dialog.nameError) {
                        { Text(stringResource(R.string.categories_error_name)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.categories_color), style = MaterialTheme.typography.labelLarge)
                FlowRow(Modifier.selectableGroup()) {
                    CategoryPalette.presets.forEach { preset ->
                        val selected = preset.argb == dialog.colorArgb
                        val name = stringResource(preset.name)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .selectable(selected = selected, role = Role.RadioButton) { onColorChange(preset.argb) }
                                .semantics { contentDescription = name },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(preset.argb), CircleShape)
                                    .then(
                                        if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier,
                                    ),
                            ) {
                                if (selected) {
                                    Icon(LifeLogIcons.Check, contentDescription = null, tint = contentColorOn(Color(preset.argb)))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
