package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.PersonViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class WorkCategory(val title: String, val icon: ImageVector, val color: Color) {
    SURVEY("สำรวจข้อมูล", Icons.Filled.Home, Color(0xFF0D9488)),
    ELDERLY_CARE("เยี่ยมผู้สูงอายุ", Icons.Filled.Person, Color(0xFF2563EB)),
    VACCINE_CHILD("วัคซีน/เด็กเล็ก", Icons.Filled.ChildCare, Color(0xFFD97706)),
    CHRONIC_DISEASE("ผู้ป่วยเรื้อรัง/NCDs", Icons.Filled.Favorite, Color(0xFFDC2626)),
    ENVIRONMENT("สุขาภิบาล/ลูกน้ำ", Icons.Filled.WaterDrop, Color(0xFF059669)),
    GENERAL("งานทั่วไป", Icons.Filled.Assignment, Color(0xFF7C3AED))
}

enum class WorkPriority(val title: String, val color: Color) {
    HIGH("ด่วนมาก", Color(0xFFDC2626)),
    MEDIUM("ปกติ", Color(0xFFD97706)),
    LOW("ทั่วไป", Color(0xFF059669))
}

enum class PlanFilter(val title: String) {
    ALL("ทั้งหมด"),
    PENDING("รอดำเนินการ"),
    DONE("เสร็จสิ้นแล้ว"),
    THIS_MONTH("เดือนนี้")
}

