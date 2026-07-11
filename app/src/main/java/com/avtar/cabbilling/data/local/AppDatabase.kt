package com.avtar.cabbilling.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.avtar.cabbilling.data.local.entity.BillEntity
import com.avtar.cabbilling.data.local.entity.ConfigEntity
import com.avtar.cabbilling.data.local.entity.TripEntity

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE config ADD COLUMN companyPhone TEXT")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bills ADD COLUMN carPlate TEXT")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS trips (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER NOT NULL DEFAULT 0,
                startAddress TEXT NOT NULL DEFAULT '',
                endAddress TEXT NOT NULL DEFAULT '',
                distanceKm REAL NOT NULL DEFAULT 0.0,
                maxSpeedKmh REAL NOT NULL DEFAULT 0.0,
                avgSpeedKmh REAL NOT NULL DEFAULT 0.0,
                isActive INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trips ADD COLUMN path TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [BillEntity::class, ConfigEntity::class, TripEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun billDao(): BillDao
    abstract fun configDao(): ConfigDao
    abstract fun tripDao(): TripDao

    companion object {
        private const val DB_NAME = "cab_billing.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
    }
}
