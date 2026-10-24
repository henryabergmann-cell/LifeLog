package app.lifelog.feature.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lifelog.domain.model.Category
import app.lifelog.domain.premium.PremiumManager
import app.lifelog.domain.repository.CategoryRepository
import app.lifelog.domain.repository.SettingsRepository
import app.lifelog.domain.stats.Statistics
import app.lifelog.domain.stats.StatisticsCalculator
import app.lifelog.domain.stats.StatsBucket
import app.lifelog.domain.time.TimeSource
import app.lifelog.domain.usecase.GetOccurrencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

enum class InsightsRange(val bucket: StatsBucket) {
    LAST_4_WEEKS(StatsBucket.WEEK),
    LAST_12_WEEKS(StatsBucket.WEEK),
    LAST_12_MONTHS(StatsBucket.MONTH),
}

data class InsightsUiState(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val range: InsightsRange = InsightsRange.LAST_4_WEEKS,
    val from: LocalDate = LocalDate.MIN,
    val toExclusive: LocalDate = LocalDate.MIN,
    val statistics: Statistics? = null,
    val categories: Map<Long, Category> = emptyMap(),
) {
    /** Average appointments per week over the selected range. */
    val perWeek: Double
        get() {
            val stats = statistics ?: return 0.0
            val days = java.time.temporal.ChronoUnit.DAYS.between(from, toExclusive).coerceAtLeast(1)
            return stats.appointmentCount * 7.0 / days
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    getOccurrences: GetOccurrencesUseCase,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    premiumManager: PremiumManager,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val range = MutableStateFlow(
        savedStateHandle.get<String>(KEY_RANGE)?.let { name -> InsightsRange.entries.firstOrNull { it.name == name } }
            ?: InsightsRange.LAST_4_WEEKS,
    )

    private data class Query(val premium: Boolean, val range: InsightsRange, val firstDay: DayOfWeek, val today: LocalDate)

    val uiState: StateFlow<InsightsUiState> = combine(
        premiumManager.isPremium,
        range,
        settingsRepository.settings.map { it.firstDayOfWeek.resolve(Locale.getDefault()) },
    ) { premium, range, firstDay -> Query(premium, range, firstDay, timeSource.today()) }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            // Free users see a teaser; don't compute anything for them.
            if (!query.premium) return@flatMapLatest flowOf(InsightsUiState(isLoading = false, isPremium = false, range = query.range))
            val (from, toExclusive) = dates(query.range, query.today, query.firstDay)
            combine(
                getOccurrences(from, toExclusive, timeSource.zone()),
                categoryRepository.categories,
            ) { occurrences, categories ->
                InsightsUiState(
                    isLoading = false,
                    isPremium = true,
                    range = query.range,
                    from = from,
                    toExclusive = toExclusive,
                    statistics = StatisticsCalculator.calculate(occurrences, from, toExclusive, query.range.bucket, query.firstDay),
                    categories = categories.associateBy { it.id },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun onRangeSelected(selected: InsightsRange) {
        range.value = selected
        savedStateHandle[KEY_RANGE] = selected.name
    }

    companion object {
        private const val KEY_RANGE = "insightsRange"

        /** Whole weeks or months up to and including today. */
        fun dates(range: InsightsRange, today: LocalDate, firstDay: DayOfWeek): Pair<LocalDate, LocalDate> {
            val current = StatisticsCalculator.bucketStart(today, range.bucket, firstDay)
            val from = when (range) {
                InsightsRange.LAST_4_WEEKS -> current.minusWeeks(3)
                InsightsRange.LAST_12_WEEKS -> current.minusWeeks(11)
                InsightsRange.LAST_12_MONTHS -> current.minusMonths(11)
            }
            return from to today.plusDays(1)
        }
    }
}
