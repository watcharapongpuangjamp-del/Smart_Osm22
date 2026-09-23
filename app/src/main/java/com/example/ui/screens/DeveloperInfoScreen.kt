package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.components.ThemeSettingsCard
import com.example.ui.theme.*
import com.example.viewmodel.AuthViewModel

/**
 * Screen displaying the Registered Village Health Volunteer (อสม.) Profile and system action cards.
 *
 * Adheres to user identity requirements:
 * - Displays the registered VHV's name, card ID, area, and health center.
 * - Restricts developer credit strictly to the app startup splash screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperInfoScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToCloudSync: () -> Unit = {},
    onNavigateToHealthKnowledge: () -> Unit = {},
    onNavigateToPlanOfWork: () -> Unit = {},
    onNavigateToDiagnostic: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToUserProfile: () -> Unit = {},
    onNavigateToVhvRegistration: () -> Unit = {},
    onNavigateToOsmRp00002: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        authViewModel.loadSurveyorProfile(context)
    }

    val userProfile by authViewModel.userProfile.collectAsState()
    val registeredName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: userProfile?.safeDisplayName?.takeIf { !it.contains("ผู้ใช้ชั่วคราว") && !it.contains("Guest") }
    val isRegistered = !registeredName.isNullOrBlank()
    val displayVhvName = registeredName ?: "ยังไม่ได้ลงทะเบียน อสม."

    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("ยืนยันการปิดแอปพลิเคชัน") },
            text = { Text("คุณต้องการออกจากแอปพลิเคชัน Smart OSM ใช่หรือไม่?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ออกจากแอป")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    val badgeBg = if (isDark) StatusVerifiedBgDark else StatusVerifiedBg
    val badgeFg = if (isDark) StatusVerifiedFgDark else StatusVerifiedFg

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        displayVhvName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToCloudSync) {
                        Icon(Icons.Filled.Sync, contentDescription = "สำรองข้อมูลและซิงค์", tint = Color.White)
                    }
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Filled.Clear, contentDescription = "ปิดแอป", tint = Color.White)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // VHV Profile Hero Crest / Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(12.dp, CircleShape, spotColor = CardShadowTint)
                    .clip(CircleShape)
                    .background(HeroGradientBrush),
                contentAlignment = Alignment.Center
            ) {
                if (!userProfile?.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userProfile?.photoUrl,
                        contentDescription = "รูปโปรไฟล์ อสม.",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.HealthAndSafety,
                        contentDescription = null,
                        tint = MintAccent,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayVhvName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isRegistered) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "ลงทะเบียนเรียบร้อยแล้ว",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (isRegistered) badgeBg else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (isRegistered) (userProfile?.roleTitle ?: "อาสาสมัครสาธารณสุขประจำหมู่บ้าน (อสม.)") else "ยังไม่ได้ลงทะเบียน อสม.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRegistered) badgeFg else MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                if (isRegistered) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "เลขประจำตัว อสม.: ${userProfile?.vhvCardId ?: "1-2602-00888-00-1"} • ${userProfile?.villageName ?: "หมู่ 7 บ้านกร่างประตูวัง"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // VHV Registration Card (Edit / Register)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToVhvRegistration)
                    .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = EmeraldPrimary),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                border = androidx.compose.foundation.BorderStroke(2.dp, MintAccent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.HowToReg, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Column {
                            Text(
                                text = "ลงทะเบียน อสม. / แก้ไขข้อมูลโปรไฟล์",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "จัดการข้อมูลประจำตัว รูปบัตร รูปโปรไฟล์ และพื้นที่รับผิดชอบ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
                }
            }

            // OSMRP00002 VHV Directory Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToOsmRp00002)
                    .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = CardShadowTint),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(EmeraldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Dataset, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Column {
                            Text(
                                text = "รายงานข้อมูล อสม. ต.ป่าขะ (OSMRP00002)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ฐานข้อมูลรายชื่อ อสม. 13 หมู่บ้าน (thaiphc.net)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = EmeraldPrimary)
                }
            }

            // Theme Settings Card
            ThemeSettingsCard()

            // User Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToUserProfile)
                    .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = CardShadowTint),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F766E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Column {
                            Text(
                                text = "ข้อมูลโปรไฟล์ผู้ใช้งาน (User Profile)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "ดูรายละเอียดบัญชี, Firebase UID, อีเมล และสถานะยืนยันตัวตน",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // Cloud Backup & Sync Quick Access Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToCloudSync)
                    .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = CardShadowTint),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(EmeraldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Column {
                            Text(
                                text = "สำรองข้อมูลและซิงค์คลาวด์",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "ส่งออกไฟล์ Excel และซิงค์ Firestore",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // Plan of Work Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToPlanOfWork)
                    .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = CardShadowTint),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.FactCheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Column {
                            Text(
                                text = "แผนการปฏิบัติงาน (Plan of Work)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "ตารางงาน อสม. รายวัน/รายเดือน & กิจกรรมเยี่ยมบ้าน",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            // Health Knowledge Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToHealthKnowledge)
                    .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = CardShadowTint),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MintAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.LocalLibrary, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
                        }
                        Column {
                            Text(
                                text = "คู่มือเกณฑ์สุขภาพและความรู้ อสม.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "เกณฑ์ความดัน BMI และความรู้ตามช่วงวัย",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            // Contact & Area Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = CardShadowTint),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    InfoRow(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        },
                        title = "พื้นที่รับผิดชอบ",
                        text = userProfile?.villageName ?: "หมู่ 7 บ้านกร่างประตูวัง ต.ป่าขะ อ.บ้านนา จ.นครนายก"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    InfoRow(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            }
                        },
                        title = "หน่วยบริการปฐมภูมิ",
                        text = userProfile?.healthCenter ?: "รพ.สต.ป่าขะ"
                    )

                    userProfile?.phoneNumber?.takeIf { it.isNotBlank() }?.let { phoneNum ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNum"))
                                    context.startActivity(dialIntent)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(badgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PhoneInTalk, contentDescription = null, tint = badgeFg, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("เบอร์โทรศัพท์ติดต่อ (แตะเพื่อโทร)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = phoneNum,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "อสม. หมอคนที่ 1 ประจำหมู่บ้าน",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.15f))
            ) {
                Text(
                    text = "“แจ้งข่าวร้าย กระจายข่าวดี ชี้บริการ ประสานงานสาธารณสุข บำบัดทุกข์ให้ประชาชน ดำรงตนเป็นตัวอย่างที่ดี”",
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: @Composable () -> Unit,
    title: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
