package app.lifelog.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.lifelog.data.db.entity.CategoryEntity
import app.lifelog.data.db.entity.EventEntity
import app.lifelog.data.db.entity.ReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifeLogDatabaseTest {

    private lateinit var db: LifeLogDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifeLogDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun rangeQueryReturnsOverlappingOneOffEventsOnly() = runTest {
        val dao = db.eventDao()
        dao.insert(event(uid = "before", startUtc = 0, endUtc = 1_000))
        dao.insert(event(uid = "overlap", startUtc = 1_500, endUtc = 2_500))
        dao.insert(event(uid = "after", startUtc = 5_000, endUtc = 6_000))

        val result = dao.observeInRange(startUtc = 2_000, endUtc = 4_000).first()

        assertEquals(listOf("overlap"), result.map { it.uid })
    }

    @Test
    fun rangeQueryIncludesSeriesThatOverlapTheRange() = runTest {
        val dao = db.eventDao()
        dao.insert(event(uid = "infinite", startUtc = 0, endUtc = 100, rrule = "FREQ=DAILY", seriesEndUtc = null))
        dao.insert(event(uid = "ended", startUtc = 0, endUtc = 100, rrule = "FREQ=DAILY", seriesEndUtc = 500))
        dao.insert(event(uid = "future", startUtc = 9_000, endUtc = 9_100, rrule = "FREQ=DAILY", seriesEndUtc = null))

        val result = dao.observeInRange(startUtc = 1_000, endUtc = 2_000).first()

        assertEquals(listOf("infinite"), result.map { it.uid })
    }

    @Test
    fun softDeletedEventsAreHiddenAndRestorable() = runTest {
        val dao = db.eventDao()
        val id = dao.insert(event(uid = "a", startUtc = 0, endUtc = 100))

        dao.softDelete(id, deletedAt = 1)
        assertTrue(dao.observeInRange(0, 1_000).first().isEmpty())

        dao.restore(id)
        assertEquals(1, dao.observeInRange(0, 1_000).first().size)
    }

    @Test
    fun fullTextSearchMatchesTitleLocationAndNotesIgnoringDiacritics() = runTest {
        val dao = db.eventDao()
        dao.insert(event(uid = "1", title = "Zahnarzt", location = "Praxis Müller"))
        dao.insert(event(uid = "2", title = "Lunch", notes = "bring the slides"))
        dao.insert(event(uid = "3", title = "Gym"))

        assertEquals(listOf("1"), dao.search("muller").first().map { it.uid })
        assertEquals(listOf("2"), dao.search("slid*").first().map { it.uid })
    }

    @Test
    fun deletingAnEventCascadesToItsReminders() = runTest {
        val eventId = db.eventDao().insert(event(uid = "a", startUtc = 0, endUtc = 100))
        db.reminderDao().insertAll(listOf(ReminderEntity(eventId = eventId, minutesBefore = 10)))

        db.eventDao().softDelete(eventId, deletedAt = 1)
        db.eventDao().purgeDeletedBefore(before = 2)

        assertTrue(db.reminderDao().getForEvent(eventId).isEmpty())
    }

    @Test
    fun deletingACategoryClearsItOnEvents() = runTest {
        val categoryId = db.categoryDao().insert(
            CategoryEntity(name = "Work", colorArgb = 0, isCustomColor = false, sortOrder = 0, createdAt = 0),
        )
        val eventId = db.eventDao().insert(event(uid = "a", categoryId = categoryId))

        db.categoryDao().delete(categoryId)

        assertNull(db.eventDao().getById(eventId)?.categoryId)
    }

    private fun event(
        uid: String,
        title: String = "Event $uid",
        location: String? = null,
        notes: String? = null,
        categoryId: Long? = null,
        startUtc: Long = 0,
        endUtc: Long = 100,
        rrule: String? = null,
        seriesEndUtc: Long? = null,
    ) = EventEntity(
        uid = uid,
        title = title,
        location = location,
        notes = notes,
        categoryId = categoryId,
        allDay = false,
        startDate = 0,
        endDate = 0,
        startTime = 0,
        endTime = 0,
        zoneId = "UTC",
        startUtc = startUtc,
        endUtc = endUtc,
        rrule = rrule,
        seriesEndUtc = seriesEndUtc,
        seriesMasterId = null,
        originalStart = null,
        deletedAt = null,
        createdAt = 0,
        updatedAt = 0,
    )
}
