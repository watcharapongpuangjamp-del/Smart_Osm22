package com.example.data.village

import org.json.JSONObject

/**
 * Data class representing village baseline GIS & demographic data for Nakhon Nayok.
 */
data class VillageBaseline(
    val provinceCode: String = "",
    val provinceName: String = "",
    val districtCode: String = "",
    val districtName: String = "",
    val subdistrictCode: String = "",
    val subdistrictName: String = "",
    val villageCode: String = "",
    val villageName: String = "",
    val latStr: String = "",
    val lonStr: String = "",
    val womenCountStr: String = "",
    val menCountStr: String = "",
    val totalPopulationStr: String = "",
    val totalHouseCountStr: String = "",
    val mainRoadName: String = "",
    val localGovName: String = "",
    val riverName: String = "",
    val damName: String = "",
    val reservoirName: String = "",
    val weirName: String = ""
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

    companion object {
        fun fromJson(obj: JSONObject): VillageBaseline {
            return VillageBaseline(
                provinceCode = obj.optString("pcode"),
                provinceName = obj.optString("pname"),
                districtCode = obj.optString("acode"),
                districtName = obj.optString("aname"),
                subdistrictCode = obj.optString("tcode"),
                subdistrictName = obj.optString("tname"),
                villageCode = obj.optString("mcode"),
                villageName = obj.optString("mname"),
                latStr = obj.optString("oct_side15_lat"),
                lonStr = obj.optString("oct_side15_lon"),
                womenCountStr = obj.optString("oct_side15_wmen"),
                menCountStr = obj.optString("oct_side15_men"),
                totalPopulationStr = obj.optString("oct_side15_total"),
                totalHouseCountStr = obj.optString("oct_side15_house"),
                mainRoadName = obj.optString("oct_side15_road_name2"),
                localGovName = obj.optString("oct_side15_local3_name"),
                riverName = obj.optString("oct_side15_river_name"),
                damName = obj.optString("oct_side15_dam_name"),
                reservoirName = obj.optString("oct_side15_reservoir_name"),
                weirName = obj.optString("oct_side15_weir_name")
            )
        }
    }
}
