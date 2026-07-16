package com.example.attendanceapp

import android.Manifest
import android.os.Handler
import android.os.Looper
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
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
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
    fcba: String, // Tambahkan parameter fcba
    onBack: () -> Unit,
    onQRVerified: (Employee) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    // --- LOGIKA TEMA VISUAL ---
    val isReceiveMode = action == AttendanceAction.RECEIVE
    val themeColor = if (isReceiveMode) Color(0xFF1A3A8F) else Color(0xFF37474F)
    val titleText = if (isReceiveMode) "SINKRONISASI DATA" else "Langkah 1 — Scan QR"
    val actionLabel = when (action) {
        AttendanceAction.RECEIVE -> "Terima Data"
        AttendanceAction.CHECK_IN -> "Masuk"
        else -> "Keluar"
    }

    var statusText by remember {
        mutableStateOf(if (isReceiveMode) "Arahkan Kamera ke QR Mandor" else "Arahkan QR Code karyawan ke kamera")
    }
    var statusColor by remember { mutableStateOf(Color(0xFFB0BEC5)) }
    var scanned by remember { mutableStateOf(false) }

    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }


    DisposableEffect(Unit) {
        onDispose {
            // 1. Paksa hardware kamera berhenti seketika
            try {
                cameraProvider?.unbindAll()
                Log.d("QRScanner", "Kamera berhasil di-destroy")
            } catch (e: Exception) {
                Log.e("QRScanner", "Gagal unbind kamera", e)
            }

            // 2. Matikan executor dan scanner
            cameraExecutor.shutdownNow() // Menggunakan shutdownNow agar thread langsung berhenti
            barcodeScanner.close()
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titleText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (isReceiveMode) {
                            Text("Mode Penerimaan Data Kerani", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isFlashOn = !isFlashOn
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    }) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Senter",
                            tint = if (isFlashOn) Color.Yellow else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A3A8F), // Berubah jadi Biru jika mode Terima
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
                                    @ExperimentalGetImage
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && !scanned) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    val scannedId = barcode.rawValue ?: continue
                                                    if (scannedId.isNotEmpty() && !scanned) {

                                                        if (isReceiveMode) {
                                                            // JIKA TERIMA DATA: Langsung bypass validasi DB karyawan
                                                            scanned = true
                                                            statusText = "✅ Data Mandor Terdeteksi"
                                                            statusColor = Color(0xFF69F0AE)

                                                            Handler(Looper.getMainLooper()).postDelayed({
                                                                onQRVerified(
                                                                    Employee(
                                                                        fccode = scannedId,
                                                                        name = "Data Transfer",
                                                                        fcba = "",
                                                                        sectionName = "",
                                                                        gangCode = "",
                                                                        position = "",
                                                                        gender = "",
                                                                        address = "",
                                                                        companyNik = "",
                                                                        faceEmbedding = null,
                                                                        lastUpdate = ""
                                                                    )
                                                                )
                                                            }, 600)
                                                        }
                                                        else {
                                                            // JIKA ABSEN BIASA
                                                            val employee = dbHelper.getEmployeeByOnlyCode(scannedId.trim(), fcba)
                                                            if (employee != null) {
                                                                scanned = true
                                                                statusText = "✅ Berhasil: ${employee.name}"
                                                                statusColor = Color(0xFF69F0AE)
                                                                Handler(Looper.getMainLooper()).postDelayed({
                                                                    onQRVerified(employee)
                                                                }, 600)
                                                            } else {
                                                                statusText = "❌ ID ($scannedId) tidak terdaftar"
                                                                statusColor = Color(0xFFEF5350)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { Log.e("QRScanner", "Scan failed", it) }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) { Log.e("QRScanner", "Binding failed", e) }
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
                                width = 1.dp,
                                color = if (scanned) Color(0xFF69F0AE) else Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    )

                    QRCornerBrackets(
                        modifier = Modifier.align(Alignment.Center),
                        color = if (scanned) Color(0xFF69F0AE) else Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A3A8F))
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

                    // --- FOOTER DENGAN TEMA BERUBAH ---
                    Surface(
                        color = if (isReceiveMode) Color(0xFF1A3A8F).copy(alpha = 0.2f) else Color(0xFF263238),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isReceiveMode) "📥 PENERIMAAN DATA OFFLINE" else "🔐 Absen $actionLabel — Langkah 1/2",
                            color = if (isReceiveMode) Color(0xFF448AFF) else Color(0xFF90A4AE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isReceiveMode)
                            "Arahkan kamera ke QR Code yang ditampilkan oleh Mandor.\nPastikan layar HP Mandor cukup terang."
                        else "Silahkan scan QR Code karyawan.\nSistem akan mencocokkan ID dengan database.",
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
        Box(modifier = Modifier.size(bracketSize).align(Alignment.TopStart)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color))
            Box(modifier = Modifier.width(thickness).fillMaxHeight().background(color))
        }
        Box(modifier = Modifier.size(bracketSize).align(Alignment.TopEnd)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color).align(Alignment.TopEnd))
            Box(modifier = Modifier.width(thickness).fillMaxHeight().background(color).align(Alignment.TopEnd))
        }
        Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomStart)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color).align(Alignment.BottomStart))
            Box(modifier = Modifier.width(thickness).fillMaxHeight().background(color).align(Alignment.BottomStart))
        }
        Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomEnd)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color).align(Alignment.BottomEnd))
            Box(modifier = Modifier.width(thickness).fillMaxHeight().background(color).align(Alignment.BottomEnd))
        }
    }
}