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
    sessionManager: SessionManager, // Tambahkan SessionManager
    dbHelper: AttendanceDatabaseHelper,
    onRegisterFace: () -> Unit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onHistory: () -> Unit,
    onQRGenerator: () -> Unit,
    onLogout: () -> Unit // Tambahkan callback logout
) {
    // Ambil data nama dari SessionManager
    val userName = remember { sessionManager.getName() ?: "User" }

    // Mengambil data master secara dinamis
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
        // Tombol Logout di pojok kanan atas
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
            // Profil User yang Login
            Text(
                text = "Halo, $userName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "SISTEM ABSENSI",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 8.dp)
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
                        text = if (isDataReady) "🟢 Koneksi Database Aktif" else "🔴 Database Kosong",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDataReady) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Gunakan menu di bawah untuk operasional",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Grid Menu Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRegisterFace,
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                ) {
                    Text("📸 Daftar\nWajah", textAlign = TextAlign.Center, fontSize = 12.sp)
                }
                Button(
                    onClick = onQRGenerator,
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                ) {
                    Text("🔲 Gen\nQR Code", textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Action Buttons
            Button(
                onClick = onCheckIn,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text("📷  ABSEN MASUK", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCheckOut,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("🚪  ABSEN KELUAR", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color.White, Color.White)))
            ) {
                Text("📋  RIWAYAT", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
