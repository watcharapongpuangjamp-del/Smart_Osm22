package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.util.BiometricAuthHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.auth.UserProfile
import com.example.ui.components.ThemeQuickToggleButton
import com.example.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private val EmeraldPrimary = Color(0xFF0F766E)
private val TealAccent = Color(0xFF0D9488)
private val CardBorderColor = Color(0xFFE2E8F0)

/**
 * Screen displaying the authenticated user's details retrieved from Firebase Auth,
 * such as display name, email, avatar, verification status, and provider metadata.
 *
 * Adheres strictly to the system Identity model:
 * User Identity = Firebase Authentication user.uid
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showSignOutDialog by remember { mutableStateOf(false) }

    val isBiometricHardwareAvailable = remember(context) { BiometricAuthHelper.isBiometricAvailable(context) }
    var isBiometricEnabled by remember(context) { mutableStateOf(BiometricAuthHelper.isBiometricEnabled(context)) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("user_profile_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ข้อมูลโปรไฟล์ผู้ใช้งาน",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_profile_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "กลับ"
                        )
                    }
                },
                actions = {
                    ThemeQuickToggleButton()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(2.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val profile = userProfile

            if (profile != null) {
                // 1. Hero Profile Header Card
                UserProfileHeroCard(
                    profile = profile
                )

                // 2. Identity Architecture Card (User Identity = Firebase UID)
                UserIdentityCard(
                    uid = profile.uid,
                    onCopyUid = {
                        clipboardManager.setText(AnnotatedString(profile.uid))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("คัดลอก UID เรียบร้อยแล้ว")
                        }
                    }
                )

                // 3. Authenticated Details List Card
                UserDetailsSection(
                    profile = profile
                )

                // 4. Action Buttons
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "การจัดการบัญชี",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Switch account / Re-login
                        Button(
                            onClick = onNavigateToLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_profile_switch_account"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("สลับบัญชี / เข้าสู่ระบบใหม่", fontWeight = FontWeight.SemiBold)
                        }

                        // Sign Out Button
                        OutlinedButton(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_profile_sign_out"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ออกจากระบบ (Sign Out)", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Biometric / Security Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ความปลอดภัย (Security & Biometrics)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Fingerprint,
                                        contentDescription = "ลายนิ้วมือ",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "ปลดล็อกด้วยสแกนลายนิ้วมือ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isBiometricHardwareAvailable) {
                                            if (isBiometricEnabled) "เปิดใช้งาน เข้าแอปได้รวดเร็ว" else "ปิดการใช้งาน (ใช้ PIN เท่านั้น)"
                                        } else {
                                            "อุปกรณ์นี้ไม่รองรับหรือยังไม่ได้ลงทะเบียนลายนิ้วมือ"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isBiometricEnabled && isBiometricHardwareAvailable,
                                onCheckedChange = { checked ->
                                    if (isBiometricHardwareAvailable) {
                                        isBiometricEnabled = checked
                                        BiometricAuthHelper.setBiometricEnabled(context, checked)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (checked) "เปิดใช้งานสแกนลายนิ้วมือแล้ว" else "ปิดใช้งานสแกนลายนิ้วมือแล้ว"
                                            )
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("อุปกรณ์นี้ไม่รองรับหรือยังไม่ได้ลงทะเบียนลายนิ้วมือในระบบ Android")
                                        }
                                    }
                                },
                                enabled = isBiometricHardwareAvailable,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }
            } else {
                // Not Authenticated / Guest State Card
                UnauthenticatedCard(
                    onNavigateToLogin = onNavigateToLogin
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(text = "ยืนยันการออกจากระบบ", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "คุณต้องการออกจากระบบ Firebase Authentication หรือไม่? " +
                            "ระบบจะกลับสู่โหมดข้อมูลเฉพาะที่ (Local Room Mode) โดยข้อมูลในเครื่องจะไม่สูญหาย"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        authViewModel.signOut()
                        Toast.makeText(context, "ออกจากระบบเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("dialog_sign_out_confirm")
                ) {
                    Text("ออกจากระบบ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                    modifier = Modifier.testTag("dialog_sign_out_dismiss")
                ) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}

/**
 * Visual Hero Banner displaying user avatar, display name, email, and authentication badge.
 */
