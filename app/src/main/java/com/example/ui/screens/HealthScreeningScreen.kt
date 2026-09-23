package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.example.R
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.example.data.HealthScreening
import com.example.data.Person
import com.example.viewmodel.PersonViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.GeminiViewModel
import com.example.viewmodel.GeminiUiState
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.rememberScrollState
import java.time.LocalDate
import java.time.Period
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreeningScreen(
    viewModel: PersonViewModel,
    personId: Long,
    onNavigateBack: () -> Unit,
    geminiViewModel: GeminiViewModel = viewModel()
) {
    var person by remember { mutableStateOf<Person?>(null) }
    val screenings by viewModel.getScreeningsForPerson(personId).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var showAiAdvice by remember { mutableStateOf(false) }
    val geminiUiState by geminiViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(personId) {
        person = viewModel.getPersonById(personId)
    }

    val weightEntries = remember(screenings) {
        screenings.reversed().filter { it.weight != null }.mapIndexed { index, item ->
            FloatEntry(index.toFloat(), item.weight!!.toFloat())
        }
    }
    val chartModel = remember(weightEntries) {
        if (weightEntries.size >= 2) entryModelOf(weightEntries) else null
    }

    val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("th", "TH"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.health_screening_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        person?.let {
                            Text(
                                it.fullName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel_button), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.example.ui.theme.EmeraldPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = com.example.ui.theme.EmeraldPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_screening))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (screenings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_screening_history),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (chartModel != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().height(200.dp).padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("แนวโน้มน้ำหนัก (กก.)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Chart(
                                        chart = lineChart(),
                                        model = chartModel,
                                        startAxis = rememberStartAxis(),
                                        bottomAxis = rememberBottomAxis(),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        item {
                            val latest = screenings.firstOrNull()
                            if (latest != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(16.dp),
                                    onClick = {
                                        person?.let { p ->
                                            val age = if (p.birthDate != null) {
                                                Period.between(p.birthDate, LocalDate.now()).years
                                            } else 0
                                            
                                            geminiViewModel.generateHealthAdvice(
                                                personName = p.fullName,
                                                age = age,
                                                gender = p.gender.name,
                                                weightKg = latest.weight ?: 0.0,
                                                heightCm = latest.height ?: 0.0,
                                                systolic = latest.systolic ?: 0,
                                                diastolic = latest.diastolic ?: 0,
                                                sugar = (latest.bloodSugar ?: 0).toDouble()
                                            )
                                            showAiAdvice = true
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(stringResource(R.string.ai_get_advice), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text("วิเคราะห์แนวโน้มสุขภาพด้วย Gemini Pro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    items(screenings) { item ->
                        ScreeningCard(screening = item, onDelete = { viewModel.deleteScreening(item) })
                    }
                }
            }
        }

        if (showAddDialog) {
            HealthScreeningFormDialog(
                onDismiss = { showAddDialog = false },
                onSave = { weight, height, systolic, diastolic, bloodSugar, note ->
                    val bmi = if (weight != null && height != null && height > 0) {
                        val heightMeter = height / 100.0
                        weight / (heightMeter * heightMeter)
                    } else null
                    
                    viewModel.insertScreening(
                        HealthScreening(
                            personId = personId,
                            weight = weight,
                            height = height,
                            bmi = bmi,
                            systolic = systolic,
                            diastolic = diastolic,
                            bloodSugar = bloodSugar,
                            note = note,
                            vhvName = "อสม. ในพื้นที่" // Placeholder or from profile
                        )
                    )
                    showAddDialog = false
                }
            )
        }

        if (showAiAdvice) {
            AiAdviceDialog(
                uiState = geminiUiState,
                onDismiss = {
                    showAiAdvice = false
                    geminiViewModel.clearState()
                }
            )
        }
    }
}

@Composable
fun AiAdviceDialog(
    uiState: GeminiUiState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ai_advisor_title))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                when (uiState) {
                    is GeminiUiState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.ai_loading))
                        }
                    }
                    is GeminiUiState.Success -> {
                        Text(uiState.response, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.ai_disclaimer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                    is GeminiUiState.Error -> {
                        Text("เกิดข้อผิดพลาด: ${uiState.message}", color = MaterialTheme.colorScheme.error)
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ตกลง")
            }
        }
    )
}

