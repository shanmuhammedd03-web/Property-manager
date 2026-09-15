package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Booking
import com.example.data.model.Property

@Database(entities = [Property::class, Booking::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun bookingDao(): BookingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "property_booking.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        try {
                            val now = System.currentTimeMillis()
                            db.execSQL("INSERT OR IGNORE INTO properties (id, name, note, createdAt) VALUES (1, 'Property A', 'Standard Suite', $now)")
                            db.execSQL("INSERT OR IGNORE INTO properties (id, name, note, createdAt) VALUES (2, 'Property B', 'Deluxe Villa', $now)")
                            db.execSQL("INSERT OR IGNORE INTO properties (id, name, note, createdAt) VALUES (3, 'Property C', 'Executive Studio', $now)")
                        } catch (_: Exception) {
                            // Non-fatal if initial seed fails
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
