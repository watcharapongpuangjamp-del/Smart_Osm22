package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Scheduler for background sync worker targeting Public Health Volunteer (OSM) data.
 */
object OsmSyncScheduler {
    private const val TAG = "OsmSyncScheduler"

    /**
     * Schedules periodic background sync every 15 minutes when connected to network.
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<OsmSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            OsmSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
        Log.i(TAG, "Enqueued periodic background sync worker for OSM data (every 15 min)")
    }

    /**
     * Enqueues a one-time immediate background sync worker for OSM data.
     */
    fun triggerOneTimeSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeSyncRequest = OneTimeWorkRequestBuilder<OsmSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            OsmSyncWorker.ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeSyncRequest
        )
        Log.i(TAG, "Enqueued one-time immediate background sync worker for OSM data")
    }

    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(OsmSyncWorker.WORK_NAME)
    }
}
