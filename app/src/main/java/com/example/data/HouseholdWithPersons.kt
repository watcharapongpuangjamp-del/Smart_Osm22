package com.example.data

import androidx.room.Embedded
import androidx.room.Relation

data class HouseholdWithPersons(
    @Embedded val household: Household,
    @Relation(
        parentColumn = "id",
        entityColumn = "householdId"
    )
    val persons: List<Person>
)
