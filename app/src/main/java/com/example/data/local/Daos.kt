package com.example.data.local

import androidx.room.*
import com.example.data.model.Booking
import com.example.data.model.Match
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: User)

    @Query("UPDATE users SET coinBalance = :newBalance WHERE id = :id")
    suspend fun updateCoinBalance(id: String, newBalance: Int)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY dateTimeMillis ASC")
    fun getAllMatches(): Flow<List<Match>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    fun getMatchById(matchId: Int): Flow<Match?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: Match): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<Match>)

    @Update
    suspend fun updateMatch(match: Match)

    @Query("UPDATE matches SET slotsBooked = slotsBooked + 1 WHERE id = :matchId")
    suspend fun incrementSlotsBooked(matchId: Int)

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatch(matchId: Int)

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY bookedAtMillis DESC")
    fun getBookingsForUser(userId: String): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE matchId = :matchId AND userId = :userId LIMIT 1")
    suspend fun getBookingForUserInMatch(matchId: Int, userId: String): Booking?

    @Query("SELECT * FROM bookings WHERE matchId = :matchId AND userId = :userId")
    fun observeBookingForUserInMatch(matchId: Int, userId: String): Flow<Booking?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Query("DELETE FROM bookings WHERE matchId = :matchId AND userId = :userId")
    suspend fun cancelBooking(matchId: Int, userId: String)

    @Query("DELETE FROM bookings WHERE matchId = :matchId")
    suspend fun deleteBookingsForMatch(matchId: Int)

    @Query("DELETE FROM bookings")
    suspend fun deleteAllBookings()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: String): Flow<List<com.example.data.model.WalletTransaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<com.example.data.model.WalletTransaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): com.example.data.model.WalletTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: com.example.data.model.WalletTransaction): Long

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}

@Dao
interface SupportMessageDao {
    @Query("SELECT * FROM support_messages WHERE userId = :userId ORDER BY timestamp ASC")
    fun getSupportMessagesForUser(userId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.model.SupportMessage>>

    @Query("SELECT * FROM support_messages ORDER BY timestamp ASC")
    fun getAllSupportMessages(): kotlinx.coroutines.flow.Flow<List<com.example.data.model.SupportMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: com.example.data.model.SupportMessage): Long

    @Query("DELETE FROM support_messages")
    suspend fun deleteAllMessages()
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE `key` = :key LIMIT 1")
    fun observeConfigByKey(key: String): Flow<com.example.data.model.AppConfig?>

    @Query("SELECT * FROM app_config WHERE `key` = :key LIMIT 1")
    suspend fun getConfigByKey(key: String): com.example.data.model.AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: com.example.data.model.AppConfig)
}

