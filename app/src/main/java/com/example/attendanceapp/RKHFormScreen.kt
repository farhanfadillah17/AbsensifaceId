package com.example.attendanceapp

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHFormScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    fcba: String, // Pastikan ini "SRE"
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // --- STATE HEADER ---
    var noRkh by remember { mutableStateOf("Generating...") }
    var rkhDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var selectedAfd by remember { mutableStateOf("") }
    var selectedGang by remember { mutableStateOf("") }

    // --- STATE DETAIL ---
    var selectedJob by remember { mutableStateOf("") }
    var selectedLoc by remember { mutableStateOf("") }
    var hk by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }

    // State untuk Dialog Pencarian
    var activeDialog by remember { mutableStateOf<String?>(null) }

    // --- LOGIC OTOMATIS NOMOR RKH (SEPERTI SPB) ---
    LaunchedEffect(fcba) {
        // Memanggil fungsi generator nomor dari DatabaseHelper
        noRkh = dbHelper.generateNoRKH(fcba)
    }

    // Load Data Master
    val afdelingList = remember(fcba) { dbHelper.getAfdelingList(fcba) }

    // List Gang akan berubah otomatis jika Afdeling dipilih
    val gangList = remember(fcba, selectedAfd) {
        if (selectedAfd.isNotEmpty()) {
            dbHelper.getGangsByAfdeling(selectedAfd, fcba)
        } else emptyList()
    }

    val jobList = remember(fcba) { dbHelper.getJobList(fcba) }
    val locationList = remember(fcba) { dbHelper.getBlockList(fcba) }

    // Dialog Pencarian
    if (activeDialog != null) {
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
                "AFD" -> {
                    selectedAfd = selectedValue
                    selectedGang = "" // Reset gang jika afdeling berubah
                }
                "GANG" -> selectedGang = selectedValue
                "JOB" -> selectedJob = selectedValue
                "LOC" -> selectedLoc = selectedValue
            }
            activeDialog = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input RKH ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
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
            // --- SECTION 1: HEADER (NOMOR & TANGGAL) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PENGESAHAN RKH", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A3A8F))

                    OutlinedTextField(
                        value = fcba, // Diambil dari parameter fungsi RKHFormScreen
                        onValueChange = { },
                        label = { Text("BUSINESS UNIT (FCBA)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true, // Tidak bisa diubah manual
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE9ECEF),
                            unfocusedContainerColor = Color(0xFFE9ECEF),
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = Color.DarkGray
                        )
                    )



                    OutlinedTextField(
                        value = noRkh,
                        onValueChange = { }, // Disable edit manual
                        label = { Text("NO RKH (OTOMATIS)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE9ECEF),
                            unfocusedContainerColor = Color(0xFFE9ECEF)
                        )
                    )

                    OutlinedTextField(
                        value = rkhDate,
                        onValueChange = { rkhDate = it },
                        label = { Text("TANGGAL RKH") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )

                    ClickableSearchField(label = "AFDELING", value = selectedAfd) {
                        activeDialog = "AFD"
                    }

                    ClickableSearchField(
                        label = "GANG CODE",
                        value = selectedGang,
                        enabled = selectedAfd.isNotEmpty()
                    ) {
                        if (selectedAfd.isEmpty()) {
                            Toast.makeText(context, "Pilih Afdeling terlebih dahulu!", Toast.LENGTH_SHORT).show()
                        } else {
                            activeDialog = "GANG"
                        }
                    }
                }
            }

            // --- SECTION 2: DETAIL PEKERJAAN ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DETAIL PEKERJAAN", fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))

                    ClickableSearchField(label = "JOB CODE / KEGIATAN", value = selectedJob) {
                        activeDialog = "JOB"
                    }

                    ClickableSearchField(label = "LOCATION / BLOK", value = selectedLoc) {
                        activeDialog = "LOC"
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hk,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) hk = it },
                            label = { Text("HK") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) unit = it },
                            label = { Text("UNIT") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = output,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) output = it },
                            label = { Text("OUTPUT") },
                            modifier = Modifier.weight(1.2f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TOMBOL SIMPAN ---
            Button(
                onClick = {
                    if (selectedGang.isEmpty() || selectedJob.isEmpty() || hk.isEmpty()) {
                        Toast.makeText(context, "Semua data wajib diisi!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Memastikan nomor terbaru digenerate ulang saat simpan untuk cegah duplikat
                        val finalNoRkh = dbHelper.generateNoRKH(fcba)

                        val result = dbHelper.insertRKHFull(
                            noRkh = finalNoRkh,
                            tanggal = rkhDate,
                            fcba = fcba,
                            afd = selectedAfd,
                            gang = selectedGang,
                            job = selectedJob,
                            loc = selectedLoc,
                            hk = hk.toDoubleOrNull() ?: 0.0,
                            unit = unit.toDoubleOrNull() ?: 0.0,
                            out = output.toDoubleOrNull() ?: 0.0
                        )

                        if (result != -1L) {
                            Toast.makeText(context, "RKH $finalNoRkh Berhasil Tersimpan", Toast.LENGTH_LONG).show()
                            onSuccess()
                        } else {
                            Toast.makeText(context, "Gagal Simpan ke Database", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SIMPAN RKH", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}