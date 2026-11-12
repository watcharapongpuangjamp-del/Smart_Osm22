package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DataStatus
import com.example.data.Gender
import com.example.data.Household
import com.example.data.HouseholdRole
import com.example.data.Person
import com.example.data.PersonHistory
import com.example.data.PersonStatus
import com.example.data.PopulationEvent
import com.example.data.PopulationEventType
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
class PopulationRegistrationRoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testHouseholdDao_insertAndQueryOperations() = runBlocking {
        val householdDao = db.householdDao()

        val h1 = Household(
            householdUuid = "h-uuid-001",
            houseNo = "12/3",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก",
            latitude = 14.2155,
            longitude = 101.0723,
            dataStatus = DataStatus.VERIFIED
        )
        val h1Id = householdDao.insert(h1)
        assertTrue(h1Id > 0)

        // Query by ID, houseNo, UUID
        val byId = householdDao.getHouseholdById(h1Id)
        assertNotNull(byId)
        assertEquals("h-uuid-001", byId?.householdUuid)
        assertEquals("12/3", byId?.houseNo)

        val byUuid = householdDao.getHouseholdByUuid("h-uuid-001")
        assertNotNull(byUuid)
        assertEquals(h1Id, byUuid?.id)

        val byNo = householdDao.getHouseholdByNo("12/3")
        assertNotNull(byNo)
        assertEquals("h-uuid-001", byNo?.householdUuid)

        // Exists check
        assertTrue(householdDao.existsByHouseholdUuid("h-uuid-001"))
        assertFalse(householdDao.existsByHouseholdUuid("h-non-existing"))

        // Village query Flow
        val villageHouseholds = householdDao.getHouseholdsByVillageNo("8").first()
        assertEquals(1, villageHouseholds.size)
        assertEquals("12/3", villageHouseholds[0].houseNo)

        // Search query Flow
        val searchResult = householdDao.searchHouseholds("12").first()
        assertEquals(1, searchResult.size)
        assertEquals("h-uuid-001", searchResult[0].householdUuid)

        val noMatch = householdDao.searchHouseholds("9999").first()
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun testHouseholdDao_batchInsertAndDelete() = runBlocking {
        val householdDao = db.householdDao()

        val households = listOf(
            Household(householdUuid = "h-batch-1", houseNo = "101", villageNo = "5"),
            Household(householdUuid = "h-batch-2", houseNo = "102", villageNo = "5"),
            Household(householdUuid = "h-batch-3", houseNo = "103", villageNo = "6")
        )
        val insertedIds = householdDao.insertAll(households)
        assertEquals(3, insertedIds.size)

        val count = householdDao.getTotalHouseholdsCount().first()
        assertEquals(3, count)

        // Delete by UUID
        val deletedRows = householdDao.deleteByUuid("h-batch-1")
        assertEquals(1, deletedRows)
        assertFalse(householdDao.existsByHouseholdUuid("h-batch-1"))

        val newCount = householdDao.getTotalHouseholdsCount().first()
        assertEquals(2, newCount)
    }

