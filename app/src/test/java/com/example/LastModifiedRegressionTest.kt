package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.ExcelImportUseCase
import com.example.domain.ImportAction
import com.example.viewmodel.PersonViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LastModifiedRegressionTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PersonRepository
    private lateinit var excelImportUseCase: ExcelImportUseCase
    private lateinit var viewModel: PersonViewModel
    private lateinit var context: Context

    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()

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
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testPersonUpdateChangesLastModified() = runBlocking {
        val pastTimestamp = 1_000_000L

        // 1. Insert household and person with explicit past lastModified
        val household = Household(
            householdUuid = "H-TEST-MOD-01",
            houseNo = "10/1",
            villageNo = "1",
            subdistrict = "Sub",
            district = "Dist",
            province = "Prov",
            lastModified = pastTimestamp
        )
        val hId = db.householdDao().insert(household)

        val person = Person(
            personUuid = "P-TEST-MOD-01",
            householdId = hId,
            nationalId = "1100500123456",
            fullName = "นาย ก่อนแก้ไข",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 1, 1),
            lastModified = pastTimestamp
        )
        val pId = db.personDao().insertPerson(person)

        val initialPerson = db.personDao().getPersonById(pId)
        assertNotNull(initialPerson)
        assertEquals(pastTimestamp, initialPerson?.lastModified)

        // 2. Act: Edit person via ViewModel
        val editCandidate = initialPerson!!.copy(fullName = "นาย หลังแก้ไขเสร็จ")
        viewModel.update(editCandidate)
        testDispatcher.scheduler.advanceUntilIdle()

        // 3. Assert: Verify in Room that old lastModified < new lastModified
        val updatedPerson = db.personDao().getPersonById(pId)
        assertNotNull(updatedPerson)
        assertEquals("นาย หลังแก้ไขเสร็จ", updatedPerson?.fullName)
        assertTrue(
            "Expected old lastModified ($pastTimestamp) < new lastModified (${updatedPerson?.lastModified})",
            pastTimestamp < (updatedPerson?.lastModified ?: 0L)
        )
    }

    @Test
    fun testHouseholdUpdateChangesLastModified() = runBlocking {
        val pastTimestamp = 2_000_000L

        // 1. Insert household with explicit past lastModified
        val household = Household(
            householdUuid = "H-TEST-MOD-02",
            houseNo = "20/2",
            villageNo = "2",
            subdistrict = "Sub",
            district = "Dist",
            province = "Prov",
            lastModified = pastTimestamp
        )
        val hId = db.householdDao().insert(household)

        val initialHousehold = db.householdDao().getHouseholdById(hId)
        assertNotNull(initialHousehold)
        assertEquals(pastTimestamp, initialHousehold?.lastModified)

        // 2. Act: Edit household via ViewModel
        val editCandidate = initialHousehold!!.copy(houseNo = "20/99")
        viewModel.updateHousehold(editCandidate)
        testDispatcher.scheduler.advanceUntilIdle()

        // 3. Assert: Verify in Room that old lastModified < new lastModified
        val updatedHousehold = db.householdDao().getHouseholdById(hId)
        assertNotNull(updatedHousehold)
        assertEquals("20/99", updatedHousehold?.houseNo)
        assertTrue(
            "Expected old lastModified ($pastTimestamp) < new lastModified (${updatedHousehold?.lastModified})",
            pastTimestamp < (updatedHousehold?.lastModified ?: 0L)
        )
    }

    @Test
    fun testExcelImportUpdateChangesLastModified() = runBlocking {
        val pastTimestamp = 3_000_000L
        val targetPersonUuid = "P-EXCEL-MOD-01"

        // 1. Insert household and person with explicit past lastModified
        val household = Household(
            householdUuid = "H-EXCEL-MOD-01",
            houseNo = "100/1",
            villageNo = "1",
            subdistrict = "Sub",
            district = "Dist",
            province = "Prov",
            lastModified = pastTimestamp
        )
        val hId = db.householdDao().insert(household)

        val person = Person(
            personUuid = targetPersonUuid,
            householdId = hId,
            nationalId = "1100500999999",
            fullName = "นาย ก่อนนำเข้า Excel",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1985, 6, 15),
            lastModified = pastTimestamp
        )
        val pId = db.personDao().insertPerson(person)

        val initialPerson = db.personDao().getPersonById(pId)
        assertNotNull(initialPerson)
        assertEquals(pastTimestamp, initialPerson?.lastModified)

        // 2. Prepare Excel workbook with matching personUuid and updated name
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("UpdateTest")
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("personUuid")
        headerRow.createCell(1).setCellValue("houseNo")
        headerRow.createCell(2).setCellValue("fullName")
        headerRow.createCell(3).setCellValue("birthDate")

        val dataRow = sheet.createRow(1)
        dataRow.createCell(0).setCellValue(targetPersonUuid)
        dataRow.createCell(1).setCellValue("100/1")
        dataRow.createCell(2).setCellValue("นาย หลังนำเข้าจาก Excel")
        dataRow.createCell(3).setCellValue("15/06/2528")

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()

        // 3. Act: Create and Commit import plan
        val plan = excelImportUseCase.createImportPlan(ByteArrayInputStream(out.toByteArray()))
        assertEquals(1, plan.plannedItems.size)
        assertEquals(ImportAction.UPDATE, plan.plannedItems[0].action)

        val commitResult = excelImportUseCase.commitImportPlan(plan)
        assertEquals(1, commitResult.successCount)

        // 4. Assert: Verify in Room that person data is updated and old lastModified < new lastModified
        val updatedPerson = db.personDao().getPersonById(pId)
        assertNotNull(updatedPerson)
        assertEquals("นาย หลังนำเข้าจาก Excel", updatedPerson?.fullName)
        assertTrue(
            "Expected old lastModified ($pastTimestamp) < new lastModified (${updatedPerson?.lastModified})",
            pastTimestamp < (updatedPerson?.lastModified ?: 0L)
        )
    }

    @Test
    fun testRoomPersistsCustomLastModified() = runBlocking {
        val customTimestamp = 9876543210L

        val household = Household(
            householdUuid = "H-ROOM-PERSIST-01",
            houseNo = "55/5",
            villageNo = "5",
            subdistrict = "Sub",
            district = "Dist",
            province = "Prov",
            lastModified = customTimestamp
        )
        val hId = db.householdDao().insert(household)

        val person = Person(
            personUuid = "P-ROOM-PERSIST-01",
            householdId = hId,
            nationalId = "1100500777777",
            fullName = "นางสาว ทดสอบเวลา",
            gender = Gender.FEMALE,
            birthDate = LocalDate.of(1995, 3, 20),
            lastModified = customTimestamp
        )
        val pId = db.personDao().insertPerson(person)

        val retrievedH = db.householdDao().getHouseholdById(hId)
        val retrievedP = db.personDao().getPersonById(pId)

        assertEquals(customTimestamp, retrievedH?.lastModified)
        assertEquals(customTimestamp, retrievedP?.lastModified)
    }
}
