package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataListScreen(
    type: String,
    dbHelper: AttendanceDatabaseHelper,
    fcba: String,
    onBack: () -> Unit
) {
    val primaryColor = Color(0xFF1A3A8F)
    var searchQuery by remember { mutableStateOf("") }
    
    val rawData = remember(type, fcba) {
        when (type) {
            "JOB" -> dbHelper.getAllJobsDetails(fcba)
            "FIELD" -> dbHelper.getAllFieldsDetails(fcba)
            "TPH" -> dbHelper.getAllTphsDetails(fcba)
            "FCBA" -> dbHelper.getAllFcbasDetails()
            "MILL" -> dbHelper.getAllMills(fcba).map { 
                mapOf("Code" to it.code, "Name" to it.name, "FCBA" to it.fcba)
            }
            "VEHICLE" -> dbHelper.getAllVehicles(fcba).map {
                mapOf("Code" to it.code, "Name" to it.name, "Reg No" to it.regNo, "FCBA" to it.fcba)
            }
            "NURSERY" -> dbHelper.getAllNurseriesDetails(fcba)
            "GC" -> dbHelper.getGcMaster(fcba)
            "WORKSHOP" -> dbHelper.getWorkshopMaster(fcba)
            "BJR" -> dbHelper.getAllBjrsDetails(fcba)
            else -> emptyList()
        }
    }

    val filteredData = remember(rawData, searchQuery) {
        if (searchQuery.isBlank()) rawData
        else {
            rawData.filter { map ->
                map.values.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar $type", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F7FA))
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Cari data...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = primaryColor
                )
            )

            if (filteredData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Data tidak ditemukan", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredData) { item ->
                        MasterDataItemCard(item, primaryColor)
                    }
                }
            }
        }
    }
}

@Composable
fun MasterDataItemCard(item: Map<String, String>, primaryColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Kita coba tebak kolom mana yang jadi judul
            val keys = item.keys.toList()
            val titleKey = keys.find { it.contains("NAME", ignoreCase = true) || it.contains("DESCRIPTION", ignoreCase = true) || it.contains("BJR", ignoreCase = true) } ?: keys.firstOrNull()
            val codeKey = keys.find { it.contains("CODE", ignoreCase = true) || it.contains("ID", ignoreCase = true) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item[titleKey] ?: "-",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                if (codeKey != null && codeKey != titleKey) {
                    Surface(
                        color = Color(0xFFE8F0FE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item[codeKey] ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray)

            // Tampilkan sisa kolom
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.forEach { (key, value) ->
                    if (key != titleKey && key != codeKey && value.isNotBlank() && key.uppercase() != "LASTUPDATE" && key.uppercase() != "LASTTIME" && key.uppercase() != "FCIP") {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "$key:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray,
                                modifier = Modifier.width(150.dp)
                            )
                            Text(
                                text = value,
                                fontSize = 11.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
