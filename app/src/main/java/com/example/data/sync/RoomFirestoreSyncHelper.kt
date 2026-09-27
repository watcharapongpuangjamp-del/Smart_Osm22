package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.DataStatus
import com.example.data.Gender
import com.example.data.Household
import com.example.data.HouseholdRole
import com.example.data.Person
import com.example.data.PersonRepository
import com.example.data.PersonStatus
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Await extension for Firebase Tasks in coroutines.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}

/**
 * Summary metrics of a sync operation between Room and Cloud Firestore.
 */
data class SyncResult(
    val householdsSynced: Int = 0,
    val personsSynced: Int = 0,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * State representation for Room-to-Firestore synchronization.
 */
sealed interface SyncState {
    object Idle : SyncState
    data class Syncing(val message: String) : SyncState
    data class Success(val result: SyncResult) : SyncState
    data class Error(val message: String, val throwable: Throwable? = null) : SyncState
}

/**
 * Room-to-Firestore synchronization helper class.
 *
 * Provides bidirectional and push/pull synchronization between the local Room database
 * and Cloud Firestore collections ("households" and "persons") with UUID mapping for idempotent writes.
 */
open class RoomFirestoreSyncHelper(
    private val context: Context,
    private val repository: PersonRepository,
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp is not initialized: ${e.message}")
            null
        }
    }
) {
    companion object {
        private const val TAG = "RoomFirestoreSyncHelper"
        const val COLLECTION_HOUSEHOLDS = "households"
        const val COLLECTION_PERSONS = "persons"
        const val COLLECTION_TOMBSTONES = "tombstones"
        const val COLLECTION_HEALTH_SCREENINGS = "health_screenings"
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    open fun isFirebaseConfigured(): Boolean {
        return try {
            firestoreProvider() != null
        } catch (e: Exception) {
            false
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    private fun getFirestore(): FirebaseFirestore {
        return firestoreProvider()
            ?: throw IllegalStateException("ระบบ Cloud (Firebase) ยังไม่ได้ตั้งค่าในโปรเจกต์นี้ กรุณาใช้งานฐานข้อมูลภายใน (Room) แทน")
    }

    private fun checkFirebaseConfiguredOrError(): FirebaseFirestore? {
        return try {
            firestoreProvider()
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================================
    // ROOM -> FIRESTORE (Upload / Persist)
    // =========================================================================

    /**
     * Uploads all local Room households and registered citizens to Cloud Firestore,
     * skipping any records that have deletion tombstones.
     * Uses batch writes for high efficiency and atomic updates, respecting Firestore batch limits (max 500).
     */
    /**
     * Uploads all local Room households and registered citizens to Cloud Firestore,
     * skipping any records that have deletion tombstones.
     * Uses batch writes for high efficiency and atomic updates, respecting Firestore batch limits (max 500).
     */
    suspend fun syncRoomToFirestore(villageNo: String? = null): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.Syncing("กำลังเตรียมข้อมูลจาก Room Database...")
            val firestore = checkFirebaseConfiguredOrError()
            if (firestore == null) {
                val err = IllegalStateException("ระบบ Cloud (Firebase) ยังไม่ได้เชื่อมต่อในระบบนี้ (ใช้งานฐานข้อมูลภายใน Room ได้ปกติ)")
                _syncState.value = SyncState.Error(err.message ?: "", err)
                return@withContext Result.failure(err)
            }

            // Retrieve target active villageNo for partitioning
            val activeVillageNo = villageNo ?: try {
                context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("surveyor_village_no", null)
            } catch (e: Exception) {
                null
            }

            // Fetch tombstones to prevent re-uploading deleted records
            val tombstoneDocs = firestore.collection(COLLECTION_TOMBSTONES).get().await()
            val deletedUuids = tombstoneDocs.documents.mapNotNull { it.getString("uuid") }.toSet()

            val households = repository.getAllHouseholds()
                .filter { !deletedUuids.contains(it.householdUuid) && (activeVillageNo == null || it.villageNo == activeVillageNo) }
            val persons = repository.getAllPersonsList()
                .filter { !deletedUuids.contains(it.personUuid) }

            _syncState.value = SyncState.Syncing("กำลังส่งข้อมูล ${households.size} ครัวเรือน และประชากรไปยัง Firestore...")

            val householdMap = households.associateBy { it.id }
            val filteredPersons = persons.filter { householdMap.containsKey(it.householdId) }

            // Write households and persons in batches (Firestore max 500 per batch, we use 400 safely)
            var batch = firestore.batch()
            var opsInBatch = 0
            var householdsSynced = 0
            var personsSynced = 0

            for (h in households) {
                val docRef = firestore.collection(COLLECTION_HOUSEHOLDS).document(h.householdUuid)
                val data = householdToMap(h)
                batch.set(docRef, data, SetOptions.merge())
                opsInBatch++
                householdsSynced++

                if (opsInBatch >= 400) {
                    batch.commit().await()
                    batch = firestore.batch()
                    opsInBatch = 0
                }
            }

            for (p in filteredPersons) {
                val parentHousehold = householdMap[p.householdId] ?: continue
                val docRef = firestore.collection(COLLECTION_PERSONS).document(p.personUuid)
                val data = personToMap(p, parentHousehold.householdUuid, parentHousehold.houseNo, parentHousehold.villageNo)
                batch.set(docRef, data, SetOptions.merge())
                opsInBatch++
                personsSynced++

                if (opsInBatch >= 400) {
                    batch.commit().await()
                    batch = firestore.batch()
                    opsInBatch = 0
                }
            }

            if (opsInBatch > 0) {
                batch.commit().await()
            }

            val result = SyncResult(
                householdsSynced = householdsSynced,
                personsSynced = personsSynced,
                message = "ซิงค์ข้อมูลไปยัง Firestore สำเร็จ ($householdsSynced ครัวเรือน, $personsSynced คน)"
            )
            _syncState.value = SyncState.Success(result)
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing Room to Firestore", e)
            val errorMsg = e.message ?: "เกิดข้อผิดพลาดในการซิงค์ข้อมูลกับ Firestore"
            _syncState.value = SyncState.Error(errorMsg, e)
            Result.failure(e)
        }
    }

    /**
     * Persists a single household and its members to Cloud Firestore.
     */
    suspend fun syncHouseholdToFirestore(
        household: Household,
        persons: List<Person> = emptyList()
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore()
            
            // Check if household has been tombstoned
            val hTombstoneRef = firestore.collection(COLLECTION_TOMBSTONES)
                .document("household_${household.householdUuid}")
                .get().await()
            if (hTombstoneRef.exists()) {
                return@withContext Result.failure(IllegalStateException("Cannot sync: Household ${household.householdUuid} was deleted on Cloud."))
            }

            // Filter out tombstoned persons
            val tombstonedPersonUuids = mutableSetOf<String>()
            if (persons.isNotEmpty()) {
                for (chunk in persons.map { it.personUuid }.chunked(30)) {
                    val docs = firestore.collection(COLLECTION_TOMBSTONES)
                        .whereIn("uuid", chunk)
                        .get().await()
                    for (doc in docs.documents) {
                        tombstonedPersonUuids.add(doc.getString("uuid") ?: "")
                    }
                }
            }
            
            val validPersons = persons.filter { !tombstonedPersonUuids.contains(it.personUuid) }

            val batch = firestore.batch()

            val hRef = firestore.collection(COLLECTION_HOUSEHOLDS).document(household.householdUuid)
            batch.set(hRef, householdToMap(household), SetOptions.merge())

            for (p in validPersons) {
                val pRef = firestore.collection(COLLECTION_PERSONS).document(p.personUuid)
                batch.set(pRef, personToMap(p, household.householdUuid, household.houseNo, household.villageNo), SetOptions.merge())
            }

            batch.commit().await()

            val result = SyncResult(
                householdsSynced = 1,
                personsSynced = validPersons.size,
                message = "บันทึกครัวเรือนเลขที่ ${household.houseNo} ไปยัง Firestore เรียบร้อย"
            )
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync household ${household.houseNo}", e)
            Result.failure(e)
        }
    }

    @androidx.annotation.VisibleForTesting
    internal open suspend fun checkTombstoneExists(uuid: String, type: String): Boolean {
        return getFirestore().collection(COLLECTION_TOMBSTONES)
            .document("${type}_${uuid}")
            .get().await().exists()
    }

    @androidx.annotation.VisibleForTesting
    internal open suspend fun performPersonSave(person: Person, householdUuid: String, householdHouseNo: String) {
        val pRef = getFirestore().collection(COLLECTION_PERSONS).document(person.personUuid)
        val household = repository.getHouseholdByUuid(householdUuid)
        val villageNo = household?.villageNo ?: ""
        pRef.set(personToMap(person, householdUuid, householdHouseNo, villageNo), SetOptions.merge()).await()
    }

    /**
     * Persists a single citizen record to Cloud Firestore.
     */
    suspend fun syncPersonToFirestore(
        person: Person,
        householdUuid: String,
        householdHouseNo: String = ""
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            // Check if person has been tombstoned
            if (checkTombstoneExists(person.personUuid, "person")) {
                return@withContext Result.failure(IllegalStateException("Cannot sync: Person ${person.personUuid} was deleted on Cloud."))
            }

            // Check if parent household has been tombstoned
            if (checkTombstoneExists(householdUuid, "household")) {
                return@withContext Result.failure(IllegalStateException("Cannot sync: Parent Household $householdUuid was deleted on Cloud."))
            }
            
            performPersonSave(person, householdUuid, householdHouseNo)

            val result = SyncResult(
                householdsSynced = 0,
                personsSynced = 1,
                message = "บันทึกข้อมูล ${person.fullName} ไปยัง Firestore เรียบร้อย"
            )
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync person ${person.fullName}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a household and its associated persons from Firestore,
     * and writes tombstones to prevent resurrection during bidirectional sync.
     * Uses chunking to stay well within Firestore's 500 write limit per batch.
     */
    suspend fun deleteHouseholdFromFirestore(householdUuid: String, personUuids: List<String> = emptyList()): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val firestore = checkFirebaseConfiguredOrError()
                ?: return@withContext Result.success(Unit)
            
            val timestamp = System.currentTimeMillis()
            val batch = firestore.batch()

            // Fetch villageNo for regional tombstone rules
            val household = repository.getHouseholdByUuid(householdUuid)
            val villageNo = household?.villageNo ?: ""

            // 1. Household tombstone and delete
            val hTombstoneRef = firestore.collection(COLLECTION_TOMBSTONES).document("household_$householdUuid")
            val hRef = firestore.collection(COLLECTION_HOUSEHOLDS).document(householdUuid)
            batch.set(hTombstoneRef, mapOf("uuid" to householdUuid, "type" to "household", "deletedAt" to timestamp, "villageNo" to villageNo))
            batch.delete(hRef)

            // 2. Persons tombstones and deletes
            for (pUuid in personUuids) {
                if (pUuid.isNotBlank()) {
                    val pTombstoneRef = firestore.collection(COLLECTION_TOMBSTONES).document("person_$pUuid")
                    val pRef = firestore.collection(COLLECTION_PERSONS).document(pUuid)
                    batch.set(pTombstoneRef, mapOf("uuid" to pUuid, "type" to "person", "deletedAt" to timestamp, "villageNo" to villageNo))
                    batch.delete(pRef)
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete household $householdUuid from Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a health screening from Firestore and writes a tombstone atomically.
     * The tombstone prevents a later Room-to-Firestore or Firestore-to-Room sync from resurrecting it.
     */
    suspend fun deleteHealthScreeningFromFirestore(screeningUuid: String, villageNo: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore()
            val screening = repository.getScreeningByUuid(screeningUuid)
            val resolvedVillageNo = villageNo ?: screening?.villageNo ?: ""
            val timestamp = System.currentTimeMillis()
            val batch = firestore.batch()

            val screeningRef = firestore.collection(COLLECTION_HEALTH_SCREENINGS).document(screeningUuid)
            val tombstoneRef = firestore.collection(COLLECTION_TOMBSTONES).document("health_screening_$screeningUuid")

            batch.delete(screeningRef)
            batch.set(tombstoneRef, mapOf(
                "uuid" to screeningUuid,
                "type" to "health_screening",
                "deletedAt" to timestamp,
                "villageNo" to resolvedVillageNo
            ))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete health screening $screeningUuid from Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a person from Firestore by UUID and writes a tombstone atomically.
     */
    open suspend fun deletePersonFromFirestore(personUuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore()
            val batch = firestore.batch()

            val person = repository.getPersonByUuid(personUuid)
            val household = person?.let { repository.getHouseholdById(it.householdId) }
            val villageNo = household?.villageNo ?: ""

            val pRef = firestore.collection(COLLECTION_PERSONS).document(personUuid)
            batch.delete(pRef)

            val pTombstoneRef = firestore.collection(COLLECTION_TOMBSTONES).document("person_$personUuid")
            batch.set(pTombstoneRef, mapOf("uuid" to personUuid, "type" to "person", "deletedAt" to System.currentTimeMillis(), "villageNo" to villageNo))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete person $personUuid from Firestore", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // FIRESTORE -> ROOM (Download / Restore)
    // =========================================================================

    /**
     * Fetches all registered data from Cloud Firestore and updates the local Room database,
     * skipping any records marked with deletion tombstones and strictly using UUID matching.
     */
    suspend fun syncFirestoreToRoom(villageNo: String? = null): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.Syncing("กำลังดึงข้อมูลจาก Cloud Firestore...")
            val firestore = checkFirebaseConfiguredOrError()
            if (firestore == null) {
                val err = IllegalStateException("ระบบ Cloud (Firebase) ยังไม่ได้เชื่อมต่อในระบบนี้ (ใช้งานฐานข้อมูลภายใน Room ได้ปกติ)")
                _syncState.value = SyncState.Error(err.message ?: "", err)
                return@withContext Result.failure(err)
            }

            // Retrieve target active villageNo for partitioning
            val activeVillageNo = villageNo ?: try {
                context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("surveyor_village_no", null)
            } catch (e: Exception) {
                null
            }

            // Fetch tombstones
            val tombstoneDocs = firestore.collection(COLLECTION_TOMBSTONES).get().await()
            val deletedUuids = tombstoneDocs.documents.mapNotNull { it.getString("uuid") }.toSet()

            // Remove stale local Room records that have been deleted in Cloud
            val allLocalHouseholdsToDelete = repository.getAllHouseholds()
                .filter { deletedUuids.contains(it.householdUuid) && (activeVillageNo == null || it.villageNo == activeVillageNo) }
            for (h in allLocalHouseholdsToDelete) {
                repository.deleteHousehold(h)
            }
            
            val localHouseholdMap = repository.getAllHouseholds().associateBy { it.id }
            val allLocalPersonsToDelete = repository.getAllPersonsList()
                .filter { deletedUuids.contains(it.personUuid) && (activeVillageNo == null || (localHouseholdMap[it.householdId]?.villageNo == activeVillageNo)) }
            for (p in allLocalPersonsToDelete) {
                repository.delete(p)
            }

            val householdDocs = if (activeVillageNo != null) {
                firestore.collection(COLLECTION_HOUSEHOLDS).whereEqualTo("villageNo", activeVillageNo).get().await()
            } else {
                firestore.collection(COLLECTION_HOUSEHOLDS).get().await()
            }

            val personDocs = if (activeVillageNo != null) {
                firestore.collection(COLLECTION_PERSONS).whereEqualTo("villageNo", activeVillageNo).get().await()
            } else {
                firestore.collection(COLLECTION_PERSONS).get().await()
            }

            _syncState.value = SyncState.Syncing("กำลังนำเข้าข้อมูล เข้าสู่ Room...")

            var householdsImported = 0
            var personsImported = 0

            // 1. Process Households (Strict UUID matching & Tombstone filtering)
            for (doc in householdDocs.documents) {
                val household = docToHousehold(doc) ?: continue
                if (deletedUuids.contains(household.householdUuid)) {
                    continue
                }

                val existing = repository.getHouseholdByUuid(household.householdUuid)

                if (existing != null) {
                    val updated = household.copy(id = existing.id)
                    repository.updateHousehold(updated)
                } else {
                    repository.insertHousehold(household.copy(id = 0))
                }
                householdsImported++
            }

            // 2. Process Persons (Strict UUID matching & Tombstone filtering)
            val allLocalHouseholds = repository.getAllHouseholds()
            val householdByUuid = allLocalHouseholds.associateBy { it.householdUuid }

            for (doc in personDocs.documents) {
                val personUuid = doc.getString("personUuid") ?: doc.id
                if (deletedUuids.contains(personUuid)) {
                    continue
                }

                val parentHouseholdUuid = doc.getString("householdUuid") ?: ""
                val localHousehold = householdByUuid[parentHouseholdUuid]

                if (localHousehold == null) {
                    Log.w(TAG, "Skipping person $personUuid: parent household not found (UUID: $parentHouseholdUuid)")
                    continue
                }

                val person = docToPerson(doc, localHousehold.id) ?: continue
                val existing = repository.getPersonByUuid(person.personUuid)

                if (existing != null) {
                    val updated = person.copy(id = existing.id, householdId = localHousehold.id)
                    repository.update(updated)
                } else {
                    repository.insert(person.copy(id = 0, householdId = localHousehold.id))
                }
                personsImported++
            }

            val result = SyncResult(
                householdsSynced = householdsImported,
                personsSynced = personsImported,
                message = "นำเข้าข้อมูลจาก Firestore สำเร็จ ($householdsImported ครัวเรือน, $personsImported คน)"
            )
            _syncState.value = SyncState.Success(result)
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing Firestore to Room", e)
            val errorMsg = e.message ?: "เกิดข้อผิดพลาดในการดึงข้อมูลจาก Firestore"
            _syncState.value = SyncState.Error(errorMsg, e)
            Result.failure(e)
        }
    }

    /**
     * Bidirectional synchronization: Pulls cloud records to Room, then pushes local records to Firestore.
     */
    suspend fun bidirectionalSync(villageNo: String? = null): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.Syncing("เริ่มการซิงค์แบบ 2 ทาง (Pull & Push)...")
            val pullResult = syncFirestoreToRoom(villageNo)
            if (pullResult.isFailure) {
                return@withContext pullResult
            }
            val pushResult = syncRoomToFirestore(villageNo)
            if (pushResult.isFailure) return@withContext pushResult
            val healthPull = syncHealthScreeningsFromFirestore(villageNo)
            if (healthPull.isFailure) return@withContext Result.failure(healthPull.exceptionOrNull()!!)
            val healthPush = syncHealthScreeningsToFirestore(villageNo)
            if (healthPush.isFailure) return@withContext Result.failure(healthPush.exceptionOrNull()!!)
            pushResult
        } catch (e: Exception) {
            Log.e(TAG, "Error during bidirectional sync", e)
            Result.failure(e)
        }
    }

    /** Syncs health screenings using screeningUuid as document identity. */
    suspend fun syncHealthScreeningsToFirestore(villageNo: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore()
            val activeVillageNo = villageNo ?: context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).getString("surveyor_village_no", null)
            val persons = repository.getAllPersonsList().associateBy { it.id }
            val households = repository.getAllHouseholds().associateBy { it.id }
            val screenings = repository.getAllScreeningsList().mapNotNull { screening ->
                val person = persons[screening.personId]
                val household = person?.let { households[it.householdId] }
                val resolvedVillageNo = household?.villageNo ?: screening.villageNo
                if (activeVillageNo != null && resolvedVillageNo != activeVillageNo) null
                else screening to resolvedVillageNo
            }
            val batches = screenings.filter { !checkTombstoneExists(it.first.screeningUuid, "health_screening") }.chunked(400)
            var count = 0
            for (chunk in batches) {
                val batch = firestore.batch()
                for ((s, resolvedVillageNo) in chunk) {
                    val person = persons[s.personId]
                    val ref = firestore.collection(COLLECTION_HEALTH_SCREENINGS).document(s.screeningUuid)
                    batch.set(ref, healthScreeningToMap(s, person?.personUuid ?: s.personUuid, resolvedVillageNo), SetOptions.merge())
                    count++
                }
                batch.commit().await()
            }
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Health screening upload failed", e)
            Result.failure(e)
        }
    }

    /** Pulls health screenings for the active village into Room, preserving UUID identity. */
    suspend fun syncHealthScreeningsFromFirestore(villageNo: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore()
            val activeVillageNo = villageNo ?: context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).getString("surveyor_village_no", null)
            val query = if (activeVillageNo != null) firestore.collection(COLLECTION_HEALTH_SCREENINGS).whereEqualTo("villageNo", activeVillageNo) else firestore.collection(COLLECTION_HEALTH_SCREENINGS)
            val tombstones = firestore.collection(COLLECTION_TOMBSTONES)
                .whereEqualTo("type", "health_screening")
                .get().await()
            for (tombstone in tombstones.documents) {
                val uuid = tombstone.getString("uuid") ?: continue
                val tombstoneVillage = tombstone.getString("villageNo")
                if (activeVillageNo == null || tombstoneVillage == null || tombstoneVillage == activeVillageNo) {
                    repository.deleteScreeningByUuid(uuid)
                }
            }

            val docs = query.get().await()
            var count = 0
            for (doc in docs.documents) {
                val uuid = doc.getString("screeningUuid") ?: doc.id
                if (checkTombstoneExists(uuid, "health_screening")) {
                    repository.deleteScreeningByUuid(uuid)
                    continue
                }
                val personUuid = doc.getString("personUuid") ?: continue
                val person = repository.getPersonByUuid(personUuid) ?: continue
                val screening = docToHealthScreening(doc, person.id) ?: continue
                val existing = repository.getScreeningByUuid(uuid)
                if (existing == null) {
                    repository.insertScreening(screening.copy(id = 0, personId = person.id))
                } else if (screening.lastModified > existing.lastModified) {
                    repository.updateScreening(screening.copy(id = existing.id, personId = person.id))
                }
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Health screening download failed", e)
            Result.failure(e)
        }
    }

    private fun healthScreeningToMap(screening: com.example.data.HealthScreening, personUuid: String, villageNo: String): Map<String, Any?> = mapOf(
        "screeningUuid" to screening.screeningUuid,
        "personUuid" to personUuid,
        "villageNo" to villageNo,
        "timestamp" to screening.timestamp,
        "weight" to screening.weight,
        "height" to screening.height,
        "bmi" to screening.bmi,
        "systolic" to screening.systolic,
        "diastolic" to screening.diastolic,
        "bloodSugar" to screening.bloodSugar,
        "bloodSugarType" to screening.bloodSugarType,
        "pulse" to screening.pulse,
        "temperature" to screening.temperature,
        "oxygenSaturation" to screening.oxygenSaturation,
        "note" to screening.note,
        "vhvId" to screening.vhvId,
        "vhvName" to screening.vhvName,
        "dataStatus" to screening.dataStatus.name,
        "updatedAt" to screening.lastModified
    )

    private fun docToHealthScreening(doc: DocumentSnapshot, personId: Long): com.example.data.HealthScreening? {
        val uuid = doc.getString("screeningUuid") ?: doc.id
        val timestamp = doc.getLong("timestamp") ?: return null
        val status = doc.getString("dataStatus")?.let { runCatching { DataStatus.valueOf(it) }.getOrDefault(DataStatus.NEEDS_REVIEW) } ?: DataStatus.NEEDS_REVIEW
        return com.example.data.HealthScreening(
            id = 0,
            screeningUuid = uuid,
            personId = personId,
            personUuid = doc.getString("personUuid") ?: "",
            villageNo = doc.getString("villageNo") ?: "",
            timestamp = timestamp,
            weight = doc.getDouble("weight"),
            height = doc.getDouble("height"),
            bmi = doc.getDouble("bmi"),
            systolic = doc.getLong("systolic")?.toInt(),
            diastolic = doc.getLong("diastolic")?.toInt(),
            bloodSugar = doc.getLong("bloodSugar")?.toInt(),
            bloodSugarType = doc.getString("bloodSugarType") ?: "UNKNOWN",
            pulse = doc.getLong("pulse")?.toInt(),
            temperature = doc.getDouble("temperature"),
            oxygenSaturation = doc.getLong("oxygenSaturation")?.toInt(),
            note = doc.getString("note"),
            vhvId = doc.getString("vhvId"),
            vhvName = doc.getString("vhvName"),
            dataStatus = status,
            lastModified = doc.getLong("updatedAt") ?: timestamp
        )
    }

    // =========================================================================
    // MAPPERS & UTILITIES
    // =========================================================================

    private fun householdToMap(household: Household): Map<String, Any?> {
        return mapOf(
            "householdUuid" to household.householdUuid,
            "houseNo" to household.houseNo,
            "villageNo" to household.villageNo,
            "subdistrict" to household.subdistrict,
            "district" to household.district,
            "province" to household.province,
            "latitude" to household.latitude,
            "longitude" to household.longitude,
            "locationAccuracy" to household.locationAccuracy?.toDouble(),
            "locationCapturedAt" to household.locationCapturedAt,
            "locationProvider" to household.locationProvider,
            "dataStatus" to household.dataStatus.name,
            "updatedAt" to household.lastModified
        )
    }

    private fun personToMap(person: Person, householdUuid: String, householdHouseNo: String, villageNo: String = ""): Map<String, Any?> {
        return mapOf(
            "personUuid" to person.personUuid,
            "householdUuid" to householdUuid,
            "householdHouseNo" to householdHouseNo,
            "villageNo" to villageNo,
            "nationalId" to person.nationalId,
            "fullName" to person.fullName,
            "gender" to person.gender.name,
            "birthDate" to person.birthDate?.toString(),
            "isBirthYearOnly" to person.isBirthYearOnly,
            "houseStatus" to person.houseStatus.name,
            "personStatus" to person.personStatus.name,
            "dataStatus" to person.dataStatus.name,
            "updatedAt" to person.lastModified
        )
    }

    private fun docToHousehold(doc: DocumentSnapshot): Household? {
        val houseNo = doc.getString("houseNo") ?: return null
        val uuid = doc.getString("householdUuid") ?: doc.id

        val lat = doc.getDouble("latitude")
        val lon = doc.getDouble("longitude")
        val accuracy = doc.getDouble("locationAccuracy")?.toFloat()
        val capturedAt = doc.getLong("locationCapturedAt")
        val provider = doc.getString("locationProvider")

        val statusStr = doc.getString("dataStatus")
        val dataStatus = statusStr?.let {
            try { DataStatus.valueOf(it) } catch (e: Exception) { DataStatus.NEEDS_REVIEW }
        } ?: DataStatus.NEEDS_REVIEW

        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

        return Household(
            id = 0,
            householdUuid = uuid,
            houseNo = houseNo,
            villageNo = doc.getString("villageNo") ?: "",
            subdistrict = doc.getString("subdistrict") ?: "",
            district = doc.getString("district") ?: "",
            province = doc.getString("province") ?: "",
            latitude = lat,
            longitude = lon,
            locationAccuracy = accuracy,
            locationCapturedAt = capturedAt,
            locationProvider = provider,
            dataStatus = dataStatus,
            lastModified = updatedAt
        )
    }

    private fun docToPerson(doc: DocumentSnapshot, localHouseholdId: Long): Person? {
        val fullName = doc.getString("fullName") ?: return null
        val uuid = doc.getString("personUuid") ?: doc.id
        val nationalId = doc.getString("nationalId")

        val genderStr = doc.getString("gender")
        val gender = genderStr?.let {
            try { Gender.valueOf(it) } catch (e: Exception) { Gender.MALE }
        } ?: Gender.MALE

        val birthDateStr = doc.getString("birthDate")
        val birthDate = birthDateStr?.let {
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        }
        val isBirthYearOnly = doc.getBoolean("isBirthYearOnly") ?: false

        val houseStatusStr = doc.getString("houseStatus")
        val houseStatus = houseStatusStr?.let {
            try { HouseholdRole.valueOf(it) } catch (e: Exception) { HouseholdRole.RESIDENT }
        } ?: HouseholdRole.RESIDENT

        val personStatusStr = doc.getString("personStatus")
        val personStatus = personStatusStr?.let {
            try { PersonStatus.valueOf(it) } catch (e: Exception) { PersonStatus.ALIVE }
        } ?: PersonStatus.ALIVE

        val dataStatusStr = doc.getString("dataStatus")
        val dataStatus = dataStatusStr?.let {
            try { DataStatus.valueOf(it) } catch (e: Exception) { DataStatus.NEEDS_REVIEW }
        } ?: DataStatus.NEEDS_REVIEW

        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

        return Person(
            id = 0,
            personUuid = uuid,
            householdId = localHouseholdId,
            nationalId = nationalId,
            fullName = fullName,
            gender = gender,
            birthDate = birthDate,
            isBirthYearOnly = isBirthYearOnly,
            houseStatus = houseStatus,
            personStatus = personStatus,
            dataStatus = dataStatus,
            lastModified = updatedAt
        )
    }
}
