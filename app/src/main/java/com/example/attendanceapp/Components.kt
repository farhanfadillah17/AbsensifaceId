package com.example.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepDot(label = "QR", active = currentStep >= 1, completed = currentStep > 1)
        Box(modifier = Modifier.width(40.dp).height(2.dp).background(if (currentStep > 1) Color(0xFF69F0AE) else Color.Gray))
        StepDot(label = "Wajah", active = currentStep >= 2, completed = currentStep > 2)
    }
}

@Composable
fun StepDot(label: String, active: Boolean, completed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (completed) Color(0xFF69F0AE) else if (active) Color(0xFF3F51B5) else Color.Gray,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}