package com.example.data

enum class Gender(val value: String) {
    MALE("ชาย"),
    FEMALE("หญิง"),
    UNKNOWN("ไม่ระบุ");

    companion object {
        fun fromString(value: String): Gender {
            return entries.find { it.value == value || it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class HouseholdRole(val value: String) {
    HEAD("เจ้าบ้าน"),
    RESIDENT("ผู้อาศัย"),
    UNKNOWN("ไม่ระบุ");

    companion object {
        fun fromString(value: String): HouseholdRole {
            return entries.find { it.value == value || it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class PersonStatus(val value: String) {
    ALIVE("มีชีวิต"),
    DEAD("เสียชีวิต"),
    MOVED("ย้ายออก"),
    UNKNOWN("ไม่ระบุ");

    companion object {
        fun fromString(value: String): PersonStatus {
            return entries.find { it.value == value || it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class DataStatus(val value: String) {
    VERIFIED("ยืนยันแล้ว"),
    NEEDS_REVIEW("ต้องตรวจสอบ"),
    UNKNOWN("ไม่ระบุ");

    companion object {
        fun fromString(value: String): DataStatus {
            return entries.find { it.value == value || it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
