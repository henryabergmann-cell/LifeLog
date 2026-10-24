package app.lifelog.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Current time and device zone. Injected so that tests are deterministic. */
interface TimeSource {
    fun now(): Instant

    /** Read on every call: the device zone can change while the app runs. */
    fun zone(): ZoneId

    fun today(): LocalDate = now().atZone(zone()).toLocalDate()
}