    @Test
    fun testPersonDao_crudAndSearchOperations() = runBlocking {
        val householdDao = db.householdDao()
        val personDao = db.personDao()

        val hId = householdDao.insert(
            Household(householdUuid = "h-uuid-persontest", houseNo = "55", villageNo = "8")
        )

        val p1 = Person(
            personUuid = "p-uuid-001",
            householdId = hId,
            nationalId = "1234567890123",
            fullName = "นางสาวปราณี มีสุข",
            gender = Gender.FEMALE,
            birthDate = LocalDate.of(1985, 3, 15),
            houseStatus = HouseholdRole.HEAD,
            personStatus = PersonStatus.ALIVE,
            dataStatus = DataStatus.VERIFIED
        )
        val p1Id = personDao.insertPerson(p1)
        assertTrue(p1Id > 0)

        // Exists check
        assertTrue(personDao.existsByPersonUuid("p-uuid-001"))
        assertTrue(personDao.existsByNationalId("1234567890123"))
        assertFalse(personDao.existsByPersonUuid("p-non-existent"))
        assertFalse(personDao.existsByNationalId("0000000000000"))

        // Query by ID, UUID, NationalId
        val byId = personDao.getPersonById(p1Id)
        assertEquals("นางสาวปราณี มีสุข", byId?.fullName)

        val byUuid = personDao.getPersonByUuid("p-uuid-001")
        assertEquals(p1Id, byUuid?.id)

        val byNatId = personDao.getPersonByNationalId("1234567890123")
        assertEquals("p-uuid-001", byNatId?.personUuid)

        // Search by name or national ID
        val searchByName = personDao.searchPersons("ปราณี").first()
        assertEquals(1, searchByName.size)

        val searchByNatId = personDao.searchPersons("123456").first()
        assertEquals(1, searchByNatId.size)

        // Search by village
        val byVillage = personDao.getPersonsByVillage("8").first()
        assertEquals(1, byVillage.size)
        assertEquals("นางสาวปราณี มีสุข", byVillage[0].fullName)

        // Count by status
        val aliveCount = personDao.getPersonsCountByStatus(PersonStatus.ALIVE).first()
        assertEquals(1, aliveCount)
        val deadCount = personDao.getPersonsCountByStatus(PersonStatus.DEAD).first()
        assertEquals(0, deadCount)

        // Update
        val updatedPerson = p1.copy(id = p1Id, fullName = "นางปราณี มีสุขดี")
        personDao.updatePerson(updatedPerson)
        val afterUpdate = personDao.getPersonById(p1Id)
        assertEquals("นางปราณี มีสุขดี", afterUpdate?.fullName)

        // Delete by UUID
        val deletedCount = personDao.deleteByUuid("p-uuid-001")
        assertEquals(1, deletedCount)
        assertFalse(personDao.existsByPersonUuid("p-uuid-001"))
    }

    @Test
    fun testCascadeDelete_deletingHouseholdDeletesPersons() = runBlocking {
        val householdDao = db.householdDao()
        val personDao = db.personDao()

        val hId = householdDao.insert(
            Household(householdUuid = "h-cascade-test", houseNo = "77", villageNo = "1")
        )
        personDao.insertPerson(
            Person(
                personUuid = "p-child-1",
                householdId = hId,
                fullName = "สมาชิกคนที่ 1"
            )
        )
        personDao.insertPerson(
            Person(
                personUuid = "p-child-2",
                householdId = hId,
                fullName = "สมาชิกคนที่ 2"
            )
        )

        assertEquals(2, personDao.getPersonsByHouseholdIdList(hId).size)

        // Delete parent household
        householdDao.deleteById(hId)

        // SQLite ForeignKey ON DELETE CASCADE removes all child persons
        val remainingPersons = personDao.getPersonsByHouseholdIdList(hId)
        assertTrue(remainingPersons.isEmpty())
        assertFalse(personDao.existsByPersonUuid("p-child-1"))
        assertFalse(personDao.existsByPersonUuid("p-child-2"))
    }

    @Test
    fun testPopulationEvents_recordTracking() = runBlocking {
        val householdDao = db.householdDao()
        val personDao = db.personDao()
        val eventDao = db.populationEventDao()

        val hId = householdDao.insert(Household(householdUuid = "h-event-test", houseNo = "88"))
        val pId = personDao.insertPerson(Person(personUuid = "p-event-test", householdId = hId, fullName = "ทารกแรกเกิด"))

        val event = PopulationEvent(
            type = PopulationEventType.BIRTH,
            title = "แจ้งเกิด",
            description = "เกิดเมื่อเวลา 09:00 น.",
            latitude = 14.2155,
            longitude = 101.0723,
            householdId = hId,
            personId = pId,
            personName = "ทารกแรกเกิด"
        )
        val eventId = eventDao.insertEvent(event)
        assertTrue(eventId > 0)

        val eventsForHousehold = eventDao.getEventsByHouseholdId(hId).first()
        assertEquals(1, eventsForHousehold.size)
        assertEquals(PopulationEventType.BIRTH, eventsForHousehold[0].type)
    }

    @Test
    fun testPersonHistory_auditLogging() = runBlocking {
        val historyDao = db.personHistoryDao()

        val history = PersonHistory(
            personId = 100L,
            action = "CREATE",
            oldValue = null,
            newValue = "{\"fullName\": \"นายทดสอบ\"}",
            operatorId = "vhv-officer-uid",
            operatorName = "อสม. ผู้ปฏิบัติงาน",
            role = "VHV",
            source = "ROOM_LOCAL"
        )
        historyDao.insert(history)

        val allHistory = historyDao.getAllHistory().first()
        assertTrue(allHistory.isNotEmpty())
        assertEquals("CREATE", allHistory[0].action)
        assertEquals("vhv-officer-uid", allHistory[0].operatorId)
    }
}
