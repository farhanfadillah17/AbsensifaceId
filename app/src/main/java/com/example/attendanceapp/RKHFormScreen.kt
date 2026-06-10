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
    empId: String, // Menggunakan fccode untuk logging jika perlu
    fcba: String,  // Diambil otomatis dari session
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

    // State untuk Dropdown (Ambil dari DB)
    // State untuk Dropdown (Gunakan fungsi spesifik yang sudah kita perbaiki)
    val afdelingList = remember(fcba) { dbHelper.getAfdelingList(fcba) }
    val gangList = remember(fcba) { dbHelper.getGangList(fcba) }
    val jobList = remember(fcba) { dbHelper.getJobList(fcba) }
    val locationList = remember(fcba) { dbHelper.getBlockList(fcba) }


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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                enabled = false
            )

            // Dropdowns
            RKHDropdown("AFDELING", selectedAfd, afdelingList) { selectedAfd = it }
            RKHDropdown("GANG CODE", selectedGang, gangList) { selectedGang = it }
            RKHDropdown("JOB CODE", selectedJob, jobList) { selectedJob = it }
            RKHDropdown("LOCATION CODE", selectedLoc, locationList) { selectedLoc = it }

            // Numeric Inputs
            OutlinedTextField(
                value = hk,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) hk = it },
                label = { Text("JUMLAH HK") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            OutlinedTextField(
                value = unit,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) unit = it },
                label = { Text("UNIT") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            OutlinedTextField(
                value = output,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) output = it },
                label = { Text("OUTPUT") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RKHDropdown(label: String, selectedValue: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Data Kosong") },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onSelected(selectionOption)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

