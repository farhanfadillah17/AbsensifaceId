package com.example.attendanceapp

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RKHItemCard(
    data: Map<String, String>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Baris 1: Nomor RKH
                Text(
                    text = data["no_rkh"] ?: "-",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A3A8F),
                    fontSize = 16.sp
                )
                Text(
                    text = data["type"]?.substringBefore(" ") ?: "",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            // Baris: JOB
            Text(
                text = data["job_name"] ?: data["job_code"] ?: "-",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black
            )

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Kolom 1
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text("Afd: ${data["afdeling"] ?: "-"}", fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, null, Modifier.size(14.dp), Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text("Gang: ${data["gangcode"] ?: "-"}", fontSize = 13.sp)
                    }
                }
                // Kolom 2
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, Modifier.size(14.dp), Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text("Blok: ${data["location"] ?: "-"}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, Modifier.size(14.dp), Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text("HK: ${data["jumlah_hk"] ?: "-"}", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHMainScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    fcba: String,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit // Untuk Edit Header
) {
    // 1. STATE MANAGEMENT - Gunakan getAllRKHListMap agar konsisten
    var rkhDataList by remember { mutableStateOf(dbHelper.getAllRKHListMap(fcba)) }
    Log.d("test rkh", "RKHMainScreen: $rkhDataList")
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<Map<String, String>>() }

    val todayLabel = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    // 2. REFRESH DATA OTOMATIS SAAT SCREEN DIBUKA
    LaunchedEffect(Unit) {
        // Memanggil fungsi yang sudah kita modifikasi dengan filter TODAY sebelumnya
        rkhDataList = dbHelper.getAllRKHListMap(fcba)
    }

    // State untuk mengontrol visibilitas menu/dialog
    var showMenu by remember { mutableStateOf(false) }
    var selectedItemForMenu by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedDetailItem by remember { mutableStateOf<Map<String, String>?>(null) }
    var itemToDelete by remember { mutableStateOf<Map<String, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSelectionMode) "Pilih RKH (${selectedItems.size})" else "Daftar RKH",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedItems.clear()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // TOMBOL CEKLIS DI POJOK KANAN ATAS
                    IconButton(onClick = {
                        isSelectionMode = !isSelectionMode
                        if (!isSelectionMode) selectedItems.clear()
                    }) {
                        Icon(
                            imageVector =Icons.Default.Checklist,
                            contentDescription = "Mode Seleksi",
                            tint = if (isSelectionMode) Color.Yellow else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        },
        floatingActionButton = {
            if (isSelectionMode) {
                // TOMBOL PRINT (Hanya muncul jika ada yang dipilih)
                if (selectedItems.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val itemsToPrint = selectedItems.toList() // Buat salinan list
                            if (itemsToPrint.isNotEmpty()) {
                                PrintHelper.printMultiple(context, itemsToPrint)
                                isSelectionMode = false
                                selectedItems.clear()
                            } else {
                                Toast.makeText(context, "Pilih data terlebih dahulu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        icon = { Icon(Icons.Default.Print, contentDescription = null) },
                        text = { Text("Cetak ${selectedItems.size} RKH") },
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                }
            } else {
                // TOMBOL TAMBAH BIASA
                FloatingActionButton(onClick = onAddClick, containerColor = Color(0xFF1A3A8F)) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White)
                }
            }
        }
    ) { padding ->
        // --- TAMPILAN LIST DATA ---
        if (rkhDataList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada data RKH tersimpan hari ini", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rkhDataList) { rkh ->
                    val isSelected = selectedItems.contains(rkh)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSelectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedItems.add(rkh) else selectedItems.remove(rkh)
                                }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            RKHItemCard(
                                data = rkh,
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedItems.remove(rkh) else selectedItems.add(rkh)
                                    } else {
                                        selectedDetailItem = rkh
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedItemForMenu = rkh
                                        showMenu = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 1. MODAL BOTTOM SHEET (LONG PRESS MENU) KONSISTEN ---
        if (showMenu && selectedItemForMenu != null) {
            ModalBottomSheet(
                onDismissRequest = { showMenu = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Opsi RKH: ${selectedItemForMenu!!["no_rkh"] ?: "-"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A3A8F),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // OPSI: LIHAT DATA
                    ListItem(
                        headlineContent = { Text("Lihat Data") },
                        leadingContent = { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.Blue) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            selectedDetailItem = selectedItemForMenu
                        }
                    )

                    // OPSI: EDIT HEADER
                    ListItem(
                        headlineContent = { Text("Edit Header") },
                        leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFF57C00)) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            onEditClick(selectedItemForMenu!!["id"] ?: "")
                        }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

                    // OPSI: HAPUS DATA
                    ListItem(
                        headlineContent = { Text("Hapus Data", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            itemToDelete = selectedItemForMenu
                        }
                    )

                    // OPSI: PRINT (TAMBAHAN BARU)
                    ListItem(
                        headlineContent = { Text("Cetak RKH (Thermal)") },
                        leadingContent = { Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF455A64)) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            selectedItemForMenu?.let { rkhData ->
                                PrintHelper.printDirect(context, rkhData)
                            }
                        }
                    )
                }
            }
        }

        // --- 2. DIALOG DETAIL (LIHAT DATA) ---
        if (selectedDetailItem != null) {
            AlertDialog(
                onDismissRequest = { selectedDetailItem = null },
                title = { Text("Detail RKH", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("No RKH", selectedDetailItem!!["no_rkh"])
                        DetailRow("Tipe", selectedDetailItem!!["type"])
                        DetailRow("Afdeling", selectedDetailItem!!["afdeling"])
                        DetailRow("Gang", selectedDetailItem!!["gangcode"])
                        DetailRow("Pekerjaan", "${selectedDetailItem!!["job_code"]} - ${selectedDetailItem!!["job_name"]}")
                        DetailRow("Lokasi/Blok", selectedDetailItem!!["location"])
                        DetailRow("Jumlah HK", selectedDetailItem!!["jumlah_hk"])
                        DetailRow("Unit", "${selectedDetailItem!!["unit"]} ${selectedDetailItem!!["uom_unit"]}")
                        DetailRow("Output", "${selectedDetailItem!!["output"]} ${selectedDetailItem!!["uom_output"]}")
                        DetailRow("Supervisor", listOfNotNull(selectedDetailItem!!["sup1_name"], selectedDetailItem!!["sup2_name"], selectedDetailItem!!["sup3_name"], selectedDetailItem!!["sup4_name"]).filter { it.isNotEmpty() }.joinToString("\n "))
                        DetailRow("Tgl Buat", selectedDetailItem!!["created_at"])
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedDetailItem = null }) { Text("Tutup") }
                }
            )
        }

        // --- 3. DIALOG KONFIRMASI HAPUS ---
        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Hapus Data") },
                text = { Text("Apakah Anda yakin ingin menghapus RKH ${itemToDelete!!["no_rkh"]}?") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        // Di dalam Dialog Konfirmasi Hapus RKHFormScreen.kt
                        onClick = {
                            val rkhId = itemToDelete?.get("id")
                            if (!rkhId.isNullOrEmpty()) {
                                // 1. Hapus dari database (Gunakan ID agar hanya baris tersebut yang terhapus)
                                if (dbHelper.deleteRKHById(rkhId)) {
                                    Toast.makeText(context, "Berhasil dihapus", Toast.LENGTH_SHORT).show()

                                    // 2. Ambil data terbaru dari database
                                    rkhDataList = dbHelper.getAllRKHListMap(fcba)
                                }
                            }
                            // 3. Tutup dialog konfirmasi hapus
                            itemToDelete = null
                        }
                    ) { Text("Hapus", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) { Text("Batal") }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHFormScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    fcba: String,
    editId: String? = null,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isEditMode = editId != null && editId.isNotEmpty()
    // --- 1. STATE NAVIGATION ---
    var currentStep by remember { mutableStateOf(1) }

    // --- 2. STATE DATA ---
    val rkhTypes = remember {
        listOf(
            "RKH Panen",
            "RKH Perawatan",
            "RKH Bibitan",
            "RKH Traksi",   // Pastikan ada kata TRAKSI untuk filter
            "RKH Workshop",
            "RKH Umum"
        )
    }
    var selectedType by remember { mutableStateOf("") }
    var showTypeDialog by remember { mutableStateOf(false) }

    // Tambahkan ini di bagian State Data
    var addedBlocks by remember { mutableStateOf(listOf<Map<String, String>>()) }
    var originalIds by remember { mutableStateOf(setOf<String>()) }

    var noRkh by remember { mutableStateOf("Generating...") }
    var rkhDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    var sup1Code by remember { mutableStateOf("") }
    var sup1Name by remember { mutableStateOf("") }
    var sup2Code by remember { mutableStateOf("") }
    var sup2Name by remember { mutableStateOf("") }
    var sup3Code by remember { mutableStateOf("") }
    var sup3Name by remember { mutableStateOf("") }
    var sup4Code by remember { mutableStateOf("") }
    var sup4Name by remember { mutableStateOf("") }

    var selectedAfd by remember { mutableStateOf("") }
    var selectedGang by remember { mutableStateOf("") }
    var selectedJob by remember { mutableStateOf("") }
    var selectedLoc by remember { mutableStateOf("") }

    var hk by remember { mutableStateOf("") } // Jumlah HK sekarang di Step 1
    var unit by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var bjrValue by remember { mutableStateOf("0.0") }
    var weightValue by remember { mutableStateOf("0.0") }

    var activeDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editId) {
        if (editId != null) {
            Log.d("RKH_EDIT", "Memuat data untuk No RKH: $editId")

            // 1. Ambil Data Header berdasarkan ID
            val headerData = dbHelper.getRKHById(editId)
            headerData?.let {
                val currentNoRkh = it["no_rkh"] ?: editId
                noRkh = currentNoRkh
                selectedType = it["type_rkh"] ?: it["type"] ?: ""
                selectedAfd = it["afdeling"] ?: ""
                selectedGang = it["gangcode"] ?: ""
                
                // Ambil Nama Job berdasarkan Code yang tersimpan
                val jobCode = it["job_code"] ?: ""
                val jobData = dbHelper.getJobByCode(jobCode, fcba)
                selectedJob = if (jobData != null) "${jobData["FCCODE"]} - ${jobData["FCNAME"]}" else jobCode
                
                hk = it["jumlah_hk"] ?: ""

                // Supervisor
                sup1Code = it["supervisi1_code"] ?: ""
                sup1Name = it["supervisi1_name"] ?: ""
                sup2Code = it["supervisi2_code"] ?: ""
                sup2Name = it["supervisi2_name"] ?: ""
                sup3Code = it["supervisi3_code"] ?: ""
                sup3Name = it["supervisi3_name"] ?: ""
                sup4Code = it["supervisi4_code"] ?: ""
                sup4Name = it["supervisi4_name"] ?: ""

                // 2. Ambil SEMUA BLOK untuk RKH ini agar tidak hilang saat save
                val blocks = dbHelper.getRKHBlocksByNo(currentNoRkh, fcba)
                addedBlocks = blocks
                originalIds = blocks.mapNotNull { it["id"] }.toSet()
            }

        } else {
            // --- MODE BARU ---
            Log.d("RKH_DEBUG", "Mode Baru, Generating No RKH...")
            // Pastikan fcba tidak kosong sebelum generate
            if (fcba.isNotEmpty()) {
                val generated = dbHelper.generateNoRKH(fcba)
                noRkh = generated // Ini akan mengubah "Generating..." menjadi nomor asli
            }
        }
    }



    LaunchedEffect(unit, bjrValue) {
        val u = unit.toDoubleOrNull() ?: 0.0
        val b = bjrValue.toDoubleOrNull() ?: 0.0
        weightValue = String.format(java.util.Locale.US, "%.2f", u * b)
    }

    // Data Lists
    val focusManager = LocalFocusManager.current
    val afdelingList = remember(fcba) { dbHelper.getAfdelingList(fcba) }
    val gangList = remember(fcba, selectedAfd) {
        if (selectedAfd.isNotEmpty()) dbHelper.getGangsByAfdeling(selectedAfd, fcba) else emptyList()
    }
    val jobList = remember(fcba, selectedType) { dbHelper.getJobList(fcba, selectedType) }
    val locationList = remember(fcba, selectedAfd, selectedType) {
        val type = selectedType.uppercase()
        val cleanFcba = fcba.trim()
        val cleanAfd = selectedAfd.trim()

        when {
            // 1. JALUR TRAKSI
            type.contains("TRAKSI") -> {
                val data = dbHelper.getTraksiVehicleMaster(cleanFcba)
                if (data.isEmpty()) {
                    Log.d("RKH_FLOW", "Data Traksi Kosong di DB")
                    emptyList()
                } else {
                    data.map { "${it["code"]} - ${it["name"]}" }
                }
            }

            // 2. JALUR WORKSHOP
            type.contains("WORKSHOP") -> {
                val data = dbHelper.getWorkshopMaster(cleanFcba)
                data.map { "${it["code"]} - ${it["name"]}" }
            }

            type.contains("UMUM") -> {
                val data = dbHelper.getGcMaster(cleanFcba)
                if (data.isEmpty()) {
                    emptyList()
                } else {
                    // Menampilkan format: KODE - NAMA
                    data.map { "${it["code"]} - ${it["name"]}" }
                }
            }

            // 3. JALUR BIBITAN
            type.contains("BIBITAN") -> {
                dbHelper.getNurseryLocations(cleanFcba)
            }

            // 4. JALUR PERAWATAN / UMUM / PANEN / PERHITUNGAN BUAH
            // Jika Afdeling dipilih, gunakan filter Afdeling
            cleanAfd.isNotEmpty() -> {
                val blocksByAfd = dbHelper.getBlocksByLocation(cleanAfd, fcba)
                if (blocksByAfd.isEmpty()) {
                    // Fallback: Jika blok per afdeling tidak ditemukan, coba ambil semua blok di FCBA tersebut
                    dbHelper.getBlockList(fcba)
                } else {
                    blocksByAfd
                }
            }

            // 5. JALUR DEFAULT (Fallback jika Afdeling belum dipilih atau tipe lainnya)
            else -> {
                val allBlocks = dbHelper.getBlockList(cleanFcba)
                if (allBlocks.isEmpty()) {
                    Log.d("RKH_FLOW", "Semua jalur lokasi kosong untuk FCBA: $cleanFcba")
                }
                allBlocks
            }
        }
    }
    val supervisorOptions = remember(fcba) { dbHelper.getSupervisorsMap(fcba) }

    // Dialog Logic
    if (activeDialog != null) {
        if (activeDialog!!.startsWith("SUP")) {
            SearchableListDialog(
                title = "Cari $activeDialog",
                options = supervisorOptions.map { it["name"] ?: "" },
                onDismiss = { activeDialog = null }
            ) { selectedName ->
                val selectedCode = supervisorOptions.find { it["name"] == selectedName }?.get("code") ?: ""
                when (activeDialog) {
                    "SUP1" -> { sup1Code = selectedCode; sup1Name = selectedName }
                    "SUP2" -> { sup2Code = selectedCode; sup2Name = selectedName }
                    "SUP3" -> { sup3Code = selectedCode; sup3Name = selectedName }
                    "SUP4" -> { sup4Code = selectedCode; sup4Name = selectedName }
                }
                activeDialog = null
            }
        } else {
            SearchableListDialog(
                title = "Cari $activeDialog",
                options = when (activeDialog) {
                    "AFD" -> afdelingList
                    "GANG" -> gangList
                    "JOB" -> jobList
                    "LOC" -> locationList
                    else -> emptyList()
                },
                onDismiss = { activeDialog = null }
            ) { selectedValue ->
                when (activeDialog) {
                    "AFD" -> { selectedAfd = selectedValue; selectedGang = "" }
                    "GANG" -> selectedGang = selectedValue
                    "JOB" -> selectedJob = selectedValue
                    "LOC" -> {
                        selectedLoc = selectedValue
                        if (selectedType.uppercase().contains("PANEN")) {
                            val bjr = dbHelper.getBjrByBlock(selectedValue, fcba)
                            bjrValue = bjr.toString()
                        } else {
                            bjrValue = "0.0"
                        }
                    }
                }
                activeDialog = null
            }
        }
    }


    if (showTypeDialog) {
        MasterSearchDialog(
            title = "Pilih Tipe RKH",
            items = rkhTypes,
            onDismiss = { showTypeDialog = false },
            onSelect = { selectedType = it; showTypeDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentStep == 1) "Input RKH - Step 1" else "Input RKH - Step 2", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        // PERBAIKAN NAVIGASI BACK
                        if (currentStep == 2) currentStep = 1 else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A3A8F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentStep == 1) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("PENGESAHAN RKH", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A3A8F))

                        OutlinedTextField(value = fcba, onValueChange = {}, label = { Text("FCBA") }, modifier = Modifier.fillMaxWidth(), readOnly = true)
                        ClickableSearchField(label = "TYPE RKH", value = selectedType) { showTypeDialog = true }
                        ClickableSearchField(label = "AFDELING", value = selectedAfd) { activeDialog = "AFD" }
                        ClickableSearchField(label = "GANG CODE", value = selectedGang, enabled = selectedAfd.isNotEmpty()) { activeDialog = "GANG" }

                        // PERBAIKAN: SUPERVISI SEKARANG BISA DIPILIH (Bukan Ketik Manual)
                        Text("PERSONIL SUPERVISI", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        ClickableSearchField(label = "SUPERVISI 1", value = sup1Name) { activeDialog = "SUP1" }
                        ClickableSearchField(label = "SUPERVISI 2", value = sup2Name) { activeDialog = "SUP2" }
                        ClickableSearchField(label = "SUPERVISI 3", value = sup3Name) { activeDialog = "SUP3" }
                        ClickableSearchField(label = "SUPERVISI 4", value = sup4Name) { activeDialog = "SUP4" }

                        Text("DETAIL PEKERJAAN", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        ClickableSearchField(label = "JOB CODE", value = selectedJob) { activeDialog = "JOB" }

                        OutlinedTextField(
                            value = hk,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) hk = it },
                            label = { Text("JUMLAH HK") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true, // KUNCI UTAMA: Agar tidak bisa enter baris baru
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done // Selesai / Tutup Keyboard
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (selectedAfd.isNotEmpty() && selectedGang.isNotEmpty() && selectedJob.isNotEmpty() && hk.isNotEmpty()) {
                            currentStep = 2
                        } else {
                            Toast.makeText(context, "Lengkapi data (Afd, Gang, Job, HK)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("NEXT (PILIH LOKASI)") }

            } else {
                // --- STEP 2: LOKASI & DAFTAR BLOK ---
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val isNursery = selectedType.contains("bibitan", ignoreCase = true)

                            Text(
                                text = if (isNursery) "INPUT NURSERY UNTUK RKH: $noRkh" else "INPUT BLOK UNTUK RKH: $noRkh",
                                fontWeight = FontWeight.Bold,
                                color = if (isNursery) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )

                            ClickableSearchField(
                                label = if (isNursery) "PILIH LOKASI NURSERY" else "PILIH LOKASI / BLOK",
                                value = selectedLoc
                            ) {
                                activeDialog = "LOC"
                            }

                            OutlinedTextField(
                                value = unit,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) unit = it },
                                label = { Text("Unit") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                            )

                            if (selectedType.uppercase().contains("PANEN")) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = bjrValue,
                                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) bjrValue = it },
                                        label = { Text("BJR") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                                    )
                                    OutlinedTextField(
                                        value = weightValue,
                                        onValueChange = {},
                                        label = { Text("Weight (KG)") },
                                        modifier = Modifier.weight(1f),
                                        readOnly = true,
                                        singleLine = true
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = output,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) output = it },
                                label = { Text("Output") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                            )

                            // TOMBOL TAMBAH KE DAFTAR (Hanya muncul jika BUKAN mode edit)
