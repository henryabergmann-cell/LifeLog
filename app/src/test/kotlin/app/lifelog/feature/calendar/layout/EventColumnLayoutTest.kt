package app.lifelog.feature.calendar.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class EventColumnLayoutTest {

    private fun layout(vararg items: Pair<Int, Int>, minDuration: Int = 0) =
        EventColumnLayout.layout(items.map { TimedItem(it.first, it.second) }, minDuration)

    @Test
    fun `separate events take the full width`() {
        assertEquals(
            listOf(Placement(0, 1), Placement(0, 1)),
            layout(540 to 600, 600 to 660), // touching, not overlapping
        )
    }

    @Test
    fun `overlapping events share the width`() {
        assertEquals(
            listOf(Placement(0, 2), Placement(1, 2)),
            layout(540 to 660, 600 to 720),
        )
    }

    @Test
    fun `a chain of overlaps reuses freed columns`() {
        // A overlaps B, B overlaps C, A ends before C starts: two columns suffice.
        assertEquals(
            listOf(Placement(0, 2), Placement(1, 2), Placement(0, 2)),
            layout(0 to 60, 30 to 90, 70 to 100),
        )
    }

    @Test
    fun `results keep input order regardless of start order`() {
        assertEquals(
            listOf(Placement(1, 2), Placement(0, 2)),
            layout(600 to 720, 540 to 660),
        )
    }

    @Test
    fun `short events are padded to the minimum drawn height`() {
        // 9:00-9:05 and 9:10-9:20 don't overlap in time, but both are drawn 20 minutes tall.
        assertEquals(
            listOf(Placement(0, 2), Placement(1, 2)),
            layout(540 to 545, 550 to 560, minDuration = 20),
        )
    }

    @Test
    fun `three simultaneous events get three columns`() {
        assertEquals(
            listOf(Placement(0, 3), Placement(1, 3), Placement(2, 3)),
            layout(0 to 120, 0 to 60, 30 to 90),
        )
    }
}
