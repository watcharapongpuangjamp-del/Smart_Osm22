package com.example.data.vhv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing VHV Members directory sourced from ThaiPHC Report OSMRP00002.
 */
class VhvRepository(
    private val vhvMemberDao: VhvMemberDao
) {
    companion object {
        private const val TAG = "VhvRepository"
    }

    /**
     * Ensures the database is seeded with official OSMRP00002 data for Tambon Pa Kha.
     */
    suspend fun seedOsmRp00002DataIfEmpty() = withContext(Dispatchers.IO) {
        try {
            val count = vhvMemberDao.getVhvCount()
            if (count == 0) {
                Log.d(TAG, "Seeding OSMRP00002 VHV records for Tambon Pa Kha...")
                vhvMemberDao.insertAll(OsmRp00002Data.PA_KHA_VHV_MEMBERS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed OSMRP00002 VHV records", e)
        }
    }

    fun getAllVhvMembersFlow(): Flow<List<VhvMemberEntity>> {
        return vhvMemberDao.getAllVhvMembersFlow()
    }

    fun getVhvMembersByVillageFlow(villageNo: String): Flow<List<VhvMemberEntity>> {
        return if (villageNo == "ALL" || villageNo.isBlank()) {
            vhvMemberDao.getAllVhvMembersFlow()
        } else {
            vhvMemberDao.getVhvMembersByVillageFlow(villageNo)
        }
    }

    fun searchVhvMembersFlow(query: String): Flow<List<VhvMemberEntity>> {
        return vhvMemberDao.searchVhvMembersFlow(query)
    }

    suspend fun getVhvByCardId(cardId: String): VhvMemberEntity? = withContext(Dispatchers.IO) {
        vhvMemberDao.getVhvByCardId(cardId)
    }

    suspend fun insertOrUpdateVhv(member: VhvMemberEntity) = withContext(Dispatchers.IO) {
        vhvMemberDao.insert(member)
    }
}
