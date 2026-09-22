package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "households",
    indices = [
        Index(value = ["householdUuid"], unique = true)
    ]
)
data class Household(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val householdUuid: String = java.util.UUID.randomUUID().toString(),
    val houseNo: String,
    val villageNo: String = "",
    val subdistrict: String = "",
    val district: String = "",
    val province: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Float? = null,
    val locationCapturedAt: Long? = null,
    val locationProvider: String? = null,
    val dataStatus: DataStatus = DataStatus.NEEDS_REVIEW,
    val lastModified: Long = System.currentTimeMillis()
)
