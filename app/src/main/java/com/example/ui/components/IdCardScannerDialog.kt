package com.example.ui.components

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintAccent
import com.example.utils.IdCardScanResult
import com.example.utils.NationalIdBarcodeParser
import com.example.viewmodel.PersonViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IdCardScannerDialog(
    onDismiss: () -> Unit,
    onScanned: (IdCardScanResult) -> Unit,
    viewModel: PersonViewModel? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val coroutineScope = rememberCoroutineScope()

    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var hasProcessed by remember { mutableStateOf(false) }
    var detectedFeedback by remember { mutableStateOf<IdCardScanResult?>(null) }
    var showTestPanel by remember { mutableStateOf(true) }
    var manualInputText by remember { mutableStateOf("") }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "laserScan")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserProgress"
    )

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Handles scanned result and database enrichment
    fun handleScannedCode(rawText: String) {
        if (hasProcessed) return
        hasProcessed = true

        coroutineScope.launch(Dispatchers.IO) {
            var parsed = NationalIdBarcodeParser.parse(rawText)

            // If national ID is present and we have missing name/address, attempt DB lookup
            if (viewModel != null && parsed.nationalId != null && (parsed.fullName == null || parsed.houseNo == null)) {
                try {
                    val existingPerson = viewModel.getPersonByNationalId(parsed.nationalId!!)
                    if (existingPerson != null) {
                        val household = viewModel.getHouseholdById(existingPerson.householdId)
                        parsed = parsed.copy(
                            fullName = parsed.fullName ?: existingPerson.fullName,
                            houseNo = parsed.houseNo ?: household?.houseNo,
                            villageNo = parsed.villageNo ?: household?.villageNo,
                            subdistrict = parsed.subdistrict ?: household?.subdistrict,
                            district = parsed.district ?: household?.district,
                            province = parsed.province ?: household?.province
                        )
                    }
                } catch (e: Exception) {
                    Log.e("IdCardScanner", "Database lookup error", e)
                }
            }

            withContext(Dispatchers.Main) {
                detectedFeedback = parsed
                // Wait briefly to show confirmation feedback, then dismiss and callback
                kotlinx.coroutines.delay(600)
                onScanned(parsed)
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Camera Stream
            if (cameraPermissionState.status.isGranted) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val mainExecutor = ContextCompat.getMainExecutor(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val scanner = BarcodeScanning.getClient()
                            val analysisExecutor = Executors.newSingleThreadExecutor()

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && !hasProcessed) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                barcode.rawValue?.let { code ->
                                                    if (!hasProcessed) {
                                                        handleScannedCode(code)
                                                    }
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            Log.e("IdCardScanner", "Scan failure", it)
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = camera.cameraControl
                            } catch (e: Exception) {
                                Log.e("IdCardScanner", "Camera binding failed", e)
                            }
                        }, mainExecutor)

                        previewView
                    }
                )

                // High-tech Viewfinder Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height

                    // Card aspect ratio: width 320dp, height 200dp
                    val cardWidth = 320.dp.toPx().coerceAtMost(canvasW * 0.88f)
                    val cardHeight = cardWidth * 0.62f
                    val left = (canvasW - cardWidth) / 2
                    val top = (canvasH - cardHeight) / 2 - 40.dp.toPx()

                    // Dimmed backdrop outside cutout
                    drawRect(color = Color.Black.copy(alpha = 0.65f))

                    // Transparent cutout
                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(cardWidth, cardHeight),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )

                    // Corner accents
                    val cornerSize = 36.dp.toPx()
                    val strokeW = 4.dp.toPx()
                    val cornerColor = MintAccent

                    // Top-Left
                    drawLine(cornerColor, Offset(left, top), Offset(left + cornerSize, top), strokeW)
                    drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerSize), strokeW)

                    // Top-Right
                    drawLine(cornerColor, Offset(left + cardWidth, top), Offset(left + cardWidth - cornerSize, top), strokeW)
                    drawLine(cornerColor, Offset(left + cardWidth, top), Offset(left + cardWidth, top + cornerSize), strokeW)

                    // Bottom-Left
                    drawLine(cornerColor, Offset(left, top + cardHeight), Offset(left + cornerSize, top + cardHeight), strokeW)
                    drawLine(cornerColor, Offset(left, top + cardHeight), Offset(left, top + cardHeight - cornerSize), strokeW)

                    // Bottom-Right
                    drawLine(cornerColor, Offset(left + cardWidth, top + cardHeight), Offset(left + cardWidth - cornerSize, top + cardHeight), strokeW)
                    drawLine(cornerColor, Offset(left + cardWidth, top + cardHeight), Offset(left + cardWidth, top + cardHeight - cornerSize), strokeW)

                    // Animated Laser line
                    val laserY = top + (cardHeight * laserProgress)
                    drawLine(
                        color = Color(0xFF10B981),
                        start = Offset(left + 8.dp.toPx(), laserY),
                        end = Offset(left + cardWidth - 8.dp.toPx(), laserY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            } else {
                // Permission Request Fallback
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "ต้องการสิทธิ์การใช้งานกล้อง",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "ระบบต้องการใช้กล้องเพื่อสแกนบาร์โค้ดบัตรประชาชน และกรอกข้อมูลอัตโนมัติ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("อนุญาตให้ใช้กล้อง", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Top Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "ปิด", tint = Color.White)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Badge, contentDescription = null, tint = MintAccent, modifier = Modifier.size(18.dp))
                        Text(
                            "สแกนบัตรประชาชน (Smart ML Kit)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        cameraControl?.enableTorch(isFlashOn)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "เปิด/ปิดแฟลช",
                        tint = if (isFlashOn) Color.Yellow else Color.White
                    )
                }
            }

            // Instruction Prompt above card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = (-150).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "วางบาร์โค้ดด้านหลังบัตร หรือ QR Code ในกรอบ",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Detected Banner (when success)
            detectedFeedback?.let { res ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldPrimary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Column {
                            Text("สแกนสำเร็จ!", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(res.displaySummary, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Bottom Test & Simulation Panel (Essential for Emulator / Testing)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTestPanel = !showTestPanel },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.DeveloperMode, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                "ทดสอบสแกนจำลอง (สำหรับ Emulator / ทดสอบ):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (showTestPanel) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showTestPanel) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "แตะเพื่อจำลองการสแกนบัตรประชาชนจริง:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Quick sample 1
                            OutlinedButton(
                                onClick = {
                                    handleScannedCode("1509900123456|นายสมชาย ใจดี|123/4|3|สารภี|สารภี|เชียงใหม่")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "นายสมชาย ใจดี (บ้านเลขที่ 123/4 หมู่ 3 ต.สารภี)",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }

                            // Quick sample 2
                            OutlinedButton(
                                onClick = {
                                    handleScannedCode("3100200543219#นางสมศรี สุขเกษม#บ้านเลขที่ 88/1 หมู่ 5 ต.หนองหอย อ.เมือง จ.เชียงใหม่")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "นางสมศรี สุขเกษม (บ้านเลขที่ 88/1 หมู่ 5 ต.หนองหอย)",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }

                            // Quick sample 3: 1D Barcode (Code 128)
                            OutlinedButton(
                                onClick = {
                                    handleScannedCode("5500100892114")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "บาร์โค้ด 13 หลัก: 5500100892114 (Code 128)",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }

                            // Manual entry box
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualInputText,
                                    onValueChange = { manualInputText = it },
                                    placeholder = { Text("พิมพ์บาร์โค้ดหรือ QR เพื่อทดสอบ...", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Button(
                                    onClick = {
                                        if (manualInputText.isNotBlank()) {
                                            handleScannedCode(manualInputText)
                                        }
                                    },
                                    enabled = manualInputText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("ทดสอบ", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
