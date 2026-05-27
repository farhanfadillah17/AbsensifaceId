package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    dbHelper: AttendanceDatabaseHelper,
    onBack: () -> Unit
) {
    // State untuk menampung data
    var records by remember { mutableStateOf<List<AttendanceRecord>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Efek untuk mengambil data secara Asynchronous (Tidak mengunci UI)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Mengambil data di background thread
            records = dbHelper.getAllAttendance()
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Absensi", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1A3A8F), // Warna Indigo Dashboard
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    // Tampilkan Loading jika data sedang diambil
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1A3A8F))
                        Spacer(Modifier.height(16.dp))
                        Text("Memuat data...", color = Color.Gray)
                    }
                }
                records.isNullOrEmpty() -> {
                    // Tampilkan pesan jika data kosong
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Belum ada riwayat absensi",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    // Tampilkan List jika data ada
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(records!!) { record ->
                            AttendanceItemCard(record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceItemCard(record: AttendanceRecord) {
    val isCheckIn = record.action.contains("IN", ignoreCase = true)
    val cardAccent = if (isCheckIn) Color(0xFF2E7D32) else Color(0xFFC62828) // Hijau Masuk, Merah Keluar

    // Format Tanggal agar lebih rapi
    val formattedTime = remember(record.timestamp) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = parser.parse(record.timestamp)
            date?.let { formatter.format(it) } ?: record.timestamp
        } catch (e: Exception) {
            record.timestamp
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Status
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = cardAccent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isCheckIn) Icons.Default.Login else Icons.Default.Logout,
                        contentDescription = null,
                        tint = cardAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.employeeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF333333)
                )
                Text(
                    text = "ID: ${record.employeeId}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = Color(0xFF1A3A8F),
                    fontWeight = FontWeight.Medium
                )
            }

            // Label Action
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = cardAccent.copy(alpha = 0.1f)
            ) {
                Text(
                    text = if (isCheckIn) "MASUK" else "KELUAR",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardAccent
                )
            }
        }
    }
}