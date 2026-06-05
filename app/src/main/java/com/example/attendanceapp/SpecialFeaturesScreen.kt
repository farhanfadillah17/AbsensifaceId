package com.example.attendanceapp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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

    // Data Sources
    // Data Sources yang sudah diperbaiki total:
    val rkhList = remember<List<Map<String, String>>> {
        dbHelper.getRKHList()
    }
    val supervisorList = remember<List<Map<String, String>>> {
        dbHelper.getEmployeesByRole("SUPERVISI")
    }
    val fcbaUser = "41"

    val availableStaff = remember {
        // Gunakan try-catch agar jika query gagal, aplikasi tidak force close
        try {
            dbHelper.getEmployeesAlreadyCheckedIn(fcbaUser)
        } catch (e: Exception) {
            emptyList()
        }
    }


    // Form States
    var selectedRKH by remember { mutableStateOf<Map<String, String>?>(null) }
    var supervisi1 by remember { mutableStateOf("") }
    var supervisi2 by remember { mutableStateOf("") }
    var selectedEmployees by remember { mutableStateOf(setOf<String>()) }

    var unit by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var lembur by remember { mutableStateOf("0") }
    var dapatBeras by remember { mutableStateOf(false) }

    // Dropdown Expanded States
    var rkhExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Form $category", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- BAGIAN RKH ---
            Text("Informasi RKH", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            ExposedDropdownMenuBox(
                expanded = rkhExpanded,
                onExpandedChange = { rkhExpanded = !rkhExpanded }
            ) {
                OutlinedTextField(
                    value = selectedRKH?.get("no_rkh") ?: "Pilih No. RKH",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("No. RKH") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rkhExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = rkhExpanded, onDismissRequest = { rkhExpanded = false }) {
                    rkhList.forEach { rkh ->
                        DropdownMenuItem(
                            text = { Text(rkh["no_rkh"] ?: "") },
                            onClick = {
                                selectedRKH = rkh
                                rkhExpanded = false
                            }
                        )
                    }
                }
            }

            // Gang Code & Location (Otomatis & Read Only)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selectedRKH?.get("gang_code") ?: "",
                    onValueChange = {},
                    label = { Text("Gang Code") },
                    readOnly = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = selectedRKH?.get("location") ?: "",
                    onValueChange = {},
                    label = { Text("Location") },
                    readOnly = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- BAGIAN SUPERVISI ---
            Text("Supervisi", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            // Supervisi 1 & 2 (Contoh dropdown sederhana)
            OutlinedTextField(
                value = supervisi1,
                onValueChange = { supervisi1 = it },
                label = { Text("Supervisi 1") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // --- BAGIAN KARYAWAN (Multi Select) ---
            Text("Pilih Karyawan (Hanya yang sudah absen)", fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                    availableStaff.forEach { staff ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedEmployees.contains(staff["id"]),
                                onCheckedChange = { isChecked ->
                                    val current = selectedEmployees.toMutableSet()
                                    if (isChecked) current.add(staff["id"]!!) else current.remove(staff["id"])
                                    selectedEmployees = current
                                }
                            )
                            Text("${staff["id"]} - ${staff["name"]}", fontSize = 12.sp)
                        }
                    }
                }
            }
            Text("Terpilih: ${selectedEmployees.size} Karyawan", fontSize = 11.sp, color = Color.Gray)

            Spacer(Modifier.height(16.dp))

            // --- INPUT NUMERIK ---
            Text("Hasil Kerja", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = output,
                    onValueChange = { output = it },
                    label = { Text("Output") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Rate") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = dapatBeras, onCheckedChange = { dapatBeras = it })
                Text("Beras")
                Spacer(Modifier.width(20.dp))
                OutlinedTextField(
                    value = lembur,
                    onValueChange = { lembur = it },
                    label = { Text("Lembur (Jam)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- BUTTON SIMPAN DENGAN VALIDASI ---
            Button(
                onClick = {
                    try {
                        // 1. Ambil target HK dengan aman
                        val targetHK = selectedRKH?.get("target_hk")?.toIntOrNull() ?: 0
                        val countSelected = selectedEmployees.size

                        when {
                            selectedRKH == null ->
                                Toast.makeText(context, "Pilih No. RKH!", Toast.LENGTH_SHORT).show()

                            selectedEmployees.isEmpty() ->
                                Toast.makeText(context, "Pilih minimal 1 karyawan!", Toast.LENGTH_SHORT).show()

                            // 2. Perbaikan Rule Validasi (Gunakan kurung kurawal ${})
                            targetHK > 0 && countSelected > targetHK -> {
                                Toast.makeText(context, "Karyawan ($countSelected) > Target HK ($targetHK)!", Toast.LENGTH_LONG).show()
                            }

                            unit.isEmpty() || output.isEmpty() ->
                                Toast.makeText(context, "Unit & Output wajib diisi!", Toast.LENGTH_SHORT).show()

                            else -> {
                                // 3. Simpan dengan parameter yang sudah divalidasi
                                val result = dbHelper.savePlantationProgress(
                                    rkh = selectedRKH!!["no_rkh"] ?: "",
                                    category = category,
                                    employees = selectedEmployees.toList(),
                                    unit = unit.toDoubleOrNull() ?: 0.0,
                                    output = output.toDoubleOrNull() ?: 0.0,
                                    rate = rate.toDoubleOrNull() ?: 0.0,
                                    lembur = lembur.toIntOrNull() ?: 0,
                                    beras = if (dapatBeras) 1 else 0
                                )

                                if (result != -1L) {
                                    Toast.makeText(context, "Berhasil Simpan $category", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                } else {
                                    Toast.makeText(context, "Gagal simpan ke database!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Tampilkan error jika terjadi crash saat proses klik
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
            ) {
                Text("SIMPAN PROGRESS OFFLINE", fontWeight = FontWeight.Bold)
            }
        }
    }
}