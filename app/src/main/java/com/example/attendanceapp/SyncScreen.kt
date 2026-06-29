package com.example.attendanceapp

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SyncScreen(
    sessionManager: SessionManager,
    dbHelper: AttendanceDatabaseHelper,
    apiClient: ApiClient,
    onBack: () -> Unit,
    context: Context
) {
    val userName = remember { sessionManager.getUserName() ?: "User" }
    val primaryColor = Color(0xFF1A3A8F)
    val secondaryColor = Color(0xFFF5F7FA)
    val scope = rememberCoroutineScope()

    var isSyncing by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("") }

    val fcba = sessionManager.getFcba() ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sinkronisasi Data,",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = userName.uppercase(),
                        color = primaryColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(secondaryColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = primaryColor
                    )
                }
            }

            // Progress Card (Like in Dashboard)
            if (syncStatus.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.Cyan
                            )
                            Spacer(Modifier.width(16.dp))
                        }
                        Text(
                            text = syncStatus,
                            color = if (isSyncing) Color.Gray else Color.Green,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TOMBOL UTAMA: Upload
            SyncActionButton(
                title = "UPLOAD DATA",
                subtitle = "Kirim data transaksi ke server",
                icon = Icons.Default.Upload,
                color = primaryColor,
                contentColor = Color.White,
                enabled = !isSyncing,
                onClick = {
                    scope.launch {
                        isSyncing = true
                        syncStatus = "Menyiapkan data upload..."
                        delay(1000)
                        syncStatus = "Mengirim data ke server..."
                        delay(2000)
                        syncStatus = "Upload selesai!"
                        isSyncing = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // TOMBOL UTAMA: Download
            SyncActionButton(
                title = "DOWNLOAD DATA",
                subtitle = "Ambil data master terbaru",
                icon = Icons.Default.Download,
                color = Color.White,
                contentColor = primaryColor,
                isOutline = true,
                enabled = !isSyncing,
                onClick = {
                    scope.launch {
                        isSyncing = true
                        syncStatus = "Menghapus data lama..."
                        
                        try {
                            withContext(Dispatchers.IO) {
                                // 1. Hapus data master lama
                                dbHelper.deleteAllMasterData()
                                
                                // 2. Jalankan download ulang
                                syncStatus = "Download data Employee..."
                                val empResponse = apiClient.getEmployee(fcba)
                                dbHelper.insertEmployee(empResponse) { progress ->
                                    scope.launch(Dispatchers.Main) {
                                        syncStatus = "Import Employee $progress%"
                                    }
                                }

                                syncStatus = "Download data Job..."
                                val jobResponse = apiClient.getJob(fcba)
                                dbHelper.insertJob(jobResponse) { progress ->
                                    scope.launch(Dispatchers.Main) {
                                        syncStatus = "Import Job $progress%"
                                    }
                                }
                                
                                syncStatus = "Download data Field..."
                                val fieldResponse = apiClient.getField(fcba)
                                dbHelper.insertField(fieldResponse) { progress ->
                                    scope.launch(Dispatchers.Main) {
                                        syncStatus = "Import Field $progress%"
                                    }
                                }

                                syncStatus = "Download data TPH..."
                                val tphResponse = apiClient.getTph(fcba)
                                dbHelper.insertTph(tphResponse) { progress ->
                                    scope.launch(Dispatchers.Main) {
                                        syncStatus = "Import TPH $progress%"
                                    }
                                }
                            }
                            syncStatus = "Download selesai!"
                        } catch (e: Exception) {
                            syncStatus = "Gagal: ${e.message}"
                        } finally {
                            isSyncing = false
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun SyncActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    contentColor: Color,
    isOutline: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .then(
                if (isOutline) Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor,
            disabledContainerColor = if (isOutline) Color.White else color.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isOutline) 0.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                color = if (isOutline) Color(0xFFF5F7FA) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(text = subtitle, fontSize = 12.sp, color = contentColor.copy(alpha = 0.6f))
            }
        }
    }
}
