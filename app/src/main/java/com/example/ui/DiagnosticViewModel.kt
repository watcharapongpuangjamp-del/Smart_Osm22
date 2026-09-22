package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Household
import com.example.data.Person
import com.example.data.PersonRepository
import com.example.data.sync.RoomFirestoreSyncHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DiagnosticResult(
    val orphanedInRoomHouseholds: List<String> = emptyList(),
    val orphanedInRoomPersons: List<String> = emptyList(),
    val orphanedInFirestoreHouseholds: List<String> = emptyList(),
    val orphanedInFirestorePersons: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DiagnosticViewModel(
    private val repository: PersonRepository,
    private val firestore: FirebaseFirestore?
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticResult())
    val state: StateFlow<DiagnosticResult> = _state

    fun runDiagnostic() {
        if (firestore == null) {
            _state.value = _state.value.copy(error = "Firebase is not configured.")
            return
        }

        viewModelScope.launch {
            _state.value = DiagnosticResult(isLoading = true)

            try {
                // Fetch local
                val localHouseholds = repository.getAllHouseholds()
                val localPersons = repository.getAllPersonsList()
                val localHouseholdUuids = localHouseholds.map { it.householdUuid }.toSet()
                val localPersonUuids = localPersons.map { it.personUuid }.toSet()

                // Fetch cloud
                val cloudHouseholdsSnapshot = firestore.collection(RoomFirestoreSyncHelper.COLLECTION_HOUSEHOLDS).get().await()
                val cloudPersonsSnapshot = firestore.collection(RoomFirestoreSyncHelper.COLLECTION_PERSONS).get().await()

                val cloudHouseholdUuids = cloudHouseholdsSnapshot.documents.map { it.id }.toSet()
                val cloudPersonUuids = cloudPersonsSnapshot.documents.map { it.id }.toSet()

                // Identify orphans
                val orphanedInRoomHouseholds = localHouseholdUuids.filter { it !in cloudHouseholdUuids }
                val orphanedInRoomPersons = localPersonUuids.filter { it !in cloudPersonUuids }
                val orphanedInFirestoreHouseholds = cloudHouseholdUuids.filter { it !in localHouseholdUuids }
                val orphanedInFirestorePersons = cloudPersonUuids.filter { it !in localPersonUuids }

                _state.value = DiagnosticResult(
                    orphanedInRoomHouseholds = orphanedInRoomHouseholds,
                    orphanedInRoomPersons = orphanedInRoomPersons,
                    orphanedInFirestoreHouseholds = orphanedInFirestoreHouseholds,
                    orphanedInFirestorePersons = orphanedInFirestorePersons,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = DiagnosticResult(error = e.message, isLoading = false)
            }
        }
    }
}
