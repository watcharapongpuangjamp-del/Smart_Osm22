package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Household
import com.example.data.DataStatus
import com.example.ui.theme.*
import com.example.viewmodel.PersonViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HouseholdFormScreen(
    viewModel: PersonViewModel,
    householdId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var houseNo by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationAccuracy by remember { mutableStateOf<Float?>(null) }
    var locationCapturedAt by remember { mutableStateOf<Long?>(null) }
    var locationProvider by remember { mutableStateOf<String?>(null) }
    var dataStatus by remember { mutableStateOf(DataStatus.NEEDS_REVIEW) }
    
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(householdId) {
        if (householdId != -1L) {
            val household = viewModel.getHouseholdById(householdId)
            household?.let {
                houseNo = it.houseNo
                latitude = it.latitude
                longitude = it.longitude
                locationAccuracy = it.locationAccuracy
                locationCapturedAt = it.locationCapturedAt
                locationProvider = it.locationProvider
                dataStatus = it.dataStatus
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (householdId == -1L) "เพิ่มครัวเรือนใหม่" else "แก้ไขข้อมูลครัวเรือน",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (houseNo.isNotBlank()) {
                        val household = Household(
                            id = if (householdId == -1L) 0 else householdId,
                            houseNo = houseNo.trim(),
                            latitude = latitude,
                            longitude = longitude,
                            locationAccuracy = locationAccuracy,
                            locationCapturedAt = locationCapturedAt,
                            locationProvider = locationProvider,
                            dataStatus = dataStatus
                        )
                        if (householdId == -1L) {
                            viewModel.insertHousehold(household) { newId ->
                                onNavigateToDetail(newId)
                            }
                        } else {
                            viewModel.updateHousehold(household)
                            onNavigateBack()
                        }
                    } else {
                        Toast.makeText(context, "กรุณากรอกบ้านเลขที่", Toast.LENGTH_SHORT).show()
                    }
                },
                icon = { Icon(Icons.Filled.Save, contentDescription = null) },
                text = { Text("บันทึกครัวเรือน", fontWeight = FontWeight.Bold) },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp), spotColor = CardShadowTint)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Address
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(18.dp), spotColor = CardShadowTint),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp, 16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(EmeraldPrimary)
                        )
                        Text("ข้อมูลประจำครัวเรือน", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    OutlinedTextField(
                        value = houseNo,
                        onValueChange = { houseNo = it },
                        label = { Text("บ้านเลขที่ *") },
                        placeholder = { Text("เช่น 12/3 หรือ 45") },
                        leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null, tint = EmeraldPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            
            // Card 2: GPS Telemetry
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(18.dp), spotColor = CardShadowTint),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp, 16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(TealSecondary)
                        )
                        Text("พิกัดแผนที่ (GPS Tracking)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    if (latitude != null && longitude != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StatusVerifiedBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = StatusVerifiedFg, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Lat: $latitude, Lon: $longitude", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                locationAccuracy?.let { acc ->
                                    val isGood = acc <= 20f
                                    Text(
                                        "ความแม่นยำ: ±${acc}m (${if (isGood) "ระดับสูง" else "ปานกลาง"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isGood) StatusVerifiedFg else StatusNeedsReviewFg,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ยังไม่ได้บันทึกพิกัดตำแหน่งบ้าน", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (locationPermissionState.status.isGranted) {
                                coroutineScope.launch {
                                    try {
                                        Toast.makeText(context, "กำลังระบุพิกัดความแม่นยำสูง...", Toast.LENGTH_SHORT).show()
                                        @SuppressLint("MissingPermission")
                                        val locationRequest = com.google.android.gms.location.CurrentLocationRequest.Builder()
                                            .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                                            .build()
                                        @SuppressLint("MissingPermission")
                                        val location = fusedLocationClient.getCurrentLocation(locationRequest, null).await()
                                        if (location != null) {
                                            latitude = location.latitude
                                            longitude = location.longitude
                                            locationAccuracy = if (location.hasAccuracy()) location.accuracy else null
                                            locationCapturedAt = location.time
                                            locationProvider = location.provider
                                            Toast.makeText(context, "บันทึกพิกัดสำเร็จ", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "ไม่สามารถหาตำแหน่งได้", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                locationPermissionState.launchPermissionRequest()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (latitude == null) "ดึงพิกัด GPS ปัจจุบัน" else "อัปเดตพิกัดใหม่", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

