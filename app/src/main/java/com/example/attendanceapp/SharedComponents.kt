package com.example.attendanceapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ClickableSearchField(
    label: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onClick() }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.Search, null) },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledBorderColor = if (enabled) Color.Gray else Color(0xFFE0E0E0),
                disabledLabelColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun SearchableListDialog(
    title: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = options.filter { it.contains(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Cari...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(filtered) { item ->
                        ListItem(
                            headlineContent = { Text(item) },
                            modifier = Modifier.clickable { onSelect(item) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun SearchableMapDialog(
    title: String,
    options: List<Map<String, String>>,
    displayProvider: (Map<String, String>) -> String,
    onDismiss: () -> Unit,
    onSelect: (Map<String, String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = options.filter { displayProvider(it).contains(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Cari...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(filtered) { item ->
                        ListItem(
                            headlineContent = { Text(displayProvider(item)) },
                            modifier = Modifier.clickable { onSelect(item) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun MultiSearchableListDialog(
    title: String,
    options: List<Map<String, String>>, // Berisi ID dan Nama Karyawan
    selectedItems: Set<String>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredOptions = options.filter {
        it["name"]?.contains(searchQuery, ignoreCase = true) == true ||
                it["id"]?.contains(searchQuery, ignoreCase = true) == true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari Nama/NIK...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(filteredOptions) { item ->
                        val id = item["id"] ?: ""
                        val name = item["name"] ?: ""

                        ListItem(
                            headlineContent = { Text("$id - $name") },
                            leadingContent = {
                                Checkbox(
                                    checked = selectedItems.contains(id),
                                    onCheckedChange = null // Click handled by ListItem
                                )
                            },
                            modifier = Modifier.clickable { onToggle(id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Selesai") }
        }
    )
}



