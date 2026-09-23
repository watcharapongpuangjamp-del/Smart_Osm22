package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Person
import com.example.data.DataStatus
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.theme.*
import com.example.viewmodel.PersonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseDetailScreen(
    viewModel: PersonViewModel,
    householdId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToMap: (Long) -> Unit = {},
    onAddMemberClick: () -> Unit,
    onEditMemberClick: (Long) -> Unit,
    onHistoryClick: (Long) -> Unit,
    onScreeningClick: (Long) -> Unit = {}
) {
    val householdWithPersons by viewModel.getHouseholdWithPersonsById(householdId).collectAsStateWithLifecycle(initialValue = null)
    
    var personToDelete by remember { mutableStateOf<Person?>(null) }
    var showDeleteHouseholdDialog by remember { mutableStateOf(false) }
    var deleteErrorMsg by remember { mutableStateOf<String?>(null) }

    if (deleteErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { deleteErrorMsg = null },
            title = { Text("เกิดข้อผิดพลาด") },
            text = { Text(deleteErrorMsg ?: "") },
            confirmButton = {
                Button(onClick = { deleteErrorMsg = null }) { Text("ตกลง") }
            }
        )
    }

    if (personToDelete != null) {
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text("ยืนยันการลบข้อมูล") },
            text = { Text("คุณต้องการลบข้อมูลของ \"${personToDelete?.fullName}\" ออกจากทะเบียนครัวเรือนหรือไม่?") },
            confirmButton = {
                Button(
                    onClick = {
                        personToDelete?.let { 
                            viewModel.delete(it) { success, errorMsg -> 
                                if (!success && errorMsg != null) {
                                    deleteErrorMsg = errorMsg
                                }
                            }
                        }
                        personToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("ลบข้อมูล") }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) { Text("ยกเลิก") }
            }
        )
    }

    if (showDeleteHouseholdDialog && householdWithPersons != null) {
        val house = householdWithPersons!!.household
        val membersCount = householdWithPersons!!.persons.size
        AlertDialog(
            onDismissRequest = { showDeleteHouseholdDialog = false },
            title = { Text("ยืนยันการลบบ้านเลขที่ ${house.houseNo}") },
            text = { Text("การลบบ้านหลังนี้จะลบข้อมูลสมาชิกในบ้านทั้งหมด ($membersCount คน) ตามระบบ CASCADE อย่างถาวร แน่ใจหรือไม่?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHousehold(house) { success, errorMsg ->
                            showDeleteHouseholdDialog = false
                            if (success) {
                                onNavigateBack()
                            } else {
                                deleteErrorMsg = errorMsg ?: "ไม่สามารถลบบ้านได้"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("ลบบ้านและสมาชิก") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteHouseholdDialog = false }) { Text("ยกเลิก") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "บ้านเลขที่ ${householdWithPersons?.household?.houseNo ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ย้อนกลับ", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteHouseholdDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "ลบบ้าน", tint = Color.White)
                    }
                    ThemeQuickToggleButton(iconTint = Color.White)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMemberClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("เพิ่มสมาชิก", fontWeight = FontWeight.Bold) },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp), spotColor = CardShadowTint)
            )
        }
    ) { padding ->
        if (householdWithPersons == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
            return@Scaffold
        }
        
        val houseMembers = householdWithPersons!!.persons
        val household = householdWithPersons!!.household

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Household Summary Hero Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = CardShadowTint)
                        .clip(RoundedCornerShape(22.dp))
                        .background(HeroGradientBrush)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "ครัวเรือนในเขตความรับผิดชอบ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "บ้านเลขที่ ${household.houseNo}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Surface(
                                        color = if (household.dataStatus == DataStatus.VERIFIED) 
                                            Color.White.copy(alpha = 0.2f) 
                                        else 
                                            Color(0xFFFF5252).copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable {
                                            val newStatus = if (household.dataStatus == DataStatus.VERIFIED) 
                                                DataStatus.NEEDS_REVIEW 
                                            else 
                                                DataStatus.VERIFIED
                                            viewModel.updateHouseholdDataStatus(household, newStatus)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (household.dataStatus == DataStatus.VERIFIED) 
                                                    Icons.Filled.CheckCircle 
                                                else 
                                                    Icons.Filled.Error,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                household.dataStatus.value,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Home, contentDescription = null, tint = MintAccent, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("สมาชิกทั้งหมด", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${houseMembers.size} คน", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("เจ้าบ้าน", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(2.dp))
                                val headCount = houseMembers.count { it.houseStatus == com.example.data.HouseholdRole.HEAD }
                                Text("$headCount คน", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MintAccent)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        if (household.latitude != null && household.longitude != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable { onNavigateToMap(household.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MintAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "พิกัด GPS: ${household.latitude}, ${household.longitude}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                                Text("ดูบนแผนที่ ›", style = MaterialTheme.typography.labelSmall, color = MintAccent, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable { onNavigateToMap(household.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "ยังไม่ได้ปักหมุดพิกัด",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Text("ปักหมุดบนแผนที่ ›", style = MaterialTheme.typography.labelSmall, color = MintAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "รายชื่อสมาชิกในบ้าน",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${houseMembers.size} ท่าน",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (houseMembers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("ยังไม่มีข้อมูลสมาชิกในบ้านหลังนี้", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("แตะปุ่ม \"เพิ่มสมาชิก\" ด้านล่างเพื่อเริ่มบันทึก", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            } else {
                items(houseMembers, key = { it.id }) { person ->
                    val age = viewModel.calculateAge(person.birthDate, person.personStatus)
                    val isHead = person.houseStatus == com.example.data.HouseholdRole.HEAD
                    val isDead = person.personStatus == com.example.data.PersonStatus.DEAD
                    val isDarkTheme = isSystemInDarkTheme()

                    val verifiedBg = if (isDarkTheme) StatusVerifiedBgDark else StatusVerifiedBg
                    val verifiedFg = if (isDarkTheme) StatusVerifiedFgDark else StatusVerifiedFg
                    val deadBg = if (isDarkTheme) StatusDeadBgDark else StatusDeadBg
                    val deadFg = if (isDarkTheme) StatusDeadFgDark else StatusDeadFg
                    val reviewBg = if (isDarkTheme) StatusNeedsReviewBgDark else StatusNeedsReviewBg
                    val reviewFg = if (isDarkTheme) StatusNeedsReviewFgDark else StatusNeedsReviewFg

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(18.dp), spotColor = CardShadowTint),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isHead) 1.5.dp else 1.dp,
                            color = if (isHead) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Badge
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDead) deadBg
                                        else if (person.gender == com.example.data.Gender.MALE) (if (isDarkTheme) Color(0xFF0C3348) else Color(0xFFE0F2FE))
                                        else (if (isDarkTheme) Color(0xFF451528) else Color(0xFFFCE7F3))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = if (isDead) deadFg
                                    else if (person.gender == com.example.data.Gender.MALE) (if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7))
                                    else (if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFDB2777)),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            // Details Column
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = person.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isHead) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Filled.Star, contentDescription = "เจ้าบ้าน", tint = GoldenAmber, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "อายุ: ${age ?: "-"} ปี | ปชช: ${person.nationalId ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ModernChip(
                                        text = person.houseStatus.value,
                                        bg = if (isHead) verifiedBg else MaterialTheme.colorScheme.surfaceVariant,
                                        fg = if (isHead) verifiedFg else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isDead) {
                                        ModernChip(text = "เสียชีวิต", bg = deadBg, fg = deadFg)
                                    }
                                    if (person.dataStatus == com.example.data.DataStatus.NEEDS_REVIEW) {
                                        ModernChip(text = "รอตรวจสอบ", bg = reviewBg, fg = reviewFg)
                                    }
                                }
                            }
                            
                            // Actions
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                IconButton(
                                    onClick = { onScreeningClick(person.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.Badge, contentDescription = "คัดกรองสุขภาพ", tint = GoldenAmber, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onHistoryClick(person.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.History, contentDescription = "ประวัติแก้ไข", tint = TealSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onEditMemberClick(person.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "แก้ไข", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { personToDelete = person },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "ลบ", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ModernChip(text: String, bg: Color, fg: Color) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bg,
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

