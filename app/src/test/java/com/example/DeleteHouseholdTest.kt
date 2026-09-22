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
class DeleteHouseholdTest {

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
    fun testDeleteHouseholdWithoutMembers() = runBlocking {
        val household = Household(
            householdUuid = "H-DEL-001",
            houseNo = "100/1",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก"
        )
        val householdId = repository.insertHousehold(household)
        val insertedHousehold = repository.getHouseholdById(householdId)
        assertNotNull(insertedHousehold)

        // Delete household
        val result = repository.deleteHousehold(insertedHousehold!!)
        assertTrue(result.isSuccess)

        // Verify household is deleted
        val deletedHousehold = repository.getHouseholdById(householdId)
        assertNull(deletedHousehold)
    }

    @Test
    fun testDeleteHouseholdWithMembersCascade() = runBlocking {
        val household = Household(
            householdUuid = "H-DEL-002",
            houseNo = "100/2",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก"
        )
        val householdId = repository.insertHousehold(household)
        val insertedHousehold = repository.getHouseholdById(householdId)
        assertNotNull(insertedHousehold)

        // Insert persons linked to this household
        val person1 = Person(
            personUuid = "P-DEL-001",
            householdId = householdId,
            fullName = "นายทดสอบ หนึ่ง",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1980, 1, 1),
            houseStatus = HouseholdRole.HEAD,
            personStatus = PersonStatus.ALIVE
        )
        val person2 = Person(
            personUuid = "P-DEL-002",
            householdId = householdId,
            fullName = "นางทดสอบ สอง",
            gender = Gender.FEMALE,
            birthDate = LocalDate.of(1985, 5, 5),
            houseStatus = HouseholdRole.RESIDENT,
            personStatus = PersonStatus.ALIVE
        )

        repository.insert(person1)
        repository.insert(person2)

        // Verify persons exist
        assertNotNull(repository.getPersonByUuid("P-DEL-001"))
        assertNotNull(repository.getPersonByUuid("P-DEL-002"))

        // Delete household
        val result = repository.deleteHousehold(insertedHousehold!!)
        assertTrue(result.isSuccess)

        // Verify household is deleted
        assertNull(repository.getHouseholdById(householdId))

        // Verify persons are deleted via ON DELETE CASCADE
        assertNull(repository.getPersonByUuid("P-DEL-001"))
        assertNull(repository.getPersonByUuid("P-DEL-002"))
    }
    
    @Test
    fun testDeletePersonPreservesHistory() = runBlocking {
        val household = Household(
            householdUuid = "H-DEL-003",
            houseNo = "100/3",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก"
        )
        val householdId = repository.insertHousehold(household)

        val person = Person(
            personUuid = "P-DEL-003",
            householdId = householdId,
            fullName = "นายทดสอบ สาม",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 1, 1),
            houseStatus = HouseholdRole.RESIDENT,
            personStatus = PersonStatus.ALIVE
        )
        repository.insert(person)

        val insertedPerson = repository.getPersonByUuid("P-DEL-003")
        assertNotNull(insertedPerson)

        // Delete person
        repository.delete(insertedPerson!!)

        // Verify person is deleted locally
        assertNull(repository.getPersonByUuid("P-DEL-003"))

        // Verify PersonHistory is created for DELETE action
        val historyList = repository.getHistoryForPerson(insertedPerson.id).first()
        assertTrue(historyList.any { it.action == "DELETE" })
    }

    @Test
    fun testCloudToRoomDeletionRemovesLocalRecord() = runBlocking {
        val household = Household(
            householdUuid = "H-TOMBSTONE-001",
            houseNo = "101/1",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก"
        )
        val householdId = repository.insertHousehold(household)

        // Simulate Cloud -> Room tombstone deletion logic
        val deletedUuids = setOf("H-TOMBSTONE-001")
        val allLocalHouseholdsToDelete = repository.getAllHouseholds().filter { deletedUuids.contains(it.householdUuid) }
        for (h in allLocalHouseholdsToDelete) {
            repository.deleteHousehold(h)
        }

        assertNull(repository.getHouseholdByUuid("H-TOMBSTONE-001"))
    }
}
