package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PersonViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonHistoryScreen(
    viewModel: PersonViewModel,
    personId: Long,
    onNavigateBack: () -> Unit
) {
    val historyList by viewModel.getHistoryForPerson(personId).collectAsStateWithLifecycle(initialValue = emptyList())
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ประวัติการแก้ไขข้อมูล",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ย้อนกลับ",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.example.ui.theme.EmeraldPrimary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (historyList.isEmpty()) {
                item {
                    Text("ไม่มีประวัติการแก้ไข", modifier = Modifier.padding(16.dp))
                }
            } else {
                items(historyList) { history ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Action: ${history.action}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (history.action) {
                                        "CREATE" -> MaterialTheme.colorScheme.primary
                                        "UPDATE" -> MaterialTheme.colorScheme.secondary
                                        "DELETE" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = dateFormat.format(Date(history.timestamp)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ผู้ทำรายการ: ${history.operatorName} (Device: ${history.deviceId}, Source: ${history.source})", style = MaterialTheme.typography.bodySmall)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (history.oldValue != null) {
                                Text("ข้อมูลเดิม:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text(history.oldValue, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (history.newValue != null) {
                                Text("ข้อมูลใหม่:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text(history.newValue, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
