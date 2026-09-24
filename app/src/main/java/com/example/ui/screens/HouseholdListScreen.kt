package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Villa
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.theme.*
import com.example.viewmodel.PersonViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdListScreen(
    viewModel: PersonViewModel,
    onHouseClick: (Long) -> Unit,
    onAddHouseClick: () -> Unit,
    onScanQrClick: () -> Unit = {}
) {
    val houseSummary by viewModel.houseSummary.collectAsStateWithLifecycle()
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()
    val importPlan by viewModel.importPlan.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ทั้งหมด") }

    val filterOptions = listOf(
        "ทั้งหมด", 
        "มีผู้สูงอายุ", 
        "มีเด็กเล็ก", 
        "มีผู้เสียชีวิต", 
        "ไม่มีพิกัด GPS"
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importExcelData(context, it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportExcelData(context, it) { success, message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    if (importPlan != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearImportPlan() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("ตรวจสอบแผนการนำเข้า", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "พบคอมมิตข้อมูลทั้งหมด ${importPlan!!.totalRows} แถว กรุณาตรวจสอบแผนการบันทึกก่อนยืนยันเพื่อความถูกต้องของข้อมูล",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = StatusVerifiedBg.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, StatusVerifiedBg)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("เพิ่มใหม่", style = MaterialTheme.typography.labelSmall, color = StatusVerifiedFg)
                                Text("${importPlan!!.insertCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusVerifiedFg)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("อัปเดต", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Text("${importPlan!!.updateCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1.2f),
                            colors = CardDefaults.cardColors(containerColor = StatusNeedsReviewBg.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, StatusNeedsReviewBg)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("ต้องตรวจ", style = MaterialTheme.typography.labelSmall, color = StatusNeedsReviewFg)
                                Text("${importPlan!!.needsReviewCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusNeedsReviewFg)
                            }
                        }
                    }

                    if (importPlan!!.errors.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusDeadBg.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, StatusDeadBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "พบข้อผิดพลาดร้ายแรง (${importPlan!!.errors.size} รายการ):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusDeadFg,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.heightIn(max = 80.dp)) {
                                    androidx.compose.foundation.lazy.LazyColumn {
                                        items(importPlan!!.errors) { err ->
                                            Text(
                                                "• แถวที่ ${err.rowNumber}: ${err.reason}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StatusDeadFg
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val itemsNeedingReview = importPlan!!.plannedItems.filter { it.action == com.example.domain.ImportAction.NEEDS_REVIEW }
                    if (itemsNeedingReview.isNotEmpty()) {
                        Text(
                            "รายการประชากรที่ติดข้อสงสัยและต้องการการตรวจสอบก่อน:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusNeedsReviewFg
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.heightIn(max = 180.dp)) {
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(itemsNeedingReview) { item ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "แถวที่ ${item.rowNum}: ${item.personData.fullName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "บ้านเลขที่ ${item.householdData.houseNo}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = OnSurfaceSecondary
                                                )
                                            }
                                            item.reviewReasons.forEach { reason ->
                                                Text(
                                                    "⚠️ $reason",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = StatusNeedsReviewFg,
                                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusVerifiedBg.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, StatusVerifiedBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "🎉 ตรวจสอบแล้วไม่มีปัญหาความขัดแย้งของข้อมูล ทุกรายการสามารถบันทึกได้ทันที",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusVerifiedFg
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.commitCurrentImportPlan { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("ยืนยันการนำเข้า", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.clearImportPlan() }
                ) {
                    Text("ยกเลิก", color = OnSurfaceSecondary)
                }
            }
        )
    }

    if (importResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearImportResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StatusVerifiedBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("ผลการนำเข้าข้อมูล", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ทั้งหมด: ${importResult!!.totalRows} รายการ", fontWeight = FontWeight.Bold)
                    Row {
                        Text("• สำเร็จ: ", color = OnSurfaceSecondary)
                        Text("${importResult!!.successCount} รายการ", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        Text("• ผิดพลาด: ", color = OnSurfaceSecondary)
                        Text("${importResult!!.failedCount} รายการ", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = HairlineBorder)
                    Text("รายละเอียดจำแนกข้อผิดพลาด:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("- ข้อมูลซ้ำ: ${importResult!!.duplicateCount}", style = MaterialTheme.typography.bodySmall)
                    Text("- เลขบัตร ปชช. ไม่ถูกต้อง: ${importResult!!.invalidNationalIdCount}", style = MaterialTheme.typography.bodySmall)
                    Text("- รูปแบบวันเกิดผิด: ${importResult!!.invalidBirthDateCount}", style = MaterialTheme.typography.bodySmall)
                    Text("- ไม่มีเลขที่บ้าน: ${importResult!!.invalidHouseNoCount}", style = MaterialTheme.typography.bodySmall)
                    Text("- ข้อมูลที่ต้องตรวจสอบ: ${importResult!!.needsReviewCount}", style = MaterialTheme.typography.bodySmall, color = StatusNeedsReviewFg)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearImportResult() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("รับทราบ")
                }
            }
        )
    }

    if (isImporting) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("กำลังนำเข้าข้อมูล...") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("กำลังประมวลผลไฟล์ Excel กรุณารอสักครู่")
                }
            },
            confirmButton = { }
        )
    }

    val filteredList = remember(houseSummary, searchQuery, selectedFilter) {
        var result = houseSummary
        if (searchQuery.isNotBlank()) {
            result = result.filter { it.houseNo.contains(searchQuery.trim(), ignoreCase = true) }
        }
        when (selectedFilter) {
            "มีผู้สูงอายุ" -> result = result.filter { it.elderly > 0 }
            "มีเด็กเล็ก" -> result = result.filter { it.children > 0 }
            "มีผู้เสียชีวิต" -> result = result.filter { it.deceased > 0 }
            "ไม่มีพิกัด GPS" -> result = result.filter { it.latitude == null || it.longitude == null || it.latitude == 0.0 }
        }
        result
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "ทะเบียนครัวเรือน",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "ทั้งหมด ${houseSummary.size} ครัวเรือน",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onScanQrClick) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "สแกน QR Code")
                    }
                    ThemeQuickToggleButton(iconTint = Color.White)
                    IconButton(
                        onClick = { exportLauncher.launch("smart_osm_households.xlsx") }
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "ส่งออก Excel")
                    }
                    IconButton(
                        onClick = { importLauncher.launch("*/*") }
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = "นำเข้า Excel")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddHouseClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("เพิ่มครัวเรือน", fontWeight = FontWeight.Bold) },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp), spotColor = CardShadowTint)
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Search Bar header span
            item(span = { GridItemSpan(2) }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ค้นหาด้วยบ้านเลขที่...", color = OnSurfaceTertiary) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = EmeraldPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "ล้างการค้นหา", tint = OnSurfaceTertiary)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Filter Chips
            item(span = { GridItemSpan(2) }) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions) { option ->
                        val isSelected = selectedFilter == option
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = option },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            // Quick Data Sync Banner
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MintAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "ผลการค้นหา: ${filteredList.size} หลังคาเรือน" else "ข้อมูลสำรวจล่าสุด",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "แตะเพื่อดูสมาชิก",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick Import Button for PopulationData_Moo8.xlsx
            item(span = { GridItemSpan(2) }) {
                Button(
                    onClick = {
                        viewModel.importWorkspaceExcelFile("PopulationData_Moo8.xlsx") { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("นำเข้าข้อมูลทะเบียนประชากร (ไฟล์ตัวอย่าง)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (filteredList.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_state),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                if (searchQuery.isBlank()) "ยังไม่มีข้อมูลครัวเรือน" else "ไม่พบข้อมูลบ้านเลขที่ \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "กดปุ่ม + ด้านล่างเพื่อเพิ่มข้อมูลใหม่",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.householdId }) { summary ->
                    HouseholdCard(
                        summary = summary,
                        onClick = { onHouseClick(summary.householdId) }
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun HouseholdCard(
    summary: com.example.data.HouseSummary,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val badgeBg = if (isDark) StatusVerifiedBgDark else StatusVerifiedBg
    val badgeFg = if (isDark) StatusVerifiedFgDark else StatusVerifiedFg

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = CardShadowTint)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (summary.totalMembers > 0) badgeBg else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (summary.totalMembers > 0) badgeFg else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${summary.totalMembers}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.totalMembers > 0) badgeFg else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column {
                Text(
                    text = "บ้านเลขที่",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summary.houseNo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (!summary.headName.isNullOrBlank()) {
                    Text(
                        text = summary.headName,
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${summary.totalMembers} คนในทะเบียน",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

