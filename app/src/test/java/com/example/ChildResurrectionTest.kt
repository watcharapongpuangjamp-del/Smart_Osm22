package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.data.sync.RoomFirestoreSyncHelper
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
class ChildResurrectionTest {

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
    fun testSyncPersonRejectsIfHouseholdTombstoneExists() = runBlocking {
        var saveCalled = false

        val syncHelper = object : RoomFirestoreSyncHelper(context, repository, { null }) {
            override suspend fun checkTombstoneExists(uuid: String, type: String): Boolean {
                if (type == "person" && uuid == "P-001") return false
                if (type == "household" && uuid == "H-001") return true
                return false
            }

            override suspend fun performPersonSave(person: Person, householdUuid: String, householdHouseNo: String) {
                saveCalled = true
            }
        }
        
        val person = Person(
            personUuid = "P-001",
            householdId = 1L,
            fullName = "นาย Orphan",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 1, 1),
            houseStatus = HouseholdRole.RESIDENT,
            personStatus = PersonStatus.ALIVE
        )

        val result = syncHelper.syncPersonToFirestore(person, "H-001", "123")

        assertTrue("Sync should fail when parent household is tombstoned", result.isFailure)
        assertFalse("performPersonSave should not be called", saveCalled)
        
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(
            "Exception should mention Parent Household",
            exception?.message?.contains("Parent Household H-001 was deleted") == true
        )
    }

    @Test
    fun testSyncPersonRejectsIfPersonTombstoneExists() = runBlocking {
        var saveCalled = false

        val syncHelper = object : RoomFirestoreSyncHelper(context, repository, { null }) {
            override suspend fun checkTombstoneExists(uuid: String, type: String): Boolean {
                if (type == "person" && uuid == "P-002") return true
                if (type == "household" && uuid == "H-001") return false
                return false
            }

            override suspend fun performPersonSave(person: Person, householdUuid: String, householdHouseNo: String) {
                saveCalled = true
            }
        }
        
        val person = Person(
            personUuid = "P-002",
            householdId = 1L,
            fullName = "นาย Dead",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 1, 1),
            houseStatus = HouseholdRole.RESIDENT,
            personStatus = PersonStatus.ALIVE
        )

        val result = syncHelper.syncPersonToFirestore(person, "H-001", "123")

        assertTrue("Sync should fail when person is tombstoned", result.isFailure)
        assertFalse("performPersonSave should not be called", saveCalled)
        
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(
            "Exception should mention Person",
            exception?.message?.contains("Person P-002 was deleted") == true
        )
    }
    
    @Test
    fun testSyncPersonSucceedsIfNoTombstoneExists() = runBlocking {
        var saveCalled = false

        val syncHelper = object : RoomFirestoreSyncHelper(context, repository, { null }) {
            override suspend fun checkTombstoneExists(uuid: String, type: String): Boolean {
                return false // No tombstones exist
            }

            override suspend fun performPersonSave(person: Person, householdUuid: String, householdHouseNo: String) {
                saveCalled = true
            }
        }
        
        val person = Person(
            personUuid = "P-003",
            householdId = 1L,
            fullName = "นาย Alive",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 1, 1),
            houseStatus = HouseholdRole.RESIDENT,
            personStatus = PersonStatus.ALIVE
        )

        val result = syncHelper.syncPersonToFirestore(person, "H-001", "123")

        assertTrue("Sync should succeed", result.isSuccess)
        assertTrue("performPersonSave should be called", saveCalled)
    }
}
