package com.example.data.vhv

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VhvMemberDao {

    @Query("SELECT * FROM vhv_members ORDER BY CAST(villageNo AS INTEGER) ASC, fullName ASC")
    fun getAllVhvMembersFlow(): Flow<List<VhvMemberEntity>>

    @Query("SELECT * FROM vhv_members WHERE villageNo = :villageNo ORDER BY fullName ASC")
    fun getVhvMembersByVillageFlow(villageNo: String): Flow<List<VhvMemberEntity>>

    @Query("SELECT * FROM vhv_members WHERE vhvCardId = :vhvCardId LIMIT 1")
    suspend fun getVhvByCardId(vhvCardId: String): VhvMemberEntity?

    @Query("SELECT * FROM vhv_members WHERE nationalId = :nationalId LIMIT 1")
    suspend fun getVhvByNationalId(nationalId: String): VhvMemberEntity?

    @Query("""
        SELECT * FROM vhv_members 
        WHERE fullName LIKE '%' || :query || '%' 
           OR vhvCardId LIKE '%' || :query || '%' 
           OR nationalId LIKE '%' || :query || '%' 
           OR phone LIKE '%' || :query || '%'
        ORDER BY CAST(villageNo AS INTEGER) ASC, fullName ASC
    """)
    fun searchVhvMembersFlow(query: String): Flow<List<VhvMemberEntity>>

    @Query("SELECT COUNT(*) FROM vhv_members")
    suspend fun getVhvCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<VhvMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: VhvMemberEntity): Long

    @Update
    suspend fun update(member: VhvMemberEntity)

    @Query("DELETE FROM vhv_members")
    suspend fun deleteAll()
}
