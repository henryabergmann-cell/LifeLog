package app.lifelog.feature.calendar.layout

/** A timed item within one day, in minutes from midnight. */
data class TimedItem(val startMinute: Int, val endMinute: Int)

/** Horizontal slot of an item: column [column] of [columnCount] equal columns. */
data class Placement(val column: Int, val columnCount: Int)

/**
 * Side-by-side layout for overlapping events in the day and week time grids.
 *
 * Items are grouped into clusters of transitively overlapping items. Within a cluster each
 * item takes the leftmost column that is free at its start, and all members share the
 * cluster's column count, so widths line up.
 */
object EventColumnLayout {

    /**
     * Returns one placement per item, in input order. Items shorter than [minDurationMinutes]
     * are treated as that long, since they are drawn at a minimum height.
     */
    fun layout(items: List<TimedItem>, minDurationMinutes: Int = 0): List<Placement> {
        val placements = arrayOfNulls<Placement>(items.size)
        val order = items.indices.sortedWith(
            compareBy<Int>({ items[it].startMinute }, { -effectiveEnd(items[it], minDurationMinutes) }),
        )

        val columnEnds = mutableListOf<Int>()
        val cluster = mutableListOf<Pair<Int, Int>>() // item index to column
        var clusterEnd = Int.MIN_VALUE

        fun closeCluster() {
            cluster.forEach { (index, column) -> placements[index] = Placement(column, columnEnds.size) }
            cluster.clear()
            columnEnds.clear()
        }

        for (index in order) {
            val item = items[index]
            val end = effectiveEnd(item, minDurationMinutes)
            if (cluster.isNotEmpty() && item.startMinute >= clusterEnd) closeCluster()

            var column = columnEnds.indexOfFirst { it <= item.startMinute }
            if (column == -1) {
                columnEnds += end
                column = columnEnds.lastIndex
            } else {
                columnEnds[column] = end
            }
            cluster += index to column
            clusterEnd = if (cluster.size == 1) end else maxOf(clusterEnd, end)
        }
        closeCluster()

        return placements.map { checkNotNull(it) }
    }

    private fun effectiveEnd(item: TimedItem, minDuration: Int): Int =
        maxOf(item.endMinute, item.startMinute + minDuration)
}
