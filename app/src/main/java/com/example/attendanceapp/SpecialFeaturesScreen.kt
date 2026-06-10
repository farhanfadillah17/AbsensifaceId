package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    val fcbaUser = sharedPref.getString("fcba", "41") ?: "41"

    // --- DATA SOURCES ---
    val rkhList = remember { dbHelper.getRKHList() }

    // Perbaikan: Ambil data Mandor/Supervisi secara spesifik untuk dropdown Supervisi
    val supervisorOptions = remember { dbHelper.getSupervisors() }

    val jobMasterList = remember { dbHelper.getDropdownData("JOB", fcbaUser) }

    // Filter Karyawan: Hanya yang sudah absen hari ini
    val availableStaff = remember {
        try { dbHelper.getEmployeesAlreadyCheckedIn(fcbaUser) } catch (e: Exception) { emptyList() }
    }

    // --- FORM STATES ---
    var selectedRKH by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedBlock by remember { mutableStateOf("") }
    var selectedJobType by remember { mutableStateOf("") }
    var supervisi1 by remember { mutableStateOf("") }
    var supervisi2 by remember { mutableStateOf("") }
    var supervisi3 by remember { mutableStateOf("") }
    var supervisi4 by remember { mutableStateOf("") }
    var selectedEmployees by remember { mutableStateOf(setOf<String>()) }

    var unit by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var lembur by remember { mutableStateOf("0") }
    var dapatBeras by remember { mutableStateOf(false) }
    var keterangan by remember { mutableStateOf("") }

    // Dropdown Expanded States
    var rkhExpanded by remember { mutableStateOf(false) }
    var blockExpanded by remember { mutableStateOf(false) }
    var jobExpanded by remember { mutableStateOf(false) }
    var s1Expanded by remember { mutableStateOf(false) }
    var s2Expanded by remember { mutableStateOf(false) }
    var s3Expanded by remember { mutableStateOf(false) }
    var s4Expanded by remember { mutableStateOf(false) }

    // Logic: Lokasi diambil otomatis dari RKH
    // Di dalam ProgressFormScreen.kt
