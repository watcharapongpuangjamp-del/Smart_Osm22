package com.example.domain

import com.example.data.Household
import com.example.data.Person
import com.example.utils.NationalIdStatus

enum class ImportAction {
    INSERT,
    UPDATE,
    SKIP,
    NEEDS_REVIEW
}

data class PlannedPersonImport(
    val rowNum: Int,
    var action: ImportAction,
    val personData: Person,
    val householdData: Household,
    val isNewHousehold: Boolean,
    val nationalIdStatus: NationalIdStatus,
    val isAmbiguousHousehold: Boolean = false,
    val isDuplicateUuid: Boolean = false,
    val reviewReasons: MutableList<String> = mutableListOf()
)

data class ImportPlan(
    val plannedItems: List<PlannedPersonImport>,
    val totalRows: Int,
    val insertCount: Int,
    val updateCount: Int,
    val skipCount: Int,
    val needsReviewCount: Int,
    val errors: List<ImportError>
)
