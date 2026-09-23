package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Person
import com.example.data.PersonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.math.BigDecimal
import android.content.Context
import android.net.Uri
import android.widget.Toast
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Cell

import com.example.data.Household
import com.example.data.HouseholdWithPersons
import java.time.Period
import com.example.utils.ValidationUtils

import com.example.data.HouseSummary
import com.example.data.DataStatus
import com.example.data.Gender
import com.example.data.HouseholdRole
import com.example.data.PersonStatus
import com.example.data.PopulationEvent
import com.example.data.PopulationEventType
import com.example.data.sync.RoomFirestoreSyncHelper
import com.example.data.sync.SyncResult
import com.example.data.sync.SyncState

class PersonViewModel(
    private val repository: PersonRepository,
    private val excelImportUseCase: com.example.domain.ExcelImportUseCase,
    private val syncHelper: RoomFirestoreSyncHelper? = null
) : ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (com.example.BuildConfig.DEBUG && repository.getAllHouseholds().isEmpty()) {
                    seedBaselineData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun seedBaselineData() {
        val h1Id = repository.insertHousehold(
            Household(
                householdUuid = java.util.UUID.randomUUID().toString(),
                houseNo = "45/1",
                villageNo = "8",
                subdistrict = "ป่าขะ",
                district = "บ้านนา",
                province = "นครนายก",
                latitude = 14.2155,
                longitude = 101.0723,
                dataStatus = DataStatus.VERIFIED
            )
        )
        repository.insert(Person(
            personUuid = java.util.UUID.randomUUID().toString(),
            householdId = h1Id,
            nationalId = "3260100123451",
            fullName = "นายสมชาย ใจดี",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1975, 4, 12),
            houseStatus = HouseholdRole.HEAD,
            personStatus = PersonStatus.ALIVE,
            dataStatus = DataStatus.VERIFIED
        ))
        repository.insert(Person(
            personUuid = java.util.UUID.randomUUID().toString(),
            householdId = h1Id,
            nationalId = "3260100123452",
            fullName = "นางสาวสมหญิง ใจดี",
            gender = Gender.FEMALE,
            birthDate = LocalDate.of(1978, 9, 25),
            houseStatus = HouseholdRole.RESIDENT,
            personStatus = PersonStatus.ALIVE,
            dataStatus = DataStatus.VERIFIED
        ))
        repository.insert(Person(
            personUuid = java.util.UUID.randomUUID().toString(),
            householdId = h1Id,
            nationalId = "3260100123453",
            fullName = "เด็กชายต้นกล้า ใจดี",
            gender = Gender.MALE,
            birthDate = LocalDate.of(2015, 6, 10),
            houseStatus = HouseholdRole.RESIDENT,
            personStatus = PersonStatus.ALIVE,
            dataStatus = DataStatus.VERIFIED
        ))

        // Seed some sample population events
        repository.insertEvent(
            PopulationEvent(
                type = PopulationEventType.BIRTH,
                title = "เด็กชายสมหวัง ใจดี",
                description = "เกิดเมื่อเวลา 08:30 น. รพ.บ้านนา",
                latitude = 14.2160,
                longitude = 101.0730,
                householdId = h1Id,
                personName = "เด็กชายสมหวัง ใจดี"
            )
        )
        repository.insertEvent(
            PopulationEvent(
                type = PopulationEventType.HEALTH_CHECK,
                title = "ตรวจคัดกรองเบาหวาน/ความดัน",
                description = "ผลการตรวจปกติ ความดัน 120/80",
                latitude = 14.2150,
                longitude = 101.0715,
                householdId = h1Id,
                personName = "นายสมชาย ใจดี"
            )
        )

        val h2Id = repository.insertHousehold(
            Household(
                householdUuid = java.util.UUID.randomUUID().toString(),
                houseNo = "88",
                villageNo = "8",
                subdistrict = "ป่าขะ",
                district = "บ้านนา",
                province = "นครนายก",
                latitude = 14.2162,
                longitude = 101.0741,
                dataStatus = DataStatus.VERIFIED
            )
        )
        repository.insert(Person(
            personUuid = java.util.UUID.randomUUID().toString(),
            householdId = h2Id,
            nationalId = "3260100987651",
            fullName = "นายประเสริฐ มั่งมี",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1960, 12, 1),
            houseStatus = HouseholdRole.HEAD,
            personStatus = PersonStatus.ALIVE,
            dataStatus = DataStatus.VERIFIED
        ))
        repository.insert(Person(
            personUuid = java.util.UUID.randomUUID().toString(),
            householdId = h2Id,
            nationalId = "3260100987652",
            fullName = "นางจันทร์ มั่งมี",
            gender = Gender.FEMALE,
            birthDate = LocalDate.of(1963, 3, 15),
            houseStatus = HouseholdRole.RESIDENT,
            personStatus = PersonStatus.ALIVE,
            dataStatus = DataStatus.VERIFIED
        ))

        val h3Id = repository.insertHousehold(
            Household(
                householdUuid = java.util.UUID.randomUUID().toString(),
                houseNo = "12/4",
                villageNo = "8",
                subdistrict = "ป่าขะ",
                district = "บ้านนา",
                province = "นครนายก",
                latitude = 14.2140,
                longitude = 101.0705,
                dataStatus = DataStatus.VERIFIED
            )
        )

        repository.insertEvent(
            PopulationEvent(
                type = PopulationEventType.MOVE_IN,
                title = "ย้ายเข้าจากกรุงเทพฯ",
                description = "ย้ายเข้าเพื่อมาดูแลบิดามารดา",
                latitude = 14.2135,
                longitude = 101.0695,
                householdId = h3Id,
                personName = "นางสาววิลาวัลย์ มั่งมี"
            )
        )
        repository.insert(Person(
            personUuid = java.util.UUID.randomUUID().toString(),
            householdId = h3Id,
            nationalId = "3260100555441",
            fullName = "นางสาวกัลยา รักสงบ",
            gender = Gender.FEMALE,
            birthDate = LocalDate.of(1992, 8, 19),
            houseStatus = HouseholdRole.HEAD,
            personStatus = PersonStatus.ALIVE,
            dataStatus = DataStatus.VERIFIED
        ))
    }
    
    val syncState: StateFlow<SyncState> = syncHelper?.syncState
        ?: kotlinx.coroutines.flow.MutableStateFlow(SyncState.Idle)

    fun syncToFirestore(onComplete: ((Result<SyncResult>) -> Unit)? = null) {
        if (syncHelper == null) return
        viewModelScope.launch {
            val result = syncHelper.syncRoomToFirestore()
            onComplete?.invoke(result)
        }
    }

    fun syncFromFirestore(onComplete: ((Result<SyncResult>) -> Unit)? = null) {
        if (syncHelper == null) return
        viewModelScope.launch {
            val result = syncHelper.syncFirestoreToRoom()
            onComplete?.invoke(result)
        }
    }

    fun bidirectionalSync(onComplete: ((Result<SyncResult>) -> Unit)? = null) {
        if (syncHelper == null) return
        viewModelScope.launch {
            val result = syncHelper.bidirectionalSync()
            onComplete?.invoke(result)
        }
    }

    fun resetSyncState() {
        syncHelper?.resetSyncState()
    }
    
    private val _importResult = kotlinx.coroutines.flow.MutableStateFlow<com.example.domain.ExcelImportResult?>(null)
    val importResult: StateFlow<com.example.domain.ExcelImportResult?> = _importResult
    
    private val _isImporting = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    fun clearImportResult() {
        _importResult.value = null
    }

    fun importWorkspaceExcelFile(fileName: String = "ทะเบียนประชากร_หมู่8_รายงานสรุป-1.xlsx", onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isImporting.value = true
            try {
                val possiblePaths = listOf(
                    fileName,
                    "ทะเบียนประชากร_หมู่8_รายงานสรุป.xlsx",
                    "/app/$fileName",
                    "app/$fileName"
                )
                var file: java.io.File? = null
                for (p in possiblePaths) {
                    val f = java.io.File(p)
                    if (f.exists()) {
                        file = f
                        break
                    }
                }

                if (file == null || !file.exists()) {
                    withContext(Dispatchers.Main) {
                        _isImporting.value = false
                        onComplete(false, "ไม่พบไฟล์ $fileName บนเครื่อง")
                    }
                    return@launch
                }

                val inputStream = file.inputStream()
                val plan = excelImportUseCase.createImportPlan(inputStream)
                val result = excelImportUseCase.commitImportPlan(plan)
                _importResult.value = result
                withContext(Dispatchers.Main) {
                    _isImporting.value = false
                    onComplete(true, "นำเข้าข้อมูลจาก ${file.name} สำเร็จ (${result.successCount} รายการ)")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isImporting.value = false
                    onComplete(false, "เกิดข้อผิดพลาดในการนำเข้า: ${e.message}")
                }
            }
        }
    }
    
    val allPersons: StateFlow<List<Person>> = repository.allPersons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val allHouseholdsWithPersons: StateFlow<List<HouseholdWithPersons>> = repository.allHouseholdsWithPersons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalPersonsCount: StateFlow<Int> = repository.totalPersonsCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalHouseholdsCount: StateFlow<Int> = repository.totalHouseholdsCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun insertHousehold(household: Household, onComplete: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertHousehold(household.copy(lastModified = System.currentTimeMillis()))
            withContext(Dispatchers.Main) {
                onComplete(id)
            }
        }
    }

    fun addNewHouseholdWithHead(
        houseNo: String,
        headName: String,
        latitude: Double?,
        longitude: Double?,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val uuid = java.util.UUID.randomUUID().toString()
            val household = Household(
                householdUuid = uuid,
                houseNo = houseNo.ifBlank { "000/0" },
                latitude = latitude,
                longitude = longitude,
                locationProvider = if (latitude != null && longitude != null) "MANUAL_DIALOG" else null,
                locationCapturedAt = if (latitude != null && longitude != null) System.currentTimeMillis() else null,
                lastModified = System.currentTimeMillis()
            )
            val houseId = repository.insertHousehold(household)
            
            if (headName.isNotBlank()) {
                val person = Person(
                    personUuid = java.util.UUID.randomUUID().toString(),
                    householdId = houseId,
                    fullName = headName.trim(),
                    houseStatus = HouseholdRole.HEAD,
                    lastModified = System.currentTimeMillis()
                )
                repository.insert(person)
            }
            
            withContext(Dispatchers.Main) {
                onComplete(houseId)
            }
        }
    }
    fun updateHousehold(household: Household) = viewModelScope.launch {
        repository.updateHousehold(household.copy(lastModified = System.currentTimeMillis()))
    }

    fun updateHouseholdDataStatus(household: Household, status: DataStatus) = viewModelScope.launch {
        repository.updateHousehold(household.copy(dataStatus = status, lastModified = System.currentTimeMillis()))
    }

    fun updateHouseholdLocation(
        householdId: Long,
        latitude: Double,
        longitude: Double,
        provider: String = "MANUAL_PIN",
        accuracy: Float? = null,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = repository.getHouseholdById(householdId)
                if (existing == null) {
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(false, "ไม่พบข้อมูลครัวเรือน ID: $householdId")
                    }
                    return@launch
                }
                val updated = existing.copy(
                    latitude = latitude,
                    longitude = longitude,
                    locationAccuracy = accuracy,
                    locationCapturedAt = System.currentTimeMillis(),
                    locationProvider = provider,
                    lastModified = System.currentTimeMillis()
                )
                repository.updateHousehold(updated)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, e.message ?: "เกิดข้อผิดพลาดในการบันทึกพิกัด")
                }
            }
        }
    }

    fun removeHouseholdLocation(
        householdId: Long,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = repository.getHouseholdById(householdId)
                if (existing == null) {
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(false, "ไม่พบข้อมูลครัวเรือน ID: $householdId")
                    }
                    return@launch
                }
                val updated = existing.copy(
                    latitude = null,
                    longitude = null,
                    locationAccuracy = null,
                    locationCapturedAt = null,
                    locationProvider = null,
                    lastModified = System.currentTimeMillis()
                )
                repository.updateHousehold(updated)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, e.message ?: "เกิดข้อผิดพลาดในการลบพิกัด")
                }
            }
        }
    }
    fun deleteHousehold(household: Household, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("PersonViewModel", "Starting delete household: id=${household.id}, uuid=${household.householdUuid}")
                
                // 1. Delete from Firestore FIRST
                // This ensures Cloud tombstones are written BEFORE local Room deletion.
                // Prevents resurrection if app crashes between local delete and Cloud sync.
                if (syncHelper != null && syncHelper.isFirebaseConfigured()) {
                    val cloudResult = syncHelper.deleteHouseholdFromFirestore(household.householdUuid)
                    if (cloudResult.isFailure) {
                        val cloudEx = cloudResult.exceptionOrNull()
                        withContext(Dispatchers.Main) {
                            onResult(false, "ลบข้อมูล Cloud ไม่สำเร็จ (ป้องกันการสูญหายหรือคืนชีพ): ${cloudEx?.message}")
                        }
                        return@launch
                    }
                }

                // 2. Delete locally
                val result = repository.deleteHousehold(household)
                if (result.isFailure) {
                    val ex = result.exceptionOrNull()
                    withContext(Dispatchers.Main) {
                        onResult(false, ex?.message ?: "เกิดข้อผิดพลาดในการลบบ้าน")
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    android.util.Log.d("PersonViewModel", "Household deleted successfully (Local & Cloud)")
                    onResult(true, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("PersonViewModel", "Exception deleting household", e)
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด")
                }
            }
        }
    }
    
    suspend fun getHouseholdById(id: Long): Household? = repository.getHouseholdById(id)
    fun getHouseholdWithPersonsById(id: Long) = repository.getHouseholdWithPersonsById(id)

    fun insert(person: Person) = viewModelScope.launch { 
        repository.insert(person.copy(lastModified = System.currentTimeMillis())) 
    }
    fun update(person: Person) = viewModelScope.launch { 
        repository.update(person.copy(lastModified = System.currentTimeMillis())) 
    }
    fun delete(person: Person, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Delete from Firestore FIRST
                // This ensures Cloud tombstones are written BEFORE local Room deletion.
                // Prevents resurrection if app crashes between local delete and Cloud sync.
                val helper = syncHelper
                if (helper != null && helper.isFirebaseConfigured()) {
                    val cloudResult = helper.deletePersonFromFirestore(person.personUuid)
                    if (cloudResult.isFailure) {
                        val cloudEx = cloudResult.exceptionOrNull()
                        withContext(Dispatchers.Main) {
                            onResult(false, "ลบข้อมูล Cloud ไม่สำเร็จ (ป้องกันการสูญหายหรือคืนชีพ): ${cloudEx?.message}")
                        }
                        return@launch
                    }
                }

                // 2. Delete locally
                repository.delete(person)
                
                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด")
                }
            }
        }
    }
    
    suspend fun getPersonById(id: Long): Person? = repository.getPersonById(id)
    suspend fun getPersonByNationalId(nationalId: String): Person? = repository.getPersonByNationalId(nationalId)
    
    fun validateThaiNationalId(id: String): Boolean = ValidationUtils.isValidThaiNationalId(id)

    fun calculateAge(birthDate: LocalDate?, personStatus: com.example.data.PersonStatus): Int? {
        if (personStatus == com.example.data.PersonStatus.DEAD || birthDate == null) return null
        return Period.between(birthDate, LocalDate.now()).years
    }

    fun getAgeGroup(age: Int?): String {
        if (age == null) return "ไม่ระบุ"
        return when {
            age <= 5 -> "เด็กเล็ก (0-5)"
            age <= 12 -> "เด็กวัยเรียน (6-12)"
            age <= 17 -> "วัยรุ่น (13-17)"
            age <= 24 -> "วัยหนุ่มสาว (18-24)"
            age <= 39 -> "วัยทำงานตอนต้น (25-39)"
            age <= 59 -> "วัยทำงานตอนกลาง (40-59)"
            else -> "ผู้สูงอายุ (60+)"
        }
    }

    val ageGroupSummary: StateFlow<Map<String, Int>> = allPersons.map { persons ->
        val summary = mutableMapOf<String, Int>().withDefault { 0 }
        persons.forEach { person ->
            val age = calculateAge(person.birthDate, person.personStatus)
            val group = getAgeGroup(age)
            summary[group] = summary.getValue(group) + 1
        }
        summary
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val houseSummary: StateFlow<List<HouseSummary>> = repository.houseSummary.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allEvents: StateFlow<List<PopulationEvent>> = repository.allEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertEvent(event: PopulationEvent) = viewModelScope.launch {
        repository.insertEvent(event)
    }

    fun deleteEvent(event: PopulationEvent) = viewModelScope.launch {
        repository.deleteEvent(event)
    }

    fun getHistoryForPerson(personId: Long) = repository.getHistoryForPerson(personId)

    // Health Screening Operations
    fun getScreeningsForPerson(personId: Long) = repository.getScreeningsForPerson(personId)

    fun insertScreening(screening: com.example.data.HealthScreening) = viewModelScope.launch {
        repository.insertScreening(screening)
    }

    fun updateScreening(screening: com.example.data.HealthScreening) = viewModelScope.launch {
        repository.updateScreening(screening)
    }

    fun deleteScreening(screening: com.example.data.HealthScreening) = viewModelScope.launch {
        repository.deleteScreening(screening)
    }

    fun importExcelData(context: Context, uri: Uri) {
        if (_isImporting.value) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _isImporting.value = true
            var inputStream: java.io.InputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val result = excelImportUseCase(inputStream)
                    _importResult.value = result
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "ไม่สามารถเปิดไฟล์ได้", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                inputStream?.close()
                _isImporting.value = false
            }
        }
    }

    fun exportExcelData(context: Context, uri: Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
                val sheet = workbook.createSheet(com.example.domain.SmartOsmExcelSchema.SHEET_NAME)
                
                val hRow = sheet.createRow(0)
                val headers = com.example.domain.SmartOsmExcelSchema.CANONICAL_COLUMNS
                headers.forEachIndexed { idx, title ->
                    hRow.createCell(idx).setCellValue(title)
                }

                val households = repository.getAllHouseholds().associateBy { it.id }
                val persons = repository.getAllPersonsList()

                persons.forEachIndexed { index, p ->
                    val row = sheet.createRow(index + 1)
                    val h = households[p.householdId]

                    row.createCell(0).setCellValue(com.example.domain.SmartOsmExcelSchema.SCHEMA_VERSION)
                    row.createCell(1).setCellValue(h?.householdUuid ?: "")
                    row.createCell(2).setCellValue(p.personUuid)
                    row.createCell(3).setCellValue(h?.houseNo ?: "")
                    row.createCell(4).setCellValue(h?.villageNo ?: "")
                    row.createCell(5).setCellValue(h?.subdistrict ?: "")
                    row.createCell(6).setCellValue(h?.district ?: "")
                    row.createCell(7).setCellValue(h?.province ?: "")
                    row.createCell(8).setCellValue(p.nationalId ?: "")
                    row.createCell(9).setCellValue(p.fullName)
                    row.createCell(10).setCellValue(p.gender.name)
                    row.createCell(11).setCellValue(p.birthDate?.toString() ?: "")
                    row.createCell(12).setCellValue(if (p.isBirthYearOnly) "YEAR" else "DAY")
                    row.createCell(13).setCellValue(p.houseStatus.name)
                    row.createCell(14).setCellValue(p.personStatus.name)
                    row.createCell(15).setCellValue(p.dataStatus.name)
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()

                withContext(Dispatchers.Main) {
                    onComplete(true, "ส่งออกข้อมูล Smart_Osm Schema V1 สำเร็จ")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, "เกิดข้อผิดพลาด: ${e.message}")
                }
            }
        }
    }

    /**
     * Exports population registry database as a temporary Excel file and returns a shareable content Uri
     * for opening or saving via Android System Chooser (Google Drive, Microsoft OneDrive, Dropbox, LINE, etc.).
     */
    fun exportAndShareExcelData(
        context: Context,
        onComplete: (Boolean, Uri?, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
                val sheet = workbook.createSheet(com.example.domain.SmartOsmExcelSchema.SHEET_NAME)

                val hRow = sheet.createRow(0)
                val headers = com.example.domain.SmartOsmExcelSchema.CANONICAL_COLUMNS
                headers.forEachIndexed { idx, title ->
                    hRow.createCell(idx).setCellValue(title)
                }

                val households = repository.getAllHouseholds().associateBy { it.id }
                val persons = repository.getAllPersonsList()

                persons.forEachIndexed { index, p ->
                    val row = sheet.createRow(index + 1)
                    val h = households[p.householdId]

                    row.createCell(0).setCellValue(com.example.domain.SmartOsmExcelSchema.SCHEMA_VERSION)
                    row.createCell(1).setCellValue(h?.householdUuid ?: "")
                    row.createCell(2).setCellValue(p.personUuid)
                    row.createCell(3).setCellValue(h?.houseNo ?: "")
                    row.createCell(4).setCellValue(h?.villageNo ?: "")
                    row.createCell(5).setCellValue(h?.subdistrict ?: "")
                    row.createCell(6).setCellValue(h?.district ?: "")
                    row.createCell(7).setCellValue(h?.province ?: "")
                    row.createCell(8).setCellValue(p.nationalId ?: "")
                    row.createCell(9).setCellValue(p.fullName)
                    row.createCell(10).setCellValue(p.gender.name)
                    row.createCell(11).setCellValue(p.birthDate?.toString() ?: "")
                    row.createCell(12).setCellValue(if (p.isBirthYearOnly) "YEAR" else "DAY")
                    row.createCell(13).setCellValue(p.houseStatus.name)
                    row.createCell(14).setCellValue(p.personStatus.name)
                    row.createCell(15).setCellValue(p.dataStatus.name)
                }

                val exportDir = java.io.File(context.cacheDir, "exports")
                if (!exportDir.exists()) exportDir.mkdirs()
                val fileName = "smart_osm_backup_${System.currentTimeMillis()}.xlsx"
                val file = java.io.File(exportDir, fileName)

                file.outputStream().use { fos ->
                    workbook.write(fos)
                }
                workbook.close()

                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                withContext(Dispatchers.Main) {
                    onComplete(true, contentUri, "เตรียมไฟล์สำรองข้อมูลเรียบร้อยแล้ว (${persons.size} รายการ)")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, null, "เกิดข้อผิดพลาดในการสร้างไฟล์แชร์: ${e.message}")
                }
            }
        }
    }
}
