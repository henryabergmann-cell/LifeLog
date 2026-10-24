package app.lifelog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifelog.domain.usecase.DeletedEvent
import app.lifelog.domain.usecase.EventDeletion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App-wide state for the shell: currently the pending "deleted, undo?" message. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val eventDeletion: EventDeletion,
) : ViewModel() {

    val lastDeleted: StateFlow<DeletedEvent?> = eventDeletion.lastDeleted

    fun onUndo(deleted: DeletedEvent) {
        viewModelScope.launch { eventDeletion.undo(deleted) }
    }

    fun onUndoDismissed(deleted: DeletedEvent) = eventDeletion.dismiss(deleted)
}