// Pastikan kunci yang dipanggil adalah "location"
    val locationCode = selectedRKH?.get("location") ?: ""
    val blockList = remember(locationCode) {
        if (locationCode.isNotEmpty()) dbHelper.getBlocksByLocation(locationCode) else emptyList()
    }

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

            // 1. RKH (Aturan: Ambil Ref dari RKH)
            Text("Referensi RKH", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            RKHDropdownCommon(
                label = "No. RKH",
                // Tampilan di kotak input saat sudah dipilih
                value = if (selectedRKH != null) "RKH:${selectedRKH?.get("no_rkh")} - ${selectedRKH?.get("location")}" else "",

                // Tampilan daftar di dalam dropdown (RKH:1 - AFD-01)
                items = rkhList.map { "RKH:${it["no_rkh"]} - ${it["location"]}" },

                expanded = rkhExpanded,
                onExpandedChange = { rkhExpanded = it }
            ) { selectedString ->
                // Karena item diubah menjadi string gabungan, kita perlu mengambil kembali ID aslinya
                // Kita ambil angka setelah kata "RKH:" dan sebelum " -"
                val rkhIdOnly = selectedString.substringAfter("RKH:").substringBefore(" -")

                selectedRKH = rkhList.find { it["no_rkh"] == rkhIdOnly }

                // Reset data yang bergantung pada RKH lama
                selectedBlock = ""
                rkhExpanded = false
            }

            // 2. Gang, Job, Loc Otomatis (Aturan: Read Only & Ambil dari RKH)
            // 2. Gang, Job, Loc Otomatis (Aturan: Read Only & Ambil dari RKH)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = selectedRKH?.get("gang_code") ?: "",
                    onValueChange = {},
                    label = { Text("Gang Code") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    // PERBAIKAN DI SINI
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Gray,
                        unfocusedIndicatorColor = Color.LightGray
                    )
                )
                OutlinedTextField(
                    value = selectedRKH?.get("job_code") ?: "",
                    onValueChange = {},
                    label = { Text("Job RKH") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    // PERBAIKAN DI SINI
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Gray,
                        unfocusedIndicatorColor = Color.LightGray
                    )
                )
            }
            OutlinedTextField(
                value = locationCode,
                onValueChange = {},
                label = { Text("Location (RKH)") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                // PERBAIKAN DI SINI
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF0F0F0),
                    unfocusedContainerColor = Color(0xFFF0F0F0),
                    focusedIndicatorColor = Color.Gray,
                    unfocusedIndicatorColor = Color.LightGray
                )
            )

            // 3. Blok (Sesuai Location RKH) & Jenis Pekerjaan
            Text("Detail Pekerjaan", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F), modifier = Modifier.padding(top = 16.dp))
            RKHDropdownCommon("Pilih Blok", selectedBlock, blockList, blockExpanded, { blockExpanded = it }) { selectedBlock = it; blockExpanded = false }
            RKHDropdownCommon("Jenis Pekerjaan", selectedJobType, jobMasterList, jobExpanded, { jobExpanded = it }) { selectedJobType = it; jobExpanded = false }

            // 4. Supervisi (Aturan: Ambil dari Master Karyawan)
            Text("Supervisi", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F), modifier = Modifier.padding(top = 16.dp))
            RKHDropdownCommon("Supervisi 1 (Wajib)", supervisi1, supervisorOptions, s1Expanded, { s1Expanded = it }) { supervisi1 = it; s1Expanded = false }
            RKHDropdownCommon("Supervisi 2", supervisi2, supervisorOptions, s2Expanded, { s2Expanded = it }) { supervisi2 = it; s2Expanded = false }
            RKHDropdownCommon("Supervisi 3", supervisi3, supervisorOptions, s3Expanded, { s3Expanded = it }) { supervisi3 = it; s3Expanded = false }
            RKHDropdownCommon("Supervisi 4", supervisi4, supervisorOptions, s4Expanded, { s4Expanded = it }) { supervisi4 = it; s4Expanded = false }

            // 5. Multi Select Karyawan (Aturan: Hanya yang sudah absen)
            Text("Pilih Karyawan", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                Column(Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                    if (availableStaff.isEmpty()) {
                        Text("Tidak ada karyawan yang absen hari ini", fontSize = 12.sp, modifier = Modifier.padding(8.dp), color = Color.Red)
                    }
                    availableStaff.forEach { staff ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selectedEmployees.contains(staff["id"]), onCheckedChange = { isChecked ->
                                val current = selectedEmployees.toMutableSet()
                                if (isChecked) current.add(staff["id"]!!) else current.remove(staff["id"])
                                selectedEmployees = current
                            })
                            Text("${staff["id"]} - ${staff["name"]}", fontSize = 12.sp)
                        }
                    }
                }
            }
            Text("Terpilih: ${selectedEmployees.size} Karyawan", fontSize = 11.sp, color = Color.Gray)

            // 6. Hasil Kerja
            Text("Hasil Kerja", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F), modifier = Modifier.padding(top = 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(output, { output = it }, label = { Text("Output") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(rate, { rate = it }, label = { Text("Rate") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(lembur, { lembur = it }, label = { Text("Lembur") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(16.dp))
                Checkbox(checked = dapatBeras, onCheckedChange = { dapatBeras = it })
                Text("Beras")
            }

            OutlinedTextField(
                value = keterangan,
                onValueChange = { keterangan = it },
                label = { Text("Keterangan") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(80.dp)
            )

            Button(
                onClick = {
                    val targetHK = selectedRKH?.get("jumlah_hk")?.toDoubleOrNull() ?: 0.0
                    if (selectedRKH == null) {
                        Toast.makeText(context, "Pilih No. RKH terlebih dahulu!", Toast.LENGTH_SHORT).show()
                    } else if (selectedEmployees.size > targetHK && targetHK > 0) {
                        Toast.makeText(context, "Jumlah karyawan melebihi HK RKH ($targetHK)!", Toast.LENGTH_SHORT).show()
                    } else if (supervisi1.isEmpty()) {
                        Toast.makeText(context, "Supervisi 1 wajib diisi!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Logika Simpan
                        dbHelper.savePlantationProgress(
                            rkh = selectedRKH!!["no_rkh"] ?: "",
                            category = category,
                            employees = selectedEmployees.toList(),
                            unit = unit.toDoubleOrNull() ?: 0.0,
                            output = output.toDoubleOrNull() ?: 0.0,
                            rate = rate.toDoubleOrNull() ?: 0.0,
                            lembur = lembur.toIntOrNull() ?: 0,
                            beras = if (dapatBeras) 1 else 0
                        )
                        Toast.makeText(context, "Data Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
            ) { Text("SIMPAN") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHDropdownCommon(label: String, value: String, items: List<String>, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onSelect: (String) -> Unit) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = value.ifEmpty { "Pilih $label" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            if (items.isEmpty()) {
                DropdownMenuItem(text = { Text("Tidak ada data") }, onClick = { onExpandedChange(false) })
            }
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelect(item) })
            }
        }
    }
}