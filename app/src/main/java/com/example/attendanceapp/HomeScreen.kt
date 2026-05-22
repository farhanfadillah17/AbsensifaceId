package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
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
    sessionManager: SessionManager,
    dbHelper: AttendanceDatabaseHelper,
    onRegisterFace: () -> Unit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onHistory: () -> Unit,
    // onQRGenerator dihapus dari sini
    onLogout: () -> Unit
) {
    // Menggunakan getUserName() agar sinkron dengan SessionManager yang diperbaiki
    val userName = remember { sessionManager.getUserName() ?: "User" }

    val allEmployees = remember { dbHelper.getAllMasterEmployees() }
    val isDataReady = allEmployees.isNotEmpty()
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
        IconButton(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Halo, $userName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "MENU ABSENSI",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Status Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🛡️", fontSize = 50.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$profileCount Data Master Termuat",
                color = if (isDataReady) Color(0xFF81C784) else Color(0xFFEF9A9A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Daftar Wajah (Sekarang tampil penuh/single di barisnya)
            Button(
                onClick = onRegisterFace,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                Text("📸  DAFTAR WAJAH KARYAWAN", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Action Buttons
            Button(
                onClick = onCheckIn,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text("📷  ABSEN MASUK", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCheckOut,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("🚪  ABSEN KELUAR", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(Color.White, Color.White))
                )
            ) {
                Text("📋  LIHAT RIWAYAT", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}