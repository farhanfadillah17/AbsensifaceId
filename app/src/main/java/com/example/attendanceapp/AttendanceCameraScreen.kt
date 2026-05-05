package com.example.attendanceapp

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun AttendanceCameraScreen(
    action: AttendanceAction,
    verifiedEmployee: Employee?,
    faceDataHelper: FaceDataHelper,
    dbHelper: AttendanceDatabaseHelper,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // --- STATE ---
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var hasFlash by remember { mutableStateOf(false) }

    var isVerifying by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Arahkan wajah untuk verifikasi") }
    var faceDetected by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        FaceDetection.getClient(options)
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

                // FIX: Gunakan variabel lokal untuk menghindari Smart Cast error
                val currentEmployee = verifiedEmployee

                if (mediaImage != null && !isVerifying && currentEmployee != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                    faceDetector.process(image)
                        .addOnSuccessListener { faces ->
                            faceDetected = faces.isNotEmpty()
                            if (faces.isNotEmpty() && !isVerifying) {
                                isVerifying = true
                                val face = faces[0]

                                scope.launch {
                                    statusText = "Sedang memverifikasi..."
                                    delay(1000)

                                    val currentFeatures = faceDataHelper.extractFeatures(face)

                                    // FIX: Gunakan currentEmployee (non-null)
                                    val isMatched = faceDataHelper.verifyFace(currentEmployee.id, currentFeatures)

                                    if (isMatched) {
                                        statusText = "✅ Verifikasi Berhasil!"
                                        // FIX: Gunakan currentEmployee (non-null)
                                        dbHelper.saveAttendance(currentEmployee.id, action.name)
                                        delay(1500)
                                        onSuccess()
                                    } else {
                                        statusText = "❌ Wajah tidak cocok"
                                        delay(2000)
                                        isVerifying = false
                                        statusText = "Coba arahkan wajah kembali"
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
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                    preview,
                    imageAnalysis
                )
                cameraControl = camera.cameraControl
                hasFlash = camera.cameraInfo.hasFlashUnit()
                cameraControl?.enableTorch(isFlashOn)
            } catch (e: Exception) {
                Log.e("AttendanceCamera", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // --- UI Tetap Sama ---
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1A237E),
                shadowElevation = 8.dp
            ) {
                Column(Modifier.padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)) {
                    Text(
                        "Konfirmasi Wajah: ${verifiedEmployee?.name ?: "Unknown"}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Tujuan: Absen ${if(action == AttendanceAction.CHECK_IN) "Masuk" else "Keluar"}",
                        color = Color(0xFF90CAF9),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(CircleShape)
                    .border(
                        width = 4.dp,
                        color = if (faceDetected) Color(0xFF64FFDA) else Color.Gray,
                        shape = CircleShape
                    )
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize(),
                    update = {
                        cameraControl?.enableTorch(isFlashOn)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = statusText,
                color = if (faceDetected) Color(0xFF64FFDA) else Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 50.dp, start = 40.dp, end = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }

                Button(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                        isFlashOn = false
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f))
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, "Switch", tint = Color.White)
                }

                if (lensFacing == CameraSelector.LENS_FACING_BACK && hasFlash) {
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier.background(
                            if (isFlashOn) Color.Yellow.copy(0.5f) else Color.White.copy(0.1f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            "Flash",
                            tint = if (isFlashOn) Color.Black else Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}