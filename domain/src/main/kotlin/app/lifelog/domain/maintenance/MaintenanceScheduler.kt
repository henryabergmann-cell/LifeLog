package app.lifelog.domain.maintenance

/** Schedules recurring background housekeeping (e.g. purging deleted events). Idempotent. */
interface MaintenanceScheduler {
    fun schedule()
}
