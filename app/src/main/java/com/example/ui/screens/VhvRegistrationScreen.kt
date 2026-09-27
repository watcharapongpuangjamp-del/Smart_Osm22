package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.ThemeQuickToggleButton
import com.example.viewmodel.AuthViewModel

private val EmeraldPrimary = Color(0xFF0F766E)
private val TealAccent = Color(0xFF0D9488)

/**
 * Enhanced, flexible Registration & Profile Management Screen for Village Health Volunteers (VHV / อสม.).
 *
 * Supports multiple registration modes:
 * 1) Direct Form Input (Name, VHV Card ID, Citizen ID, Health Center, Phone, Role, Area).
 * 2) Scan / Upload VHV Card Photo (รูปบัตร อสม.) with instant AI Auto-Scan/OCR fill.
 * 3) Upload Custom Profile Picture (รูปโปรไฟล์ อสม.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VhvRegistrationScreen(
    authViewModel: AuthViewModel,
    onRegistrationSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToOsmRp00002: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val saveScope = rememberCoroutineScope()
    val userProfile by authViewModel.userProfile.collectAsState()
    val villageAssignment by authViewModel.villageAssignment.collectAsState()
    val villageAssignmentLoading by authViewModel.villageAssignmentLoading.collectAsState()
    val villageAssignmentError by authViewModel.villageAssignmentError.collectAsState()

    // Form States initialized with current registered values or defaults
    var fullName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var vhvCardId by remember(userProfile) { mutableStateOf(userProfile?.vhvCardId ?: "") }
    var citizenId by remember(userProfile) { mutableStateOf(userProfile?.citizenId ?: "") }
    var healthCenter by remember(userProfile) { mutableStateOf(userProfile?.healthCenter ?: "") }
    var phoneNumber by remember(userProfile) { mutableStateOf(userProfile?.phoneNumber ?: "") }
    var villageNo by remember(userProfile) { mutableStateOf(userProfile?.villageNo ?: "") }
    var villageName by remember(userProfile) { mutableStateOf(userProfile?.villageName ?: "") }
    var subdistrict by remember(userProfile) { mutableStateOf(userProfile?.subdistrict ?: "") }
    var district by remember(userProfile) { mutableStateOf(userProfile?.district ?: "") }
    var province by remember(userProfile) { mutableStateOf(userProfile?.province ?: "") }
    var roleTitle by remember(userProfile) { mutableStateOf(userProfile?.roleTitle ?: "อสม. ประจำหมู่บ้าน") }
    var photoUrlUri by remember(userProfile) { mutableStateOf<Uri?>(userProfile?.photoUrl?.let { Uri.parse(it) }) }
    var cardPhotoUri by remember(userProfile) { mutableStateOf<Uri?>(userProfile?.vhvCardPhotoUrl?.let { Uri.parse(it) }) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Form, 1 = Card Scan, 2 = Profile Photo
    var isSubmitting by remember { mutableStateOf(false) }
    var isScanningCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.loadVillageAssignment()
    }

    LaunchedEffect(villageAssignment) {
        villageAssignment?.let { assignment ->
            villageNo = assignment.villageNo
            villageName = assignment.villageName.ifBlank { "หมู่ ${assignment.villageNo}" }
            if (assignment.subdistrict.isNotBlank()) subdistrict = assignment.subdistrict
            if (assignment.district.isNotBlank()) district = assignment.district
            if (assignment.province.isNotBlank()) province = assignment.province
        }
    }


    // Image Picker for Profile Photo
    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            photoUrlUri = it
            Toast.makeText(context, "เลือกรูปโปรไฟล์เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
        }
    }

    // Image Picker for VHV Card Photo
    val cardPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            cardPhotoUri = it
            Toast.makeText(context, "อัปโหลดรูปบัตร อสม. เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ลงทะเบียนสมาชิก อสม. / แก้ไขโปรไฟล์",
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profile Badge / Avatar Preview
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EmeraldPrimary, TealAccent)
                            )
                        )
                        .clickable { profilePhotoPicker.launch("image/*") }
                        .shadow(8.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrlUri != null) {
                        AsyncImage(
                            model = photoUrlUri,
                            contentDescription = "รูปโปรไฟล์ อสม.",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    // Camera Edit Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "เปลี่ยนรูปโปรไฟล์",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = if (fullName.isNotBlank()) fullName else "ลงทะเบียน อสม. ใหม่",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "ระบบรองรับการลงทะเบียน 3 รูปแบบ: กรอกข้อมูลตรง, สแกนรูปบัตร อสม. และเลือกรูปโปรไฟล์",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // OSMRP00002 Import Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToOsmRp00002),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.Dataset,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "เลือกข้อมูลจากไดเรกทอรี อสม. ปัจจุบัน",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "ข้อมูลต้องมาจากฐานข้อมูล อสม. ปัจจุบันที่ตรวจสอบแล้ว",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = EmeraldPrimary)
                    }
                }

                // Registration Mode Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = EmeraldPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("1. กรอกข้อมูล", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("2. สแกนบัตร อสม.", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("3. รูปโปรไฟล์", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                // Tab Content Switcher
                when (selectedTab) {
                    0 -> {
                        // TAB 1: FORM INPUT
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                SectionHeader(icon = Icons.Filled.Badge, title = "ข้อมูลบัตรประจำตัว อสม.")

                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("ชื่อ-นามสกุล อสม.") },
                                    placeholder = { Text("เช่น นายสมชาย ใจดี") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_vhv_name"),
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = vhvCardId,
                                    onValueChange = { vhvCardId = it },
                                    label = { Text("เลขประจำตัว / รหัสบัตร อสม.") },
                                    placeholder = { Text("เช่น 1-2602-00888-00-1") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_vhv_card_id"),
                                    leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = citizenId,
                                    onValueChange = { if (it.length <= 13) citizenId = it },
                                    label = { Text("เลขประจำตัวประชาชน (13 หลัก)") },
                                    placeholder = { Text("เช่น 1260200888123") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_vhv_citizen_id"),
                                    leadingIcon = { Icon(Icons.Filled.Fingerprint, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = healthCenter,
                                    onValueChange = { healthCenter = it },
                                    label = { Text("หน่วยบริการปฐมภูมิ / รพ.สต.") },
                                    placeholder = { Text("เช่น รพ.สต.ป่าขะ") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_vhv_health_center"),
                                    leadingIcon = { Icon(Icons.Filled.LocalHospital, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { if (it.length <= 10) phoneNumber = it },
                                    label = { Text("เบอร์โทรศัพท์ติดต่อ") },
                                    placeholder = { Text("เช่น 0812345678") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_vhv_phone"),
                                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
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

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                SectionHeader(icon = Icons.Filled.Place, title = "พื้นที่รับผิดชอบ (Village Area)")

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "พื้นที่รับผิดชอบที่ได้รับอนุมัติ",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (villageAssignmentLoading) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("กำลังตรวจสอบพื้นที่รับผิดชอบจากระบบ...")
                                            }
                                        } else if (villageAssignment != null) {
                                            Text(
                                                text = villageName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "หมู่ที่ ${villageNo}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        } else {
                                            Text(
                                                text = villageAssignmentError
                                                    ?: "ยังไม่ได้รับการกำหนดพื้นที่รับผิดชอบ",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedButton(
                                                onClick = { authViewModel.loadVillageAssignment() }
                                            ) {
                                                Text("ตรวจสอบอีกครั้ง")
                                            }
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
                    }

                    1 -> {
                        // TAB 2: VHV CARD SCAN / PHOTO UPLOAD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                SectionHeader(icon = Icons.Filled.DocumentScanner, title = "สแกนหรือถ่ายรูปบัตร อสม.")

                                Text(
                                    text = "สามารถถ่ายรูปหรืออัปโหลดภาพถ่ายบัตรประจำตัว อสม. เพื่อให้ระบบดึงข้อมูลเข้าสู่ฟอร์มโดยอัตโนมัติ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                // Card Image Preview Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(2.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                        .clickable { cardPhotoPicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cardPhotoUri != null) {
                                        AsyncImage(
                                            model = cardPhotoUri,
                                            contentDescription = "รูปบัตร อสม.",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Badge,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Text(
                                                "แตะเพื่อเลือกรูปบัตร อสม.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { cardPhotoPicker.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("เลือกรูปบัตร")
                                    }

                                    Button(
                                        onClick = {
                                            isScanningCard = false
                                            Toast.makeText(
                                                context,
                                                "ระบบสแกน AI ยังไม่เชื่อม OCR จริง กรุณากรอกข้อมูลจากบัตร อสม. ด้วยตนเอง",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isScanningCard
                                    ) {
                                        if (isScanningCard) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                        } else {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("สแกน AI")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 3: PROFILE PHOTO UPLOAD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                SectionHeader(icon = Icons.Filled.AddPhotoAlternate, title = "เลือกรูปโปรไฟล์ อสม.")

                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(3.dp, EmeraldPrimary, CircleShape)
                                        .clickable { profilePhotoPicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (photoUrlUri != null) {
                                        AsyncImage(
                                            model = photoUrlUri,
                                            contentDescription = "รูปโปรไฟล์ อสม.",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.PersonAdd,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(60.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { profilePhotoPicker.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("เลือกรูปโปรไฟล์จากคลังภาพ")
                                }
                            }
                        }
                    }
                }

                // Main Save & Complete Button
                Button(
                    onClick = {
                        if (fullName.isBlank()) {
                            Toast.makeText(context, "กรุณากรอกชื่อ-นามสกุล อสม.", Toast.LENGTH_SHORT).show()
                            selectedTab = 0
                            return@Button
                        }
                        val assignment = villageAssignment
                        if (assignment == null) {
                            Toast.makeText(
                                context,
                                villageAssignmentError ?: "ยังไม่ได้รับการกำหนดพื้นที่รับผิดชอบ ไม่สามารถบันทึกข้อมูลได้",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        isSubmitting = true
                        saveScope.launch {
                            val result = authViewModel.saveSurveyorProfile(
                                context = context,
                                fullName = fullName,
                                villageNo = assignment.villageNo,
                                villageName = assignment.villageName.ifBlank { "หมู่ ${assignment.villageNo}" },
                                subdistrict = assignment.subdistrict.ifBlank { subdistrict },
                                district = assignment.district.ifBlank { district },
                                province = assignment.province.ifBlank { province },
                                phone = phoneNumber,
                                role = roleTitle,
                                vhvCardId = vhvCardId,
                                citizenId = citizenId,
                                healthCenter = healthCenter,
                                photoUrl = photoUrlUri?.toString(),
                                vhvCardPhotoUrl = cardPhotoUri?.toString()
                            )
                            isSubmitting = false

                            result.fold(
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "บันทึกข้อมูลสมาชิก อสม. เรียบร้อยแล้ว",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onRegistrationSuccess()
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "ไม่สามารถบันทึกข้อมูลสมาชิก อสม. ได้",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .testTag("btn_vhv_reg_submit"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "บันทึกข้อมูลสมาชิก อสม.",
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
