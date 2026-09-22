package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PopulationEventDao {
    @Query("SELECT * FROM population_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<PopulationEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PopulationEvent): Long

    @Update
    suspend fun updateEvent(event: PopulationEvent)

    @Delete
    suspend fun deleteEvent(event: PopulationEvent)

    @Query("SELECT * FROM population_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): PopulationEvent?

    @Query("SELECT * FROM population_events WHERE personId = :personId ORDER BY timestamp DESC")
    fun getEventsByPersonId(personId: Long): Flow<List<PopulationEvent>>

    @Query("SELECT * FROM population_events WHERE householdId = :householdId ORDER BY timestamp DESC")
    fun getEventsByHouseholdId(householdId: Long): Flow<List<PopulationEvent>>
}
