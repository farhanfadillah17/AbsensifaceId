package com.example.attendanceapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.util.concurrent.Executors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceTransferScreen(
    dbHelper: AttendanceDatabaseHelper,
    userRole: String,
    onBack: () -> Unit,
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Otomatis buat QR saat layar dibuka (Semua Role)
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val data = dbHelper.getAttendanceForQRCode()
            if (data.isNotEmpty()) {
                qrBitmap = generateQRCode(data)
            }
        } catch (e: Exception) {
            Log.e("QR_ERROR", "Gagal: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Data Absensi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A3A8F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F7FA))
        ) {
            // --- TAMPILAN QR (KIRIM DATA) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFF1A3A8F))
                    Spacer(Modifier.height(16.dp))
                    Text("Mengambil data...")
                } else if (qrBitmap != null) {
                    Text(
                        "SCAN QR INI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF1A3A8F)
                    )
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        shadowElevation = 10.dp,
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White
                    ) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(320.dp)
                                .padding(20.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Tunjukkan ke HP penerima untuk transfer data",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                } else {
                    // Jika data kosong
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("Tidak ada data untuk dikirim hari ini", color = Color.Red)
                }


            }
        }
    }
}


@Composable
fun TransferMenu(userRole: String, onReceiveClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tombol Kirim Data Mandor SUDAH DIHAPUS dari sini agar tidak muncul lagi

        if (userRole == "KERANI" || userRole == "ADMIN") {
            Text("Mode Penerima Data", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            Button(
                onClick = onReceiveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Icon(Icons.Default.QrCodeScanner, null)
                Spacer(Modifier.width(12.dp))
                Text("TERIMA DATA (KERANI)", fontWeight = FontWeight.Bold)
            }
        } else if (userRole == "MANDOR") {
            // Pesan fallback jika otomatisasi gagal
            Text("Menunggu data...")
        }
    }
}



// ─── GENERATOR QR CODE ────────────────────────────────────────────────────────

fun generateQRCode(content: String): Bitmap? {
    return try {
        val size = 512
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) { null }
}

// ─── SCANNER QR CODE (CAMERA X + ML KIT) ──────────────────────────────────────

@Composable
fun QRScannerView(onCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val scanner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let {
                                        onCodeScanned(it)
                                        cameraProvider.unbindAll() // Hentikan kamera setelah scan
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) {
                    Log.e("QRScanner", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}