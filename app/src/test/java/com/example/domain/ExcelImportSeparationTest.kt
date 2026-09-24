package com.example.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExcelImportSeparationTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context
    private lateinit var useCase: ExcelImportUseCase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        useCase = ExcelImportUseCase(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun createTestExcelInputStream(
        houseNo: String = "999/8",
        fullName: String = "นายทองดี สุขใจ",
        nationalId: String = "1-1007-01112-40-6"
    ): ByteArrayInputStream {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(SmartOsmExcelSchema.SHEET_NAME)
        
        // Headers row
        val headerRow = sheet.createRow(0)
        val headers = SmartOsmExcelSchema.CANONICAL_COLUMNS
        headers.forEachIndexed { idx, colName ->
            headerRow.createCell(idx).setCellValue(colName)
        }

        // Data row
        val dataRow = sheet.createRow(1)
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_SCHEMA_VERSION)).setCellValue(SmartOsmExcelSchema.SCHEMA_VERSION)
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_HOUSE_NO)).setCellValue(houseNo)
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_VILLAGE_NO)).setCellValue("8")
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_SUBDISTRICT)).setCellValue("ป่าขะ")
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_DISTRICT)).setCellValue("บ้านนา")
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_PROVINCE)).setCellValue("นครนายก")
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_NATIONAL_ID)).setCellValue(nationalId)
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_FULL_NAME)).setCellValue(fullName)
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_GENDER)).setCellValue("ชาย")
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_BIRTH_DATE)).setCellValue("01/01/2500")
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_BIRTH_DATE_PRECISION)).setCellValue("DAY")
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_HOUSE_STATUS)).setCellValue("เจ้าบ้าน")
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_PERSON_STATUS)).setCellValue("มีชีวิต")
        dataRow.createCell(headers.indexOf(SmartOsmExcelSchema.COL_DATA_STATUS)).setCellValue("VERIFIED")

        val outStream = ByteArrayOutputStream()
        workbook.write(outStream)
        workbook.close()
        return ByteArrayInputStream(outStream.toByteArray())
    }

    @Test
    fun testCreateImportPlanDoesNotWriteToDatabase() = runBlocking {
        val inputStream = createTestExcelInputStream()
        
        // Generate plan
        val plan = useCase.createImportPlan(inputStream)
        
        // Assert plan exists and captures metadata correctly
        assertNotNull(plan)
        assertEquals(1, plan.totalRows)
        assertEquals(1, plan.insertCount)
        assertEquals(0, plan.updateCount)
        assertEquals(0, plan.needsReviewCount)
        assertTrue(plan.errors.isEmpty())

        // Verify that the database remains completely empty (No records written!)
        val localHouseholds = db.householdDao().getAllHouseholds()
        val localPersons = db.personDao().getAllPersonsList()
        assertTrue("Database households must be empty before committing", localHouseholds.isEmpty())
        assertTrue("Database persons must be empty before committing", localPersons.isEmpty())
    }

    @Test
    fun testCommitImportPlanPersistsToDatabase() = runBlocking {
        val inputStream = createTestExcelInputStream(houseNo = "888/2", fullName = "นางมี สุขใจ")
        
        // 1. Create Plan
        val plan = useCase.createImportPlan(inputStream)
        
        // 2. Commit Plan
        val result = useCase.commitImportPlan(plan)
        
        // Assert import outcome is success
        assertNotNull(result)
        assertEquals(1, result.totalRows)
        assertEquals(1, result.successCount)
        assertEquals(0, result.failedCount)

        // Verify that records are now successfully persisted in database
        val localHouseholds = db.householdDao().getAllHouseholds()
        val localPersons = db.personDao().getAllPersonsList()
        assertEquals(1, localHouseholds.size)
        assertEquals(1, localPersons.size)

        val savedHousehold = localHouseholds.first()
        val savedPerson = localPersons.first()

        assertEquals("888/2", savedHousehold.houseNo)
        assertEquals("นางมี สุขใจ", savedPerson.fullName)
        assertEquals(savedHousehold.id, savedPerson.householdId)
    }
}
