package com.example.attendanceapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.decodeBitmap

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.layout.ContentScale
import com.google.accompanist.permissions.isGranted
import java.io.ByteArrayOutputStream
import com.google.accompanist.permissions.rememberPermissionState

// ... (bagian import tetap sama)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressMenuScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    fcba: String,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    // Navigasi internal: "CHOOSER", "LIST", "FORM"
    var currentStep by remember { mutableStateOf("CHOOSER") }
    var selectedCategory by remember { mutableStateOf("") }
    var itemToEdit by remember { mutableStateOf<Map<String, String>?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Any?>(null) } // Sesuaikan tipe datanya
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
                dbHelper = dbHelper, // Kirim dbHelper
                onBack = { currentStep = "CHOOSER" },
                onAddClick = {
                    itemToEdit = null // Reset agar form kosong (Input Baru)
                    currentStep = "FORM"
                },
                onEditClick = { item ->
                    itemToEdit = item  // Simpan data item yang diklik ke state
                    currentStep = "FORM" // Pindah ke layar form
                },
                onRefresh = {
                    // Trigger LaunchedEffect untuk ambil data ulang
                    progressData = dbHelper.getAllProgress().filter {
                        it["category"]?.uppercase() == selectedCategory.uppercase()
                    }
                }
            )
        }
        "FORM" -> {
            ProgressFormScreen(
                category = selectedCategory,
                dbHelper = dbHelper,
                empId = empId,
                userFcba = fcba,
                initialData = itemToEdit, // KIRIM DATA INI KE FORM
                onBack = {
                    itemToEdit = null // Reset agar saat tambah baru form kosong
                    currentStep = "LIST"
                },
                onSaveSuccess = {
                    itemToEdit = null // Reset setelah sukses
                    currentStep = "LIST"
                }
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

            // Baris 1: PERAWATAN dan PEMBIBITAN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SimpleCategoryButton(
                    title = "PERAWATAN",
                    icon = Icons.Default.Build,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect("PERAWATAN") }
                )

                SimpleCategoryButton(
                    title = "PEMBIBITAN",
                    icon = Icons.Default.Spa,
                    color = Color(0xFFF9A825),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect("PEMBIBITAN") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Baris 2: UMUM (Ukurannya akan sama karena menggunakan weight(1f) dan Spacer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SimpleCategoryButton(
                    title = "UMUM",
                    icon = Icons.Default.Assignment, // Ikon daftar tugas
                    color = Color(0xFF1A3A8F),      // Warna biru identitas app
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect("UMUM") }
                )

                // Spacer ini penting agar tombol UMUM tidak melebar memenuhi layar
                // dan tetap berukuran sama dengan tombol di atasnya.
                Spacer(modifier = Modifier.weight(1f))
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProgressListScreen(
    category: String,
    progressData: List<Map<String, String>>,
    dbHelper: AttendanceDatabaseHelper, // Tambahkan dbHelper
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onRefresh: () -> Unit, // Tambahkan callback untuk refresh data setelah hapus
    onEditClick: (Map<String, String>) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Map<String, String>?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

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
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada data $category tersimpan", color = Color.Gray)
            }
        } else {
            LazyColumn(Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)) {
                items(progressData) { item ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .combinedClickable(
                                onClick = { /* Klik biasa bisa untuk lihat detail langsung */ },
                                onLongClick = {
                                    selectedItem = item
                                    showSheet = true
                                }
                            ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("RKH: ${item["no_rkh"]}", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
                            val lokasi = item["location_code"] ?: item["block_code"] ?: "Lokasi Kosong"
                            Text("Lokasi: $lokasi", fontSize = 14.sp)
                            Text("Waktu: ${item["created_at"]}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // --- BOTTOM SHEET MENU ---
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        "Opsi Data: ${selectedItem?.get("no_rkh")}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    ListItem(
                        headlineContent = { Text("Lihat Detail") },
                        leadingContent = { Icon(Icons.Default.Visibility, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showSheet = false
                            Toast.makeText(context, "Fitur Detail Belum Tersedia", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Edit Header") },
                        leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showSheet = false
                            // Pastikan parameter onEditClick sudah ada di fungsi SpecialFeaturesScreen
                            onEditClick(selectedItem!!)
                        }
                    )
                    Divider()
                    ListItem(
                        headlineContent = { Text("Hapus Data", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        modifier = Modifier.clickable {
                            val id = selectedItem?.get("id")?.toIntOrNull()
                            if (id != null) {
                                // Panggil fungsi hapus di dbHelper Anda
                                 dbHelper.deleteProgress(id)
                                Toast.makeText(context, "Data Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                                onRefresh() // Refresh list
                            }
                            showSheet = false
                        }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressFormScreen(
    category: String,
    userFcba: String,
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    initialData: Map<String, String>? = null,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    // 1. Berikan nama unik agar tidak bentrok dengan parameter fungsi (userFcba)
    // Gunakan parameter userFcba jika ada, jika tidak ada baru ambil dari SharedPref
    val currentFcba = (sharedPref.getString("fcba", userFcba) ?: userFcba).uppercase()

    // --- DATA SOURCES ---
    // 2. Gunakan currentFcba secara konsisten di semua pemanggilan dan remember key
    val rkhList = remember(currentFcba, category) {
        when (category) {
            "UMUM" -> {
                // Jika kategori UMUM, ambil semua No RKH tanpa filter type
                dbHelper.getAllRKHListMap(currentFcba)
            }
            "PERAWATAN" -> {
                // Ambil hanya yang tipenya Perawatan
                dbHelper.getRKHListByTypeMap(currentFcba, "RKH Perawatan (perawatan)")
            }
            "PEMBIBITAN" -> {
                // Ambil hanya yang tipenya Bibitan
                dbHelper.getRKHListByTypeMap(currentFcba, "RKH Bibitan (bibitan)")
            }
            else -> {
                // Fallback default
                dbHelper.getRKHList(currentFcba)
            }
        }
    }
    val supervisorOptions = remember(currentFcba) { dbHelper.getSupervisors(currentFcba) }
    val jobMasterList = remember(currentFcba) { dbHelper.getDropdownData("JOB", currentFcba) }
    val isEditMode = initialData != null
    // 3. Pastikan availableStaff memantau perubahan currentFcba
    val availableStaff = remember(currentFcba) {
        try {
            dbHelper.getEmployeesAlreadyCheckedIn(currentFcba)
        } catch (e: Exception) {
            android.util.Log.e("DB_ERROR", "Gagal load staff: ${e.message}")
            emptyList<Map<String, String>>()
        }
    }

    // --- FORM STATES ---
    var selectedRKH by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedBlock by remember { mutableStateOf("") }
    var selectedJobType by remember { mutableStateOf("") }
    var supervisi1 by remember { mutableStateOf("") }
    var supervisi2 by remember { mutableStateOf("") }
    var supervisi3 by remember { mutableStateOf("") }
    var supervisi4 by remember { mutableStateOf("") }

    var unit by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }

// Setiap kali selectedRKH berubah (saat user memilih dari dialog)
    // --- LOGIKA AUTO-FILL ---
    LaunchedEffect(selectedRKH) {
        selectedRKH?.get("no_rkh")?.let { noRkh ->
            // Ambil detail lengkap dari DB
            val detail = dbHelper.getRKHDetail(noRkh)

            if (detail != null) {
                // 1. PENTING: Update selectedRKH dengan detail lengkap dari DB
                // Ini agar key "location_code" tersedia saat dialog SELECT_LOCATION dibuka
                selectedRKH = detail

                // 2. Isi otomatis field lainnya (kecuali Lokasi/selectedLocationCode)
                selectedJobType = detail["job_code"] ?: ""
                supervisi1 = detail["supervisor1"] ?: ""
                supervisi2 = detail["supervisor2"] ?: ""
                supervisi3 = detail["supervisor3"] ?: ""
                supervisi4 = detail["supervisor4"] ?: ""

                unit = detail["unit"] ?: ""
                output = detail["output"] ?: ""

                // 3. JANGAN isi selectedLocationCode di sini agar form tetap kosong (User pilih manual)
                // selectedLocationCode tetap "" karena sudah di-reset di onSelect RKH

                Toast.makeText(context, "Data RKH Berhasil Dimuat", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var selectedEmployees by remember { mutableStateOf(setOf<String>()) }



    var rate by remember { mutableStateOf("") }
    var lembur by remember { mutableStateOf("0") }
    var dapatBeras by remember { mutableStateOf(false) }
    var keterangan by remember { mutableStateOf("") }

    // State untuk Dialog Pencarian (Shared Component)
    var activeDialog by remember { mutableStateOf<String?>(null) }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    // Logic: Blok berdasarkan lokasi RKH
    var selectedLocationCode by remember { mutableStateOf(initialData?.get("location_code") ?: "") }
    var showSmartCamera by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

// Launcher Kamera
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        if (it != null) bitmap = it
    }

// Launcher Galeri
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            showSmartCamera = false // Tutup kamera setelah pilih galeri
        }
    }

    // --- LOGIKA DIALOG PENCARIAN (Shared Component) ---
    if (activeDialog != null) {
        when (activeDialog) {
            "RKH" -> SearchableMapDialog(
                title = "Cari No RKH",
                options = rkhList,
                displayProvider = { "RKH:${it["no_rkh"]} " },
                onDismiss = { activeDialog = null },
                onSelect = {
                    selectedRKH = it
                    selectedLocationCode = "" // RESET lokasi agar user memilih ulang untuk RKH baru
                    activeDialog = null
                }
            )

            "SELECT_LOCATION" -> {
                // 1. Ambil data dari RKH yang sedang dipilih
                val rkhType = (selectedRKH?.get("type") ?: "").uppercase()

                // 2. Ambil string lokasi (Contoh: "F01, F02, F03")
                val rawLocationString = selectedRKH?.get("location_code") ?: ""

                // 3. Pecah string menjadi List dan bersihkan spasi
                val optionsFromRKH = rawLocationString.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                android.util.Log.d("DEBUG_PROGRESS", "Lokasi di RKH: $optionsFromRKH")

                SearchableListDialog(
                    title = if (rkhType.contains("BIBITAN")) "Pilih Lokasi Nursery (Sesuai RKH)" else "Pilih Blok (Sesuai RKH)",
                    options = optionsFromRKH, // Hanya menampilkan yang ada di RKH
                    onDismiss = { activeDialog = null },
                    onSelect = {
                        selectedLocationCode = it
                        activeDialog = null
                    }
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

            "WORKERS" -> SearchableMapDialog(
                title = "Pilih Karyawan",
                options = availableStaff,
                // Menampilkan ID dan Nama di dalam list
                displayProvider = { "${it["id"]} - ${it["name"]}" },
                onDismiss = { activeDialog = null },
                onSelect = { empMap ->
                    // Karena hanya satu, kita reset Set-nya dan isi dengan satu ID saja
                    selectedEmployees = setOf(empMap["id"] ?: "")
                    activeDialog = null
                }
            )
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

        if (showSmartCamera) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showSmartCamera = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false // Agar fullscreen
                )
            ) {
                SmartCameraView(
                    onImageCaptured = { capturedBitmap ->
                        bitmap = capturedBitmap
                        showSmartCamera = false
                    },
                    onGalleryClick = {
                        galleryLauncher.launch("image/*")
                    },
                    onClose = { showSmartCamera = false }
                )
            }
        }
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
                value = if (selectedRKH != null) "RKH:${selectedRKH?.get("no_rkh")} " else "",
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


            // 4. Supervisi (Diubah menjadi 4 baris)
            Text("Personil Supervisi", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            ClickableSearchField("Supervisi 1 (Wajib)", supervisi1) { activeDialog = "SUP1" }
            ClickableSearchField("Supervisi 2", supervisi2) { activeDialog = "SUP2" }
            ClickableSearchField("Supervisi 3", supervisi3) { activeDialog = "SUP3" }
            ClickableSearchField("Supervisi 4", supervisi4) { activeDialog = "SUP4" }



            // 5. Karyawan
            Text("Pilih Karyawan", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

            ClickableSearchField(
                label = "Klik untuk mencari karyawan",
                value = if (selectedEmployees.isEmpty()) {
                    "Belum ada karyawan dipilih"
                } else {
                    // Mencari nama karyawan berdasarkan ID yang ada di Set
                    val selectedId = selectedEmployees.first()
                    val emp = availableStaff.find { it["id"] == selectedId }
                    emp?.get("name") ?: selectedId
                },
                onClick = { activeDialog = "WORKERS" }
            )

            Text("Terpilih: ${selectedEmployees.size} Karyawan", fontSize = 11.sp, color = Color.Gray)

            Text("Lokasi Kerja", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            ClickableSearchField(
                label = "Pilih Lokasi / Blok",
                value = selectedLocationCode,
                onClick = {
                    if (selectedRKH != null) {
                        activeDialog = "SELECT_LOCATION"
                    } else {
                        Toast.makeText(context, "Pilih No RKH terlebih dahulu", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // 6. Hasil Kerja
            Text("Hasil Kerja", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
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

            // --- INPUT FOTO (PENGGANTI BERAS & LEMBUR) ---
            Text("Dokumentasi Foto", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

            if (bitmap != null) {
                Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }


            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        // Cek apakah izin sudah diberikan
                        if (cameraPermissionState.status.isGranted) {
                            showSmartCamera = true // Panggil kamera kustom
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Kamera")
                }

            }



            Button(
                onClick = {
                    if (isEditMode) {
                        // JALANKAN UPDATE (Edit Header)
                        val success = dbHelper.updateProgressHeader(
                            id = initialData!!["id"]?.toIntOrNull() ?: 0,
                            noRkh = selectedRKH?.get("no_rkh") ?: "",
                            block = selectedBlock,
                            jobType = selectedJobType,
                            sup1 = supervisi1,
                            sup2 = supervisi2,
                            sup3 = supervisi3,
                            sup4 = supervisi4

                        )
                        if (success) {
                            Toast.makeText(context, "Header Berhasil Diperbarui", Toast.LENGTH_SHORT).show()
                            onSaveSuccess()
                        }
                    } else {
                        // JALANKAN SAVE BARU
                        if (selectedRKH == null) {
                            Toast.makeText(context, "Pilih RKH!", Toast.LENGTH_SHORT).show()
                        } else if (supervisi1.isEmpty()) {
                            Toast.makeText(context, "Supervisi 1 wajib!", Toast.LENGTH_SHORT).show()
                        } else if (selectedEmployees.isEmpty()) {
                            Toast.makeText(context, "Pilih karyawan!", Toast.LENGTH_SHORT).show()
                        } else {
                            // 1. Simpan foto ke storage internal dan ambil alamat path-nya
                            val savedPath = if (bitmap != null) {
                                dbHelper.saveImageToFile(context, bitmap!!, "ProgressWork")
                            } else null


                            dbHelper.savePlantationProgress(
                                rkh = selectedRKH!!["no_rkh"] ?: "",
                                category = category,
                                employees = selectedEmployees.toList(),
                                supervisors = listOf(supervisi1, supervisi2, supervisi3, supervisi4),
                                unit = unit.toDoubleOrNull() ?: 0.0,
                                output = output.toDoubleOrNull() ?: 0.0,
                                rate = rate.toDoubleOrNull() ?: 0.0,
                                lembur = lembur.toIntOrNull() ?: 0,
                                beras = if (dapatBeras) 1 else 0,
                                locationCode = selectedLocationCode,
                                location = selectedLocationCode,
                                photoPath = savedPath
                            )
                            onSaveSuccess()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                // Ganti Color.Orange dengan kode HEX di bawah ini
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditMode) Color(0xFFF2994A) else Color(0xFF1A3A8F)
                )
            ) {
                Icon(if (isEditMode) Icons.Default.Edit else Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isEditMode) "PERBARUI HEADER" else "SIMPAN PROGRESS")
            }
        }
    }
}




