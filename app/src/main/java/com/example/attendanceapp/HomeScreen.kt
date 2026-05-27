package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    onLogout: () -> Unit,
    // TAMBAHKAN DUA BARIS DI BAWAH INI
    onFeature2: () -> Unit,
    onFeature3: () -> Unit
) {
    // Ambil data nama dari SessionManager
    val userName = remember { sessionManager.getUserName() ?: "User" }

    // Definisi Warna Tema
    val primaryColor = Color(0xFF1A3A8F)
    val secondaryColor = Color(0xFFF5F7FA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Latar belakang Putih
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Nama & Tombol Logout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selamat Datang,",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = userName.uppercase(),
                        color = primaryColor, // Nama berwarna Indigo
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.background(secondaryColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = primaryColor)
                }
            }

            // Status Card telah dihapus untuk tampilan yang lebih simpel

            Spacer(modifier = Modifier.height(40.dp))

            // TOMBOL UTAMA: Absen Masuk
            MainActionButton(
                title = "ABSEN MASUK",
                subtitle = "Klik saat memulai jam kerja",
                icon = Icons.Default.Login,
                color = primaryColor,
                contentColor = Color.White,
                onClick = onCheckIn
            )

            Spacer(modifier = Modifier.height(16.dp))

            // TOMBOL UTAMA: Absen Keluar
            MainActionButton(
                title = "ABSEN KELUAR",
                subtitle = "Klik saat pulang kerja",
                icon = Icons.Default.Logout,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onCheckOut
            )

            Spacer(modifier = Modifier.weight(1f))

            // MENU BAWAH: Riwayat & Daftar Wajah
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    title = "Riwayat",
                    icon = Icons.Default.History,
                    modifier = Modifier.weight(1f),
                    onClick = onHistory
                )
                SecondaryButton(
                    title = "Daftar Wajah",
                    icon = Icons.Default.Face,
                    modifier = Modifier.weight(1f),
                    onClick = onRegisterFace
                )
            }
        }
    }
}

@Composable
fun MainActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    contentColor: Color,
    isOutline: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .then(
                if (isOutline) Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isOutline) 0.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                color = if (isOutline) Color(0xFFF5F7FA) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(text = subtitle, fontSize = 12.sp, color = contentColor.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun SecondaryButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A3A8F)),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFE0E0E0)))
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}