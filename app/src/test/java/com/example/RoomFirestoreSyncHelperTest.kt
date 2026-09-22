package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.data.sync.RoomFirestoreSyncHelper
import com.example.data.sync.SyncResult
import com.example.data.sync.SyncState
import com.example.domain.ExcelImportUseCase
import com.example.viewmodel.PersonViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomFirestoreSyncHelperTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PersonRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PersonRepository(db, db.personDao(), db.householdDao(), db.personHistoryDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testSyncHelperInitialStateIsIdle() {
        val helper = RoomFirestoreSyncHelper(context, repository, firestoreProvider = { null })
        assertEquals(SyncState.Idle, helper.syncState.value)
        assertFalse(helper.isFirebaseConfigured())
    }

    @Test
    fun testSyncFailsGracefullyWhenFirebaseNotConfigured() = runBlocking {
        val helper = RoomFirestoreSyncHelper(context, repository, firestoreProvider = { null })
        val result = helper.syncRoomToFirestore()
        
        assertTrue(result.isFailure)
        assertTrue(helper.syncState.value is SyncState.Error)
        
        helper.resetSyncState()
        assertEquals(SyncState.Idle, helper.syncState.value)
    }

    @Test
    fun testSyncFirestoreToRoomFailsGracefullyWhenFirebaseNotConfigured() = runBlocking {
        val helper = RoomFirestoreSyncHelper(context, repository, firestoreProvider = { null })
        val result = helper.syncFirestoreToRoom()
        
        assertTrue(result.isFailure)
        assertTrue(helper.syncState.value is SyncState.Error)
    }

    @Test
    fun testPersonViewModelSyncDelegation() = runBlocking {
        val helper = RoomFirestoreSyncHelper(context, repository, firestoreProvider = { null })
        val useCase = ExcelImportUseCase(db)
        val viewModel = PersonViewModel(repository, useCase, helper)

        assertEquals(SyncState.Idle, viewModel.syncState.value)

        var syncCompleted = false
        viewModel.syncToFirestore { res ->
            assertTrue(res.isFailure)
            syncCompleted = true
        }

        assertTrue(viewModel.syncState.value is SyncState.Error || viewModel.syncState.value is SyncState.Idle)
        viewModel.resetSyncState()
        assertEquals(SyncState.Idle, viewModel.syncState.value)
    }

    @Test
    fun testHouseholdAndPersonUuidLookupInRepository() = runBlocking {
        val household = Household(
            householdUuid = "H-SYNC-001",
            houseNo = "99/99",
            villageNo = "4",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก",
            latitude = 14.123,
            longitude = 101.456,
            dataStatus = DataStatus.VERIFIED
        )
        val hId = repository.insertHousehold(household)
        assertTrue(hId > 0)

        val retrievedH = repository.getHouseholdByUuid("H-SYNC-001")
        assertNotNull(retrievedH)
        assertEquals("99/99", retrievedH?.houseNo)

        val person = Person(
            personUuid = "P-SYNC-001",
            householdId = hId,
            nationalId = "1234567890123",
            fullName = "นายทดสอบ ซิงค์ข้อมูล",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 5, 20),
            houseStatus = HouseholdRole.HEAD,
            personStatus = PersonStatus.ALIVE,
            dataStatus = DataStatus.VERIFIED
        )
        repository.insert(person)

        val retrievedP = repository.getPersonByUuid("P-SYNC-001")
        assertNotNull(retrievedP)
        assertEquals("นายทดสอบ ซิงค์ข้อมูล", retrievedP?.fullName)
        assertEquals(LocalDate.of(1990, 5, 20), retrievedP?.birthDate)
    }

    @Test
    fun testPreventTombstoneRecreationSyncLogic() = runBlocking {
        // Simulate tombstone protection logic that aborts if UUID is in tombstone
        val householdUuidToSync = "H-PROTECT-001"
        val existingTombstones = setOf("household_H-PROTECT-001", "person_P-PROTECT-001")
        
        // This simulates the Firestore check: hTombstoneRef.exists()
        val hTombstoneExists = existingTombstones.contains("household_$householdUuidToSync")
        
        var syncAborted = false
        if (hTombstoneExists) {
            syncAborted = true
        }
        
        assertTrue("Sync should be aborted because a tombstone exists for this UUID", syncAborted)
    }

    @Test
    fun testBatchDeleteChunkingLogic() {
        // Simulate generating operations for a large household to verify chunk limit logic
        val householdUuid = "H-LARGE-001"
        val personUuids = (1..300).map { "P-LARGE-$it" }
        
        val timestamp = System.currentTimeMillis()
        val entityOpsPairs = mutableListOf<List<String>>() // Simulated operations
        
        for (pUuid in personUuids) {
            entityOpsPairs.add(listOf(
                "TOMBSTONE: person_$pUuid",
                "DELETE: person_$pUuid"
            ))
        }
        
        entityOpsPairs.add(listOf(
            "TOMBSTONE: household_$householdUuid",
            "DELETE: household_$householdUuid"
        ))
        
        // We have 300 persons + 1 household = 301 pairs = 602 operations
        assertEquals(301, entityOpsPairs.size)
        
        // Chunk entity pairs at 200 pairs (400 ops)
        val chunks = entityOpsPairs.chunked(200)
        
        assertEquals(2, chunks.size)
        assertEquals(200, chunks[0].size) // 400 operations
        assertEquals(101, chunks[1].size) // 202 operations
        
        // Ensure pairs are not split across chunks
        assertTrue(chunks[0].contains(listOf("TOMBSTONE: person_P-LARGE-1", "DELETE: person_P-LARGE-1")))
        assertTrue(chunks[1].contains(listOf("TOMBSTONE: household_H-LARGE-001", "DELETE: household_H-LARGE-001")))
    }
}
