package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "person_history")
data class PersonHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val action: String, // "CREATE", "UPDATE", "DELETE", "IMPORT"
    val oldValue: String?, // JSON or formatted string of old state
    val newValue: String?, // JSON or formatted string of new state
    val timestamp: Long = System.currentTimeMillis(),
    val operatorId: String = "SYSTEM", 
    val operatorName: String = "System",
    val role: String = "SYSTEM",
    val deviceId: String = "local",
    val source: String = "SYSTEM" // e.g. "manual", "excel_import", "sync"
)
