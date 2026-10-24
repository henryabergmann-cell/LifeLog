package app.lifelog.domain.stats

import app.lifelog.domain.model.Occurrence
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class StatsBucket { WEEK, MONTH }

data class BucketCount(val start: LocalDate, val count: Int)

data class CategoryCount(val categoryId: Long?, val count: Int)

data class Statistics(
    val appointmentCount: Int,
    /** Sum of timed appointment durations; all-day appointments don't count as hours. */
    val totalTimedMinutes: Long,
    /** Appointments per week or month, oldest first, including empty periods. */
    val buckets: List<BucketCount>,
    /** Appointments per weekday, starting with the first day of the week. */
    val weekdayCounts: List<Pair<DayOfWeek, Int>>,
    /** Start hour (0–23) with the most timed appointments; null if there are none. */
    val busiestHour: Int?,
    /** Most used categories first; a null ID means "no category". */
    val categoryCounts: List<CategoryCount>,
    val busiestDate: LocalDate?,
    val busiestDateCount: Int,
) {
    val isEmpty: Boolean get() = appointmentCount == 0
}

/**
 * Statistics over occurrences that *start* in [from, toExclusive). Counting by start date
 * means a multi-day appointment is counted once.
 */
object StatisticsCalculator {

    fun calculate(
        occurrences: List<Occurrence>,
        from: LocalDate,
        toExclusive: LocalDate,
        bucket: StatsBucket,
        firstDayOfWeek: DayOfWeek,
    ): Statistics {
        val inRange = occurrences.filter {
            val date = it.start.toLocalDate()
            !date.isBefore(from) && date.isBefore(toExclusive)
        }
        val timed = inRange.filterNot { it.isAllDay }

        val bucketCounts = inRange.groupingBy { bucketStart(it.start.toLocalDate(), bucket, firstDayOfWeek) }.eachCount()
        val buckets = generateSequence(bucketStart(from, bucket, firstDayOfWeek)) { next(it, bucket) }
            .takeWhile { it.isBefore(toExclusive) }
            .map { BucketCount(it, bucketCounts[it] ?: 0) }
            .toList()

        val weekdayCounts = inRange.groupingBy { it.start.dayOfWeek }.eachCount()
        val weekdays = (0L until 7L).map { firstDayOfWeek.plus(it) }.map { it to (weekdayCounts[it] ?: 0) }

        val busiestHour = timed.groupingBy { it.start.hour }.eachCount()
            .maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key })?.key

        val busiestDay = inRange.groupingBy { it.start.toLocalDate() }.eachCount()
            .maxWithOrNull(compareBy<Map.Entry<LocalDate, Int>> { it.value }.thenByDescending { it.key })

        return Statistics(
            appointmentCount = inRange.size,
            totalTimedMinutes = timed.sumOf { Duration.between(it.start, it.end).toMinutes().coerceAtLeast(0) },
            buckets = buckets,
            weekdayCounts = weekdays,
            busiestHour = busiestHour,
            categoryCounts = inRange.groupingBy { it.event.categoryId }.eachCount()
                .map { (id, count) -> CategoryCount(id, count) }
                .sortedWith(compareByDescending<CategoryCount> { it.count }.thenBy { it.categoryId ?: Long.MAX_VALUE }),
            busiestDate = busiestDay?.key,
            busiestDateCount = busiestDay?.value ?: 0,
        )
    }

    fun bucketStart(date: LocalDate, bucket: StatsBucket, firstDayOfWeek: DayOfWeek): LocalDate = when (bucket) {
        StatsBucket.WEEK -> date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        StatsBucket.MONTH -> date.withDayOfMonth(1)
    }

    private fun next(start: LocalDate, bucket: StatsBucket): LocalDate = when (bucket) {
        StatsBucket.WEEK -> start.plusWeeks(1)
        StatsBucket.MONTH -> start.plusMonths(1)
    }
}
