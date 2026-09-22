package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.example.data.auth.UserProfile
import com.google.firebase.auth.FirebaseUser
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintAccent
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.AuthViewModel

/**
 * Standard, clean login screen for Smart OSM.
 *
 * Implements a world-class, human-centric Google Sign-In experience
 * via Android Credential Manager, with seamless offline local-first mode
 * and surveyor onboarding (area/village assignment).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onContinueOffline: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    val prefs = remember { context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) }

    // Area onboarding dialog for first-time surveyors
    var showAreaSetupDialog by remember { mutableStateOf(false) }
    var surveyorNameInput by remember { mutableStateOf("") }
    var surveyorPhoneInput by remember { mutableStateOf("") }
    var selectedVillageNo by remember { mutableStateOf("8") }
    var selectedVillageName by remember { mutableStateOf("หมู่ 8 บ้านกร่างประดู่วัง") }

    // Missing Web Client ID setup dialog (fallback only if config is absent)
    var showMissingClientIdDialog by remember { mutableStateOf(false) }
    var inputClientId by remember { mutableStateOf(prefs.getString("web_client_id", "") ?: "") }

    // Secondary email auth accordion
    var showEmailAuthOptions by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedAuthTab by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Register

    // Load surveyor profile on initial render
    LaunchedEffect(Unit) {
        authViewModel.loadSurveyorProfile(context)
    }

    // React to auth success or errors
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                val isSetupDone = prefs.getBoolean("surveyor_setup_completed", false)
                if (!isSetupDone) {
                    val userDisplayName = when (val u = state.user) {
                        is FirebaseUser -> u.displayName
                        is UserProfile -> u.displayName
                        else -> null
                    }
                    val userPhone = when (val u = state.user) {
                        is FirebaseUser -> u.phoneNumber
                        is UserProfile -> u.phoneNumber
                        else -> null
                    }
                    surveyorNameInput = userDisplayName ?: ""
                    surveyorPhoneInput = userPhone ?: ""
                    showAreaSetupDialog = true
                } else {
                    onLoginSuccess()
                }
            }
            is AuthUiState.Error -> {
                if (state.message == "MISSING_WEB_CLIENT_ID") {
                    showMissingClientIdDialog = true
                }
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "เข้าสู่ระบบ Smart OSM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_login_back")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "กลับ"
                            )
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Elegant Brand Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.HealthAndSafety,
                            contentDescription = "Smart OSM Logo",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Smart OSM",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "ระบบสารสนเทศและการสำรวจสุขภาพชุมชน",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "รพ.สต.บ้านกร่างประดู่วัง • อ.บ้านนา จ.นครนายก",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. State Feedback Banners (Clean, Non-Intrusive)
            when (val state = uiState) {
                is AuthUiState.Loading -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("banner_loading"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                is AuthUiState.Error -> {
                    if (state.message != "MISSING_WEB_CLIENT_ID") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("banner_error"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                IconButton(onClick = { authViewModel.resetState() }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "ปิด",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }

            // 3. Primary Card: Either Active Session OR Standard Sign-In
            val activeUser = currentUser
            if (activeUser != null) {
                // Active Session Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_user_profile"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar & Name
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f))
                                .border(2.dp, EmeraldPrimary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val photoUrl = activeUser.photoUrl?.toString()
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "รูปโปรไฟล์",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initial = activeUser.displayName?.firstOrNull()?.uppercase()
                                    ?: activeUser.email?.firstOrNull()?.uppercase()
                                if (initial != null) {
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = activeUser.displayName?.takeIf { it.isNotBlank() }
                                    ?: userProfile?.safeDisplayName
                                    ?: "ผู้สำรวจ Smart OSM",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            if (!activeUser.email.isNullOrBlank()) {
                                Text(
                                    text = activeUser.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Area & Role Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "พื้นที่รับผิดชอบ:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = userProfile?.villageName ?: "หมู่ 8 บ้านกร่างประดู่วัง",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Continue Button
                        Button(
                            onClick = onLoginSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_enter_app"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text(
                                text = "เข้าสู่ระบบสำรวจ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        // Switch Account / Sign out Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    authViewModel.signOut()
                                    authViewModel.signInWithGoogle(context)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_switch_account"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("สลับบัญชี", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { authViewModel.signOut() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_sign_out"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ออกจากระบบ", fontSize = 13.sp)
                            }
                        }

                        if (onNavigateToProfile != null) {
                            TextButton(
                                onClick = onNavigateToProfile,
                                modifier = Modifier.testTag("btn_login_view_profile")
                            ) {
                                Text(
                                    "ดูข้อมูลโปรไฟล์และสิทธิ์ผู้ใช้งาน",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            } else {
                // Standard Sign-In Card (Google-First)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ลงชื่อเข้าใช้ระบบ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "เข้าสู่ระบบด้วยบัญชี Google เพื่อเชื่อมโยงสิทธิ์ผู้สำรวจและซิงค์ข้อมูลลงพื้นที่ได้อย่างปลอดภัย",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // OFFICIAL GOOGLE SIGN-IN BUTTON
                        val isGoogleLoading = uiState is AuthUiState.Loading &&
                                (uiState as AuthUiState.Loading).message.contains("Google", ignoreCase = true)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !isGoogleLoading) {
                                    authViewModel.signInWithGoogle(context)
                                }
                                .testTag("btn_google_sign_in")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isGoogleLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                        color = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "กำลังเชื่อมต่อ Google...",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    GoogleLogoIcon(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = "ลงชื่อเข้าใช้ด้วย Google",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Divider with "หรือ"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text(
                                text = "หรือ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        // Offline Mode Button
                        OutlinedButton(
                            onClick = onContinueOffline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_continue_local"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Icon(
                                Icons.Filled.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "เข้าใช้งานแบบออฟไลน์ (ในเครื่อง)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        // One-click Test Surveyor Login (gigatvthai@gmail.com)
                        TextButton(
                            onClick = {
                                authViewModel.signInWithGoogleTest(context, "gigatvthai@gmail.com")
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_quick_test_login")
                        ) {
                            Icon(
                                Icons.Filled.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "เข้าสู่ระบบด่วนด้วยบัญชีผู้สำรวจ (gigatvthai@gmail.com)",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Collapsible text button for Secondary Email / Guest Auth
                        TextButton(
                            onClick = { showEmailAuthOptions = !showEmailAuthOptions }
                        ) {
                            Text(
                                text = if (showEmailAuthOptions) "ซ่อนตัวเลือกเพิ่มเติม" else "ตัวเลือกเพิ่มเติม (อีเมล / โหมดชั่วคราว)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                if (showEmailAuthOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Smoothly expand Email & Anonymous auth
                        AnimatedVisibility(visible = showEmailAuthOptions) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TabRow(
                                    selectedTabIndex = selectedAuthTab,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                ) {
                                    Tab(
                                        selected = selectedAuthTab == 0,
                                        onClick = { selectedAuthTab = 0 },
                                        text = { Text("เข้าสู่ระบบ", fontWeight = FontWeight.Bold) }
                                    )
                                    Tab(
                                        selected = selectedAuthTab == 1,
                                        onClick = { selectedAuthTab = 1 },
                                        text = { Text("ลงทะเบียนใหม่", fontWeight = FontWeight.Bold) }
                                    )
                                }

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("อีเมล") },
                                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_email"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("รหัสผ่าน") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (passwordVisible) "ซ่อนรหัสผ่าน" else "แสดงรหัสผ่าน"
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            if (selectedAuthTab == 0) {
                                                authViewModel.signInWithEmail(email, password)
                                            } else {
                                                authViewModel.signUpWithEmail(email, password)
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_password"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (selectedAuthTab == 0) {
                                            authViewModel.signInWithEmail(email, password)
                                        } else {
                                            authViewModel.signUpWithEmail(email, password)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_email_action"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Icon(
                                        if (selectedAuthTab == 0) Icons.AutoMirrored.Filled.Login else Icons.Filled.PersonAdd,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (selectedAuthTab == 0) "เข้าสู่ระบบด้วยอีเมล" else "ลงทะเบียนบัญชีใหม่",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { authViewModel.signInAnonymously() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("btn_anonymous_sign_in"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ใช้งานแบบชั่วคราว (Guest)", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 4. First-Time Surveyor Area Onboarding Dialog
    if (showAreaSetupDialog) {
        val villageOptions = listOf(
            "8" to "หมู่ 8 บ้านกร่างประดู่วัง (พื้นที่หลัก)",
            "1" to "หมู่ 1 บ้านป่าขะ",
            "2" to "หมู่ 2 บ้านดอนกลาง",
            "3" to "หมู่ 3 บ้านโคกกระชาย",
            "4" to "หมู่ 4 บ้านวังยายหุ่น",
            "5" to "หมู่ 5 บ้านคลองเหมือง",
            "6" to "หมู่ 6 บ้านหนองบัว",
            "7" to "หมู่ 7 บ้านเกาะกระชาย"
        )

        AlertDialog(
            onDismissRequest = { /* Force completion */ },
            icon = {
                Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "ยินดีต้อนรับสู่ Smart OSM",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "กรุณาระบุข้อมูลผู้สำรวจและพื้นที่รับผิดชอบ เพื่อเริ่มปฏิบัติงาน",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = surveyorNameInput,
                        onValueChange = { surveyorNameInput = it },
                        label = { Text("ชื่อ-นามสกุล ผู้สำรวจ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = surveyorPhoneInput,
                        onValueChange = { surveyorPhoneInput = it },
                        label = { Text("เบอร์โทรศัพท์ (ถ้ามี)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "เลือกพื้นที่รับผิดชอบ (หมู่บ้าน):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        villageOptions.forEach { (no, name) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedVillageNo = no
                                        selectedVillageName = name.substringBefore(" (")
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            ) {
                                RadioButton(
                                    selected = selectedVillageNo == no,
                                    onClick = {
                                        selectedVillageNo = no
                                        selectedVillageName = name.substringBefore(" (")
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedVillageNo == no) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = surveyorNameInput.trim().ifBlank {
                            currentUser?.displayName ?: "ผู้สำรวจ อสม."
                        }
                        authViewModel.saveSurveyorProfile(
                            context = context,
                            villageNo = selectedVillageNo,
                            villageName = selectedVillageName,
                            subdistrict = "ต.ป่าขะ",
                            district = "อ.บ้านนา",
                            province = "จ.นครนายก",
                            phone = surveyorPhoneInput.trim().takeIf { it.isNotBlank() },
                            role = "อสม. ประจำหมู่บ้าน"
                        )
                        showAreaSetupDialog = false
                        onLoginSuccess()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("เริ่มปฏิบัติงาน", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 5. Missing Web Client ID Configuration Dialog (Smooth dev setup)
    if (showMissingClientIdDialog) {
        AlertDialog(
            onDismissRequest = { showMissingClientIdDialog = false },
            icon = {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = EmeraldPrimary)
            },
            title = {
                Text("ตั้งค่า Google Sign-In", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "กรุณาระบุ Web Client ID จาก Google Cloud Console เพื่อเปิดใช้งาน Google Sign-In (ระบบจะบันทึกให้อัตโนมัติ)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputClientId,
                        onValueChange = { inputClientId = it },
                        label = { Text("Web Client ID") },
                        placeholder = { Text("xxx.apps.googleusercontent.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showMissingClientIdDialog = false
                            authViewModel.signInWithGoogleTest(context)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ทดสอบทันที (Dev Test)")
                    }
                    Button(
                        onClick = {
                            if (inputClientId.isNotBlank()) {
                                authViewModel.saveWebClientId(context, inputClientId)
                                showMissingClientIdDialog = false
                                authViewModel.signInWithGoogle(context, inputClientId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("บันทึกและเชื่อมต่อ")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showMissingClientIdDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

/**
 * Authentic 4-color Google "G" logo vector.
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.22f
        val center = Offset(w / 2, h / 2)

        // Red top arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 205f,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Yellow left arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 120f,
            sweepAngle = 85f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Green bottom arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 25f,
            sweepAngle = 95f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Blue right arc
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 310f,
            sweepAngle = 75f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Blue horizontal crossbar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(center.x * 0.95f, center.y),
            end = Offset(w - stroke * 0.35f, center.y),
            strokeWidth = stroke
        )
    }
}
