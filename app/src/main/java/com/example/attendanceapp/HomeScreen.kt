package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    faceDataHelper: FaceDataHelper,
    dbHelper: AttendanceDatabaseHelper, // Tambahkan dbHelper untuk cek jumlah karyawan
    onRegisterFace: () -> Unit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onHistory: () -> Unit,
    onQRGenerator: () -> Unit
) {
    // Mengambil data secara dinamis dari database
    val allEmployees = remember { dbHelper.getAllEmployees() }
    val isRegistered = allEmployees.isNotEmpty()
    val profileCount = allEmployees.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1B5E), Color(0xFF1A3A8F), Color(0xFFE8EAF6))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "SISTEM ABSENSI",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 3.sp
            )
            Text(
                text = "QR Code + Face Recognition",
                fontSize = 13.sp,
                color = Color(0xFFB3C5F0),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 2FA badge
            Surface(
                color = Color(0xFF00695C),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                Text(
                    text = "🔐 Autentikasi 2 Faktor (2FA)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Face icon
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRegistered) Color(0xFF1B5E20).copy(alpha = 0.8f)
                        else Color(0xFF283593).copy(alpha = 0.8f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isRegistered) "✅" else "👤", fontSize = 60.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isRegistered) "$profileCount Karyawan Terdaftar" else "Belum ada karyawan terdaftar",
                color = if (isRegistered) Color(0xFF81C784) else Color(0xFFEF9A9A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegistered)
                            "🟢 Sistem Siap Digunakan"
                        else
                            "🔴 Silahkan Daftarkan Karyawan Terlebih Dahulu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isRegistered) Color(0xFF2E7D32) else Color(0xFFC62828),
                        textAlign = TextAlign.Center
                    )
                    if (isRegistered) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Alur: Scan QR → Verifikasi Wajah → Selesai",
                            color = Color(0xFF78909C),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Row: Register + QR Generator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRegisterFace,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                ) {
                    Text("📸 Daftar\nKaryawan", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                Button(
                    onClick = onQRGenerator,
                    enabled = isRegistered,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                ) {
                    Text("🔲 Generator\nQR Code", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Check In
            Button(
                onClick = onCheckIn,
                enabled = isRegistered,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text("📷  ABSEN MASUK", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Check Out
            Button(
                onClick = onCheckOut,
                enabled = isRegistered,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("🚪  ABSEN KELUAR", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // History
            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color.White, Color.White)))
            ) {
                Text("📋  LIHAT RIWAYAT", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}