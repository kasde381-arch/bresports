package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Booking
import com.example.data.model.Match
import com.example.data.model.User
import com.example.data.model.WalletTransaction
import com.example.data.repository.BookingResult
import com.example.data.repository.TournamentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import com.example.data.model.SupportMessage
import com.example.data.model.AppConfig
import com.example.data.model.AppUpdateInfo
import com.example.data.repository.VersionChecker
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat


data class SupportTicket(
    val id: String,
    val userId: String,
    val name: String,
    val uid: String,
    val issueDescription: String,
    val response: String,
    val isClosed: Boolean,
    val timestamp: Long
)

class TournamentViewModel(
    application: Application,
    private val repository: TournamentRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _isAuthChecking = MutableStateFlow(true)
    val isAuthChecking: StateFlow<Boolean> = _isAuthChecking.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", true))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _announcement = MutableStateFlow(
        prefs.getString("admin_announcement", "🚨 Room ID for Match #24 will be live 15 mins before start time. Download the Bermuda map to avoid disqualification!") ?: ""
    )
    val announcement: StateFlow<String> = _announcement.asStateFlow()

    fun updateAnnouncement(newAnnouncement: String) {
        prefs.edit().putString("admin_announcement", newAnnouncement).apply()
        _announcement.value = newAnnouncement
    }

    private val _emergencyNotification = MutableStateFlow(
        prefs.getString("admin_emergency_notification", "") ?: ""
    )
    val emergencyNotification: StateFlow<String> = _emergencyNotification.asStateFlow()

    fun updateEmergencyNotification(newNotification: String) {
        prefs.edit().putString("admin_emergency_notification", newNotification).apply()
        _emergencyNotification.value = newNotification
    }

    private val _supportTickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets.asStateFlow()

    fun loadSupportTickets() {
        val ids = prefs.getStringSet("ticket_ids", emptySet()) ?: emptySet()
        val list = mutableListOf<SupportTicket>()
        for (id in ids) {
            val userId = prefs.getString("ticket_${id}_userId", "") ?: ""
            val name = prefs.getString("ticket_${id}_name", "") ?: ""
            val uid = prefs.getString("ticket_${id}_uid", "") ?: ""
            val description = prefs.getString("ticket_${id}_description", "") ?: ""
            val response = prefs.getString("ticket_${id}_response", "") ?: ""
            val isClosed = prefs.getBoolean("ticket_${id}_isClosed", false)
            val timestamp = prefs.getLong("ticket_${id}_timestamp", 0L)
            list.add(SupportTicket(id, userId, name, uid, description, response, isClosed, timestamp))
        }
        _supportTickets.value = list.sortedByDescending { it.timestamp }
    }

    fun raiseSupportTicket(name: String, uid: String, description: String) {
        viewModelScope.launch {
            val id = "TKT-${(1000..9999).random()}"
            val activeEmail = prefs.getString("active_email", "") ?: "local_user"
            
            val ids = prefs.getStringSet("ticket_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            ids.add(id)
            
            prefs.edit()
                .putStringSet("ticket_ids", ids)
                .putString("ticket_${id}_userId", activeEmail)
                .putString("ticket_${id}_name", name)
                .putString("ticket_${id}_uid", uid)
                .putString("ticket_${id}_description", description)
                .putString("ticket_${id}_response", "")
                .putBoolean("ticket_${id}_isClosed", false)
                .putLong("ticket_${id}_timestamp", System.currentTimeMillis())
                .apply()
                
            loadSupportTickets()
            _eventFlow.emit(UIEvent.ShowMessage("Support ticket raised successfully! ID: $id"))
        }
    }

    fun replyToSupportTicket(id: String, response: String) {
        viewModelScope.launch {
            prefs.edit()
                .putString("ticket_${id}_response", response)
                .apply()
            loadSupportTickets()
            _eventFlow.emit(UIEvent.ShowMessage("Replied to Ticket $id"))
        }
    }

    fun closeSupportTicket(id: String) {
        viewModelScope.launch {
            prefs.edit()
                .putBoolean("ticket_${id}_isClosed", true)
                .apply()
            loadSupportTickets()
            _eventFlow.emit(UIEvent.ShowMessage("Ticket $id Closed!"))
        }
    }

    fun processAutomatedTournamentResult(matchId: Int, playerUid: String, kills: Int, placement: Int) {
        viewModelScope.launch {
            val match = repository.allMatches.first().find { it.id == matchId }
            if (match == null) {
                _eventFlow.emit(UIEvent.ShowMessage("Match not found!"))
                return@launch
            }

            val emails = prefs.getStringSet("registered_emails", emptySet()) ?: emptySet()
            var userEmail: String? = null
            for (email in emails) {
                val uid = prefs.getString("user_uid_$email", "") ?: ""
                if (uid.trim().equals(playerUid.trim(), ignoreCase = true)) {
                    userEmail = email
                    break
                }
            }

            if (userEmail == null) {
                _eventFlow.emit(UIEvent.ShowMessage("No registered user found with UID: $playerUid"))
                return@launch
            }

            val killReward = kills * match.perKillPrize
            val placementReward = when (placement) {
                1 -> (match.prizePool * 0.50).toInt()
                2 -> (match.prizePool * 0.30).toInt()
                3 -> (match.prizePool * 0.20).toInt()
                else -> 0
            }
            val totalCoins = killReward + placementReward

            if (totalCoins <= 0) {
                _eventFlow.emit(UIEvent.ShowMessage("Calculated coins is 0. No wallet update required."))
                return@launch
            }

            val currentCoins = prefs.getInt("user_coins_$userEmail", 0)
            val newCoins = currentCoins + totalCoins
            prefs.edit().putInt("user_coins_$userEmail", newCoins).apply()

            val dbUser = repository.getUser(userEmail).firstOrNull() ?: User(id = userEmail)
            repository.saveUserProfile(dbUser.copy(id = userEmail, coinBalance = newCoins))

            val txn = WalletTransaction(
                userId = userEmail,
                type = "DEPOSIT",
                amount = totalCoins,
                paymentMethod = "Tournament Winnings",
                accountDetail = "Match: ${match.title} (Pl: $placement, Kills: $kills)",
                status = "SUCCESS",
                transactionRef = "WIN" + (100000..999999).random()
            )
            repository.insertTransaction(txn)

            refreshRegisteredUsers()
            _eventFlow.emit(UIEvent.ShowMessage("Success: Credited $totalCoins Coins to UID $playerUid ($placementReward Placement + $killReward Kills)"))
        }
    }

    private val _activeUserId = MutableStateFlow(
        if (prefs.getBoolean("is_logged_in", true)) {
            prefs.getString("active_email", "kasde381@gmail.com")?.ifBlank { "kasde381@gmail.com" } ?: "kasde381@gmail.com"
        } else {
            ""
        }
    )
    val activeUserId: StateFlow<String> = _activeUserId.asStateFlow()

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            val savedLoggedIn = prefs.getBoolean("is_logged_in", true)
            val activeEmail = prefs.getString("active_email", "kasde381@gmail.com")?.ifBlank { "kasde381@gmail.com" } ?: "kasde381@gmail.com"
            if (savedLoggedIn && activeEmail.isNotBlank()) {
                _activeUserId.value = activeEmail
                _isLoggedIn.value = true
            } else {
                _activeUserId.value = ""
                _isLoggedIn.value = false
            }
            _isAuthChecking.value = false
        }
    }

    // State flows representing our reactive DB data
    @OptIn(ExperimentalCoroutinesApi::class)
    val user: StateFlow<User?> = _activeUserId
        .flatMapLatest { userId ->
            repository.getUser(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val matches: StateFlow<List<Match>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val userBookings: StateFlow<List<Booking>> = _activeUserId
        .flatMapLatest { userId ->
            repository.getBookingsForUser(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<WalletTransaction>> = _activeUserId
        .flatMapLatest { userId ->
            repository.getTransactionsForUser(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<WalletTransaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestVersionCode: StateFlow<String> = repository.observeAppConfig("latest_version_code")
        .map { it?.value ?: "1" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1")

    private val _appUpdateInfo = MutableStateFlow(AppUpdateInfo())
    val appUpdateInfo: StateFlow<AppUpdateInfo> = _appUpdateInfo.asStateFlow()

    private val _isUpdateDismissed = MutableStateFlow(false)
    val isUpdateDismissed: StateFlow<Boolean> = _isUpdateDismissed.asStateFlow()

    private val _remoteConfigUrl = MutableStateFlow(
        prefs.getString("version_config_url", VersionChecker.DEFAULT_VERSION_JSON_URL) ?: VersionChecker.DEFAULT_VERSION_JSON_URL
    )
    val remoteConfigUrl: StateFlow<String> = _remoteConfigUrl.asStateFlow()

    fun updateRemoteConfigUrl(newUrl: String) {
        prefs.edit().putString("version_config_url", newUrl).apply()
        _remoteConfigUrl.value = newUrl
        viewModelScope.launch {
            _eventFlow.emit(UIEvent.ShowMessage("Updated Version Config URL!"))
        }
    }

    fun dismissUpdateDialog() {
        _isUpdateDismissed.value = true
    }

    fun checkAppVersionOnline(currentLocalVersionCode: Int = 1, showToastOnUpToDate: Boolean = false) {
        viewModelScope.launch {
            _appUpdateInfo.value = _appUpdateInfo.value.copy(checkStatus = "CHECKING")
            
            // Check remote version json file on GitHub or configured URL
            val result = VersionChecker.checkRemoteVersion(
                configUrl = _remoteConfigUrl.value,
                currentVersionCode = currentLocalVersionCode
            )
            
            if (result.checkStatus == "SUCCESS") {
                _appUpdateInfo.value = result
                _isUpdateDismissed.value = false
                repository.saveAppConfig(AppConfig("latest_version_code", result.latestVersionCode.toString()))
                if (result.latestVersionCode > currentLocalVersionCode) {
                    _eventFlow.emit(UIEvent.ShowMessage("New update found: v${result.latestVersionName}!"))
                } else if (showToastOnUpToDate) {
                    _eventFlow.emit(UIEvent.ShowMessage("App is up to date (v$currentLocalVersionCode.0)"))
                }
            } else {
                // If remote check failed or offline, fall back to DB stored latest_version_code
                val storedVersionStr = repository.observeAppConfig("latest_version_code").firstOrNull()?.value ?: "$currentLocalVersionCode"
                val storedVersionCode = storedVersionStr.toIntOrNull() ?: currentLocalVersionCode
                
                _appUpdateInfo.value = AppUpdateInfo(
                    latestVersionCode = storedVersionCode,
                    latestVersionName = "$storedVersionCode.0",
                    apkUrl = "https://kasde381-arch.github.io/bresports/",
                    releaseNotes = "• Critical tournament lobby stability fixes\n• Instant wallet deposit & coin sync improvements",
                    isForceUpdate = storedVersionCode > currentLocalVersionCode,
                    checkStatus = "SUCCESS",
                    errorMessage = result.errorMessage
                )
                if (storedVersionCode > currentLocalVersionCode) {
                    _eventFlow.emit(UIEvent.ShowMessage("Update available: v$storedVersionCode.0"))
                } else if (showToastOnUpToDate) {
                    _eventFlow.emit(UIEvent.ShowMessage("App is up to date (v$currentLocalVersionCode.0)"))
                }
            }
        }
    }

    fun simulateUpdateAvailable(targetVersionCode: Int = 2, apkUrl: String? = null, isForce: Boolean = true) {
        viewModelScope.launch {
            val downloadUrl = apkUrl ?: "https://kasde381-arch.github.io/bresports/"
            _appUpdateInfo.value = AppUpdateInfo(
                latestVersionCode = targetVersionCode,
                latestVersionName = "$targetVersionCode.0.0",
                minSupportedVersionCode = 1,
                apkUrl = downloadUrl,
                releaseNotes = "• Brand new Tournament Battle Lobby UI\n• Anti-cheat security patches\n• Instant UPI cashout integration\n• Enhanced stability & bug fixes",
                isForceUpdate = isForce,
                checkStatus = "SUCCESS"
            )
            _isUpdateDismissed.value = false
            repository.saveAppConfig(AppConfig("latest_version_code", targetVersionCode.toString()))
            _eventFlow.emit(UIEvent.ShowMessage("Simulated update trigger: v$targetVersionCode.0.0"))
        }
    }

    fun updateLatestVersionCode(newVersion: String) {
        viewModelScope.launch {
            repository.saveAppConfig(AppConfig("latest_version_code", newVersion))
            _eventFlow.emit(UIEvent.ShowMessage("Latest version code updated to $newVersion"))
        }
    }


    val trustedAdmins: StateFlow<String> = repository.observeAppConfig("trusted_admins")
        .map { it?.value ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isCurrentUserAdmin: StateFlow<Boolean> = combine(user, _activeUserId, trustedAdmins) { u, activeId, admins ->
        val emailToCheck = u?.email?.ifBlank { activeId } ?: activeId
        val idToCheck = u?.id?.ifBlank { activeId } ?: activeId
        val gameUidToCheck = u?.gameUid ?: ""
        val phoneToCheck = u?.phone ?: ""

        val isAdmin = emailToCheck.equals("kasde381@gmail.com", ignoreCase = true) ||
                emailToCheck.contains("admin", ignoreCase = true) ||
                idToCheck.equals("kasde381@gmail.com", ignoreCase = true) ||
                idToCheck.contains("admin", ignoreCase = true) ||
                gameUidToCheck == "842910485" || gameUidToCheck == "84920419"

        if (isAdmin) {
            true
        } else {
            val list = admins.split(";").filter { it.isNotBlank() }.map { it.split("|").firstOrNull() ?: "" }
            val phoneMatch = phoneToCheck.isNotBlank() && list.any { it == phoneToCheck }
            val uidMatch = gameUidToCheck.isNotBlank() && list.any { it == gameUidToCheck }
            val emailMatch = emailToCheck.isNotBlank() && list.any { it.equals(emailToCheck, ignoreCase = true) }
            val idMatch = idToCheck.isNotBlank() && list.any { it.equals(idToCheck, ignoreCase = true) }
            phoneMatch || uidMatch || emailMatch || idMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _registeredUsers = MutableStateFlow<List<User>>(emptyList())
    val registeredUsers: StateFlow<List<User>> = _registeredUsers.asStateFlow()

    fun refreshRegisteredUsers() {
        val emails = prefs.getStringSet("registered_emails", emptySet()) ?: emptySet()
        val list = mutableListOf<User>()
        for (email in emails) {
            val username = prefs.getString("user_username_$email", "") ?: ""
            val phone = prefs.getString("user_phone_$email", "") ?: ""
            val gameUid = prefs.getString("user_uid_$email", "") ?: ""
            val gameName = prefs.getString("user_ign_$email", "") ?: ""
            val promoCode = prefs.getString("user_promo_$email", "") ?: ""
            val coinBalance = prefs.getInt("user_coins_$email", 1000)
            val referralCode = prefs.getString("user_referral_code_$email", "") ?: ""
            val totalEarned = prefs.getInt("total_referral_earnings_$email", 0)
            val referredBy = prefs.getString("referred_by_$email", "") ?: ""
            val joinedAt = prefs.getLong("user_joined_at_$email", 0L)
            
            list.add(
                User(
                    id = email,
                    email = email,
                    username = username,
                    phone = phone,
                    gameUid = gameUid,
                    gameName = gameName,
                    promoCode = promoCode,
                    coinBalance = coinBalance,
                    referralCode = referralCode,
                    referredByCode = referredBy,
                    totalEarnedReferrals = totalEarned,
                    joinedAtMillis = if (joinedAt == 0L) {
                        // fallback for old users: default date from 3-4 days ago
                        if (email == "kasde381@gmail.com") System.currentTimeMillis() - 4 * 24 * 3600 * 1000L
                        else System.currentTimeMillis()
                    } else joinedAt
                )
            )
        }
        _registeredUsers.value = list.sortedBy { it.joinedAtMillis }
    }

    fun addTrustedAdmin(phoneOrUid: String): Boolean {
        val trimmed = phoneOrUid.trim()
        if (trimmed.isBlank()) return false
        
        // Find if this phoneOrUid matches any registered user to get their name
        val emails = prefs.getStringSet("registered_emails", emptySet()) ?: emptySet()
        var foundName = "User ($trimmed)"
        for (email in emails) {
            val savedUid = prefs.getString("user_uid_$email", "") ?: ""
            val savedPhone = prefs.getString("user_phone_$email", "") ?: ""
            val savedIgn = prefs.getString("user_ign_$email", "") ?: ""
            if (savedUid == trimmed || savedPhone == trimmed) {
                foundName = savedIgn.ifBlank { "User ($trimmed)" }
                break
            }
        }
        
        val currentAdmins = trustedAdmins.value
        val list = currentAdmins.split(";").filter { it.isNotBlank() }.map {
            val parts = it.split("|")
            (parts.getOrNull(0) ?: "") to (parts.getOrNull(1) ?: "")
        }.toMutableList()
        
        if (list.any { it.first == trimmed }) {
            return false // Already added
        }
        
        list.add(trimmed to foundName)
        val newAdminsStr = list.joinToString(";") { "${it.first}|${it.second}" }
        
        viewModelScope.launch {
            repository.saveAppConfig(AppConfig("trusted_admins", newAdminsStr))
            _eventFlow.emit(UIEvent.ShowMessage("Added $foundName as Trusted Admin!"))
        }
        return true
    }

    fun revokeTrustedAdmin(phoneOrUid: String) {
        val trimmed = phoneOrUid.trim()
        val currentAdmins = trustedAdmins.value
        val list = currentAdmins.split(";").filter { it.isNotBlank() }.map {
            val parts = it.split("|")
            (parts.getOrNull(0) ?: "") to (parts.getOrNull(1) ?: "")
        }.filter { it.first != trimmed }
        
        val newAdminsStr = list.joinToString(";") { "${it.first}|${it.second}" }
        
        viewModelScope.launch {
            repository.saveAppConfig(AppConfig("trusted_admins", newAdminsStr))
            _eventFlow.emit(UIEvent.ShowMessage("Revoked Admin access for $trimmed"))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val supportMessages: StateFlow<List<SupportMessage>> = _activeUserId
        .flatMapLatest { userId ->
            repository.getSupportMessagesForUser(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSupportMessages: StateFlow<List<SupportMessage>> = repository.allSupportMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loginWithGoogle(email: String, displayName: String) {
        viewModelScope.launch {
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("active_email", email)
                .apply()
            _activeUserId.value = email
            _isLoggedIn.value = true

            // Update user profile
            val currentUser = repository.getUser(email).firstOrNull() ?: User(id = email)
            val updatedUser = currentUser.copy(
                id = email,
                email = email,
                gameName = if (displayName.isNotBlank()) displayName else currentUser.gameName.ifBlank { "BOOYAH_SLAYER" },
                gameUid = if (currentUser.gameUid.isEmpty()) "842910485" else currentUser.gameUid,
                username = if (displayName.isNotBlank()) displayName.replace(" ", "_").lowercase() else "booyah_slayer"
            )
            repository.saveUserProfile(updatedUser)
            _eventFlow.emit(UIEvent.ShowMessage("Successfully signed in as ${updatedUser.username}"))
        }
    }

    fun registerUser(
        username: String,
        email: String,
        phone: String,
        gameUid: String,
        gameName: String,
        password: String,
        promoCode: String
    ): Boolean {
        if (username.isBlank() || email.isBlank() || phone.isBlank() || gameUid.isBlank() || gameName.isBlank() || password.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(UIEvent.ShowMessage("Please fill in all required fields."))
            }
            return false
        }

        val emails = prefs.getStringSet("registered_emails", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (emails.contains(email)) {
            viewModelScope.launch {
                _eventFlow.emit(UIEvent.ShowMessage("Email is already registered. Please login."))
            }
            return false
        }

        // Determine if there is a valid referral code from another user
        var referrerEmail: String? = null
        val trimmedPromo = promoCode.trim()
        if (trimmedPromo.isNotBlank()) {
            for (e in emails) {
                val refCode = prefs.getString("user_referral_code_$e", "") ?: ""
                if (refCode.isNotBlank() && refCode.equals(trimmedPromo, ignoreCase = true)) {
                    referrerEmail = e
                    break
                }
            }
        }

        // 5 Coins sign-up bonus if referred by a valid code, otherwise standard initial balance (0)
        // plus optionally random promo coins if they used some other system promo code
        val baseCoins = 0
        val referralBonus = if (referrerEmail != null) 5 else 0
        val systemPromoBonus = if (referrerEmail == null && promoCode.isNotBlank()) (5..10).random() else 0
        val initialCoins = baseCoins + referralBonus + systemPromoBonus

        // Generate unique referral code for this new user
        val generatedCode = "BR-${username.uppercase().filter { it.isLetterOrDigit() }.take(5)}-${(100..999).random()}"

        emails.add(email)
        prefs.edit()
            .putStringSet("registered_emails", emails)
            .putString("user_password_$email", password)
            .putString("user_username_$email", username)
            .putString("user_phone_$email", phone)
            .putString("user_uid_$email", gameUid)
            .putString("user_ign_$email", gameName)
            .putString("user_promo_$email", promoCode)
            .putInt("user_coins_$email", initialCoins)
            .putBoolean("user_first_login_$email", true)
            .putString("user_referral_code_$email", generatedCode)
            .putInt("total_referral_earnings_$email", 0)
            .putString("referred_by_$email", referrerEmail ?: "")
            .putBoolean("referral_rewarded_$email", false)
            .putLong("user_joined_at_$email", System.currentTimeMillis())
            .apply()

        refreshRegisteredUsers()

        viewModelScope.launch {
            if (referrerEmail != null) {
                _eventFlow.emit(UIEvent.ShowMessage("Account created! 5 Coins Referral Bonus credited! Please Login."))
            } else if (promoCode.isNotBlank()) {
                _eventFlow.emit(UIEvent.ShowMessage("Account created with $systemPromoBonus Coins Promo! Please Login."))
            } else {
                _eventFlow.emit(UIEvent.ShowMessage("Registration Successful! Please Login to continue."))
            }
        }
        return true
    }

    fun loginWithEmailAndPassword(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(UIEvent.ShowMessage("Please enter email and password."))
            }
            return false
        }

        val emails = prefs.getStringSet("registered_emails", emptySet()) ?: emptySet()
        if (!emails.contains(email)) {
            viewModelScope.launch {
                _eventFlow.emit(UIEvent.ShowMessage("Account not found. Please Sign Up first."))
            }
            return false
        }

        val savedPassword = prefs.getString("user_password_$email", "")
        if (savedPassword != password) {
            viewModelScope.launch {
                _eventFlow.emit(UIEvent.ShowMessage("Incorrect password. Please try again."))
            }
            return false
        }

        val isFirstLogin = prefs.getBoolean("user_first_login_$email", true)

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("active_email", email)
            .putBoolean("user_first_login_$email", false)
            .apply()

        _activeUserId.value = email
        _isLoggedIn.value = true

        val username = prefs.getString("user_username_$email", "") ?: ""
        val phone = prefs.getString("user_phone_$email", "") ?: ""
        val gameUid = prefs.getString("user_uid_$email", "") ?: ""
        val gameName = prefs.getString("user_ign_$email", "") ?: ""
        val promoCode = prefs.getString("user_promo_$email", "") ?: ""
        val coinBalance = prefs.getInt("user_coins_$email", 1000)

        // Generate unique referral code on login if missing
        var myReferralCode = prefs.getString("user_referral_code_$email", "") ?: ""
        if (myReferralCode.isBlank()) {
            myReferralCode = "BR-${username.uppercase().filter { it.isLetterOrDigit() }.take(5)}-${(100..999).random()}"
            prefs.edit().putString("user_referral_code_$email", myReferralCode).apply()
        }
        val totalEarned = prefs.getInt("total_referral_earnings_$email", 0)
        val referredBy = prefs.getString("referred_by_$email", "") ?: ""

        viewModelScope.launch {
            val currentUser = repository.getUser(email).firstOrNull() ?: User(id = email)
            val updatedUser = currentUser.copy(
                id = email,
                email = email,
                gameName = gameName,
                gameUid = gameUid,
                username = username,
                phone = phone,
                promoCode = promoCode,
                coinBalance = coinBalance,
                referralCode = myReferralCode,
                referredByCode = referredBy,
                totalEarnedReferrals = totalEarned
            )
            repository.saveUserProfile(updatedUser)

            if (isFirstLogin) {
                // If they were referred, we create a custom Referral SignUp Bonus transaction!
                if (referredBy.isNotBlank()) {
                    val txn = WalletTransaction(
                        type = "DEPOSIT",
                        amount = 5,
                        paymentMethod = "Referral Bonus",
                        accountDetail = "Sign-up Bonus",
                        status = "SUCCESS",
                        transactionRef = "REF" + (100000..999999).random()
                    )
                    repository.insertTransaction(txn)
                } else if (promoCode.isNotBlank()) {
                    val txn = WalletTransaction(
                        type = "DEPOSIT",
                        amount = coinBalance - 1000,
                        paymentMethod = "Promo Code",
                        accountDetail = promoCode,
                        status = "SUCCESS",
                        transactionRef = "PROMO" + (100000..999999).random()
                    )
                    repository.insertTransaction(txn)
                }
            }

            _eventFlow.emit(UIEvent.ShowMessage("Welcome back, $gameName!"))
        }

        return true
    }

    fun isUidRegistered(uid: String, currentEmail: String? = null): Boolean {
        val emails = prefs.getStringSet("registered_emails", emptySet()) ?: emptySet()
        for (email in emails) {
            if (currentEmail != null && email.equals(currentEmail, ignoreCase = true)) continue
            val savedUid = prefs.getString("user_uid_$email", "") ?: ""
            if (savedUid == uid) {
                return true
            }
        }
        return false
    }

    fun updateBookingScreenshot(booking: Booking, screenshotUri: String) {
        viewModelScope.launch {
            repository.updateBooking(booking.copy(screenshotUri = screenshotUri))
            _eventFlow.emit(UIEvent.ShowMessage("Continuous gameplay screenshot proof uploaded!"))
        }
    }

    fun logout() {
        viewModelScope.launch {
            prefs.edit()
                .putBoolean("is_logged_in", false)
                .remove("active_email")
                .apply()
            _activeUserId.value = ""
            _isLoggedIn.value = false
            _eventFlow.emit(UIEvent.ShowMessage("Logged out successfully"))
        }
    }

    // Seed database if empty
    init {
        checkAppVersionOnline(currentLocalVersionCode = 1)
        viewModelScope.launch {
            repository.prepopulateDataIfEmpty()
            repository.checkAndGenerateDailyMatches()
            
            // Periodic check to trigger daily match generation when date rolls over at midnight
            launch {
                while (true) {
                    try {
                        repository.checkAndGenerateDailyMatches()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(15000)
                }
            }

            // Periodic check for Match Start status transition (Auto-Live Scheduler)
            launch {
                while (true) {
                    try {
                        checkAndAutoUpdateMatchLifecycles()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(15000)
                }
            }
            
            val emails = prefs.getStringSet("registered_emails", emptySet())?.toMutableSet() ?: mutableSetOf()
            var modified = false

            // Explicitly purge dummy, placeholder, and outside test users
            val dummyEmails = listOf(
                "mortal_god@gmail.com",
                "sensi_pro@gmail.com",
                "alpha_striker@gmail.com",
                "dynamo_ff@gmail.com",
                "player.pro@gmail.com"
            )
            for (dummy in dummyEmails) {
                if (emails.contains(dummy)) {
                    emails.remove(dummy)
                    prefs.edit()
                        .remove("user_password_$dummy")
                        .remove("user_username_$dummy")
                        .remove("user_phone_$dummy")
                        .remove("user_uid_$dummy")
                        .remove("user_ign_$dummy")
                        .remove("user_coins_$dummy")
                        .remove("user_joined_at_$dummy")
                        .remove("user_referral_code_$dummy")
                        .remove("referred_by_$dummy")
                        .apply()
                    modified = true
                }
            }

            if (!emails.contains("kasde381@gmail.com")) {
                emails.add("kasde381@gmail.com")
                prefs.edit()
                    .putString("user_password_kasde381@gmail.com", "password")
                    .putString("user_username_kasde381@gmail.com", "booyah_slayer")
                    .putString("user_phone_kasde381@gmail.com", "9876543210")
                    .putString("user_uid_kasde381@gmail.com", "842910485")
                    .putString("user_ign_kasde381@gmail.com", "BOOYAH_SLAYER")
                    .putInt("user_coins_kasde381@gmail.com", 1500)
                    .putLong("user_joined_at_kasde381@gmail.com", System.currentTimeMillis() - 4 * 24 * 3600 * 1000L)
                    .putString("user_referral_code_kasde381@gmail.com", "BR-SLAYE-485")
                    .apply()
                modified = true
            }

            if (modified) {
                prefs.edit().putStringSet("registered_emails", emails).apply()
            }
            
            // Re-load the list of registered users to reflect changes immediately
            refreshRegisteredUsers()
            loadSupportTickets()

            try {
                val current = repository.supportMessages.first()
                if (current.isEmpty()) {
                    repository.insertSupportMessage(
                        SupportMessage(
                            senderId = "system_bot",
                            senderName = "Support Bot",
                            text = "Welcome to BR Esports Support! 🎮\nHow can we help you today? Feel free to ask about Deposits, Withdrawals, Tournament schedules, or any other queries."
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            user.collect { currentUser ->
                if (currentUser != null) {
                    val activeEmail = prefs.getString("active_email", "") ?: ""
                    if (activeEmail.isNotEmpty()) {
                        val currentSavedCode = prefs.getString("user_referral_code_$activeEmail", "") ?: ""
                        val finalCode = if (currentSavedCode.isBlank()) {
                            val generated = "BR-${currentUser.username.ifBlank { "PLAYER" }.uppercase().filter { it.isLetterOrDigit() }.take(5)}-${(100..999).random()}"
                            prefs.edit().putString("user_referral_code_$activeEmail", generated).apply()
                            generated
                        } else {
                            currentSavedCode
                        }
                        
                        val totalEarned = prefs.getInt("total_referral_earnings_$activeEmail", 0)
                        
                        if (currentUser.referralCode != finalCode || currentUser.totalEarnedReferrals != totalEarned) {
                            val updatedUser = currentUser.copy(
                                referralCode = finalCode,
                                totalEarnedReferrals = totalEarned
                            )
                            repository.saveUserProfile(updatedUser)
                        }

                        prefs.edit()
                            .putString("user_uid_$activeEmail", currentUser.gameUid)
                            .putString("user_ign_$activeEmail", currentUser.gameName)
                            .putInt("user_coins_$activeEmail", currentUser.coinBalance)
                            .apply()
                        refreshRegisteredUsers()
                    }
                }
            }
        }
        
        refreshRegisteredUsers()
    }

    fun sendSupportMessage(text: String, senderId: String = "user", senderName: String = "You") {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.insertSupportMessage(
                SupportMessage(
                    senderId = senderId,
                    senderName = senderName,
                    text = text
                )
            )
            if (senderId == "user") {
                simulateSupportResponse(text)
            }
        }
    }

    private fun simulateSupportResponse(userText: String) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            val responseText = when {
                userText.contains("deposit", ignoreCase = true) || userText.contains("add coin", ignoreCase = true) -> {
                    "For instant coin deposits, use our 'DEPOSIT' button on the Profile screen to open UPI Sandbox Simulator or deep-link options. Once completed, your balance will update instantly!"
                }
                userText.contains("withdraw", ignoreCase = true) || userText.contains("cashout", ignoreCase = true) -> {
                    "Withdrawal requests are processed instantly to your specified UPI ID. Minimum withdrawal is 100 coins. Make sure your UPI ID is correct in your Profile's Withdrawal form."
                }
                userText.contains("room", ignoreCase = true) || userText.contains("id", ignoreCase = true) || userText.contains("password", ignoreCase = true) -> {
                    "Custom room IDs and passwords are automatically displayed on your 'My Bookings' tab 15 minutes before the match start time. Ensure you check on time!"
                }
                userText.contains("free fire", ignoreCase = true) || userText.contains("uid", ignoreCase = true) -> {
                    "Please ensure your Free Fire Game UID and IGN match exactly. You can update these anytime in your Profile tab."
                }
                userText.contains("hello", ignoreCase = true) || userText.contains("hi", ignoreCase = true) || userText.contains("hey", ignoreCase = true) -> {
                    "Hello! Thanks for reaching out to BR Esports Support. How can we assist you with your tournaments or wallet today?"
                }
                else -> {
                    "Thank you for contacting BR Esports Support. A dedicated support agent has been notified and will reply shortly. Support hours: 10:00 AM to 10:00 PM."
                }
            }
            repository.insertSupportMessage(
                SupportMessage(
                    senderId = "admin",
                    senderName = "BR Support Agent",
                    text = responseText
                )
            )
        }
    }


    // UI event flow to communicate booking status messages
    private val _eventFlow = MutableSharedFlow<UIEvent>()
    val eventFlow: SharedFlow<UIEvent> = _eventFlow.asSharedFlow()

    // Observe booking details for a specific match
    fun getBookingForMatch(matchId: Int): Flow<Booking?> {
        return repository.observeBookingForMatch(matchId, _activeUserId.value)
    }

    // 1. Profile Actions
    fun saveProfile(gameUid: String, gameName: String, email: String, avatar: String) {
        viewModelScope.launch {
            val currentUserId = _activeUserId.value
            val currentUser = user.value ?: User(id = currentUserId)
            val updatedUser = currentUser.copy(
                id = currentUserId,
                gameUid = gameUid,
                gameName = gameName,
                email = email,
                avatar = avatar
            )
            repository.saveUserProfile(updatedUser)
            _eventFlow.emit(UIEvent.ShowMessage("Profile updated successfully!"))
        }
    }

    fun refillCoins(amount: Int = 1000) {
        viewModelScope.launch {
            val currentUserId = _activeUserId.value
            val currentUser = user.value ?: User(id = currentUserId)
            val newBalance = currentUser.coinBalance + amount
            repository.saveUserProfile(currentUser.copy(id = currentUserId, coinBalance = newBalance))
            prefs.edit().putInt("user_coins_$currentUserId", newBalance).apply()
            refreshRegisteredUsers()
            
            val txn = WalletTransaction(
                userId = currentUserId,
                type = "DEPOSIT",
                amount = amount,
                paymentMethod = "Sandbox Quick Refill",
                accountDetail = "System Sandbox Account",
                status = "SUCCESS",
                transactionRef = "TXN" + (100000..999999).random()
            )
            repository.insertTransaction(txn)
            _eventFlow.emit(UIEvent.ShowMessage("Added $amount coins to your balance!"))
        }
    }

    fun applyReferralCode(code: String) {
        viewModelScope.launch {
            val currentUserId = _activeUserId.value
            val currentUser = user.value ?: User(id = currentUserId)
            if (currentUser.promoCode.isNotBlank()) {
                _eventFlow.emit(UIEvent.ShowMessage("You have already applied a referral/promo code!"))
                return@launch
            }
            if (code.isBlank() || code.length < 3) {
                _eventFlow.emit(UIEvent.ShowMessage("Please enter a valid referral code."))
                return@launch
            }
            
            // Add 200 coins bonus
            val newBalance = currentUser.coinBalance + 200
            val updatedUser = currentUser.copy(
                id = currentUserId,
                promoCode = code,
                coinBalance = newBalance
            )
            repository.saveUserProfile(updatedUser)
            prefs.edit().putInt("user_coins_$currentUserId", newBalance).apply()
            refreshRegisteredUsers()
            
            // Log transaction
            val txn = WalletTransaction(
                userId = currentUserId,
                type = "DEPOSIT",
                amount = 200,
                paymentMethod = "Referral Signup Bonus",
                accountDetail = "Applied Referral Code: $code",
                status = "SUCCESS",
                transactionRef = "REF" + (100000..999999).random()
            )
            repository.insertTransaction(txn)
            _eventFlow.emit(UIEvent.ShowMessage("Referral Code applied successfully! 200 Coins added!"))
        }
    }

    fun creditReferralReward(friendName: String) {
        viewModelScope.launch {
            val currentUserId = _activeUserId.value
            val currentUser = user.value ?: User(id = currentUserId)
            val newBalance = currentUser.coinBalance + 300
            repository.saveUserProfile(currentUser.copy(id = currentUserId, coinBalance = newBalance))
            prefs.edit().putInt("user_coins_$currentUserId", newBalance).apply()
            refreshRegisteredUsers()
            
            val txn = WalletTransaction(
                userId = currentUserId,
                type = "DEPOSIT",
                amount = 300,
                paymentMethod = "Referral Match Bonus",
                accountDetail = "Referred Friend: $friendName completed first match",
                status = "SUCCESS",
                transactionRef = "REF" + (100000..999999).random()
            )
            repository.insertTransaction(txn)
            _eventFlow.emit(UIEvent.ShowMessage("BOOYAH! Friend '$friendName' finished their first tournament. 300 Coins credited to your wallet!"))
        }
    }

    fun claimDailyLuckyBonus(coinsClaimed: Int) {
        viewModelScope.launch {
            val currentUserId = _activeUserId.value
            val currentUser = user.value ?: User(id = currentUserId)
            val newBalance = currentUser.coinBalance + coinsClaimed
            repository.saveUserProfile(currentUser.copy(id = currentUserId, coinBalance = newBalance))
            prefs.edit().putInt("user_coins_$currentUserId", newBalance).apply()
            refreshRegisteredUsers()
            
            val txn = WalletTransaction(
                userId = currentUserId,
                type = "DEPOSIT",
                amount = coinsClaimed,
                paymentMethod = "Daily Lucky Lootbox",
                accountDetail = "Claimed Free Daily Bonus",
                status = "SUCCESS",
                transactionRef = "REF" + (100000..999999).random()
            )
            repository.insertTransaction(txn)
            
            // Persist the daily claim timestamp in Prefs for the current user
            if (currentUserId.isNotBlank()) {
                prefs.edit().putLong("last_claim_timestamp_$currentUserId", System.currentTimeMillis()).apply()
            }
            
            _eventFlow.emit(UIEvent.ShowMessage("Congratulations! You won $coinsClaimed Coins from the Lucky Lootbox! 🎁"))
        }
    }

    fun getLastClaimTimestamp(): Long {
        val currentUserId = _activeUserId.value
        return if (currentUserId.isNotBlank()) {
            prefs.getLong("last_claim_timestamp_$currentUserId", 0L)
        } else {
            0L
        }
    }

    fun resetDailyLuckyBonusCooldown() {
        val currentUserId = _activeUserId.value
        if (currentUserId.isNotBlank()) {
            prefs.edit().putLong("last_claim_timestamp_$currentUserId", 0L).apply()
        }
    }

    fun depositCoins(amount: Int, paymentMethod: String, accountDetail: String) {
        viewModelScope.launch {
            val currentUserId = _activeUserId.value
            val txn = WalletTransaction(
                userId = currentUserId,
                type = "DEPOSIT",
                amount = amount,
                paymentMethod = paymentMethod,
                accountDetail = accountDetail,
                status = "PENDING",
                transactionRef = "TXN" + (100000..999999).random()
            )
            repository.insertTransaction(txn)
            _eventFlow.emit(UIEvent.ShowMessage("Deposit of $amount coins submitted! Awaiting Admin approval."))
        }
    }

    fun depositViaQr(amount: Int, utr: String, senderUpi: String) {
        viewModelScope.launch {
            val currentUserId = _activeUserId.value
            val txn = WalletTransaction(
                userId = currentUserId,
                type = "DEPOSIT",
                amount = amount,
                title = "Deposit Request",
                paymentMethod = "UPI QR (Anil Kasde)",
                accountDetail = "UPI: $senderUpi | UTR: $utr",
                status = "PENDING",
                transactionRef = utr
            )
            repository.insertTransaction(txn)
            _eventFlow.emit(UIEvent.ShowMessage("Payment submitted! UTR: $utr. Verification in progress. Awaiting Admin Approval."))
        }
    }

    fun depositViaUpiDeepLink(amount: Int, txnRef: String, status: String, paymentDetails: String) {
        viewModelScope.launch {
            val currentUserId = _activeUserId.value
            val verifiedStatus = PaymentGatewayVerifier.verifyTransactionWithGateway(txnRef, amount)
            
            val finalStatus = when (verifiedStatus) {
                GatewayStatus.SUCCESS -> "SUCCESS"
                GatewayStatus.PENDING -> "PENDING"
                GatewayStatus.FAILED -> "FAILED"
                GatewayStatus.FAILED_TAMPERED -> {
                    _eventFlow.emit(UIEvent.ShowMessage("🚨 Security Alert: Transaction tampered or invalid! Payment blocked."))
                    return@launch
                }
            }
            
            if (finalStatus == "FAILED") {
                _eventFlow.emit(UIEvent.ShowMessage("Payment Failed. Transaction was not verified by gateway."))
                return@launch
            }
            
            val txn = WalletTransaction(
                userId = currentUserId,
                type = "DEPOSIT",
                amount = amount,
                paymentMethod = "UPI Auto-Gateway",
                accountDetail = paymentDetails,
                status = finalStatus,
                transactionRef = txnRef
            )
            repository.insertTransaction(txn)
            
            if (finalStatus == "SUCCESS") {
                val currentUser = repository.getUser(currentUserId).firstOrNull() ?: User(id = currentUserId)
                val newBalance = currentUser.coinBalance + amount
                repository.saveUserProfile(currentUser.copy(id = currentUserId, coinBalance = newBalance))
                prefs.edit().putInt("user_coins_$currentUserId", newBalance).apply()
                refreshRegisteredUsers()
                _eventFlow.emit(UIEvent.ShowMessage("Instant Deposit of $amount Coins Successful! (Verified)"))
            } else if (finalStatus == "PENDING") {
                _eventFlow.emit(UIEvent.ShowMessage("UPI Transaction Pending/Processing. Ref: $txnRef"))
            }
        }
    }

    fun approveTransaction(id: Int, quiet: Boolean = false) {
        viewModelScope.launch {
            val txn = repository.getTransactionById(id)
            if (txn != null && txn.status == "PENDING") {
                val updatedTxn = txn.copy(status = "SUCCESS")
                repository.insertTransaction(updatedTxn)
                
                val txnUserId = txn.userId.ifBlank { _activeUserId.value }
                val targetUser = repository.getUser(txnUserId).firstOrNull() ?: User(id = txnUserId)
                
                if (txn.type == "DEPOSIT") {
                    val newBalance = targetUser.coinBalance + txn.amount
                    repository.saveUserProfile(targetUser.copy(id = txnUserId, coinBalance = newBalance))
                    prefs.edit().putInt("user_coins_$txnUserId", newBalance).apply()
                    refreshRegisteredUsers()
                    if (!quiet) {
                        _eventFlow.emit(UIEvent.ShowMessage("Admin Approved: ${txn.amount} coins credited to $txnUserId!"))
                    }
                } else if (txn.type == "WITHDRAWAL") {
                    if (!quiet) {
                        _eventFlow.emit(UIEvent.ShowMessage("Admin Approved: Withdrawal of ${txn.amount} coins successful for $txnUserId!"))
                    }
                }
            }
        }
    }

    fun rejectTransaction(id: Int) {
        viewModelScope.launch {
            val txn = repository.getTransactionById(id)
            if (txn != null && txn.status == "PENDING") {
                val updatedTxn = txn.copy(status = "FAILED")
                repository.insertTransaction(updatedTxn)
                
                val txnUserId = txn.userId.ifBlank { _activeUserId.value }
                if (txn.type == "WITHDRAWAL") {
                    val targetUser = repository.getUser(txnUserId).firstOrNull() ?: User(id = txnUserId)
                    val newBalance = targetUser.coinBalance + txn.amount
                    repository.saveUserProfile(targetUser.copy(id = txnUserId, coinBalance = newBalance))
                    prefs.edit().putInt("user_coins_$txnUserId", newBalance).apply()
                    refreshRegisteredUsers()
                    _eventFlow.emit(UIEvent.ShowMessage("Admin Rejected: ${txn.amount} coins refunded to $txnUserId."))
                } else {
                    _eventFlow.emit(UIEvent.ShowMessage("Admin Rejected: Transaction marked as FAILED."))
                }
            }
        }
    }

    fun withdrawCoins(amount: Int, paymentMethod: String, accountDetail: String): Boolean {
        val currentUserId = _activeUserId.value
        val currentUser = user.value ?: User(id = currentUserId)
        if (currentUser.coinBalance < amount) {
            viewModelScope.launch {
                _eventFlow.emit(UIEvent.ShowMessage("Insufficient Coin Balance. Please Deposit Coins First."))
            }
            return false
        }
        viewModelScope.launch {
            val newBalance = currentUser.coinBalance - amount
            repository.saveUserProfile(currentUser.copy(id = currentUserId, coinBalance = newBalance))
            prefs.edit().putInt("user_coins_$currentUserId", newBalance).apply()
            refreshRegisteredUsers()
            
            val txn = WalletTransaction(
                userId = currentUserId,
                type = "WITHDRAWAL",
                amount = amount,
                title = "Withdrawal Request ($accountDetail)",
                paymentMethod = paymentMethod,
                accountDetail = accountDetail,
                status = "PENDING",
                transactionRef = "TXN" + (100000..999999).random()
            )
            repository.insertTransaction(txn)
            _eventFlow.emit(UIEvent.ShowMessage("Withdrawal of $amount coins submitted! Admin has been notified."))
        }
        return true
    }

    // 2. Booking Actions
    fun registerForMatch(
        matchId: Int,
        bookingType: String, // "Solo" or "Squad"
        teamName: String?,
        player1Name: String,
        player1Uid: String,
        player2Name: String? = null,
        player2Uid: String? = null,
        player3Name: String? = null,
        player3Uid: String? = null,
        player4Name: String? = null,
        player4Uid: String? = null,
        entryFee: Int
    ) {
        viewModelScope.launch {
            val currentUserId = _activeUserId.value
            val booking = Booking(
                matchId = matchId,
                userId = currentUserId,
                bookingType = bookingType,
                teamName = teamName,
                player1Name = player1Name,
                player1Uid = player1Uid,
                player2Name = player2Name,
                player2Uid = player2Uid,
                player3Name = player3Name,
                player3Uid = player3Uid,
                player4Name = player4Name,
                player4Uid = player4Uid,
                entryFeePaid = entryFee,
                bookedAtMillis = getSyncedTime()
            )
            
            when (val result = repository.bookSlot(booking, entryFee)) {
                is BookingResult.Success -> {
                    // Update user's profile with the details they registered with if they were blank or changed
                    user.value?.let { currentUser ->
                        if (currentUser.gameName != player1Name || currentUser.gameUid != player1Uid) {
                            repository.saveUserProfile(currentUser.copy(
                                id = currentUserId,
                                gameName = player1Name,
                                gameUid = player1Uid
                            ))
                        }
                    }

                    // Referral system: reward referrer upon friend's first successful match registration
                    if (currentUserId.isNotBlank()) {
                        val hasJoinedFirst = prefs.getBoolean("has_joined_first_match_$currentUserId", false)
                        if (!hasJoinedFirst) {
                            prefs.edit().putBoolean("has_joined_first_match_$currentUserId", true).apply()
                            
                            val referrerEmail = prefs.getString("referred_by_$currentUserId", "") ?: ""
                            val alreadyRewarded = prefs.getBoolean("referral_rewarded_$currentUserId", false)
                            if (referrerEmail.isNotBlank() && !alreadyRewarded) {
                                prefs.edit().putBoolean("referral_rewarded_$currentUserId", true).apply()
                                
                                val referrerCoins = prefs.getInt("user_coins_$referrerEmail", 1000)
                                val currentEarnings = prefs.getInt("total_referral_earnings_$referrerEmail", 0)
                                
                                val newCoins = referrerCoins + 10
                                val newEarnings = currentEarnings + 10
                                
                                prefs.edit()
                                    .putInt("user_coins_$referrerEmail", newCoins)
                                    .putInt("total_referral_earnings_$referrerEmail", newEarnings)
                                    .apply()
                                
                                // Insert transaction for the referrer
                                val txnRef = "REF" + (100000..999999).random()
                                val friendName = user.value?.gameName ?: user.value?.username ?: currentUserId
                                val refTxn = WalletTransaction(
                                    id = 0, // Auto-generate in Room
                                    userId = referrerEmail, // associate with the referrer
                                    type = "DEPOSIT",
                                    amount = 10,
                                    paymentMethod = "Referral Bonus",
                                    accountDetail = "Friend '$friendName' joined first match",
                                    status = "SUCCESS",
                                    transactionRef = txnRef,
                                    timestamp = System.currentTimeMillis()
                                )
                                repository.insertTransaction(refTxn)
                            }
                        }
                    }
                    _eventFlow.emit(UIEvent.BookingSuccess(result.message))
                }
                is BookingResult.Error -> {
                    _eventFlow.emit(UIEvent.ShowMessage(result.message))
                }
            }
        }
    }

    fun cancelRegistration(matchId: Int, entryFee: Int) {
        viewModelScope.launch {
            val currentUserId = _activeUserId.value
            when (val result = repository.cancelBooking(matchId, currentUserId, entryFee)) {
                is BookingResult.Success -> {
                    _eventFlow.emit(UIEvent.ShowMessage(result.message))
                }
                is BookingResult.Error -> {
                    _eventFlow.emit(UIEvent.ShowMessage(result.message))
                }
            }
        }
    }

    // 3. Admin Panel Actions
    fun createNewMatch(
        title: String,
        gameMode: String,
        map: String,
        dateTimeMillis: Long,
        prizePool: Int,
        entryFee: Int,
        perKillPrize: Int,
        slotsTotal: Int,
        status: String = "UPCOMING",
        category: String = "FF MAX",
        roomId: String? = null,
        roomPassword: String? = null
    ) {
        viewModelScope.launch {
            val match = Match(
                title = title,
                gameMode = gameMode,
                map = map,
                dateTimeMillis = dateTimeMillis,
                prizePool = prizePool,
                entryFee = entryFee,
                perKillPrize = perKillPrize,
                slotsTotal = slotsTotal,
                slotsBooked = 0,
                status = status,
                category = category,
                roomId = roomId.takeIf { !it.isNullOrBlank() },
                roomPassword = roomPassword.takeIf { !it.isNullOrBlank() }
            )
            repository.createMatch(match)
            _eventFlow.emit(UIEvent.ShowMessage("New $category match '$title' created!"))
        }
    }

    fun updateMatchStatus(match: Match, newStatus: String) {
        viewModelScope.launch {
            val updated = match.copy(status = newStatus)
            repository.updateMatch(updated)
            _eventFlow.emit(UIEvent.ShowMessage("Match status updated to $newStatus"))
        }
    }

    fun updateMatchRoomCredentials(match: Match, roomId: String?, roomPass: String?) {
        viewModelScope.launch {
            val updated = match.copy(roomId = roomId, roomPassword = roomPass)
            repository.updateMatch(updated)
            _eventFlow.emit(UIEvent.ShowMessage("Room credentials updated!"))
        }
    }

    fun declareMatchWinners(match: Match, first: String?, second: String?, third: String?) {
        viewModelScope.launch {
            val updated = match.copy(
                winner1Name = first,
                winner2Name = second,
                winner3Name = third,
                status = "PAST"
            )
            repository.updateMatch(updated)
            _eventFlow.emit(UIEvent.ShowMessage("Match winners declared!"))
        }
    }

    fun deleteMatch(matchId: Int) {
        viewModelScope.launch {
            repository.deleteMatch(matchId)
            _eventFlow.emit(UIEvent.ShowMessage("Match deleted successfully!"))
        }
    }

    fun forceRefreshDailyMatches() {
        viewModelScope.launch {
            repository.forceGenerateDailyMatches()
            _eventFlow.emit(UIEvent.ShowMessage("Daily Esports Auto-Matches successfully generated/reset!"))
        }
    }

    fun getSyncedTime(): Long {
        return repository.getSyncedTimeCached()
    }

    fun checkAndAutoUpdateMatchLifecycles() {
        viewModelScope.launch {
            val now = getSyncedTime()
            val currentMatches = matches.value
            
            for (match in currentMatches) {
                val isMyBooking = userBookings.value.any { it.matchId == match.id }
                
                // 1. Room ID and password available 15 minutes before Match Start Time
                if (match.status == "UPCOMING" && match.roomId != null && match.roomPassword != null) {
                    val fifteenMinsBeforeStart = match.dateTimeMillis - 15 * 60 * 1000
                    if (now >= fifteenMinsBeforeStart && now < match.dateTimeMillis) {
                        if (isMyBooking) {
                            val hasSentRoomAlert = prefs.getBoolean("room_alert_sent_${match.id}", false)
                            if (!hasSentRoomAlert) {
                                prefs.edit().putBoolean("room_alert_sent_${match.id}", true).apply()
                                showLocalNotification(
                                    title = "Room Credentials Ready! 🎫",
                                    message = "Room ID & Password for '${match.title}' are now available. View them in My Registrations."
                                )
                                _eventFlow.emit(UIEvent.ShowMessage("Room ID for '${match.title}' is now posted!"))
                            }
                        }
                    }
                }
                
                // 2. Start of Match -> Live (if started < 60 minutes ago)
                if (match.status == "UPCOMING") {
                    if (now >= match.dateTimeMillis + 60 * 60 * 1000) {
                        // Started more than 60 minutes ago -> COMPLETED/PAST
                        val updated = match.copy(status = "PAST")
                        repository.updateMatch(updated)
                        _eventFlow.emit(UIEvent.ShowMessage("Match '${match.title}' is completed!"))
                    } else if (now >= match.dateTimeMillis) {
                        // Started less than 60 minutes ago -> LIVE
                        val updated = match.copy(status = "LIVE")
                        repository.updateMatch(updated)
                        
                        if (isMyBooking) {
                            val hasSentLiveAlert = prefs.getBoolean("live_alert_sent_${match.id}", false)
                            if (!hasSentLiveAlert) {
                                prefs.edit().putBoolean("live_alert_sent_${match.id}", true).apply()
                                showLocalNotification(
                                    title = "Match is Live! 🔴",
                                    message = "'${match.title}' on ${match.map} has started! Join the Custom Room now."
                                )
                            }
                        }
                        _eventFlow.emit(UIEvent.ShowMessage("Match '${match.title}' is now LIVE!"))
                    }
                } else if (match.status == "LIVE") {
                    if (now >= match.dateTimeMillis + 60 * 60 * 1000) {
                        // Live for 60 minutes -> COMPLETED/PAST
                        val updated = match.copy(status = "PAST")
                        repository.updateMatch(updated)
                        _eventFlow.emit(UIEvent.ShowMessage("Match '${match.title}' has automatically ended and is now COMPLETED! 🏆"))
                    } else {
                        // Send live notification just in case it wasn't triggered as upcoming
                        if (isMyBooking) {
                            val hasSentLiveAlert = prefs.getBoolean("live_alert_sent_${match.id}", false)
                            if (!hasSentLiveAlert) {
                                prefs.edit().putBoolean("live_alert_sent_${match.id}", true).apply()
                                showLocalNotification(
                                    title = "Match is Live! 🔴",
                                    message = "'${match.title}' on ${match.map} has started! Join the Custom Room now."
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showLocalNotification(title: String, message: String) {
        try {
            val context = getApplication<Application>()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channelId = "match_live_alerts"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Match Live Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when registered matches become live"
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                
            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sealed class for UI events
    sealed class UIEvent {
        data class ShowMessage(val message: String) : UIEvent()
        data class BookingSuccess(val message: String) : UIEvent()
    }

    // Factory Class
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TournamentViewModel::class.java)) {
                val database = AppDatabase.getDatabase(application)
                val repository = TournamentRepository(
                    database.userDao(),
                    database.matchDao(),
                    database.bookingDao(),
                    database.transactionDao(),
                    database.supportMessageDao(),
                    database.appConfigDao()
                )
                @Suppress("UNCHECKED_CAST")
                return TournamentViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

enum class GatewayStatus {
    SUCCESS,
    PENDING,
    FAILED,
    FAILED_TAMPERED
}

object PaymentGatewayVerifier {
    /**
     * Simulates secure server-side verification of payment gateway transactions.
     * In a live app, this performs a backend API call with HMAC signature validation
     * to prevent client-side parameter tampering (such as bypassing success status).
     */
    fun verifyTransactionWithGateway(transactionRef: String, amount: Int): GatewayStatus {
        if (transactionRef.isBlank()) {
            return GatewayStatus.FAILED
        }
        
        // Block tampered/suspicious refs
        if (transactionRef.lowercase().contains("fake") || transactionRef.lowercase().contains("tamper")) {
            return GatewayStatus.FAILED_TAMPERED
        }
        
        // Secure callback validation: Let's assume only references generated securely
        // or verified by official gateway webhook are approved.
        // For simulation, transaction references starting with "SECURE_GATEWAY_SUCCESS"
        // or official secure references are approved. Others are blocked as failed/pending.
        return if (transactionRef.startsWith("SECURE_GATEWAY_SUCCESS") || transactionRef.startsWith("TXN_SECURE_VERIFIED")) {
            GatewayStatus.SUCCESS
        } else if (transactionRef.startsWith("TXN_PENDING")) {
            GatewayStatus.PENDING
        } else {
            // Unverified or QR direct UTR transactions default to FAILED_TAMPERED or FAILED
            // for gateway verification to block fake success.
            GatewayStatus.FAILED
        }
    }
}
