package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "persons",
    foreignKeys = [
        ForeignKey(
            entity = Household::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("householdId"),
        Index("nationalId", unique = true),
        Index("personUuid", unique = true)
    ]
)
data class Person(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personUuid: String = java.util.UUID.randomUUID().toString(),
    val householdId: Long,
    val nationalId: String? = null,
    val fullName: String,
    val gender: Gender = Gender.MALE,
    val birthDate: LocalDate? = null,
    val isBirthYearOnly: Boolean = false,
    val houseStatus: HouseholdRole = HouseholdRole.RESIDENT,
    val personStatus: PersonStatus = PersonStatus.ALIVE,
    val dataStatus: DataStatus = DataStatus.NEEDS_REVIEW,
    val lastModified: Long = System.currentTimeMillis()
)
