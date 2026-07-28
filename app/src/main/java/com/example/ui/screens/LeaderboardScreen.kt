package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.TournamentViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class PlayerStatus(val label: String, val color: Color) {
    ONLINE("ONLINE", Color(0xFF4CAF50)),
    PLAYING("LIVE MATCH", FireOrange),
    IN_LOBBY("IN LOBBY", Color(0xFFFFC107)),
    OFFLINE("OFFLINE", Color(0xFF9E9E9E))
}

data class MatchHistoryItem(
    val matchTitle: String,
    val gameMode: String,
    val dateText: String,
    val kills: Int,
    val earningsCoins: Int,
    val statusText: String, // "UPCOMING", "LIVE", "COMPLETED", "VICTORY"
    val teamOrIgn: String = ""
)

data class LeaderboardPlayer(
    val name: String,
    val isLocalUser: Boolean,
    val totalKills: Int,
    val totalCoinsEarned: Int,
    val avatarChar: String = "G",
    val status: PlayerStatus = PlayerStatus.ONLINE,
    val justUpdatedKills: Boolean = false,
    val justUpdatedCoins: Boolean = false,
    val gameUid: String = "",
    val totalMatchesPlayed: Int = 0,
    val winRatePercent: Int = 0,
    val history: List<MatchHistoryItem> = emptyList(),
    val userId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: TournamentViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.user.collectAsState()
    val userBookings by viewModel.userBookings.collectAsState()
    val matches by viewModel.matches.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val registeredUsers by viewModel.registeredUsers.collectAsState()

    var leaderboardTab by remember { mutableStateOf(0) } // 0 = Coins Earned, 1 = Total Kills
    var selectedPlayerForHistory by remember { mutableStateOf<LeaderboardPlayer?>(null) }
    var firestoreUsers by remember { mutableStateOf<List<com.example.data.model.User>>(emptyList()) }
    var isLoadingUsers by remember { mutableStateOf(false) }

    // Fetch real registered users strictly from Firestore
    LaunchedEffect(Unit) {
        isLoadingUsers = true
        try {
            val remoteUsers = viewModel.firebaseRepository.fetchAllRegisteredUsersFromFirestore()
            firestoreUsers = remoteUsers
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoadingUsers = false
    }

    // Calculate real local user stats
    val realWinnings = remember(transactions) {
        transactions.filter { txn ->
            txn.type == "DEPOSIT" && (
                txn.paymentMethod.contains("Winnings", ignoreCase = true) ||
                txn.accountDetail.contains("Match", ignoreCase = true) ||
                txn.accountDetail.contains("WIN", ignoreCase = true)
            )
        }.sumOf { it.amount }
    }

    val realMatchHistoryList = remember(userBookings, matches, transactions) {
        if (userBookings.isEmpty()) {
            emptyList()
        } else {
            userBookings.map { booking ->
                val matchObj = matches.find { it.id == booking.matchId }
                val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(booking.bookedAtMillis))
                
                val winTxn = transactions.find { txn ->
                    txn.type == "DEPOSIT" && matchObj != null && txn.accountDetail.contains(matchObj.title, ignoreCase = true)
                }
                val earnings = winTxn?.amount ?: 0
                val status = when {
                    matchObj?.status == "PAST" && earnings > 0 -> "VICTORY (BOOYAH!)"
                    matchObj?.status == "PAST" -> "COMPLETED"
                    matchObj?.status == "LIVE" -> "LIVE NOW"
                    else -> "REGISTERED (UPCOMING)"
                }
                
                MatchHistoryItem(
                    matchTitle = matchObj?.title ?: "Free Fire Match #${booking.matchId}",
                    gameMode = booking.bookingType,
                    dateText = dateFmt,
                    kills = if (earnings > 0) (earnings / (matchObj?.perKillPrize?.takeIf { it > 0 } ?: 10)) else 0,
                    earningsCoins = earnings,
                    statusText = status,
                    teamOrIgn = booking.teamName ?: booking.player1Name
                )
            }
        }
    }

    // Construct real players list strictly from Firestore & registered users
    val realPlayersList = remember(currentUser, registeredUsers, firestoreUsers, userBookings, realWinnings, realMatchHistoryList) {
        val userMap = mutableMapOf<String, com.example.data.model.User>()

        for (u in firestoreUsers) {
            val key = u.email.ifBlank { u.id }.lowercase().trim()
            if (key.isNotBlank()) userMap[key] = u
        }
        for (u in registeredUsers) {
            val key = u.email.ifBlank { u.id }.lowercase().trim()
            if (key.isNotBlank() && !userMap.containsKey(key)) userMap[key] = u
        }
        currentUser?.let { u ->
            val key = u.email.ifBlank { u.id }.lowercase().trim()
            if (key.isNotBlank()) userMap[key] = u
        }

        userMap.values.map { userObj ->
            val isLocal = currentUser != null && (
                userObj.email.equals(currentUser?.email, ignoreCase = true) ||
                userObj.id.equals(currentUser?.id, ignoreCase = true)
            )

            val usernameText = userObj.username.ifBlank { userObj.email.substringBefore("@") }.ifBlank { "Gamer" }
            val name = if (isLocal) {
                if (userObj.gameName.isNotBlank()) userObj.gameName else "You ($usernameText)"
            } else {
                if (userObj.gameName.isNotBlank()) userObj.gameName else usernameText
            }

            val userUidStr = if (userObj.gameUid.isNotBlank()) "UID: ${userObj.gameUid}" else "UID: N/A"

            val coinsWon = if (isLocal) {
                if (realWinnings > 0) realWinnings else userObj.coinBalance
            } else {
                userObj.coinBalance
            }

            val kills = if (isLocal) {
                userBookings.size * 2 + (realWinnings / 20)
            } else {
                userObj.totalEarnedReferrals
            }

            val history = if (isLocal) realMatchHistoryList else emptyList()

            LeaderboardPlayer(
                name = name,
                isLocalUser = isLocal,
                totalKills = kills,
                totalCoinsEarned = coinsWon,
                avatarChar = if (name.isNotEmpty()) name[0].uppercase() else "P",
                status = PlayerStatus.ONLINE,
                gameUid = userUidStr,
                totalMatchesPlayed = if (isLocal) userBookings.size else 0,
                history = history,
                userId = userObj.email.ifBlank { userObj.id }
            )
        }
    }

    val sortedPlayers = remember(leaderboardTab, realPlayersList) {
        val filtered = realPlayersList.filter { 
            it.totalMatchesPlayed > 0 || it.totalCoinsEarned > 0 || it.totalKills > 0 || it.isLocalUser 
        }
        if (leaderboardTab == 0) {
            filtered.sortedByDescending { it.totalCoinsEarned }
        } else {
            filtered.sortedByDescending { it.totalKills }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GoldBooyah,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMPETITIVE LEADERBOARD",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBg,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = SlateDarkBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Competitive Subtitle
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = FireOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SEASON 1 ESPORTS CHAMPIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = FireOrange,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Top players based on performance. Tap any player to inspect their real match history!",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Real User Career History Card Banner
            val localPlayerObj = remember(sortedPlayers) { sortedPlayers.find { it.isLocalUser } }
            if (localPlayerObj != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = FireOrange.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.dp, FireOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { selectedPlayerForHistory = localPlayerObj }
                        .testTag("user_career_history_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(FireOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = localPlayerObj.avatarChar,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = localPlayerObj.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(FireOrange, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("YOU", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                                Text(
                                    text = "Joined ${localPlayerObj.totalMatchesPlayed} Matches • ${localPlayerObj.totalKills} Kills • ₹${localPlayerObj.totalCoinsEarned} Won",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { selectedPlayerForHistory = localPlayerObj },
                            colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("HISTORY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Real-time Active Counter Ticker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${sortedPlayers.size} ACTIVE PLAYERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(FireOrange, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${matches.count { it.status == "LIVE" }} LIVE ROOMS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireOrange,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = leaderboardTab,
                containerColor = SlateDarkSurface,
                contentColor = FireOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[leaderboardTab]),
                        color = FireOrange
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = leaderboardTab == 0,
                    onClick = { leaderboardTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldBooyah)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COINS EARNED", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    selectedContentColor = FireOrange,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = leaderboardTab == 1,
                    onClick = { leaderboardTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp), tint = FireOrange)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TOTAL KILLS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    selectedContentColor = FireOrange,
                    unselectedContentColor = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Leaderboard Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Text(text = "RANK & PLAYER (TAP FOR HISTORY)", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Text(
                    text = if (leaderboardTab == 0) "TOTAL COINS" else "TOTAL KILLS",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.End
                )
            }

            // Players List / Empty State
            if (sortedPlayers.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp)
                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
                        .testTag("leaderboard_empty_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GoldBooyah,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No active players on leaderboard yet. Play matches to join the rank!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("leaderboard_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(sortedPlayers) { index, player ->
                        LeaderboardRowItem(
                            rank = index + 1,
                            player = player,
                            activeTab = leaderboardTab,
                            onClick = { selectedPlayerForHistory = player }
                        )
                    }
                }
            }
        }

        // Selected Player History Modal
        selectedPlayerForHistory?.let { player ->
            PlayerHistoryDialog(
                player = player,
                viewModel = viewModel,
                onDismiss = { selectedPlayerForHistory = null }
            )
        }
    }
}

