package com.example.attendanceapp

import android.util.Log
import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RegisterFaceScreen(
    dbHelper: AttendanceDatabaseHelper,
    employee: Employee,
    faceDataHelper: FaceDataHelper,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var samplesCount by remember { mutableStateOf(0) }
    val totalSamplesNeeded = 5
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Arahkan wajah ke kamera") }
    var faceDetected by remember { mutableStateOf(false) }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        FaceDetection.getClient(options)
    }

    // Perbaikan pada DisposableEffect
    DisposableEffect(Unit) {
        onDispose {
            // 1. Matikan Executor
            cameraExecutor.shutdown()

            // 2. Tutup Face Detector
            faceDetector.close()

            // 3. PENTING: Matikan kamera secara paksa saat meninggalkan layar
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
                Log.d("RegisterFace", "Kamera berhasil dimatikan (Unbound)")
            } catch (e: Exception) {
                Log.e("RegisterFace", "Gagal mematikan kamera: ${e.message}")
            }
        }
    }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
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

                if (mediaImage != null && samplesCount < totalSamplesNeeded && !isProcessing) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    faceDetector.process(image)
                        .addOnSuccessListener { faces ->
                            faceDetected = faces.isNotEmpty()
                            if (faces.isNotEmpty()) {
                                val face = faces[0]
                                val faceWidth = face.boundingBox.width()
                                val frameWidth = mediaImage.width
                                val faceSizeRatio = faceWidth.toFloat() / frameWidth.toFloat()

                                if (faceSizeRatio > 0.25) {
                                    isProcessing = true
                                    val instruction = when(samplesCount) {
                                        0 -> "Lihat lurus ke kamera"
                                        1 -> "Miringkan sedikit ke kanan"
                                        2 -> "Miringkan sedikit ke kiri"
                                        3 -> "Tundukkan kepala sedikit"
                                        else -> "Angkat kepala sedikit"
                                    }
                                    statusText = "Tahan... $instruction"

                                    scope.launch {
                                        delay(1500)
                                        val features = faceDataHelper.extractFeatures(face)
                                        // PERBAIKAN 1: Menggunakan fccode dan fcba sesuai struktur baru
                                        val success = faceDataHelper.saveFaceSample(
                                            fccode = employee.fccode,
                                            fcba = employee.fcba,
                                            features = features
                                        )
                                        if (success) {
                                            samplesCount++
                                            statusText = "Berhasil ($samplesCount/$totalSamplesNeeded)"
                                            delay(1000)
                                        }
                                        isProcessing = false
                                    }
                                } else {
                                    statusText = "Dekatkan wajah ke kamera"
                                }
                            } else {
                                statusText = "Wajah tidak terdeteksi"
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                    preview,
                    imageAnalysis
                )
                cameraControl = camera.cameraControl
                cameraControl?.enableTorch(isFlashOn)
            } catch (e: Exception) {
                Log.e("RegisterFace", "Error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrasi Wajah (2/2)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A237E),
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
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Karyawan: ${employee.name}", color = Color.White, fontWeight = FontWeight.Bold)
                    // PERBAIKAN 2: Mengganti employee.id menjadi employee.fccode
                    Text("ID: ${employee.fccode}", color = Color(0xFF90CAF9), fontSize = 12.sp)
                }
            }

            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                if (cameraPermission.status.isGranted) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(4.dp, if (faceDetected) Color(0xFF64FFDA) else Color.Gray, CircleShape),
                        update = {
                            cameraControl?.enableTorch(isFlashOn)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                                CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                            isFlashOn = false
                        },
                        modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch", tint = Color.White)
                    }

                    if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        IconButton(
                            onClick = { isFlashOn = !isFlashOn },
                            modifier = Modifier.background(
                                if (isFlashOn) Color.Yellow.copy(0.5f) else Color.Black.copy(0.5f),
                                CircleShape
                            )
                        ) {
                            Icon(
                                if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = statusText,
                color = if (faceDetected) Color(0xFF64FFDA) else Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { samplesCount.toFloat() / totalSamplesNeeded.toFloat() },
                modifier = Modifier.fillMaxWidth(0.7f).height(8.dp),
                color = Color(0xFF3F51B5),
                trackColor = Color.Gray
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onSuccess,
                enabled = samplesCount >= totalSamplesNeeded,
                modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                Text(
                    if (samplesCount >= totalSamplesNeeded) "SELESAIKAN PENDAFTARAN"
                    else "MENGAMBIL DATA... ($samplesCount/$totalSamplesNeeded)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}