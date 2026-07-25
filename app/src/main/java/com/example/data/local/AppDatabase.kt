package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Booking
import com.example.data.model.Match
import com.example.data.model.User
import com.example.data.model.WalletTransaction
import com.example.data.model.SupportMessage
import com.example.data.model.AppConfig

@Database(entities = [User::class, Match::class, Booking::class, WalletTransaction::class, SupportMessage::class, AppConfig::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun matchDao(): MatchDao
    abstract fun bookingDao(): BookingDao
    abstract fun transactionDao(): TransactionDao
    abstract fun supportMessageDao(): SupportMessageDao
    abstract fun appConfigDao(): AppConfigDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "booyah_arena_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