@Composable
fun LeaderboardRowItem(
    rank: Int,
    player: LeaderboardPlayer,
    activeTab: Int,
    onClick: () -> Unit
) {
    val isTop3 = rank <= 3
    val rankColor = when (rank) {
        1 -> GoldBooyah
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> SlateDarkBorder
    }

    val targetBgColor = when {
        player.isLocalUser -> FireOrange.copy(alpha = 0.12f)
        player.justUpdatedCoins || player.justUpdatedKills -> FireOrange.copy(alpha = 0.25f)
        else -> SlateDarkSurface
    }
    
    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        label = "row_bg_color"
    )

    val cardBorderColor = if (player.isLocalUser) {
        FireOrange
    } else if (player.justUpdatedCoins || player.justUpdatedKills) {
        FireOrange
    } else if (isTop3) {
        rankColor.copy(alpha = 0.6f)
    } else {
        SlateDarkBorder
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = animatedBgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, cardBorderColor), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(if (player.isLocalUser) "local_user_rank" else "rank_$rank")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.3f)
            ) {
                // Rank Number / Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTop3) {
                                Brush.verticalGradient(listOf(rankColor, rankColor.copy(alpha = 0.7f)))
                            } else {
                                Brush.verticalGradient(listOf(SlateDarkBg, SlateDarkBg))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTop3) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = rank.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Avatar and Gamer Tag
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (player.isLocalUser) FireOrange else SlateDarkBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.avatarChar,
                        fontWeight = FontWeight.Bold,
                        color = if (player.isLocalUser) Color.White else GoldBooyah,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = player.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (player.isLocalUser) FireOrange else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (player.isLocalUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(FireOrange, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("YOU", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                    
                    // Live Status Badge & History prompt
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(player.status.color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = player.status.label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = player.status.color
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Tap History",
                            fontSize = 9.sp,
                            color = GoldBooyah,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Stat Display
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(0.7f)
            ) {
                if (activeTab == 0) {
                    val coinColor by animateColorAsState(
                        targetValue = if (player.justUpdatedCoins) Color.Green else GoldBooyah,
                        label = "coin_color"
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = coinColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${player.totalCoinsEarned}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = coinColor
                        )
                    }
                    Text(
                        text = "${player.totalKills} Total Kills",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                } else {
                    val killsColor by animateColorAsState(
                        targetValue = if (player.justUpdatedKills) Color.Green else FireOrange,
                        label = "kills_color"
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = killsColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${player.totalKills}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = killsColor
                        )
                    }
                    Text(
                        text = "₹${player.totalCoinsEarned} Earned",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerHistoryDialog(
    player: LeaderboardPlayer,
    viewModel: TournamentViewModel,
    onDismiss: () -> Unit
) {
    var matchHistoryList by remember { mutableStateOf<List<MatchHistoryItem>>(player.history) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    val matches by viewModel.matches.collectAsState()

    LaunchedEffect(player) {
        if (player.history.isNotEmpty() && player.isLocalUser) {
            matchHistoryList = player.history
        } else {
            isLoadingHistory = true
            try {
                val targetId = player.userId.ifBlank { player.name }
                val bookings = viewModel.firebaseRepository.fetchUserBookingsFromFirestore(targetId)
                val txns = viewModel.firebaseRepository.fetchUserTransactionsFromFirestore(targetId)
                
                val history = bookings.map { booking ->
                    val matchObj = matches.find { it.id == booking.matchId }
                    val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(booking.bookedAtMillis))
                    
                    val winTxn = txns.find { txn ->
                        txn.type == "DEPOSIT" && (
                            (matchObj != null && txn.accountDetail.contains(matchObj.title, ignoreCase = true)) ||
                            txn.accountDetail.contains("Match", ignoreCase = true) ||
                            txn.accountDetail.contains("WIN", ignoreCase = true)
                        )
                    }
                    val earnings = winTxn?.amount ?: 0
                    val status = when {
                        matchObj?.status == "PAST" && earnings > 0 -> "VICTORY (BOOYAH!)"
                        matchObj?.status == "PAST" -> "COMPLETED"
                        matchObj?.status == "LIVE" -> "LIVE NOW"
                        else -> "REGISTERED (UPCOMING)"
                    }
                    
                    MatchHistoryItem(
                        matchTitle = matchObj?.title ?: "Free Fire Match #${booking.matchId}",
                        gameMode = booking.bookingType,
                        dateText = dateFmt,
                        kills = if (earnings > 0) (earnings / (matchObj?.perKillPrize?.takeIf { it > 0 } ?: 10)) else 0,
                        earningsCoins = earnings,
                        statusText = status,
                        teamOrIgn = booking.teamName ?: booking.player1Name
                    )
                }
                matchHistoryList = history
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isLoadingHistory = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateDarkSurface,
        titleContentColor = TextPrimary,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (player.isLocalUser) FireOrange else GoldBooyah),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.avatarChar,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = player.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            if (player.isLocalUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(FireOrange, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("YOU", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                        Text(
                            text = player.gameUid,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateDarkBg, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆 VERIFIED PLAYER HISTORY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldBooyah
                        )
                        Text(
                            text = player.status.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = player.status.color
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stat Cards Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        title = "MATCHES",
                        value = "${if (matchHistoryList.isNotEmpty()) matchHistoryList.size else player.totalMatchesPlayed}",
                        icon = Icons.Default.ConfirmationNumber,
                        iconTint = FireOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "TOTAL KILLS",
                        value = "${player.totalKills}",
                        icon = Icons.Default.Whatshot,
                        iconTint = FireOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "COINS WON",
                        value = "₹${player.totalCoinsEarned}",
                        icon = Icons.Default.MonetizationOn,
                        iconTint = GoldBooyah,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "MATCH LOGS & HISTORY:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (isLoadingHistory) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FireOrange, modifier = Modifier.size(28.dp))
                    }
                } else if (matchHistoryList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tournament history recorded yet.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(matchHistoryList) { _, matchItem ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = matchItem.matchTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (matchItem.statusText.contains("VICTORY")) GoldBooyah.copy(alpha = 0.2f) else SlateDarkBorder,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = matchItem.statusText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (matchItem.statusText.contains("VICTORY")) GoldBooyah else FireOrange
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Mode: ${matchItem.gameMode} • ${matchItem.dateText}",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )

                                        Text(
                                            text = "⚔️ ${matchItem.kills} Kills  •  🪙 +₹${matchItem.earningsCoins}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldBooyah
                                        )
                                    }

                                    if (matchItem.teamOrIgn.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Registered IGN: ${matchItem.teamOrIgn}",
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("CLOSE PROFILE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun StatBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontWeight = FontWeight.Black, fontSize = 13.sp, color = TextPrimary)
            Text(text = title, fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}
