package com.example

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DataStatus
import com.example.data.Gender
import com.example.data.Household
import com.example.data.HouseholdRole
import com.example.data.Person
import com.example.data.PersonStatus
import com.example.domain.ExcelImportUseCase
import com.example.domain.ImportAction
import com.example.domain.SmartOsmExcelSchema
import com.example.utils.NationalIdStatus
import com.example.utils.ValidationUtils
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
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmartOsmEightRequirementsTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    // 1. PersonDao และ HouseholdDao เอา REPLACE ออก
    @Test
    fun test1_daosDoNotSilentlyReplaceOnConflict() = runBlocking {
        val household = Household(
            householdUuid = "H-UNIQUE-001",
            houseNo = "10/1",
            villageNo = "1",
            subdistrict = "เมือง",
            district = "เมือง",
            province = "เชียงใหม่"
        )
        val hId = db.householdDao().insert(household)
        assertTrue(hId > 0)

        // Inserting the same household UUID should throw SQLiteConstraintException instead of silently replacing
        val duplicateH = household.copy(id = 0, houseNo = "10/2")
        try {
            db.householdDao().insert(duplicateH)
            fail("Expected SQLiteConstraintException on household UUID conflict")
        } catch (e: SQLiteConstraintException) {
            // Expected: REPLACE is removed, now ABORT
            assertTrue(true)
        }

        val person = Person(
            personUuid = "P-UNIQUE-001",
            householdId = hId,
            nationalId = "1100500123451",
            fullName = "สมชาย สบายดี",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 1, 1),
            isBirthYearOnly = false
        )
        val pId = db.personDao().insertPerson(person)
        assertTrue(pId > 0)

        // Inserting the same person UUID should throw SQLiteConstraintException instead of silently replacing
        val duplicateP = person.copy(id = 0, fullName = "สมชาย อื่น")
        try {
            db.personDao().insertPerson(duplicateP)
            fail("Expected SQLiteConstraintException on person UUID conflict")
        } catch (e: SQLiteConstraintException) {
            // Expected: REPLACE is removed, now ABORT
            assertTrue(true)
        }
    }

    // 2. ทำ SmartOsmExcelSchema กลางจริง
    @Test
    fun test2_centralSmartOsmExcelSchemaResolvesHeaders() {
        assertEquals("SMART_OSM_EXCEL_V1", SmartOsmExcelSchema.SCHEMA_VERSION)
        assertEquals("SMART_OSM_V1", SmartOsmExcelSchema.SHEET_NAME)

        // Test Thai aliases
        assertEquals(SmartOsmExcelSchema.COL_HOUSE_NO, SmartOsmExcelSchema.resolveColumnKey("บ้านเลขที่"))
        assertEquals(SmartOsmExcelSchema.COL_HOUSE_NO, SmartOsmExcelSchema.resolveColumnKey("เลขที่บ้าน"))
        assertEquals(SmartOsmExcelSchema.COL_NATIONAL_ID, SmartOsmExcelSchema.resolveColumnKey("เลขบัตรประชาชน"))
        assertEquals(SmartOsmExcelSchema.COL_NATIONAL_ID, SmartOsmExcelSchema.resolveColumnKey("เลขประจำตัวประชาชน"))
        assertEquals(SmartOsmExcelSchema.COL_FULL_NAME, SmartOsmExcelSchema.resolveColumnKey("ชื่อ-นามสกุล"))
        assertEquals(SmartOsmExcelSchema.COL_BIRTH_DATE, SmartOsmExcelSchema.resolveColumnKey("วันเกิด"))
        assertEquals(SmartOsmExcelSchema.COL_BIRTH_DATE_PRECISION, SmartOsmExcelSchema.resolveColumnKey("ความแม่นยำวันเกิด"))

        // Test English aliases
        assertEquals(SmartOsmExcelSchema.COL_HOUSE_NO, SmartOsmExcelSchema.resolveColumnKey("houseNo"))
        assertEquals(SmartOsmExcelSchema.COL_HOUSE_NO, SmartOsmExcelSchema.resolveColumnKey("house_no"))
        assertEquals(SmartOsmExcelSchema.COL_NATIONAL_ID, SmartOsmExcelSchema.resolveColumnKey("nationalId"))
        assertEquals(SmartOsmExcelSchema.COL_PERSON_UUID, SmartOsmExcelSchema.resolveColumnKey("personUuid"))
        assertEquals(SmartOsmExcelSchema.COL_HOUSEHOLD_UUID, SmartOsmExcelSchema.resolveColumnKey("householdUuid"))
    }

    // 3. เปลี่ยน Excel Import เป็น Header Mapping
    @Test
    fun test3_excelImportHeaderMappingSupportsShuffledColumns() = runBlocking {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("ImportTest")

        // Shuffled headers order: FullName first, then HouseNo, then BirthDate, then NationalId
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("ชื่อ-นามสกุล")
        headerRow.createCell(1).setCellValue("บ้านเลขที่")
        headerRow.createCell(2).setCellValue("วันเกิด")
        headerRow.createCell(3).setCellValue("เลขบัตรประชาชน")
        headerRow.createCell(4).setCellValue("เพศ")

        // Data row
        val dataRow = sheet.createRow(1)
        dataRow.createCell(0).setCellValue("วิชัย ใจดี")
        dataRow.createCell(1).setCellValue("99/9")
        dataRow.createCell(2).setCellValue("01/01/2530")
        dataRow.createCell(3).setCellValue("1234567890121")
        dataRow.createCell(4).setCellValue("ชาย")

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()

        val useCase = ExcelImportUseCase(db)
        val plan = useCase.createImportPlan(ByteArrayInputStream(out.toByteArray()))

        assertEquals(1, plan.plannedItems.size)
        val item = plan.plannedItems[0]
        assertEquals("วิชัย ใจดี", item.personData.fullName)
        assertEquals("99/9", item.householdData.houseNo)
        assertEquals(Gender.MALE, item.personData.gender)
    }

    // 4. สร้าง ImportPlan (INSERT, UPDATE, SKIP, NEEDS_REVIEW)
    @Test
    fun test4_importPlanLifecycleAndActions() = runBlocking {
        // Pre-insert a person
        val hId = db.householdDao().insert(
            Household(householdUuid = "H-PLAN-1", houseNo = "123", villageNo = "1", subdistrict = "A", district = "B", province = "C")
        )
        val existingPersonUuid = "P-EXISTING-1"
        db.personDao().insertPerson(
            Person(
                personUuid = existingPersonUuid,
                householdId = hId,
                nationalId = "1234567890121",
                fullName = "เดิมชื่อเก่า",
                gender = Gender.MALE,
                birthDate = LocalDate.of(1995, 5, 5),
                isBirthYearOnly = false
            )
        )

        // Create Excel with 2 rows: 1 UPDATE (matches existing personUuid), 1 INSERT (new person)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("PlanTest")
        val hRow = sheet.createRow(0)
        hRow.createCell(0).setCellValue("personUuid")
        hRow.createCell(1).setCellValue("houseNo")
        hRow.createCell(2).setCellValue("fullName")
        hRow.createCell(3).setCellValue("birthDate")

        val row1 = sheet.createRow(1)
        row1.createCell(0).setCellValue(existingPersonUuid)
        row1.createCell(1).setCellValue("123")
        row1.createCell(2).setCellValue("เดิมชื่อใหม่")
        row1.createCell(3).setCellValue("05/05/2538")

        val row2 = sheet.createRow(2)
        row2.createCell(0).setCellValue("P-NEW-2")
        row2.createCell(1).setCellValue("123")
        row2.createCell(2).setCellValue("คนใหม่ สดใส")
        row2.createCell(3).setCellValue("01/01/2540")

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()

        val useCase = ExcelImportUseCase(db)
        val plan = useCase.createImportPlan(ByteArrayInputStream(out.toByteArray()))

        assertEquals(2, plan.plannedItems.size)
        assertEquals(ImportAction.UPDATE, plan.plannedItems[0].action)
        assertEquals(ImportAction.INSERT, plan.plannedItems[1].action)

        val result = useCase.commitImportPlan(plan)
        assertEquals(2, result.successCount)
    }

    // 5. Duplicate UUID → NEEDS_REVIEW ห้ามสร้าง UUID ใหม่
    @Test
    fun test5_duplicateUuidMarksNeedsReviewAndDoesNotRegenerateUuid() = runBlocking {
        val dupUuid = "P-DUP-UUID-999"
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("DupUuidTest")
        val hRow = sheet.createRow(0)
        hRow.createCell(0).setCellValue("personUuid")
        hRow.createCell(1).setCellValue("houseNo")
        hRow.createCell(2).setCellValue("fullName")
        hRow.createCell(3).setCellValue("birthDate")

        val row1 = sheet.createRow(1)
        row1.createCell(0).setCellValue(dupUuid)
        row1.createCell(1).setCellValue("101")
        row1.createCell(2).setCellValue("คนแรก")
        row1.createCell(3).setCellValue("01/01/2530")

        val row2 = sheet.createRow(2)
        row2.createCell(0).setCellValue(dupUuid) // Duplicate personUuid in file!
        row2.createCell(1).setCellValue("102")
        row2.createCell(2).setCellValue("คนที่สอง ซ้ำ UUID")
        row2.createCell(3).setCellValue("01/01/2532")

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()

        val useCase = ExcelImportUseCase(db)
        val plan = useCase.createImportPlan(ByteArrayInputStream(out.toByteArray()))

        assertEquals(2, plan.plannedItems.size)
        val secondItem = plan.plannedItems[1]

        // Requirement 5 assertions:
        assertTrue(secondItem.isDuplicateUuid)
        assertEquals(ImportAction.NEEDS_REVIEW, secondItem.action)
        // MUST NOT generate new random UUID, must keep the exact duplicate UUID
        assertEquals(dupUuid, secondItem.personData.personUuid)
        assertTrue(secondItem.reviewReasons.any { it.contains("Duplicate personUuid") })
    }

    // 6. เพิ่ม NationalIdStatus
    @Test
    fun test6_nationalIdStatusDetection() {
        // Valid 13 digits checksum
        val validStatus = ValidationUtils.checkNationalIdStatus("1234567890121")
        assertEquals(NationalIdStatus.VALID, validStatus)

        // Invalid checksum
        val invalidStatus = ValidationUtils.checkNationalIdStatus("1234567890120")
        assertEquals(NationalIdStatus.INVALID, invalidStatus)

        // Missing / blank
        val missingStatus1 = ValidationUtils.checkNationalIdStatus("")
        assertEquals(NationalIdStatus.MISSING, missingStatus1)
        val missingStatus2 = ValidationUtils.checkNationalIdStatus(null)
        assertEquals(NationalIdStatus.MISSING, missingStatus2)

        // Unverified
        val unverifiedStatus = ValidationUtils.checkNationalIdStatus("1234567890121", isUnverified = true)
        assertEquals(NationalIdStatus.UNVERIFIED, unverifiedStatus)
    }

    // 7. แก้ Birth Year Precision ตอน Edit/Save
    @Test
    fun test7_birthYearPrecisionPreserved() {
        val yearOnlyDate = LocalDate.of(1955, 1, 1)
        val display = ValidationUtils.formatThaiDateDisplay(yearOnlyDate, isBirthYearOnly = true)
        assertEquals("พ.ศ. 2498", display)

        val fullDateDisplay = ValidationUtils.formatThaiDateDisplay(yearOnlyDate, isBirthYearOnly = false)
        assertEquals("01/01/2498", fullDateDisplay)

        val personWithYearPrecision = Person(
            personUuid = "P-YEAR-1",
            householdId = 1L,
            fullName = "คุณยาย มีแต่ปีเกิด",
            gender = Gender.FEMALE,
            birthDate = yearOnlyDate,
            isBirthYearOnly = true
        )
        assertTrue(personWithYearPrecision.isBirthYearOnly)

        val updated = personWithYearPrecision.copy(fullName = "คุณยาย สบายดี")
        // Check precision remains true after editing/saving
        assertTrue(updated.isBirthYearOnly)
    }

    // 8. Household ambiguous match → NEEDS_REVIEW
    @Test
    fun test8_ambiguousHouseholdMatchMarksNeedsReview() = runBlocking {
        // Pre-insert two distinct households with the EXACT same address components
        val h1 = Household(
            householdUuid = "H-AMBIG-1",
            houseNo = "50",
            villageNo = "3",
            subdistrict = "สันผีเสื้อ",
            district = "เมือง",
            province = "เชียงใหม่"
        )
        val h2 = Household(
            householdUuid = "H-AMBIG-2",
            houseNo = "50",
            villageNo = "3",
            subdistrict = "สันผีเสื้อ",
            district = "เมือง",
            province = "เชียงใหม่"
        )
        db.householdDao().insert(h1)
        db.householdDao().insert(h2)

        // Now import an Excel row that only specifies address without householdUuid
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("AmbiguousTest")
        val hRow = sheet.createRow(0)
        hRow.createCell(0).setCellValue("houseNo")
        hRow.createCell(1).setCellValue("villageNo")
        hRow.createCell(2).setCellValue("subdistrict")
        hRow.createCell(3).setCellValue("district")
        hRow.createCell(4).setCellValue("province")
        hRow.createCell(5).setCellValue("fullName")
        hRow.createCell(6).setCellValue("birthDate")

        val dataRow = sheet.createRow(1)
        dataRow.createCell(0).setCellValue("50")
        dataRow.createCell(1).setCellValue("3")
        dataRow.createCell(2).setCellValue("สันผีเสื้อ")
        dataRow.createCell(3).setCellValue("เมือง")
        dataRow.createCell(4).setCellValue("เชียงใหม่")
        dataRow.createCell(5).setCellValue("สมหมาย อยู่บ้านกำกวม")
        dataRow.createCell(6).setCellValue("01/01/2535")

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()

        val useCase = ExcelImportUseCase(db)
        val plan = useCase.createImportPlan(ByteArrayInputStream(out.toByteArray()))

        assertEquals(1, plan.plannedItems.size)
        val item = plan.plannedItems[0]

        // Requirement 8 assertions:
        assertTrue(item.isAmbiguousHousehold)
        assertEquals(ImportAction.NEEDS_REVIEW, item.action)
        assertTrue(item.reviewReasons.any { it.contains("Ambiguous Match") })
    }
}
