package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.util.*

@Composable
fun SPBMainScreen(
    dbHelper: AttendanceDatabaseHelper,
    fcba: String,
    empId: String,
    onBackMenu: () -> Unit
) {
    // Pastikan defaultnya adalah false agar muncul LIST dulu
    var isAddingNew by remember { mutableStateOf(false) }

    if (isAddingNew) {
        SPBFormScreen(
            dbHelper = dbHelper,
            fcba = fcba,
            empId = empId,
            onBack = { isAddingNew = false },
            onSuccess = { isAddingNew = false }
        )
    } else {
        SPBListScreen(
            dbHelper = dbHelper,
            onTambahClick = { isAddingNew = true }, // Klik tombol + baru masuk ke Form
            onBack = onBackMenu
        )
    }
}

@Composable
fun SPBListScreen(
    dbHelper: AttendanceDatabaseHelper,
    onTambahClick: () -> Unit,
    onBack: () -> Unit
) {
    val spbListData = remember { mutableStateListOf<Map<String, String>>() }

    // Load data setiap kali layar muncul
    LaunchedEffect(Unit) {
        spbListData.clear()
        spbListData.addAll(dbHelper.getAllSPB())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar SPB") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            // Tombol Tambah Data (+)
            FloatingActionButton(
                onClick = onTambahClick,
                containerColor = Color(0xFF1A3A8F),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah SPB")
            }
        }
    ) { padding ->
        if (spbListData.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada data SPB", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pastikan 'items' sudah diimport (langkah nomor 1)
                items(spbListData) { item ->
                    SPBItemCard(item)
                }
            }
        }
    }
}

@Composable
fun SPBItemCard(item: Map<String, String>) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item["spb_no"] ?: "", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
                Text(item["created_at"] ?: "", fontSize = 12.sp, color = Color.Gray)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Lokasi", fontSize = 10.sp, color = Color.Gray)
                    Text(item["location_code"] ?: "-", fontWeight = FontWeight.Medium)
                }
                Column(Modifier.weight(1f)) {
                    Text("Jumlah", fontSize = 10.sp, color = Color.Gray)
                    Text("${item["unit"]} Janjang", fontWeight = FontWeight.Medium)
                }
                Column(Modifier.weight(1f)) {
                    Text("Sopir", fontSize = 10.sp, color = Color.Gray)
                    Text(item["sopir_name"] ?: "-", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}


@Composable
fun SPBFormScreen(
    dbHelper: AttendanceDatabaseHelper,
    fcba: String,
    empId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? MainActivity // Referensi ke MainActivity untuk akses NFC

    // --- STATE HEADER ---
    var spbNo by remember { mutableStateOf("") }
    var selectedMill by remember { mutableStateOf("") }
    var sopirName by remember { mutableStateOf("") }
    var selectedVehicle by remember { mutableStateOf("") }
    var pemuat1 by remember { mutableStateOf("") }
    var pemuat2 by remember { mutableStateOf("") }

    // --- STATE DETAIL ---
    var locCode by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var tph by remember { mutableStateOf("") }
    var empCode by remember { mutableStateOf("") }

    // --- STATE NFC ---
    var showScanDialog by remember { mutableStateOf(false) }

    // --- LOGIKA NFC SCANNER ---
    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = {
                showScanDialog = false
                activity?.onNfcRead = null
            },
            title = { Text("Siap Scan NFC") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Tempelkan kartu NFC ke bagian belakang HP untuk membaca data lokasi.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showScanDialog = false
                    activity?.onNfcRead = null
                }) { Text("Batal") }
            }
        )

        // Daftarkan listener ke MainActivity saat dialog muncul
        LaunchedEffect(Unit) {
            activity?.onNfcRead = { rawJson ->
                try {
                    // Parsing JSON dari Kartu
                    val gson = com.google.gson.Gson()
                    val dataMap = gson.fromJson(rawJson, Map::class.java)

                    // Ambil value berdasarkan key yang disimpan saat Fruit Counting
                    val scannedLoc = dataMap["location_code"]?.toString() ?: ""
                    val scannedUnit = dataMap["unit"]?.toString() ?: ""

                    // Update UI di Main Thread
                    activity?.runOnUiThread {
                        if (scannedLoc.isNotEmpty()) {
                            locCode = scannedLoc
                            unit = scannedUnit
                            showScanDialog = false // Tutup dialog jika berhasil
                            activity.onNfcRead = null
                            Toast.makeText(context, "Data Lokasi Berhasil Dibaca", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Format kartu tidak valid!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Generate No SPB saat buka
    LaunchedEffect(Unit) {
        spbNo = dbHelper.generateNoSPB(fcba)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buat SPB Baru") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())) {

            // CARD HEADER
            Card(
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("HEADER SPB", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

                    OutlinedTextField(value = fcba, onValueChange = {}, label = { Text("FCBA") }, readOnly = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = spbNo, onValueChange = {}, label = { Text("NO SPB") }, readOnly = true, modifier = Modifier.fillMaxWidth())

                    ClickableSearchField(label = "MILL / PABRIK", value = selectedMill) { /* Open Dialog Mill */ }
                    OutlinedTextField(value = sopirName, onValueChange = { sopirName = it }, label = { Text("NAMA SOPIR") }, modifier = Modifier.fillMaxWidth())
                    ClickableSearchField(label = "KODE KENDARAAN", value = selectedVehicle) { /* Open Dialog Vehicle */ }

                    Text("PEMUAT", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pemuat1, onValueChange = { pemuat1 = it }, label = { Text("Pemuat 1") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pemuat2, onValueChange = { pemuat2 = it }, label = { Text("Pemuat 2") }, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // CARD DETAIL (NFC SECTION)
            Card(
                border = BorderStroke(1.dp, Color(0xFFE65100)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DETAIL BUAH (SCAN NFC)", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))

                    // TOMBOL SCAN NFC
                    Button(
                        onClick = { showScanDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Nfc, null)
                        Spacer(Modifier.width(8.dp))
                        Text("SCAN LOKASI DARI KARTU")
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.LightGray)

                    OutlinedTextField(
                        value = locCode,
                        onValueChange = { locCode = it },
                        label = { Text("LOCATION CODE") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tph,
                            onValueChange = { tph = it },
                            label = { Text("TPH") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("UNIT / JANJANG") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                        )
                    }

                    OutlinedTextField(
                        value = empCode,
                        onValueChange = { empCode = it },
                        label = { Text("EMPLOYEE CODE (PEMILIK)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if(locCode.isEmpty()) {
                        Toast.makeText(context, "Mohon scan lokasi terlebih dahulu!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val unitInt = unit.toIntOrNull() ?: 0
                    val empCodeInt = empCode.toIntOrNull() ?: 0
                    // Logika Simpan ke Database
                    dbHelper.saveSPB(spbNo, fcba, selectedMill, sopirName, selectedVehicle, locCode, tph, unitInt, empCodeInt)
                    Toast.makeText(context, "SPB $spbNo Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                    onSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
            ) {
                Text("SIMPAN SPB", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}