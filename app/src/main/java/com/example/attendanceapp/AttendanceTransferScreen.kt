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
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onBack: () -> Unit
) {
    var showScanner by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Data Lapangan") },
                navigationIcon = {
                    IconButton(onClick = if (showScanner || qrBitmap != null) {
                        { showScanner = false; qrBitmap = null }
                    } else onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F7FA))) {
            when {
                showScanner -> {
                    QRScannerView(
                        onCodeScanned = { result ->
                            dbHelper.receiveTransferredData(result)
                            showScanner = false
                            Toast.makeText(context, "Data Berhasil Diterima!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                qrBitmap != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Scan oleh Kerani", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(16.dp))
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(300.dp).background(Color.White).padding(10.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { qrBitmap = null }) { Text("Tutup QR") }
                    }
                }
                else -> {
                    TransferMenu(
                        userRole = userRole,
                        onSendClick = {
                            val data = dbHelper.getAttendanceForQRCode()
                            if (data.isNotEmpty()) {
                                qrBitmap = generateQRCode(data)
                                Log.e("QR_CODE", data)
                            } else {
                                Toast.makeText(context, "Tidak ada data absen hari ini", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReceiveClick = { showScanner = true }
                    )
                }
            }
        }
    }
}

@Composable
fun TransferMenu(userRole: String, onSendClick: () -> Unit, onReceiveClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userRole == "MANDOR" || userRole == "ADMIN") {
            Button(
                onClick = onSendClick,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Send, null)
                Spacer(Modifier.width(12.dp))
                Text("KIRIM DATA (MANDOR)", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (userRole == "KERANI" || userRole == "ADMIN") {
            Button(
                onClick = onReceiveClick,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Icon(Icons.Default.QrCodeScanner, null)
                Spacer(Modifier.width(12.dp))
                Text("TERIMA DATA (KERANI)", fontWeight = FontWeight.Bold)
            }
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