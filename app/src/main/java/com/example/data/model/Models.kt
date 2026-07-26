package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "local_user",
    val gameUid: String = "",
    val gameName: String = "",
    val email: String = "",
    val avatar: String = "ic_avatar_1",
    val coinBalance: Int = 0, // Initial balance 0 coins for new registrations
    val username: String = "",
    val phone: String = "",
    val promoCode: String = "",
    val referralCode: String = "",
    val referredByCode: String = "",
    val totalEarnedReferrals: Int = 0,
    val joinedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "matches")
data class Match(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val gameMode: String, // "Solo" or "Squad" or "Duo"
    val map: String, // "Bermuda", "Purgatory", "Kalahari"
    val dateTimeMillis: Long, // timestamp
    val prizePool: Int, // e.g. 1500
    val entryFee: Int, // e.g. 100
    val perKillPrize: Int = 0, // e.g. 5
    val status: String, // "UPCOMING", "LIVE", "PAST"
    val slotsTotal: Int, // e.g. 48 for Solo, 12 for Squad teams
    val slotsBooked: Int,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val winner1Name: String? = null,
    val winner2Name: String? = null,
    val winner3Name: String? = null,
    val category: String = "FF MAX"
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val userId: String = "local_user",
    val bookingType: String, // "Solo" or "Squad"
    val teamName: String? = null,
    val player1Name: String,
    val player1Uid: String,
    val player2Name: String? = null,
    val player2Uid: String? = null,
    val player3Name: String? = null,
    val player3Uid: String? = null,
    val player4Name: String? = null,
    val player4Uid: String? = null,
    val entryFeePaid: Int = 0,
    val bookedAtMillis: Long = System.currentTimeMillis(),
    val screenshotUri: String? = null
)

@Entity(tableName = "transactions")
data class WalletTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "local_user",
    val type: String = "DEPOSIT", // "DEPOSIT", "WITHDRAWAL", "MATCH_ENTRY", "MATCH_WINNING"
    val amount: Int = 0,
    val title: String = "", // e.g. "Joined Match #24", "Admin Approved Deposit", "Won Lone Wolf Match"
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String = "", // e.g. "UPI (Paytm/GPay)", "Card", "NetBanking", "UPI Cashout", "Bank Transfer"
    val accountDetail: String = "", // UPI ID / Phone / Bank Account Number
    val status: String = "SUCCESS", // "SUCCESS", "PENDING", "FAILED"
    val transactionRef: String = "" // unique simulated transaction reference, e.g. "TXN8491038"
) {
    val displayTitle: String
        get() = title.ifBlank {
            when (type) {
                "DEPOSIT" -> if (paymentMethod.isNotBlank()) "Deposit ($paymentMethod)" else "Coin Deposit"
                "WITHDRAWAL" -> if (paymentMethod.isNotBlank()) "Withdrawal ($paymentMethod)" else "Coin Withdrawal"
                "MATCH_ENTRY" -> "Match Entry Fee"
                "MATCH_WINNING" -> "Match Prize Winnings"
                else -> if (paymentMethod.isNotBlank()) paymentMethod else "Transaction"
            }
        }
}

@Entity(tableName = "support_messages")
data class SupportMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "local_user",
    val senderId: String, // "user" or "admin" or "system_bot"
    val senderName: String, // e.g. "You" or "Support Agent"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val key: String,
    val value: String
)

data class AppUpdateInfo(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val minSupportedVersionCode: Int = 1,
    val apkUrl: String = "https://github.com/kasde381-arch/bresports/releases/download/v1.0.0/app-release.apk",
    val releaseNotes: String = "• Critical tournament lobby stability fixes\n• Instant wallet deposit & coin sync improvements",
    val isForceUpdate: Boolean = false,
    val checkStatus: String = "IDLE", // "IDLE", "CHECKING", "SUCCESS", "ERROR"
    val errorMessage: String? = null
)

