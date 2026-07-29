package com.example.data.repository

import com.example.data.local.BookingDao
import com.example.data.local.MatchDao
import com.example.data.local.UserDao
import com.example.data.local.TransactionDao
import com.example.data.local.SupportMessageDao
import com.example.data.local.AppConfigDao
import com.example.data.model.Booking
import com.example.data.model.Match
import com.example.data.model.User
import com.example.data.model.WalletTransaction
import com.example.data.model.SupportMessage
import com.example.data.model.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class TournamentRepository(
    private val userDao: UserDao,
    private val matchDao: MatchDao,
    private val bookingDao: BookingDao,
    private val transactionDao: TransactionDao,
    private val supportMessageDao: SupportMessageDao,
    private val appConfigDao: AppConfigDao,
    private val firebaseRepository: FirebaseRepository = FirebaseRepository()
) {
    val allMatches: Flow<List<Match>> = matchDao.getAllMatches()
    
    val allTransactions: Flow<List<WalletTransaction>> = transactionDao.getAllTransactions()
    fun getTransactionsForUser(userId: String): Flow<List<WalletTransaction>> = transactionDao.getTransactionsForUser(userId)
    
    fun getSupportMessagesForUser(userId: String): Flow<List<SupportMessage>> = supportMessageDao.getSupportMessagesForUser(userId)
    val supportMessages: Flow<List<SupportMessage>> = supportMessageDao.getAllSupportMessages()
    val allSupportMessages: Flow<List<SupportMessage>> = supportMessageDao.getAllSupportMessages()

    fun observeAppConfig(key: String): Flow<AppConfig?> = appConfigDao.observeConfigByKey(key)

    suspend fun getAppConfig(key: String): AppConfig? = appConfigDao.getConfigByKey(key)

    suspend fun saveAppConfig(config: AppConfig) {
        appConfigDao.insertConfig(config)
    }

    suspend fun insertSupportMessage(message: SupportMessage): Long {
        return supportMessageDao.insertMessage(message)
    }

    suspend fun clearSupportMessages() {
        supportMessageDao.deleteAllMessages()
    }

    
    suspend fun getTransactionById(id: Int): WalletTransaction? {
        return transactionDao.getTransactionById(id)
    }
    
    suspend fun insertTransaction(transaction: WalletTransaction): Long {
        if (transaction.userId.isNotBlank()) {
            firebaseRepository.saveTransactionToFirestore(transaction)
        }
        return transactionDao.insertTransaction(transaction)
    }
    
    fun getUser(userId: String = "local_user"): Flow<User?> = userDao.getUserById(userId)
    
    fun getMatchById(matchId: Int): Flow<Match?> = matchDao.getMatchById(matchId)
    
    fun getBookingsForUser(userId: String = "local_user"): Flow<List<Booking>> = bookingDao.getBookingsForUser(userId)
    
    fun observeBookingForMatch(matchId: Int, userId: String = "local_user"): Flow<Booking?> = 
        bookingDao.observeBookingForUserInMatch(matchId, userId)

    suspend fun saveUserProfile(user: User) {
        userDao.insertOrUpdateUser(user)
        val docId = user.email.ifBlank { user.id }.trim().lowercase()
        if (docId.isNotBlank()) {
            firebaseRepository.saveUserProfileToFirestore(user.copy(id = docId, email = user.email.ifBlank { docId }))
        }
    }

    suspend fun createMatch(match: Match): Long {
        val existing = matchDao.getAllMatches().firstOrNull() ?: emptyList()
        val processed = applyFFMaxRules(match, existing)
        return matchDao.insertMatch(processed)
    }

    suspend fun updateMatch(match: Match) {
        val existing = matchDao.getAllMatches().firstOrNull() ?: emptyList()
        val processed = applyFFMaxRules(match, existing)
        matchDao.updateMatch(processed)
    }

    suspend fun deleteMatch(matchId: Int) {
        matchDao.deleteMatch(matchId)
    }

    /**
     * Book a slot for a match. Deducts coins from user balance, creates the booking,
     * and increments the match's booked slots.
     */
    suspend fun bookSlot(booking: Booking, entryFee: Int): BookingResult {
        val userFlow = userDao.getUserById(booking.userId)
        val user = userFlow.firstOrNull() ?: User(id = booking.userId)
        
        if (user.coinBalance < entryFee) {
            return BookingResult.Error("Insufficient Coin Balance. Please Deposit Coins First.")
        }

        // Verify slot availability
        val matchFlow = matchDao.getMatchById(booking.matchId)
        val match = matchFlow.firstOrNull() ?: return BookingResult.Error("Match not found.")
        
        if (match.slotsBooked >= match.slotsTotal) {
            return BookingResult.Error("Match is fully booked! No slots available.")
        }

        // Check if already registered
        val existingBooking = bookingDao.getBookingForUserInMatch(booking.matchId, booking.userId)
        if (existingBooking != null) {
            return BookingResult.Error("You are already registered for this tournament!")
        }

        // Perform transactional operations
        val newBalance = user.coinBalance - entryFee
        val updatedUser = user.copy(coinBalance = newBalance)
        userDao.updateCoinBalance(booking.userId, newBalance)
        if (booking.userId.isNotBlank()) {
            firebaseRepository.saveUserProfileToFirestore(updatedUser)
        }
        
        bookingDao.insertBooking(booking)
        firebaseRepository.saveBookingToFirestore(booking)
        
        val updatedMatch = match.copy(slotsBooked = match.slotsBooked + 1)
        matchDao.updateMatch(updatedMatch)

        // Insert wallet transaction for registration entry fee
        val txnRef = "TXN" + (100000..999999).random()
        val entryFeeTxn = WalletTransaction(
            userId = booking.userId,
            type = "MATCH_ENTRY",
            amount = entryFee,
            title = "Joined Match #${match.id} - ${match.title}",
            paymentMethod = "Match Entry Fee",
            accountDetail = "Match #${match.id}: ${match.title}",
            status = "SUCCESS",
            transactionRef = txnRef,
            timestamp = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(entryFeeTxn)
        firebaseRepository.saveTransactionToFirestore(entryFeeTxn)
        
        return BookingResult.Success("Successfully booked slot in '${match.title}'!")
    }

    /**
     * Cancel slot booking. Refunds coins, deletes booking, and decrements booked slots.
     */
    suspend fun cancelBooking(matchId: Int, userId: String = "local_user", entryFee: Int): BookingResult {
        val booking = bookingDao.getBookingForUserInMatch(matchId, userId) ?: return BookingResult.Error("Registration not found.")
        
        val userFlow = userDao.getUserById(userId)
        val user = userFlow.firstOrNull() ?: return BookingResult.Error("User not found.")

        val matchFlow = matchDao.getMatchById(matchId)
        val match = matchFlow.firstOrNull() ?: return BookingResult.Error("Match not found.")

        // Perform cancellation
        bookingDao.cancelBooking(matchId, userId)
        
        val newBalance = user.coinBalance + entryFee
        val updatedUser = user.copy(coinBalance = newBalance)
        userDao.updateCoinBalance(userId, newBalance)
        if (userId.isNotBlank()) {
            firebaseRepository.saveUserProfileToFirestore(updatedUser)
        }

        val updatedMatch = match.copy(slotsBooked = (match.slotsBooked - 1).coerceAtLeast(0))
        matchDao.updateMatch(updatedMatch)

        // Insert wallet transaction for refund
        val txnRef = "REF" + (100000..999999).random()
        val refundTxn = WalletTransaction(
            userId = userId,
            type = "DEPOSIT",
            amount = entryFee,
            paymentMethod = "Match Entry Fee Refund",
            accountDetail = "Match #${match.id}: ${match.title}",
            status = "SUCCESS",
            transactionRef = txnRef,
            timestamp = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(refundTxn)
        firebaseRepository.saveTransactionToFirestore(refundTxn)

        return BookingResult.Success("Registration cancelled and $entryFee coins refunded!")
    }

    suspend fun updateBooking(booking: Booking) {
        bookingDao.insertBooking(booking)
        firebaseRepository.saveBookingToFirestore(booking)
    }

    /**
     * Populate some default matches if the database is currently empty.
     */
    suspend fun prepopulateDataIfEmpty() {
        val freshResetConfig = appConfigDao.getConfigByKey("db_v13_fresh_reset")
        if (freshResetConfig == null || freshResetConfig.value != "true") {
            matchDao.deleteAllMatches()
            bookingDao.deleteAllBookings()
            transactionDao.deleteAllTransactions()
            supportMessageDao.deleteAllMessages()
            appConfigDao.insertConfig(AppConfig("db_v13_fresh_reset", "true"))
        }

        val currentMatches = matchDao.getAllMatches().firstOrNull()
        val now = System.currentTimeMillis()
        
        // Recreate if no matches exist or if matches are older than 24 hours
        val shouldRecreate = currentMatches.isNullOrEmpty() || currentMatches.all { now - it.dateTimeMillis > 24L * 3600L * 1000L }
        
        if (shouldRecreate) {
            // Delete old matches if any to start clean
            if (!currentMatches.isNullOrEmpty()) {
                currentMatches.forEach { matchDao.deleteMatch(it.id) }
            }

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            
            // Base time is start of current hour
            val baseCal = Calendar.getInstance().apply {
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val baseTime = baseCal.timeInMillis

            val newMatches = mutableListOf<Match>()

            // 1. Past Match (Started 2 hours ago)
            val pastIdx = 1
            val (pastFee, pastKill) = getPricingForIndex(pastIdx)
            newMatches.add(
                Match(
                    title = String.format(Locale.US, "FF MAX #%02d", pastIdx),
                    gameMode = "Solo",
                    map = "Bermuda",
                    dateTimeMillis = baseTime - 2L * 3600L * 1000L,
                    prizePool = 1000,
                    entryFee = pastFee,
                    perKillPrize = pastKill,
                    status = "PAST",
                    slotsTotal = 48,
                    slotsBooked = 0,
                    roomId = "102849",
                    roomPassword = "done",
                    winner1Name = null,
                    winner2Name = null,
                    winner3Name = null
                )
            )

            // 2. Live Match (Started 15 mins ago)
            val liveIdx = 2
            val (liveFee, liveKill) = getPricingForIndex(liveIdx)
            newMatches.add(
                Match(
                    title = String.format(Locale.US, "FF MAX #%02d", liveIdx),
                    gameMode = "Solo",
                    map = "Bermuda",
                    dateTimeMillis = baseTime - 15 * 60 * 1000, // started 15 mins ago
                    prizePool = 1000,
                    entryFee = liveFee,
                    perKillPrize = liveKill,
                    status = "LIVE",
                    slotsTotal = 48,
                    slotsBooked = 0,
                    roomId = "8120395",
                    roomPassword = "byh"
                )
            )

            // 3. Upcoming matches scheduled for every single hour for the next 12 hours!
            for (i in 3..14) {
                val hourOffset = i - 2
                val (fee, kill) = getPricingForIndex(i)
                
                newMatches.add(
                    Match(
                        title = String.format(Locale.US, "FF MAX #%02d", i),
                        gameMode = "Solo",
                        map = "Bermuda",
                        dateTimeMillis = baseTime + hourOffset * 3600L * 1000L, // exact hourly schedule!
                        prizePool = 1000,
                        entryFee = fee,
                        perKillPrize = kill,
                        status = "UPCOMING",
                        slotsTotal = 48,
                        slotsBooked = 0
                    )
                )
            }

            matchDao.insertMatches(newMatches)

            // Seed default app configs
            appConfigDao.insertConfig(
                AppConfig(
                    key = "latest_version_code",
                    value = "1"
                )
            )
        } else {
            // If matches exist but the upcoming schedule is sparse, let's insert additional upcoming matches so there is always a full 12-hour queue starting from the max found time!
            val maxMatchTime = currentMatches.maxOf { it.dateTimeMillis }
            if (maxMatchTime < now + 12L * 3600L * 1000L) {
                // Determine how many matches we need to add to cover at least the next 12 hours
                val startHourCal = Calendar.getInstance().apply {
                    timeInMillis = maxMatchTime
                    add(Calendar.HOUR_OF_DAY, 1)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                var currentMaxIndex = currentMatches.mapNotNull { m ->
                    val mRes = Regex("#(\\d+)").find(m.title)
                    if (mRes != null && (m.title.uppercase(Locale.US).contains("FF MAX") || m.title.uppercase(Locale.US).contains("FF_MAX"))) {
                        mRes.groupValues[1].toIntOrNull()
                    } else {
                        null
                    }
                }.maxOrNull() ?: 14

                val addMatches = mutableListOf<Match>()
                for (i in 1..12) {
                    val nextTime = startHourCal.timeInMillis
                    if (nextTime > now + 24L * 3600L * 1000L) break // Don't schedule more than 24h ahead
                    
                    currentMaxIndex++
                    val (fee, kill) = getPricingForIndex(currentMaxIndex)
                    
                    addMatches.add(
                        Match(
                            title = String.format(Locale.US, "FF MAX #%02d", currentMaxIndex),
                            gameMode = "Solo",
                            map = "Bermuda",
                            dateTimeMillis = nextTime,
                            prizePool = 1000,
                            entryFee = fee,
                            perKillPrize = kill,
                            status = "UPCOMING",
                            slotsTotal = 48,
                            slotsBooked = 0
                        )
                    )
                    startHourCal.add(Calendar.HOUR_OF_DAY, 1)
                }
                if (addMatches.isNotEmpty()) {
                    matchDao.insertMatches(addMatches)
                }
            }
        }
    }

    private var timeOffset: Long = 0L
    private var lastSyncTime: Long = 0L

    private suspend fun getNetworkTime(): Long {
        return withContext(Dispatchers.IO) {
            var connection: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL("https://www.google.com")
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.connect()
                val dateHeader = connection.getHeaderField("Date")
                if (dateHeader != null) {
                    val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                    val parsedDate = format.parse(dateHeader)
                    if (parsedDate != null) {
                        return@withContext parsedDate.time
                    }
                }
            } catch (e: Exception) {
                // Silently ignore network exception
            } finally {
                connection?.disconnect()
            }
            System.currentTimeMillis()
        }
    }

    suspend fun getSyncedTimeMillis(): Long {
        val now = System.currentTimeMillis()
        if (lastSyncTime == 0L) {
            val cachedOffsetStr = appConfigDao.getConfigByKey("time_offset_ms")?.value
            if (cachedOffsetStr != null) {
                timeOffset = cachedOffsetStr.toLongOrNull() ?: 0L
            }
            lastSyncTime = now
            @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val networkTime = getNetworkTime()
                    timeOffset = networkTime - System.currentTimeMillis()
                    appConfigDao.insertConfig(com.example.data.model.AppConfig("time_offset_ms", timeOffset.toString()))
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } else if (now - lastSyncTime > 300000L) {
            lastSyncTime = now
            @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val networkTime = getNetworkTime()
                    timeOffset = networkTime - System.currentTimeMillis()
                    appConfigDao.insertConfig(com.example.data.model.AppConfig("time_offset_ms", timeOffset.toString()))
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        return System.currentTimeMillis() + timeOffset
    }

    fun getSyncedTimeCached(): Long {
        return System.currentTimeMillis() + timeOffset
    }

    suspend fun checkAndGenerateDailyMatches() {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = tz
        }.format(Date(getSyncedTimeMillis()))

        val lastResetConfig = appConfigDao.getConfigByKey("last_daily_reset_date")
        val lastResetDate = lastResetConfig?.value ?: ""

        if (lastResetDate != todayStr) {
            appConfigDao.insertConfig(com.example.data.model.AppConfig("last_daily_reset_date", todayStr))

            val currentMatches = matchDao.getAllMatches().firstOrNull() ?: emptyList()

            val dailyTemplates = mutableListOf<DailyMatchTemplate>()
            
            for (hour in 0..23) {
                val matchNum = hour + 1
                val (entryFee, perKill) = getPricingForIndex(matchNum)
                
                dailyTemplates.add(
                    DailyMatchTemplate(
                        title = String.format(Locale.US, "FF MAX #%02d", matchNum),
                        gameMode = "Solo",
                        map = "Bermuda",
                        hour = hour,
                        minute = 0,
                        prizePool = 1000,
                        entryFee = entryFee,
                        perKillPrize = perKill,
                        slotsTotal = 48
                    )
                )
            }

            val todayDate = Date(getSyncedTimeMillis())
            val baseCal = Calendar.getInstance(tz).apply {
                time = todayDate
            }
            val year = baseCal.get(Calendar.YEAR)
            val month = baseCal.get(Calendar.MONTH)
            val day = baseCal.get(Calendar.DAY_OF_MONTH)

            for (tmpl in dailyTemplates) {
                val cal = Calendar.getInstance(tz).apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, tmpl.hour)
                    set(Calendar.MINUTE, tmpl.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val matchTimeMillis = cal.timeInMillis

                val existingMatch = currentMatches.find { 
                    it.title == tmpl.title || 
                    it.title.startsWith(tmpl.title.split(":")[0] + ":") 
                }

                if (existingMatch != null) {
                    // Clear existing bookings for this match
                    bookingDao.deleteBookingsForMatch(existingMatch.id)

                    val updatedMatch = existingMatch.copy(
                        title = tmpl.title,
                        gameMode = tmpl.gameMode,
                        map = tmpl.map,
                        dateTimeMillis = matchTimeMillis,
                        prizePool = tmpl.prizePool,
                        entryFee = tmpl.entryFee,
                        perKillPrize = tmpl.perKillPrize,
                        status = "UPCOMING",
                        slotsTotal = tmpl.slotsTotal,
                        slotsBooked = 0,
                        roomId = null,
                        roomPassword = null,
                        winner1Name = null,
                        winner2Name = null,
                        winner3Name = null
                    )
                    matchDao.updateMatch(updatedMatch)
                } else {
                    val newMatch = Match(
                        title = tmpl.title,
                        gameMode = tmpl.gameMode,
                        map = tmpl.map,
                        dateTimeMillis = matchTimeMillis,
                        prizePool = tmpl.prizePool,
                        entryFee = tmpl.entryFee,
                        perKillPrize = tmpl.perKillPrize,
                        status = "UPCOMING",
                        slotsTotal = tmpl.slotsTotal,
                        slotsBooked = 0
                    )
                    matchDao.insertMatch(newMatch)
                }
            }
        }
    }

    suspend fun forceGenerateDailyMatches() {
        appConfigDao.insertConfig(com.example.data.model.AppConfig("last_daily_reset_date", ""))
        checkAndGenerateDailyMatches()
    }

    fun applyFFMaxRules(match: Match, existingMatches: List<Match> = emptyList()): Match {
        val titleUpper = match.title.uppercase(Locale.US)
        if (titleUpper.contains("FF MAX") || titleUpper.contains("FF_MAX")) {
            // Find if there's already an index in the title like "#05" or "#5"
            val numberRegex = Regex("#(\\d+)")
            val matchResult = numberRegex.find(match.title)
            val index = if (matchResult != null) {
                matchResult.groupValues[1].toIntOrNull() ?: 1
            } else {
                // Determine the next sequential index based on existing matches
                val maxIndex = existingMatches.mapNotNull { m ->
                    val mRes = numberRegex.find(m.title)
                    if (mRes != null && (m.title.uppercase(Locale.US).contains("FF MAX") || m.title.uppercase(Locale.US).contains("FF_MAX"))) {
                        mRes.groupValues[1].toIntOrNull()
                    } else {
                        null
                    }
                }.maxOrNull() ?: 0
                maxIndex + 1
            }

            val newTitle = String.format(Locale.US, "FF MAX #%02d", index)
            val (entryFee, perKill) = getPricingForIndex(index)
            return match.copy(
                title = newTitle,
                gameMode = "Solo",
                map = "Bermuda",
                prizePool = 1000,
                entryFee = entryFee,
                perKillPrize = perKill,
                slotsTotal = 48
            )
        }
        return match
    }

    private fun getPricingForIndex(index: Int): Pair<Int, Int> {
        return when (index) {
            1 -> Pair(2, 1)
            2 -> Pair(3, 2)
            3 -> Pair(4, 3)
            4 -> Pair(5, 4)
            5 -> Pair(6, 5)
            6 -> Pair(7, 5)
            7 -> Pair(8, 6)
            8 -> Pair(9, 7)
            9 -> Pair(10, 8)
            10 -> Pair(11, 9)
            11 -> Pair(12, 10)
            12 -> Pair(13, 11)
            13 -> Pair(14, 12)
            else -> Pair(15, 13)
        }
    }
}

private data class DailyMatchTemplate(
    val title: String,
    val gameMode: String,
    val map: String,
    val hour: Int,
    val minute: Int,
    val prizePool: Int,
    val entryFee: Int,
    val perKillPrize: Int,
    val slotsTotal: Int
)

sealed class BookingResult {
    data class Success(val message: String) : BookingResult()
    data class Error(val message: String) : BookingResult()
}