@Composable
fun ScreeningCard(screening: HealthScreening, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm น.", Locale("th", "TH"))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(screening.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "ลบ", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Blood Pressure
                if (screening.systolic != null && screening.diastolic != null) {
                    val bpStatus = when {
                        screening.systolic >= 160 || screening.diastolic >= 100 -> "สูงมาก" to Color(0xFFB71C1C)
                        screening.systolic >= 140 || screening.diastolic >= 90 -> "สูง" to Color(0xFFD32F2F)
                        screening.systolic >= 130 || screening.diastolic >= 85 -> "ค่อนข้างสูง" to Color(0xFFF57C00)
                        else -> "ปกติ" to Color(0xFF2E7D32)
                    }
                    ScreeningItem(
                        label = stringResource(R.string.bp_label) + " (mmHg)",
                        value = "${screening.systolic}/${screening.diastolic}",
                        status = bpStatus.first,
                        statusColor = bpStatus.second,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // BMI
                if (screening.bmi != null) {
                    val bmiStatus = when {
                        screening.bmi >= 30 -> "อ้วนอันตราย" to Color(0xFFB71C1C)
                        screening.bmi >= 25 -> "อ้วน" to Color(0xFFD32F2F)
                        screening.bmi >= 23 -> "ท้วม" to Color(0xFFF57C00)
                        screening.bmi >= 18.5 -> "ปกติ" to Color(0xFF2E7D32)
                        else -> "ผอม" to Color(0xFF0288D1)
                    }
                    ScreeningItem(
                        label = "BMI",
                        value = "%.1f".format(screening.bmi),
                        status = bmiStatus.first,
                        statusColor = bmiStatus.second,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            if (screening.bloodSugar != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val sugarStatus = when {
                    screening.bloodSugar >= 126 -> "เสี่ยงเบาหวาน" to Color(0xFFD32F2F)
                    screening.bloodSugar >= 100 -> "เริ่มสูง" to Color(0xFFF57C00)
                    else -> "ปกติ" to Color(0xFF2E7D32)
                }
                ScreeningItem(
                    label = stringResource(R.string.sugar_label),
                    value = screening.bloodSugar.toString(),
                    status = sugarStatus.first,
                    statusColor = sugarStatus.second
                )
            }
            
            if (!screening.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.note_label) + ": ${screening.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScreeningItem(
    label: String,
    value: String,
    status: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(statusColor.copy(alpha = 0.05f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(100.dp),
            color = statusColor.copy(alpha = 0.1f)
        ) {
            Text(
                status,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HealthScreeningFormDialog(
    onDismiss: () -> Unit,
    onSave: (Double?, Double?, Int?, Int?, Int?, String) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var bloodSugar by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_screening), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("น้ำหนัก (กก.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("ส่วนสูง (ซม.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Text("ความดันโลหิต", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = systolic,
                        onValueChange = { systolic = it },
                        label = { Text("ตัวบน (SYS)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = diastolic,
                        onValueChange = { diastolic = it },
                        label = { Text("ตัวล่าง (DIA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                OutlinedTextField(
                    value = bloodSugar,
                    onValueChange = { bloodSugar = it },
                    label = { Text("ระดับน้ำตาลในเลือด (mg/dL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("หมายเหตุ / คำแนะนำ") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        weight.toDoubleOrNull(),
                        height.toDoubleOrNull(),
                        systolic.toIntOrNull(),
                        diastolic.toIntOrNull(),
                        bloodSugar.toIntOrNull(),
                        note
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.EmeraldPrimary)
            ) {
                Text("บันทึก")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}
