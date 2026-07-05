package com.example.attendanceapp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.gson.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcRegisterScreen(
    dbHelper: AttendanceDatabaseHelper,
    employee: Employee,
    onBack: () -> Unit,
    onStartWriting: (String) -> Unit
) {
    val context = LocalContext.current
    var isWaitingForNfc by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrasi Kartu NFC", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A3A8F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Detail Karyawan:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(employee.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF1A3A8F))
                    Text("ID: ${employee.fccode}", fontSize = 14.sp)
                    Text("BA: ${employee.fcba}", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (!isWaitingForNfc) {
                Button(
                    onClick = {
                        val json = JsonObject().apply {
                            addProperty("fccode", employee.fccode)
                            addProperty("sectionname", employee.sectionName)
                            addProperty("gangcode", employee.gangCode)
                            addProperty("fcba", employee.fcba)
                        }
                        onStartWriting(json.toString())
                        isWaitingForNfc = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A8F))
                ) {
                    Icon(Icons.Default.Nfc, null)
                    Spacer(Modifier.width(12.dp))
                    Text("MULAI TULIS KARTU", fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = Color(0xFF1A3A8F).copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF1A3A8F))
                            Icon(Icons.Default.Nfc, null, modifier = Modifier.size(48.dp), tint = Color(0xFF1A3A8F))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "TEMPELKAN KARTU NFC SEKARANG",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A3A8F),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Jangan lepas kartu sampai muncul notifikasi berhasil",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    TextButton(onClick = { isWaitingForNfc = false }) {
                        Text("Batal")
                    }
                }
            }
        }
    }
}
