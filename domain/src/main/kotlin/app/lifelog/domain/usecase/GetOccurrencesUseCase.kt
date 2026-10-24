package app.lifelog.domain.usecase

import app.lifelog.domain.model.Occurrence
import app.lifelog.domain.occurrence.OccurrenceExpander
import app.lifelog.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetOccurrencesUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    /** Occurrences overlapping [from] (inclusive) to [toExclusive] in [zone], kept up to date. */
    operator fun invoke(from: LocalDate, toExclusive: LocalDate, zone: ZoneId): Flow<List<Occurrence>> {
        // Padded by a day on each side: all-day events are stored at UTC midnight, and zone
        // offsets reach ±14 h. The expander filters precisely afterwards.
        val start = from.minusDays(1).atStartOfDay(zone).toInstant()
        val end = toExclusive.plusDays(1).atStartOfDay(zone).toInstant()
        return repository.observeCandidates(start, end)
            .map { events -> OccurrenceExpander.expand(events, from, toExclusive, zone) }
    }
}
