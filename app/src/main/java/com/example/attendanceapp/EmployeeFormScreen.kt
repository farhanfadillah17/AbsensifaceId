package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeFormScreen(
    dbHelper: AttendanceDatabaseHelper,
    onBack: () -> Unit,
    onNext: (Employee) -> Unit
) {
    // State input
    var empId      by remember { mutableStateOf("") }
    var name       by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var position   by remember { mutableStateOf("") }
    var phone      by remember { mutableStateOf("") }

    // State error
    var idError    by remember { mutableStateOf("") }
    var nameError  by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrasi Karyawan (1/2)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A237E),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp),
        ) {
            Text(
                "Langkah 1: Identitas",
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Lengkapi data diri",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Pastikan ID Karyawan unik dan benar.",
                color = Color(0xFFB0BEC5),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // ID Karyawan
            FormField(
                value = empId,
                onValueChange = {
                    empId = it
                    if (idError.isNotEmpty()) idError = ""
                },
                label = "ID Karyawan *",
                placeholder = "Contoh: EMP001",
                error = idError,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(16.dp))

            // Nama
            FormField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError.isNotEmpty()) nameError = ""
                },
                label = "Nama Lengkap *",
                placeholder = "Nama sesuai KTP",
                error = nameError,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(16.dp))

            // Departemen
            FormField(
                value = department,
                onValueChange = { department = it },
                label = "Departemen",
                placeholder = "Contoh: IT, HR, Produksi",
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(16.dp))

            // Jabatan
            FormField(
                value = position,
                onValueChange = { position = it },
                label = "Jabatan",
                placeholder = "Contoh: Staff, Supervisor",
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(16.dp))

            // No HP
            FormField(
                value = phone,
                onValueChange = { phone = it },
                label = "No. Telepon",
                placeholder = "Contoh: 0812...",
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() }
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    val trimmedId = empId.trim()
                    val trimmedName = name.trim()

                    // Validasi
                    var isValid = true

                    if (trimmedId.isEmpty()) {
                        idError = "ID tidak boleh kosong"
                        isValid = false
                    } else if (dbHelper.employeeExists(trimmedId)) {
                        idError = "ID ini sudah terdaftar"
                        isValid = false
                    }

                    if (trimmedName.isEmpty()) {
                        nameError = "Nama tidak boleh kosong"
                        isValid = false
                    }

                    if (isValid) {
                        val emp = Employee(
                            id = trimmedId,
                            name = trimmedName,
                            department = department.trim(),
                            position = position.trim(),
                            phone = phone.trim()
                        )

                        // CATATAN: insertEmployee dipanggil di sini.
                        // Jika user batal di tengah jalan saat scan wajah,
                        // data karyawan akan tetap ada di DB tanpa data wajah.
                        dbHelper.insertEmployee(emp)
                        onNext(emp)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text("LANJUT KE SCAN WAJAH", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {}
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error.isNotEmpty(),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onAny = { onImeAction() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF90CAF9),
                unfocusedLabelColor = Color(0xFF90CAF9),
                focusedBorderColor = Color(0xFF42A5F5),
                unfocusedBorderColor = Color(0xFF37474F),
                errorBorderColor = Color(0xFFEF5350),
                errorLabelColor = Color(0xFFEF5350),
                cursorColor = Color.White
            )
        )
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = Color(0xFFEF5350),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}