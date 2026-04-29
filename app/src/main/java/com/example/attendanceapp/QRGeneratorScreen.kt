package com.example.attendanceapp

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRGeneratorScreen(
    faceDataHelper: FaceDataHelper,
    onBack: () -> Unit
) {
    val profiles = remember { faceDataHelper.getAllProfiles() }
    var selectedProfile by remember { mutableStateOf<FaceDataHelper.FaceProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Code Karyawan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00695C),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(padding)
        ) {
            if (profiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada karyawan terdaftar.\nDaftarkan wajah terlebih dahulu.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Tap kartu untuk tampilkan QR Code",
                            color = Color(0xFF78909C),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(profiles) { profile ->
                        EmployeeQRCard(profile = profile) {
                            selectedProfile = profile
                        }
                    }
                }
            }
        }
    }

    // Dialog QR Code
    selectedProfile?.let { profile ->
        QRDialog(profile = profile, onDismiss = { selectedProfile = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeQRCard(profile: FaceDataHelper.FaceProfile, onClick: () -> Unit) {
    Card(
        onClick, Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF00897B), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.employeeName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("ID: ${profile.employeeId}", color = Color(0xFF78909C), fontSize = 12.sp)
            }
            Surface(
                color = Color(0xFF00695C),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "🔲 QR",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun QRDialog(profile: FaceDataHelper.FaceProfile, onDismiss: () -> Unit) {
    val qrContent = "EMP_ID:${profile.employeeId}|NAME:${profile.employeeName}"
    val qrBitmap = remember(qrContent) { generateQRBitmap(qrContent) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "QR Code Absensi",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A237E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(profile.employeeName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF37474F))
                Text("ID: ${profile.employeeId}", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(220.dp)
                            .background(Color.White)
                    )
                } else {
                    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        Text("Gagal generate QR", color = Color.Red)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Tunjukkan QR ini saat absensi",
                    color = Color(0xFF78909C),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
                ) { Text("Tutup", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

fun generateQRBitmap(content: String, size: Int = 512): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) { null }
}
