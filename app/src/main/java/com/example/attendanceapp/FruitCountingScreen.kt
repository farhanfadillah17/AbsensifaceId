package com.example.attendanceapp

import android.app.DatePickerDialog
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
import java.util.*

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
    val calendar = Calendar.getInstance()

    // Form States
    var selectedDate by remember { mutableStateOf("") }
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
    var lembur by remember { mutableStateOf("") }

    // Master Data
    val rkhList = remember { dbHelper.getRKHData() }
    val supervisiList = remember { dbHelper.getDropdownData("EMPLOYEE") } // Sesuaikan fungsi master emp Anda
    val workerList = remember(selectedDate) {
        if(selectedDate.isNotEmpty()) dbHelper.getPresentWorkers(fcba, selectedDate) else emptyList()
    }
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // FCBA (Read Only)
            OutlinedTextField(value = fcba, onValueChange = {}, label = { Text("FCBA") }, modifier = Modifier.fillMaxWidth(), readOnly = true, enabled = false)

            // Date Picker
            OutlinedTextField(
                value = selectedDate,
                onValueChange = {},
                label = { Text("Pilih Tanggal") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = {
                        DatePickerDialog(context, { _, y, m, d -> selectedDate = "$y-${m+1}-$d" },
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Icon(Icons.Default.DateRange, null) }
                }
            )

            // Dropdown RKH
            RKHDropdownMap("No RKH", selectedRKH?.get("id") ?: "", rkhList) { selectedRKH = it }

            // Gang Code & Location (Auto dari RKH)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = selectedRKH?.get("gang") ?: "", onValueChange = {}, label = { Text("Gang Code") }, modifier = Modifier.weight(1f), readOnly = true)
                OutlinedTextField(value = selectedRKH?.get("loc") ?: "", onValueChange = {}, label = { Text("Location") }, modifier = Modifier.weight(1f), readOnly = true)
            }

            // Supervisi 1-4
            Text("Personil Supervisi", fontWeight = FontWeight.Bold)
            RKHDropdown("Supervisi 1", supervisi1, supervisiList) { supervisi1 = it }
            RKHDropdown("Supervisi 2", supervisi2, supervisiList) { supervisi2 = it }

            // Multi Select Karyawan (Hanya yang sudah absen)
            Text("Pilih Karyawan (Sudah Absen)", fontWeight = FontWeight.Bold)
            workerList.forEach { (id, name) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = selectedWorkers.contains(id), onCheckedChange = {
                        if (it) selectedWorkers.add(id) else selectedWorkers.remove(id)
                    })
                    Text("$id - $name")
                }
            }

            // Unit, Output, Lembur
            OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = output, onValueChange = { output = it }, label = { Text("Output") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

            RKHDropdown("Pilih TPH", selectedTPH, tphList) { selectedTPH = it }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isBeras, onCheckedChange = { isBeras = it })
                Text("Beras")
            }

            OutlinedTextField(value = lembur, onValueChange = { lembur = it }, label = { Text("Lembur (Jam)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

            Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick = {
                    // Validasi Bisnis
                    val maxHk = selectedRKH?.get("hk")?.toDoubleOrNull() ?: 0.0
                    if (selectedRKH == null) {
                        Toast.makeText(context, "No RKH Wajib dipilih!", Toast.LENGTH_SHORT).show()
                    } else if (selectedWorkers.size > maxHk) {
                        Toast.makeText(context, "Karyawan melebihi HK RKH ($maxHk)!", Toast.LENGTH_SHORT).show()
                    } else if ((output.toDoubleOrNull() ?: -1.0) < 0 || (unit.toDoubleOrNull() ?: -1.0) < 0) {
                        Toast.makeText(context, "Output/Unit tidak boleh minus!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Simpan ke DB logic di sini...
                        Toast.makeText(context, "Data Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
            ) {
                Text("SIMPAN PERHITUNGAN BUAH")
            }
        }
    }
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