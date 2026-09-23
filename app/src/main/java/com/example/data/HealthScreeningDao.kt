package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthScreeningDao {
    @Insert
    suspend fun insert(screening: HealthScreening)

    @Update
    suspend fun update(screening: HealthScreening)

    @Delete
    suspend fun delete(screening: HealthScreening)

    @Query("SELECT * FROM health_screenings WHERE personId = :personId ORDER BY timestamp DESC")
    fun getScreeningsForPerson(personId: Long): Flow<List<HealthScreening>>

    @Query("SELECT * FROM health_screenings ORDER BY timestamp DESC")
    fun getAllScreenings(): Flow<List<HealthScreening>>

    @Query("SELECT * FROM health_screenings WHERE screeningUuid = :uuid LIMIT 1")
    suspend fun getScreeningByUuid(uuid: String): HealthScreening?
}
