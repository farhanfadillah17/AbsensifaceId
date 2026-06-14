package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SPBFormScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    val fcbaUser = (sharedPref.getString("fcba", "SRE") ?: "SRE").uppercase()

    // --- STATES ---
    var selectedRKH by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedLocation by remember { mutableStateOf("") }
    var selectedMill by remember { mutableStateOf("") }

    var vehicleNo by remember { mutableStateOf("") }
    var selectedTPH by remember { mutableStateOf("") }
    var totalJanjang by remember { mutableStateOf("") }
    var spbNo by remember { mutableStateOf("SPB-${System.currentTimeMillis() / 10000}") }
    val tanggal = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

    // State untuk Dialog (Mencegah Lag)
    var activeDialog by remember { mutableStateOf<String?>(null) }

    // --- DATA SOURCES ---
    val rkhOptions = remember { dbHelper.getRKHList() }
    val locationOptions = remember { dbHelper.getAllFieldCodes() }
    // driverOptions dihapus
    val millOptions = remember { dbHelper.getBusinessUnits().ifEmpty { listOf("PKS 1", "PKS 2") } }
    val tphOptions = remember(selectedLocation) {
        if (selectedLocation.isNotEmpty()) dbHelper.getTPHByLocation(selectedLocation) else emptyList()
    }

    // --- LOGIKA DIALOG PENCARIAN ---
    if (activeDialog != null) {
        when (activeDialog) {
            "RKH" -> SearchableMapDialog(
                title = "Pilih No RKH",
                options = rkhOptions,
                displayProvider = { "RKH:${it["no_rkh"]} - ${it["location"]}" },
                onDismiss = { activeDialog = null },
                onSelect = {
                    selectedRKH = it
                    selectedLocation = it["location"] ?: ""
                    selectedTPH = ""
                    activeDialog = null
                }
            )
            "LOC" -> SearchableListDialog(
                title = "Pilih Lokasi/Blok",
                options = locationOptions,
                onDismiss = { activeDialog = null },
                onSelect = { selectedLocation = it; selectedTPH = ""; activeDialog = null }
            )
            "MILL" -> SearchableListDialog(
                title = "Pilih Pabrik",
                options = millOptions,
                onDismiss = { activeDialog = null },
                onSelect = { selectedMill = it; activeDialog = null }
            )

            "TPH" -> SearchableListDialog(
                title = "Pilih TPH",
                options = tphOptions,
                onDismiss = { activeDialog = null },
                onSelect = { selectedTPH = it; activeDialog = null }
            )
            // Dialog DRIVER dihapus
        }
    }

    // Styling Read Only
    val readOnlyColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = Color.Black,
        disabledContainerColor = Color(0xFFF5F5F5),
        disabledBorderColor = Color.Gray
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FORM SPB (PENGANGKUTAN)", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Info
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = spbNo, onValueChange = {}, label = { Text("No. SPB") }, modifier = Modifier.weight(1.2f), enabled = false, colors = readOnlyColors)
                OutlinedTextField(value = tanggal, onValueChange = {}, label = { Text("Tanggal") }, modifier = Modifier.weight(0.8f), enabled = false, colors = readOnlyColors)
            }

            OutlinedTextField(value = fcbaUser, onValueChange = {}, label = { Text("FCBA") }, modifier = Modifier.fillMaxWidth(), enabled = false, colors = readOnlyColors)

            Divider()

            Text("Referensi & Lokasi", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

            ClickableSearchField(
                label = "Pilih No RKH",
                value = if (selectedRKH != null) "RKH:${selectedRKH?.get("no_rkh")} - ${selectedRKH?.get("location")}" else "",
                onClick = { activeDialog = "RKH" }
            )

            ClickableSearchField(
                label = "Lokasi / Blok (Data FIELD)",
                value = selectedLocation,
                onClick = { activeDialog = "LOC" }
            )

            OutlinedTextField(value = empId, onValueChange = {}, label = { Text("Kerani Kirim") }, modifier = Modifier.fillMaxWidth(), enabled = false, colors = readOnlyColors)

            Divider()

            Text("Detail Pengiriman", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

            ClickableSearchField("Pilih Pabrik (Mill)", selectedMill) { activeDialog = "MILL" }
            // Input Sopir dihapus

            OutlinedTextField(
                value = vehicleNo,
                onValueChange = { vehicleNo = it },
                label = { Text("Nomor Plat Kendaraan") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            Text("Detail Muatan", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

            ClickableSearchField("Pilih TPH", selectedTPH) { activeDialog = "TPH" }

            OutlinedTextField(
                value = totalJanjang,
                onValueChange = { if (it.all { c -> c.isDigit() }) totalJanjang = it },
                label = { Text("Jumlah Janjang") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // driverName.isEmpty() dihapus dari validasi
                    if (selectedLocation.isEmpty() || totalJanjang.isEmpty() || selectedMill.isEmpty()) {
                        Toast.makeText(context, "Lengkapi semua data!", Toast.LENGTH_SHORT).show()
                    } else {
                        val res = dbHelper.saveSPB(
                            spbNo = spbNo,
                            fcba = fcbaUser,
                            rkh = selectedRKH?.get("no_rkh") ?: "",
                            location = selectedLocation,
                            mill = selectedMill,
                            // driver parameter dihapus
                            vehicle = vehicleNo,
                            tph = selectedTPH,
                            janjang = totalJanjang.toIntOrNull() ?: 0
                        )
                        if (res != -1L) {
                            Toast.makeText(context, "SPB Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } else {
                            Toast.makeText(context, "Gagal Simpan ke Database!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SIMPAN & CETAK SPB", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { Toast.makeText(context, "Membaca data NFC...", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Nfc, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AMBIL DATA DARI NFC BUAH")
            }
        }
    }
}