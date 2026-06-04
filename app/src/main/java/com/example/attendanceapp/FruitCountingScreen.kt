package com.example.attendanceapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FruitCountingScreen(
    dbHelper: AttendanceDatabaseHelper,
    empId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var qty by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perhitungan Buah", color = Color.White, fontWeight = FontWeight.Bold) },
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
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text("Informasi Hasil Panen", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(20.dp))

            // Input Lokasi / Blok
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Lokasi / Blok") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contoh: Blok A12") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input Jumlah Buah (Numeric)
            OutlinedTextField(
                value = qty,
                onValueChange = { if (it.all { char -> char.isDigit() }) qty = it },
                label = { Text("Jumlah Buah (Janjang)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("0") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val count = qty.toIntOrNull() ?: 0
                    if (count > 0 && location.isNotBlank()) {
                        val success = dbHelper.saveFruitCounting(empId, count, location)
                        if (success) {
                            onSuccess() // Kembali ke layar sebelumnya jika berhasil
                        } else {
                            // Tampilkan pesan error jika gagal (opsional)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F)),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("SIMPAN DATA PANEN", fontWeight = FontWeight.Bold)
            }
        }
    }
}