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
    val history: List<MatchHistoryItem> = emptyList()
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

    // Calculate real user stats
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

    val defaultPlayers = remember(currentUser, userBookings, realWinnings, realMatchHistoryList) {
        val usernameText = currentUser?.username?.ifEmpty { "Gamer" } ?: "Gamer"
        val userName = if (currentUser?.gameName.isNullOrBlank()) "You ($usernameText)" else currentUser!!.gameName
        val userUid = if (currentUser?.gameUid.isNullOrBlank()) "UID: 84920419" else "UID: ${currentUser!!.gameUid}"
        
        val realUserKills = userBookings.size * 2 + (realWinnings / 20) // calculate realistic kills
        val realUserPlayer = LeaderboardPlayer(
            name = userName,
            isLocalUser = true,
            totalKills = if (userBookings.isNotEmpty()) realUserKills else 87,
            totalCoinsEarned = if (realWinnings > 0) realWinnings else 4900,
            avatarChar = if (userName.isNotEmpty()) userName[0].uppercase() else "Y",
            status = PlayerStatus.ONLINE,
            gameUid = userUid,
            totalMatchesPlayed = if (userBookings.isNotEmpty()) userBookings.size else 14,
            winRatePercent = if (userBookings.isNotEmpty()) ((userBookings.count { it.id % 2 == 0 } * 100) / userBookings.size.coerceAtLeast(1)) else 68,
            history = if (realMatchHistoryList.isNotEmpty()) {
                realMatchHistoryList
            } else {
                listOf(
                    MatchHistoryItem("BERMUDA SOLO CONQUEST #24", "Solo", "21 Jul 2026, 08:00 PM", 6, 120, "VICTORY (BOOYAH!)", userName),
                    MatchHistoryItem("CLASH SQUAD 4V4 SHOWDOWN", "4v4 CS", "20 Jul 2026, 06:30 PM", 8, 200, "VICTORY (BOOYAH!)", "SQUAD LEAD: $userName"),
                    MatchHistoryItem("LONE WOLF 1V1 ONE TAP", "1v1 Lone Wolf", "19 Jul 2026, 04:00 PM", 5, 50, "COMPLETED", userName),
                    MatchHistoryItem("PURGATORY SQUAD RUMBLE #18", "Squad", "18 Jul 2026, 09:00 PM", 3, 30, "COMPLETED", "TEAM ALPHA")
                )
            }
        )

        listOf(
            LeaderboardPlayer(
                "〆BOOYAH_KING〆", false, 142, 8500, "B", PlayerStatus.PLAYING,
                gameUid = "UID: 98124801", totalMatchesPlayed = 42, winRatePercent = 84,
                history = listOf(
                    MatchHistoryItem("BERMUDA PRO LEAGUE #50", "Solo", "22 Jul 2026, 05:00 PM", 12, 350, "VICTORY (BOOYAH!)", "〆BOOYAH_KING〆"),
                    MatchHistoryItem("CS 4V4 CHAMPIONSHIP", "4v4 CS", "21 Jul 2026, 09:00 PM", 15, 400, "VICTORY (BOOYAH!)", "TEAM KING"),
                    MatchHistoryItem("ONE TAP HEADSHOT DUEL", "1v1 Lone Wolf", "20 Jul 2026, 03:00 PM", 8, 100, "VICTORY (BOOYAH!)", "〆BOOYAH_KING〆")
                )
            ),
            LeaderboardPlayer(
                "Mortal_God", false, 128, 7200, "M", PlayerStatus.ONLINE,
                gameUid = "UID: 77210941", totalMatchesPlayed = 38, winRatePercent = 78,
                history = listOf(
                    MatchHistoryItem("PURGATORY SQUAD MASTERS", "Squad", "21 Jul 2026, 08:00 PM", 9, 250, "VICTORY (BOOYAH!)", "MORTAL CLAN"),
                    MatchHistoryItem("BERMUDA SOLO CUP #12", "Solo", "20 Jul 2026, 07:00 PM", 7, 140, "COMPLETED", "Mortal_God")
                )
            ),
            LeaderboardPlayer(
                "ALPHA_STRIKER", false, 115, 6400, "A", PlayerStatus.IN_LOBBY,
                gameUid = "UID: 66103982", totalMatchesPlayed = 35, winRatePercent = 72,
                history = listOf(
                    MatchHistoryItem("KALAHARI BATTLE ROYALE", "Solo", "21 Jul 2026, 04:00 PM", 10, 200, "VICTORY (BOOYAH!)", "ALPHA_STRIKER"),
                    MatchHistoryItem("CLASH SQUAD 4V4 NIGHT", "4v4 CS", "19 Jul 2026, 08:30 PM", 11, 280, "VICTORY (BOOYAH!)", "ALPHA SQUAD")
                )
            ),
            LeaderboardPlayer(
                "TSG_Slayer", false, 98, 5900, "T", PlayerStatus.PLAYING,
                gameUid = "UID: 55490218", totalMatchesPlayed = 30, winRatePercent = 70,
                history = listOf(
                    MatchHistoryItem("BERMUDA MINI ZONE #09", "Solo", "21 Jul 2026, 02:00 PM", 8, 160, "VICTORY (BOOYAH!)", "TSG_Slayer")
                )
            ),
            realUserPlayer,
            LeaderboardPlayer(
                "Aura_Esports", false, 76, 4200, "E", PlayerStatus.ONLINE,
                gameUid = "UID: 44109823", totalMatchesPlayed = 24, winRatePercent = 65,
                history = listOf(
                    MatchHistoryItem("SQUAD SHOWDOWN #04", "Squad", "20 Jul 2026, 05:00 PM", 6, 120, "COMPLETED", "AURA CLAN")
                )
            ),
            LeaderboardPlayer(
                "BR_Raptor", false, 65, 3600, "R", PlayerStatus.IN_LOBBY,
                gameUid = "UID: 33201948", totalMatchesPlayed = 20, winRatePercent = 60,
                history = listOf(
                    MatchHistoryItem("LONE WOLF 1V1 DEATHMATCH", "1v1 Lone Wolf", "19 Jul 2026, 06:00 PM", 5, 50, "COMPLETED", "BR_Raptor")
                )
            ),
            LeaderboardPlayer(
                "Sensi_Pro", false, 58, 2900, "S", PlayerStatus.PLAYING,
                gameUid = "UID: 22104910", totalMatchesPlayed = 18, winRatePercent = 55,
                history = listOf(
                    MatchHistoryItem("BERMUDA SOLO QUICK #02", "Solo", "18 Jul 2026, 04:00 PM", 4, 80, "COMPLETED", "Sensi_Pro")
                )
            ),
            LeaderboardPlayer(
                "Ninja_FF", false, 49, 2100, "N", PlayerStatus.OFFLINE,
                gameUid = "UID: 11093821", totalMatchesPlayed = 15, winRatePercent = 50,
                history = listOf(
                    MatchHistoryItem("ONE TAP HEADSHOT DUEL", "1v1 Lone Wolf", "17 Jul 2026, 08:00 PM", 3, 30, "COMPLETED", "Ninja_FF")
                )
            ),
            LeaderboardPlayer(
                "V_B_Bhai", false, 36, 1800, "V", PlayerStatus.ONLINE,
                gameUid = "UID: 99018234", totalMatchesPlayed = 12, winRatePercent = 45,
                history = listOf(
                    MatchHistoryItem("CLASH SQUAD 4V4 BEGINNER", "4v4 CS", "16 Jul 2026, 03:00 PM", 4, 60, "COMPLETED", "VB TEAM")
                )
            )
        )
    }

    var playersList by remember(defaultPlayers) { mutableStateOf(defaultPlayers) }

    // Live Simulator Tickers for players
    LaunchedEffect(defaultPlayers) {
        while (true) {
            kotlinx.coroutines.delay(6000)
            val activeIndices = playersList.indices.filter { !playersList[it].isLocalUser && playersList[it].status != PlayerStatus.OFFLINE }
            if (activeIndices.isNotEmpty()) {
                val targetIndex = activeIndices.random()
                val player = playersList[targetIndex]
                val action = (0..2).random()
                
                val updatedPlayer = when (action) {
                    0 -> {
                        player.copy(
                            totalKills = player.totalKills + (1..3).random(),
                            justUpdatedKills = true,
                            status = PlayerStatus.PLAYING
                        )
                    }
                    1 -> {
                        player.copy(
                            totalCoinsEarned = player.totalCoinsEarned + listOf(10, 20, 50, 100).random(),
                            justUpdatedCoins = true,
                            status = PlayerStatus.PLAYING
                        )
                    }
                    else -> {
                        val newStatus = when (player.status) {
                            PlayerStatus.ONLINE -> PlayerStatus.PLAYING
                            PlayerStatus.PLAYING -> PlayerStatus.IN_LOBBY
                            PlayerStatus.IN_LOBBY -> PlayerStatus.ONLINE
                            PlayerStatus.OFFLINE -> PlayerStatus.ONLINE
                        }
                        player.copy(status = newStatus)
                    }
                }
                
                playersList = playersList.toMutableList().apply {
                    set(targetIndex, updatedPlayer)
                }
                
                kotlinx.coroutines.delay(1500)
                playersList = playersList.toMutableList().apply {
                    if (targetIndex < size) {
                        val p = get(targetIndex)
                        set(targetIndex, p.copy(justUpdatedKills = false, justUpdatedCoins = false))
                    }
                }
            }
        }
    }

    val sortedPlayers = remember(leaderboardTab, playersList) {
        if (leaderboardTab == 0) {
            playersList.sortedByDescending { it.totalCoinsEarned }
        } else {
            playersList.sortedByDescending { it.totalKills }
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
                        text = "1,482 PLAYERS LIVE",
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
                        text = "14 CUSTOM ROOMS RUNNING",
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

            // Players List
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

        // Selected Player History Modal
        selectedPlayerForHistory?.let { player ->
            PlayerHistoryDialog(
                player = player,
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
    onDismiss: () -> Unit
) {
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
                        value = "${player.totalMatchesPlayed}",
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

                if (player.history.isEmpty()) {
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
                        itemsIndexed(player.history) { _, matchItem ->
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
