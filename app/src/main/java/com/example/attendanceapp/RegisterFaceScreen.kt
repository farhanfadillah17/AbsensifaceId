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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RegisterFaceScreen(
    faceDataHelper: FaceDataHelper,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var employeeId by remember { mutableStateOf("") }
    var employeeName by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Arahkan wajah ke kamera lalu tekan Ambil Foto") }
    var statusColor by remember { mutableStateOf(Color.White) }
    var capturedFeatures by remember { mutableStateOf<FloatArray?>(null) }
    var faceDetected by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var registeredProfiles by remember { mutableStateOf(faceDataHelper.getAllProfiles()) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.2f)
        .build()
    val faceDetector = remember { FaceDetection.getClient(faceDetectorOptions) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Wajah", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B5E),
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
                        Text("Izin kamera diperlukan", color = Color.White, fontSize = 16.sp)
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
                    .height(360.dp)
            ) {
                var previewUseCase by remember { mutableStateOf<Preview?>(null) }
                var imageAnalysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }

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
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    faceDetector.process(image)
                                        .addOnSuccessListener { faces ->
                                            faceDetected = faces.isNotEmpty()
                                            if (faces.isNotEmpty()) {
                                                val face = faces[0]
                                                capturedFeatures = faceDataHelper.extractFeatures(face)
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
                                Log.e("Camera", "Bind failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Face detection overlay
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .align(Alignment.Center)
                        .border(
                            width = 3.dp,
                            color = if (faceDetected) Color(0xFF69F0AE) else Color(0xFFFF5252),
                            shape = RoundedCornerShape(120.dp)
                        )
                )

                // Detection badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                    color = if (faceDetected) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (faceDetected) "✅ Wajah Terdeteksi" else "❌ Tidak Ada Wajah",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom panel
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!showForm) {
                    Text(
                        text = statusText,
                        color = statusColor.takeIf { it != Color.White } ?: Color(0xFFB0BEC5),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            if (faceDetected && capturedFeatures != null) {
                                showForm = true
                                statusText = "Wajah berhasil ditangkap. Isi data karyawan."
                            } else {
                                statusText = "⚠️ Arahkan wajah ke kamera terlebih dahulu!"
                                statusColor = Color(0xFFFFAB40)
                            }
                        },
                        enabled = faceDetected,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                    ) {
                        Text("📸  Ambil Foto Wajah", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    // Form input data karyawan
                    Text(
                        "Isi Data Karyawan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = employeeId,
                        onValueChange = { employeeId = it },
                        label = { Text("ID Karyawan", color = Color(0xFF90CAF9)) },
                        placeholder = { Text("Contoh: EMP001", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = employeeName,
                        onValueChange = { employeeName = it },
                        label = { Text("Nama Karyawan", color = Color(0xFF90CAF9)) },
                        placeholder = { Text("Contoh: Budi Santoso", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showForm = false },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("Batal") }

                        Button(
                            onClick = {
                                if (employeeId.isBlank() || employeeName.isBlank()) {
                                    statusText = "⚠️ ID dan Nama tidak boleh kosong!"
                                    statusColor = Color(0xFFFFAB40)
                                    showForm = false
                                } else {
                                    val id = employeeId.trim().ifEmpty { UUID.randomUUID().toString().take(8) }
                                    faceDataHelper.saveFaceProfile(id, employeeName.trim(), capturedFeatures!!)
                                    registeredProfiles = faceDataHelper.getAllProfiles()
                                    statusText = "✅ Wajah ${employeeName.trim()} berhasil didaftarkan!"
                                    statusColor = Color(0xFF69F0AE)
                                    employeeId = ""
                                    employeeName = ""
                                    capturedFeatures = null
                                    showForm = false
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                        ) { Text("Simpan", fontWeight = FontWeight.Bold) }
                    }
                }

                // Daftar wajah terdaftar
                if (registeredProfiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Wajah Terdaftar:", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    registeredProfiles.forEach { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(profile.employeeName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("ID: ${profile.employeeId}", color = Color.Gray, fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = {
                                    faceDataHelper.deleteProfile(profile.employeeId)
                                    registeredProfiles = faceDataHelper.getAllProfiles()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
                            ) { Text("Hapus") }
                        }
                    }
                }
            }
        }
    }
}
