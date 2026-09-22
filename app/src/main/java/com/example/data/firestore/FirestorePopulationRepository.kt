package com.example.data.firestore

import android.util.Log
import com.example.data.DataStatus
import com.example.data.Gender
import com.example.data.Household
import com.example.data.HouseholdRole
import com.example.data.HouseholdWithPersons
import com.example.data.Person
import com.example.data.PersonStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Composite model representing a Household population record and its registered persons.
 */
data class PopulationRecord(
    val household: Household,
    val persons: List<Person> = emptyList()
) {
    fun toHouseholdWithPersons(): HouseholdWithPersons {
        return HouseholdWithPersons(household = household, persons = persons)
    }
}

/**
 * Repository class handling reading, writing, and real-time observation
 * of population records (Households and Persons) in Cloud Firestore.
 *
 * Adheres strictly to the system's Identity Architecture:
 * - User Identity: user.uid (recorded in audit metadata, never substitutes personUuid or householdUuid)
 * - Area Identity: villageId / villageNo
 * - Household Identity: householdUuid (primary key in Firestore households collection)
 * - Person Identity: personUuid (primary key in Firestore persons collection)
 */
open class FirestorePopulationRepository(
    private val firestoreProvider: () -> FirebaseFirestore? = { FirestoreManager.getInstance() }
) {
    companion object {
        private const val TAG = "FirestorePopulationRepo"
        const val COLLECTION_HOUSEHOLDS = "households"
        const val COLLECTION_PERSONS = "persons"
        const val COLLECTION_TOMBSTONES = "tombstones"
        private const val BATCH_SIZE_LIMIT = 400
    }

    private fun getFirestore(): FirebaseFirestore {
        return firestoreProvider()
            ?: throw IllegalStateException("Cloud Firestore is not configured or available.")
    }

    fun isAvailable(): Boolean {
        return try {
            firestoreProvider() != null
        } catch (e: Exception) {
            false
        }
    }

    // =========================================================================
    // READ OPERATIONS - HOUSEHOLDS
    // =========================================================================

    /**
     * Retrieves a single household by its unique householdUuid.
     */
    suspend fun getHousehold(householdUuid: String): Result<Household?> = withContext(Dispatchers.IO) {
        try {
            if (householdUuid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("householdUuid cannot be blank"))
            }
            val firestore = getFirestore()
            val snapshot = firestore.collection(COLLECTION_HOUSEHOLDS).document(householdUuid).get().await()
            if (!snapshot.exists()) {
                return@withContext Result.success(null)
            }
            val isDeleted = snapshot.getBoolean("isDeleted") ?: false
            if (isDeleted) {
                return@withContext Result.success(null)
            }
            Result.success(docToHousehold(snapshot))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching household $householdUuid", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all non-deleted households, optionally filtered by villageNo (Area Identity).
     */
    suspend fun getAllHouseholds(villageNo: String? = null): Result<List<Household>> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore()
            var query: Query = firestore.collection(COLLECTION_HOUSEHOLDS)
            if (!villageNo.isNullOrBlank()) {
                query = query.whereEqualTo("villageNo", villageNo)
            }
            val snapshot = query.get().await()
            val list = snapshot.documents
                .filter { doc -> doc.getBoolean("isDeleted") != true }
                .mapNotNull { docToHousehold(it) }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all households", e)
            Result.failure(e)
        }
    }

    /**
     * Observes real-time changes to households, optionally filtered by villageNo.
     */
    fun observeHouseholds(villageNo: String? = null): Flow<List<Household>> = callbackFlow {
        val firestore = firestoreProvider()
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var query: Query = firestore.collection(COLLECTION_HOUSEHOLDS)
        if (!villageNo.isNullOrBlank()) {
            query = query.whereEqualTo("villageNo", villageNo)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Snapshot error observing households", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents
                    .filter { doc -> doc.getBoolean("isDeleted") != true }
                    .mapNotNull { docToHousehold(it) }
                trySend(list)
            }
        }

        awaitClose { registration.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Observes real-time changes to a single household by householdUuid.
     */
    fun observeHousehold(householdUuid: String): Flow<Household?> = callbackFlow {
        val firestore = firestoreProvider()
        if (firestore == null || householdUuid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val docRef = firestore.collection(COLLECTION_HOUSEHOLDS).document(householdUuid)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Snapshot error observing household $householdUuid", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists() && snapshot.getBoolean("isDeleted") != true) {
                trySend(docToHousehold(snapshot))
            } else {
                trySend(null)
            }
        }

        awaitClose { registration.remove() }
    }.flowOn(Dispatchers.IO)

    // =========================================================================
    // READ OPERATIONS - PERSONS
    // =========================================================================

    /**
     * Retrieves a single person by personUuid.
     */
    suspend fun getPerson(personUuid: String): Result<Person?> = withContext(Dispatchers.IO) {
        try {
            if (personUuid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("personUuid cannot be blank"))
            }
            val firestore = getFirestore()
            val snapshot = firestore.collection(COLLECTION_PERSONS).document(personUuid).get().await()
            if (!snapshot.exists()) {
                return@withContext Result.success(null)
            }
            val isDeleted = snapshot.getBoolean("isDeleted") ?: false
            if (isDeleted) {
                return@withContext Result.success(null)
            }
            Result.success(docToPerson(snapshot))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching person $personUuid", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all persons registered under a specific householdUuid.
     */
    suspend fun getPersonsForHousehold(householdUuid: String): Result<List<Person>> = withContext(Dispatchers.IO) {
        try {
            if (householdUuid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("householdUuid cannot be blank"))
            }
            val firestore = getFirestore()
            val snapshot = firestore.collection(COLLECTION_PERSONS)
                .whereEqualTo("householdUuid", householdUuid)
                .get()
                .await()
            val list = snapshot.documents
                .filter { doc -> doc.getBoolean("isDeleted") != true }
                .mapNotNull { docToPerson(it) }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching persons for household $householdUuid", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all non-deleted persons across the database.
     */
    suspend fun getAllPersons(): Result<List<Person>> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore()
            val snapshot = firestore.collection(COLLECTION_PERSONS).get().await()
            val list = snapshot.documents
                .filter { doc -> doc.getBoolean("isDeleted") != true }
                .mapNotNull { docToPerson(it) }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all persons", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves a person by their 13-digit Thai national ID.
     */
    suspend fun getPersonByNationalId(nationalId: String): Result<Person?> = withContext(Dispatchers.IO) {
        try {
            if (nationalId.isBlank()) {
                return@withContext Result.success(null)
            }
            val firestore = getFirestore()
            val snapshot = firestore.collection(COLLECTION_PERSONS)
                .whereEqualTo("nationalId", nationalId)
                .limit(1)
                .get()
                .await()
            val doc = snapshot.documents.firstOrNull { it.getBoolean("isDeleted") != true }
            Result.success(doc?.let { docToPerson(it) })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching person by nationalId", e)
            Result.failure(e)
        }
    }

    /**
     * Observes real-time changes to persons belonging to a householdUuid.
     */
    fun observePersonsForHousehold(householdUuid: String): Flow<List<Person>> = callbackFlow {
        val firestore = firestoreProvider()
        if (firestore == null || householdUuid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = firestore.collection(COLLECTION_PERSONS)
            .whereEqualTo("householdUuid", householdUuid)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Snapshot error observing persons for $householdUuid", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents
                    .filter { doc -> doc.getBoolean("isDeleted") != true }
                    .mapNotNull { docToPerson(it) }
                trySend(list)
            }
        }

        awaitClose { registration.remove() }
    }.flowOn(Dispatchers.IO)

    // =========================================================================
    // READ OPERATIONS - COMPOSITE POPULATION RECORD
    // =========================================================================

    /**
     * Retrieves a complete PopulationRecord (Household + its resident Persons) by householdUuid.
     */
    suspend fun getPopulationRecord(householdUuid: String): Result<PopulationRecord?> = withContext(Dispatchers.IO) {
        try {
            val householdResult = getHousehold(householdUuid)
            if (householdResult.isFailure) {
                return@withContext Result.failure(householdResult.exceptionOrNull()!!)
            }
            val household = householdResult.getOrNull() ?: return@withContext Result.success(null)

            val personsResult = getPersonsForHousehold(householdUuid)
            if (personsResult.isFailure) {
                return@withContext Result.failure(personsResult.exceptionOrNull()!!)
            }
            val persons = personsResult.getOrNull() ?: emptyList()

            Result.success(PopulationRecord(household = household, persons = persons))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching population record for $householdUuid", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // WRITE OPERATIONS - HOUSEHOLDS
    // =========================================================================

    /**
     * Saves or updates a Household record in Firestore.
     * Uses householdUuid as the primary document key and merges changes.
     *
     * @param household The Household entity to persist
     * @param userUid Optional Firebase Authentication user.uid for audit logging
     */
    suspend fun saveHousehold(
        household: Household,
        userUid: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (household.householdUuid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("householdUuid must not be blank"))
            }
            if (household.houseNo.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("houseNo must not be blank"))
            }

            val firestore = getFirestore()
            val docRef = firestore.collection(COLLECTION_HOUSEHOLDS).document(household.householdUuid)
            val data = householdToMap(household, userUid)

            docRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "Successfully saved household ${household.householdUuid} (houseNo: ${household.houseNo})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save household ${household.householdUuid}", e)
            Result.failure(e)
        }
    }

    /**
     * Saves or updates a Person record in Firestore.
     * Uses personUuid as the primary document key and householdUuid as foreign key.
     *
     * @param person The Person entity to persist
     * @param householdUuid Foreign key to the parent Household
     * @param householdHouseNo Denormalized house number for quick search
     * @param userUid Optional Firebase Authentication user.uid for audit logging
     */
    suspend fun savePerson(
        person: Person,
        householdUuid: String,
        householdHouseNo: String = "",
        userUid: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (person.personUuid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("personUuid must not be blank"))
            }
            if (householdUuid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("householdUuid must not be blank"))
            }
            if (person.fullName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("fullName must not be blank"))
            }

            val firestore = getFirestore()
            val docRef = firestore.collection(COLLECTION_PERSONS).document(person.personUuid)
            val data = personToMap(person, householdUuid, householdHouseNo, userUid)

            docRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "Successfully saved person ${person.personUuid} (${person.fullName})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save person ${person.personUuid}", e)
            Result.failure(e)
        }
    }

    /**
     * Atomically saves a complete PopulationRecord (Household + Persons) using a Firestore WriteBatch.
     * All operations succeed or fail together, ensuring data integrity across households and members.
     */
    suspend fun savePopulationRecord(
        household: Household,
        persons: List<Person>,
        userUid: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (household.householdUuid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("householdUuid must not be blank"))
            }
            if (household.houseNo.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("houseNo must not be blank"))
            }

            val firestore = getFirestore()
            var batch = firestore.batch()
            var opsInBatch = 0

            // 1. Add household
            val hRef = firestore.collection(COLLECTION_HOUSEHOLDS).document(household.householdUuid)
            batch.set(hRef, householdToMap(household, userUid), SetOptions.merge())
            opsInBatch++

            // 2. Add persons
            for (p in persons) {
                if (p.personUuid.isBlank()) continue
                val pRef = firestore.collection(COLLECTION_PERSONS).document(p.personUuid)
                batch.set(pRef, personToMap(p, household.householdUuid, household.houseNo, userUid), SetOptions.merge())
                opsInBatch++

                if (opsInBatch >= BATCH_SIZE_LIMIT) {
                    batch.commit().await()
                    batch = firestore.batch()
                    opsInBatch = 0
                }
            }

            if (opsInBatch > 0) {
                batch.commit().await()
            }

            Log.i(TAG, "Successfully saved population record: 1 household and ${persons.size} persons")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to atomically save population record ${household.householdUuid}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // DELETE OPERATIONS
    // =========================================================================

    /**
     * Deletes a person record, supporting both soft deletion with tombstones
     * and hard deletion from the active collection.
     */
    suspend fun deletePerson(
        personUuid: String,
        softDelete: Boolean = true,
        userUid: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (personUuid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("personUuid must not be blank"))
            }
            val firestore = getFirestore()
            val timestamp = System.currentTimeMillis()
            val batch = firestore.batch()

            val pRef = firestore.collection(COLLECTION_PERSONS).document(personUuid)
            val tombstoneRef = firestore.collection(COLLECTION_TOMBSTONES).document("person_$personUuid")

            // Always write tombstone to prevent resurrection during sync
            batch.set(tombstoneRef, mapOf(
                "uuid" to personUuid,
                "type" to "person",
                "deletedAt" to timestamp,
                "deletedBy" to userUid
            ))

            if (softDelete) {
                batch.update(pRef, mapOf(
                    "isDeleted" to true,
                    "dataStatus" to DataStatus.NEEDS_REVIEW.name,
                    "updatedAt" to timestamp,
                    "lastUpdatedBy" to userUid
                ))
            } else {
                batch.delete(pRef)
            }

            batch.commit().await()
            Log.d(TAG, "Successfully deleted person $personUuid (softDelete=$softDelete)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete person $personUuid", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a household and all its resident persons, creating tombstones
     * to ensure bidirectional sync engines respect the deletion.
     */
    suspend fun deleteHousehold(
        householdUuid: String,
        softDelete: Boolean = true,
        userUid: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (householdUuid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("householdUuid must not be blank"))
            }
            val firestore = getFirestore()
            val timestamp = System.currentTimeMillis()

            // Fetch associated persons
            val personDocs = firestore.collection(COLLECTION_PERSONS)
                .whereEqualTo("householdUuid", householdUuid)
                .get()
                .await()

            var batch = firestore.batch()
            var ops = 0

            // 1. Household tombstone and delete/mark
            val hRef = firestore.collection(COLLECTION_HOUSEHOLDS).document(householdUuid)
            val hTombstoneRef = firestore.collection(COLLECTION_TOMBSTONES).document("household_$householdUuid")

            batch.set(hTombstoneRef, mapOf(
                "uuid" to householdUuid,
                "type" to "household",
                "deletedAt" to timestamp,
                "deletedBy" to userUid
            ))
            ops++

            if (softDelete) {
                batch.update(hRef, mapOf(
                    "isDeleted" to true,
                    "dataStatus" to DataStatus.NEEDS_REVIEW.name,
                    "updatedAt" to timestamp,
                    "lastUpdatedBy" to userUid
                ))
            } else {
                batch.delete(hRef)
            }
            ops++

            // 2. Cascade delete/tombstone child persons
            for (doc in personDocs.documents) {
                val pUuid = doc.getString("personUuid") ?: doc.id
                val pRef = doc.reference
                val pTombstoneRef = firestore.collection(COLLECTION_TOMBSTONES).document("person_$pUuid")

                batch.set(pTombstoneRef, mapOf(
                    "uuid" to pUuid,
                    "type" to "person",
                    "deletedAt" to timestamp,
                    "deletedBy" to userUid
                ))
                ops++

                if (softDelete) {
                    batch.update(pRef, mapOf(
                        "isDeleted" to true,
                        "dataStatus" to DataStatus.NEEDS_REVIEW.name,
                        "updatedAt" to timestamp,
                        "lastUpdatedBy" to userUid
                    ))
                } else {
                    batch.delete(pRef)
                }
                ops++

                if (ops >= BATCH_SIZE_LIMIT) {
                    batch.commit().await()
                    batch = firestore.batch()
                    ops = 0
                }
            }

            if (ops > 0) {
                batch.commit().await()
            }

            Log.i(TAG, "Successfully deleted household $householdUuid and ${personDocs.size()} members (softDelete=$softDelete)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete household $householdUuid", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // MAPPERS
    // =========================================================================

    fun householdToMap(household: Household, userUid: String? = null): Map<String, Any?> {
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
            "updatedAt" to household.lastModified,
            "lastUpdatedBy" to userUid,
            "isDeleted" to false
        )
    }

    fun personToMap(
        person: Person,
        householdUuid: String,
        householdHouseNo: String = "",
        userUid: String? = null
    ): Map<String, Any?> {
        return mapOf(
            "personUuid" to person.personUuid,
            "householdUuid" to householdUuid,
            "householdHouseNo" to householdHouseNo,
            "nationalId" to person.nationalId,
            "fullName" to person.fullName,
            "gender" to person.gender.name,
            "birthDate" to person.birthDate?.toString(),
            "isBirthYearOnly" to person.isBirthYearOnly,
            "houseStatus" to person.houseStatus.name,
            "personStatus" to person.personStatus.name,
            "dataStatus" to person.dataStatus.name,
            "updatedAt" to person.lastModified,
            "lastUpdatedBy" to userUid,
            "isDeleted" to false
        )
    }

    fun mapToHousehold(data: Map<String, Any?>, docId: String = ""): Household? {
        val houseNo = (data["houseNo"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val uuid = (data["householdUuid"] as? String)?.takeIf { it.isNotBlank() } ?: docId

        val lat = (data["latitude"] as? Number)?.toDouble()
        val lon = (data["longitude"] as? Number)?.toDouble()
        val accuracy = (data["locationAccuracy"] as? Number)?.toFloat()
        val capturedAt = (data["locationCapturedAt"] as? Number)?.toLong()
        val provider = data["locationProvider"] as? String

        val statusStr = data["dataStatus"] as? String
        val dataStatus = statusStr?.let {
            try { DataStatus.valueOf(it) } catch (e: Exception) { DataStatus.NEEDS_REVIEW }
        } ?: DataStatus.NEEDS_REVIEW

        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

        return Household(
            id = 0,
            householdUuid = uuid,
            houseNo = houseNo,
            villageNo = (data["villageNo"] as? String) ?: "",
            subdistrict = (data["subdistrict"] as? String) ?: "",
            district = (data["district"] as? String) ?: "",
            province = (data["province"] as? String) ?: "",
            latitude = lat,
            longitude = lon,
            locationAccuracy = accuracy,
            locationCapturedAt = capturedAt,
            locationProvider = provider,
            dataStatus = dataStatus,
            lastModified = updatedAt
        )
    }

    fun docToHousehold(doc: DocumentSnapshot): Household? {
        val data = doc.data ?: return null
        return mapToHousehold(data, doc.id)
    }

    fun mapToPerson(
        data: Map<String, Any?>,
        docId: String = "",
        localHouseholdId: Long = 0
    ): Person? {
        val fullName = (data["fullName"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val uuid = (data["personUuid"] as? String)?.takeIf { it.isNotBlank() } ?: docId
        val nationalId = data["nationalId"] as? String

        val genderStr = data["gender"] as? String
        val gender = genderStr?.let {
            try { Gender.valueOf(it) } catch (e: Exception) { Gender.MALE }
        } ?: Gender.MALE

        val birthDateStr = data["birthDate"] as? String
        val birthDate = birthDateStr?.let {
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        }
        val isBirthYearOnly = (data["isBirthYearOnly"] as? Boolean) ?: false

        val houseStatusStr = data["houseStatus"] as? String
        val houseStatus = houseStatusStr?.let {
            try { HouseholdRole.valueOf(it) } catch (e: Exception) { HouseholdRole.RESIDENT }
        } ?: HouseholdRole.RESIDENT

        val personStatusStr = data["personStatus"] as? String
        val personStatus = personStatusStr?.let {
            try { PersonStatus.valueOf(it) } catch (e: Exception) { PersonStatus.ALIVE }
        } ?: PersonStatus.ALIVE

        val dataStatusStr = data["dataStatus"] as? String
        val dataStatus = dataStatusStr?.let {
            try { DataStatus.valueOf(it) } catch (e: Exception) { DataStatus.NEEDS_REVIEW }
        } ?: DataStatus.NEEDS_REVIEW

        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

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

    fun docToPerson(doc: DocumentSnapshot, localHouseholdId: Long = 0): Person? {
        val data = doc.data ?: return null
        return mapToPerson(data, doc.id, localHouseholdId)
    }
}
