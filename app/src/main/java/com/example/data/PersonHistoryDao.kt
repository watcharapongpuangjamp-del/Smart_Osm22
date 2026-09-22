package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonHistoryDao {
    @Insert
    suspend fun insert(history: PersonHistory)

    @Query("SELECT * FROM person_history WHERE personId = :personId ORDER BY timestamp DESC")
    fun getHistoryForPerson(personId: Long): Flow<List<PersonHistory>>
    
    @Query("SELECT * FROM person_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<PersonHistory>>
}
