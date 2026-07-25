package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Match
import com.example.ui.theme.*
import com.example.ui.viewmodel.TournamentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentMatchesScreen(
    viewModel: TournamentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRegister: (matchId: Int) -> Unit,
    onNavigateToWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by viewModel.user.collectAsState()
    val allMatches by viewModel.matches.collectAsState()
    val myBookings by viewModel.userBookings.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Upcoming 📅", "Live 🔴", "Completed 🏆")

    var insufficientCoinsMatch by remember { mutableStateOf<Match?>(null) }

    // Filter strictly for category == 'Tournament' (or title containing 'Tournament')
    val tournamentMatches = remember(allMatches) {
        allMatches.filter { match ->
            match.category.equals("Tournament", ignoreCase = true) ||
            match.title.contains("Tournament", ignoreCase = true)
        }
    }

    val filteredMatches = remember(tournamentMatches, selectedTab) {
        val now = System.currentTimeMillis()
        tournamentMatches.filter { match ->
            val effectiveStatus = when {
                match.status == "LIVE" -> "LIVE"
                match.status == "PAST" -> "PAST"
                match.dateTimeMillis > now -> "UPCOMING"
                now - match.dateTimeMillis in 0..3600000 -> "LIVE"
                else -> "PAST"
            }
            when (selectedTab) {
                0 -> effectiveStatus == "UPCOMING"
                1 -> effectiveStatus == "LIVE"
                else -> effectiveStatus == "PAST"
            }
        }.sortedBy { if (selectedTab == 2) -it.dateTimeMillis else it.dateTimeMillis }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = GoldBooyah,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TOURNAMENT MATCHES",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Admin-Controlled Championship Events",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Coin balance pill in top bar
                    Surface(
                        onClick = onNavigateToWallet,
                        color = SlateDarkSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldBooyah.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "🪙", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${user?.coinBalance ?: 0}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GoldBooyah
                            )
                        }
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
        ) {
            // User Wallet Header Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(1.dp, GoldBooyah.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldBooyah.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🪙", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WALLET BALANCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${user?.coinBalance ?: 0} Coins",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldBooyah
                            )
                        }
                    }
                    Button(
                        onClick = onNavigateToWallet,
                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ADD COINS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(SlateDarkSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) FireOrange else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Tournament Matches List
            if (filteredMatches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GoldBooyah.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = GoldBooyah,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "NO TOURNAMENTS AVAILABLE",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "There are currently no matches under the 'Tournament' category in this view. Check back soon for official admin announcements!",
                            textAlign = TextAlign.Center,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMatches, key = { it.id }) { match ->
                        val isRegistered = myBookings.any { it.matchId == match.id }
                        TournamentMatchCard(
                            match = match,
                            isRegistered = isRegistered,
                            onJoinClick = {
                                val userCoins = user?.coinBalance ?: 0
                                if (userCoins >= match.entryFee) {
                                    onNavigateToRegister(match.id)
                                } else {
                                    insufficientCoinsMatch = match
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Insufficient Coins Dialog
    insufficientCoinsMatch?.let { match ->
        val userBalance = user?.coinBalance ?: 0
        val required = match.entryFee
        val needed = required - userBalance

        AlertDialog(
            onDismissRequest = { insufficientCoinsMatch = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = FireOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INSUFFICIENT COIN BALANCE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "To join '${match.title}', an entry fee of $required Coins is required.",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your Balance:", fontSize = 12.sp, color = TextSecondary)
                                Text("$userBalance Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldBooyah)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Entry Fee:", fontSize = 12.sp, color = TextSecondary)
                                Text("$required Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FireOrange)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = SlateDarkBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Coins Needed:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("+$needed Coins", fontSize = 12.sp, fontWeight = FontWeight.Black, color = FireOrangeLight)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please deposit coins into your wallet to complete your tournament entry.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        insufficientCoinsMatch = null
                        onNavigateToWallet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("GO TO MY WALLET", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { insufficientCoinsMatch = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = SlateDarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun TournamentMatchCard(
    match: Match,
    isRegistered: Boolean,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDateTime = remember(match.dateTimeMillis) {
        val dateObj = Date(match.dateTimeMillis)
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.US).apply { timeZone = tz }
        sdf.format(dateObj)
    }

    val now = System.currentTimeMillis()
    val effectiveStatus = remember(match.dateTimeMillis, match.status, now) {
        when {
            match.status == "LIVE" -> "LIVE"
            match.status == "PAST" -> "PAST"
            match.dateTimeMillis > now -> "UPCOMING"
            now - match.dateTimeMillis in 0..3600000 -> "LIVE"
            else -> "PAST"
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (effectiveStatus == "LIVE") FireOrange else GoldBooyah.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Header Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Badge
                Surface(
                    color = GoldBooyah.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldBooyah.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GoldBooyah,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (match.category.ifBlank { "TOURNAMENT" }).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldBooyah,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = when (effectiveStatus) {
                        "LIVE" -> Color.Red.copy(alpha = 0.2f)
                        "UPCOMING" -> FireOrange.copy(alpha = 0.15f)
                        else -> SlateDarkBg
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (effectiveStatus) {
                            "LIVE" -> "🔴 LIVE NOW"
                            "UPCOMING" -> "⏰ UPCOMING"
                            else -> "🏆 COMPLETED"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (effectiveStatus) {
                            "LIVE" -> Color.Red
                            "UPCOMING" -> FireOrangeLight
                            else -> TextSecondary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Match Title
            Text(
                text = match.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Date & Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedDateTime,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Map & Mode
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = FireOrangeLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Map: ${match.map} • Mode: ${match.gameMode}",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SlateDarkBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Grid Row (Prize, Fee, Per Kill, Slots)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Prize Pool
                Column(horizontalAlignment = Alignment.Start) {
                    Text("PRIZE POOL", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextSecondary)
                    Text("🏆 ${match.prizePool} Coins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldBooyah)
                }

                // Entry Fee
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ENTRY FEE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextSecondary)
                    Text(
                        if (match.entryFee == 0) "FREE" else "🪙 ${match.entryFee} Coins",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (match.entryFee == 0) Color.Green else FireOrangeLight
                    )
                }

                // Per Kill
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PER KILL", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextSecondary)
                    Text("🎯 ${match.perKillPrize} Coins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                // Slots Booked
                Column(horizontalAlignment = Alignment.End) {
                    Text("SLOTS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextSecondary)
                    Text("👥 ${match.slotsBooked}/${match.slotsTotal}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            // Room ID / Password Section if match is live/upcoming & available
            if (!match.roomId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = SlateDarkBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateDarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = GoldBooyah, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Room ID: ${match.roomId}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        match.roomPassword?.let { pass ->
                            Text(text = "Pass: $pass", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldBooyah)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // JOIN NOW Action Button
            if (isRegistered) {
                Button(
                    onClick = { /* Registered state indicator */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A2B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REGISTERED FOR TOURNAMENT", fontWeight = FontWeight.Bold, color = Color.Green)
                }
            } else if (effectiveStatus == "UPCOMING") {
                Button(
                    onClick = onJoinClick,
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("JOIN NOW", fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 0.5.sp)
                }
            } else if (effectiveStatus == "LIVE") {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("TOURNAMENT IN PROGRESS", fontWeight = FontWeight.Bold, color = Color.Red)
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("TOURNAMENT COMPLETED", fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        }
    }
}
