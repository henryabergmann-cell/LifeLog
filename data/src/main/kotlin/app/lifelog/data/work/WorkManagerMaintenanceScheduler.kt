package app.lifelog.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.lifelog.domain.maintenance.MaintenanceScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class WorkManagerMaintenanceScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MaintenanceScheduler {

    override fun schedule() {
        val purge = PeriodicWorkRequestBuilder<PurgeDeletedEventsWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PURGE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, purge)
    }

    private companion object {
        const val PURGE_WORK_NAME = "purge-deleted-events"
    }
}
