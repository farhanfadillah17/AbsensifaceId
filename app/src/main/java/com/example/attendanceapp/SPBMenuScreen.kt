package com.example.attendanceapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight // Ikon pengganti yang lebih stabil
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- DATA MODEL ---
data class SPBCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color,
)

// --- HALAMAN 1: MENU PILIH KATEGORI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SPBMenuScreen(
    onBack: () -> Unit,
    onCategorySelected: (String) -> Unit,
) {
    val categories = listOf(
        SPBCategory("PENGANGKUTAN LANGSUNG", Icons.Default.LocalShipping, Color(0xFF2E7D32)),
        SPBCategory("PENGANGKUTAN TIDAK LANGSUNG", Icons.AutoMirrored.Filled.AltRoute, Color(0xFF1565C0))
    )

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PILIH JENIS PENGANGKUTAN", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Silahkan pilih tipe pengangkutan:", fontSize = 14.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categories) { category ->
                    ElevatedCard(
                        onClick = { onCategorySelected(category.name) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(56.dp), color = category.color.copy(alpha = 0.1f), shape = CircleShape) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(category.icon, null, modifier = Modifier.size(28.dp), tint = category.color)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(category.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 14.sp)

                            // Perbaikan ikon di sini:
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- HALAMAN 2: FORM INPUT SPB ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SPBFormScreen(
    category: String,
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    var noPlat by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var afdeling by remember { mutableStateOf("") }
    var blok by remember { mutableStateOf("") }
    var estimasiJanjang by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FORM SPB - $category", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Informasi Pengangkutan", fontWeight = FontWeight.Bold, color = Color.Gray)
            OutlinedTextField(value = noPlat, onValueChange = { noPlat = it }, label = { Text("Nomor Plat Kendaraan") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = driverName, onValueChange = { driverName = it }, label = { Text("Nama Sopir") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            HorizontalDivider()
            Text(text = "Lokasi & Estimasi", fontWeight = FontWeight.Bold, color = Color.Gray)
            OutlinedTextField(value = afdeling, onValueChange = { afdeling = it }, label = { Text("Afdeling") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = blok, onValueChange = { blok = it }, label = { Text("Blok") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = estimasiJanjang, onValueChange = { estimasiJanjang = it }, label = { Text("Estimasi Jumlah Janjang") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onSuccess() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SIMPAN SPB", fontWeight = FontWeight.Bold)
            }
        }
    }
}
