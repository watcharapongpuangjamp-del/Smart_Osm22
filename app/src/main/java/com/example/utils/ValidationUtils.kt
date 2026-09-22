package com.example.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object ValidationUtils {
    
    // Returns a Pair of LocalDate? and Boolean indicating if it was a "year only" format
    fun parseThaiDate(dateString: String?): Pair<LocalDate?, Boolean> {
        if (dateString.isNullOrBlank()) return Pair(null, false)
        val str = dateString.trim()
        
        // Check if it's just a year (4 digits)
        if (str.length == 4 && str.all { it.isDigit() }) {
            val year = str.toInt()
            val finalYear = if (year > 2400) year - 543 else year
            return try {
                Pair(LocalDate.of(finalYear, 1, 1), true)
            } catch (e: Exception) {
                Pair(null, false)
            }
        }
        
        // Try parsing different formats
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "d/M/yyyy", "d-M-yyyy"
        )
        
        for (format in formats) {
            try {
                val formatter = DateTimeFormatter.ofPattern(format)
                val parsed = LocalDate.parse(str, formatter)
                
                // Convert Buddhist era to AD if year > 2400
                val finalYear = if (parsed.year > 2400) parsed.year - 543 else parsed.year
                val finalDate = if (parsed.year != finalYear) LocalDate.of(finalYear, parsed.monthValue, parsed.dayOfMonth) else parsed
                
                return Pair(finalDate, false)
            } catch (e: DateTimeParseException) {
                // Continue to next format
            } catch (e: Exception) {
                return Pair(null, false)
            }
        }
        
        return Pair(null, false)
    }

    fun formatThaiDateDisplay(date: LocalDate?, isBirthYearOnly: Boolean): String {
        if (date == null) return ""
        val thaiYear = date.year + 543
        return if (isBirthYearOnly) {
            "พ.ศ. $thaiYear"
        } else {
            String.format("%02d/%02d/%04d", date.dayOfMonth, date.monthValue, thaiYear)
        }
    }

    fun normalizeNationalId(id: String?): String {
        if (id == null) return ""
        // Remove spaces, dashes, and non-digit characters
        return id.replace(Regex("[^0-9]"), "")
    }

    fun isValidThaiNationalId(id: String): Boolean {
        val normalized = normalizeNationalId(id)
        if (normalized.length != 13) return false
        
        var sum = 0
        for (i in 0..11) {
            sum += normalized[i].digitToInt() * (13 - i)
        }
        val checkDigit = (11 - (sum % 11)) % 10
        return checkDigit == normalized[12].digitToInt()
    }

    fun isValidGpsCoordinates(lat: Double?, lon: Double?, accuracy: Float?): Boolean {
        if (lat == null || lon == null) return false
        if (lat < -90.0 || lat > 90.0) return false
        if (lon < -180.0 || lon > 180.0) return false
        if (lat == 0.0 && lon == 0.0) return false
        if (accuracy != null && accuracy <= 0f) return false
        return true
    }

    fun checkNationalIdStatus(id: String?, isUnverified: Boolean = false): NationalIdStatus {
        if (id == null || id.isBlank()) return NationalIdStatus.MISSING
        val normalized = normalizeNationalId(id)
        if (normalized.isBlank()) return NationalIdStatus.MISSING
        if (isUnverified) return NationalIdStatus.UNVERIFIED
        return if (isValidThaiNationalId(normalized)) NationalIdStatus.VALID else NationalIdStatus.INVALID
    }
}

enum class NationalIdStatus {
    VALID,
    INVALID,
    MISSING,
    UNVERIFIED
}
