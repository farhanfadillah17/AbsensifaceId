package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.google.gson.Gson

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.geometry.isEmpty
import kotlin.text.toIntOrNull

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FruitCountingScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    fcba: String,
    onBack: () -> Unit,
    initialData: Map<String, String>? = null,
    onSuccess: () -> Unit
) {
    // State Navigasi Internal
    var isFormVisible by remember { mutableStateOf(false) }
    var selectedDataForNfc by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedItemForEdit by remember { mutableStateOf<Map<String, String>?>(null) }
    // State untuk Menu Tekan Lama
    var selectedItemForMenu by remember { mutableStateOf<Map<String, String>?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Any?>(null) } // Sesuaikan tipe datanya
    // Refresh list data
    var fruitDataList by remember { mutableStateOf(dbHelper.getAllFruitCounting()) }

    // Dialog NFC
    if (selectedDataForNfc != null) {
        NfcTransferDialog(
            data = selectedDataForNfc!!,
            onDismiss = { selectedDataForNfc = null }
        )
    }

    if (isFormVisible) {
        FruitCountingFormContent(
            dbHelper = dbHelper,
            fcba = fcba,
            initialData = selectedItemForEdit, // Pastikan ini dikirim
            onBack = {
                isFormVisible = false
                selectedItemForEdit = null // PENTING: Reset agar form bersih saat buka lagi
            },
            onSaveSuccess = {
                fruitDataList = dbHelper.getAllFruitCounting()
                isFormVisible = false
                selectedItemForEdit = null // PENTING: Reset setelah sukses edit
            }
        )

    } else {
        // TAMPILAN UTAMA: LIST DATA
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("List Perhitungan Buah", color = Color.White) },
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
                    onClick = { isFormVisible = true },
                    containerColor = Color(0xFF1A3A8F),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Data")
                }
            }
        ) { padding ->
            if (fruitDataList.isEmpty()) {
                Box(Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                    Text("Belum ada data. Klik + untuk menambah.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fruitDataList) { data ->
                        FruitCountingItemCard(
                            data = data,
                            onClick = { selectedDataForNfc = data },
                            onLongClick = {
                                selectedItemForMenu = data
                                showMenu = true
                            }
                        )
                    }
                }
            }
        }
    }

    // --- MODAL BOTTOM SHEET MENU (LONG PRESS) ---
    if (showMenu && selectedItemForMenu != null) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Opsi Data TPH: ${selectedItemForMenu!!["tph_code"] ?: "-"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                ListItem(
                    headlineContent = { Text("Lihat Detail") },
                    leadingContent = { Icon(Icons.Default.Visibility, contentDescription = null) },
                    modifier = Modifier.clickable {
                        // Ganti showSheet menjadi showMenu
                        showMenu = false
                        Toast.makeText(context, "Fitur Detail Belum Tersedia", Toast.LENGTH_SHORT).show()
                    }
                )

                ListItem(
                    headlineContent = { Text("Edit Header") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = "Edit Header") },
                    modifier = Modifier.clickable {
                        showMenu = false
                        // 1. Set data yang akan diedit ke parameter navigasi form
                        // Kita butuh state tambahan di Screen utama jika ingin isFormVisible membawa data
                        selectedItemForEdit = selectedItemForMenu // Pastikan Anda membuat state 'itemToEdit' di level Screen
                        isFormVisible = true
                    }
                )



                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

                ListItem(
                    headlineContent = { Text("Hapus Data", color = Color.Red) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = "Hapus Data", tint = Color.Red) },
                    modifier = Modifier.clickable {
                        showMenu = false
                        val idStr = selectedItemForMenu!!["id"]
                        if (idStr != null) {
                            // Panggil fungsi hapus dari dbHelper
                            val deletedRows = dbHelper.deleteFruitCounting(idStr.toIntOrNull() ?: -1)
                            if (deletedRows > 0) {
                                Toast.makeText(context, "Data Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                                fruitDataList = dbHelper.getAllFruitCounting() // Refresh list data otomatis
                            } else {
                                Toast.makeText(context, "Gagal menghapus data", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "ID data tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FruitCountingItemCard(
    data: Map<String, String>,
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
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(data["tanggal"] ?: data["created_at"] ?: "-", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Text("TPH: ${data["tph_code"] ?: "-"}", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Lokasi", fontSize = 12.sp, color = Color.Gray)
                    Text(data["location_code"] ?: "-", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Column(Modifier.weight(1f)) {
                    Text("Output", fontSize = 12.sp, color = Color.Gray)
                    Text(data["output"] ?: "0", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Unit", fontSize = 12.sp, color = Color.Gray)
                    Text(data["unit"] ?: "0", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "Hold untuk Opsi / Klik untuk Transfer NFC",
                fontSize = 10.sp,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun NfcTransferDialog(data: Map<String, String>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val smallData = mapOf(
        "location_code" to (data["location_code"] ?: ""),
        "unit" to (data["unit"] ?: "0")
    )
    val jsonString = Gson().toJson(smallData)

    // Saat dialog muncul, kirim data ke MainActivity agar siap ditulis saat kartu ditempel
    LaunchedEffect(jsonString) {
        activity?.dataToWrite = jsonString
    }

    // Saat dialog ditutup (dismiss), hapus antrian data agar tidak menulis ke kartu lain secara tidak sengaja
    DisposableEffect(Unit) {
        onDispose {
            activity?.dataToWrite = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Nfc, null, tint = Color(0xFF1A3A8F))
                Spacer(Modifier.width(8.dp))
                Text("Siap Menulis NFC")
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Nfc,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF2E7D32) // Warna hijau menandakan siap
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Tempelkan KARTU NFC sekarang untuk menyimpan data TPH: ${data["tph_code"] ?: "-"}",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text("Jangan lepas kartu sampai muncul pesan sukses.", fontSize = 11.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FruitCountingFormContent(
    dbHelper: AttendanceDatabaseHelper,
    fcba: String,
    onBack: () -> Unit,
    initialData: Map<String, String>? = null,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current

    val isEditMode = initialData != null

    // Form States
    // Form States
    var selectedRKH by remember { mutableStateOf<Map<String, String>?>(null) }
    var supervisi1 by remember { mutableStateOf(initialData?.get("supervisor1") ?: "") }
    var supervisi2 by remember { mutableStateOf(initialData?.get("supervisor2") ?: "") }
    var supervisi3 by remember { mutableStateOf(initialData?.get("supervisor3") ?: "") }
    var supervisi4 by remember { mutableStateOf(initialData?.get("supervisor4") ?: "") }

    // PERBAIKAN: Ambil data karyawan dari initialData dan pecah string menjadi Set
    var selectedWorkers by remember {
        mutableStateOf(
            initialData?.get("employees")?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: setOf<String>()
        )
    }

    var unit by remember { mutableStateOf(initialData?.get("unit") ?: "") }
    var output by remember { mutableStateOf(initialData?.get("output") ?: "") }

    // PERBAIKAN: Ambil rate dari initialData
    var rate by remember { mutableStateOf(initialData?.get("rate") ?: "") }

    var selectedTPH by remember(initialData) { mutableStateOf(initialData?.get("tph_code") ?: "") }
//    var lembur by remember { mutableStateOf(initialData?.get("lembur") ?: "0") }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
//    var isBeras by remember { mutableStateOf(initialData?.get("beras") == "1") }
    var activeDialog by remember { mutableStateOf<String?>(null) }

    // --- STATE FOTO ---
    var showSmartCamera by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Launcher Kamera
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        if (it != null) bitmap = it
    }


    // Launcher Galeri (untuk dipanggil dari dalam SmartCameraView)
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

    // Master Data

    val rkhList = remember(fcba) { dbHelper.getRKHPanenList(fcba) }

    LaunchedEffect(rkhList) {
        if (isEditMode && rkhList.isNotEmpty()) {
            // Mencari data RKH yang sesuai dengan data yang sedang diedit
            selectedRKH = rkhList.find { it["no_rkh"] == initialData?.get("no_rkh") }
        }
    }

    LaunchedEffect(selectedRKH) {
        selectedRKH?.let { rkh ->
            if (!isEditMode || activeDialog == "RKH") {
                // Sesuaikan "supervisi1" dengan nama kolom asli di table_rkh Anda
                supervisi1 = rkh["supervisi1"] ?: supervisi1
                supervisi2 = rkh["supervisi2"] ?: supervisi2
                supervisi3 = rkh["supervisi3"] ?: supervisi3
                supervisi4 = rkh["supervisi4"] ?: supervisi4

                // Sesuaikan "unit" dan "output" dengan kolom di table_rkh
                unit = rkh["unit"] ?: unit
                output = rkh["output"] ?: output

                if (activeDialog == "RKH") {
                    Toast.makeText(context, "Data RKH Panen Dimuat", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var selectedLocationCode by remember { mutableStateOf(initialData?.get("location_code") ?: "") }
    val supervisorOptions = remember { dbHelper.getSupervisors(fcba) }
    val presentWorkers = remember { dbHelper.getEmployeesAlreadyCheckedIn(fcba) }
    val locationCodeFromRKH = selectedRKH?.get("location_code") ?: ""
    val tphList = remember(selectedLocationCode) {
        if (selectedLocationCode.isNotEmpty()) {
            dbHelper.getTPHByLocation(selectedLocationCode, fcba)
        } else {
            emptyList()
        }
    }

    // Dialog Pencarian Logic
    if (activeDialog != null) {
        val availableStaff = presentWorkers
        when (activeDialog) {
            "RKH" -> SearchableMapDialog(
                title = "Cari No RKH",
                options = rkhList,
                displayProvider = { "RKH:${it["no_rkh"]} " },
                onDismiss = { activeDialog = null },
                onSelect = { selectedRKH = it; selectedTPH = ""; activeDialog = null }
            )

            "SELECT_LOCATION" -> {
                // Pecah string "A01, A02" menjadi list [A01, A02]
                val locations = selectedRKH?.get("location_code")?.split(", ")?.filter { it.isNotEmpty() } ?: emptyList()
                SearchableListDialog(
                    title = "Pilih Blok/Lokasi",
                    options = locations,
                    onDismiss = { activeDialog = null },
                    onSelect = {
                        selectedLocationCode = it
                        selectedTPH = "" // Reset TPH jika lokasi ganti
                        activeDialog = null
                    }
                )
            }

            "TPH" -> SearchableListDialog(
                title = "Cari TPH", options = tphList,
                onDismiss = { activeDialog = null },
                onSelect = { selectedTPH = it; activeDialog = null }
            )
            "SUP1", "SUP2", "SUP3", "SUP4" -> SearchableListDialog(
                title = "Cari Personil", options = supervisorOptions,
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
            "WORKERS" -> SearchableMapDialog(
                title = "Pilih Karyawan",
                options = availableStaff,
                // Menampilkan ID dan Nama di dalam list
                displayProvider = { "${it["id"]} - ${it["name"]}" },
                onDismiss = { activeDialog = null },
                onSelect = { empMap ->
                    // Ganti selectedEmployees menjadi selectedWorkers
                    selectedWorkers = setOf(empMap["id"] ?: "")
                    activeDialog = null
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Form Perhitungan Buah", color = Color.White) },
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
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Pilih No RKH
            Text("Referensi RKH", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            ClickableSearchField(
                label = "Pilih No. RKH",
                value = if (selectedRKH != null) "RKH:${selectedRKH?.get("no_rkh")} " else "",
                onClick = { activeDialog = "RKH" }
            )

            // 2. Gang Code (Otomatis & Read Only)
            OutlinedTextField(
                value = selectedRKH?.get("gangcode") ?: selectedRKH?.get("gang_code") ?: "",
                onValueChange = {},
                label = { Text("Gang Code") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledBorderColor = Color.Gray,
                    disabledLabelColor = Color.DarkGray,
                    disabledContainerColor = Color(0xFFF5F5F5)
                )
            )

            Text("Personil Supervisi", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

            // Supervisi Rows
            ClickableSearchField("Supervisi 1 (Wajib)", supervisi1) { activeDialog = "SUP1" }
            ClickableSearchField("Supervisi 2", supervisi2) { activeDialog = "SUP2" }
            ClickableSearchField("Supervisi 3", supervisi3) { activeDialog = "SUP3" }
            ClickableSearchField("Supervisi 4", supervisi4) { activeDialog = "SUP4" }



            // Ganti Card Checklist lama dengan ini:
            Text("Pilih Karyawan", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

            ClickableSearchField(
                label = "Klik untuk mencari karyawan",
                value = if (selectedWorkers.isEmpty()) { // Gunakan selectedWorkers
                    "Belum ada karyawan dipilih"
                } else {
                    val selectedId = selectedWorkers.first() // Gunakan selectedWorkers
                    val emp = presentWorkers.find { it["id"] == selectedId }
                    emp?.get("name") ?: selectedId
                },
                onClick = { activeDialog = "WORKERS" }
            )

            // Ganti OutlinedTextField lama dengan ClickableSearchField
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

            ClickableSearchField("Pilih TPH", selectedTPH) { activeDialog = "TPH" }

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
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)) {
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
                            showSmartCamera = true // Memicu Dialog SmartCameraView muncul
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


            Spacer(Modifier.height(8.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                onClick = {
                    if (isEditMode) {
                        // JALANKAN UPDATE HEADER
                        val success = dbHelper.updateFruitHeader(
                            id = initialData!!["id"]?.toIntOrNull() ?: 0,
                            noRkh = selectedRKH?.get("no_rkh") ?: "",
                            tphCode = selectedTPH,
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
                        // LOGIKA SIMPAN BARU (Lama)
                        if (selectedRKH == null || supervisi1.isEmpty() || selectedWorkers.isEmpty() || selectedTPH.isEmpty()) {
                            Toast.makeText(context, "Lengkapi data wajib!", Toast.LENGTH_SHORT).show()
                        } else {
                            val savedPhotoPath = if (bitmap != null) {
                                dbHelper.saveImageToFile(context, bitmap!!, "FruitCounting")
                            } else null
                            // Isi parameter sesuai dengan variabel yang ada di State Form Anda
                            dbHelper.saveFruitCalculation(
                                fcba = fcba,
                                rkh = selectedRKH!!["no_rkh"] ?: "",
                                gang = selectedRKH!!["gang_code"] ?: "",
                                supervisors = listOf(supervisi1, supervisi2, supervisi3, supervisi4),
                                employees = selectedWorkers.toList(),
                                location = selectedRKH!!["location_code"] ?: "",
                                tph = selectedTPH,
                                unit = unit.toDoubleOrNull() ?: 0.0,
                                output = output.toDoubleOrNull() ?: 0.0,
                                rate = rate.toDoubleOrNull() ?: 0.0, // Tambahkan rate jika ada field-nya
                                beras =  0,
                                lembur =  0.0,
                                photoPath = savedPhotoPath
                            )
                            Toast.makeText(context, "Data Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                            onSaveSuccess()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditMode) Color(0xFFF2994A) else Color(0xFF2E7D32)
                )
            ) {
                Icon(if (isEditMode) Icons.Default.Edit else Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isEditMode) "PERBARUI HEADER" else "SIMPAN DATA")
            }
        }
    }
}