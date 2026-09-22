package com.example.domain

object SmartOsmExcelSchema {
    const val SCHEMA_VERSION = "SMART_OSM_EXCEL_V1"
    const val SHEET_NAME = "SMART_OSM_V1"

    const val COL_SCHEMA_VERSION = "schemaVersion"
    const val COL_HOUSEHOLD_UUID = "householdUuid"
    const val COL_PERSON_UUID = "personUuid"
    const val COL_HOUSE_NO = "houseNo"
    const val COL_VILLAGE_NO = "villageNo"
    const val COL_SUBDISTRICT = "subdistrict"
    const val COL_DISTRICT = "district"
    const val COL_PROVINCE = "province"
    const val COL_NATIONAL_ID = "nationalId"
    const val COL_FULL_NAME = "fullName"
    const val COL_GENDER = "gender"
    const val COL_BIRTH_DATE = "birthDate"
    const val COL_BIRTH_DATE_PRECISION = "birthDatePrecision"
    const val COL_HOUSE_STATUS = "houseStatus"
    const val COL_PERSON_STATUS = "personStatus"
    const val COL_DATA_STATUS = "dataStatus"

    val CANONICAL_COLUMNS = listOf(
        COL_SCHEMA_VERSION,
        COL_HOUSEHOLD_UUID,
        COL_PERSON_UUID,
        COL_HOUSE_NO,
        COL_VILLAGE_NO,
        COL_SUBDISTRICT,
        COL_DISTRICT,
        COL_PROVINCE,
        COL_NATIONAL_ID,
        COL_FULL_NAME,
        COL_GENDER,
        COL_BIRTH_DATE,
        COL_BIRTH_DATE_PRECISION,
        COL_HOUSE_STATUS,
        COL_PERSON_STATUS,
        COL_DATA_STATUS
    )

    private val ALIAS_MAP = mapOf(
        // Schema Version
        "schemaversion" to COL_SCHEMA_VERSION,
        "schema_version" to COL_SCHEMA_VERSION,
        "เวอร์ชัน" to COL_SCHEMA_VERSION,
        "เวอร์ชันสคีมา" to COL_SCHEMA_VERSION,

        // Household UUID
        "householduuid" to COL_HOUSEHOLD_UUID,
        "household_uuid" to COL_HOUSEHOLD_UUID,
        "householduid" to COL_HOUSEHOLD_UUID,
        "รหัสครัวเรือน" to COL_HOUSEHOLD_UUID,
        "รหัสบ้าน" to COL_HOUSEHOLD_UUID,

        // Person UUID
        "personuuid" to COL_PERSON_UUID,
        "person_uuid" to COL_PERSON_UUID,
        "personuid" to COL_PERSON_UUID,
        "รหัสบุคคล" to COL_PERSON_UUID,
        "รหัสประชากร" to COL_PERSON_UUID,

        // House No
        "houseno" to COL_HOUSE_NO,
        "house_no" to COL_HOUSE_NO,
        "housenumber" to COL_HOUSE_NO,
        "บ้านเลขที่" to COL_HOUSE_NO,
        "เลขที่บ้าน" to COL_HOUSE_NO,

        // Village No
        "villageno" to COL_VILLAGE_NO,
        "village_no" to COL_VILLAGE_NO,
        "หมู่ที่" to COL_VILLAGE_NO,
        "หมู่" to COL_VILLAGE_NO,

        // Subdistrict
        "subdistrict" to COL_SUBDISTRICT,
        "sub_district" to COL_SUBDISTRICT,
        "tambon" to COL_SUBDISTRICT,
        "ตำบล" to COL_SUBDISTRICT,
        "แขวง" to COL_SUBDISTRICT,

        // District
        "district" to COL_DISTRICT,
        "amphur" to COL_DISTRICT,
        "amphoe" to COL_DISTRICT,
        "อำเภอ" to COL_DISTRICT,
        "เขต" to COL_DISTRICT,

        // Province
        "province" to COL_PROVINCE,
        "changwat" to COL_PROVINCE,
        "จังหวัด" to COL_PROVINCE,

        // National ID
        "nationalid" to COL_NATIONAL_ID,
        "national_id" to COL_NATIONAL_ID,
        "citizenid" to COL_NATIONAL_ID,
        "citizen_id" to COL_NATIONAL_ID,
        "idcard" to COL_NATIONAL_ID,
        "เลขบัตรประชาชน" to COL_NATIONAL_ID,
        "เลขประจำตัวประชาชน" to COL_NATIONAL_ID,
        "เลขประชาชน" to COL_NATIONAL_ID,

        // Full Name
        "fullname" to COL_FULL_NAME,
        "full_name" to COL_FULL_NAME,
        "name" to COL_FULL_NAME,
        "ชื่อ-นามสกุล" to COL_FULL_NAME,
        "ชื่อนามสกุล" to COL_FULL_NAME,
        "ชื่อสกุล" to COL_FULL_NAME,
        "ชื่อ" to COL_FULL_NAME,

        // Gender
        "gender" to COL_GENDER,
        "sex" to COL_GENDER,
        "เพศ" to COL_GENDER,

        // Birth Date
        "birthdate" to COL_BIRTH_DATE,
        "birth_date" to COL_BIRTH_DATE,
        "dob" to COL_BIRTH_DATE,
        "วันเกิด" to COL_BIRTH_DATE,
        "วันเดือนปีเกิด" to COL_BIRTH_DATE,

        // Birth Date Precision
        "birthdateprecision" to COL_BIRTH_DATE_PRECISION,
        "birth_date_precision" to COL_BIRTH_DATE_PRECISION,
        "dateprecision" to COL_BIRTH_DATE_PRECISION,
        "ความแม่นยำวันเกิด" to COL_BIRTH_DATE_PRECISION,
        "ความละเอียดวันเกิด" to COL_BIRTH_DATE_PRECISION,

        // House Status / Role
        "housestatus" to COL_HOUSE_STATUS,
        "house_status" to COL_HOUSE_STATUS,
        "houserole" to COL_HOUSE_STATUS,
        "สถานะครัวเรือน" to COL_HOUSE_STATUS,
        "สถานะในบ้าน" to COL_HOUSE_STATUS,
        "สถานะในครัวเรือน" to COL_HOUSE_STATUS,

        // Person Status
        "personstatus" to COL_PERSON_STATUS,
        "person_status" to COL_PERSON_STATUS,
        "สถานะบุคคล" to COL_PERSON_STATUS,
        "สถานภาพบุคคล" to COL_PERSON_STATUS,

        // Data Status
        "datastatus" to COL_DATA_STATUS,
        "data_status" to COL_DATA_STATUS,
        "สถานะข้อมูล" to COL_DATA_STATUS
    )

    fun resolveColumnKey(headerName: String?): String? {
        if (headerName.isNullOrBlank()) return null
        val normalized = headerName.trim()
            .lowercase()
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
        return ALIAS_MAP[normalized] ?: ALIAS_MAP[headerName.trim().lowercase()]
    }
}
