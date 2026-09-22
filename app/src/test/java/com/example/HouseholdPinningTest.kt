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
class HouseholdPinningTest {

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
    fun testPinHouseholdPreservesIdentityAndUpdatesLocation() = runBlocking {
        val originalUuid = "HH-PIN-TEST-001"
        val originalLastModified = 1000L
        val household = Household(
            householdUuid = originalUuid,
            houseNo = "99/1",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก",
            lastModified = originalLastModified,
            latitude = null,
            longitude = null
        )
        val householdId = repository.insertHousehold(household)
        val inserted = repository.getHouseholdById(householdId)
        assertNotNull(inserted)
        assertNull(inserted!!.latitude)
        assertNull(inserted.longitude)
        assertEquals(originalUuid, inserted.householdUuid)

        // Perform pinning
        val pinLat = 14.21550
        val pinLon = 101.07230
        val beforeUpdate = System.currentTimeMillis()
        val updatedHousehold = inserted.copy(
            latitude = pinLat,
            longitude = pinLon,
            locationProvider = "MANUAL_PIN",
            locationCapturedAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        repository.updateHousehold(updatedHousehold)

        // Verify update in DB
        val retrieved = repository.getHouseholdById(householdId)
        assertNotNull(retrieved)
        assertEquals(pinLat, retrieved!!.latitude!!, 0.00001)
        assertEquals(pinLon, retrieved.longitude!!, 0.00001)
        assertEquals("MANUAL_PIN", retrieved.locationProvider)
        assertNotNull(retrieved.locationCapturedAt)
        // Verify UUID was NEVER regenerated or changed
        assertEquals(originalUuid, retrieved.householdUuid)
        // Verify lastModified updated
        assertTrue(retrieved.lastModified >= beforeUpdate)
    }

    @Test
    fun testRemoveHouseholdLocationPreservesAllOtherData() = runBlocking {
        val originalUuid = "HH-PIN-TEST-002"
        val household = Household(
            householdUuid = originalUuid,
            houseNo = "102/5",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก",
            latitude = 14.2100,
            longitude = 101.0700,
            locationProvider = "MANUAL_PIN",
            locationCapturedAt = 5000L,
            lastModified = 5000L
        )
        val householdId = repository.insertHousehold(household)
        val inserted = repository.getHouseholdById(householdId)
        assertNotNull(inserted?.latitude)

        // Remove location
        val beforeClear = System.currentTimeMillis()
        val cleared = inserted!!.copy(
            latitude = null,
            longitude = null,
            locationAccuracy = null,
            locationCapturedAt = null,
            locationProvider = null,
            lastModified = System.currentTimeMillis()
        )
        repository.updateHousehold(cleared)

        val retrieved = repository.getHouseholdById(householdId)
        assertNotNull(retrieved)
        assertNull(retrieved!!.latitude)
        assertNull(retrieved.longitude)
        assertNull(retrieved.locationProvider)
        assertNull(retrieved.locationCapturedAt)
        // Critical: household identity and address preserved
        assertEquals(originalUuid, retrieved.householdUuid)
        assertEquals("102/5", retrieved.houseNo)
        assertTrue(retrieved.lastModified >= beforeClear)
    }

    @Test
    fun testHouseSummaryForPopulationDistribution() = runBlocking {
        // Create 2 households: 1 pinned, 1 unpinned
        val h1Id = repository.insertHousehold(Household(
            householdUuid = "H-POP-001",
            houseNo = "50/1",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก",
            latitude = 14.2155,
            longitude = 101.0723
        ))
        val h2Id = repository.insertHousehold(Household(
            householdUuid = "H-POP-002",
            houseNo = "50/2",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก",
            latitude = null,
            longitude = null
        ))

        // Add members to h1: 1 elderly, 1 adult female, 1 child
        val birthElderly = LocalDate.now().minusYears(65)
        val birthChild = LocalDate.now().minusYears(8)
        val birthAdult = LocalDate.now().minusYears(35)

        repository.insert(Person(
            personUuid = "P-POP-001",
            householdId = h1Id,
            nationalId = "1111111111111",
            fullName = "นายสมชาย สูงวัย",
            gender = Gender.MALE,
            birthDate = birthElderly,
            houseStatus = HouseholdRole.HEAD
        ))
        repository.insert(Person(
            personUuid = "P-POP-002",
            householdId = h1Id,
            nationalId = "2222222222222",
            fullName = "นางสมใจ สูงวัย",
            gender = Gender.FEMALE,
            birthDate = birthAdult,
            houseStatus = HouseholdRole.RESIDENT
        ))
        repository.insert(Person(
            personUuid = "P-POP-003",
            householdId = h1Id,
            nationalId = "3333333333333",
            fullName = "ด.ช.สมปอง สูงวัย",
            gender = Gender.MALE,
            birthDate = birthChild,
            houseStatus = HouseholdRole.RESIDENT
        ))

        val summaryList = repository.houseSummary.first()
        val h1Summary = summaryList.find { it.householdId == h1Id }
        val h2Summary = summaryList.find { it.householdId == h2Id }

        assertNotNull(h1Summary)
        assertNotNull(h2Summary)

        // Verify h1 demographics
        assertEquals(3, h1Summary!!.totalMembers)
        assertEquals(2, h1Summary.males)
        assertEquals(1, h1Summary.females)
        assertEquals(1, h1Summary.elderly)
        assertEquals(1, h1Summary.children)
        assertNotNull(h1Summary.latitude)
        assertNotNull(h1Summary.longitude)

        // Verify h2 is unpinned
        assertEquals(0, h2Summary!!.totalMembers)
        assertNull(h2Summary.latitude)
        assertNull(h2Summary.longitude)
    }
}
