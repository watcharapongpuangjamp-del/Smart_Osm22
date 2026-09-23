package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "health_screenings",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId")]
)
data class HealthScreening(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val screeningUuid: String = UUID.randomUUID().toString(),
    val personId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val weight: Double? = null,
    val height: Double? = null,
    val bmi: Double? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val bloodSugar: Int? = null,
    val pulse: Int? = null,
    val temperature: Double? = null,
    val oxygenSaturation: Int? = null,
    val note: String? = null,
    val vhvId: String? = null,
    val vhvName: String? = null,
    val dataStatus: DataStatus = DataStatus.NEEDS_REVIEW
)
