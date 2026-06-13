package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SPBFormScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    val fcbaUser = sharedPref.getString("fcba", "SRE") ?: "SRE"

    // --- DATA SOURCES ---
    val rkhList = remember { dbHelper.getRKHList() }
    val driverOptions = remember { dbHelper.getDrivers() }
    val millOptions = remember {
        val dbData = dbHelper.getBusinessUnits()
        if (dbData.isEmpty()) {
            listOf("PKS 1 (DB KOSONG)", "PKS 2 (DB KOSONG)")
        } else {
            dbData
        }
    }

    // UPDATE: Ambil daftar lokasi dari tabel FIELD (FIELD_202606011224.sql)
    val locationOptions = remember { dbHelper.getAllFieldCodes() }

    // --- FORM STATES ---
    var selectedRKH by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedLocation by remember { mutableStateOf("") } // State utama Lokasi (Blok)
    var selectedMill by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var vehicleNo by remember { mutableStateOf("") }
    var selectedTPH by remember { mutableStateOf("") }
    var totalJanjang by remember { mutableStateOf("") }
    var spbNo by remember { mutableStateOf("SPB-${System.currentTimeMillis() / 10000}") }
    val tanggal = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

    // Dropdown States
    var rkhExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var millExpanded by remember { mutableStateOf(false) }
    var driverExpanded by remember { mutableStateOf(false) }
    var tphExpanded by remember { mutableStateOf(false) }

    // Logic: List TPH akan otomatis berubah jika selectedLocation berubah
    // Mengambil data dari tabel TPH berdasarkan FIELDCODE (selectedLocation)
    val tphOptions = remember(selectedLocation) {
        if (selectedLocation.isNotEmpty()) {
            dbHelper.getTPHByLocation(selectedLocation)
        } else {
            emptyList<String>()
        }
    }

    // Styling Read Only
    val readOnlyColors = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF0F0F0),
        unfocusedContainerColor = Color(0xFFF0F0F0)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FORM SPB (PENGANGKUTAN)", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
            // Header Info
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = spbNo, onValueChange = {}, label = { Text("No. SPB") }, modifier = Modifier.weight(1.2f), readOnly = true, colors = readOnlyColors)
                OutlinedTextField(value = tanggal, onValueChange = {}, label = { Text("Tanggal") }, modifier = Modifier.weight(0.8f), readOnly = true, colors = readOnlyColors)
            }

            OutlinedTextField(value = fcbaUser, onValueChange = {}, label = { Text("FCBA") }, modifier = Modifier.fillMaxWidth(), readOnly = true, colors = readOnlyColors)

            // 1. Referensi RKH
            // 1. Referensi RKH
            Text("Ambil Referensi RKH", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            RKHDropdownMap(
                label = "Pilih No RKH",
                // Menampilkan Nama Blok dari FIELD agar lebih informatif
                selected = if (selectedRKH != null)
                    "RKH:${selectedRKH?.get("no_rkh")} - ${selectedRKH?.get("location")}"
                else "",
                options = rkhList,
                expanded = rkhExpanded,
                onExpandedChange = { rkhExpanded = it }
            ) {
                selectedRKH = it
                // Nilai 'it["location"]' sekarang dipastikan ada di FIELD_202606011224.sql
                selectedLocation = it["location"] ?: ""
                selectedTPH = ""
                rkhExpanded = false
            }

            // 2. Pilih Lokasi (Manual / Konfirmasi dari RKH)
            // Mengambil data dari tabel FIELD
            RKHDropdownSimple(
                label = "Lokasi / Blok (Data FIELD)",
                selected = selectedLocation,
                options = locationOptions,
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = it }
            ) {
                selectedLocation = it
                selectedTPH = "" // Reset TPH jika lokasi diganti manual
                locationExpanded = false
            }

            OutlinedTextField(value = empId, onValueChange = {}, label = { Text("Kerani Kirim") }, modifier = Modifier.fillMaxWidth(), readOnly = true, colors = readOnlyColors)

            Divider()

            // 3. Detail Pengiriman
            Text("Detail Pengiriman", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

            RKHDropdownSimple(
                label = "Pilih Pabrik (Mill)",
                selected = selectedMill,
                options = millOptions,
                expanded = millExpanded,
                onExpandedChange = { millExpanded = it }
            ) {
                selectedMill = it
                millExpanded = false
            }

            RKHDropdownSimple("Pilih Sopir", driverName, driverOptions, driverExpanded, { driverExpanded = it }) {
                driverName = it
                driverExpanded = false
            }

            OutlinedTextField(value = vehicleNo, onValueChange = { vehicleNo = it }, label = { Text("Nomor Plat Kendaraan") }, modifier = Modifier.fillMaxWidth())

            Divider()

            // 4. Detail Muatan
            Text("Detail Muatan", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

            RKHDropdownSimple(
                label = "Pilih TPH",
                selected = selectedTPH,
                options = tphOptions, // List TPH difilter berdasarkan selectedLocation (FIELDCODE)
                expanded = tphExpanded,
                onExpandedChange = { tphExpanded = it }
            ) {
                selectedTPH = it
                tphExpanded = false
            }

            OutlinedTextField(
                value = totalJanjang,
                onValueChange = { totalJanjang = it },
                label = { Text("Jumlah Janjang") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Button(
                onClick = {
                    if (selectedLocation.isEmpty() || driverName.isEmpty() || totalJanjang.isEmpty() || selectedMill.isEmpty()) {
                        Toast.makeText(context, "Lengkapi semua data!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Logika Simpan Database
                        Toast.makeText(context, "SPB Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SIMPAN & CETAK SPB", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Membaca data buah dari NFC...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Nfc, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AMBIL DATA DARI NFC BUAH")
            }
        }
    }
}


// --- Komponen Dropdown Reusable ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHDropdownMap(
    label: String,
    selected: String,
    options: List<Map<String, String>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Map<String, String>) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selected, onValueChange = {}, label = { Text(label) }, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text("RKH:${item["no_rkh"]} - ${item["location"]}") },
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHDropdownSimple(
    label: String,
    selected: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selected, onValueChange = {}, label = { Text(label) }, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelect(item) })
            }
        }
    }
}