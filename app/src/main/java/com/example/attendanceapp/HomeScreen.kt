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
    onRegisterFace: () -> Unit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onHistory: () -> Unit
) {
    val isRegistered = remember { faceDataHelper.isRegistered() }
    val profileCount = remember { faceDataHelper.getEmployeeIds().size }

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
                text = "Face Recognition",
                fontSize = 14.sp,
                color = Color(0xFFB3C5F0),
                modifier = Modifier.padding(bottom = 36.dp)
            )

            // Face icon
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRegistered) Color(0xFF1B5E20).copy(alpha = 0.8f)
                        else Color(0xFF283593).copy(alpha = 0.8f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isRegistered) "✅" else "👤", fontSize = 64.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isRegistered) "$profileCount wajah terdaftar" else "Belum ada wajah terdaftar",
                color = if (isRegistered) Color(0xFF81C784) else Color(0xFFEF9A9A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegistered)
                            "🟢 Siap melakukan absensi"
                        else
                            "🔴 Daftarkan wajah terlebih dahulu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isRegistered) Color(0xFF2E7D32) else Color(0xFFC62828),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Register button
            Button(
                onClick = onRegisterFace,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                Text("📸  Daftar / Kelola Wajah", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Check In
            Button(
                onClick = onCheckIn,
                enabled = isRegistered,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text("📷  ABSEN MASUK", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Check Out
            Button(
                onClick = onCheckOut,
                enabled = isRegistered,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("🚪  ABSEN KELUAR", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // History
            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("📋  Lihat Riwayat", fontSize = 14.sp)
            }
        }
    }
}
