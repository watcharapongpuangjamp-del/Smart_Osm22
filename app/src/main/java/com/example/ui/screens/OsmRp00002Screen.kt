package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.vhv.VhvMemberEntity
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintAccent
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.VhvDirectoryViewModel

/**
 * Screen displaying the official VHV Directory Report OSMRP00002 for Tambon Pa Kha (ต.ป่าขะ อ.บ้านนา จ.นครนายก).
 * Source: ThaiPHC / กรมสนับสนุนบริการสุขภาพ กระทรวงสาธารณสุข (thaiphc.net).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsmRp00002Screen(
    vhvViewModel: VhvDirectoryViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onRegistrationCompleted: () -> Unit
) {
    val context = LocalContext.current
    val searchQuery by vhvViewModel.searchQuery.collectAsState()
    val selectedVillage by vhvViewModel.selectedVillage.collectAsState()
    val vhvMembers by vhvViewModel.vhvMembers.collectAsState()

    var selectedMemberForDetail by remember { mutableStateOf<VhvMemberEntity?>(null) }

    val villageList = remember {
        listOf(
            "ALL" to "ทั้งหมด (13 หมู่บ้าน)",
            "1" to "หมู่ 1 บ้านหนองเคี่ยม",
            "2" to "หมู่ 2 บ้านคลองผักหนาม",
            "3" to "หมู่ 3 บ้านป่าขะ",
            "4" to "หมู่ 4 บ้านท่ามะเฟือง",
            "5" to "หมู่ 5 บ้านโคกประเสริฐ",
            "6" to "หมู่ 6 บ้านหนองยาง",
            "7" to "หมู่ 7 บ้านกร่างประตูวัง",
            "8" to "หมู่ 8 บ้านคลองส่ง",
            "9" to "หมู่ 9 บ้านคลองกระโดน",
            "10" to "หมู่ 10 บ้านต้นกระบก",
            "11" to "หมู่ 11 บ้านดงขี้พุก",
            "12" to "หมู่ 12 บ้านทุ่งกระโปรง",
            "13" to "หมู่ 13 บ้านคลองนางหงษ์"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "รายงานข้อมูล อสม. ต.ป่าขะ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "รายงาน OSMRP00002 • thaiphc.net",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_osmrp_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "กลับ", tint = Color.White)
                    }
                },
                actions = {
                    ThemeQuickToggleButton(iconTint = Color.White)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Info Banner
            Surface(
                color = EmeraldPrimary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Dataset,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "ฐานข้อมูลรายชื่อ อสม. ตำบลป่าขะ (OSMRP00002)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Text(
                        text = "อ้างอิงจากรายงาน OSMRP00002 กรมสนับสนุนบริการสุขภาพ กระทรวงสาธารณสุข (thaiphc.net) • หน่วยบริการปฐมภูมิ รพ.สต.ป่าขะ อ.บ้านนา จ.นครนายก",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgeChip(
                            icon = Icons.Filled.Group,
                            text = "รวม ${vhvMembers.size} ท่าน"
                        )
                        BadgeChip(
                            icon = Icons.Filled.LocationOn,
                            text = "ต.ป่าขะ 13 หมู่บ้าน"
                        )
                    }
                }
            }

            // Search and Village Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { vhvViewModel.setSearchQuery(it) },
                    placeholder = { Text("ค้นหาชื่อ, เลขประจำตัว อสม., เลขประชาชน 13 หลัก...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vhvViewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "ล้างการค้นหา")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_osmrp"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )

                // Filter Village Horizontal Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(villageList) { (no, name) ->
                        val isSelected = selectedVillage == no
                        FilterChip(
                            selected = isSelected,
                            onClick = { vhvViewModel.setSelectedVillage(no) },
                            label = { Text(name, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // VHV Member Cards List
            if (vhvMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "ไม่พบรายชื่อ อสม. ตรงตามเงื่อนไขที่ค้นหา",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { vhvViewModel.resetFilters() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("แสดงทั้งหมด")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vhvMembers, key = { it.vhvCardId }) { vhv ->
                        VhvMemberCard(
                            vhv = vhv,
                            onCardClick = { selectedMemberForDetail = vhv },
                            onCallClick = {
                                if (vhv.phone.isNotBlank()) {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${vhv.phone}"))
                                    context.startActivity(dialIntent)
                                } else {
                                    Toast.makeText(context, "ไม่มีเบอร์โทรศัพท์", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSelectAsMeClick = {
                                authViewModel.saveSurveyorProfile(
                                    context = context,
                                    fullName = vhv.fullName,
                                    villageNo = vhv.villageNo,
                                    villageName = vhv.villageName,
                                    subdistrict = vhv.subdistrict,
                                    district = vhv.district,
                                    province = vhv.province,
                                    phone = vhv.phone,
                                    role = vhv.roleTitle,
                                    vhvCardId = vhv.vhvCardId,
                                    citizenId = vhv.nationalId,
                                    healthCenter = vhv.healthCenter
                                )
                                Toast.makeText(context, "บันทึกตัวตน ${vhv.fullName} เรียบร้อยแล้ว!", Toast.LENGTH_LONG).show()
                                onRegistrationCompleted()
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedMemberForDetail?.let { vhv ->
        AlertDialog(
            onDismissRequest = { selectedMemberForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Badge, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(vhv.fullName, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailTextRow("เลขประจำตัว อสม.:", vhv.vhvCardId)
                    DetailTextRow("เลขประจำตัวประชาชน:", vhv.nationalId)
                    DetailTextRow("ตำแหน่ง:", vhv.roleTitle)
                    DetailTextRow("พื้นที่รับผิดชอบ:", vhv.villageName)
                    DetailTextRow("หน่วยบริการ:", vhv.healthCenter)
                    DetailTextRow("เบอร์โทรศัพท์:", if (vhv.phone.isNotBlank()) vhv.phone else "ไม่ระบุ")
                    DetailTextRow("จำนวนหลังคาเรือนที่ดูแล:", "${vhv.assignedHouseholdsCount} หลังคาเรือน")
                    DetailTextRow("สถานะ:", vhv.status)
                    DetailTextRow("แหล่งที่มาข้อมูล:", vhv.reportSource)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.saveSurveyorProfile(
                            context = context,
                            fullName = vhv.fullName,
                            villageNo = vhv.villageNo,
                            villageName = vhv.villageName,
                            subdistrict = vhv.subdistrict,
                            district = vhv.district,
                            province = vhv.province,
                            phone = vhv.phone,
                            role = vhv.roleTitle,
                            vhvCardId = vhv.vhvCardId,
                            citizenId = vhv.nationalId,
                            healthCenter = vhv.healthCenter
                        )
                        selectedMemberForDetail = null
                        Toast.makeText(context, "ใช้ข้อมูล ${vhv.fullName} ลงทะเบียน อสม. เรียบร้อยแล้ว", Toast.LENGTH_LONG).show()
                        onRegistrationCompleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Filled.HowToReg, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ใช้ข้อมูลนี้ลงทะเบียน")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMemberForDetail = null }) {
                    Text("ปิด")
                }
            }
        )
    }
}

@Composable
private fun VhvMemberCard(
    vhv: VhvMemberEntity,
    onCardClick: () -> Unit,
    onCallClick: () -> Unit,
    onSelectAsMeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EmeraldPrimary, MintAccent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = vhv.fullName.takeLast(2),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Column {
                        Text(
                            text = vhv.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "เลขบัตร อสม.: ${vhv.vhvCardId}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = EmeraldPrimary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "หมู่ ${vhv.villageNo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${vhv.roleTitle} • ${vhv.villageName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "ดูแล ${vhv.assignedHouseholdsCount} หลังคาเรือน | ${vhv.healthCenter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (vhv.phone.isNotBlank()) {
                    OutlinedButton(
                        onClick = onCallClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("โทร", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onSelectAsMeClick,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("เลือกคนนี้เป็น อสม.", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = Color.White.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DetailTextRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
