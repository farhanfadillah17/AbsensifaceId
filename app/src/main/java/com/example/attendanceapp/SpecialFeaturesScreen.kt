package com.example.attendanceapp // 1. Pastikan package ini sesuai
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressFormScreen(
    category: String,
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // State Input Form
    var block by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Proses") }
    val statusOptions = listOf("Belum", "Proses", "Selesai")

    // State untuk memicu refresh list riwayat secara otomatis
    var refreshTrigger by remember { mutableStateOf(0) }

    // Mengambil riwayat dari database (akan terupdate jika refreshTrigger berubah)
    val history = remember(empId, refreshTrigger) {
        dbHelper.getWorkProgressHistory(empId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress $category", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Input Detail Pekerjaan",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1A3A8F)
            )
            Spacer(Modifier.height(16.dp))

            // --- INPUT BLOK ---
            OutlinedTextField(
                value = block,
                onValueChange = { block = it },
                label = { Text("Blok / Lokasi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // --- INPUT HASIL ---
            OutlinedTextField(
                value = result,
                onValueChange = { result = it },
                label = { Text("Hasil Kerja (Contoh: 50 Pokok)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // --- INPUT CATATAN ---
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Pekerjaan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(16.dp))

            // --- PILIHAN STATUS ---
            Text("Status Progress:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                statusOptions.forEach { status ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (status == selectedStatus),
                            onClick = { selectedStatus = status }
                        )
                        Text(status, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- TOMBOL SIMPAN ---
            Button(
                onClick = {
                    if (block.isNotBlank() && result.isNotBlank()) {
                        val isSaved = dbHelper.saveWorkProgress(
                            empId = empId,
                            category = category,
                            block = block,
                            result = result,
                            notes = notes,
                            status = selectedStatus
                        )

                        if (isSaved) {
                            Toast.makeText(context, "Data Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                            // Reset Form
                            block = ""
                            result = ""
                            notes = ""
                            selectedStatus = "Proses"
                            // Trigger refresh list riwayat
                            refreshTrigger++
                        } else {
                            Toast.makeText(context, "Gagal Simpan ke Database", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Blok dan Hasil wajib diisi", Toast.LENGTH_SHORT).show()
                    }
                }, // Perbaikan: Kurung tutup ini harus ada
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                Text("SIMPAN PROGRESS", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(30.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            // --- BAGIAN RIWAYAT PEKERJAAN ---
            Text(
                text = "Riwayat Pekerjaan Terbaru",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1A3A8F)
            )
            Spacer(Modifier.height(8.dp))

            if (history.isEmpty()) {
                Box(Modifier
                    .fillMaxWidth()
                    .padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada riwayat untuk ID: $empId", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                history.take(10).forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item["category"] ?: "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1A3A8F)
                                )
                                Text(
                                    text = item["status"] ?: "",
                                    color = if (item["status"] == "Selesai") Color(0xFF2E7D32) else Color(0xFFE65100),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Lokasi: ${item["block"]}", fontSize = 12.sp)
                            Text("Hasil: ${item["result"]}", fontSize = 12.sp)
                            Text(
                                text = "Waktu: ${item["date"]}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}



// --- SCREEN ANCAK PANEN (Direct to Harvesting Quality) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AncakPanenScreen(
    dbHelper: AttendanceDatabaseHelper, // Sekarang referensi ini akan terbaca jika dalam package yang sama
    empId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    var block by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Harvesting Quality (Ancak)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(20.dp)) {
            OutlinedTextField(
                value = block,
                onValueChange = { block = it },
                label = { Text("Kode Blok") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Kualitas") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    dbHelper.saveAncakPanen(empId, block, "GOOD", notes)
                    onSuccess()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
            ) {
                Text("SIMPAN KE HARVESTING QUALITY")
            }
        }
    }
}



// --- SCREEN AKP (Angka Kerapatan Panen) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AKPScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    onBack: () -> Unit,
    onNavigateToRKH: () -> Unit,
) {
    var block by remember { mutableStateOf("") }
    var treeCount by remember { mutableStateOf("") }
    var ripeBunches by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Angka Kerapatan Panen", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(24.dp)) {
            OutlinedTextField(
                value = block,
                onValueChange = { block = it },
                label = { Text("Kode Blok") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = treeCount,
                onValueChange = { if (it.all { c -> c.isDigit() }) treeCount = it },
                label = { Text("Jumlah Pokok Sampel") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = ripeBunches,
                onValueChange = { if (it.all { c -> c.isDigit() }) ripeBunches = it },
                label = { Text("Jumlah Janjang Matang") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val trees = treeCount.toDoubleOrNull() ?: 1.0
                    val bunches = ripeBunches.toDoubleOrNull() ?: 0.0
                    val density = bunches / trees

                    dbHelper.saveAKP(empId, block, bunches.toInt(), density)
                    onNavigateToRKH() // Langsung tampil ke RKH
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
            ) {
                Text("HITUNG & LIHAT RKH")
            }
        }
    }
}

// --- SCREEN RKH (Rencana Kerja Harian) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rencana Kerja Harian (RKH)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(24.dp)) {
            Text(text = "Daftar Rencana Kerja", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))

            // Contoh Tampilan List RKH (Bisa dikembangkan dengan mengambil data dari DB)
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Kegiatan: Panen Kelapa Sawit", fontWeight = FontWeight.Bold)
                    Text("Lokasi: Blok yang dihitung AKP")
                    Text("Target: Berdasarkan Kerapatan Panen")
                    Spacer(Modifier.height(8.dp))
                    Text("Status: Rencana", color = Color(0xFF1A3A8F), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("KEMBALI KE DASHBOARD")
            }
        }
    }
}