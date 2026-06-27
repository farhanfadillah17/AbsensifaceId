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
                Text(
                    text = "STEP 2: SCAN LOKASI NFC",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A3A8F),
                    fontSize = 16.sp
                )

                // --- DI DALAM STEP 2 Card Column ---

                if (locCode.isNotEmpty()) {
                    // Tampilkan Informasi Hasil Scan agar User Yakin
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Lokasi Terdeteksi:", fontSize = 12.sp, color = Color.Gray)
                            Text("Code: $locCode", fontWeight = FontWeight.Bold)
                            Text("Unit: $unit", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // TOMBOL SIMPAN / UPDATE FINAL
                    Button(
                        onClick = {
                            if (editId != null) {
                                // LOGIC UPDATE (Jika sedang edit header)
                                val success = dbHelper.updateSPBHeader(
                                    id = editId!!,
                                    mill = selectedMill,
                                    sopir = sopirName,
                                    vehicle = selectedVehicle,
                                    pemuat1 = pemuat1,
                                    pemuat2 = pemuat2
                                )
                                if (success) {
                                    Toast.makeText(context, "Header SPB Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, "Gagal memperbarui data!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // LOGIC SIMPAN BARU
                                val id = dbHelper.saveSPBFull(
                                    spbNo = spbNo,
                                    mill = selectedMill,
                                    sopir = sopirName,
                                    vehicle = selectedVehicle,
                                    pemuat1 = pemuat1,
                                    pemuat2 = pemuat2,
                                    fcba = fcba,
                                    locCode = locCode,
                                    unit = unit
                                )
                                if (id > 0) {
                                    Toast.makeText(context, "SPB Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                } else {
                                    Toast.makeText(context, "Gagal menyimpan data!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (editId != null) Color(0xFFF57C00) else Color(0xFF2E7D32)
                        )
                    ) {
                        Icon(if (editId != null) Icons.Default.Edit else Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (editId != null) "UPDATE HEADER SPB" else "SIMPAN SPB")
                    }
                }
                else {
                    // Tombol Scan NFC (Jika belum ada locCode)
                    Button(
                        onClick = { showScanDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
                    ) {
                        Icon(Icons.Default.Nfc, null)
                        Spacer(Modifier.width(8.dp))
                        Text("MULAI SCAN NFC")
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