data class PlanItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val targetHouseNo: String = "",
    val targetPersonName: String = "",
    val dueDate: String = "",
    val category: WorkCategory = WorkCategory.SURVEY,
    val priority: WorkPriority = WorkPriority.MEDIUM,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanOfWorkScreen(
    viewModel: PersonViewModel,
    onBack: () -> Unit = {},
    onNavigateToHouseDetail: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val allHouseholdsWithPersons by viewModel.allHouseholdsWithPersons.collectAsStateWithLifecycle()
    val allPersons by viewModel.allPersons.collectAsStateWithLifecycle()

    val prefs = remember { context.getSharedPreferences("smart_osm_work_plans", Context.MODE_PRIVATE) }

    // In-memory state synchronized with SharedPreferences JSON/String format
    var plansList by remember {
        mutableStateOf(loadPlansFromStorage(prefs))
    }

    var selectedFilter by remember { mutableStateOf(PlanFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showQuickTemplateDialog by remember { mutableStateOf(false) }
    var filterCategory by remember { mutableStateOf<WorkCategory?>(null) }

    fun savePlans(newList: List<PlanItem>) {
        plansList = newList
        savePlansToStorage(prefs, newList)
    }

    val filteredPlans = remember(plansList, selectedFilter, filterCategory) {
        plansList.filter { plan ->
            val matchStatus = when (selectedFilter) {
                PlanFilter.ALL -> true
                PlanFilter.PENDING -> !plan.isCompleted
                PlanFilter.DONE -> plan.isCompleted
                PlanFilter.THIS_MONTH -> {
                    val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    plan.dueDate.startsWith(currentMonth)
                }
            }
            val matchCategory = filterCategory == null || plan.category == filterCategory
            matchStatus && matchCategory
        }.sortedWith(compareBy({ it.isCompleted }, { it.priority.ordinal }, { it.dueDate }))
    }

    val totalCount = plansList.size
    val completedCount = plansList.count { it.isCompleted }
    val pendingCount = totalCount - completedCount
    val progressPercent = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "แผนการปฏิบัติงาน (Plan of Work)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ตารางงาน อสม. รายวัน/รายเดือน & เยี่ยมบ้าน",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ย้อนกลับ")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showQuickTemplateDialog = true },
                        modifier = Modifier.testTag("btn_quick_template")
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = "สร้างแผนงานมาตรฐาน อสม.", tint = EmeraldPrimary)
                    }
                    IconButton(
                        onClick = {
                            exportPlansAsSummary(context, plansList)
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "แชร์สรุปแผนงาน")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("เพิ่มแผนงานใหม่", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_plan")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
        ) {
            // Header Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) SurfaceVariantDark else EmeraldPrimary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ความก้าวหน้าภารกิจ อสม.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDark) OnSurfaceDark else Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "เสร็จสิ้น $completedCount จาก $totalCount รายการ",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) MintAccent else Color.White
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF0F664C) else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${(progressPercent * 100).toInt()}%",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(100.dp)),
                            color = MintAccent,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "รอดำเนินการ: $pendingCount งาน",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) OnSurfaceVariantDark else Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "เป้าหมาย: ประจำเดือนปัจจุบัน",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) OnSurfaceVariantDark else Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Quick Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlanFilter.values().forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.title, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Category Chips Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "หมวดหมู่:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SuggestionChip(
                        onClick = { filterCategory = null },
                        label = { Text("ทั้งหมด", fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (filterCategory == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    )
                    WorkCategory.values().take(3).forEach { cat ->
                        SuggestionChip(
                            onClick = { filterCategory = if (filterCategory == cat) null else cat },
                            label = { Text(cat.title, fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (filterCategory == cat) cat.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }

            // Task List
            if (filteredPlans.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "ยังไม่มีรายการแผนงานในหมวดนี้",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            OutlinedButton(
                                onClick = { showQuickTemplateDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("โหลดแผนงานมาตรฐาน อสม. (1 คลิก)")
                            }
                        }
                    }
                }
            } else {
                items(filteredPlans, key = { it.id }) { plan ->
                    PlanItemCard(
                        plan = plan,
                        onToggleDone = {
                            val updated = plansList.map {
                                if (it.id == plan.id) {
                                    it.copy(
                                        isCompleted = !it.isCompleted,
                                        completedAt = if (!it.isCompleted) System.currentTimeMillis() else null
                                    )
                                } else it
                            }
                            savePlans(updated)
                        },
                        onDelete = {
                            val updated = plansList.filter { it.id != plan.id }
                            savePlans(updated)
                            Toast.makeText(context, "ลบรายการแผนงานแล้ว", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Add Plan Dialog
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var targetHouseNo by remember { mutableStateOf("") }
        var targetPersonName by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf(LocalDate.now().plusDays(3).toString()) }
        var category by remember { mutableStateOf(WorkCategory.SURVEY) }
        var priority by remember { mutableStateOf(WorkPriority.MEDIUM) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("เพิ่มแผนงานปฏิบัติการ อสม.", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("ชื่องาน/ภารกิจ *") },
                        placeholder = { Text("เช่น เยี่ยมผู้ป่วยติดเตียง บ้านเลขที่ 45/1") },
                        modifier = Modifier.fillMaxWidth().testTag("input_plan_title"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetHouseNo,
                            onValueChange = { targetHouseNo = it },
                            label = { Text("บ้านเลขที่") },
                            placeholder = { Text("เช่น 45/1") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("กำหนดเสร็จ") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = targetPersonName,
                        onValueChange = { targetPersonName = it },
                        label = { Text("กลุ่มเป้าหมาย/ชื่อบุคคล") },
                        placeholder = { Text("เช่น นายสมชาย (ผู้สูงอายุ)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("รายละเอียดงาน/สิ่งที่ต้องทำ") },
                        placeholder = { Text("เช่น ตรวจวัดความดันโลหิต, ตรวจลูกน้ำยุงลาย") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Text("หมวดหมู่งาน:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WorkCategory.values().take(3).forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.title, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "กรุณากรอกชื่องาน", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newPlan = PlanItem(
                            title = title.trim(),
                            description = description.trim(),
                            targetHouseNo = targetHouseNo.trim(),
                            targetPersonName = targetPersonName.trim(),
                            dueDate = dueDate.trim(),
                            category = category,
                            priority = priority
                        )
                        savePlans(listOf(newPlan) + plansList)
                        showAddDialog = false
                        Toast.makeText(context, "บันทึกแผนงานเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("บันทึกแผนงาน")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    // Quick Templates Dialog
    if (showQuickTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showQuickTemplateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("สร้างแผนงานมาตรฐาน อสม.", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ระบบจะสร้างชุดแผนการปฏิบัติงานมาตรฐานประจำเดือนสำหรับ อสม. อัตโนมัติ (6 ภารกิจหลัก):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val templates = getStandardOsmTemplates()
                    templates.forEach { tmpl ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(tmpl.category.icon, contentDescription = null, tint = tmpl.category.color, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(tmpl.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(tmpl.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val templates = getStandardOsmTemplates()
                        savePlans(templates + plansList)
                        showQuickTemplateDialog = false
                        Toast.makeText(context, "เพิ่มแผนงานมาตรฐาน 6 รายการแล้ว", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("เพิ่มแผนงานมาตรฐานทั้งหมด")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickTemplateDialog = false }) {
                    Text("ปิด")
                }
            }
        )
    }
}

@Composable
fun PlanItemCard(
    plan: PlanItem,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = CardShadowTint),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.isCompleted) {
                if (isDark) SurfaceDark.copy(alpha = 0.6f) else Color(0xFFF1F5F9)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (plan.isCompleted) Color.Transparent else plan.category.color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox Circle
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier.size(36.dp)
            ) {
                if (plan.isCompleted) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "เสร็จแล้ว",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        Icons.Filled.RadioButtonUnchecked,
                        contentDescription = "ยังไม่เสร็จ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = plan.category.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = plan.category.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = plan.category.color,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (plan.dueDate.isNotBlank()) {
                        Text(
                            text = "📅 ${plan.dueDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (plan.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (plan.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                if (plan.description.isNotBlank()) {
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (plan.targetHouseNo.isNotBlank() || plan.targetPersonName.isNotBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (plan.targetHouseNo.isNotBlank()) {
                            Text(
                                text = "🏠 บ้านเลขที่ ${plan.targetHouseNo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (plan.targetPersonName.isNotBlank()) {
                            Text(
                                text = "👤 ${plan.targetPersonName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "ลบ",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun getStandardOsmTemplates(): List<PlanItem> {
    val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    return listOf(
        PlanItem(
            title = "สำรวจลูกน้ำยุงลายและใส่ทรายอะเบทประจำสัปดาห์",
            description = "ตรวจภาชนะขังน้ำรอบบ้าน แจกทรายอะเบท และให้ความรู้ป้องกันไข้เลือดออก",
            dueDate = "$currentMonth-07",
            category = WorkCategory.ENVIRONMENT,
            priority = WorkPriority.HIGH
        ),
        PlanItem(
            title = "เยี่ยมบ้านและวัดความดันโลหิตผู้สูงอายุ/ผู้ป่วย NCDs",
            description = "วัดความดัน บันทึกค่าน้ำตาลในเลือด ติดตามการทานยาต่อเนื่อง",
            dueDate = "$currentMonth-14",
            category = WorkCategory.ELDERLY_CARE,
            priority = WorkPriority.HIGH
        ),
        PlanItem(
            title = "ติดตามการฉีดวัคซีนและพัฒนาการเด็ก 0-5 ปี",
            description = "ตรวจสอบสมุดสีชมพู ชั่งน้ำหนัก วัดส่วนสูง และนัดหมายรับวัคซีนที่ รพ.สต.",
            dueDate = "$currentMonth-20",
            category = WorkCategory.VACCINE_CHILD,
            priority = WorkPriority.MEDIUM
        ),
        PlanItem(
            title = "ปรับปรุงข้อมูลสำรวจประชากรและครัวเรือน (Smart OSM)",
            description = "ตรวจสอบสถานะการอยู่อาศัย ย้ายเข้า-ออก และพิกัด GPS บ้านให้เป็นปัจจุบัน",
            dueDate = "$currentMonth-25",
            category = WorkCategory.SURVEY,
            priority = WorkPriority.MEDIUM
        ),
        PlanItem(
            title = "ประชาสัมพันธ์นัดหมายคัดกรองสุขภาพประจำปี",
            description = "แจ้งเตือนประชาชนกลุ่มเสี่ยงตรวจคัดกรองเบาหวาน/ความดันโลหิตสูง",
            dueDate = "$currentMonth-28",
            category = WorkCategory.GENERAL,
            priority = WorkPriority.LOW
        )
    )
}

private fun loadPlansFromStorage(prefs: android.content.SharedPreferences): List<PlanItem> {
    val raw = prefs.getString("saved_plans_json", null) ?: return getStandardOsmTemplates()
    return try {
        val array = org.json.JSONArray(raw)
        val list = mutableListOf<PlanItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                PlanItem(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", ""),
                    description = obj.optString("description", ""),
                    targetHouseNo = obj.optString("targetHouseNo", ""),
                    targetPersonName = obj.optString("targetPersonName", ""),
                    dueDate = obj.optString("dueDate", ""),
                    category = try { WorkCategory.valueOf(obj.optString("category", "GENERAL")) } catch (e: Exception) { WorkCategory.GENERAL },
                    priority = try { WorkPriority.valueOf(obj.optString("priority", "MEDIUM")) } catch (e: Exception) { WorkPriority.MEDIUM },
                    isCompleted = obj.optBoolean("isCompleted", false),
                    completedAt = if (obj.has("completedAt")) obj.optLong("completedAt") else null,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        list
    } catch (e: Exception) {
        getStandardOsmTemplates()
    }
}

private fun savePlansToStorage(prefs: android.content.SharedPreferences, list: List<PlanItem>) {
    try {
        val array = org.json.JSONArray()
        list.forEach { p ->
            val obj = org.json.JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("description", p.description)
                put("targetHouseNo", p.targetHouseNo)
                put("targetPersonName", p.targetPersonName)
                put("dueDate", p.dueDate)
                put("category", p.category.name)
                put("priority", p.priority.name)
                put("isCompleted", p.isCompleted)
                if (p.completedAt != null) put("completedAt", p.completedAt)
                put("createdAt", p.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_plans_json", array.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun exportPlansAsSummary(context: Context, plans: List<PlanItem>) {
    val completed = plans.count { it.isCompleted }
    val total = plans.size
    val sb = StringBuilder()
    sb.append("📋 สรุปแผนการปฏิบัติงาน อสม. (Smart OSM Plan of Work)\n")
    sb.append("ความก้าวหน้า: เสร็จสิ้น $completed/$total รายการ (${if (total > 0) (completed * 100 / total) else 0}%)\n\n")

    plans.forEachIndexed { idx, p ->
        val status = if (p.isCompleted) "✅ [เสร็จ]" else "⏳ [รอทำ]"
        sb.append("${idx + 1}. $status ${p.title}\n")
        if (p.targetHouseNo.isNotBlank()) sb.append("   - บ้านเลขที่: ${p.targetHouseNo}\n")
        if (p.dueDate.isNotBlank()) sb.append("   - กำหนด: ${p.dueDate}\n")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "สรุปแผนการปฏิบัติงาน อสม.")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "แชร์สรุปแผนงาน อสม."))
}
