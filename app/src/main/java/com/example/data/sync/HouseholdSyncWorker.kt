package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.PersonRepository
import com.example.data.firestore.FirestoreManager

class HouseholdSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "HouseholdSyncWorker"
        const val WORK_NAME = "HouseholdPeriodicSyncWork"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting periodic background sync of household registration data...")
            val appContext = applicationContext
            
            FirestoreManager.initialize(appContext)
            if (FirestoreManager.getInstance() == null) {
                Log.w(TAG, "Firestore is not configured. Skipping background sync.")
                return Result.success()
            }

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
                AppDatabase.MIGRATION_8_9
            ).build()

            val repository = PersonRepository(db, db.personDao(), db.householdDao(), db.personHistoryDao(), db.populationEventDao())
            val syncHelper = RoomFirestoreSyncHelper(
                appContext,
                repository,
                firestoreProvider = { FirestoreManager.getInstance() }
            )

            val syncResult = syncHelper.syncRoomToFirestore()
            if (syncResult.isSuccess) {
                val result = syncResult.getOrNull()
                Log.i(TAG, "Background sync completed successfully: ${result?.householdsSynced} households, ${result?.personsSynced} persons synced.")
                Result.success()
            } else {
                Log.e(TAG, "Background sync failed: ${syncResult.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during background sync worker execution", e)
            Result.retry()
        }
    }
}