//                            if (!isEditMode) {
                                Button(
                                    onClick = {
                                        if (selectedLoc.isNotEmpty()) {
                                            // Menambahkan ke list 'addedBlocks' yang sudah Anda buat di State
                                            addedBlocks = addedBlocks + mapOf(
                                                "loc" to selectedLoc,
                                                "hk" to hk,
                                                "unit" to unit,
                                                "output" to output,
                                                "bjr" to bjrValue,
                                                "weight" to weightValue
                                            )
                                            // Reset input field saja, agar bisa pilih blok lain
                                            selectedLoc = ""
                                            unit = ""
                                            output = ""
                                            bjrValue = "0.0"
                                            weightValue = "0.0"

                                        } else {
                                            Toast.makeText(context, "Pilih lokasi dulu!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Tambah Blok ke Daftar")
                                }
//                            }
                        }
                    }

                    // --- TAMPILAN DAFTAR BLOK YANG SUDAH DITAMBAHKAN ---
                    if (addedBlocks.isNotEmpty()) {
                        Text("Daftar Blok Terpilih (${addedBlocks.size}):", fontWeight = FontWeight.Bold)
                        addedBlocks.forEachIndexed { index, block ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}. ", fontWeight = FontWeight.Bold)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Blok: ${block["loc"]}", fontWeight = FontWeight.SemiBold)
                                        val detailText = if (selectedType.uppercase().contains("PANEN")) {
                                            "Unit: ${block["unit"]} | BJR: ${block["bjr"]} | Berat: ${block["weight"]} KG"
                                        } else {
                                            "Unit: ${block["unit"]} | Output: ${block["output"]}"
                                        }
                                        Text(detailText, fontSize = 12.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = {
                                        addedBlocks = addedBlocks.filterIndexed { i, _ -> i != index }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // TOMBOL FINAL SIMPAN SEMUA KE DATABASE
                    Button(
                        onClick = {
                            val finalBlocks = if (selectedLoc.isNotEmpty()) {
                                addedBlocks + mapOf(
                                    "loc" to selectedLoc,
                                    "hk" to hk,
                                    "unit" to unit,
                                    "output" to output,
                                    "bjr" to bjrValue,
                                    "weight" to weightValue
                                )
                            } else addedBlocks

                            if (finalBlocks.isEmpty()) {
                                Toast.makeText(context, "Pilih lokasi dulu!", Toast.LENGTH_SHORT).show()
                            } else {
                                if (isEditMode) {
                                    // --- LOGIKA UPDATE BERBASIS ID ---
                                    val currentIds = finalBlocks.mapNotNull { it["id"] }.toSet()
                                    val idsToDelete = originalIds - currentIds

                                    // 1. Hapus record yang dibuang dari list
                                    idsToDelete.forEach { id ->
                                        dbHelper.deleteRKHById(id)
                                    }

                                    // 2. Update atau Insert record yang ada di list
                                    finalBlocks.forEach { block ->
                                        val blockId = block["id"]
                                        if (!blockId.isNullOrEmpty()) {
                                            // UPDATE EXISTING
                                            dbHelper.updateRKHFullById(
                                                id = blockId,
                                                noRkh = noRkh,
                                                tanggal = rkhDate,
                                                type = selectedType,
                                                fcba = fcba,
                                                afd = selectedAfd,
                                                gang = selectedGang,
                                                s1 = sup1Code, s2 = sup2Code, s3 = sup3Code, s4 = sup4Code,
                                                job = selectedJob.substringBefore(" - "),
                                                loc = block["loc"] ?: "",
                                                hk = block["hk"]?.toDoubleOrNull() ?: 0.0,
                                                unit = block["unit"] ?: "",
                                                out = block["output"]?.toDoubleOrNull() ?: 0.0,
                                                bjr = block["bjr"]?.toDoubleOrNull() ?: 0.0,
                                                weight = block["weight"]?.toDoubleOrNull() ?: 0.0
                                            )
                                        } else {
                                            // INSERT NEW
                                            dbHelper.insertRKHFull(
                                                noRkh = noRkh,
                                                tanggal = rkhDate,
                                                type = selectedType,
                                                fcba = fcba,
                                                afd = selectedAfd,
                                                gang = selectedGang,
                                                s1 = sup1Code, s2 = sup2Code, s3 = sup3Code, s4 = sup4Code,
                                                job = selectedJob.substringBefore(" - "),
                                                loc = block["loc"] ?: "",
                                                hk = block["hk"]?.toDoubleOrNull() ?: 0.0,
                                                unit = block["unit"] ?: "",
                                                out = block["output"]?.toDoubleOrNull() ?: 0.0,
                                                bjr = block["bjr"]?.toDoubleOrNull() ?: 0.0,
                                                weight = block["weight"]?.toDoubleOrNull() ?: 0.0
                                            )
                                        }
                                    }
                                    Toast.makeText(context, "RKH Berhasil Diperbarui", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                } else {
                                    // --- LOGIKA SIMPAN BARU ---
                                    finalBlocks.forEach { block ->
                                        dbHelper.insertRKHFull(
                                            noRkh = noRkh,
                                            tanggal = rkhDate,
                                            type = selectedType,
                                            fcba = fcba,
                                            afd = selectedAfd,
                                            gang = selectedGang,
                                            s1 = sup1Code, s2 = sup2Code, s3 = sup3Code, s4 = sup4Code,
                                            job = selectedJob.substringBefore(" - "),
                                            loc = block["loc"] ?: "",
                                            hk = block["hk"]?.toDoubleOrNull() ?: 0.0,
                                            unit = block["unit"] ?: "",
                                            out = block["output"]?.toDoubleOrNull() ?: 0.0,
                                            bjr = block["bjr"]?.toDoubleOrNull() ?: 0.0,
                                            weight = block["weight"]?.toDoubleOrNull() ?: 0.0
                                        )
                                    }
                                    Toast.makeText(context, "Data Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditMode) Color(0xFFF2994A) else Color(0xFF2E7D32)
                        )
                    ) {
                        Icon(if (isEditMode) Icons.Default.Edit else Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isEditMode) "PERBARUI RKH" else "SIMPAN RKH SEKARANG")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value ?: "-",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF1A3A8F) // Menggunakan warna biru gelap agar senada
        )
    }
}