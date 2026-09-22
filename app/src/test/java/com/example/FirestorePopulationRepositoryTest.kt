package com.example

import com.example.data.DataStatus
import com.example.data.Gender
import com.example.data.Household
import com.example.data.HouseholdRole
import com.example.data.Person
import com.example.data.PersonStatus
import com.example.data.firestore.FirestoreManager
import com.example.data.firestore.FirestorePopulationRepository
import com.example.data.firestore.PopulationRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirestorePopulationRepositoryTest {

    private lateinit var repository: FirestorePopulationRepository

    @Before
    fun setUp() {
        FirestoreManager.resetForTesting(null)
        repository = FirestorePopulationRepository(firestoreProvider = { null })
    }

    @After
    fun tearDown() {
        FirestoreManager.resetForTesting(null)
    }

    @Test
    fun `isAvailable returns false when Firestore is not initialized`() {
        assertFalse(repository.isAvailable())
    }

    @Test
    fun `saveHousehold fails when householdUuid is blank`() = runBlocking {
        val invalidHousehold = Household(
            householdUuid = "",
            houseNo = "99/1",
            villageNo = "1"
        )
        val result = repository.saveHousehold(invalidHousehold)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("householdUuid must not be blank", result.exceptionOrNull()?.message)
    }

    @Test
    fun `saveHousehold fails when houseNo is blank`() = runBlocking {
        val invalidHousehold = Household(
            householdUuid = "h-uuid-12345",
            houseNo = "   ",
            villageNo = "1"
        )
        val result = repository.saveHousehold(invalidHousehold)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("houseNo must not be blank", result.exceptionOrNull()?.message)
    }

    @Test
    fun `savePerson fails when personUuid or householdUuid is blank`() = runBlocking {
        val invalidPerson1 = Person(
            personUuid = "",
            householdId = 1L,
            fullName = "สมชาย"
        )
        val result1 = repository.savePerson(invalidPerson1, householdUuid = "h-uuid-1")
        assertTrue(result1.isFailure)
        assertEquals("personUuid must not be blank", result1.exceptionOrNull()?.message)

        val invalidPerson2 = Person(
            personUuid = "p-uuid-2",
            householdId = 1L,
            fullName = "สมหญิง"
        )
        val result2 = repository.savePerson(invalidPerson2, householdUuid = "")
        assertTrue(result2.isFailure)
        assertEquals("householdUuid must not be blank", result2.exceptionOrNull()?.message)
    }

    @Test
    fun `householdToMap respects Identity rules and includes audit metadata`() {
        val household = Household(
            householdUuid = "H-UUID-777",
            houseNo = "123/45",
            villageNo = "3",
            subdistrict = "ท่าช้าง",
            district = "เมือง",
            province = "นครนายก",
            latitude = 14.1234,
            longitude = 101.5678,
            locationAccuracy = 5.5f,
            locationCapturedAt = 1710000000000L,
            locationProvider = "GPS",
            dataStatus = DataStatus.VERIFIED,
            lastModified = 1710000500000L
        )

        val map = repository.householdToMap(household, userUid = "firebase-auth-user-999")

        // Household Identity must be householdUuid, NOT houseNo
        assertEquals("H-UUID-777", map["householdUuid"])
        assertEquals("123/45", map["houseNo"])
        // Area Identity must be villageNo
        assertEquals("3", map["villageNo"])
        assertEquals("ท่าช้าง", map["subdistrict"])
        assertEquals("เมือง", map["district"])
        assertEquals("นครนายก", map["province"])
        assertEquals(14.1234, map["latitude"])
        assertEquals(101.5678, map["longitude"])
        assertEquals(5.5, map["locationAccuracy"])
        assertEquals(1710000000000L, map["locationCapturedAt"])
        assertEquals("GPS", map["locationProvider"])
        assertEquals("VERIFIED", map["dataStatus"])
        assertEquals(1710000500000L, map["updatedAt"])
        // User Identity must be preserved in lastUpdatedBy, never overwriting householdUuid
        assertEquals("firebase-auth-user-999", map["lastUpdatedBy"])
        assertEquals(false, map["isDeleted"])
    }

    @Test
    fun `personToMap respects Identity rules and includes audit metadata`() {
        val person = Person(
            personUuid = "P-UUID-888",
            householdId = 10L,
            nationalId = "1234567890123",
            fullName = "นายสมชาย ใจดี",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 5, 20),
            isBirthYearOnly = false,
            houseStatus = HouseholdRole.HEAD,
            personStatus = PersonStatus.ALIVE,
            dataStatus = DataStatus.VERIFIED,
            lastModified = 1710001000000L
        )

        val map = repository.personToMap(
            person = person,
            householdUuid = "H-UUID-777",
            householdHouseNo = "123/45",
            userUid = "firebase-auth-user-999"
        )

        // Person Identity must be personUuid, NOT user.uid
        assertEquals("P-UUID-888", map["personUuid"])
        // Household FK must be householdUuid
        assertEquals("H-UUID-777", map["householdUuid"])
        assertEquals("123/45", map["householdHouseNo"])
        assertEquals("1234567890123", map["nationalId"])
        assertEquals("นายสมชาย ใจดี", map["fullName"])
        assertEquals("MALE", map["gender"])
        assertEquals("1990-05-20", map["birthDate"])
        assertEquals(false, map["isBirthYearOnly"])
        assertEquals("HEAD", map["houseStatus"])
        assertEquals("ALIVE", map["personStatus"])
        assertEquals("VERIFIED", map["dataStatus"])
        assertEquals(1710001000000L, map["updatedAt"])
        // User Identity must be preserved in lastUpdatedBy, never replacing personUuid
        assertEquals("firebase-auth-user-999", map["lastUpdatedBy"])
        assertEquals(false, map["isDeleted"])
    }

    @Test
    fun `mapToHousehold safely parses Firestore data map`() {
        val map = mapOf<String, Any?>(
            "householdUuid" to "H-DOC-1",
            "houseNo" to "55/2",
            "villageNo" to "4",
            "subdistrict" to "เกาะหวาย",
            "district" to "ปากพลี",
            "province" to "นครนายก",
            "latitude" to 14.2,
            "longitude" to 101.3,
            "locationAccuracy" to 3.0,
            "locationCapturedAt" to 1700000000000L,
            "locationProvider" to "Fused",
            "dataStatus" to "VERIFIED",
            "updatedAt" to 1700000500000L
        )

        val household = repository.mapToHousehold(map, docId = "H-DOC-1")
        assertNotNull(household)
        assertEquals("H-DOC-1", household!!.householdUuid)
        assertEquals("55/2", household.houseNo)
        assertEquals("4", household.villageNo)
        assertEquals("เกาะหวาย", household.subdistrict)
        assertEquals("ปากพลี", household.district)
        assertEquals("นครนายก", household.province)
        assertEquals(14.2, household.latitude!!, 0.0001)
        assertEquals(101.3, household.longitude!!, 0.0001)
        assertEquals(3.0f, household.locationAccuracy)
        assertEquals(1700000000000L, household.locationCapturedAt)
        assertEquals("Fused", household.locationProvider)
        assertEquals(DataStatus.VERIFIED, household.dataStatus)
        assertEquals(1700000500000L, household.lastModified)
    }

    @Test
    fun `mapToHousehold returns null when houseNo is missing or blank`() {
        val map = mapOf<String, Any?>(
            "householdUuid" to "H-DOC-1",
            "houseNo" to ""
        )

        val household = repository.mapToHousehold(map, docId = "H-DOC-1")
        assertNull(household)
    }

    @Test
    fun `mapToPerson safely parses Firestore data map`() {
        val map = mapOf<String, Any?>(
            "personUuid" to "P-DOC-1",
            "fullName" to "นางสมศรี สุขเกษม",
            "nationalId" to "9876543210987",
            "gender" to "FEMALE",
            "birthDate" to "1985-12-10",
            "isBirthYearOnly" to false,
            "houseStatus" to "RESIDENT",
            "personStatus" to "ALIVE",
            "dataStatus" to "VERIFIED",
            "updatedAt" to 1710002000000L
        )

        val person = repository.mapToPerson(map, docId = "P-DOC-1", localHouseholdId = 5L)
        assertNotNull(person)
        assertEquals("P-DOC-1", person!!.personUuid)
        assertEquals(5L, person.householdId)
        assertEquals("นางสมศรี สุขเกษม", person.fullName)
        assertEquals("9876543210987", person.nationalId)
        assertEquals(Gender.FEMALE, person.gender)
        assertEquals(LocalDate.of(1985, 12, 10), person.birthDate)
        assertEquals(HouseholdRole.RESIDENT, person.houseStatus)
        assertEquals(PersonStatus.ALIVE, person.personStatus)
        assertEquals(DataStatus.VERIFIED, person.dataStatus)
        assertEquals(1710002000000L, person.lastModified)
    }

    @Test
    fun `mapToPerson handles invalid or corrupted enum strings with safe fallbacks`() {
        val map = mapOf<String, Any?>(
            "personUuid" to "P-CORRUPTED",
            "fullName" to "นายทดสอบ ระบบ",
            "gender" to "UNKNOWN_GENDER_VAL",
            "houseStatus" to "UNKNOWN_ROLE_VAL",
            "personStatus" to "UNKNOWN_STATUS_VAL",
            "dataStatus" to "UNKNOWN_DATA_VAL",
            "birthDate" to "INVALID_DATE_FORMAT"
        )

        val person = repository.mapToPerson(map, docId = "P-CORRUPTED", localHouseholdId = 0L)
        assertNotNull(person)
        assertEquals(Gender.MALE, person!!.gender) // Fallback to MALE
        assertEquals(HouseholdRole.RESIDENT, person.houseStatus) // Fallback to RESIDENT
        assertEquals(PersonStatus.ALIVE, person.personStatus) // Fallback to ALIVE
        assertEquals(DataStatus.NEEDS_REVIEW, person.dataStatus) // Fallback to NEEDS_REVIEW
        assertNull(person.birthDate) // Fallback to null
    }

    @Test
    fun `PopulationRecord converts to HouseholdWithPersons correctly`() {
        val household = Household(
            id = 1L,
            householdUuid = "H-COMPOSITE-1",
            houseNo = "77/7"
        )
        val person1 = Person(
            id = 101L,
            personUuid = "P-101",
            householdId = 1L,
            fullName = "สมาชิกคนที่ 1"
        )
        val person2 = Person(
            id = 102L,
            personUuid = "P-102",
            householdId = 1L,
            fullName = "สมาชิกคนที่ 2"
        )

        val record = PopulationRecord(
            household = household,
            persons = listOf(person1, person2)
        )

        val householdWithPersons = record.toHouseholdWithPersons()
        assertEquals("H-COMPOSITE-1", householdWithPersons.household.householdUuid)
        assertEquals(2, householdWithPersons.persons.size)
        assertEquals("สมาชิกคนที่ 1", householdWithPersons.persons[0].fullName)
        assertEquals("สมาชิกคนที่ 2", householdWithPersons.persons[1].fullName)
    }
}
