package com.example.attendanceapp

import android.Manifest
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

data class QREmployee(
    val employeeId: String,
    val employeeName: String
)

/**
 * Format QR Code yang diharapkan:
 * EMP_ID:EMP001|NAME:Budi Santoso
 *
 * Bisa digenerate dari menu "Buat QR" di halaman Kelola Karyawan
 */
fun parseQRCode(raw: String): QREmployee? {
    return try {
        val parts = raw.split("|")
        val idPart = parts.firstOrNull { it.startsWith("EMP_ID:") }
        val namePart = parts.firstOrNull { it.startsWith("NAME:") }
        if (idPart != null && namePart != null) {
            QREmployee(
                employeeId = idPart.removePrefix("EMP_ID:").trim(),
                employeeName = namePart.removePrefix("NAME:").trim()
            )
        } else null
    } catch (e: Exception) { null }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    action: AttendanceAction,
    onBack: () -> Unit,
    onQRVerified: (QREmployee) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val actionLabel = if (action == AttendanceAction.CHECK_IN) "Masuk" else "Keluar"
    val actionColor = if (action == AttendanceAction.CHECK_IN) Color(0xFF3F51B5) else Color(0xFFE53935)

    var statusText by remember { mutableStateOf("Arahkan QR Code karyawan ke kamera") }
    var statusColor by remember { mutableStateOf(Color(0xFFB0BEC5)) }
    var scanned by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Langkah 1 — Scan QR", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF37474F),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicator
            StepIndicator(currentStep = 1)

            if (!cameraPermission.status.isGranted) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Izin kamera diperlukan", color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                            Text("Izinkan Kamera")
                        }
                    }
                }
                return@Scaffold
            }

            // Camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val barcodeScanner = BarcodeScanning.getClient()
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                @androidx.camera.core.ExperimentalGetImage
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && !scanned) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                                    val raw = barcode.rawValue ?: continue
                                                    val employee = parseQRCode(raw)
                                                    if (employee != null && !scanned) {
                                                        scanned = true
                                                        statusText = "✅ QR Valid: ${employee.employeeName} (${employee.employeeId})"
                                                        statusColor = Color(0xFF69F0AE)
                                                        // Lanjut ke face scan setelah 1 detik
                                                        android.os.Handler(android.os.Looper.getMainLooper())
                                                            .postDelayed({ onQRVerified(employee) }, 800)
                                                    } else if (!scanned) {
                                                        statusText = "❌ Format QR tidak valid"
                                                        statusColor = Color(0xFFEF5350)
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA, // pakai kamera belakang untuk QR
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("QRCamera", "Error", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // QR frame guide
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .align(Alignment.Center)
                        .border(
                            width = 3.dp,
                            color = if (scanned) Color(0xFF69F0AE) else Color(0xFFFFEB3B),
                            shape = RoundedCornerShape(16.dp)
                        )
                )

                // Corner brackets
                QRCornerBrackets(
                    modifier = Modifier.align(Alignment.Center),
                    color = if (scanned) Color(0xFF69F0AE) else Color.White
                )

                // Scan line animation hint
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                    color = if (scanned) Color(0xFF1B5E20) else Color(0xFF37474F),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (scanned) "✅ QR Terverifikasi!" else "📷 Scan QR Code",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom info
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Setelah QR terverifikasi, Anda akan\ndiarahkan ke verifikasi wajah (Langkah 2)",
                    color = Color(0xFF78909C),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF263238),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🔐 Absen $actionLabel — Autentikasi 2 Faktor",
                        color = Color(0xFF90A4AE),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QRCornerBrackets(modifier: Modifier = Modifier, color: Color) {
    val size = 220.dp
    val bracketSize = 24.dp
    val thickness = 3.dp

    Box(modifier = modifier.size(size)) {
        // Top-left
        Box(modifier = Modifier.size(bracketSize).align(Alignment.TopStart)
            .border(width = thickness, color = color, shape = RoundedCornerShape(topStart = 8.dp)))
        // Top-right
        Box(modifier = Modifier.size(bracketSize).align(Alignment.TopEnd)
            .border(width = thickness, color = color, shape = RoundedCornerShape(topEnd = 8.dp)))
        // Bottom-left
        Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomStart)
            .border(width = thickness, color = color, shape = RoundedCornerShape(bottomStart = 8.dp)))
        // Bottom-right
        Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomEnd)
            .border(width = thickness, color = color, shape = RoundedCornerShape(bottomEnd = 8.dp)))
    }
}

@Composable
fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepBubble(number = 1, label = "Scan QR", isActive = currentStep == 1, isDone = currentStep > 1)
        Divider(
            modifier = Modifier.width(40.dp).padding(horizontal = 4.dp),
            color = if (currentStep > 1) Color(0xFF69F0AE) else Color(0xFF37474F),
            thickness = 2.dp
        )
        StepBubble(number = 2, label = "Wajah", isActive = currentStep == 2, isDone = currentStep > 2)
        Divider(
            modifier = Modifier.width(40.dp).padding(horizontal = 4.dp),
            color = if (currentStep > 2) Color(0xFF69F0AE) else Color(0xFF37474F),
            thickness = 2.dp
        )
        StepBubble(number = 3, label = "Selesai", isActive = currentStep == 3, isDone = false)
    }
}

@Composable
fun StepBubble(number: Int, label: String, isActive: Boolean, isDone: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when {
                        isDone -> Color(0xFF00897B)
                        isActive -> Color(0xFF3F51B5)
                        else -> Color(0xFF37474F)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isDone) "✓" else "$number",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            color = if (isActive || isDone) Color.White else Color(0xFF546E7A),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
