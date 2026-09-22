package com.example.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let { 
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromGender(value: Gender): String = value.name

    @TypeConverter
    fun toGender(value: String): Gender = try { Gender.valueOf(value) } catch (e: Exception) { Gender.UNKNOWN }

    @TypeConverter
    fun fromHouseholdRole(value: HouseholdRole): String = value.name

    @TypeConverter
    fun toHouseholdRole(value: String): HouseholdRole = try { HouseholdRole.valueOf(value) } catch (e: Exception) { HouseholdRole.UNKNOWN }

    @TypeConverter
    fun fromPersonStatus(value: PersonStatus): String = value.name

    @TypeConverter
    fun toPersonStatus(value: String): PersonStatus = try { PersonStatus.valueOf(value) } catch (e: Exception) { PersonStatus.UNKNOWN }

    @TypeConverter
    fun fromDataStatus(value: DataStatus): String = value.name

    @TypeConverter
    fun toDataStatus(value: String): DataStatus = try { DataStatus.valueOf(value) } catch (e: Exception) { DataStatus.UNKNOWN }
}