@Composable
private fun UserProfileHeroCard(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EmeraldPrimary,
                            TealAccent.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar with Coil AsyncImage or fallback
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "รูปโปรไฟล์ผู้ใช้งาน",
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback initials or icon
                        val initial = profile.displayName?.firstOrNull()?.uppercase()
                            ?: profile.email?.firstOrNull()?.uppercase()
                        if (initial != null) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // Display Name
                Text(
                    text = profile.safeDisplayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("profile_display_name")
                )

                // Email with Verification Pill
                if (!profile.email.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.testTag("profile_email")
                        )
                        if (profile.isEmailVerified) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "ยืนยันอีเมลแล้ว",
                                tint = Color(0xFF86EFAC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Status Badge Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = profile.providerLabelThai,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("profile_provider")
                        )
                    }

                    if (profile.isEmailVerified) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF15803D).copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "Verified",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${profile.villageName} • ${profile.roleTitle}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * System Identity Principle Card showing the User Identity = Firebase Authentication user.uid
 */
@Composable
private fun UserIdentityCard(
    uid: String,
    onCopyUid: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "User Identity (Firebase Auth)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "user.uid:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uid,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = EmeraldPrimary,
                            modifier = Modifier.testTag("profile_uid"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onCopyUid,
                        modifier = Modifier.testTag("btn_copy_uid")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "คัดลอก UID",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Text(
                    text = "หลักการ Identity: user.uid ใช้เป็นตัวตนผู้ใช้ระดับระบบ เชื่อมโยงกับสิทธิ์พื้นที่รับผิดชอบ (villageId) โดยแยกอิสระจาก householdUuid และ personUuid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * Detailed user metadata section.
 */
@Composable
private fun UserDetailsSection(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "รายละเอียดบัญชีผู้ใช้งาน",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider()

            // Display Name Row
            DetailItemRow(
                icon = Icons.Filled.Person,
                title = "ชื่อที่แสดง (Display Name)",
                value = profile.displayName ?: "ไม่ได้ระบุ (No Display Name)"
            )

            // Area / Village Row
            DetailItemRow(
                icon = Icons.Filled.Place,
                title = "พื้นที่รับผิดชอบ (Area / Village)",
                value = "${profile.villageName} (${profile.subdistrict} ${profile.district} ${profile.province})"
            )

            // Role Row
            DetailItemRow(
                icon = Icons.Filled.VerifiedUser,
                title = "ตำแหน่ง / สิทธิ์การใช้งาน",
                value = profile.roleTitle
            )

            // Email Row
            DetailItemRow(
                icon = Icons.Filled.Email,
                title = "อีเมล (Email Address)",
                value = profile.email ?: "ไม่มีข้อมูลอีเมล"
            )

            // Phone Row
            DetailItemRow(
                icon = Icons.Filled.Phone,
                title = "เบอร์โทรศัพท์ (Phone Number)",
                value = profile.phoneNumber ?: "ไม่ได้ระบุ"
            )

            // Provider Row
            DetailItemRow(
                icon = Icons.Filled.Security,
                title = "ประเภทผู้ให้บริการยืนยันตัวตน",
                value = profile.providerLabelThai
            )

            // Account Created Timestamp
            profile.formattedCreationDate?.let { creationDate ->
                DetailItemRow(
                    icon = Icons.Filled.Schedule,
                    title = "วันที่สร้างบัญชี (Account Created)",
                    value = creationDate
                )
            }

            // Last Sign In Timestamp
            profile.formattedLastSignInDate?.let { lastSignIn ->
                DetailItemRow(
                    icon = Icons.Filled.Login,
                    title = "เข้าสู่ระบบล่าสุด (Last Sign In)",
                    value = lastSignIn
                )
            }

            // Anonymous flag
            DetailItemRow(
                icon = Icons.Filled.Lock,
                title = "โหมดผู้ใช้งาน",
                value = if (profile.isAnonymous) "บัญชีชั่วคราว (Guest)" else "บัญชีทางการ (Registered)"
            )
        }
    }
}

@Composable
private fun DetailItemRow(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(EmeraldPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * State displayed when the user is unauthenticated or running in local offline mode.
 */
@Composable
private fun UnauthenticatedCard(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "ยังไม่ได้เข้าสู่ระบบ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "ขณะนี้ท่านกำลังใช้งานในโหมดเครื่องเฉพาะที่ (Local Room Mode) ข้อมูลถูกจัดเก็บบนอุปกรณ์อย่างปลอดภัย หากต้องการซิงค์ข้อมูลกับคลาวด์ กรุณาลงชื่อเข้าใช้ด้วย Google หรืออีเมล",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_profile_login"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("เข้าสู่ระบบด้วย Google หรืออีเมล", fontWeight = FontWeight.Bold)
            }
        }
    }
}
