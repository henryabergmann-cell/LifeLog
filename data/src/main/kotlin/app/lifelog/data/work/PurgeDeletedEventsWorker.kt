package app.lifelog.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.lifelog.domain.repository.EventRepository
import app.lifelog.domain.time.TimeSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration

/**
 * Permanently removes events that were deleted (and not undone) more than a day ago.
 * The day of slack keeps any undo snackbar that is still on screen working.
 */
@HiltWorker
internal class PurgeDeletedEventsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: EventRepository,
    private val timeSource: TimeSource,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        repository.purgeDeleted(olderThan = timeSource.now().minus(Duration.ofDays(1)))
        return Result.success()
    }
}
