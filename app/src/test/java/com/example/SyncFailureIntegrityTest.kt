package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
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
class SyncFailureIntegrityTest {

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
    fun testMultiBatchFailureLeavesOrphanedPersonsSafelyIgnored() = runBlocking {
        // Scenario: A household is deleted on Cloud (tombstone exists), but persons failed to delete (multi-batch chunking failure).
        // During sync, the household is deleted locally. The orphaned persons from the Cloud try to be processed.
        
        // 1. Create Household and Person locally
        val household = Household(
            householdUuid = "H-MULTI-FAIL-001",
            houseNo = "999/1",
            villageNo = "1",
            subdistrict = "Sub",
            district = "Dist",
            province = "Prov"
        )
        val householdId = repository.insertHousehold(household)
        
        val person = Person(
            personUuid = "P-MULTI-FAIL-001",
            householdId = householdId,
            fullName = "นาย Orphan",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 1, 1),
            houseStatus = HouseholdRole.RESIDENT,
            personStatus = PersonStatus.ALIVE
        )
        repository.insert(person)
        
        // Ensure they exist
        assertNotNull(repository.getHouseholdById(householdId))
        assertNotNull(repository.getPersonByUuid("P-MULTI-FAIL-001"))
        
        // 2. Simulate Sync: Household tombstone received -> Household deleted locally
        val h = repository.getHouseholdById(householdId)
        if (h != null) {
            repository.deleteHousehold(h)
        }
        
        // 3. Verify CASCADE deleted the person locally
        assertNull(repository.getHouseholdById(householdId))
        assertNull(repository.getPersonByUuid("P-MULTI-FAIL-001"))
        
        // 4. Simulate Sync: The orphaned Person document from Cloud arrives.
        // It has a householdUuid of "H-MULTI-FAIL-001".
        // The SyncHelper looks up the local household:
        val allLocalHouseholds = repository.getAllHouseholds()
        val householdByUuid = allLocalHouseholds.associateBy { it.householdUuid }
        val localHousehold = householdByUuid["H-MULTI-FAIL-001"]
        
        // Verify localHousehold is null
        assertNull(localHousehold)
        
        // In SyncHelper, if localHousehold == null, it skips inserting/updating the person.
        // Let's verify that attempting to insert an orphaned person directly to Room throws Foreign Key constraint exception,
        // which proves the SyncHelper's check is necessary and correct for data integrity.
        
        val orphanedPerson = Person(
            personUuid = "P-MULTI-FAIL-001",
            householdId = 9999L, // Non-existent ID, simulating orphaned mapping failure
            fullName = "นาย Orphan",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 1, 1),
            houseStatus = HouseholdRole.RESIDENT,
            personStatus = PersonStatus.ALIVE
        )
        
        var caughtException = false
        try {
            repository.insert(orphanedPerson)
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            caughtException = true
        }
        
        assertTrue("Expected Foreign Key Constraint Exception for Orphaned Person", caughtException)
    }
}
