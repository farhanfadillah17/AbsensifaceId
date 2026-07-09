package com.example.attendanceapp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceNFCScreen(
    action: AttendanceAction,
    dbHelper: AttendanceDatabaseHelper,
    onBack: () -> Unit,
    onNfcVerified: (Employee) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    
    val isReceiveMode = action == AttendanceAction.RECEIVE
    val titleText = if (isReceiveMode) "SINKRONISASI DATA" else "Langkah 1 — Scan NFC"
    val actionLabel = when (action) {
        AttendanceAction.RECEIVE -> "Terima Data"
        AttendanceAction.CHECK_IN -> "Masuk"
        else -> "Keluar"
    }

    var statusText by remember { mutableStateOf("Tempelkan kartu NFC karyawan") }
    var statusColor by remember { mutableStateOf(Color(0xFF1A3A8F)) }
    var isProcessing by remember { mutableStateOf(false) }

    // Register NFC Listener
    LaunchedEffect(Unit) {
        isProcessing = false // Reset status saat masuk screen
        activity?.dataToWrite = null // PENTING: Matikan mode menulis agar kartu tidak ter-overwrite
        activity?.onNfcRead = { rawJson ->
            if (!isProcessing) {
                try {
                    val gson = Gson()
                    val dataMap = gson.fromJson(rawJson, Map::class.java)
                    val scannedCode = dataMap["fccode"]?.toString()?.trim() ?: ""
                    val scannedBa = dataMap["fcba"]?.toString()?.trim() ?: ""

                    if (scannedCode.isNotEmpty()) {
                        isProcessing = true
                        
                        if (isReceiveMode) {
                            statusText = "✅ Data Mandor Terdeteksi"
                            statusColor = Color(0xFF2E7D32)
                            
                            activity?.runOnUiThread {
                                onNfcVerified(
                                    Employee(
                                        fccode = scannedCode,
                                        name = "Data Transfer",
                                        fcba = scannedBa,
                                        sectionName = "",
                                        gangCode = "",
                                        position = "",
                                        gender = "",
                                        address = "",
                                        companyNik = "",
                                        faceEmbedding = null,
                                        lastUpdate = ""
                                    )
                                )
                            }
                        } else {
                            val employee = dbHelper.getEmployeeByCodeBa(scannedCode, scannedBa)
                            if (employee != null) {
                                statusText = "✅ Berhasil: ${employee.name}"
                                statusColor = Color(0xFF2E7D32)
                                activity?.runOnUiThread {
                                    onNfcVerified(employee)
                                }
                            } else {
                                isProcessing = false
                                statusText = "❌ ID ($scannedCode) BA ($scannedBa) tidak terdaftar"
                                statusColor = Color(0xFFD32F2F)
                            }
                        }
                    }
                } catch (e: Exception) {
                    isProcessing = false
                    statusText = "❌ Format kartu tidak valid!"
                    statusColor = Color(0xFFD32F2F)
                }
            }
        }
    }

    // Unregister on Dispose
    DisposableEffect(Unit) {
        onDispose {
            activity?.onNfcRead = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titleText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (isReceiveMode) {
                            Text("Mode Penerimaan Data Kerani", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A3A8F),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(160.dp),
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = statusColor)
                    }
                    Icon(
                        Icons.Default.Nfc, 
                        null, 
                        modifier = Modifier.size(64.dp), 
                        tint = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = statusText,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor,
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Color(0xFF1A3A8F).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isReceiveMode) "📥 PENERIMAAN DATA OFFLINE" else "🔐 Absen $actionLabel — Langkah 1/2",
                    color = Color(0xFF1A3A8F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isReceiveMode)
                    "Tempelkan kartu NFC yang sudah diisi oleh Mandor.\nPastikan kartu menempel sempurna di bagian belakang HP."
                else "Silahkan tempelkan kartu NFC karyawan.\nSistem akan mencocokkan ID dengan database.",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Tombol Simulasi untuk Testing di Emulator
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val mockData = """
                        {
                            "fccode": "SA041",
                            "sectionname": "AFD-01",
                            "gangcode": "PN011",
                            "fcba": "KML"
                        }
                    """.trimIndent()
                    activity?.onNfcRead?.invoke(mockData)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Text("Simulasi Scan NFC (SA041)", color = Color.Black)
            }
        }
    }
}
