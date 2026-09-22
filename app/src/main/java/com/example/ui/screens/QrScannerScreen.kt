package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Person
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintAccent
import com.example.viewmodel.PersonViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    viewModel: PersonViewModel,
    onPersonFound: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    val persons by viewModel.allPersons.collectAsStateWithLifecycle()

    var scannedResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    // Pulsing animation for scan line
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanProgress"
    )

    LaunchedEffect(cameraPermissionState.status) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Process scanned value against database
    fun handleScannedCode(rawCode: String) {
        val cleaned = rawCode.trim()
        val found = persons.find { 
            it.nationalId == cleaned || it.personUuid == cleaned || it.fullName.contains(cleaned, ignoreCase = true)
        }
        if (found != null) {
            scannedResult = found.fullName
            onPersonFound(found.id)
        } else {
            errorMessage = "ไม่พบข้อมูลประชากรสำหรับรหัส: $cleaned"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "สแกน QR Code / บัตรประชาชน",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "กลับ", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            cameraControl?.enableTorch(isFlashOn)
                        }
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "เปิด/ปิดไฟแฟลช",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldPrimary)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (cameraPermissionState.status.isGranted) {
                // Camera Preview & Barcode Analyzer
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val executor = ContextCompat.getMainExecutor(ctx)

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
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { code ->
                                                if (scannedResult == null) {
                                                    scannedResult = code
                                                    handleScannedCode(code)
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        Log.e("QrScanner", "Barcode scan failed", it)
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
                            } catch (exc: Exception) {
                                Log.e("QrScanner", "Use case binding failed", exc)
                            }
                        }, executor)

                        previewView
                    }
                )

                // Viewfinder Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val boxSize = 280.dp.toPx()
                    val left = (canvasWidth - boxSize) / 2
                    val top = (canvasHeight - boxSize) / 2 - 50.dp.toPx()

                    // Semi-transparent dark overlay
                    drawRect(color = Color.Black.copy(alpha = 0.6f))

                    // Cutout frame
                    drawRect(
                        color = Color.Transparent,
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(boxSize, boxSize)
                    )

                    // Scanner Border corners
                    val cornerLength = 40.dp.toPx()
                    val strokeWidth = 4.dp.toPx()
                    val frameColor = MintAccent

                    // Top-Left
                    drawLine(frameColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left + cornerLength, top), strokeWidth)
                    drawLine(frameColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, top + cornerLength), strokeWidth)

                    // Top-Right
                    drawLine(frameColor, androidx.compose.ui.geometry.Offset(left + boxSize, top), androidx.compose.ui.geometry.Offset(left + boxSize - cornerLength, top), strokeWidth)
                    drawLine(frameColor, androidx.compose.ui.geometry.Offset(left + boxSize, top), androidx.compose.ui.geometry.Offset(left + boxSize, top + cornerLength), strokeWidth)

                    // Bottom-Left
                    drawLine(frameColor, androidx.compose.ui.geometry.Offset(left, top + boxSize), androidx.compose.ui.geometry.Offset(left + cornerLength, top + boxSize), strokeWidth)
                    drawLine(frameColor, androidx.compose.ui.geometry.Offset(left, top + boxSize), androidx.compose.ui.geometry.Offset(left, top + boxSize - cornerLength), strokeWidth)

                    // Bottom-Right
                    drawLine(frameColor, androidx.compose.ui.geometry.Offset(left + boxSize, top + boxSize), androidx.compose.ui.geometry.Offset(left + boxSize - cornerLength, top + boxSize), strokeWidth)
                    drawLine(frameColor, androidx.compose.ui.geometry.Offset(left + boxSize, top + boxSize), androidx.compose.ui.geometry.Offset(left + boxSize, top + boxSize - cornerLength), strokeWidth)

                    // Scan Laser Line
                    val currentY = top + (boxSize * scanProgress)
                    drawLine(
                        color = Color.Red,
                        start = androidx.compose.ui.geometry.Offset(left + 10, currentY),
                        end = androidx.compose.ui.geometry.Offset(left + boxSize - 10, currentY),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // Instructions & Quick Simulation Panel
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = "วาง QR Code หรือ บาร์โค้ดบัตรประชาชน ให้อยู่ในกรอบ",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Simulation / Quick Select Card for Emulators & Testing
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ทดสอบ / เลือกจำลองการสแกน (สำหรับจำลองบน Emulator):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (persons.isEmpty()) {
                                Text("ไม่พบข้อมูลประชากรในระบบ", style = MaterialTheme.typography.bodySmall)
                            } else {
                                persons.take(3).forEach { person ->
                                    OutlinedButton(
                                        onClick = { onPersonFound(person.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                                    ) {
                                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("สแกนจำลอง: ${person.fullName} (${person.nationalId ?: "ไม่มีเลขบัตร"})")
                                    }
                                }
                            }

                            errorMessage?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Permission Request Prompt
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ต้องการสิทธิ์การใช้งานกล้อง",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "แอปจำเป็นต้องใช้กล้องเพื่อสแกน QR Code และบัตรประชาชนของประชากร",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("อนุญาตการใช้กล้อง", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
