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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    action: AttendanceAction,
    dbHelper: AttendanceDatabaseHelper,
    onBack: () -> Unit,
    onQRVerified: (Employee) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    val actionLabel = if (action == AttendanceAction.CHECK_IN) "Masuk" else "Keluar"

    var statusText by remember { mutableStateOf("Arahkan QR Code karyawan ke kamera") }
    var statusColor by remember { mutableStateOf(Color(0xFFB0BEC5)) }
    var scanned by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

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
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    @androidx.camera.core.ExperimentalGetImage
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && !scanned) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    // PERBAIKAN: Ambil ID langsung dari QR
                                                    val scannedId = barcode.rawValue ?: continue

                                                    if (scannedId.isNotEmpty() && !scanned) {
                                                        // Cari karyawan di DB berdasarkan ID mentah tersebut
                                                        val employee = dbHelper.getEmployee(scannedId.trim())

                                                        if (employee != null) {
                                                            scanned = true
                                                            statusText = "✅ Berhasil: ${employee.name}"
                                                            statusColor = Color(0xFF69F0AE)

                                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                                onQRVerified(employee)
                                                            }, 600)
                                                        } else {
                                                            // Jika ID terbaca tapi tidak ada di database kita
                                                            statusText = "❌ ID ($scannedId) tidak terdaftar"
                                                            statusColor = Color(0xFFEF5350)
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnFailureListener {
                                                Log.e("QRScanner", "Scan failed", it)
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
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) {
                                    Log.e("QRScanner", "Binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Scanner Overlay
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .align(Alignment.Center)
                            .border(
                                width = 2.dp,
                                color = if (scanned) Color(0xFF69F0AE) else Color.White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                    QRCornerBrackets(modifier = Modifier.align(Alignment.Center), color = if (scanned) Color(0xFF69F0AE) else Color.White)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFF263238),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🔐 Absen $actionLabel — Langkah 1/2",
                            color = Color(0xFF90A4AE),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Silahkan scan QR Code karyawan.\nSistem akan mencocokkan ID dengan database.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QRCornerBrackets(modifier: Modifier = Modifier, color: Color) {
    val size = 240.dp
    val bracketSize = 32.dp
    val thickness = 4.dp

    Box(modifier = modifier.size(size)) {
        // Top Left
        Box(modifier = Modifier.size(bracketSize).align(Alignment.TopStart)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color))
            Box(modifier = Modifier.width(thickness).fillMaxHeight().background(color))
        }
        // Top Right
        Box(modifier = Modifier.size(bracketSize).align(Alignment.TopEnd)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color).align(Alignment.TopEnd))
            Box(modifier = Modifier.width(thickness).fillMaxHeight().background(color).align(Alignment.TopEnd))
        }
        // Bottom Left
        Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomStart)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color).align(Alignment.BottomStart))
            Box(modifier = Modifier.width(thickness).fillMaxHeight().background(color).align(Alignment.BottomStart))
        }
        // Bottom Right
        Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomEnd)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color).align(Alignment.BottomEnd))
            Box(modifier = Modifier.width(thickness).fillMaxHeight().background(color).align(Alignment.BottomEnd))
        }
    }
}