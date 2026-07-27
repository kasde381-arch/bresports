package com.example.data.repository

import com.example.data.model.Booking
import com.example.data.model.User
import com.example.data.model.WalletTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    val isFirebaseAvailable: Boolean
        get() = auth != null && db != null

    fun getCurrentUserEmail(): String? {
        return try {
            auth?.currentUser?.email
        } catch (e: Exception) {
            null
        }
    }

    suspend fun signUpWithFirebase(
        email: String,
        password: String,
        userProfile: User
    ): Result<User> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized. Check google-services.json."))
        val firestore = db ?: return Result.failure(Exception("Firestore not initialized."))

        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            val docId = email.trim().lowercase()

            val finalUser = userProfile.copy(
                id = docId,
                email = email.trim().lowercase()
            )

            // Save user profile to Cloud Firestore
            saveUserProfileToFirestore(finalUser)

            Result.success(finalUser)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun signInWithFirebase(
        email: String,
        password: String
    ): Result<User> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized. Check google-services.json."))

        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val docId = email.trim().lowercase()

            // Restore user profile directly from Firestore Database
            val userFromDb = fetchUserProfileFromFirestore(docId)
            if (userFromDb != null) {
                Result.success(userFromDb)
            } else {
                // If user auth exists but document was not found, construct default profile
                val fallbackUser = User(
                    id = docId,
                    email = docId,
                    username = docId.substringBefore("@"),
                    coinBalance = 0
                )
                saveUserProfileToFirestore(fallbackUser)
                Result.success(fallbackUser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized."))
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserProfileToFirestore(user: User) {
        val firestore = db ?: return
        try {
            val docId = user.email.ifBlank { user.id }.trim().lowercase()
            if (docId.isBlank()) return

            val userMap = hashMapOf(
                "id" to docId,
                "email" to user.email,
                "username" to user.username,
                "gameUid" to user.gameUid,
                "gameName" to user.gameName,
                "phone" to user.phone,
                "coinBalance" to user.coinBalance,
                "promoCode" to user.promoCode,
                "referralCode" to user.referralCode,
                "referredByCode" to user.referredByCode,
                "totalEarnedReferrals" to user.totalEarnedReferrals,
                "joinedAtMillis" to user.joinedAtMillis,
                "avatar" to user.avatar
            )

            firestore.collection("users")
                .document(docId)
                .set(userMap, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchUserProfileFromFirestore(emailOrId: String): User? {
        val firestore = db ?: return null
        val docId = emailOrId.trim().lowercase()
        if (docId.isBlank()) return null

        return try {
            val snapshot = firestore.collection("users").document(docId).get().await()
            if (snapshot.exists()) {
                val data = snapshot.data ?: return null
                User(
                    id = docId,
                    email = data["email"] as? String ?: docId,
                    username = data["username"] as? String ?: "",
                    gameUid = data["gameUid"] as? String ?: "",
                    gameName = data["gameName"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    coinBalance = (data["coinBalance"] as? Long)?.toInt() ?: (data["coinBalance"] as? Int) ?: 0,
                    promoCode = data["promoCode"] as? String ?: "",
                    referralCode = data["referralCode"] as? String ?: "",
                    referredByCode = data["referredByCode"] as? String ?: "",
                    totalEarnedReferrals = (data["totalEarnedReferrals"] as? Long)?.toInt() ?: 0,
                    joinedAtMillis = (data["joinedAtMillis"] as? Long) ?: System.currentTimeMillis(),
                    avatar = data["avatar"] as? String ?: "ic_avatar_1"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveBookingToFirestore(booking: Booking) {
        val firestore = db ?: return
        try {
            val bookingMap = hashMapOf(
                "matchId" to booking.matchId,
                "userId" to booking.userId,
                "bookingType" to booking.bookingType,
                "teamName" to (booking.teamName ?: ""),
                "player1Name" to booking.player1Name,
                "player1Uid" to booking.player1Uid,
                "player2Name" to (booking.player2Name ?: ""),
                "player2Uid" to (booking.player2Uid ?: ""),
                "player3Name" to (booking.player3Name ?: ""),
                "player3Uid" to (booking.player3Uid ?: ""),
                "player4Name" to (booking.player4Name ?: ""),
                "player4Uid" to (booking.player4Uid ?: ""),
                "entryFeePaid" to booking.entryFeePaid,
                "bookedAtMillis" to booking.bookedAtMillis,
                "screenshotUri" to (booking.screenshotUri ?: "")
            )

            firestore.collection("bookings")
                .add(bookingMap)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchUserBookingsFromFirestore(userId: String): List<Booking> {
        val firestore = db ?: return emptyList()
        val docId = userId.trim().lowercase()
        if (docId.isBlank()) return emptyList()

        return try {
            val snapshot = firestore.collection("bookings")
                .whereEqualTo("userId", docId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val matchId = (doc.getLong("matchId"))?.toInt() ?: return@mapNotNull null
                Booking(
                    id = doc.id.hashCode(),
                    matchId = matchId,
                    userId = doc.getString("userId") ?: docId,
                    bookingType = doc.getString("bookingType") ?: "Solo",
                    teamName = doc.getString("teamName"),
                    player1Name = doc.getString("player1Name") ?: "",
                    player1Uid = doc.getString("player1Uid") ?: "",
                    player2Name = doc.getString("player2Name"),
                    player2Uid = doc.getString("player2Uid"),
                    player3Name = doc.getString("player3Name"),
                    player3Uid = doc.getString("player3Uid"),
                    player4Name = doc.getString("player4Name"),
                    player4Uid = doc.getString("player4Uid"),
                    entryFeePaid = (doc.getLong("entryFeePaid"))?.toInt() ?: 0,
                    bookedAtMillis = doc.getLong("bookedAtMillis") ?: System.currentTimeMillis(),
                    screenshotUri = doc.getString("screenshotUri")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveTransactionToFirestore(txn: WalletTransaction) {
        val firestore = db ?: return
        try {
            val txnMap = hashMapOf(
                "userId" to txn.userId,
                "type" to txn.type,
                "amount" to txn.amount,
                "title" to txn.title,
                "timestamp" to txn.timestamp,
                "paymentMethod" to txn.paymentMethod,
                "accountDetail" to txn.accountDetail,
                "status" to txn.status,
                "transactionRef" to txn.transactionRef
            )

            firestore.collection("transactions")
                .add(txnMap)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchUserTransactionsFromFirestore(userId: String): List<WalletTransaction> {
        val firestore = db ?: return emptyList()
        val docId = userId.trim().lowercase()
        if (docId.isBlank()) return emptyList()

        return try {
            val snapshot = firestore.collection("transactions")
                .whereEqualTo("userId", docId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                WalletTransaction(
                    id = doc.id.hashCode(),
                    userId = doc.getString("userId") ?: docId,
                    type = doc.getString("type") ?: "DEPOSIT",
                    amount = (doc.getLong("amount"))?.toInt() ?: 0,
                    title = doc.getString("title") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    paymentMethod = doc.getString("paymentMethod") ?: "",
                    accountDetail = doc.getString("accountDetail") ?: "",
                    status = doc.getString("status") ?: "SUCCESS",
                    transactionRef = doc.getString("transactionRef") ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchAllRegisteredUsersFromFirestore(): List<User> {
        val firestore = db ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val email = data["email"] as? String ?: doc.id
                User(
                    id = doc.id,
                    email = email,
                    username = data["username"] as? String ?: "",
                    gameUid = data["gameUid"] as? String ?: "",
                    gameName = data["gameName"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    coinBalance = (data["coinBalance"] as? Long)?.toInt() ?: 0,
                    promoCode = data["promoCode"] as? String ?: "",
                    referralCode = data["referralCode"] as? String ?: "",
                    referredByCode = data["referredByCode"] as? String ?: "",
                    totalEarnedReferrals = (data["totalEarnedReferrals"] as? Long)?.toInt() ?: 0,
                    joinedAtMillis = (data["joinedAtMillis"] as? Long) ?: System.currentTimeMillis(),
                    avatar = data["avatar"] as? String ?: "ic_avatar_1"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
