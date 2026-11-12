package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.firestore.FirestoreManager
import kotlinx.coroutines.CancellationException

/**
 * Background WorkManager worker responsible for syncing locally registered
 * Public Health Volunteer (VHV/OSM) records with Cloud Firestore.
 *
 * The worker deliberately does not seed records from the retired OSMRP00002
 * dataset. Room is the source of locally registered records.
 */
class OsmSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "OsmSyncWorker"
        const val WORK_NAME = "OsmRp00002PeriodicSyncWork"
        const val ONE_TIME_WORK_NAME = "OsmRp00002OneTimeSyncWork"
    }

    override suspend fun doWork(): Result {
        return try {
            if (isStopped) {
                return Result.success()
            }

            Log.d(TAG, "Starting background sync worker for VHV records...")
            val appContext = applicationContext

            FirestoreManager.initialize(appContext)
            val firestore = FirestoreManager.getInstance()
            if (firestore == null) {
                Log.w(TAG, "Firestore instance unavailable. Background VHV sync will be retried.")
                return Result.retry()
            }

            // Use the shared Room database instance and its registered VHV records.
            val db = AppDatabase.getInstance(appContext)
            val vhvMemberDao = db.vhvMemberDao()

            val syncHelper = VhvFirestoreSyncHelper(
                context = appContext,
                vhvMemberDao = vhvMemberDao,
                firestoreProvider = { firestore }
            )

            val syncResult = syncHelper.syncOsmData()
            if (syncResult.isSuccess) {
                val result = syncResult.getOrNull()
                Log.i(
                    TAG,
                    "VHV background sync completed: ${result?.personsSynced ?: 0} records processed."
                )
                Result.success()
            } else {
                Log.w(
                    TAG,
                    "VHV background sync failed: ${syncResult.exceptionOrNull()?.message}"
                )
                Result.retry()
            }
        } catch (ce: CancellationException) {
            Log.d(TAG, "OsmSyncWorker cancelled normally.")
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Exception in OsmSyncWorker execution", e)
            Result.retry()
        }
    }
}
