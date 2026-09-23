package com.example.data.village

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository to load and query Nakhon Nayok Province Village Baseline GIS dataset.
 */
class VillageBaselineRepository(private val context: Context) {
    companion object {
        private const val TAG = "VillageBaselineRepo"
        private const val ASSET_FILE = "villages_nakhonnayok.json"
    }

    @Volatile
    private var cachedVillages: List<VillageBaseline>? = null

    suspend fun getAllVillages(): List<VillageBaseline> = withContext(Dispatchers.IO) {
        cachedVillages?.let { return@withContext it }

        try {
            val jsonString = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<VillageBaseline>>() {}.type
            val list: List<VillageBaseline> = Gson().fromJson(jsonString, listType) ?: emptyList()
            cachedVillages = list
            Log.i(TAG, "Loaded ${list.size} village baseline GIS records for Nakhon Nayok")
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading $ASSET_FILE", e)
            emptyList()
        }
    }

    suspend fun getVillagesBySubdistrict(subdistrictName: String): List<VillageBaseline> {
        val all = getAllVillages()
        if (subdistrictName.isBlank() || subdistrictName.equals("ALL", ignoreCase = true)) return all
        val cleanName = subdistrictName.replace("ต.", "").replace("ตำบล", "").trim()
        return all.filter { it.subdistrictName.contains(cleanName, ignoreCase = true) }
    }

    suspend fun searchVillages(query: String): List<VillageBaseline> {
        val all = getAllVillages()
        if (query.isBlank()) return all
        return all.filter {
            it.villageName.contains(query, ignoreCase = true) ||
                    it.subdistrictName.contains(query, ignoreCase = true) ||
                    it.districtName.contains(query, ignoreCase = true) ||
                    it.riverName.contains(query, ignoreCase = true) ||
                    it.damName.contains(query, ignoreCase = true) ||
                    it.reservoirName.contains(query, ignoreCase = true)
        }
    }
}
