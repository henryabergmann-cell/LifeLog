package app.lifelog.data.time

import app.lifelog.domain.time.TimeSource
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

internal class SystemTimeSource @Inject constructor() : TimeSource {
    override fun now(): Instant = Instant.now()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
