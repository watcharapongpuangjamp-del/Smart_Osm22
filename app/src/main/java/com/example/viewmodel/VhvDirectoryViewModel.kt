package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.vhv.VhvMemberEntity
import com.example.data.vhv.VhvRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VhvDirectoryViewModel(
    private val vhvRepository: VhvRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedVillage = MutableStateFlow("ALL")
    val selectedVillage: StateFlow<String> = _selectedVillage.asStateFlow()

    init {
        viewModelScope.launch {
            vhvRepository.seedOsmRp00002DataIfEmpty()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val vhvMembers: StateFlow<List<VhvMemberEntity>> = combine(_searchQuery, _selectedVillage) { query, village ->
        Pair(query, village)
    }.flatMapLatest { (query, village) ->
        if (query.isNotBlank()) {
            vhvRepository.searchVhvMembersFlow(query)
        } else {
            vhvRepository.getVhvMembersByVillageFlow(village)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedVillage(villageNo: String) {
        _selectedVillage.value = villageNo
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedVillage.value = "ALL"
    }

    fun triggerCloudSync(context: android.content.Context) {
        com.example.data.sync.OsmSyncScheduler.triggerOneTimeSync(context)
    }
}
