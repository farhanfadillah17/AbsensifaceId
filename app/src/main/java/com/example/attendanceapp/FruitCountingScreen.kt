package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    // State Navigasi Internal
    var isFormVisible by remember { mutableStateOf(false) }
    var selectedDataForNfc by remember { mutableStateOf<Map<String, String>?>(null) }

    // Refresh list data
    var fruitDataList by remember { mutableStateOf(dbHelper.getAllFruitCounting()) }

    // Dialog NFC
    if (selectedDataForNfc != null) {
        NfcTransferDialog(
            data = selectedDataForNfc!!,
            onDismiss = { selectedDataForNfc = null }
        )
    }

    if (isFormVisible) {
        // TAMPILAN FORM INPUT
        FruitCountingFormContent(
            dbHelper = dbHelper,
            fcba = fcba,
            onBack = { isFormVisible = false },
            onSaveSuccess = {
                fruitDataList = dbHelper.getAllFruitCounting() // Refresh data list
                isFormVisible = false
                // Jika ingin langsung ke dashboard setelah simpan, panggil: onSuccess()
            }
        )
    } else {
        // TAMPILAN UTAMA: LIST DATA
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("List Perhitungan Buah", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { isFormVisible = true },
                    containerColor = Color(0xFF1A3A8F),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Data")
                }
            }
        ) { padding ->
            if (fruitDataList.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Belum ada data. Klik + untuk menambah.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fruitDataList) { data ->
                        FruitCountingItemCard(data = data) {
                            selectedDataForNfc = data
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FruitCountingItemCard(data: Map<String, String>, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(data["tanggal"] ?: data["created_at"] ?: "-", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Text("TPH: ${data["tph_code"] ?: "-"}", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Lokasi", fontSize = 12.sp, color = Color.Gray)
                    Text(data["location_code"] ?: "-", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Column(Modifier.weight(1f)) {
                    Text("Output", fontSize = 12.sp, color = Color.Gray)
                    Text(data["output"] ?: "0", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Unit", fontSize = 12.sp, color = Color.Gray)
                    Text(data["unit"] ?: "0", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "Klik untuk Transfer NFC",
                fontSize = 10.sp,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun NfcTransferDialog(data: Map<String, String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer NFC") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Nfc, null, modifier = Modifier.size(64.dp), tint = Color(0xFF1A3A8F))
                Spacer(Modifier.height(16.dp))
                Text("Tempelkan HP ke Reader/HP lain untuk mengirim data TPH: ${data["tph_code"] ?: "-"}")
                Text("Data JSON: ${Gson().toJson(data)}", fontSize = 10.sp, color = Color.LightGray)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FruitCountingFormContent(
    dbHelper: AttendanceDatabaseHelper,
    fcba: String,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current

    // Form States
    var selectedRKH by remember { mutableStateOf<Map<String, String>?>(null) }
    var supervisi1 by remember { mutableStateOf("") }
    var supervisi2 by remember { mutableStateOf("") }
    var supervisi3 by remember { mutableStateOf("") }
    var supervisi4 by remember { mutableStateOf("") }
    var selectedWorkers by remember { mutableStateOf(setOf<String>()) }
    var unit by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var selectedTPH by remember { mutableStateOf("") }
    var isBeras by remember { mutableStateOf(false) }
    var lembur by remember { mutableStateOf("0") }
    var activeDialog by remember { mutableStateOf<String?>(null) }

    // Master Data
    val rkhList = remember { dbHelper.getRKHList() }
    val supervisorOptions = remember { dbHelper.getSupervisors() }
    val presentWorkers = remember { dbHelper.getEmployeesAlreadyCheckedIn(fcba) }
    val locationCodeFromRKH = selectedRKH?.get("location") ?: ""
    val tphList = remember(locationCodeFromRKH) {
        if (locationCodeFromRKH.isNotEmpty()) dbHelper.getTPHByLocation(locationCodeFromRKH) else emptyList()
    }

    // Dialog Pencarian Logic
    if (activeDialog != null) {
        when (activeDialog) {
            "RKH" -> SearchableMapDialog(
                title = "Cari No RKH", options = rkhList,
                displayProvider = { "RKH:${it["no_rkh"]} - ${it["location"]}" },
                onDismiss = { activeDialog = null },
                onSelect = { selectedRKH = it; selectedTPH = ""; activeDialog = null }
            )
            "TPH" -> SearchableListDialog(
                title = "Cari TPH", options = tphList,
                onDismiss = { activeDialog = null },
                onSelect = { selectedTPH = it; activeDialog = null }
            )
            "SUP1", "SUP2", "SUP3", "SUP4" -> SearchableListDialog(
                title = "Cari Personil", options = supervisorOptions,
                onDismiss = { activeDialog = null },
                onSelect = {
                    when(activeDialog) {
                        "SUP1" -> supervisi1 = it
                        "SUP2" -> supervisi2 = it
                        "SUP3" -> supervisi3 = it
                        "SUP4" -> supervisi4 = it
                    }
                    activeDialog = null
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Form Perhitungan Buah", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Pilih No RKH
            ClickableSearchField(
                label = "Pilih No RKH",
                value = if (selectedRKH != null) "RKH:${selectedRKH?.get("no_rkh")} - ${selectedRKH?.get("location")}" else "",
                onClick = { activeDialog = "RKH" }
            )

            // 2. Gang Code (Otomatis & Read Only)
            OutlinedTextField(
                value = selectedRKH?.get("gang_code") ?: "",
                onValueChange = {},
                label = { Text("Gang Code") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledBorderColor = Color.Gray,
                    disabledLabelColor = Color.DarkGray,
                    disabledContainerColor = Color(0xFFF5F5F5)
                )
            )

            Text("Personil Supervisi", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

            // Supervisi Rows
            ClickableSearchField("Supervisi 1 (Wajib)", supervisi1) { activeDialog = "SUP1" }
            ClickableSearchField("Supervisi 2", supervisi2) { activeDialog = "SUP2" }
            ClickableSearchField("Supervisi 3", supervisi3) { activeDialog = "SUP3" }
            ClickableSearchField("Supervisi 4", supervisi4) { activeDialog = "SUP4" }

            Text("Pilih Karyawan", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                Column(Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                    presentWorkers.forEach { staff ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                            val current = selectedWorkers.toMutableSet()
                            if (current.contains(staff["id"])) current.remove(staff["id"]) else current.add(staff["id"]!!)
                            selectedWorkers = current
                        }) {
                            Checkbox(checked = selectedWorkers.contains(staff["id"]), onCheckedChange = null)
                            Text("${staff["id"]} - ${staff["name"]}", fontSize = 13.sp)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = output, onValueChange = { output = it }, label = { Text("Output") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }

            ClickableSearchField("Pilih TPH", selectedTPH) { activeDialog = "TPH" }

            // FIELD BERAS & LEMBUR
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(checked = isBeras, onCheckedChange = { isBeras = it })
                    Text("Beras")
                }
                OutlinedTextField(
                    value = lembur,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) lembur = it },
                    label = { Text("Lembur (Jam)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth().height(55.dp),
                onClick = {
                    if (selectedRKH == null || supervisi1.isEmpty() || selectedWorkers.isEmpty() || selectedTPH.isEmpty()) {
                        Toast.makeText(context, "Lengkapi data wajib!", Toast.LENGTH_SHORT).show()
                    } else {
                        dbHelper.saveFruitCalculation(
                            fcba = fcba,
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
                        onSaveSuccess()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("SIMPAN DATA")
            }
        }
    }
}