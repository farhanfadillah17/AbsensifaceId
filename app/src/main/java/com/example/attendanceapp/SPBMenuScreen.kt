package com.example.attendanceapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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

@Composable
fun SPBFormScreen(
    dbHelper: AttendanceDatabaseHelper,
    fcba: String,
    empId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit

) {
    val context = LocalContext.current

    // --- STATE HEADER ---
    var spbNo by remember { mutableStateOf("") }
    var selectedMill by remember { mutableStateOf("") }
    var sopirName by remember { mutableStateOf("") }
    var selectedVehicle by remember { mutableStateOf("") }
    var pemuat1 by remember { mutableStateOf("") }
    var pemuat2 by remember { mutableStateOf("") }

    // --- STATE DETAIL (Satu Baris dlu) ---
    var locCode by remember { mutableStateOf("") } // Ini nanti diisi dari NFC
    var unit by remember { mutableStateOf("") }
    var tph by remember { mutableStateOf("") }
    var empCode by remember { mutableStateOf("") }

    // Generate No SPB saat buka
    LaunchedEffect(Unit) {
        spbNo = dbHelper.generateNoSPB(fcba)
    }

    Scaffold(
        topBar = { /* Sama seperti RKH */ }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())) {

            // CARD HEADER
            Card(elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("HEADER SPB", fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))

                    OutlinedTextField(value = fcba, onValueChange = {}, label = { Text("FCBA") }, readOnly = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = spbNo, onValueChange = {}, label = { Text("NO SPB") }, readOnly = true, modifier = Modifier.fillMaxWidth())

                    // Master Data Dropdowns (Gunakan SearchableListDialog Anda)
                    ClickableSearchField(label = "MILL / PABRIK", value = selectedMill) { /* Open Dialog Mill */ }

                    OutlinedTextField(value = sopirName, onValueChange = { sopirName = it }, label = { Text("NAMA SOPIR") }, modifier = Modifier.fillMaxWidth())

                    ClickableSearchField(label = "KODE KENDARAAN", value = selectedVehicle) { /* Open Dialog Vehicle */ }

                    Text("PEMUAT", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pemuat1, onValueChange = { pemuat1 = it }, label = { Text("Pemuat 1") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pemuat2, onValueChange = { pemuat2 = it }, label = { Text("Pemuat 2") }, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // CARD DETAIL (BACA NFC)
            Card(border = BorderStroke(1.dp, Color.Gray)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DETAIL BUAH (SCAN NFC)", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))

                    Button(onClick = {
                        // Trigger NFC Scanner Disini
                        // locCode = hasilScanNfc
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("SCAN LOKASI (NFC)")
                    }

                    OutlinedTextField(value = locCode, onValueChange = { locCode = it }, label = { Text("LOCATION CODE") }, readOnly = true, modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = tph, onValueChange = { tph = it }, label = { Text("TPH") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("UNIT / JANJANG") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }

                    OutlinedTextField(value = empCode, onValueChange = { empCode = it }, label = { Text("EMPLOYEE CODE (PEMILIK)") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    // Logika Simpan ke T_SPB_HEADER dan T_SPB_DETAIL
                    Toast.makeText(context, "SPB $spbNo Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                Text("SIMPAN SPB")
            }
        }
    }
}