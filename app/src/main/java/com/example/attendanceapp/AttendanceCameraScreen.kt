package com.example.attendanceapp

import android.util.Log
import android.Manifest
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
import androidx.compose.ui.graphics.Color
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AttendanceCameraScreen(
    action: AttendanceAction,
    verifiedEmployee: QREmployee?,   // data dari QR scan (2FA langkah 1)
    faceDataHelper: FaceDataHelper,
    dbHelper: AttendanceDatabaseHelper,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val actionLabel = if (action == AttendanceAction.CHECK_IN) "Masuk" else "Keluar"
    val actionColor = if (action == AttendanceAction.CHECK_IN) Color(0xFF3F51B5) else Color(0xFFE53935)

    var statusText by remember {
        mutableStateOf(
            if (verifiedEmployee != null)
                "Halo ${verifiedEmployee.employeeName}!\nArahkan wajah Anda untuk konfirmasi identitas"
            else
                "Arahkan wajah Anda ke kamera"
        )
    }
    var statusColor by remember { mutableStateOf(Color(0xFF90CAF9)) }
    var faceDetected by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var attendanceDone by remember { mutableStateOf(false) }
    var lastCapturedFeatures by remember { mutableStateOf<FloatArray?>(null) }

    val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setMinFaceSize(0.2f)
        .build()
    val faceDetector = remember { FaceDetection.getClient(faceDetectorOptions) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Langkah 2 — Verifikasi Wajah", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = actionColor,
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
            StepIndicator(currentStep = 2)

            // QR verified info bar
            if (verifiedEmployee != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1B3A1A)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✅", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "QR Terverifikasi",
                                color = Color(0xFF69F0AE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${verifiedEmployee.employeeName} • ID: ${verifiedEmployee.employeeId}",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (!cameraPermission.status.isGranted) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                        Text("Izinkan Kamera")
                    }
                }
                return@Scaffold
            }

            // Camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
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
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                @androidx.camera.core.ExperimentalGetImage
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && !attendanceDone) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    faceDetector.process(image)
                                        .addOnSuccessListener { faces ->
                                            faceDetected = faces.isNotEmpty()
                                            if (faces.isNotEmpty()) {
                                                lastCapturedFeatures = faceDataHelper.extractFeatures(faces[0])
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
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("FaceCamera", "Error", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Oval face guide
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.Center)
                        .border(
                            width = 3.dp,
                            color = if (attendanceDone) Color(0xFF69F0AE)
                                    else if (faceDetected) Color(0xFFFFEB3B)
                                    else Color(0xFFFF5252),
                            shape = RoundedCornerShape(100.dp)
                        )
                )

                // Top badge
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                    color = when {
                        attendanceDone -> Color(0xFF1B5E20)
                        faceDetected -> Color(0xFFF57F17)
                        else -> Color(0xFFB71C1C)
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = when {
                            attendanceDone -> "✅ Identitas Terkonfirmasi"
                            faceDetected -> "🟡 Wajah Terdeteksi"
                            else -> "❌ Posisikan Wajah di Frame"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom panel
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
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                if (!attendanceDone) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = actionColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Memverifikasi wajah...", color = Color(0xFF90CAF9), fontSize = 13.sp)
                    } else {
                        Button(
                            onClick = {
                                val features = lastCapturedFeatures
                                if (!faceDetected || features == null) {
                                    statusText = "⚠️ Pastikan wajah terlihat jelas di frame"
                                    statusColor = Color(0xFFFFAB40)
                                    return@Button
                                }
                                isProcessing = true

                                // Jika 2FA: cari match yang sesuai dengan employee dari QR
                                val match = faceDataHelper.findBestMatch(features)

                                if (match != null) {
                                    // Cek apakah wajah yang terdeteksi sesuai dengan QR (2FA check)
                                    val isCorrectPerson = verifiedEmployee == null ||
                                        match.employeeId == verifiedEmployee.employeeId

                                    if (isCorrectPerson) {
                                        val timestamp = System.currentTimeMillis()
                                        dbHelper.insertAttendance(
                                            action = action.name,
                                            timestamp = timestamp,
                                            employeeId = match.employeeId,
                                            employeeName = match.employeeName
                                        )
                                        val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                        statusText = "✅ Selamat datang, ${match.employeeName}!\n" +
                                            "Absen $actionLabel: ${fmt.format(Date(timestamp))}\n" +
                                            "2FA Berhasil ✓"
                                        statusColor = Color(0xFF69F0AE)
                                        attendanceDone = true
                                    } else {
                                        // Wajah tidak cocok dengan QR yang discan
                                        statusText = "❌ Wajah tidak sesuai dengan QR!\n" +
                                            "QR: ${verifiedEmployee?.employeeName}\n" +
                                            "Wajah: ${match.employeeName}"
                                        statusColor = Color(0xFFEF5350)
                                    }
                                } else {
                                    statusText = "❌ Wajah tidak dikenali.\nPastikan wajah Anda sudah terdaftar."
                                    statusColor = Color(0xFFEF5350)
                                }
                                isProcessing = false
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                            enabled = faceDetected
                        ) {
                            Text("👤  Konfirmasi Wajah & Absen $actionLabel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else {
                    // Success state
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1B5E20),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔐 2FA Sukses", color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("QR ✓  +  Wajah ✓", color = Color.White, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onSuccess,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                    ) {
                        Text("🏠  Kembali ke Beranda", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
