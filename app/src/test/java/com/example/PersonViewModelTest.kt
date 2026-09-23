package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.ExcelImportUseCase
import com.example.viewmodel.PersonViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersonViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PersonRepository
    private lateinit var excelImportUseCase: ExcelImportUseCase
    private lateinit var viewModel: PersonViewModel
    private lateinit var context: Context

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PersonRepository(db, db.personDao(), db.householdDao(), db.personHistoryDao())
        excelImportUseCase = ExcelImportUseCase(db)

        // Seed an initial record so PersonViewModel init does not spawn background seed threads on Dispatchers.IO
        runBlocking {
            db.householdDao().insert(
                Household(
                    householdUuid = "H-INIT-SETUP",
                    houseNo = "0",
                    villageNo = "0",
                    subdistrict = "Init",
                    district = "Init",
                    province = "Init"
                )
            )
        }

        viewModel = PersonViewModel(repository, excelImportUseCase, null)
    }

    @After
    fun teardown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testAddNewHouseholdWithHead() = runTest {
        var insertedId: Long = -1

        viewModel.addNewHouseholdWithHead(
            houseNo = "99/9",
            headName = "นายทดสอบ หัวหน้าบ้าน",
            latitude = 14.1234,
            longitude = 101.5678
        ) { id ->
            insertedId = id
        }

        // Real blocking sleep to allow background Dispatchers.IO threads to execute, 
        // then advance main scheduler to deliver callbacks.
        var attempts = 0
        while (insertedId == -1L && attempts < 100) {
            java.lang.Thread.sleep(15)
            testDispatcher.scheduler.advanceUntilIdle()
            attempts++
        }

        assertTrue("Expected a valid household ID to be inserted", insertedId > 0)

        // Verify household is in DB
        val household = repository.getHouseholdById(insertedId)
        assertNotNull(household)
        assertEquals("99/9", household?.houseNo)
        assertEquals(14.1234, household?.latitude ?: 0.0, 0.0001)

        // Verify head of household is added as a person
        val persons = repository.getPersonsByHouseholdIdList(insertedId)
        assertEquals(1, persons.size)
        assertEquals("นายทดสอบ หัวหน้าบ้าน", persons[0].fullName)
        assertEquals(HouseholdRole.HEAD, persons[0].houseStatus)
    }

    @Test
    fun testUpdateHouseholdLocation() = runTest {
        // Insert a base household
        val household = Household(
            householdUuid = "H-TEST-001",
            houseNo = "123",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก"
        )
        val householdId = repository.insertHousehold(household)

        var updateSuccess: Boolean? = null

        viewModel.updateHouseholdLocation(
            householdId = householdId,
            latitude = 14.9999,
            longitude = 101.8888,
            provider = "MANUAL_PIN"
        ) { success, _ ->
            updateSuccess = success
        }

        // Real blocking sleep to allow background Dispatchers.IO threads to execute
        var attempts = 0
        while (updateSuccess == null && attempts < 100) {
            java.lang.Thread.sleep(15)
            testDispatcher.scheduler.advanceUntilIdle()
            attempts++
        }

        assertEquals(true, updateSuccess)

        // Verify location was updated in DB
        val updated = repository.getHouseholdById(householdId)
        assertNotNull(updated)
        assertEquals(14.9999, updated?.latitude ?: 0.0, 0.0001)
        assertEquals(101.8888, updated?.longitude ?: 0.0, 0.0001)
        assertEquals("MANUAL_PIN", updated?.locationProvider)
    }

    @Test
    fun testDeleteHouseholdLocalFirst() = runTest {
        // Prepare household and a family member
        val household = Household(
            householdUuid = "H-TEST-002",
            houseNo = "124",
            villageNo = "8",
            subdistrict = "ป่าขะ",
            district = "บ้านนา",
            province = "นครนายก"
        )
        val householdId = repository.insertHousehold(household)
        val insertedHousehold = repository.getHouseholdById(householdId)
        assertNotNull(insertedHousehold)

        val person = Person(
            personUuid = "P-TEST-002",
            householdId = householdId,
            fullName = "นายทดสอบ มั่งคง",
            gender = Gender.MALE,
            birthDate = java.time.LocalDate.of(1995, 1, 1),
            houseStatus = HouseholdRole.HEAD,
            personStatus = PersonStatus.ALIVE
        )
        repository.insert(person)

        var deleteSuccess: Boolean? = null

        viewModel.deleteHousehold(insertedHousehold!!) { success, _ ->
            deleteSuccess = success
        }

        // Real blocking sleep to allow background Dispatchers.IO threads to execute
        var attempts = 0
        while (deleteSuccess == null && attempts < 100) {
            java.lang.Thread.sleep(15)
            testDispatcher.scheduler.advanceUntilIdle()
            attempts++
        }

        assertEquals(true, deleteSuccess)

        // Verify household is gone from DB
        assertNull(repository.getHouseholdById(householdId))

        // Verify cascaded members are gone too
        assertNull(repository.getPersonByUuid("P-TEST-002"))
    }
}
