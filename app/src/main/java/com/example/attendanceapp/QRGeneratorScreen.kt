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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    dbHelper: AttendanceDatabaseHelper,
    onBack: () -> Unit
) {
    val profiles = remember { dbHelper.getAllEmployees() }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }

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
                        "Belum ada karyawan terdaftar.\nSilahkan tambah karyawan terlebih dahulu.",
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
                            text = "Pilih karyawan untuk menampilkan QR Code",
                            color = Color(0xFF78909C),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(profiles) { employee ->
                        EmployeeQRCard(employee = employee) {
                            selectedEmployee = employee
                        }
                    }
                }
            }
        }
    }

    selectedEmployee?.let { employee ->
        QRDialog(employee = employee, onDismiss = { selectedEmployee = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeQRCard(employee: Employee, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(Color(0xFF00897B), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 22.sp) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("ID: ${employee.id}", color = Color(0xFF78909C), fontSize = 12.sp)
            }
            Surface(color = Color(0xFF00695C), shape = RoundedCornerShape(8.dp)) {
                Text(
                    "LIHAT QR",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun QRDialog(employee: Employee, onDismiss: () -> Unit) {
    // PERBAIKAN: Format QR Content sekarang HANYA berisi ID saja
    val qrContent = employee.id
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
                Text("QR Code Absensi", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1A237E))
                Spacer(modifier = Modifier.height(8.dp))
                Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF37474F))
                Text("ID Karyawan: ${employee.id}", color = Color.Gray, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(20.dp))

                if (qrBitmap != null) {
                    Surface(
                        modifier = Modifier.padding(8.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 2.dp
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(220.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Gunakan kode ini untuk melakukan scan\npada mesin absensi.",
                    color = Color(0xFF78909C),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
                ) { Text("TUTUP", fontWeight = FontWeight.Bold) }
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
    } catch (e: Exception) {
        null
    }
}