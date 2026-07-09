package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
    var isAddingNew by remember { mutableStateOf(false) }
    var currentEditId by remember { mutableStateOf<String?>(null) } // State untuk Edit

    if (isAddingNew || currentEditId != null) {
        SPBFormScreen(
            dbHelper = dbHelper,
            fcba = fcba,
            empId = empId,
            editId = currentEditId,
            onBack = {
                isAddingNew = false
                currentEditId = null
            },
            onSuccess = {
                isAddingNew = false
                currentEditId = null
            }
        )
    } else {
        SPBListScreen(
            dbHelper = dbHelper,
            fcba = fcba,
            onTambahClick = { isAddingNew = true },
            onEditClick = { id -> currentEditId = id },
            onBack = onBackMenu
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SPBListScreen(
    dbHelper: AttendanceDatabaseHelper,
    fcba: String,
    onTambahClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var spbListData by remember { mutableStateOf(dbHelper.getAllSPBListMap(fcba)) }

    LaunchedEffect(Unit) {
        spbListData = dbHelper.getAllSPBListMap(fcba)
    }
    // State Menu & Dialog
    var showMenu by remember { mutableStateOf(false) }
    var selectedItemForMenu by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedDetailItem by remember { mutableStateOf<Map<String, String>?>(null) }
    var itemToDelete by remember { mutableStateOf<Map<String, String>?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar SPB", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onTambahClick, containerColor = Color(0xFF1A3A8F)) {
                Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White)
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
                items(spbListData) { item ->
                    SPBItemCard(
                        item = item,
                        onClick = { selectedDetailItem = item },
                        onLongClick = {
                            selectedItemForMenu = item
                            showMenu = true
                        }
                    )
                }
            }
        }

        // --- 1. MODAL BOTTOM SHEET (MENU) ---
        if (showMenu && selectedItemForMenu != null) {
            ModalBottomSheet(onDismissRequest = { showMenu = false }, sheetState = sheetState) {
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
                    Text("Opsi SPB: ${selectedItemForMenu!!["no_spb"]}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    ListItem(
                        headlineContent = { Text("Lihat Detail") },
                        leadingContent = { Icon(Icons.Default.Visibility, null, tint = Color.Blue) },
                        modifier = Modifier.clickable { showMenu = false; selectedDetailItem = selectedItemForMenu }
                    )
                    ListItem(
                        headlineContent = { Text("Edit Header") },
                        leadingContent = { Icon(Icons.Default.Edit, null, tint = Color(0xFFF57C00)) },
                        modifier = Modifier.clickable { showMenu = false; onEditClick(selectedItemForMenu!!["id"] ?: "") }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Hapus Data", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            itemToDelete = selectedItemForMenu
                        }
                    )
                }
            }
        }

        // --- 2. DIALOG DETAIL ---
        if (selectedDetailItem != null) {
            AlertDialog(
                onDismissRequest = { selectedDetailItem = null },
                title = { Text("Detail SPB", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("No SPB", selectedDetailItem!!["no_spb"])
                        DetailRow("Pabrik", selectedDetailItem!!["mill_name"])
                        DetailRow("Sopir", selectedDetailItem!!["driver_name"])
                        DetailRow("Kendaraan", selectedDetailItem!!["vehicle_code"])
                        DetailRow("Pemuat", "${selectedDetailItem!!["loader_1"]}, ${selectedDetailItem!!["loader_2"]}")
                        DetailRow("Lokasi/Unit", "${selectedDetailItem!!["location_code"]} / ${selectedDetailItem!!["unit"]}")
                    }
                },
                confirmButton = { TextButton(onClick = { selectedDetailItem = null }) { Text("Tutup") } }
            )
        }

        // --- 3. DIALOG HAPUS ---
        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Hapus Data") },
                text = { Text("Yakin ingin menghapus SPB ${itemToDelete!!["no_spb"]}?") },
                confirmButton = {
                    Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Red), onClick = {
                        if (dbHelper.deleteSPB(itemToDelete!!["id"] ?: "")) {
                            spbListData = dbHelper.getAllSPBListMap(fcba)
                            Toast.makeText(context, "Data Terhapus", Toast.LENGTH_SHORT).show()
                        }
                        itemToDelete = null
                    }) { Text("Hapus") }
                },
                dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Batal") } }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SPBItemCard(
    item: Map<String, String>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item["no_spb"] ?: "", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
                Text(item["created_at"] ?: "", fontSize = 12.sp, color = Color.Gray)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1.1f)) {
                    Text("Lokasi", fontSize = 10.sp, color = Color.Gray)
                    Text(item["location_code"] ?: "-", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Column(Modifier.weight(0.9f)) {
                    Text("Unit", fontSize = 10.sp, color = Color.Gray)
                    Text("${item["unit"]} Jjg", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Sopir", fontSize = 10.sp, color = Color.Gray)
                    Text(item["driver_name"] ?: "-", fontWeight = FontWeight.Medium, fontSize = 13.sp)
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
    editId: String? = null,
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

    var activeDialog by remember { mutableStateOf<String?>(null) }
    // --- STATE DATA MASTER ---
    val mills = remember { mutableStateListOf<Mill>() }
    val vehicles = remember { mutableStateListOf<Vehicle>() }
    val employeeOptions = remember(fcba) { dbHelper.getEmployeeList(fcba) }
    LaunchedEffect(Unit) {
        mills.clear()
        mills.addAll(dbHelper.getAllMills())



        vehicles.clear()
        vehicles.addAll(dbHelper.getAllVehicles())
    }

    // --- STATE DIALOG PENCARIAN ---
    var showMillDialog by remember { mutableStateOf(false) }
    var showVehicleDialog by remember { mutableStateOf(false) }
    // --- DIALOG PILIH KARYAWAN (SOPIR & PEMUAT) ---
    if (activeDialog != null) {
        MasterSearchDialog(
            title = "Pilih $activeDialog",
            items = employeeOptions,
            onDismiss = { activeDialog = null },
            onSelect = { selectedValue ->
                when (activeDialog) {
                    "SOPIR" -> sopirName = selectedValue
                    "PEMUAT 1" -> pemuat1 = selectedValue
                    "PEMUAT 2" -> pemuat2 = selectedValue
                }
                activeDialog = null
            }
        )
    }
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
    val scannedCards = remember { mutableStateListOf<Map<String, String>>() }
    var totalUnit by remember { mutableIntStateOf(0) }

    // --- STATE NFC ---
    var showScanDialog by remember { mutableStateOf(false) }
    var isScannerStarted by remember { mutableStateOf(false) }

    // logika ini akan aktif selama user berada di Step 2, tanpa peduli dialog muncul atau tidak
    LaunchedEffect(currentStep) {
        if (currentStep == 2) {
            // Tampilkan dialog petunjuk otomatis saat pertama kali masuk ke Step 2 (opsional)

            activity?.onNfcRead = { rawJson ->
                try {
                    val gson = com.google.gson.Gson()
                    val dataMap = gson.fromJson(rawJson, Map::class.java) as Map<String, Any>

                    val scannedLoc = dataMap["location_code"]?.toString() ?: ""
                    val scannedUnit = dataMap["unit"]?.toString() ?: "0"
                    val scannedTph = dataMap["tph_code"]?.toString() ?: "-"
                    activity?.runOnUiThread {
                        if (scannedLoc.isNotEmpty()) {
                            val isAlreadyScanned = scannedCards.any {
                                it["location_code"] == scannedLoc && it["tph_code"] == scannedTph
                            }


                            if (!isAlreadyScanned) {
                                val newEntry = mapOf(
                                    "location_code" to scannedLoc,
                                    "unit" to scannedUnit,
                                    "tph_code" to scannedTph
                                )
                                scannedCards.add(newEntry)
                                totalUnit += scannedUnit.toIntOrNull() ?: 0

                                Toast.makeText(context, "Terdeteksi: $scannedLoc ($scannedUnit Jjg)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Kartu $scannedLoc sudah masuk daftar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Data kartu tidak dikenali", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Matikan listener jika user kembali ke Step 1
            activity?.onNfcRead = null
        }
    }

    // Pastikan listener dimatikan saat screen ditutup (Back)
    DisposableEffect(Unit) {
        onDispose {
            activity?.onNfcRead = null
        }
    }

    // 2. DIALOG HANYA SEBAGAI PETUNJUK
    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = { showScanDialog = false },
            title = { Text("Scanner Aktif") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF1A3A8F)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("NFC Siap. Anda bisa langsung menempelkan kartu-kartu NFC ke belakang HP satu per satu tanpa menekan tombol lagi.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showScanDialog = false
                    isScannerStarted = true // Set menjadi true agar tombol scan hilang
                }) {
                    Text("MENGERTI")
                }
            }
        )
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

                        ClickableSearchField(
                            label = "NAMA SOPIR",
                            value = sopirName,
                            onClick = { activeDialog = "SOPIR" }
                        )

                        ClickableSearchField(
                            label = "KODE KENDARAAN",
                            value = selectedVehicle,
                            onClick = { showVehicleDialog = true }
                        )

                        Text(
                            "PEMUAT",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                        )

                            ClickableSearchField(
                                label = "PEMUAT 1",
                                value = pemuat1,
                                onClick = { activeDialog = "PEMUAT 1" }
                            )


                            ClickableSearchField(
                                label = "PEMUAT 2",
                                value = pemuat2,
                                onClick = { activeDialog = "PEMUAT 2" }
                            )


                        Spacer(Modifier.height(16.dp))

                        // TOMBOL NEXT (Simpan Header ke DB)
                        // TOMBOL NEXT (Hanya Validasi & Pindah Step)
                        Button(
                            onClick = {
                                if (selectedMill.isEmpty() || sopirName.isEmpty() || selectedVehicle.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "Mohon lengkapi data header!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    // JANGAN simpan ke DB dulu, langsung pindah ke step 2
                                    currentStep = 2
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
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
                // --- TAMPILAN DI DALAM STEP 2 ---
                Text("STEP 2: SCAN LOKASI NFC", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
                Spacer(Modifier.height(12.dp))

                // Tombol Mulai Scan (Pemicu Dialog)
                if (!isScannerStarted) {
                    Button(
                        onClick = { showScanDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
                    ) {
                        Icon(Icons.Default.Nfc, null)
                        Spacer(Modifier.width(8.dp))
                        Text("MULAI SCAN NFC")
                    }
                } else {
                    // Opsional: Tampilkan indikator bahwa NFC sedang standby
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RadioButtonChecked, "Active", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Scanner Standby: Tempel kartu sekarang", color = Color(0xFF2E7D32), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (scannedCards.isNotEmpty()) {
                    // Tampilkan List Kartu yang sudah ter-scan
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Daftar Lokasi Ter-scan:", fontSize = 12.sp, color = Color.Gray)

                            scannedCards.forEach { card ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${card["location_code"]}", fontWeight = FontWeight.Bold)
                                    Text("TPH: ${card["tph_code"] ?: "-"}", fontSize = 11.sp, color = Color.DarkGray)
                                    Text("${card["unit"]} Jjg", color = Color(0xFF2E7D32))
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL UNIT:", fontWeight = FontWeight.ExtraBold)
                                Text("$totalUnit Jjg", fontWeight = FontWeight.ExtraBold, color = Color.Blue)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // TOMBOL SIMPAN
                    Button(
                        onClick = {
                            val allLocs = scannedCards.mapNotNull { it["location_code"] }.distinct().joinToString(", ")
                            val allTphs = scannedCards.mapNotNull { it["tph_code"] }.distinct().joinToString(", ")
                            // Logic simpan ke Database
                            val id = dbHelper.saveSPBFull(
                                spbNo = spbNo,
                                mill = selectedMill,
                                sopir = sopirName,
                                vehicle = selectedVehicle,
                                pemuat1 = pemuat1,
                                pemuat2 = pemuat2,
                                fcba = fcba,
                                locCode = allLocs,
                                tphCode = allTphs,
                                unit = totalUnit.toString()
                            )
                            if (id > 0) {
                                Toast.makeText(context, "SPB Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("SIMPAN SPB")
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

