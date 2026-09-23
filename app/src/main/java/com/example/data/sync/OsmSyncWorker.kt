package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.firestore.FirestoreManager
import com.example.data.vhv.OsmRp00002Data

/**
 * Background WorkManager worker responsible for syncing Public Health Volunteer (OSM)
 * data from the OSMRP00002 dataset into Firestore database for offline persistence and access.
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
            Log.d(TAG, "Starting background sync worker for OSM (Public Health Volunteer) data...")
            val appContext = applicationContext

            // Initialize Cloud Firestore with offline persistence
            FirestoreManager.initialize(appContext)
            val firestore = FirestoreManager.getInstance()
            if (firestore == null) {
                Log.w(TAG, "Firestore instance unavailable. Skipping background OSM sync.")
                return Result.success()
            }

            // Build Room database instance with full migration chain
            val db = Room.databaseBuilder(
                appContext,
                AppDatabase::class.java, "person_db"
            ).addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11
            ).build()

            val vhvMemberDao = db.vhvMemberDao()

            // Seed local database if empty
            if (vhvMemberDao.getVhvCount() == 0) {
                Log.i(TAG, "Seeding OSMRP00002 VHV dataset into Room...")
                vhvMemberDao.insertAll(OsmRp00002Data.PA_KHA_VHV_MEMBERS)
            }

            val syncHelper = VhvFirestoreSyncHelper(
                context = appContext,
                vhvMemberDao = vhvMemberDao,
                firestoreProvider = { firestore }
            )

            val syncResult = syncHelper.syncOsmData()
            if (syncResult.isSuccess) {
                val res = syncResult.getOrNull()
                Log.i(TAG, "OSM background sync finished successfully: ${res?.personsSynced} records processed.")
                Result.success()
            } else {
                Log.e(TAG, "OSM background sync failed: ${syncResult.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal exception in OsmSyncWorker execution", e)
            Result.retry()
        }
    }
}
