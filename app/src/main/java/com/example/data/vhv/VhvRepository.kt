package com.example.data.vhv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing the current VHV member directory.
 *
 * The previous OSMRP00002 seed dataset was removed because it is incomplete
 * and no longer matches the current official VHV records.
 */
class VhvRepository(
    private val vhvMemberDao: VhvMemberDao
) {

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
