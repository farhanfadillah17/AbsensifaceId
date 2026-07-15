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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    dbHelper: AttendanceDatabaseHelper,
    sessionManager: SessionManager,
    apiClient: ApiClient,
    onLoginSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // State untuk input
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // --- STATE BARU UNTUK IMPORT DATA ---
    var isImporting by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf("") }

    val listPinalty = mutableListOf<String>("Pinalty 1", "Pinalty 2", "Pinalty 3")

    listPinalty.forEachIndexed { index, string ->

        Log.d("list string", "p"+(index+1))

    }

    // Logic untuk impor data otomatis saat aplikasi dibuka
    LaunchedEffect(Unit) {

        isImporting = true

        try {

            var existingData = withContext(Dispatchers.IO) {
                dbHelper.getAllUser().size
            }

            if (existingData == 0) {

                importStatus = "Menyiapkan data user..."

                withContext(Dispatchers.IO) {

                    val response = apiClient.getUser()

                    dbHelper.insertUsers(response) { count ->

                        scope.launch(Dispatchers.Main) {
                            importStatus = "Mengimpor User $count%"
                        }

                    }

                }

                importStatus = "Data user siap!"
            }

            existingData = withContext(Dispatchers.IO) {
                dbHelper.getAllAccess().size
            }

            if (existingData == 0) {

                importStatus = "Menyiapkan data akses..."

                withContext(Dispatchers.IO) {

                    val response = apiClient.getAccess()

                    dbHelper.insertAccess(response) { count ->

                        scope.launch(Dispatchers.Main) {
                            importStatus = "Mengimpor akses $count%"
                        }

                    }

                }

                importStatus = "Data akses siap!"
            }

            importStatus = "Sinkronisasi selesai"

        } catch (e: Exception) {

            Log.e(
                "IMPORT_DATA",
                "Error: ${e.message}",
                e
            )

            importStatus =
                "Gagal mengunduh data: ${e.localizedMessage}"

        } finally {

            isImporting = false

        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- TAMPILAN STATUS IMPORT (Hanya muncul jika sedang proses) ---
        if (isImporting || importStatus.contains("siap")) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.Cyan
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = importStatus,
                        color = if (isImporting) Color.Gray else Color.Green,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Text(
            text = "SIPDRO",
            color = Color(0xFF1A3A8F),
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
                username = it
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
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = Color(0xFF1A3A8F),
                unfocusedLabelColor = Color.Gray,
                focusedBorderColor = Color(0xFF1A3A8F),
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
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedLabelColor = Color(0xFF1A3A8F),
                unfocusedLabelColor = Color.Gray,
                focusedBorderColor = Color(0xFF1A3A8F),
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
                val inputUser = username.trim().lowercase()
                val inputPass = password.trim()

                if (inputUser.isEmpty() || inputPass.isEmpty()) {
                    error = "Username dan password wajib diisi!"
                    return@Button
                }

                isLoading = true
                focusManager.clearFocus()
                Log.d("LOGIN_UI", "Mencoba Login -> User: '$inputUser'")

                try {
                    val userProfile = dbHelper.checkLogin(inputUser, inputPass)

                    if (userProfile != null) {
                        Log.d("LOGIN_UI", "SUCCESS: Berhasil sebagai ${userProfile.username}")
                        sessionManager.saveSession(
                            fccode = userProfile.empcode,
                            fcba = userProfile.fcba,
                            name = userProfile.username,
                            role = userProfile.role
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
                containerColor = Color(0xFF1A3A8F),
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
            text = "App Version 1.0.45",
            color = Color.DarkGray,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}