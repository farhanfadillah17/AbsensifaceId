package com.example.attendanceapp

import android.util.Log // Tambahkan import ini
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    dbHelper: AttendanceDatabaseHelper,
    sessionManager: SessionManager,
    onLoginSuccess: () -> Unit,
) {
    var fccode by remember { mutableStateOf("") }
    var passwordKtp by remember { mutableStateOf("") } // Ubah nama variabel agar jelas
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("LOGIN ABSENSI", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        // INPUT ID KARYAWAN (Contoh: 02-00172)
        OutlinedTextField(
            value = fccode,
            onValueChange = { fccode = it },
            label = { Text("ID Karyawan ") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.Cyan,
                unfocusedLabelColor = Color.Gray
            )
        )

        Spacer(Modifier.height(16.dp))

        // INPUT NOMOR KTP (Sesuai kolom IDENTITYCARD di Database)
        OutlinedTextField(
            value = passwordKtp,
            onValueChange = { passwordKtp = it },
            label = { Text("Password ") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.Cyan,
                unfocusedLabelColor = Color.Gray
            )
        )

        if (error.isNotEmpty()) {
            Text(error, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                Log.d("LOGIN_ACTION", "Mencoba Login: ID=$fccode, Password=$passwordKtp")

                val user = dbHelper.checkLogin(fccode, passwordKtp)

                if (user != null) {
                    Log.d("LOGIN_ACTION", "Login Berhasil: ${user.name}")
                    sessionManager.saveSession(user.fccode, user.fcba, user.name)
                    onLoginSuccess()
                } else {
                    Log.e("LOGIN_ACTION", "Login Gagal: Data tidak ditemukan di database")
                    error = "ID atau Nomor KTP Salah!"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("MASUK")
        }
    }
}