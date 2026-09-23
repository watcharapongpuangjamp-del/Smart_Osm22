package com.example.data.village

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class representing village baseline GIS & demographic data for Nakhon Nayok.
 */
@JsonClass(generateAdapter = true)
data class VillageBaseline(
    @Json(name = "pcode") val provinceCode: String = "",
    @Json(name = "pname") val provinceName: String = "",
    @Json(name = "acode") val districtCode: String = "",
    @Json(name = "aname") val districtName: String = "",
    @Json(name = "tcode") val subdistrictCode: String = "",
    @Json(name = "tname") val subdistrictName: String = "",
    @Json(name = "mcode") val villageCode: String = "",
    @Json(name = "mname") val villageName: String = "",
    @Json(name = "oct_side15_lat") val latStr: String = "",
    @Json(name = "oct_side15_lon") val lonStr: String = "",
    @Json(name = "oct_side15_wmen") val womenCountStr: String = "",
    @Json(name = "oct_side15_men") val menCountStr: String = "",
    @Json(name = "oct_side15_total") val totalPopulationStr: String = "",
    @Json(name = "oct_side15_house") val totalHouseCountStr: String = "",
    @Json(name = "oct_side15_road_name2") val mainRoadName: String = "",
    @Json(name = "oct_side15_local3_name") val localGovName: String = "",
    @Json(name = "oct_side15_river_name") val riverName: String = "",
    @Json(name = "oct_side15_dam_name") val damName: String = "",
    @Json(name = "oct_side15_reservoir_name") val reservoirName: String = "",
    @Json(name = "oct_side15_weir_name") val weirName: String = ""
) {
    val latitude: Double
        get() = latStr.toDoubleOrNull() ?: 14.2031

    val longitude: Double
        get() = lonStr.toDoubleOrNull() ?: 101.2123

    val totalHouseCount: Int
        get() = totalHouseCountStr.toIntOrNull() ?: 0

    val totalPopulation: Int
        get() = totalPopulationStr.toIntOrNull() ?: 0

    val womenCount: Int
        get() = womenCountStr.toIntOrNull() ?: 0

    val menCount: Int
        get() = menCountStr.toIntOrNull() ?: 0

    val villageNo: String
        get() {
            val last2 = villageCode.takeLast(2)
            val parsed = last2.toIntOrNull()
            return if (parsed != null && parsed < 50) parsed.toString() else villageCode.takeLast(2)
        }
}
