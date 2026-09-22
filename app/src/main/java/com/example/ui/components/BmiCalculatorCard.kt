package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary

@Composable
fun BmiCalculatorCard(
    modifier: Modifier = Modifier
) {
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }

    val weight = weightInput.toFloatOrNull()
    val heightCm = heightInput.toFloatOrNull()

    val bmi = remember(weight, heightCm) {
        if (weight != null && heightCm != null && heightCm > 0f) {
            val heightM = heightCm / 100f
            weight / (heightM * heightM)
        } else {
            null
        }
    }

    val bmiCategory = remember(bmi) {
        when {
            bmi == null -> Triple("กรอกน้ำหนักและส่วนสูง", "รอข้อมูลการคำนวณ", Color.Gray)
            bmi < 18.5f -> Triple("น้ำหนักน้อย / ผอม", "เสี่ยงต่อภาวะขาดสารอาหาร", Color(0xFF0288D1))
            bmi < 23.0f -> Triple("ปกติ (สมส่วน)", "อยู่ในเกณฑ์สุขภาพดีเยี่ยม", Color(0xFF2E7D32))
            bmi < 25.0f -> Triple("ท้วม / น้ำหนักเกิน", "เริ่มเสี่ยงต่อโรคเรื้อรัง", Color(0xFFF57C00))
            bmi < 30.0f -> Triple("อ้วน (ระดับ 1)", "เสี่ยงต่อโรคเบาหวานและความดัน", Color(0xFFD32F2F))
            else -> Triple("อ้วนอันตราย (ระดับ 2)", "เสี่ยงสูงมาก ควรปรึกษาแพทย์", Color(0xFFB71C1C))
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = EmeraldPrimary)
                }
                Column {
                    Text(
                        text = "เครื่องคำนวณดัชนีมวลกาย (BMI)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ประเมินภาวะโภชนาการเบื้องต้น",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("น้ำหนัก (กก.)") },
                    placeholder = { Text("เช่น 60") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = { Text("ส่วนสูง (ซม.)") },
                    placeholder = { Text("เช่น 170") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            if (bmi != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = bmiCategory.third.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ค่า BMI ของคุณ: %.1f".format(bmi),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = bmiCategory.third
                        )
                        Text(
                            text = bmiCategory.first,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = bmiCategory.third
                        )
                        Text(
                            text = bmiCategory.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "กรุณากรอกน้ำหนักและส่วนสูงเพื่อคำนวณ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
