package com.example.attendanceapp

import android.widget.Toast
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHFormScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    fcba: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current

    // State untuk Form
    var selectedAfd by remember { mutableStateOf("") }
    var selectedGang by remember { mutableStateOf("") }
    var selectedJob by remember { mutableStateOf("") }
    var selectedLoc by remember { mutableStateOf("") }
    var hk by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }

    // State untuk Dialog Pencarian
    var activeDialog by remember { mutableStateOf<String?>(null) }

    // Master Data Load
    val afdelingList = remember(fcba) { dbHelper.getAfdelingList(fcba) }

    val gangList = remember(fcba, selectedAfd) {
        if (selectedAfd.isNotEmpty()) {
            dbHelper.getGangsByAfdeling(selectedAfd, fcba)
        } else {
            emptyList<String>()
        }
    }

    val jobList = remember(fcba) { dbHelper.getJobList(fcba) }
    val locationList = remember(fcba) { dbHelper.getBlockList(fcba) }

    // --- LOGIKA DIALOG PENCARIAN ---
    if (activeDialog != null) {
        val dialogTitle = when (activeDialog) {
            "AFD" -> "Cari Afdeling"
            "GANG" -> "Cari Gang Code"
            "JOB" -> "Cari Job Code"
            "LOC" -> "Cari Location Code"
            else -> ""
        }

        val currentOptions: List<String> = when (activeDialog) {
            "AFD" -> afdelingList
            "GANG" -> gangList
            "JOB" -> jobList
            "LOC" -> locationList
            else -> emptyList<String>()
        }

        // PERBAIKAN: Gunakan Trailing Lambda (onSelect di luar kurung)
        // dan tentukan tipe data secara eksplisit (: String)
        SearchableListDialog(
            title = dialogTitle,
            options = currentOptions,
            onDismiss = { activeDialog = null }
        ) { selectedValue: String ->
            when (activeDialog) {
                "AFD" -> {
                    selectedAfd = selectedValue
                    selectedGang = ""
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
                title = { Text("Form RKH", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. FCBA (Read Only)
            OutlinedTextField(
                value = fcba,
                onValueChange = {},
                label = { Text("FCBA") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledContainerColor = Color(0xFFF0F0F0)
                )
            )

            // 2. Searchable Fields (Menggunakan fungsi dari SharedComponents.kt)
            ClickableSearchField(label = "AFDELING", value = selectedAfd) { activeDialog = "AFD" }

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

            ClickableSearchField(label = "JOB CODE", value = selectedJob) { activeDialog = "JOB" }
            ClickableSearchField(label = "LOCATION CODE", value = selectedLoc) { activeDialog = "LOC" }

            // 3. Numeric Inputs
            OutlinedTextField(
                value = hk,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) hk = it },
                label = { Text("JUMLAH HK") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedAfd.isEmpty() || selectedGang.isEmpty() || hk.isEmpty()) {
                        Toast.makeText(context, "Mohon lengkapi Afdeling, Gang, dan HK!", Toast.LENGTH_SHORT).show()
                    } else {
                        val result = dbHelper.insertRKH(
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
                            Toast.makeText(context, "RKH Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SIMPAN RKH", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}