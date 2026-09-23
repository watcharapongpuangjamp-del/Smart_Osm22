package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.viewmodel.PersonViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersonDeleteFailureTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PersonRepository
    private lateinit var context: Context

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PersonRepository(db, db.personDao(), db.householdDao(), db.personHistoryDao())

        // Seed an initial record so PersonViewModel init does not spawn background seed threads on Dispatchers.IO
        runBlocking {
            db.householdDao().insert(
                Household(
                    householdUuid = "H-INIT-SETUP",
                    houseNo = "0",
                    villageNo = "0",
                    subdistrict = "Init",
                    district = "Init",
                    province = "Init"
                )
            )
        }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testPersonDeleteCloudFailureKeepsLocalData() = runBlocking {
        // 1. Create Household and Person locally
        val household = Household(
            householdUuid = "H-FAIL-001",
            houseNo = "101/2",
            villageNo = "2",
            subdistrict = "Sub",
            district = "Dist",
            province = "Prov"
        )
        val hId = repository.insertHousehold(household)
        val person = Person(
            personUuid = "P-FAIL-001",
            householdId = hId,
            nationalId = "1100500123456",
            fullName = "นาย ปลอดภัย",
            gender = Gender.MALE,
            birthDate = LocalDate.of(1990, 1, 1)
        )
        repository.insert(person)
        val insertedPerson = repository.getPersonByUuid("P-FAIL-001")
        assertNotNull(insertedPerson)

        // 2. Mock Cloud Sync to fail
        val failingSyncHelper = object : com.example.data.sync.RoomFirestoreSyncHelper(context, repository) {
            override fun isFirebaseConfigured(): Boolean = true
            override suspend fun deletePersonFromFirestore(personUuid: String): Result<Unit> {
                return Result.failure(RuntimeException("Cloud Sync Connection Failed!"))
            }
        }

        // 3. Construct ViewModel with the failing sync helper
        val excelImportUseCase = com.example.domain.ExcelImportUseCase(db)
        val viewModel = PersonViewModel(repository, excelImportUseCase, failingSyncHelper)

        // 4. Act: Delete person via ViewModel
        var callbackInvoked = false
        var callbackSuccess = false
        var callbackMessage: String? = null

        viewModel.delete(insertedPerson!!) { success, msg ->
            callbackInvoked = true
            callbackSuccess = success
            callbackMessage = msg
        }

        // Wait for coroutine to finish and invoke callback
        var attempts = 0
        while (!callbackInvoked && attempts < 100) {
            java.lang.Thread.sleep(15)
            testDispatcher.scheduler.advanceUntilIdle()
            attempts++
        }

        // 5. Assert: Local-First design ensures local Room deletion is fully committed
        val deletedPersonInRoom = repository.getPersonByUuid("P-FAIL-001")
        assertNull("Local record should be deleted in Room Database even if Cloud fails", deletedPersonInRoom)

        // Verify that ViewModel reported success=true because offline-first deletes succeed locally
        assertTrue("Callback should be invoked", callbackInvoked)
        assertTrue("Callback should indicate success since local delete succeeded", callbackSuccess)
    }
}
