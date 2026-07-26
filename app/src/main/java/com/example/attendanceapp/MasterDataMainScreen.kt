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

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun MasterDataMainScreen(
    sessionManager: SessionManager,
    onEmployeeClick: () -> Unit,
    onFieldClick: () -> Unit,
    onJobClick: () -> Unit,
    onTphClick: () -> Unit,
    onFcbaClick: () -> Unit,
    onMillClick: () -> Unit,
    onVehicleClick: () -> Unit,
    onNurseryClick: () -> Unit,
    onGcClick: () -> Unit,
    onWorkshopClick: () -> Unit,
    onBjrClick: () -> Unit,
    onBack: () -> Unit
) {
    val userName = remember { sessionManager.getUserName() ?: "User" }
    val primaryColor = Color(0xFF1A3A8F) // Biru konsisten dengan SyncScreen
    val secondaryColor = Color(0xFFF5F7FA)
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
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
                        text = "Master Data,",
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

            // Sub-menu 1: Employee
            MasterDataMenuButton(
                title = "DATA KARYAWAN",
                subtitle = "Kelola dan daftar wajah karyawan",
                icon = Icons.Default.Badge,
                color = primaryColor,
                contentColor = Color.White,
                onClick = onEmployeeClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 2: Field
            MasterDataMenuButton(
                title = "DATA FIELD / BLOK",
                subtitle = "Lihat informasi blok dan lahan",
                icon = Icons.Default.Map,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onFieldClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 3: Job
            MasterDataMenuButton(
                title = "DATA PEKERJAAN",
                subtitle = "Daftar jenis pekerjaan (Job)",
                icon = Icons.Default.Work,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onJobClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 4: TPH
            MasterDataMenuButton(
                title = "DATA TPH",
                subtitle = "Daftar Tempat Pengumpulan Hasil",
                icon = Icons.Default.LocationOn,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onTphClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 5: FCBA
            MasterDataMenuButton(
                title = "BUSINESS UNIT (FCBA)",
                subtitle = "Daftar unit bisnis / estate",
                icon = Icons.Default.Business,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onFcbaClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 6: Mill
            MasterDataMenuButton(
                title = "PABRIK / MILL",
                subtitle = "Daftar pabrik kelapa sawit",
                icon = Icons.Default.Factory,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onMillClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 7: Vehicle
            MasterDataMenuButton(
                title = "KENDARAAN / VEHICLE",
                subtitle = "Daftar kendaraan operasional",
                icon = Icons.Default.LocalShipping,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onVehicleClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 8: Nursery
            MasterDataMenuButton(
                title = "NURSERY / BIBITAN",
                subtitle = "Informasi pembibitan tanaman",
                icon = Icons.Default.Grass,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onNurseryClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 9: GC
            MasterDataMenuButton(
                title = "GC MASTER",
                subtitle = "Master data biaya umum",
                icon = Icons.Default.Settings,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onGcClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 10: Workshop
            MasterDataMenuButton(
                title = "WORKSHOP MASTER",
                subtitle = "Master data bengkel",
                icon = Icons.Default.Build,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onWorkshopClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-menu 11: BJR
            MasterDataMenuButton(
                title = "PERIODE BJR",
                subtitle = "Daftar Berat Janjang Rata-rata",
                icon = Icons.Default.Analytics,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                onClick = onBjrClick
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MasterDataMenuButton(
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
