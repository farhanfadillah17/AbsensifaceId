package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FruitCountingScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    fcba: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    // 1. Ambil FCBA dari Shared Preferences
    val fcbaUser = sharedPref.getString("fcba", "41") ?: "41"

    // Form States
    var selectedRKH by remember { mutableStateOf<Map<String, String>?>(null) }
    var supervisi1 by remember { mutableStateOf("") }
    var supervisi2 by remember { mutableStateOf("") }
    var supervisi3 by remember { mutableStateOf("") }
    var supervisi4 by remember { mutableStateOf("") }
    var selectedWorkers by remember { mutableStateOf(setOf<String>()) }
    var unit by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") } // Tambahkan Rate
    var selectedTPH by remember { mutableStateOf("") }
    var isBeras by remember { mutableStateOf(false) }
    var lembur by remember { mutableStateOf("0") }

    // Master Data Load
    val rkhList = remember { dbHelper.getRKHList() }
    val supervisorOptions = remember { dbHelper.getSupervisors() }
    val presentWorkers = remember { dbHelper.getEmployeesAlreadyCheckedIn(fcbaUser) }

    // TPH dinamis berdasarkan Location RKH
    val locationCodeFromRKH = selectedRKH?.get("location") ?: ""
    // Perbaiki baris yang error menjadi seperti ini:
    val tphList = remember(locationCodeFromRKH) {
        if (locationCodeFromRKH.isNotEmpty()) {
            dbHelper.getTPHByLocation(locationCodeFromRKH)
        } else {
            emptyList<String>() // Tambahkan <String> di sini
        }
    }


    // Warna untuk Field Read Only
    val readOnlyColors = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF0F0F0),
        unfocusedContainerColor = Color(0xFFF0F0F0),
        disabledContainerColor = Color(0xFFF0F0F0)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perhitungan Buah", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FCBA (Read Only)
            OutlinedTextField(value = fcbaUser, onValueChange = {}, label = { Text("FCBA") }, readOnly = true, modifier = Modifier.fillMaxWidth(), colors = readOnlyColors)

            // 1. Dropdown RKH (Format RKH:1 - AFD-01)
            RKHDropdownMap(
                label = "No RKH",
                selected = if (selectedRKH != null) "RKH:${selectedRKH?.get("no_rkh")} - ${selectedRKH?.get("location")}" else "",
                options = rkhList
            ) {
                selectedRKH = it
                selectedTPH = "" // Reset TPH jika RKH berubah
            }

            // 2. Gang Code & Location (Read Only dari RKH)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = selectedRKH?.get("gang_code") ?: "", onValueChange = {}, label = { Text("Gang Code") }, modifier = Modifier.weight(1f), readOnly = true, colors = readOnlyColors)
                OutlinedTextField(value = selectedRKH?.get("location") ?: "", onValueChange = {}, label = { Text("Location") }, modifier = Modifier.weight(1f), readOnly = true, colors = readOnlyColors)
            }

            // 3. Supervisi 1 - 4
            Text("Personil Supervisi (Master Karyawan)", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            RKHDropdownSimple("Supervisi 1 (Wajib)", supervisi1, supervisorOptions) { supervisi1 = it }
            RKHDropdownSimple("Supervisi 2 (Hitung Buah)", supervisi2, supervisorOptions) { supervisi2 = it }
            RKHDropdownSimple("Supervisi 3", supervisi3, supervisorOptions) { supervisi3 = it }
            RKHDropdownSimple("Supervisi 4", supervisi4, supervisorOptions) { supervisi4 = it }

            // 4. Karyawan (Hanya yang sudah absen)
            Text("Pilih Karyawan (Sudah Absen)", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                Column(Modifier.padding(8.dp)) {
                    if (presentWorkers.isEmpty()) {
                        Text("Belum ada karyawan yang absen hari ini", color = Color.Red, fontSize = 12.sp)
                    }
                    presentWorkers.forEach { staff ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selectedWorkers.contains(staff["id"]), onCheckedChange = { isChecked ->
                                val current = selectedWorkers.toMutableSet()
                                if (isChecked) current.add(staff["id"]!!) else current.remove(staff["id"])
                                selectedWorkers = current
                            })
                            Text("${staff["id"]} - ${staff["name"]}", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Info Validasi HK
            val maxHk = selectedRKH?.get("jumlah_hk")?.toDoubleOrNull() ?: 0.0
            Text(
                text = "Terpilih: ${selectedWorkers.size} Karyawan (Target HK RKH: $maxHk)",
                fontSize = 11.sp,
                color = if (selectedWorkers.size > maxHk && maxHk > 0) Color.Red else Color.Gray
            )

            // 5. Unit, Output, Rate
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = output, onValueChange = { output = it }, label = { Text("Output") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("Rate") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }

            // 6. TPH (Berdasarkan Location RKH)
            RKHDropdownSimple("Pilih TPH", selectedTPH, tphList) { selectedTPH = it }

            // 7. Beras & Lembur
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isBeras, onCheckedChange = { isBeras = it })
                Text("Beras")
                Spacer(Modifier.width(16.dp))
                OutlinedTextField(value = lembur, onValueChange = { lembur = it }, label = { Text("Lembur (Jam)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // 8. Tombol Simpan
            Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick = {
                    when {
                        selectedRKH == null -> Toast.makeText(context, "Pilih No RKH!", Toast.LENGTH_SHORT).show()
                        supervisi1.isEmpty() -> Toast.makeText(context, "Supervisi 1 Wajib!", Toast.LENGTH_SHORT).show()
                        selectedWorkers.isEmpty() -> Toast.makeText(context, "Pilih minimal 1 karyawan!", Toast.LENGTH_SHORT).show()
                        selectedWorkers.size > maxHk && maxHk > 0 -> Toast.makeText(context, "Jumlah karyawan melebihi HK RKH!", Toast.LENGTH_SHORT).show()
                        selectedTPH.isEmpty() -> Toast.makeText(context, "Pilih TPH!", Toast.LENGTH_SHORT).show()
                        else -> {
                            dbHelper.saveFruitCalculation(
                                fcba = fcbaUser,
                                rkh = selectedRKH!!["no_rkh"] ?: "",
                                gang = selectedRKH!!["gang_code"] ?: "",
                                supervisors = listOf(supervisi1, supervisi2, supervisi3, supervisi4),
                                employees = selectedWorkers.toList(),
                                location = locationCodeFromRKH,
                                tph = selectedTPH,
                                unit = unit.toDoubleOrNull() ?: 0.0,
                                output = output.toDoubleOrNull() ?: 0.0,
                                rate = rate.toDoubleOrNull() ?: 0.0,
                                beras = if (isBeras) 1 else 0,
                                lembur = lembur.toDoubleOrNull() ?: 0.0
                            )
                            Toast.makeText(context, "Data Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("SIMPAN DATA")
            }

            // 9. Tombol NFC
            Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick= {
                    if (selectedRKH == null || selectedTPH.isEmpty()) {
                        Toast.makeText(context, "Lengkapi data sebelum transfer NFC", Toast.LENGTH_SHORT).show()
                    } else {
                        val jsonData = prepareNfcJson(
                            selectedRKH, supervisi1, supervisi2, supervisi3, supervisi4,
                            selectedWorkers.toList(), unit, output, selectedTPH, isBeras, lembur
                        )
                        android.util.Log.d("NFC_PAYLOAD", jsonData)
                        Toast.makeText(context, "NFC Ready: Tempelkan Tag Kerani Kirim", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Icon(Icons.Default.Nfc, null)
                Spacer(Modifier.width(8.dp))
                Text("TRANSFER KE NFC")
            }
        }
    }
}

// Fungsi Bantu Dropdown Map (RKH)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHDropdownMap(label: String, selected: String, options: List<Map<String, String>>, onSelect: (Map<String, String>) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text("RKH:${item["no_rkh"]} - ${item["location"]}") },
                    onClick = { onSelect(item); expanded = false }
                )
            }
        }
    }
}

// Fungsi Bantu Dropdown List String (TPH / Supervisi)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHDropdownSimple(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelect(item); expanded = false })
            }
        }
    }
}

fun prepareNfcJson(
    rkh: Map<String, String>?,
    s1: String, s2: String, s3: String, s4: String,
    workers: List<String>,
    unit: String, output: String, tph: String, beras: Boolean, lembur: String
): String {
    val data = mutableMapOf<String, Any>()
    data["id"] = System.currentTimeMillis()
    data["noRkh"] = rkh?.get("no_rkh") ?: ""
    data["gangCode"] = rkh?.get("gang_code") ?: ""
    data["location"] = rkh?.get("location") ?: ""
    data["tph"] = tph
    data["unit"] = unit
    data["output"] = output
    data["beras"] = if (beras) 1 else 0
    data["lembur"] = lembur
    data["supervisors"] = listOf(s1, s2, s3, s4)
    data["workers"] = workers
    return Gson().toJson(data)
}