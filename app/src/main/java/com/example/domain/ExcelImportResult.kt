package com.example.domain

data class ExcelImportResult(
    val totalRows: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val duplicateCount: Int = 0,
    val invalidNationalIdCount: Int = 0,
    val invalidBirthDateCount: Int = 0,
    val invalidHouseNoCount: Int = 0,
    val needsReviewCount: Int = 0,
    val errors: List<ImportError> = emptyList()
)

data class ImportError(
    val rowNumber: Int,
    val reason: String
)
