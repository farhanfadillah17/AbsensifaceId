package com.example.attendanceapp

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    // State Input Form
    var block by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Proses") }
    val statusOptions = listOf("Belum", "Proses", "Selesai")

    // State untuk memicu refresh list riwayat secara otomatis
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Explicitly define type to avoid "Cannot infer type" error
    val history: List<Map<String, String>> = remember(empId, refreshTrigger) {
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

            OutlinedTextField(
                value = block,
                onValueChange = { block = it },
                label = { Text("Blok / Lokasi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = result,
                onValueChange = { result = it },
                label = { Text("Hasil Kerja (Contoh: 50 Pokok)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Pekerjaan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(16.dp))

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
                            block = ""
                            result = ""
                            notes = ""
                            selectedStatus = "Proses"
                            refreshTrigger++
                        } else {
                            Toast.makeText(context, "Gagal Simpan ke Database", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Blok dan Hasil wajib diisi", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SIMPAN PROGRESS", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(30.dp))
            HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Riwayat Pekerjaan Terbaru",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1A3A8F)
            )
            Spacer(Modifier.height(8.dp))

            if (history.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AncakPanenScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
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
                    if (block.isNotBlank()) {
                        dbHelper.saveAncakPanen(empId, block, "GOOD", notes)
                        Toast.makeText(context, "Berhasil simpan ancak", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } else {
                        Toast.makeText(context, "Blok harus diisi", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
            ) {
                Text("SIMPAN KE HARVESTING QUALITY")
            }
        }
    }
}

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
                onValueChange = { if (it.all { char -> char.isDigit() }) treeCount = it },
                label = { Text("Jumlah Pokok Sampel") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = ripeBunches,
                onValueChange = { if (it.all { char -> char.isDigit() }) ripeBunches = it },
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
                    onNavigateToRKH()
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5))
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