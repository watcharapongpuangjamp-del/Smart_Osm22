package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PopulationEventType(val value: String) {
    BIRTH("การเกิด"),
    DEATH("การตาย"),
    MOVE_IN("ย้ายเข้า"),
    MOVE_OUT("ย้ายออก"),
    HEALTH_CHECK("การตรวจสุขภาพ");
}

@Entity(
    tableName = "population_events",
    foreignKeys = [
        ForeignKey(
            entity = Household::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("householdId"),
        Index("personId")
    ]
)
data class PopulationEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: PopulationEventType,
    val title: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val householdId: Long? = null,
    val personId: Long? = null,
    val personName: String? = null
)
