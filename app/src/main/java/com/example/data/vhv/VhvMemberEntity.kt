package com.example.data.vhv

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an official Village Health Volunteer (อสม.) member record
 * sourced from ThaiPHC Report OSMRP00002 (กรมสนับสนุนบริการสุขภาพ กระทรวงสาธารณสุข).
 *
 * Territory: ตำบลป่าขะ อำเภอบ้านนา จังหวัดนครนายก (รพ.สต.ป่าขะ)
 */
@Entity(
    tableName = "vhv_members",
    indices = [
        Index(value = ["vhvCardId"], unique = true),
        Index(value = ["nationalId"]),
        Index(value = ["villageNo"])
    ]
)
data class VhvMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vhvCardId: String,
    val nationalId: String,
    val fullName: String,
    val gender: String = "หญิง",
    val phone: String = "",
    val villageNo: String,
    val villageName: String,
    val subdistrict: String = "ต.ป่าขะ",
    val district: String = "อ.บ้านนา",
    val province: String = "จ.นครนายก",
    val healthCenter: String = "รพ.สต.ป่าขะ",
    val roleTitle: String = "อสม. ประจำหมู่บ้าน",
    val assignedHouseholdsCount: Int = 12,
    val status: String = "ปฏิบัติงานปกติ",
    val reportSource: String = "thaiphc.net / OSMRP00002",
    val updatedTimestamp: Long = System.currentTimeMillis()
)
