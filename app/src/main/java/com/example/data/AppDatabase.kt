package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Person::class, Household::class, PersonHistory::class, PopulationEvent::class, HealthScreening::class], version = 10, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun householdDao(): HouseholdDao
    abstract fun personHistoryDao(): PersonHistoryDao
    abstract fun populationEventDao(): PopulationEventDao
    abstract fun healthScreeningDao(): HealthScreeningDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) {} }
        val MIGRATION_2_3 = object : Migration(2, 3) { override fun migrate(db: SupportSQLiteDatabase) {} }
        val MIGRATION_3_4 = object : Migration(3, 4) { override fun migrate(db: SupportSQLiteDatabase) {} }
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Household: Add columns
                db.execSQL("ALTER TABLE households ADD COLUMN villageNo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE households ADD COLUMN subdistrict TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE households ADD COLUMN district TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE households ADD COLUMN province TEXT NOT NULL DEFAULT ''")
                
                // Recreate Person table to enforce new schema
                db.execSQL("CREATE TABLE IF NOT EXISTS `persons_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `householdId` INTEGER NOT NULL, `nationalId` TEXT NOT NULL, `fullName` TEXT NOT NULL, `gender` TEXT NOT NULL, `birthDate` TEXT, `houseStatus` TEXT NOT NULL, `personStatus` TEXT NOT NULL, `dataStatus` TEXT NOT NULL, FOREIGN KEY(`householdId`) REFERENCES `households`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("INSERT INTO `persons_new` (`id`, `householdId`, `nationalId`, `fullName`, `gender`, `birthDate`, `houseStatus`, `personStatus`, `dataStatus`) SELECT `id`, `householdId`, `nationalId`, `fullName`, `gender`, `birthDate`, `houseStatus`, `personStatus`, `dataStatus` FROM `persons`")
                db.execSQL("DROP TABLE `persons`")
                db.execSQL("ALTER TABLE `persons_new` RENAME TO `persons`")
                
                // Create unique indices
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_households_houseNo_villageNo_subdistrict_district_province` ON `households` (`houseNo`, `villageNo`, `subdistrict`, `district`, `province`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_persons_householdId` ON `persons` (`householdId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_persons_nationalId` ON `persons` (`nationalId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add householdUuid and drop the old index
                db.execSQL("ALTER TABLE households ADD COLUMN householdUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("DROP INDEX IF EXISTS `index_households_houseNo_villageNo_subdistrict_district_province`")
                db.execSQL("UPDATE households SET householdUuid = lower(hex(randomblob(16)))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_households_householdUuid` ON `households` (`householdUuid`)")

                // 2. Add new columns to persons (personUuid, isBirthYearOnly)
                db.execSQL("ALTER TABLE persons ADD COLUMN personUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE persons SET personUuid = lower(hex(randomblob(16)))")
                db.execSQL("ALTER TABLE persons ADD COLUMN isBirthYearOnly INTEGER NOT NULL DEFAULT 0")

                // 3. Recreate person_history for the new columns
                db.execSQL("CREATE TABLE IF NOT EXISTS `person_history_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `personId` INTEGER NOT NULL, `action` TEXT NOT NULL, `oldValue` TEXT, `newValue` TEXT, `timestamp` INTEGER NOT NULL, `operatorId` TEXT NOT NULL, `operatorName` TEXT NOT NULL, `role` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `source` TEXT NOT NULL)")
                db.execSQL("INSERT INTO `person_history_new` (`id`, `personId`, `action`, `oldValue`, `newValue`, `timestamp`, `operatorId`, `operatorName`, `role`, `deviceId`, `source`) SELECT `id`, `personId`, `action`, `oldValue`, `newValue`, `timestamp`, 'SYSTEM', 'System', 'SYSTEM', 'local', 'SYSTEM' FROM `person_history`")
                db.execSQL("DROP TABLE `person_history`")
                db.execSQL("ALTER TABLE `person_history_new` RENAME TO `person_history`")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Make nationalId nullable and add personUuid unique index
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `persons_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `personUuid` TEXT NOT NULL DEFAULT '',
                        `householdId` INTEGER NOT NULL,
                        `nationalId` TEXT,
                        `fullName` TEXT NOT NULL,
                        `gender` TEXT NOT NULL,
                        `birthDate` TEXT,
                        `isBirthYearOnly` INTEGER NOT NULL DEFAULT 0,
                        `houseStatus` TEXT NOT NULL,
                        `personStatus` TEXT NOT NULL,
                        `dataStatus` TEXT NOT NULL,
                        FOREIGN KEY(`householdId`) REFERENCES `households`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("""
                    INSERT INTO `persons_new` (`id`, `personUuid`, `householdId`, `nationalId`, `fullName`, `gender`, `birthDate`, `isBirthYearOnly`, `houseStatus`, `personStatus`, `dataStatus`)
                    SELECT `id`,
                           CASE WHEN `personUuid` IS NULL OR `personUuid` = '' THEN lower(hex(randomblob(16))) ELSE `personUuid` END,
                           `householdId`,
                           CASE WHEN `nationalId` = '' THEN NULL ELSE `nationalId` END,
                           `fullName`, `gender`, `birthDate`, `isBirthYearOnly`, `houseStatus`, `personStatus`, `dataStatus`
                    FROM `persons`
                """)
                db.execSQL("DROP TABLE `persons`")
                db.execSQL("ALTER TABLE `persons_new` RENAME TO `persons`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_persons_householdId` ON `persons` (`householdId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_persons_nationalId` ON `persons` (`nationalId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_persons_personUuid` ON `persons` (`personUuid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_households_householdUuid` ON `households` (`householdUuid`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val currentTime = System.currentTimeMillis()
                db.execSQL("ALTER TABLE persons ADD COLUMN lastModified INTEGER NOT NULL DEFAULT $currentTime")
                db.execSQL("ALTER TABLE households ADD COLUMN lastModified INTEGER NOT NULL DEFAULT $currentTime")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `population_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `householdId` INTEGER,
                        `personId` INTEGER,
                        `personName` TEXT,
                        FOREIGN KEY(`householdId`) REFERENCES `households`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_population_events_householdId` ON `population_events` (`householdId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_population_events_personId` ON `population_events` (`personId`)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `health_screenings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `screeningUuid` TEXT NOT NULL,
                        `personId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `weight` REAL,
                        `height` REAL,
                        `bmi` REAL,
                        `systolic` INTEGER,
                        `diastolic` INTEGER,
                        `bloodSugar` INTEGER,
                        `pulse` INTEGER,
                        `temperature` REAL,
                        `oxygenSaturation` INTEGER,
                        `note` TEXT,
                        `vhvId` TEXT,
                        `vhvName` TEXT,
                        `dataStatus` TEXT NOT NULL,
                        FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_screenings_personId` ON `health_screenings` (`personId`)")
            }
        }
    }
}
