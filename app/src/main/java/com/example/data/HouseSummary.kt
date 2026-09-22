package com.example.data

data class HouseSummary(
    val householdId: Long,
    val houseNo: String,
    val totalMembers: Int,
    val males: Int,
    val females: Int,
    val owners: Int,
    val residents: Int,
    val deceased: Int = 0,
    val elderly: Int = 0,
    val children: Int = 0,
    val latitude: Double?,
    val longitude: Double?,
    val dataStatus: DataStatus = DataStatus.NEEDS_REVIEW,
    val headName: String? = null
)

