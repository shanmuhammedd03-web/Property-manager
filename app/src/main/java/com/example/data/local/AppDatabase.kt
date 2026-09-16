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
                .fallbackToDestructiveMigration(false)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try {
                            // Remove legacy default seed properties like 'Property A', 'Property B', 'Property C'
                            db.execSQL("DELETE FROM bookings WHERE propertyId IN (SELECT id FROM properties WHERE name IN ('Property A', 'Property B', 'Property C'))")
                            db.execSQL("DELETE FROM properties WHERE name IN ('Property A', 'Property B', 'Property C')")
                        } catch (_: Exception) {
                            // Non-fatal if cleanup fails
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
