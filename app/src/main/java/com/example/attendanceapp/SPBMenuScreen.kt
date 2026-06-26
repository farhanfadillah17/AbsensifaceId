package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import java.util.*
import kotlin.text.split

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
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada data SPB", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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

    var currentStep by remember { mutableIntStateOf(1) } // 1 untuk Header, 2 untuk NFC/Lokasi
    var spbHeaderId by remember { mutableLongStateOf(-1L) } // Menyimpan ID setelah simpan header
    // --- STATE HEADER ---
    var spbNo by remember { mutableStateOf("") }
    var selectedMill by remember { mutableStateOf("") }
    var sopirName by remember { mutableStateOf("") }
    var selectedVehicle by remember { mutableStateOf("") }
    var pemuat1 by remember { mutableStateOf("") }
    var pemuat2 by remember { mutableStateOf("") }

    // --- STATE DATA MASTER ---
    val mills = remember { mutableStateListOf<Mill>() }
    val vehicles = remember { mutableStateListOf<Vehicle>() }

    LaunchedEffect(Unit) {
        mills.clear()
        mills.addAll(dbHelper.getAllMills())



        vehicles.clear()
        vehicles.addAll(dbHelper.getAllVehicles())
    }

    // --- STATE DIALOG PENCARIAN ---
    var showMillDialog by remember { mutableStateOf(false) }
    var showVehicleDialog by remember { mutableStateOf(false) }
    // Dialog Pilih Mill
    if (showMillDialog) {
        MasterSearchDialog(
            title = "Pilih Mill / Pabrik",
            items = mills.map { "${it.code} - ${it.name}" },
            onDismiss = { showMillDialog = false },
            onSelect = {
                selectedMill = it.split(" - ")[0] // Simpan kodenya saja
                showMillDialog = false
            }
        )
    }

    // Dialog Pilih Kendaraan
    if (showVehicleDialog) {
        MasterSearchDialog(
            title = "Pilih Kendaraan",
            items = vehicles.map { "${it.code} - ${it.name} (${it.regNo})" },
            onDismiss = { showVehicleDialog = false },
            onSelect = {
                selectedVehicle = it.split(" - ")[0] // Simpan kodenya saja
                showVehicleDialog = false
            }
        )
    }
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
                            Toast.makeText(
                                context,
                                "Data Lokasi Berhasil Dibaca",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Format kartu tidak valid!", Toast.LENGTH_SHORT)
                            .show()
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
                title = { Text(if (currentStep == 1) "Buat SPB - Step 1" else "Buat SPB - Step 2") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == 2) currentStep = 1 else onBack()
                    }) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // --- TAMPILAN STEP 1: HEADER ---
            if (currentStep == 1) {
                Card(
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("HEADER SPB", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

                        OutlinedTextField(
                            value = fcba,
                            onValueChange = {},
                            label = { Text("FCBA") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = spbNo,
                            onValueChange = {},
                            label = { Text("NO SPB") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ClickableSearchField(
                            label = "MILL / PABRIK",
                            value = selectedMill,
                            onClick = { showMillDialog = true }
                        )

                        OutlinedTextField(
                            value = sopirName,
                            onValueChange = { sopirName = it },
                            label = { Text("NAMA SOPIR") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        ClickableSearchField(
                            label = "KODE KENDARAAN",
                            value = selectedVehicle,
                            onClick = { showVehicleDialog = true }
                        )

                        Text(
                            "PEMUAT",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pemuat1,
                                onValueChange = { pemuat1 = it },
                                label = { Text("Pemuat 1") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pemuat2,
                                onValueChange = { pemuat2 = it },
                                label = { Text("Pemuat 2") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // TOMBOL NEXT (Simpan Header ke DB)
                        Button(
                            onClick = {
                                if (selectedMill.isEmpty() || sopirName.isEmpty() || selectedVehicle.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "Mohon lengkapi data header!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    // Simpan Header ke DB dan ambil ID-nya
                                    val id = dbHelper.saveSPBHeader(
                                        spbNo = spbNo,
                                        mill = selectedMill,
                                        sopir = sopirName,
                                        vehicle = selectedVehicle,
                                        pemuat1 = pemuat1,
                                        pemuat2 = pemuat2,
                                        fcba = fcba
                                    )
                                    if (id > 0) {
                                        spbHeaderId = id
                                        currentStep = 2 // PINDAH KE STEP 2
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Gagal menyimpan header",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
                        ) {
                            Text("NEXT: SCAN LOKASI")
                            Icon(Icons.Default.ArrowForward, null, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

         else {
        // --- TAMPILAN STEP 2: SCAN LOKASI DENGAN NFC (MENYAMPING RAPI SEPERTI RKH) ---
        Card(
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth() // Memaksa card memenuhi lebar layar
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "STEP 2: SCAN LOKASI NFC",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A3A8F),
                    fontSize = 16.sp
                )

                if (locCode.isEmpty()) {
                    // Tampilan Instruksi Scan Full Width
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFF1A3A8F)
                        )
                        Text(
                            text = "Tempelkan kartu NFC untuk mengambil lokasi",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showScanDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
                        ) {
                            Text("MULAI SCAN")
                        }
                    }
                } else {
                    // Tampilan Hasil Scan Menyamping dan Menyesuaikan Lebar Layar Penuh
                    OutlinedTextField(
                        value = locCode,
                        onValueChange = {},
                        label = { Text("Lokasi (Block)") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        label = { Text("Estimasi Janjang") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // TOMBOL AKSI MENYAMPING (Row Bersandingan seperti RKH)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { currentStep = 1 }, // Kembali ke step 1
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("KEMBALI")
                        }

                        Button(
                            onClick = {
                                val success = dbHelper.updateSPBLocation(spbHeaderId, locCode, unit)
                                if (success) {
                                    Toast.makeText(context, "SPB Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                } else {
                                    Toast.makeText(context, "Gagal memperbarui data lokasi", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.5f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(6.dp))
                            Text("SIMPAN & SELESAI")
                        }
                    }
                }
            }
        }
    }
        }
    }
}

@Composable
fun MasterSearchDialog(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = items.filter { it.contains(searchQuery, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(filteredItems) { item ->
                        TextButton(
                            onClick = { onSelect(item) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(item, textAlign = TextAlign.Left, modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun ClickableSearchField(label: String, value: String, onClick: () -> Unit) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        // Overlay transparan untuk menangkap klik
        Box(modifier = Modifier
            .matchParentSize()
            .clickable { onClick() }
        )
    }
}