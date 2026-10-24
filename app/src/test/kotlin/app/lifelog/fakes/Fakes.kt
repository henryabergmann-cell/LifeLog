package app.lifelog.fakes

import app.lifelog.domain.model.AppSettings
import app.lifelog.domain.model.Category
import app.lifelog.domain.model.Event
import app.lifelog.domain.model.JournalEntry
import app.lifelog.domain.repository.JournalRepository
import java.time.LocalDate
import app.lifelog.domain.premium.PremiumManager
import app.lifelog.domain.repository.CategoryRepository
import app.lifelog.domain.repository.EventRepository
import app.lifelog.domain.repository.SettingsRepository
import app.lifelog.domain.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<AppSettings> = state
    val current: AppSettings get() = state.value

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        state.update(transform)
    }
}

class FakeTimeSource(
    var instant: Instant = Instant.parse("2026-10-07T10:00:00Z"),
    var zoneId: ZoneId = ZoneOffset.UTC,
) : TimeSource {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
}

class FakePremiumManager(premium: Boolean = false) : PremiumManager {
    override val isPremium = MutableStateFlow(premium)
}

/** In-memory events; range filtering is left to the expander, like the real candidate query. */
class FakeEventRepository : EventRepository {
    private val events = MutableStateFlow<Map<Long, Event>>(emptyMap())
    private val deleted = MutableStateFlow<Set<Long>>(emptySet())
    private var nextId = 1L

    val all: List<Event> get() = events.value.values.toList()

    private val visible: Flow<List<Event>> =
        events.map { map -> map.values.filter { it.id !in deleted.value } }

    override fun observeCandidates(start: Instant, end: Instant): Flow<List<Event>> = visible

    override fun observeEvent(id: Long): Flow<Event?> = events.map { it[id]?.takeIf { e -> e.id !in deleted.value } }

    override suspend fun getEvent(id: Long): Event? = events.value[id]?.takeIf { it.id !in deleted.value }

    override suspend fun save(event: Event): Long {
        val id = if (event.id == 0L) nextId++ else event.id
        val uid = event.uid.ifBlank { "uid-$id" }
        events.update { it + (id to event.copy(id = id, uid = uid)) }
        return id
    }

    override suspend fun softDelete(id: Long) {
        deleted.update { it + id }
        events.update { it.toMap() } // re-emit
    }

    override suspend fun restore(id: Long) {
        deleted.update { it - id }
        events.update { it.toMap() }
    }

    override suspend fun purgeDeleted(olderThan: Instant): Int = 0

    override fun search(query: String): Flow<List<Event>> =
        visible.map { list -> list.filter { it.title.contains(query, ignoreCase = true) } }
}

class FakeJournalRepository : JournalRepository {
    private val state = MutableStateFlow<Map<LocalDate, JournalEntry>>(emptyMap())
    var saveCount = 0
        private set

    override val entries: Flow<List<JournalEntry>> = state.map { it.values.sortedByDescending { e -> e.date } }

    override fun observe(date: LocalDate): Flow<JournalEntry?> = state.map { it[date] }

    override suspend fun save(entry: JournalEntry) {
        saveCount++
        state.update { it + (entry.date to entry) }
    }

    override suspend fun delete(date: LocalDate) = state.update { it - date }
}

class FakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    private val state = MutableStateFlow(initial)
    override val categories: Flow<List<Category>> = state

    override suspend fun count(): Int = state.value.size

    override suspend fun save(category: Category): Long {
        if (category.id != 0L) {
            state.update { list -> list.map { if (it.id == category.id) category else it } }
            return category.id
        }
        val id = (state.value.maxOfOrNull { it.id } ?: 0L) + 1
        state.update { it + category.copy(id = id, sortOrder = it.size) }
        return id
    }

    override suspend fun delete(id: Long) = state.update { list -> list.filterNot { it.id == id } }
}
