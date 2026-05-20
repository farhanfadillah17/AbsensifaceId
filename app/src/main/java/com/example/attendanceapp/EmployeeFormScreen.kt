package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeFormScreen(
    dbHelper: AttendanceDatabaseHelper,
    onBack: () -> Unit,
    onNext: (Employee) -> Unit,
) {
    var masterEmployees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var filteredEmployees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

    // Load data di background thread
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val data = dbHelper.getAllMasterEmployees()
            masterEmployees = data
            filteredEmployees = data
            isLoading = false
        }
    }

    // Update filter saat user mengetik nama/fccode
    LaunchedEffect(searchQuery) {
        filteredEmployees = if (searchQuery.isEmpty()) {
            masterEmployees
        } else {
            masterEmployees.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.fccode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrasi Wajah", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A237E))
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212))
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text("Langkah 1: Pilih Karyawan", color = Color(0xFF90CAF9), fontSize = 14.sp)
                Text("Pilih dari Master Data", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)

                Spacer(Modifier.height(24.dp))

                // TOMBOL PEMICU BOTTOM SHEET (Pengganti Dropdown agar tidak lag)
                Text("Cari Nama atau FCCODE", color = Color.Gray, fontSize = 12.sp)
                OutlinedTextField(
                    value = if (selectedEmployee != null) "${selectedEmployee!!.fccode} - ${selectedEmployee!!.name}" else "Klik untuk mencari...",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSheet = true },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.White,
                        disabledBorderColor = Color.Gray,
                        disabledTrailingIconColor = Color.White
                    )
                )

                Spacer(Modifier.height(24.dp))

                if (selectedEmployee != null) {
                    InfoDisplayField(label = "Nama Karyawan", value = selectedEmployee!!.name)
                    InfoDisplayField(label = "Seksi / Dept", value = selectedEmployee!!.sectionName)
                    InfoDisplayField(label = "Jabatan", value = selectedEmployee!!.position)
                    InfoDisplayField(label = "Business Area (FCBA)", value = selectedEmployee!!.fcba)
                }

                Spacer(Modifier.height(40.dp))

                if (showError) {
                    Text("Silakan pilih karyawan terlebih dahulu", color = Color.Red, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (selectedEmployee != null) onNext(selectedEmployee!!) else showError = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
                ) {
                    Text("LANJUT SCAN WAJAH", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // MODAL BOTTOM SHEET UNTUK PENCARIAN (Anti-Lag)
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E1E1E)
        ) {
            Column(modifier = Modifier.fillMaxHeight(0.8f).padding(16.dp)) {
                Text("Cari Karyawan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))

                // Input Pencarian
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Ketik nama atau kode...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(Modifier.height(16.dp))

                // Daftar menggunakan LazyColumn (Sangat Cepat untuk ribuan data)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredEmployees) { emp ->
                        ListItem(
                            headlineContent = { Text(emp.name, color = Color.White) },
                            supportingContent = { Text("${emp.fccode} | ${emp.sectionName}", color = Color.Gray) },
                            modifier = Modifier.clickable {
                                selectedEmployee = emp
                                showSheet = false
                                showError = false
                                searchQuery = "" // Reset search
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoDisplayField(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(label, color = Color(0xFF90CAF9), fontSize = 12.sp)
        Text(
            text = value.ifEmpty { "-" },
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
    }
}