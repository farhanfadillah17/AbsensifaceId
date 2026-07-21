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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList


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
    var spbListData by remember { mutableStateOf(dbHelper.getAllSPBHeader(fcba)) }

    LaunchedEffect(Unit) {
        spbListData = dbHelper.getAllSPBHeader(fcba)
    }
    // State Menu & Dialog
    var showMenu by remember { mutableStateOf(false) }
    var selectedItemForMenu by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedDetailItem by remember { mutableStateOf<Map<String, String>?>(null) }
    // Ambil detail secara otomatis saat selectedDetailItem berubah
    val detailsList = remember(selectedDetailItem) {
        val spbNo = selectedDetailItem?.get("spb_no") ?: ""
        if (spbNo.isNotEmpty()) {
            dbHelper.getSPBDetailsByNo(spbNo, fcba)
        } else {
            emptyList()
        }
    }
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
                Column(Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
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

                    // Di dalam ModalBottomSheet SPBListScreen
                    ListItem(
                        headlineContent = { Text("Cetak SPB (Thermal)") },
                        leadingContent = { Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF455A64)) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            selectedItemForMenu?.let { header ->
                                // 1. AMBIL DETAIL LANGSUNG DARI DB (Penting!)
                                // Kita tidak bisa mengandalkan detailsList dari state UI karena sering null saat menu dibuka
                                val spbNo = header["spb_no"] ?: ""
                                val actualDetails = if (spbNo.isNotEmpty()) {
                                    dbHelper.getSPBDetailsByNo(spbNo, fcba)
                                } else {
                                    emptyList()
                                }

                                val printData = header.toMutableMap()

                                // 2. Agregasi Lokasi dan TPH dari actualDetails
                                val allLocations = actualDetails.map { it["location_code"] ?: "" }
                                    .distinct()
                                    .filter { it.isNotEmpty() }
                                    .joinToString(", ")

                                val allTphs = actualDetails.map { it["tph_code"] ?: "" }
                                    .distinct()
                                    .filter { it.isNotEmpty() }
                                    .joinToString(", ")

                                val totalJanjang = actualDetails.sumOf { it["unit"]?.toIntOrNull() ?: 0 }

                                // 3. Masukkan ke printData dengan Key yang sesuai dengan PrintHelper.kt
                                printData["no_spb"] = spbNo
                                printData["mill_code"] = header["mill_code"] ?: "-"
                                printData["sopir_name"] = header["sopir_name"] ?: "-"
                                printData["vehicle_code"] = header["vehicle_code"] ?: "-"

                                // Data Agregasi
                                printData["location_code"] = if (allLocations.isEmpty()) "-" else allLocations
                                printData["tph_code"] = if (allTphs.isEmpty()) "-" else allTphs
                                printData["unit"] = totalJanjang.toString()

                                // 4. Kirim ke PrintHelper
                                PrintHelper.printDirect(context, printData)
                            }
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
                        // Gunakan key sesuai konstanta SH_... di DatabaseHelper
                        DetailRow("No SPB", selectedDetailItem!!["spb_no"])
                        DetailRow("Pabrik", selectedDetailItem!!["mill_code"])
                        DetailRow("Sopir", selectedDetailItem!!["sopir_name"])
                        DetailRow("Kendaraan", selectedDetailItem!!["vehicle_code"])
                        DetailRow("Pemuat 1", selectedDetailItem!!["pemuat_1"])
                        DetailRow("Pemuat 2", selectedDetailItem!!["pemuat_2"])

                        HorizontalDivider(Modifier.padding(vertical = 4.dp))

                        Text("Daftar Lokasi & Unit:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        // Tampilkan semua lokasi yang ada di tabel DETAIL
                        detailsList.forEach { detail ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• ${detail["location_code"]}", fontSize = 13.sp)
                                Text("${detail["unit"]} Jjg", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedDetailItem = null }) { Text("Tutup") }
                }
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
                Text(item["spb_no"] ?: "", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
                Text(item["created_at"] ?: "", fontSize = 12.sp, color = Color.Gray)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1.1f)) {
                    Text("Lokasi", fontSize = 10.sp, color = Color.Gray)
                    // Ambil first_loc (lokasi pertama) dari hasil query header
                    Text(item["first_loc"] ?: "-", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Column(Modifier.weight(0.9f)) {
                    Text("Total Janjang", fontSize = 10.sp, color = Color.Gray)
                    // Ambil total_unit (sum dari detail)
                    Text("${item["total_unit"] ?: "0"} Jjg", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Sopir", fontSize = 10.sp, color = Color.Gray)
                    // Gunakan "sopir_name" sesuai konstanta SH_SOPIR
                    Text(item["sopir_name"] ?: "-", fontWeight = FontWeight.Medium, fontSize = 13.sp)
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
        mills.addAll(dbHelper.getAllMills(fcba))



        vehicles.clear()
        vehicles.addAll(dbHelper.getAllVehicles(fcba))

        if (editId == null) {
            spbNo = dbHelper.generateNoSPB(fcba)
        }
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
            activity?.onNfcRead = { rawJson ->
                try {
                    val gson = com.google.gson.Gson()
                    val dataMap = gson.fromJson(rawJson, Map::class.java) as Map<String, Any>

                    val scannedLoc = dataMap["location_code"]?.toString() ?: ""
                    val scannedUnit = dataMap["unit"]?.toString() ?: "0"
                    val scannedTph = dataMap["tph_code"]?.toString() ?: "-"

                    activity?.runOnUiThread {
                        // Dialog harus terbuka agar scan diproses
                        if (showScanDialog && scannedLoc.isNotEmpty()) {
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

                                // --- KUNCI: DIALOG OTOMATIS TUTUP ---
                                showScanDialog = false

                                Toast.makeText(context, "Berhasil: $scannedLoc", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Kartu sudah ada di daftar", Toast.LENGTH_SHORT).show()
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
            title = { Text("Siap Scan Kartu", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(70.dp),
                        tint = Color(0xFF1A3A8F)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Silahkan tempelkan kartu NFC ke belakang HP Anda...", textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                TextButton(onClick = { showScanDialog = false }) {
                    Text("BATAL", color = Color.Red)
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
                // --- TAMPILAN STEP 2: SCAN LOKASI DENGAN NFC ---
                Card(
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("STEP 2: SCAN LOKASI NFC", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

                        // Tombol Tambah Data - Memicu Dialog Scan
                        Button(
                            onClick = {
                                showScanDialog = true
                                isScannerStarted = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
                        ) {
                            Icon(Icons.Default.Nfc, null)
                            Spacer(Modifier.width(8.dp))
                            Text("TAMBAH DATA (SCAN NFC)")
                        }

                        Spacer(Modifier.height(8.dp))

                        if (scannedCards.isNotEmpty()) {
                            Text("Daftar Lokasi Ter-scan:", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                            // List Kartu yang sudah ter-scan
                            scannedCards.forEachIndexed { index, card ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "${card["location_code"]}",
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "TPH: ${card["tph_code"] ?: "-"}",
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            "${card["unit"]} Jjg",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )

                                        IconButton(onClick = {
                                            // 1. Ambil unit untuk update total
                                            val unitToSubtract = card["unit"]?.toIntOrNull() ?: 0

                                            // 2. Hapus langsung dari list (TIDAK pakai '=' )
                                            if (index < scannedCards.size) {
                                                scannedCards.removeAt(index)

                                                // 3. Update Total
                                                totalUnit -= unitToSubtract
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, "Hapus", tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Total Unit
                            Surface(
                                color = Color(0xFF1A3A8F),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("TOTAL JANJANG", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("$totalUnit Jjg", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Tampilan jika belum ada data
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // PERBAIKAN: Gunakan modifier untuk mengatur size
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Belum ada data. Klik tombol di atas untuk scan.",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                    Spacer(Modifier.height(16.dp))

                    // TOMBOL SIMPAN
                        Button(
                            onClick = {
                                if (scannedCards.isEmpty()) {
                                    Toast.makeText(context, "Belum ada data lokasi yang di-scan!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                // 1. Siapkan Data Header
                                val headerMap = mapOf(
                                    "spb_no" to spbNo,
                                    "tanggal" to currentDate,
                                    "mill_code" to selectedMill,
                                    "sopir_name" to sopirName,
                                    "vehicle_code" to selectedVehicle,
                                    "pemuat_1" to pemuat1,
                                    "pemuat_2" to pemuat2,
                                    "fcba" to fcba
                                )

                                // 2. Siapkan Data Detail (Konversi dari scannedCards)
                                val detailsList = scannedCards.map { card ->
                                    mapOf(
                                        "location_code" to (card["location_code"] ?: ""),
                                        "unit" to (card["unit"] ?: "0"),
                                        "tph_code" to (card["tph_code"] ?: ""),
                                        "employee_code" to (card["employee_code"] ?: "")
                                    )
                                }

                                // 3. Panggil fungsi insertSPBFull yang baru dibuat di DatabaseHelper
                                val isSuccess = dbHelper.insertSPBFull(headerMap, detailsList)

                                if (isSuccess) {
                                    Toast.makeText(context, "SPB Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                                    onSuccess() // Kembali ke Dashboard atau Menu Utama
                                } else {
                                    Toast.makeText(context, "Gagal menyimpan SPB ke Database!", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
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

