package com.example.data.repository

import com.example.data.model.Booking
import com.example.data.model.DepositRequest
import com.example.data.model.WalletRequest
import com.example.data.model.User
import com.example.data.model.WalletTransaction
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.util.Log
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseRepository {

    private fun ensureFirebaseInitialized() {
        try {
            FirebaseApp.getInstance()
        } catch (e: Throwable) {
            // If default app is not initialized by google-services plugin content provider
            e.printStackTrace()
        }
    }

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

    suspend fun loginWithGoogleFromFirebaseUser(firebaseUser: FirebaseUser): Result<User> {
        return try {
            val email = (firebaseUser.email ?: "").trim().lowercase()
            val displayName = (firebaseUser.displayName ?: "").ifBlank { email.substringBefore("@") }
            val photoUrl = firebaseUser.photoUrl?.toString() ?: ""
            val uid = firebaseUser.uid

            if (email.isBlank()) {
                return Result.failure(Exception("Google Account email address is missing."))
            }

            val docId = email

            val existingUser = fetchUserProfileFromFirestore(docId)
            if (existingUser != null) {
                val updatedUser = existingUser.copy(
                    email = email,
                    gameName = if (existingUser.gameName.isBlank()) displayName else existingUser.gameName,
                    avatar = if (existingUser.avatar.isBlank() || existingUser.avatar == "ic_avatar_1") photoUrl.ifBlank { "ic_avatar_1" } else existingUser.avatar
                )
                saveUserProfileToFirestore(updatedUser)
                Result.success(updatedUser)
            } else {
                val cleanUsername = displayName.replace(" ", "_").lowercase().ifBlank { email.substringBefore("@") }
                val initialGameName = displayName.ifBlank { "Player_${email.substringBefore("@")}" }
                val initialGameUid = (10000000..99999999).random().toString()
                val referralCode = "BR-${cleanUsername.take(4).uppercase().filter { it.isLetter() }}-${(100..999).random()}"

                val freshUser = User(
                    id = docId,
                    email = email,
                    username = cleanUsername,
                    gameName = initialGameName,
                    gameUid = initialGameUid,
                    phone = "",
                    coinBalance = 0,
                    avatar = photoUrl.ifBlank { "ic_avatar_1" },
                    referralCode = referralCode,
                    joinedAtMillis = System.currentTimeMillis()
                )
                saveUserProfileToFirestore(freshUser)
                Result.success(freshUser)
            }
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error in loginWithGoogleFromFirebaseUser: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleIdToken(
        idToken: String,
        email: String = "",
        displayName: String = "",
        photoUrl: String = ""
    ): Result<User> {
        val firebaseAuth = auth
        if (firebaseAuth != null) {
            return try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    loginWithGoogleFromFirebaseUser(firebaseUser)
                } else {
                    loginWithGoogle(inputEmail = email, inputDisplayName = displayName, inputPhotoUrl = photoUrl)
                }
            } catch (e: Exception) {
                Log.w("FirebaseRepository", "signInWithCredential failed, using direct Google account info fallback", e)
                loginWithGoogle(inputEmail = email, inputDisplayName = displayName, inputPhotoUrl = photoUrl)
            }
        } else {
            return loginWithGoogle(inputEmail = email, inputDisplayName = displayName, inputPhotoUrl = photoUrl)
        }
    }

    suspend fun loginWithGoogle(
        inputEmail: String? = null,
        inputDisplayName: String? = null,
        inputPhotoUrl: String? = null
    ): Result<User> {
        return try {
            val firebaseUser = auth?.currentUser

            val email = (firebaseUser?.email ?: inputEmail ?: "").trim().lowercase()
            val displayName = (firebaseUser?.displayName ?: inputDisplayName ?: "").ifBlank { email.substringBefore("@") }
            val photoUrl = firebaseUser?.photoUrl?.toString() ?: inputPhotoUrl ?: ""

            if (email.isBlank()) {
                return Result.failure(Exception("Please select a valid Google account with an email address."))
            }

            val docId = email

            val existingUser = fetchUserProfileFromFirestore(docId)
            if (existingUser != null) {
                val updatedUser = existingUser.copy(
                    email = email,
                    gameName = if (existingUser.gameName.isBlank()) displayName else existingUser.gameName,
                    avatar = if (existingUser.avatar.isBlank() || existingUser.avatar == "ic_avatar_1") photoUrl.ifBlank { "ic_avatar_1" } else existingUser.avatar
                )
                saveUserProfileToFirestore(updatedUser)
                Result.success(updatedUser)
            } else {
                val cleanUsername = displayName.replace(" ", "_").lowercase().ifBlank { email.substringBefore("@") }
                val initialGameName = displayName.ifBlank { "Player_${email.substringBefore("@")}" }
                val initialGameUid = (10000000..99999999).random().toString()
                val referralCode = "BR-${cleanUsername.take(4).uppercase().filter { it.isLetter() }}-${(100..999).random()}"

                val freshUser = User(
                    id = docId,
                    email = email,
                    username = cleanUsername,
                    gameName = initialGameName,
                    gameUid = initialGameUid,
                    phone = "",
                    coinBalance = 0,
                    avatar = photoUrl.ifBlank { "ic_avatar_1" },
                    referralCode = referralCode,
                    joinedAtMillis = System.currentTimeMillis()
                )

                saveUserProfileToFirestore(freshUser)
                Result.success(freshUser)
            }
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error in loginWithGoogle: ${e.message}", e)
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
        val firestore = db ?: run {
            Log.e("FirestoreSync", "Firestore instance is null when saving user ${user.email}")
            return
        }
        try {
            val docId = user.email.ifBlank { user.id }.trim().lowercase()
            if (docId.isBlank()) return

            val effectiveCoins = user.coinBalance
            val role = if (docId == "kasde381@gmail.com") "admin" else "user"

            val userMap = hashMapOf<String, Any>(
                "id" to docId,
                "email" to user.email.ifBlank { docId },
                "username" to user.username,
                "gameUid" to user.gameUid,
                "gameName" to user.gameName,
                "phone" to user.phone,
                "coins" to effectiveCoins,
                "coinBalance" to effectiveCoins,
                "walletBalance" to effectiveCoins,
                "promoCode" to user.promoCode,
                "referralCode" to user.referralCode,
                "referredByCode" to user.referredByCode,
                "totalEarnedReferrals" to user.totalEarnedReferrals,
                "joinedAtMillis" to user.joinedAtMillis,
                "avatar" to user.avatar,
                "role" to role
            )

            Log.d("FirestoreSync", "Writing document users/$docId to Firestore: $userMap")

            val setTask = firestore.collection("users")
                .document(docId)
                .set(userMap, SetOptions.merge())

            setTask.addOnSuccessListener {
                Log.d("FirestoreSync", "SUCCESS: Document users/$docId saved successfully in Cloud Firestore!")
            }.addOnFailureListener { e ->
                Log.e("FirestoreSync", "FAILURE: Failed to write document users/$docId to Cloud Firestore: ${e.message}", e)
            }

            setTask.await()
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Exception writing user profile to Firestore: ${e.message}", e)
        }
    }

    fun observeUserProfileFromFirestore(emailOrId: String): Flow<User?> = callbackFlow {
        val firestore = db ?: run {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docId = emailOrId.trim().lowercase()
        if (docId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users").document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseRepository", "Listen failed for user profile $docId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: return@addSnapshotListener
                    val rawCoin = data["coinBalance"] ?: data["walletBalance"]
                    val coinVal = when (rawCoin) {
                        is Number -> rawCoin.toInt()
                        is String -> rawCoin.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val emailVal = data["email"] as? String ?: docId
                    val user = User(
                        id = docId,
                        email = emailVal,
                        username = data["username"] as? String ?: "",
                        gameUid = data["gameUid"] as? String ?: "",
                        gameName = data["gameName"] as? String ?: "",
                        phone = data["phone"] as? String ?: "",
                        coinBalance = coinVal,
                        promoCode = data["promoCode"] as? String ?: "",
                        referralCode = data["referralCode"] as? String ?: "",
                        referredByCode = data["referredByCode"] as? String ?: "",
                        totalEarnedReferrals = (data["totalEarnedReferrals"] as? Long)?.toInt() ?: 0,
                        joinedAtMillis = (data["joinedAtMillis"] as? Long) ?: System.currentTimeMillis(),
                        avatar = data["avatar"] as? String ?: "ic_avatar_1"
                    )
                    trySend(user)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun fetchUserProfileFromFirestore(emailOrId: String): User? {
        val firestore = db ?: return null
        val docId = emailOrId.trim().lowercase()
        if (docId.isBlank()) return null

        return try {
            val snapshot = firestore.collection("users").document(docId).get().await()
            if (snapshot.exists()) {
                val data = snapshot.data ?: return null
                val rawCoin = data["coinBalance"] ?: data["walletBalance"]
                val coinVal = when (rawCoin) {
                    is Number -> rawCoin.toInt()
                    is String -> rawCoin.toIntOrNull() ?: 0
                    else -> 0
                }
                User(
                    id = docId,
                    email = data["email"] as? String ?: docId,
                    username = data["username"] as? String ?: "",
                    gameUid = data["gameUid"] as? String ?: "",
                    gameName = data["gameName"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    coinBalance = coinVal,
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

    fun observeUserTransactionsFromFirestore(userId: String): Flow<List<WalletTransaction>> = callbackFlow {
        val firestore = db ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val docId = userId.trim().lowercase()
        if (docId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("transactions")
            .whereEqualTo("userId", docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseRepository", "Listen failed for user transactions $docId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val txns = snapshot.documents.mapNotNull { doc ->
                        val rawAmt = doc.get("amount")
                        val amt = when (rawAmt) {
                            is Number -> rawAmt.toInt()
                            is String -> rawAmt.toIntOrNull() ?: 0
                            else -> 0
                        }
                        val rawTs = doc.get("timestamp")
                        val ts = when (rawTs) {
                            is Number -> rawTs.toLong()
                            is String -> rawTs.toLongOrNull() ?: System.currentTimeMillis()
                            else -> System.currentTimeMillis()
                        }
                        WalletTransaction(
                            id = doc.id.hashCode(),
                            userId = doc.getString("userId") ?: docId,
                            type = doc.getString("type") ?: "DEPOSIT",
                            amount = amt,
                            title = doc.getString("title") ?: "",
                            timestamp = ts,
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            accountDetail = doc.getString("accountDetail") ?: "",
                            status = doc.getString("status") ?: "SUCCESS",
                            transactionRef = doc.getString("transactionRef") ?: doc.getString("reference") ?: ""
                        )
                    }
                    trySend(txns)
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeUserBookingsFromFirestore(userId: String): Flow<List<Booking>> = callbackFlow {
        val firestore = db ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val docId = userId.trim().lowercase()
        if (docId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("bookings")
            .whereEqualTo("userId", docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseRepository", "Listen failed for user bookings $docId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val bookings = snapshot.documents.mapNotNull { doc ->
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
                    trySend(bookings)
                }
            }
        awaitClose { registration.remove() }
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

    suspend fun isUtrAlreadySubmitted(userId: String, utr: String): Boolean {
        val cleanUtr = utr.trim()
        if (cleanUtr.isBlank()) return false
        val firestore = db ?: return false
        return try {
            val reqSnapshot = firestore.collection("deposit_requests")
                .whereEqualTo("utrNumber", cleanUtr)
                .get()
                .await()
            if (!reqSnapshot.isEmpty) return true

            val txnSnapshot = firestore.collection("transactions")
                .whereEqualTo("transactionRef", cleanUtr)
                .get()
                .await()
            !txnSnapshot.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveWalletRequestToFirestore(request: WalletRequest): String? {
        val firestore = db ?: return null
        return try {
            val map = hashMapOf(
                "userId" to request.userId,
                "userEmail" to request.userEmail,
                "type" to request.type,
                "amount" to request.amount,
                "utrOrPaymentDetails" to request.utrOrPaymentDetails,
                "status" to "PENDING",
                "timestamp" to request.timestamp
            )
            val docRef = firestore.collection("requests").add(map).await()
            val docId = docRef.id
            
            try {
                firestore.collection("wallet_requests").document(docId).set(map).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (request.type.equals("DEPOSIT", ignoreCase = true)) {
                try {
                    val depMap = hashMapOf(
                        "userId" to request.userId,
                        "userName" to request.userEmail,
                        "amount" to request.amount.toInt(),
                        "utrNumber" to request.utrOrPaymentDetails,
                        "status" to "PENDING",
                        "timestamp" to request.timestamp
                    )
                    firestore.collection("deposit_requests").document(docId).set(depMap).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            docId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchPendingWalletRequestsFromFirestore(): List<WalletRequest> {
        val firestore = db ?: return emptyList()
        val list = mutableListOf<WalletRequest>()
        val seenIds = mutableSetOf<String>()

        val collections = listOf("requests", "wallet_requests", "deposit_requests")
        for (col in collections) {
            try {
                val snapshot = firestore.collection(col).get().await()
                for (doc in snapshot.documents) {
                    if (seenIds.contains(doc.id)) continue
                    val data = doc.data ?: continue
                    val status = (data["status"] as? String ?: "PENDING").uppercase()
                    if (status != "PENDING") continue

                    val userId = data["userId"] as? String ?: ""
                    val userEmail = data["userEmail"] as? String ?: (data["userName"] as? String ?: userId)
                    val rawType = (data["type"] as? String ?: "DEPOSIT").uppercase()
                    val type = if (rawType == "WITHDRAWAL" || rawType == "WITHDRAW") "WITHDRAWAL" else "DEPOSIT"
                    val rawAmt = data["amount"]
                    val amount = when (rawAmt) {
                        is Number -> rawAmt.toDouble()
                        is String -> rawAmt.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    val utrOrDetails = (data["utrOrPaymentDetails"] as? String)
                        ?.ifBlank { null }
                        ?: (data["utrNumber"] as? String ?: (data["accountDetail"] as? String ?: ""))
                    val rawTs = data["timestamp"]
                    val ts = when (rawTs) {
                        is Number -> rawTs.toLong()
                        is String -> rawTs.toLongOrNull() ?: System.currentTimeMillis()
                        else -> System.currentTimeMillis()
                    }

                    seenIds.add(doc.id)
                    list.add(
                        WalletRequest(
                            id = doc.id,
                            userId = userId,
                            userEmail = userEmail,
                            type = type,
                            amount = amount,
                            utrOrPaymentDetails = utrOrDetails,
                            status = "PENDING",
                            timestamp = ts
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list.sortedByDescending { it.timestamp }
    }

    private suspend fun updateUserCoinBalanceInFirestore(
        userEmail: String,
        userId: String,
        amount: Double,
        isDeposit: Boolean
    ) {
        val firestore = db ?: return
        val emailClean = userEmail.trim().lowercase()
        val userIdClean = userId.trim().lowercase()

        val docIdsToTry = listOf(emailClean, userIdClean, userEmail.trim(), userId.trim())
            .distinct()
            .filter { it.isNotBlank() }

        var updated = false

        for (docId in docIdsToTry) {
            val userRef = firestore.collection("users").document(docId)
            val snap = userRef.get().await()
            if (snap.exists()) {
                val currentCoin = (snap.get("coinBalance") as? Number)?.toDouble() ?: 0.0
                val newCoin = if (isDeposit) {
                    currentCoin + amount
                } else {
                    (currentCoin - amount).coerceAtLeast(0.0)
                }
                userRef.set(
                    mapOf(
                        "coinBalance" to newCoin.toInt(),
                        "walletBalance" to newCoin.toInt()
                    ),
                    SetOptions.merge()
                ).await()
                updated = true
                Log.d("FirestoreBalance", "Updated user [$docId] coinBalance to $newCoin")
                break
            }
        }

        if (!updated && emailClean.isNotBlank()) {
            val querySnap = firestore.collection("users")
                .whereEqualTo("email", emailClean)
                .get()
                .await()
            for (doc in querySnap.documents) {
                val currentCoin = (doc.get("coinBalance") as? Number)?.toDouble() ?: 0.0
                val newCoin = if (isDeposit) {
                    currentCoin + amount
                } else {
                    (currentCoin - amount).coerceAtLeast(0.0)
                }
                doc.reference.set(
                    mapOf(
                        "coinBalance" to newCoin.toInt(),
                        "walletBalance" to newCoin.toInt()
                    ),
                    SetOptions.merge()
                ).await()
                updated = true
            }
        }

        if (!updated && emailClean.isNotBlank()) {
            val initCoin = if (isDeposit) amount.toInt() else 0
            firestore.collection("users").document(emailClean).set(
                mapOf(
                    "id" to emailClean,
                    "email" to userEmail,
                    "coinBalance" to initCoin,
                    "walletBalance" to initCoin
                ),
                SetOptions.merge()
            ).await()
        }
    }

    suspend fun approveWalletRequestInFirestore(request: WalletRequest): Boolean {
        val firestore = db ?: return false
        val reqId = request.id.trim()
        val userEmail = request.userEmail.ifBlank { request.userName }.ifBlank { request.userId }.trim()

        Log.d("RequestApproval", "Approving request: id=$reqId, email=$userEmail, type=${request.type}, amount=${request.amount}")

        try {
            val collections = listOf("requests", "wallet_requests", "deposit_requests")
            if (reqId.isNotBlank()) {
                for (col in collections) {
                    try {
                        firestore.collection(col).document(reqId).update("status", "APPROVED").await()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            if (request.utrOrPaymentDetails.isNotBlank()) {
                for (col in collections) {
                    try {
                        val matchingDocs = firestore.collection(col)
                            .whereEqualTo("utrOrPaymentDetails", request.utrOrPaymentDetails)
                            .get()
                            .await()
                        for (d in matchingDocs.documents) {
                            d.reference.update("status", "APPROVED").await()
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }

            val isDeposit = request.type.equals("DEPOSIT", ignoreCase = true)
            updateUserCoinBalanceInFirestore(userEmail, request.userId, request.amount, isDeposit)

            if (request.utrOrPaymentDetails.isNotBlank()) {
                val existingTxns = firestore.collection("transactions")
                    .whereEqualTo("transactionRef", request.utrOrPaymentDetails)
                    .get()
                    .await()
                for (doc in existingTxns.documents) {
                    doc.reference.update("status", "SUCCESS").await()
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun rejectWalletRequestInFirestore(request: WalletRequest): Boolean {
        val firestore = db ?: return false
        val reqId = request.id.trim()

        try {
            val collections = listOf("requests", "wallet_requests", "deposit_requests")
            if (reqId.isNotBlank()) {
                for (col in collections) {
                    try {
                        firestore.collection(col).document(reqId).update("status", "REJECTED").await()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            if (request.utrOrPaymentDetails.isNotBlank()) {
                for (col in collections) {
                    try {
                        val matchingDocs = firestore.collection(col)
                            .whereEqualTo("utrOrPaymentDetails", request.utrOrPaymentDetails)
                            .get()
                            .await()
                        for (d in matchingDocs.documents) {
                            d.reference.update("status", "REJECTED").await()
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            if (request.utrOrPaymentDetails.isNotBlank()) {
                val existingTxns = firestore.collection("transactions")
                    .whereEqualTo("transactionRef", request.utrOrPaymentDetails)
                    .get()
                    .await()
                for (doc in existingTxns.documents) {
                    doc.reference.update("status", "FAILED").await()
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun saveDepositRequestToFirestore(depositRequest: DepositRequest): String? {
        val req = WalletRequest(
            id = depositRequest.id,
            userId = depositRequest.userId,
            userEmail = depositRequest.userName.ifBlank { depositRequest.userId },
            type = "DEPOSIT",
            amount = depositRequest.amount,
            utrOrPaymentDetails = depositRequest.utrNumber,
            status = depositRequest.status,
            timestamp = depositRequest.timestamp
        )
        return saveWalletRequestToFirestore(req)
    }

    suspend fun fetchDepositRequestsFromFirestore(): List<DepositRequest> {
        return fetchPendingWalletRequestsFromFirestore()
    }

    suspend fun updateDepositRequestStatusInFirestore(requestId: String, status: String) {
        val req = WalletRequest(id = requestId, status = status)
        if (status.equals("APPROVED", ignoreCase = true) || status.equals("SUCCESS", ignoreCase = true)) {
            approveWalletRequestInFirestore(req)
        } else if (status.equals("REJECTED", ignoreCase = true)) {
            rejectWalletRequestInFirestore(req)
        }
    }

    suspend fun approveDepositRequestInFirestore(depositRequest: DepositRequest) {
        approveWalletRequestInFirestore(depositRequest)
    }

    suspend fun incrementUserWalletInFirestore(userId: String, amount: Int) {
        val firestore = db ?: return
        val docId = userId.trim().lowercase()
        if (docId.isBlank()) return
        try {
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection("users").document(docId)
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val currentBal = (snapshot.getLong("coinBalance"))?.toInt() ?: 0
                    transaction.update(docRef, "coinBalance", currentBal + amount)
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
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
