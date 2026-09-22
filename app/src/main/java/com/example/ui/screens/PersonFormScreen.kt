package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Person
import com.example.ui.theme.*
import com.example.viewmodel.PersonViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonFormScreen(
    viewModel: PersonViewModel,
    personId: Long,
    householdId: Long,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(personId != -1L) }

    var nationalId by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(com.example.data.Gender.MALE) }
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var houseStatus by remember { mutableStateOf(com.example.data.HouseholdRole.HEAD) }
    var personStatus by remember { mutableStateOf(com.example.data.PersonStatus.ALIVE) }
    var dataStatus by remember { mutableStateOf(com.example.data.DataStatus.VERIFIED) }
    var isBirthYearOnly by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = birthDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: Instant.now().toEpochMilli()
    )

    LaunchedEffect(personId) {
        if (personId != -1L) {
            val person = viewModel.getPersonById(personId)
            person?.let {
                nationalId = it.nationalId ?: ""
                fullName = it.fullName
                gender = it.gender
                birthDate = it.birthDate
                houseStatus = it.houseStatus
                personStatus = it.personStatus
                dataStatus = it.dataStatus
                isBirthYearOnly = it.isBirthYearOnly
            }
            isLoading = false
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        birthDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        isBirthYearOnly = false
                    }
                    showDatePicker = false
                }) { Text("ตกลง") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("ยกเลิก") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (personId == -1L) "เพิ่มข้อมูลประชากร" else "แก้ไขข้อมูลประชากร",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ย้อนกลับ",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.example.ui.theme.EmeraldPrimary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: General Info
                FormSectionCard(title = "ข้อมูลพื้นฐาน") {
                    var nationalIdError by remember { mutableStateOf(false) }
                    var nationalIdErrorMessage by remember { mutableStateOf("") }
                    
                    OutlinedTextField(
                        value = nationalId,
                        onValueChange = {
                            if (it.length <= 13 && it.all { char -> char.isDigit() }) {
                                nationalId = it
                                nationalIdError = false
                            }
                        },
                        label = { Text("เลขบัตรประชาชน (13 หลัก)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = nationalIdError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    if (nationalIdError) {
                        Text(nationalIdErrorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("ชื่อ-นามสกุล") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownMenuField(
                                label = "เพศ",
                                options = listOf("ชาย", "หญิง"),
                                selectedOption = gender.value,
                                onOptionSelected = { gender = com.example.data.Gender.fromString(it) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = com.example.utils.ValidationUtils.formatThaiDateDisplay(birthDate, isBirthYearOnly),
                                onValueChange = {},
                                label = { Text("วันเกิด") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Filled.DateRange, contentDescription = "เลือกวันเกิด")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isBirthYearOnly,
                            onCheckedChange = { checked ->
                                isBirthYearOnly = checked
                                if (checked && birthDate != null) {
                                    birthDate = LocalDate.of(birthDate!!.year, 1, 1)
                                }
                            }
                        )
                        Text(
                            text = "ระบุเฉพาะปีเกิด (พ.ศ.) ไม่ทราบวันเดือน",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable {
                                isBirthYearOnly = !isBirthYearOnly
                                if (isBirthYearOnly && birthDate != null) {
                                    birthDate = LocalDate.of(birthDate!!.year, 1, 1)
                                }
                            }
                        )
                    }

                    if (isBirthYearOnly) {
                        var yearInput by remember(birthDate) {
                            mutableStateOf(if (birthDate != null) (birthDate!!.year + 543).toString() else "")
                        }
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { input ->
                                if (input.length <= 4 && input.all { it.isDigit() }) {
                                    yearInput = input
                                    if (input.length == 4) {
                                        val y = input.toInt()
                                        val adYear = if (y > 2400) y - 543 else y
                                        birthDate = LocalDate.of(adYear, 1, 1)
                                    }
                                }
                            },
                            label = { Text("ระบุปีเกิด พ.ศ. (เช่น 2498)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Section 2: Status
                FormSectionCard(title = "รายละเอียดและสถานะ") {
                    DropdownMenuField(
                        label = "สถานะในบ้าน",
                        options = listOf("เจ้าบ้าน", "ผู้อาศัย"),
                        selectedOption = houseStatus.value,
                        onOptionSelected = { houseStatus = com.example.data.HouseholdRole.fromString(it) }
                    )

                    DropdownMenuField(
                        label = "สถานะบุคคล",
                        options = listOf("มีชีวิต", "เสียชีวิต"),
                        selectedOption = personStatus.value,
                        onOptionSelected = { personStatus = com.example.data.PersonStatus.fromString(it) }
                    )

                    DropdownMenuField(
                        label = "สถานะข้อมูล",
                        options = listOf("ยืนยันแล้ว", "ต้องตรวจสอบ"),
                        selectedOption = dataStatus.value,
                        onOptionSelected = { dataStatus = com.example.data.DataStatus.fromString(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val normalizedId = com.example.utils.ValidationUtils.normalizeNationalId(nationalId)
                            if (normalizedId.isNotBlank() && !viewModel.validateThaiNationalId(normalizedId)) {
                                Toast.makeText(context, "เลขบัตรประชาชนไม่ถูกต้องตามหลักการคำนวณ", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            
                            // Check for duplicates
                            if (normalizedId.isNotBlank()) {
                                val existingPerson = viewModel.getPersonByNationalId(normalizedId)
                                if (existingPerson != null && existingPerson.id != personId) {
                                    Toast.makeText(context, "เลขบัตรประชาชนนี้มีอยู่ในระบบแล้ว", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                            }

                            if (fullName.isNotBlank() && householdId != -1L) {
                                val person = Person(
                                    id = if (personId == -1L) 0 else personId,
                                    householdId = householdId,
                                    nationalId = normalizedId.ifBlank { null },
                                    fullName = fullName,
                                    gender = gender,
                                    birthDate = birthDate,
                                    isBirthYearOnly = isBirthYearOnly,
                                    houseStatus = houseStatus,
                                    personStatus = personStatus,
                                    dataStatus = dataStatus
                                )
                                if (personId == -1L) {
                                    viewModel.insert(person)
                                } else {
                                    viewModel.update(person)
                                }
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "กรุณากรอกข้อมูลให้ครบถ้วน", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("บันทึกข้อมูล", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FormSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp), spotColor = CardShadowTint),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            content()
        }
    }
}
