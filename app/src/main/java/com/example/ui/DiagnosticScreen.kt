package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DiagnosticScreen(viewModel: DiagnosticViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("System Diagnostic", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { viewModel.runDiagnostic() }, enabled = !state.isLoading) {
            Text(if (state.isLoading) "Running..." else "Run Diagnostic")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.error != null) {
            Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn {
                item { Text("Orphaned in Room (Households): ${state.orphanedInRoomHouseholds.size}", style = MaterialTheme.typography.titleMedium) }
                items(state.orphanedInRoomHouseholds) { Text(it) }
                
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                item { Text("Orphaned in Room (Persons): ${state.orphanedInRoomPersons.size}", style = MaterialTheme.typography.titleMedium) }
                items(state.orphanedInRoomPersons) { Text(it) }

                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                item { Text("Orphaned in Firestore (Households): ${state.orphanedInFirestoreHouseholds.size}", style = MaterialTheme.typography.titleMedium) }
                items(state.orphanedInFirestoreHouseholds) { Text(it) }
                
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                item { Text("Orphaned in Firestore (Persons): ${state.orphanedInFirestorePersons.size}", style = MaterialTheme.typography.titleMedium) }
                items(state.orphanedInFirestorePersons) { Text(it) }
            }
        }
    }
}
