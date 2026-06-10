package com.example.attendanceapp

import android.app.DatePickerDialog
import android.content.Context // Tambahkan ini
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
import java.util.*
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FruitCountingScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,        // <--- Tambahkan ini
    fcba: String,         // <--- Tambahkan ini
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    // 1. Ambil FCBA dari Shared Preferences (Read Only)
    val fcba = sharedPref.getString("fcba", "") ?: ""

    // Form States
    var selectedRKH by remember { mutableStateOf<Map<String, String>?>(null) }
    var supervisi1 by remember { mutableStateOf("") }
    var supervisi2 by remember { mutableStateOf("") }
    var supervisi3 by remember { mutableStateOf("") }
    var supervisi4 by remember { mutableStateOf("") }
    val selectedWorkers = remember { mutableStateListOf<String>() }
    var unit by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var selectedTPH by remember { mutableStateOf("") }
    var isBeras by remember { mutableStateOf(false) }
    var lembur by remember { mutableStateOf("0") }

    // Master Data Load
    val rkhList = remember { dbHelper.getRKHData() } // Ambil dari tabel RKH
    val empList = remember { dbHelper.getDropdownData("EMPLOYEE", fcba) }// Master Karyawan
    val presentWorkers = remember { dbHelper.getPresentWorkers(fcba) } // Hanya yang sudah absen
    val tphList = remember(selectedRKH) {
        selectedRKH?.get("loc")?.let { dbHelper.getTPHByLocation(it) } ?: emptyList()
    }

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
            OutlinedTextField(value = fcba, onValueChange = {}, label = { Text("FCBA") }, readOnly = true, modifier = Modifier.fillMaxWidth())

            // Dropdown RKH
            RKHDropdownMap("No RKH", selectedRKH?.get("id") ?: "", rkhList) { selectedRKH = it }

            // Gang Code & Location (Otomatis dari RKH - Read Only)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = selectedRKH?.get("gang") ?: "", onValueChange = {}, label = { Text("Gang Code") }, modifier = Modifier.weight(1f), readOnly = true)
                OutlinedTextField(value = selectedRKH?.get("loc") ?: "", onValueChange = {}, label = { Text("Location") }, modifier = Modifier.weight(1f), readOnly = true)
            }

            // Supervisi 1 - 4
            Text("Personil Supervisi", fontWeight = FontWeight.Bold)
            RKHDropdown("Supervisi 1", supervisi1, empList) { supervisi1 = it }
            RKHDropdown("Supervisi 2", supervisi2, empList) { supervisi2 = it }
            RKHDropdown("Supervisi 3", supervisi3, empList) { supervisi3 = it }
            RKHDropdown("Supervisi 4", supervisi4, empList) { supervisi4 = it }

            // Multi Select Karyawan (Hanya yang sudah absen)
            Text("Pilih Karyawan (Sudah Absen)", fontWeight = FontWeight.Bold)
            if (presentWorkers.isEmpty()) {
                Text("Belum ada karyawan yang absen di FCBA ini", color = Color.Red, fontSize = 12.sp)
            }
            presentWorkers.forEach { (id, name) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = selectedWorkers.contains(id), onCheckedChange = {
                        if (it) selectedWorkers.add(id) else selectedWorkers.remove(id)
                    })
                    Text("$id - $name", fontSize = 14.sp)
                }
            }

            // Unit & Output (Input Number)
            OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = output, onValueChange = { output = it }, label = { Text("Output") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

            // TPH (Berdasarkan Location)
            RKHDropdown("Pilih TPH", selectedTPH, tphList) { selectedTPH = it }

            // Beras & Lembur
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isBeras, onCheckedChange = { isBeras = it })
                Text("Beras")
            }
            OutlinedTextField(value = lembur, onValueChange = { lembur = it }, label = { Text("Lembur (Jam)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))

            // Tombol Simpan & Transfer
            Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick = {
                    // VALIDASI BISNIS
                    val maxHk = selectedRKH?.get("hk")?.toDoubleOrNull() ?: 0.0
                    val outVal = output.toDoubleOrNull() ?: 0.0
                    val unitVal = unit.toDoubleOrNull() ?: 0.0
                    val lemburVal = lembur.toDoubleOrNull() ?: -1.0

                    when {
                        selectedRKH == null -> Toast.makeText(context, "Pilih No RKH!", Toast.LENGTH_SHORT).show()
                        selectedWorkers.size > maxHk -> Toast.makeText(context, "Karyawan melebihi HK ($maxHk)!", Toast.LENGTH_SHORT).show()
                        unitVal <= 0 -> Toast.makeText(context, "Unit harus > 0", Toast.LENGTH_SHORT).show()
                        outVal <= 0 -> Toast.makeText(context, "Output harus > 0", Toast.LENGTH_SHORT).show()
                        lemburVal < 0 -> Toast.makeText(context, "Lembur minimal 0", Toast.LENGTH_SHORT).show()
                        selectedTPH.isEmpty() -> Toast.makeText(context, "Pilih TPH!", Toast.LENGTH_SHORT).show()
                        else -> {
                            // LOGIK SIMPAN SQLITE
                            saveToSQLite(dbHelper, /* parameter */)
                            Toast.makeText(context, "Data Tersimpan Offline", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("SIMPAN DATA")
            }

            Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick= {
                    val jsonData = prepareNfcJson(
                        selectedRKH, supervisi1, supervisi2, supervisi3, supervisi4,
                        selectedWorkers, unit, output, selectedTPH, isBeras, lembur
                    )
                    // Selanjutnya kirim jsonData ini melalui fungsi NFC Hardware Anda
                    android.util.Log.d("NFC_PAYLOAD", jsonData)
                    Toast.makeText(context, "Data siap dikirim via NFC", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Icon(Icons.Default.Nfc, null)
                Spacer(Modifier.width(8.dp))
                Text("TRANSFER NFC")
            }
        }
    }
}

fun prepareNfcJson(
    rkh: Map<String, String>?,
    s1: String,
    s2: String,
    s3: String,
    s4: String,
    workers: List<String>,
    unit: String,
    output: String,
    tph: String,
    beras: Boolean,
    lembur: String
): String {
    val data = mutableMapOf<String, Any>()

    // Header & Info Utama
    data["id"] = System.currentTimeMillis() // Unique ID untuk cek duplikasi di penerima
    data["noRkh"] = rkh?.get("id") ?: ""
    data["gangCode"] = rkh?.get("gang") ?: ""
    data["location"] = rkh?.get("loc") ?: ""
    data["tph"] = tph

    // Data Perhitungan
    data["unit"] = unit
    data["output"] = output
    data["beras"] = if (beras) 1 else 0
    data["lembur"] = lembur

    // Personil
    data["supervisor1"] = s1
    data["supervisor2"] = s2
    data["supervisor3"] = s3
    data["supervisor4"] = s4
    data["daftarKaryawan"] = workers // List ID Karyawan

    // Mengonversi Map ke String JSON
    return Gson().toJson(data)
}

/**
 * Fungsi Placeholder untuk Simpan ke SQLite
 */
fun saveToSQLite(dbHelper: AttendanceDatabaseHelper, /* parameter lain */) {
    // Logika dbHelper.insertFruitCounting(...) Anda di sini
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHDropdownMap(label: String, selected: String, options: List<Map<String, String>>, onSelect: (Map<String, String>) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selected, onValueChange = {}, label = { Text(label) }, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(text = { Text("RKH: ${item["id"]} - ${item["afd"]}") }, onClick = { onSelect(item); expanded = false })
            }
        }
    }
}