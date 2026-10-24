package app.lifelog.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifelog.domain.model.Category
import app.lifelog.domain.model.Occurrence
import app.lifelog.domain.occurrence.OccurrenceExpander
import app.lifelog.domain.repository.CategoryRepository
import app.lifelog.domain.repository.EventRepository
import app.lifelog.domain.time.TimeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SearchResults {
    data object Idle : SearchResults

    data class Found(val query: String, val occurrences: List<Occurrence>) : SearchResults
}

data class SearchUiState(
    val results: SearchResults = SearchResults.Idle,
    val categories: Map<Long, Category> = emptyMap(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: EventRepository,
    categoryRepository: CategoryRepository,
    timeSource: TimeSource,
) : ViewModel() {

    /** What the user typed; bound to the text field without delay. */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    val uiState: StateFlow<SearchUiState> = combine(
        query
            .debounce(DEBOUNCE_MS)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { text ->
                if (text.isEmpty()) {
                    flowOf(SearchResults.Idle)
                } else {
                    repository.search(text).map { events ->
                        val zone = timeSource.zone()
                        SearchResults.Found(
                            query = text,
                            occurrences = events.map { OccurrenceExpander.occurrenceOf(it, zone) }.sortedByDescending { it.start },
                        )
                    }
                }
            },
        categoryRepository.categories.map { list -> list.associateBy { it.id } },
    ) { results, categories -> SearchUiState(results, categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(text: String) {
        savedStateHandle[KEY_QUERY] = text
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val DEBOUNCE_MS = 250L
    }
}
