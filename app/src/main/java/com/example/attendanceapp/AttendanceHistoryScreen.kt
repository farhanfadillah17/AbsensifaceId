package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    dbHelper: AttendanceDatabaseHelper,
    onBack: () -> Unit
) {
    // FIX: Menentukan tipe data secara eksplisit agar compiler tidak bingung
    val records: List<AttendanceRecord> = remember { dbHelper.getAllAttendance() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Absensi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A237E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F4FF))
                .padding(padding)
        ) {
            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "📭\nBelum ada riwayat absensi",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(records) { record ->
                        AttendanceItemCard(record)
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceItemCard(record: AttendanceRecord) {
    val isCheckIn = record.action == "CHECK_IN"
    val actionLabel = if (isCheckIn) "✅ Masuk" else "🔴 Keluar"
    val cardAccent = if (isCheckIn) Color(0xFF3F51B5) else Color(0xFFE53935)

    // FIX LOGIKA TANGGAL:
    // record.timestamp adalah String dari SQLite (format: yyyy-MM-dd HH:mm:ss)
    // Kita ubah menjadi format yang lebih enak dibaca (dd MMM yyyy, HH:mm)
    val formattedTime = remember(record.timestamp) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = parser.parse(record.timestamp)
            date?.let { formatter.format(it) } ?: record.timestamp
        } catch (e: Exception) {
            record.timestamp // Jika gagal parsing, tampilkan teks aslinya
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Indikator
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(cardAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCheckIn) "📷" else "🚪",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.employeeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A237E)
                )
                Text(
                    text = "ID: ${record.employeeId}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = Color(0xFF5C6BC0),
                    fontWeight = FontWeight.Medium
                )
            }

            // Badge Status
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardAccent.copy(alpha = 0.12f)
            ) {
                Text(
                    text = actionLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cardAccent
                )
            }
        }
    }
}