package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressMenuScreen(onBack: () -> Unit, onCategorySelected: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Kategori Progress", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A3A8F))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressCard("PERAWATAN", "Pemeliharaan tanaman & kebersihan", Icons.Default.Build) {
                onCategorySelected("PERAWATAN")
            }
            ProgressCard("PEMBIBITAN", "Penyemaian & perawatan bibit baru", Icons.Default.Agriculture) {
                onCategorySelected("PEMBIBITAN")
            }
        }
    }
}

@Composable
fun ProgressCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(40.dp), tint = Color(0xFF1A3A8F))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(desc, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

