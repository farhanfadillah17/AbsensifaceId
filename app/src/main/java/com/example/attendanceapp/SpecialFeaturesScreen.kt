package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign

// ... (bagian import tetap sama)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressMenuScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    onBack: () -> Unit,
) {
    // Navigasi internal: "CHOOSER", "LIST", "FORM"
    var currentStep by remember { mutableStateOf("CHOOSER") }
    var selectedCategory by remember { mutableStateOf("") }

    // State untuk data list
    var progressData by remember { mutableStateOf(emptyList<Map<String, String>>()) }

    // --- FIX NAVIGASI: Menangani tombol Back fisik/gestur HP ---
    BackHandler(enabled = true) {
        when (currentStep) {
            "FORM" -> currentStep = "LIST"
            "LIST" -> currentStep = "CHOOSER"
            else -> onBack() // Jika di CHOOSER, baru kembali ke Dashboard
        }
    }

    LaunchedEffect(currentStep, selectedCategory) {
        if (currentStep == "LIST") {
            // Memastikan data terbaru diambil dari DB
            progressData = dbHelper.getAllProgress().filter {
                it["category"]?.uppercase() == selectedCategory.uppercase()
            }
        }
    }

    when (currentStep) {
        "CHOOSER" -> {
            ProgressCategoryChooser(
                onBack = onBack,
                onSelect = { category ->
                    selectedCategory = category
                    currentStep = "LIST"
                }
            )
        }
        "LIST" -> {
            ProgressListScreen(
                category = selectedCategory,
                progressData = progressData,
                // --- FIX NAVIGASI: Tombol ArrowBack di TopAppBar ---
                onBack = { currentStep = "CHOOSER" },
                onAddClick = { currentStep = "FORM" }
            )
        }
        "FORM" -> {
            ProgressFormScreen(
                category = selectedCategory,
                dbHelper = dbHelper,
                empId = empId,
                onBack = { currentStep = "LIST" },
                onSuccess = { currentStep = "LIST" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressCategoryChooser(onBack: () -> Unit, onSelect: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Kategori Progress", color = Color.White, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Silahkan pilih kategori pekerjaan:",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            // Baris Tombol Berdampingan (Simpel seperti Absen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tombol Perawatan (Hijau - Seperti Absen Masuk)
                SimpleCategoryButton(
                    title = "PERAWATAN",
                    icon = Icons.Default.Build,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect("PERAWATAN") }
                )

                // Tombol Pembibitan (Orange/Kuning - Seperti Absen Keluar)
                SimpleCategoryButton(
                    title = "PEMBIBITAN",
                    icon = Icons.Default.Spa,
                    color = Color(0xFFF9A825),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect("PEMBIBITAN") }
                )
            }
        }
    }
}

