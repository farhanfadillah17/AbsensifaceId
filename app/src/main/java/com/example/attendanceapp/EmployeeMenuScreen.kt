package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmployeeMenuScreen(
    sessionManager: SessionManager,
    onFaceRegisterClick: () -> Unit,
    onNfcRegisterClick: () -> Unit,
    onBack: () -> Unit
) {
    val userName = remember { sessionManager.getUserName() ?: "User" }
    val primaryColor = Color(0xFF1A3A8F)
    val secondaryColor = Color(0xFFF5F7FA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Data Karyawan,",
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

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(secondaryColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sub-menu 1: Face Registration
            MasterDataMenuButton(
                title = "REGISTRASI WAJAH",
                subtitle = "Daftarkan sampel wajah baru",
                icon = Icons.Default.Face,
                color = primaryColor,
                contentColor = Color.White,
                onClick = onFaceRegisterClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 2: NFC Registration
            MasterDataMenuButton(
                title = "REGISTRASI KARTU NFC",
                subtitle = "Tulis data karyawan ke kartu",
                icon = Icons.Default.Nfc,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onNfcRegisterClick
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
