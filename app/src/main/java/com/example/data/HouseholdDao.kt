package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(household: Household): Long

    @Update
    suspend fun update(household: Household)

    @Delete
    suspend fun delete(household: Household)

    @Query("SELECT * FROM households WHERE id = :id LIMIT 1")
    suspend fun getHouseholdById(id: Long): Household?

    @Query("SELECT * FROM households WHERE houseNo = :houseNo LIMIT 1")
    suspend fun getHouseholdByNo(houseNo: String): Household?

    @Query("SELECT * FROM households WHERE householdUuid = :uuid LIMIT 1")
    suspend fun getHouseholdByUuid(uuid: String): Household?

    @Transaction
    @Query("SELECT * FROM households ORDER BY houseNo ASC")
    fun getHouseholdsWithPersons(): Flow<List<HouseholdWithPersons>>

    @Query("SELECT * FROM households ORDER BY houseNo ASC")
    suspend fun getAllHouseholds(): List<Household>

    @Query("SELECT COUNT(*) FROM households")
    fun getTotalHouseholdsCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM households WHERE id = :householdId LIMIT 1")
    fun getHouseholdWithPersonsById(householdId: Long): Flow<HouseholdWithPersons?>

    @Query("""
        SELECT h.id as householdId, h.houseNo, h.latitude, h.longitude, h.dataStatus as dataStatus,
        (SELECT fullName FROM persons WHERE householdId = h.id AND houseStatus = 'HEAD' LIMIT 1) as headName,
        COUNT(p.id) as totalMembers,
        SUM(CASE WHEN p.gender = 'MALE' THEN 1 ELSE 0 END) as males,
        SUM(CASE WHEN p.gender = 'FEMALE' THEN 1 ELSE 0 END) as females,
        SUM(CASE WHEN p.houseStatus = 'HEAD' THEN 1 ELSE 0 END) as owners,
        SUM(CASE WHEN p.houseStatus = 'RESIDENT' THEN 1 ELSE 0 END) as residents,
        SUM(CASE WHEN p.personStatus = 'DEAD' THEN 1 ELSE 0 END) as deceased,
        SUM(CASE WHEN p.personStatus = 'ALIVE' AND (CAST(strftime('%Y', 'now') AS INTEGER) - CAST(substr(p.birthDate, 1, 4) AS INTEGER)) >= 60 THEN 1 ELSE 0 END) as elderly,
        SUM(CASE WHEN p.personStatus = 'ALIVE' AND (CAST(strftime('%Y', 'now') AS INTEGER) - CAST(substr(p.birthDate, 1, 4) AS INTEGER)) BETWEEN 0 AND 12 THEN 1 ELSE 0 END) as children
        FROM households h
        LEFT JOIN persons p ON h.id = p.householdId
        GROUP BY h.id, h.houseNo, h.latitude, h.longitude, h.dataStatus
        ORDER BY h.houseNo ASC
    """)
    fun getHouseSummary(): Flow<List<HouseSummary>>
}
