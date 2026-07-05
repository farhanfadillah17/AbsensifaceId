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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                        Text("Blok: ${data["location_code"] ?: "-"}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

    // State untuk mengontrol visibilitas menu/dialog
    var showMenu by remember { mutableStateOf(false) }
    var selectedItemForMenu by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedDetailItem by remember { mutableStateOf<Map<String, String>?>(null) }
    var itemToDelete by remember { mutableStateOf<Map<String, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar RKH", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = Color(0xFF1A3A8F)) {
                Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White)
            }
        }
    ) { padding ->
        // --- TAMPILAN LIST DATA ---
        if (rkhDataList.isEmpty()) {
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada data RKH tersimpan", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rkhDataList) { rkh ->
                    RKHItemCard(
                        data = rkh,
                        onClick = {
                            // Klik biasa diarahkan langsung ke Detail (Opsional)
                            selectedDetailItem = rkh
                        },
                        onLongClick = {
                            // HOLD (Long Press) untuk memunculkan Bottom Sheet
                            selectedItemForMenu = rkh
                            showMenu = true
                        }
                    )
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
                        DetailRow("Lokasi/Blok", selectedDetailItem!!["location_code"])
                        DetailRow("Jumlah HK", selectedDetailItem!!["jumlah_hk"])
                        DetailRow("Unit", selectedDetailItem!!["unit"])
                        DetailRow("Output", selectedDetailItem!!["output"])
                        DetailRow("Supervisor", listOfNotNull(selectedDetailItem!!["supervisi1"], selectedDetailItem!!["supervisi2"], selectedDetailItem!!["supervisi3"], selectedDetailItem!!["supervisi4"]).filter { it.isNotEmpty() }.joinToString(", "))
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
                            val id = itemToDelete?.get("id")
                            if (id != null) {
                                if (dbHelper.deleteRKH(id)) {
                                    Toast.makeText(context, "Berhasil dihapus", Toast.LENGTH_SHORT).show()

                                    // PAKSA REFRESH menggunakan fungsi yang mengandung ID
                                    rkhDataList = dbHelper.getAllRKHListMap(fcba)
                                }
                            }
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
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // --- 1. STATE NAVIGATION ---
    var currentStep by remember { mutableStateOf(1) }

    // --- 2. STATE DATA ---
    val rkhTypes = remember {
        listOf(
            "RKH Panen (Perhitungan buah)",
            "RKH Perawatan (perawatan)",
            "RKH Bibitan (bibitan)",
            "RKH Traksi (umum)",
            "RKH Umum (umum)"
        )
    }
    var selectedType by remember { mutableStateOf("") }
    var showTypeDialog by remember { mutableStateOf(false) }

    // Tambahkan ini di bagian State Data
    var addedBlocks by remember { mutableStateOf(listOf<Map<String, String>>()) }

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

    var activeDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fcba) {
        noRkh = dbHelper.generateNoRKH(fcba)
    }



    // Data Lists
    val afdelingList = remember(fcba) { dbHelper.getAfdelingList(fcba) }
    val gangList = remember(fcba, selectedAfd) {
        if (selectedAfd.isNotEmpty()) dbHelper.getGangsByAfdeling(selectedAfd, fcba) else emptyList()
    }
    val jobList = remember(fcba) { dbHelper.getJobList(fcba) }
    val locationList = remember(fcba, selectedAfd, selectedType) {
        if (selectedType.contains("bibitan", ignoreCase = true)) {
            // JALUR PEMBIBITAN: Ambil data dari tabel Master Nursery
            dbHelper.getNurseryLocations(fcba)
        } else if (selectedAfd.isNotEmpty()) {
            // JALUR BLOK: Ambil blok berdasarkan Afdeling
            val filtered = dbHelper.getBlocksByLocation(selectedAfd)

            // Jika filter afdeling tidak menghasilkan data, tampilkan semua blok (fallback)
            if (filtered.isEmpty()) {
                dbHelper.getBlockList(fcba)
            } else {
                filtered
            }
        } else {
            // Default: Tampilkan semua blok jika afdeling belum dipilih
            dbHelper.getBlockList(fcba)
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
                    "LOC" -> selectedLoc = selectedValue
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("UNIT") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        OutlinedTextField(
                            value = output,
                            onValueChange = { output = it },
                            label = { Text("OUTPUT") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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




                            // TOMBOL TAMBAH KE DAFTAR (Bukan Simpan ke DB dulu)
                            Button(
                                onClick = {
                                    if (selectedLoc.isNotEmpty()) {
                                        // Menambahkan ke list 'addedBlocks' yang sudah Anda buat di State
                                        addedBlocks = addedBlocks + mapOf(
                                            "loc" to selectedLoc,
                                            "hk" to hk
                                        )
                                        // Reset input field saja, agar bisa pilih blok lain
                                        selectedLoc = ""

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
                                addedBlocks + mapOf("loc" to selectedLoc, "hk" to hk)
                            } else addedBlocks

                            if (finalBlocks.isEmpty()) {
                                Toast.makeText(context, "Belum ada blok yang dipilih!", Toast.LENGTH_SHORT).show()
                            } else {

                                android.util.Log.d("SIMPAN_RKH", "Menyimpan RKH: $noRkh | HK: $hk | Unit: $unit | Output: $output")
                                // Loop untuk simpan setiap blok ke Database dengan No RKH yang sama
                                finalBlocks.forEach { block ->
                                    dbHelper.insertRKHFull(
                                        noRkh = noRkh,
                                        tanggal = rkhDate,
                                        type = selectedType,
                                        fcba = fcba,
                                        afd = selectedAfd,
                                        gang = selectedGang,
                                        s1 = sup1Code, s2 = sup2Code, s3 = sup3Code, s4 = sup4Code,
                                        job = selectedJob,
                                        loc = block["loc"] ?: "",
                                        hk = block["hk"]?.toDoubleOrNull() ?: 0.0,
                                        unit = unit.toDoubleOrNull() ?: 0.0,
                                        out = output.toDoubleOrNull() ?: 0.0
                                    )
                                }
                                Toast.makeText(context, "Berhasil simpan ${finalBlocks.size} blok", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("SIMPAN RKH SEKARANG", fontWeight = FontWeight.Bold)
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