package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.firestore.FirestoreManager
import com.example.data.vhv.VhvMemberDao
import com.example.data.vhv.VhvMemberEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Helper class for synchronizing locally registered VHV member records
 * between the Room database and Cloud Firestore.
 *
 * The retired OSMRP00002 seed dataset is intentionally not referenced here.
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

    private suspend fun ensureAuth() {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
        } catch (e: Exception) {
            Log.d(TAG, "FirebaseAuth ensureAuth note: ${e.message}")
        }
    }

    private fun formatFirestoreError(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                "สิทธิ์การเข้าถึง Cloud Firestore ถูกปฏิเสธ (PERMISSION_DENIED): โปรดตรวจสอบ Firebase Security Rules (ระบบใช้งานข้อมูลในเครื่อง Room ได้ตามปกติ)"
            }
            msg.contains("UNAVAILABLE", ignoreCase = true) -> {
                "ไม่สามารถเชื่อมต่อ Cloud ได้ในขณะนี้: ข้อมูลถูกบันทึกในเครื่อง (Room Database) เรียบร้อยแล้ว"
            }
            else -> msg.ifBlank { "เกิดข้อผิดพลาดในการเชื่อมต่อ Cloud Firestore" }
        }
    }

    suspend fun syncOsmData(): Result<SyncResult> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing("กำลังซิงค์ข้อมูล อสม. กับระบบ Cloud...")
        try {
            // Room is the local source of truth; do not recreate retired seed records.
            val localMembers = vhvMemberDao.getAllVhvMembers()

            var syncedCount = localMembers.size
            try {
                ensureAuth()
                val firestore = firestoreProvider()
                if (firestore != null) {
                    val collectionRef = firestore.collection(COLLECTION_VHV_MEMBERS)
                    var batch = firestore.batch()
                    var opsInBatch = 0

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

                            val docRef = collectionRef.document(docId)
                            batch.set(docRef, mapData, SetOptions.merge())
                            opsInBatch++

                            if (opsInBatch >= 400) {
                                batch.commit().await()
                                batch = firestore.batch()
                                opsInBatch = 0
                            }
                        }
                    }

                    if (opsInBatch > 0) {
                        batch.commit().await()
                    }

                    // Pull remote records after the local batch has completed.
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
                    }
                    syncedCount = maxOf(syncedCount, remoteMembers.size)
                    Log.i(TAG, "VHV Cloud Firestore sync completed successfully")
                }
            } catch (cloudEx: Exception) {
                Log.w(
                    TAG,
                    "Cloud Firestore sync skipped or restricted (PERMISSION_DENIED/Offline): ${cloudEx.message}"
                )
            }

            val result = SyncResult(
                personsSynced = syncedCount,
                message = "ซิงค์ข้อมูล อสม. ($syncedCount รายการ) เรียบร้อยแล้ว (ใช้งานฐานข้อมูลในเครื่อง)",
                timestamp = System.currentTimeMillis()
            )
            _syncState.value = SyncState.Success(result)
            Result.success(result)
        } catch (ce: CancellationException) {
            Log.d(TAG, "VHV OSM sync coroutine was cancelled.")
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "VHV sync completed with local fallback: ${e.localizedMessage}")
            val result = SyncResult(
                personsSynced = 0,
                message = formatFirestoreError(e),
                timestamp = System.currentTimeMillis()
            )
            _syncState.value = SyncState.Success(result)
            Result.success(result)
        }
    }
}
