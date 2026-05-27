package com.example.attendanceapp

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    dbHelper: AttendanceDatabaseHelper,
    sessionManager: SessionManager,
    onLoginSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState() // Tambah scroll agar tidak terpotong keyboard

    // State untuk input
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Tema Dark
            .verticalScroll(scrollState) // Biar bisa scroll kalau layar kecil
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ATTENDANCE SYSTEM",
            color = Color.Cyan, // Beri sedikit warna biar menarik
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Silahkan masuk ke akun Anda",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(48.dp))

        // INPUT USERNAME
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it // Jangan lowercase di sini agar user tidak bingung saat mengetik
                error = ""
            },
            label = { Text("Username") },
            placeholder = { Text("Masukkan username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            isError = error.isNotEmpty(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.Cyan,
                unfocusedLabelColor = Color.Gray,
                focusedBorderColor = Color.Cyan,
                errorBorderColor = Color.Red
            )
        )

        Spacer(Modifier.height(16.dp))

        // INPUT PASSWORD
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = ""
            },
            label = { Text("Password") },
            placeholder = { Text("Masukkan password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            isError = error.isNotEmpty(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.Cyan,
                unfocusedLabelColor = Color.Gray,
                focusedBorderColor = Color.Cyan,
                errorBorderColor = Color.Red
            )
        )

        // Pesan Error
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = Color(0xFFFF5252),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(40.dp))

        // TOMBOL LOGIN
        Button(
            onClick = {
                val inputUser = username.trim().lowercase() // Kita lowercase di sini saat submit
                val inputPass = password.trim()

                if (inputUser.isEmpty() || inputPass.isEmpty()) {
                    error = "Username dan password wajib diisi!"
                    return@Button
                }

                isLoading = true
                focusManager.clearFocus() // Tutup keyboard otomatis
                Log.d("LOGIN_UI", "Mencoba Login -> User: '$inputUser'")

                try {
                    // Panggil Database Helper
                    val userProfile = dbHelper.checkLogin(inputUser, inputPass)

                    if (userProfile != null) {
                        Log.d("LOGIN_UI", "SUCCESS: Berhasil sebagai ${userProfile.username}")

                        // Pastikan saat panggil saveSession, tambahkan role dari hasil query database
                        sessionManager.saveSession(
                            fccode = userProfile.empcode,
                            fcba = userProfile.fcba,
                            name = userProfile.username,
                            role = userProfile.role // Ambil role dari objek userProfile
                        )

                        Toast.makeText(context, "Selamat Datang, ${userProfile.username}", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } else {
                        Log.e("LOGIN_UI", "FAILED: Kredensial tidak cocok")
                        error = "Username atau Password salah!"
                    }
                } catch (e: Exception) {
                    Log.e("LOGIN_UI", "CRASH: ${e.localizedMessage}")
                    error = "Terjadi masalah sistem. Silahkan coba lagi."
                } finally {
                    isLoading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00ACC1),
                disabledContainerColor = Color(0xFF005662)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("MASUK", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = {
                Toast.makeText(context, "Hubungi IT Admin untuk bantuan", Toast.LENGTH_LONG).show()
            },
            enabled = !isLoading
        ) {
            Text("Lupa Password?", color = Color.Gray)
        }

        Text(
            text = "App Version 1.0.44", // Update versi biar gampang cek saat build
            color = Color.DarkGray,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}