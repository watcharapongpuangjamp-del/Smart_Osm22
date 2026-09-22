package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Gender
import com.example.data.Person
import com.example.data.PersonStatus
import com.example.ui.theme.*
import com.example.viewmodel.PersonViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonListScreen(
    viewModel: PersonViewModel,
    onPersonClick: (Long, Long) -> Unit // (personId, householdId)
) {
    val allHouseholdsWithPersons by viewModel.allHouseholdsWithPersons.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ทั้งหมด") }

    val filterOptions = listOf(
        "ทั้งหมด", 
        "ผู้สูงอายุ (60+)", 
        "เด็กวัยเรียน (6-12)", 
        "เด็กเล็ก (0-5)", 
        "เสียชีวิตแล้ว"
    )

    // Flat map to get list of pairs: (Person, HouseNo)
    val personsWithHouse = remember(allHouseholdsWithPersons) {
        allHouseholdsWithPersons.flatMap { hw ->
            hw.persons.map { person -> person to hw.household.houseNo }
        }
    }

    val filteredList = remember(personsWithHouse, searchQuery, selectedFilter) {
        var result = personsWithHouse

        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim()
            result = result.filter { 
                it.first.fullName.contains(query, ignoreCase = true) || 
                (it.first.nationalId?.contains(query) == true) 
            }
        }

        val currentYear = LocalDate.now().year
        when (selectedFilter) {
            "ผู้สูงอายุ (60+)" -> result = result.filter { 
                val age = it.first.birthDate?.let { dob -> currentYear - dob.year }
                it.first.personStatus == PersonStatus.ALIVE && age != null && age >= 60 
            }
            "เด็กวัยเรียน (6-12)" -> result = result.filter { 
                val age = it.first.birthDate?.let { dob -> currentYear - dob.year }
                it.first.personStatus == PersonStatus.ALIVE && age != null && age in 6..12 
            }
            "เด็กเล็ก (0-5)" -> result = result.filter { 
                val age = it.first.birthDate?.let { dob -> currentYear - dob.year }
                it.first.personStatus == PersonStatus.ALIVE && age != null && age in 0..5 
            }
            "เสียชีวิตแล้ว" -> result = result.filter { it.first.personStatus == PersonStatus.DEAD }
        }
        
        result.sortedBy { it.first.fullName }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "รายชื่อประชากร",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "ทั้งหมด ${personsWithHouse.size} คน",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ค้นหาด้วยชื่อ หรือเลขบัตร...", color = OnSurfaceTertiary) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = EmeraldPrimary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "ล้างการค้นหา", tint = OnSurfaceTertiary)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { option ->
                    val isSelected = selectedFilter == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedFilter != "ทั้งหมด") "ผลการค้นหา: ${filteredList.size} คน" else "รายชื่อทั้งหมด",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }
                
                if (filteredList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ไม่พบรายชื่อที่ค้นหา", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(filteredList, key = { it.first.id }) { (person, houseNo) ->
                        PersonListCard(
                            person = person,
                            houseNo = houseNo,
                            onClick = { onPersonClick(person.id, person.householdId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonListCard(
    person: Person,
    houseNo: String,
    onClick: () -> Unit
) {
    val isAlive = person.personStatus == PersonStatus.ALIVE
    val currentYear = LocalDate.now().year
    val age = if (isAlive && person.birthDate != null) currentYear - person.birthDate.year else null

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isAlive) MintAccent else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = if (isAlive) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // House No Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("บ้านเลขที่ $houseNo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (isAlive && age != null) {
                        Text("อายุ $age ปี", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (!isAlive) {
                        Text("เสียชีวิต", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
