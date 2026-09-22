package com.example.data

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

import androidx.room.withTransaction

class LocalDateAdapter {
    @ToJson
    fun toJson(value: LocalDate?): String? = value?.toString()

    @FromJson
    fun fromJson(value: String?): LocalDate? = value?.let {
        try { LocalDate.parse(it) } catch (e: Exception) { null }
    }
}

class PersonRepository(
    private val db: AppDatabase,
    private val personDao: PersonDao,
    private val householdDao: HouseholdDao,
    private val personHistoryDao: PersonHistoryDao,
    private val populationEventDao: PopulationEventDao
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .add(LocalDateAdapter())
        .build()
    private val personAdapter = moshi.adapter(Person::class.java)

    val allPersons: Flow<List<Person>> = personDao.getAllPersons()
    val allHouseholdsWithPersons: Flow<List<HouseholdWithPersons>> = householdDao.getHouseholdsWithPersons()
    val houseSummary: Flow<List<HouseSummary>> = householdDao.getHouseSummary()
    
    val totalPersonsCount: Flow<Int> = personDao.getTotalPersonsCount()
    val totalHouseholdsCount: Flow<Int> = householdDao.getTotalHouseholdsCount()

    // Household Operations
    suspend fun insertHousehold(household: Household): Long {
        return householdDao.insert(household)
    }

    suspend fun updateHousehold(household: Household) {
        householdDao.update(household)
    }

    suspend fun deleteHousehold(household: Household): Result<Unit> {
        return try {
            Log.d("PersonRepository", "Deleting household id: ${household.id}, uuid: ${household.householdUuid}, houseNo: ${household.houseNo}")
            householdDao.delete(household)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PersonRepository", "Failed to delete household id: ${household.id}", e)
            Result.failure(e)
        }
    }

    suspend fun getHouseholdById(id: Long): Household? {
        return householdDao.getHouseholdById(id)
    }
    
    suspend fun getHouseholdByNo(houseNo: String): Household? {
        return householdDao.getHouseholdByNo(houseNo)
    }

    suspend fun getHouseholdByUuid(uuid: String): Household? {
        return householdDao.getHouseholdByUuid(uuid)
    }
    
    fun getHouseholdWithPersonsById(id: Long): Flow<HouseholdWithPersons?> {
        return householdDao.getHouseholdWithPersonsById(id)
    }

    // Person Operations
    suspend fun insert(person: Person) {
        db.withTransaction {
            val newId = personDao.insertPerson(person)
            val insertedPerson = person.copy(id = newId)
            personHistoryDao.insert(
                PersonHistory(
                    personId = newId,
                    action = "CREATE",
                    oldValue = null,
                    newValue = personAdapter.toJson(insertedPerson)
                )
            )
        }
    }

    suspend fun update(person: Person) {
        db.withTransaction {
            val oldPerson = personDao.getPersonById(person.id)
            personDao.updatePerson(person)
            personHistoryDao.insert(
                PersonHistory(
                    personId = person.id,
                    action = "UPDATE",
                    oldValue = oldPerson?.let { personAdapter.toJson(it) },
                    newValue = personAdapter.toJson(person)
                )
            )
        }
    }

    suspend fun delete(person: Person) {
        db.withTransaction {
            val oldPerson = personDao.getPersonById(person.id)
            personDao.deletePerson(person)
            personHistoryDao.insert(
                PersonHistory(
                    personId = person.id,
                    action = "DELETE",
                    oldValue = oldPerson?.let { personAdapter.toJson(it) },
                    newValue = null
                )
            )
        }
    }
    
    suspend fun getPersonById(id: Long): Person? {
        return personDao.getPersonById(id)
    }
    
    suspend fun getPersonByNationalId(nationalId: String): Person? {
        return personDao.getPersonByNationalId(nationalId)
    }

    suspend fun getPersonByUuid(uuid: String): Person? {
        return personDao.getPersonByUuid(uuid)
    }
    
    fun getHistoryForPerson(personId: Long): Flow<List<PersonHistory>> {
        return personHistoryDao.getHistoryForPerson(personId)
    }

    // Population Event Operations
    val allEvents: Flow<List<PopulationEvent>> = populationEventDao.getAllEvents()

    suspend fun insertEvent(event: PopulationEvent): Long {
        return populationEventDao.insertEvent(event)
    }

    suspend fun updateEvent(event: PopulationEvent) {
        populationEventDao.updateEvent(event)
    }

    suspend fun deleteEvent(event: PopulationEvent) {
        populationEventDao.deleteEvent(event)
    }

    fun getEventsByPersonId(personId: Long): Flow<List<PopulationEvent>> {
        return populationEventDao.getEventsByPersonId(personId)
    }

    fun getEventsByHouseholdId(householdId: Long): Flow<List<PopulationEvent>> {
        return populationEventDao.getEventsByHouseholdId(householdId)
    }

    suspend fun getAllHouseholds(): List<Household> = householdDao.getAllHouseholds()
    suspend fun getAllPersonsList(): List<Person> = personDao.getAllPersonsList()
}
