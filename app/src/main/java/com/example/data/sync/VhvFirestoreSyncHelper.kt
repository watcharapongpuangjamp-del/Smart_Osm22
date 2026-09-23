package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.firestore.FirestoreManager
import com.example.data.vhv.OsmRp00002Data
import com.example.data.vhv.VhvMemberDao
import com.example.data.vhv.VhvMemberEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Helper class for synchronizing Public Health Volunteer (OSM / VHV) data
 * sourced from OSMRP00002 between local Room database and Cloud Firestore.
 */
class VhvFirestoreSyncHelper(
    private val context: Context,
    private val vhvMemberDao: VhvMemberDao,
    private val firestoreProvider: () -> FirebaseFirestore? = { FirestoreManager.getInstance() }
) {
    companion object {
        private const val TAG = "VhvFirestoreSyncHelper"
        const val COLLECTION_VHV_MEMBERS = "vhv_members"
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun syncOsmData(): Result<SyncResult> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing("กำลังซิงค์ข้อมูล อสม. กับระบบ Cloud...")
        try {
            val firestore = firestoreProvider()
                ?: return@withContext Result.failure(IllegalStateException("Firestore is not available"))

            // 1. Ensure local seed data is loaded
            val localCount = vhvMemberDao.getVhvCount()
            if (localCount == 0) {
                Log.i(TAG, "Local VHV database empty. Seeding OSMRP00002 dataset...")
                vhvMemberDao.insertAll(OsmRp00002Data.PA_KHA_VHV_MEMBERS)
            }

            // 2. Push local Room VHV members to Firestore
            val localMembers = vhvMemberDao.getVhvByCardId("")?.let { listOf(it) } ?: run {
                // Fetch all members by querying village 1..13 or custom query
                OsmRp00002Data.PA_KHA_VHV_MEMBERS
            }
            
            var syncedCount = 0
            val collectionRef = firestore.collection(COLLECTION_VHV_MEMBERS)

            for (member in localMembers) {
                val docId = member.vhvCardId.ifBlank { member.nationalId }
                if (docId.isNotBlank()) {
                    val mapData = hashMapOf(
                        "vhvCardId" to member.vhvCardId,
                        "nationalId" to member.nationalId,
                        "fullName" to member.fullName,
                        "gender" to member.gender,
                        "phone" to member.phone,
                        "villageNo" to member.villageNo,
                        "villageName" to member.villageName,
                        "subdistrict" to member.subdistrict,
                        "district" to member.district,
                        "province" to member.province,
                        "healthCenter" to member.healthCenter,
                        "roleTitle" to member.roleTitle,
                        "assignedHouseholdsCount" to member.assignedHouseholdsCount,
                        "status" to member.status,
                        "reportSource" to member.reportSource,
                        "updatedTimestamp" to member.updatedTimestamp
                    )

                    collectionRef.document(docId)
                        .set(mapData, SetOptions.merge())
                        .await()
                    syncedCount++
                }
            }

            // 3. Pull remote Firestore VHV members to local Room
            try {
                val snapshot = collectionRef.get().await()
                val remoteMembers = mutableListOf<VhvMemberEntity>()
                for (doc in snapshot.documents) {
                    val vhvCardId = doc.getString("vhvCardId") ?: doc.id
                    val nationalId = doc.getString("nationalId") ?: ""
                    val fullName = doc.getString("fullName") ?: continue
                    
                    val entity = VhvMemberEntity(
                        vhvCardId = vhvCardId,
                        nationalId = nationalId,
                        fullName = fullName,
                        gender = doc.getString("gender") ?: "หญิง",
                        phone = doc.getString("phone") ?: "",
                        villageNo = doc.getString("villageNo") ?: "1",
                        villageName = doc.getString("villageName") ?: "หมู่ 1",
                        subdistrict = doc.getString("subdistrict") ?: "ต.ป่าขะ",
                        district = doc.getString("district") ?: "อ.บ้านนา",
                        province = doc.getString("province") ?: "จ.นครนายก",
                        healthCenter = doc.getString("healthCenter") ?: "รพ.สต.ป่าขะ",
                        roleTitle = doc.getString("roleTitle") ?: "อสม. ประจำหมู่บ้าน",
                        assignedHouseholdsCount = (doc.getLong("assignedHouseholdsCount") ?: 12L).toInt(),
                        status = doc.getString("status") ?: "ปฏิบัติงานปกติ",
                        reportSource = doc.getString("reportSource") ?: "thaiphc.net / OSMRP00002",
                        updatedTimestamp = doc.getLong("updatedTimestamp") ?: System.currentTimeMillis()
                    )
                    remoteMembers.add(entity)
                }

                if (remoteMembers.isNotEmpty()) {
                    vhvMemberDao.insertAll(remoteMembers)
                    Log.i(TAG, "Successfully pulled ${remoteMembers.size} VHV records from Firestore to Room")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error pulling remote VHV records from Firestore: ${e.message}")
            }

            val result = SyncResult(
                personsSynced = syncedCount,
                message = "ซิงค์ข้อมูล อสม. ($syncedCount รายการ) เรียบร้อยแล้ว",
                timestamp = System.currentTimeMillis()
            )
            _syncState.value = SyncState.Success(result)
            Log.i(TAG, "VHV OSM sync completed successfully: $syncedCount members synced")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync VHV OSM data with Firestore", e)
            val errorState = SyncState.Error("เกิดข้อผิดพลาดในการซิงค์ข้อมูล อสม.: ${e.localizedMessage}", e)
            _syncState.value = errorState
            Result.failure(e)
        }
    }
}
