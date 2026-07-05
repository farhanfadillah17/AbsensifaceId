package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    // onLogout dihapus dari parameter karena tombol ditiadakan
    onFeature2: () -> Unit, // Untuk Transfer Data
    onFeature3: () -> Unit  // Untuk Receive Data
) {
    // Ambil data nama dari SessionManager
    val userName = remember { sessionManager.getUserName() ?: "User" }

    // State untuk mengontrol tampilan menu titik tiga
    var showMenu by remember { mutableStateOf(false) }

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
            // Header: Nama & Menu Titik Tiga
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
                        color = primaryColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Menu Titik Tiga menggantikan Tombol Logout
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.background(secondaryColor, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = primaryColor
                        )
                    }

                    // Di dalam HomeScreen.kt pada bagian DropdownMenu

                    // Di dalam HomeScreen.kt pada bagian DropdownMenu
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Kirim Data") },
                            leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onFeature2() // Ini akan memicu navigasi ke layar Transfer Data
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Terima Data") }, // Ubah label
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onFeature3() // Menjalankan Screen.QR_SCAN dengan action RECEIVE
                            }
                        )
                    }

                }
            }

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

            Spacer(modifier = Modifier.height(16.dp))

            // TOMBOL UTAMA: Absen Masuk
            MainActionButton(
                title = "RIWAYAT ABSENSI",
                subtitle = "Klik untuk lihat data absensi",
                icon = Icons.Default.History,
                color = primaryColor,
                contentColor = Color.White,
                onClick = onHistory
            )

//            Spacer(modifier = Modifier.weight(1f))
//
//            // MENU BAWAH: Riwayat & Daftar Wajah
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 40.dp),
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                SecondaryButton(
//                    title = "Riwayat",
//                    icon = Icons.Default.History,
//                    modifier = Modifier.weight(1f),
//                    onClick = onHistory
//                )
//                SecondaryButton(
//                    title = "Daftar Wajah",
//                    icon = Icons.Default.Face,
//                    modifier = Modifier.weight(1f),
//                    onClick = onRegisterFace
//                )
//            }
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