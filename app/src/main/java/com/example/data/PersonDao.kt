package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY id ASC")
    fun getAllPersons(): Flow<List<Person>>

    @Query("SELECT * FROM persons ORDER BY id ASC")
    suspend fun getAllPersonsList(): List<Person>

    @Query("SELECT COUNT(*) FROM persons")
    fun getTotalPersonsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPerson(person: Person): Long

    @Update
    suspend fun updatePerson(person: Person)

    @Delete
    suspend fun deletePerson(person: Person)

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: Long): Person?

    @Query("SELECT * FROM persons WHERE personUuid = :uuid LIMIT 1")
    suspend fun getPersonByUuid(uuid: String): Person?
    
    @Query("SELECT * FROM persons WHERE nationalId = :nationalId LIMIT 1")
    suspend fun getPersonByNationalId(nationalId: String): Person?
    
    @Query("SELECT * FROM persons WHERE householdId = :householdId")
    fun getPersonsByHouseholdId(householdId: Long): Flow<List<Person>>
}
