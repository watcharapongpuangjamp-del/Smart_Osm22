package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ThemeQuickToggleButton
import com.example.viewmodel.AuthViewModel

private val EmeraldPrimary = Color(0xFF0F766E)
private val TealAccent = Color(0xFF0D9488)

/**
 * Enhanced, complete registration screen for Village Health Volunteers (VHV / อสม.).
 *
 * Implements the system's "Complete Registration" flow, ensuring every VHV 
 * is correctly linked to their identity (user.uid) and area (villageId).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VhvRegistrationScreen(
    authViewModel: AuthViewModel,
    onRegistrationSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val userProfile by authViewModel.userProfile.collectAsState()

    // Form States
    var fullName by remember { mutableStateOf(userProfile?.displayName ?: "") }
    var phoneNumber by remember { mutableStateOf(userProfile?.phoneNumber ?: "") }
    var villageNo by remember { mutableStateOf(userProfile?.villageNo ?: "7") }
    var villageName by remember { mutableStateOf(userProfile?.villageName ?: "หมู่ 7 บ้านกร่างประตูวัง") }
    var subdistrict by remember { mutableStateOf(userProfile?.subdistrict ?: "ต.ป่าขะ") }
    var district by remember { mutableStateOf(userProfile?.district ?: "อ.บ้านนา") }
    var province by remember { mutableStateOf(userProfile?.province ?: "จ.นครนายก") }
    var roleTitle by remember { mutableStateOf(userProfile?.roleTitle ?: "อสม. ประจำหมู่บ้าน") }

    var isSubmitting by remember { mutableStateOf(false) }

    // Village Options (Real list for Subdistrict Pa Kha, District Ban Na, Province Nakhon Nayok)
    val villageOptions = listOf(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ลงทะเบียนสมาชิก อสม. ใหม่",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_vhv_reg_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "กลับ")
                    }
                },
                actions = {
                    ThemeQuickToggleButton()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Image/Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EmeraldPrimary, TealAccent)
                            )
                        )
                        .shadow(8.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Text(
                    text = "ข้อมูลสมาชิก อสม. ประจำพื้นที่",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "กรุณากรอกข้อมูลส่วนตัวและพื้นที่รับผิดชอบให้ครบถ้วนเพื่อประสิทธิภาพในการสำรวจและซิงค์ข้อมูล",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Form Sections
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SectionHeader(icon = Icons.Filled.Person, title = "ข้อมูลส่วนบุคคล")

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("ชื่อ-นามสกุล") },
                            placeholder = { Text("เช่น นายสมชาย ใจดี") },
                            modifier = Modifier.fillMaxWidth().testTag("input_vhv_name"),
                            leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { if (it.length <= 10) phoneNumber = it },
                            label = { Text("เบอร์โทรศัพท์") },
                            placeholder = { Text("เช่น 0812345678") },
                            modifier = Modifier.fillMaxWidth().testTag("input_vhv_phone"),
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = roleTitle,
                            onValueChange = { roleTitle = it },
                            label = { Text("ตำแหน่ง / บทบาท") },
                            modifier = Modifier.fillMaxWidth().testTag("input_vhv_role"),
                            leadingIcon = { Icon(Icons.Filled.Work, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        SectionHeader(icon = Icons.Filled.Place, title = "พื้นที่รับผิดชอบ (Village Area)")

                        // Village Selector (Simplified Dropdown simulation)
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = villageName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("หมู่บ้าน / พื้นที่") },
                                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("input_vhv_village"),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                villageOptions.forEach { (no, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            villageNo = no
                                            villageName = name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = subdistrict,
                                onValueChange = { subdistrict = it },
                                label = { Text("ตำบล") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = district,
                                onValueChange = { district = it },
                                label = { Text("อำเภอ") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = province,
                            onValueChange = { province = it },
                            label = { Text("จังหวัด") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        if (fullName.isBlank()) {
                            Toast.makeText(context, "กรุณากรอกชื่อ-นามสกุล", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        authViewModel.saveSurveyorProfile(
                            context = context,
                            villageNo = villageNo,
                            villageName = villageName,
                            subdistrict = subdistrict,
                            district = district,
                            province = province,
                            phone = phoneNumber,
                            role = roleTitle
                        )
                        
                        // Small delay for UX feel
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            isSubmitting = false
                            Toast.makeText(context, "ลงทะเบียนสมาชิก อสม. สำเร็จ", Toast.LENGTH_LONG).show()
                            onRegistrationSuccess()
                        }, 800)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .testTag("btn_vhv_reg_submit"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.HowToReg, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "บันทึกและเริ่มต้นการใช้งาน",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EmeraldPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )
    }
}
