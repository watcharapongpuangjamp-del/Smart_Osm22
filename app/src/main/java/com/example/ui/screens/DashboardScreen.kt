package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DataStatus
import com.example.data.Gender
import com.example.data.Household
import com.example.data.HouseholdRole
import com.example.data.Person
import com.example.data.PersonStatus
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.theme.*
import com.example.viewmodel.PersonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PersonViewModel,
    onNavigateToHouseholds: () -> Unit = {},
    onNavigateToNewHousehold: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToInfo: () -> Unit = {},
    onNavigateToPlanOfWork: () -> Unit = {},
    onNavigateToHouseDetail: (Long) -> Unit = {},
    onNavigateToQrScan: () -> Unit = {}
) {
    val allPersons by viewModel.allPersons.collectAsStateWithLifecycle()
    val allHouseholdsWithPersons by viewModel.allHouseholdsWithPersons.collectAsStateWithLifecycle()
    val totalPersonsCount by viewModel.totalPersonsCount.collectAsStateWithLifecycle()
    val totalHouseholdsCount by viewModel.totalHouseholdsCount.collectAsStateWithLifecycle()
    val ageGroupSummary by viewModel.ageGroupSummary.collectAsStateWithLifecycle()

    val isDark = isSystemInDarkTheme()

    var showNotificationDialog by remember { mutableStateOf(false) }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text(notificationTitle, fontWeight = FontWeight.Bold) },
            text = { Text(notificationMessage) },
            confirmButton = {
                Button(onClick = { showNotificationDialog = false }) {
                    Text("รับทราบ")
                }
            }
        )
    }

    // Calculated citizen summaries
    val maleCount = remember(allPersons) { allPersons.count { it.gender == Gender.MALE } }
    val femaleCount = remember(allPersons) { allPersons.count { it.gender == Gender.FEMALE } }

    val aliveCount = remember(allPersons) { allPersons.count { it.personStatus == PersonStatus.ALIVE } }
    val deadCount = remember(allPersons) { allPersons.count { it.personStatus == PersonStatus.DEAD } }
    val movedCount = remember(allPersons) { allPersons.count { it.personStatus == PersonStatus.MOVED } }

    val verifiedCount = remember(allPersons) { allPersons.count { it.dataStatus == DataStatus.VERIFIED } }
    val needsReviewCount = remember(allPersons) { allPersons.count { it.dataStatus == DataStatus.NEEDS_REVIEW } }

    val needsReviewHouseholdsCount = remember(allHouseholdsWithPersons) {
        allHouseholdsWithPersons.count { it.household.dataStatus == DataStatus.NEEDS_REVIEW }
    }

    val seniorsCount = remember(allPersons) {
        allPersons.count { p ->
            val age = viewModel.calculateAge(p.birthDate, p.personStatus)
            (age ?: 0) >= 60
        }
    }
    val childrenCount = remember(allPersons) {
        allPersons.count { p ->
            val age = viewModel.calculateAge(p.birthDate, p.personStatus)
            age != null && age <= 5
        }
    }

    val gpsHouseholdsCount = remember(allHouseholdsWithPersons) {
        allHouseholdsWithPersons.count { it.household.latitude != null && it.household.longitude != null }
    }

    // Up to 5 most recently registered citizens paired with household info
    val recentCitizens = remember(allHouseholdsWithPersons) {
        allHouseholdsWithPersons.flatMap { hw ->
            hw.persons.map { person -> Pair(person, hw.household) }
        }.sortedByDescending { it.first.id }.take(4)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.HealthAndSafety,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "SMART OSM",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                            Text(
                                "ระบบสารสนเทศสุขภาพชุมชน",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        notificationTitle = "แจ้งเตือนงานสาธารณสุขชุมชน (อสม.)"
                        notificationMessage = "• สำรวจผู้สูงอายุติดบ้าน/ติดเตียงประจำเดือน\n• ตรวจคัดกรองโรคความดันโลหิตและเบาหวาน\n• บันทึกข้อมูลครัวเรือนที่ยังขาดพิกัด GPS"
                        showNotificationDialog = true
                    }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "การแจ้งเตือน", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToQrScan) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "สแกน QR Code", tint = Color.White)
                    }
                    ThemeQuickToggleButton(iconTint = Color.White)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Atmospheric Hero Card: Population & Community Overview
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = CardShadowTint
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(HeroGradientBrush)
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.wrapContentSize()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MintAccent)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "พื้นที่รับผิดชอบ อสม.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "ประชากรที่ลงทะเบียนแล้ว",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "$totalPersonsCount",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "คน",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Groups,
                                    contentDescription = null,
                                    tint = MintAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Secondary summary indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Home,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$totalHouseholdsCount ครัวเรือน",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MintAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "พิกัด GPS $gpsHouseholdsCount หลัง",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFFA7F3D0),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ตรวจแล้ว $verifiedCount คน",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Reusable BMI Calculator Component
            item {
                com.example.ui.components.BmiCalculatorCard()
            }

            // 3. Key App Functions (Quick Navigation Grid)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ฟังก์ชันหลักของระบบ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "เมนูด่วน",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 2x2 Bento Action Cards Grid
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionCard(
                                modifier = Modifier.weight(1f),
                                title = "สำรวจครัวเรือน",
                                subtitle = "ค้นหา & สมาชิก $totalHouseholdsCount หลัง",
                                icon = Icons.Filled.Home,
                                iconBgColor = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                iconTintColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                onClick = onNavigateToHouseholds
                            )
                            QuickActionCard(
                                modifier = Modifier.weight(1f),
                                title = "เพิ่มบ้านใหม่",
                                subtitle = "ลงทะเบียน + พิกัด GPS",
                                icon = Icons.Filled.AddHome,
                                iconBgColor = if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1),
                                iconTintColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488),
                                onClick = onNavigateToNewHousehold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionCard(
                                modifier = Modifier.weight(1f),
                                title = "แผนปฏิบัติงาน",
                                subtitle = "ตารางงาน อสม. & เยี่ยมบ้าน",
                                icon = Icons.Filled.FactCheck,
                                iconBgColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
                                iconTintColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                onClick = onNavigateToPlanOfWork
                            )
                            QuickActionCard(
                                modifier = Modifier.weight(1f),
                                title = "ข้อมูล อสม. & ตั้งค่า",
                                subtitle = "ข้อมูลอาสาสมัคร & ธีม",
                                icon = Icons.Filled.Settings,
                                iconBgColor = if (isDark) Color(0xFF3B0764) else Color(0xFFF3E8FF),
                                iconTintColor = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED),
                                onClick = onNavigateToInfo
                            )
                        }
                    }
                }
            }

            // 3. Registered Citizens Summary (Gender & Priority Target Groups)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "สรุปข้อมูลประชากร",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "กลุ่มเพศและกลุ่มเป้าหมาย",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 4-Card Demographic KPI Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CitizenStatCard(
                            modifier = Modifier.weight(1f),
                            title = "เพศชาย",
                            count = maleCount,
                            unit = "คน",
                            icon = Icons.Filled.Male,
                            tint = Color(0xFF0284C7),
                            bg = if (isDark) Color(0xFF0C3348) else Color(0xFFE0F2FE)
                        )
                        CitizenStatCard(
                            modifier = Modifier.weight(1f),
                            title = "เพศหญิง",
                            count = femaleCount,
                            unit = "คน",
                            icon = Icons.Filled.Female,
                            tint = Color(0xFFDB2777),
                            bg = if (isDark) Color(0xFF451528) else Color(0xFFFCE7F3)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CitizenStatCard(
                            modifier = Modifier.weight(1f),
                            title = "ผู้สูงอายุ (60+)",
                            count = seniorsCount,
                            unit = "คน",
                            badge = "เยี่ยมบ้าน",
                            icon = Icons.Filled.Elderly,
                            tint = Color(0xFFD97706),
                            bg = if (isDark) Color(0xFF452205) else Color(0xFFFEF3C7)
                        )
                        CitizenStatCard(
                            modifier = Modifier.weight(1f),
                            title = "เด็กปฐมวัย (0-5)",
                            count = childrenCount,
                            unit = "คน",
                            badge = "วัคซีน",
                            icon = Icons.Filled.ChildCare,
                            tint = Color(0xFF0D9488),
                            bg = if (isDark) Color(0xFF0F3836) else Color(0xFFCCFBF1)
                        )
                    }
                }
            }

            // 4. Vital Status & Quality Check
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(18.dp), spotColor = CardShadowTint),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "สถานะการมีชีวิต & ความถูกต้องข้อมูล",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusItemPill(
                                modifier = Modifier.weight(1f),
                                label = "มีชีวิต",
                                count = aliveCount,
                                color = if (isDark) StatusVerifiedFgDark else StatusVerifiedFg,
                                bgColor = if (isDark) StatusVerifiedBgDark else StatusVerifiedBg
                            )
                            StatusItemPill(
                                modifier = Modifier.weight(1f),
                                label = "เสียชีวิต",
                                count = deadCount,
                                color = if (isDark) StatusDeadFgDark else StatusDeadFg,
                                bgColor = if (isDark) StatusDeadBgDark else StatusDeadBg
                            )
                            StatusItemPill(
                                modifier = Modifier.weight(1f),
                                label = "ย้ายถิ่น",
                                count = movedCount,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                                bgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusItemPill(
                                modifier = Modifier.weight(1f),
                                label = "รอตรวจคน",
                                count = needsReviewCount,
                                color = if (isDark) StatusNeedsReviewFgDark else StatusNeedsReviewFg,
                                bgColor = if (isDark) StatusNeedsReviewBgDark else StatusNeedsReviewBg
                            )
                            StatusItemPill(
                                modifier = Modifier.weight(1f),
                                label = "รอตรวจบ้าน",
                                count = needsReviewHouseholdsCount,
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFFD97706),
                                bgColor = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
                            )
                        }

                        // Verification Progress Bar
                        val verifyPercent = if (totalPersonsCount > 0) (verifiedCount.toFloat() / totalPersonsCount.toFloat()) else 0f
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ความสมบูรณ์ของฐานข้อมูล",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(verifyPercent * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(verifyPercent.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(EmeraldPrimary)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Demographic Age Breakdown (Layered Card with Progress Meters)
            item {
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "โครงสร้างประชากรตามช่วงวัย",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ageGroupSummary.values.sum()} รายการ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = CardShadowTint
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (ageGroupSummary.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.Analytics,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "ยังไม่มีข้อมูลวันเกิดประชากรในระบบ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                val standardGroups = listOf(
                                    "0-5",
                                    "6-12",
                                    "13-17",
                                    "18-24",
                                    "25-39",
                                    "40-59",
                                    "60+"
                                )
                                val dataList = listOf(
                                    ageGroupSummary["เด็กเล็ก (0-5)"] ?: 0,
                                    ageGroupSummary["เด็กวัยเรียน (6-12)"] ?: 0,
                                    ageGroupSummary["วัยรุ่น (13-17)"] ?: 0,
                                    ageGroupSummary["วัยหนุ่มสาว (18-24)"] ?: 0,
                                    ageGroupSummary["วัยทำงานตอนต้น (25-39)"] ?: 0,
                                    ageGroupSummary["วัยทำงานตอนกลาง (40-59)"] ?: 0,
                                    ageGroupSummary["ผู้สูงอายุ (60+)"] ?: 0
                                )

                                val chartEntryModel = com.patrykandpatrick.vico.core.entry.entryModelOf(*dataList.map { it.toFloat() }.toTypedArray())

                                com.patrykandpatrick.vico.compose.chart.Chart(
                                    chart = com.patrykandpatrick.vico.compose.chart.column.columnChart(
                                        columns = listOf(com.patrykandpatrick.vico.compose.component.lineComponent(
                                            color = MaterialTheme.colorScheme.primary,
                                            thickness = 16.dp,
                                            shape = com.patrykandpatrick.vico.core.component.shape.Shapes.roundedCornerShape(topLeftPercent = 50, topRightPercent = 50)
                                        ))
                                    ),
                                    model = chartEntryModel,
                                    startAxis = com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis(
                                        valueFormatter = { value, _ -> value.toInt().toString() }
                                    ),
                                    bottomAxis = com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis(
                                        valueFormatter = { value, _ -> standardGroups.getOrNull(value.toInt()) ?: "" },
                                        labelRotationDegrees = -45f
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(220.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 6. Recently Registered Citizens
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ผู้ลงทะเบียนล่าสุด",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ดูทั้งหมด (${totalPersonsCount})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onNavigateToHouseholds() }
                                .padding(4.dp)
                        )
                    }

                    if (recentCitizens.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "ยังไม่มีข้อมูลประชากรที่ลงทะเบียน",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                FilledTonalButton(
                                    onClick = onNavigateToNewHousehold,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ลงทะเบียนครัวเรือนแรก")
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentCitizens.forEach { (person, household) ->
                                val age = viewModel.calculateAge(person.birthDate, person.personStatus)
                                val isHead = person.houseStatus == HouseholdRole.HEAD

                                Card(
                                    onClick = { onNavigateToHouseDetail(household.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = CardShadowTint),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (person.gender == Gender.MALE) (if (isDark) Color(0xFF0C3348) else Color(0xFFE0F2FE))
                                                    else (if (isDark) Color(0xFF451528) else Color(0xFFFCE7F3))
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = if (person.gender == Gender.MALE) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                                                else (if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = person.fullName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isHead) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (isDark) StatusVerifiedBgDark else StatusVerifiedBg
                                                    ) {
                                                        Text(
                                                            text = "เจ้าบ้าน",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isDark) StatusVerifiedFgDark else StatusVerifiedFg,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(3.dp))

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "บ้านเลขที่ ${household.houseNo}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = " • ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "อายุ: ${age ?: "-"} ปี",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "ดูรายละเอียด",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }
}

/**
 * High-touch Quick Action Card for key app navigation destinations
 */
@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(18.dp), spotColor = CardShadowTint),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTintColor, modifier = Modifier.size(24.dp))
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Compact Demographic KPI Card
 */
@Composable
fun CitizenStatCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    unit: String,
    badge: String? = null,
    icon: ImageVector,
    tint: Color,
    bg: Color
) {
    Card(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = CardShadowTint),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    badge?.let {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tint.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = tint,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact pill for citizen status
 */
@Composable
fun StatusItemPill(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = color
            )
        }
    }
}