@Composable
fun SimpleCategoryButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(110.dp), // Ukuran kotak proporsional
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun CategoryCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(48.dp))
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProgressListContent(padding: PaddingValues, data: List<Map<String, String>>) {
    if (data.isEmpty()) {
        Box(Modifier
            .fillMaxSize()
            .padding(padding), contentAlignment = Alignment.Center) {
            Text("Belum ada data progres tersimpan", color = Color.Gray)
        }
    } else {
        LazyColumn(Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            items(data) { item ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("RKH: ${item["no_rkh"]}", fontWeight = FontWeight.Bold)
                        Text("Kategori: ${item["category"]}")
                        Text("Lokasi: ${item["location_code"]}")
                        Text("Waktu: ${item["created_at"]}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressListScreen(
    category: String,
    progressData: List<Map<String, String>>,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data $category", color = Color.White) },
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
                onClick = onAddClick,
                containerColor = Color(0xFF1A3A8F),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        if (progressData.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada data $category tersimpan", color = Color.Gray)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(progressData) { item ->
                    Card(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("RKH: ${item["no_rkh"]}", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

                            // --- FIX BUG LOKASI ---
                            // Gunakan "location_code" sesuai mapping di DatabaseHelper
                            // Tambahkan fallback "Tidak ada lokasi" jika null
                            val lokasi = item["location_code"] ?: item["block_code"] ?: "Lokasi Kosong"
                            Text("Lokasi: $lokasi", fontSize = 14.sp)

                            Text("Waktu: ${item["created_at"]}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressFormScreen(
    category: String,
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    // Gunakan .uppercase() untuk memastikan sinkron dengan database
    val fcbaUser = (sharedPref.getString("fcba", "SRE") ?: "SRE").uppercase()

    // --- DATA SOURCES ---
    val rkhList = remember { dbHelper.getRKHList() }
    val supervisorOptions = remember { dbHelper.getSupervisors() }
    val jobMasterList = remember { dbHelper.getDropdownData("JOB", fcbaUser) }
    // List akan otomatis memuat ulang jika fcbaUser berubah
    val availableStaff = remember(fcbaUser) {
        try {
            dbHelper.getEmployeesAlreadyCheckedIn(fcbaUser)
        } catch (e: Exception) {
            emptyList<Map<String, String>>()
        }
    }

    // --- FORM STATES ---
    var selectedRKH by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedBlock by remember { mutableStateOf("") }
    var selectedJobType by remember { mutableStateOf("") }

    // PERBAIKAN: Supervisi diubah menjadi 4
    var supervisi1 by remember { mutableStateOf("") }
    var supervisi2 by remember { mutableStateOf("") }
    var supervisi3 by remember { mutableStateOf("") }
    var supervisi4 by remember { mutableStateOf("") }

    var selectedEmployees by remember { mutableStateOf(setOf<String>()) }

    var unit by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var lembur by remember { mutableStateOf("0") }
    var dapatBeras by remember { mutableStateOf(false) }
    var keterangan by remember { mutableStateOf("") }

    // State untuk Dialog Pencarian (Shared Component)
    var activeDialog by remember { mutableStateOf<String?>(null) }

    // Logic: Blok berdasarkan lokasi RKH
    val locationCode = selectedRKH?.get("location") ?: ""
    val blockList = remember(locationCode) {
        if (locationCode.isNotEmpty()) dbHelper.getBlocksByLocation(locationCode) else emptyList()
    }

    // --- LOGIKA DIALOG PENCARIAN (Shared Component) ---
    if (activeDialog != null) {
        when (activeDialog) {
            "RKH" -> {
                SearchableMapDialog(
                    title = "Cari No. RKH",
                    options = rkhList,
                    displayProvider = { "RKH:${it["no_rkh"]} - ${it["location"]}" },
                    onDismiss = { activeDialog = null },
                    onSelect = {
                        selectedRKH = it
                        selectedBlock = "" // Reset blok saat RKH ganti
                        activeDialog = null
                    }
                )
            }
            "BLOCK" -> {
                SearchableListDialog(
                    title = "Pilih Blok",
                    options = blockList,
                    onDismiss = { activeDialog = null },
                    onSelect = { selectedBlock = it; activeDialog = null }
                )
            }
            "JOB" -> {
                SearchableListDialog(
                    title = "Jenis Pekerjaan",
                    options = jobMasterList,
                    onDismiss = { activeDialog = null },
                    onSelect = { selectedJobType = it; activeDialog = null }
                )
            }
            "SUP1", "SUP2", "SUP3", "SUP4" -> {
                SearchableListDialog(
                    title = "Pilih Supervisi",
                    options = supervisorOptions,
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Form $category", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. RKH
            Text("Referensi RKH", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            ClickableSearchField(
                label = "Pilih No. RKH",
                value = if (selectedRKH != null) "RKH:${selectedRKH?.get("no_rkh")} - ${selectedRKH?.get("location")}" else "",
                onClick = { activeDialog = "RKH" }
            )

            // 2. Info Otomatis dari RKH (Read Only)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selectedRKH?.get("gang_code") ?: "",
                    onValueChange = {},
                    label = { Text("Gang") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(disabledContainerColor = Color(0xFFF0F0F0), disabledTextColor = Color.Black)
                )
                OutlinedTextField(
                    value = selectedRKH?.get("job_code") ?: "",
                    onValueChange = {},
                    label = { Text("Job RKH") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(disabledContainerColor = Color(0xFFF0F0F0), disabledTextColor = Color.Black)
                )
            }

            // 3. Detail Pekerjaan
            Text("Detail Pekerjaan", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            ClickableSearchField("Pilih Blok", selectedBlock) {
                if (locationCode.isEmpty()) Toast.makeText(context, "Pilih RKH dulu!", Toast.LENGTH_SHORT).show()
                else activeDialog = "BLOCK"
            }
            ClickableSearchField("Jenis Pekerjaan", selectedJobType) { activeDialog = "JOB" }

            // 4. Supervisi (Diubah menjadi 4 baris)
            Text("Personil Supervisi", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            ClickableSearchField("Supervisi 1 (Wajib)", supervisi1) { activeDialog = "SUP1" }
            ClickableSearchField("Supervisi 2", supervisi2) { activeDialog = "SUP2" }
            ClickableSearchField("Supervisi 3", supervisi3) { activeDialog = "SUP3" }
            ClickableSearchField("Supervisi 4", supervisi4) { activeDialog = "SUP4" }

            // 5. Karyawan
            Text("Pilih Karyawan", fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                Column(Modifier
                    .padding(8.dp)
                    .heightIn(max = 250.dp)
                    .verticalScroll(rememberScrollState())) {
                    if (availableStaff.isEmpty()) {
                        Text("Tidak ada karyawan absen", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                    }
                    availableStaff.forEach { staff ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedEmployees.contains(staff["id"]),
                                onCheckedChange = { isChecked ->
                                    val current = selectedEmployees.toMutableSet()
                                    if (isChecked) current.add(staff["id"]!!) else current.remove(staff["id"])
                                    selectedEmployees = current
                                }
                            )
                            Text("${staff["id"]} - ${staff["name"]}", fontSize = 12.sp)
                        }
                    }
                }
            }
            Text("Terpilih: ${selectedEmployees.size} Karyawan", fontSize = 11.sp, color = Color.Gray)

            // 6. Hasil Kerja
            Text("Hasil Kerja", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(output, { output = it }, label = { Text("Output") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(lembur, { lembur = it }, label = { Text("Jam Lembur") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.width(16.dp))
                Checkbox(checked = dapatBeras, onCheckedChange = { dapatBeras = it })
                Text("Beras")
            }

            OutlinedTextField(keterangan, { keterangan = it }, label = { Text("Keterangan") }, modifier = Modifier
                .fillMaxWidth()
                .height(90.dp))

            Button(
                onClick = {
                    if (selectedRKH == null) {
                        Toast.makeText(context, "Pilih RKH!", Toast.LENGTH_SHORT).show()
                    } else if (supervisi1.isEmpty()) {
                        Toast.makeText(context, "Supervisi 1 wajib!", Toast.LENGTH_SHORT).show()
                    } else if (selectedEmployees.isEmpty()) {
                        Toast.makeText(context, "Pilih karyawan!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Mengirim list supervisi (1-4) ke database helper
                        // Di dalam ProgressFormScreen bagian Button onClick
                        dbHelper.savePlantationProgress(
                            rkh = selectedRKH!!["no_rkh"] ?: "",
                            category = category, // Ini akan berisi "PERAWATAN" atau "PEMBIBITAN"
                            employees = selectedEmployees.toList(),
                            supervisors = listOf(supervisi1, supervisi2, supervisi3, supervisi4),
                            unit = unit.toDoubleOrNull() ?: 0.0,
                            output = output.toDoubleOrNull() ?: 0.0,
                            rate = rate.toDoubleOrNull() ?: 0.0,
                            lembur = lembur.toIntOrNull() ?: 0,
                            beras = if (dapatBeras) 1 else 0,
                            locationCode = selectedBlock
                        )
                        onSuccess() // Ini akan memicu currentStep = "LIST" di ProgressMenuScreen
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("SIMPAN PROGRESS")
            }
        }
    }
}