package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Match
import com.example.ui.theme.*
import com.example.ui.viewmodel.TournamentViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.content.Intent
import android.net.Uri
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: TournamentViewModel,
    onNavigateToRegister: (Int) -> Unit,
    onNavigateToBookingDetail: (Int) -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToTournamentMatches: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val matches by viewModel.matches.collectAsState()
    val userBookings by viewModel.userBookings.collectAsState()
    val user by viewModel.user.collectAsState()
    val announcement by viewModel.announcement.collectAsState()
    val emergencyNotification by viewModel.emergencyNotification.collectAsState()
    var showEmergencyDialog by remember(emergencyNotification) { mutableStateOf(emergencyNotification.isNotEmpty()) }
    val context = LocalContext.current
    
    var showInsufficientCoinsDialog by remember { mutableStateOf(false) }
    var matchToJoin by remember { mutableStateOf<Match?>(null) }
    var showWatchLiveStream by remember { mutableStateOf(false) }
    var showSupportOptionsSheet by remember { mutableStateOf(false) }
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Upcoming 📅", "Live 🔴", "Completed 🏆")

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("FF MAX") }

    val filteredMatches = remember(matches, selectedTab, searchQuery, selectedCategory) {
        val now = viewModel.getSyncedTime()
        val list = matches.filter { match ->
            val effectiveStatus = when {
                match.status == "LIVE" -> "LIVE"
                match.status == "PAST" -> "PAST"
                match.dateTimeMillis > now -> "UPCOMING"
                now - match.dateTimeMillis in 0..3600000 -> "LIVE"
                else -> "PAST"
            }
            val targetStatus = when (selectedTab) {
                0 -> "UPCOMING"
                1 -> "LIVE"
                else -> "PAST"
            }
            
            val matchesStatus = effectiveStatus == targetStatus
            val matchesSearch = searchQuery.isEmpty() || 
                match.title.contains(searchQuery, ignoreCase = true) || 
                match.map.contains(searchQuery, ignoreCase = true)
                
            val matchesCategory = when (selectedCategory) {
                "FF MAX" -> match.category.equals("FF MAX", ignoreCase = true) || match.title.contains("FF MAX", ignoreCase = true) || match.title.contains("FF_MAX", ignoreCase = true)
                "Lone Wolf" -> match.category.equals("Lone Wolf", ignoreCase = true) || match.gameMode.equals("Solo", ignoreCase = true) || match.title.contains("Lone Wolf", ignoreCase = true)
                "One Tap" -> match.category.equals("One Tap", ignoreCase = true) || match.title.contains("One Tap", ignoreCase = true) || match.title.contains("Headshot", ignoreCase = true)
                "Clash Squad" -> match.category.equals("Clash Squad", ignoreCase = true) || match.gameMode.equals("Squad", ignoreCase = true) || match.title.contains("Clash Squad", ignoreCase = true)
                "Full Map" -> match.category.equals("Full Map", ignoreCase = true) || listOf("Bermuda", "Purgatory", "Kalahari").any { it.equals(match.map, ignoreCase = true) }
                "Tournament", "Custom Mode" -> match.category.equals("Tournament", ignoreCase = true) || match.title.contains("Tournament", ignoreCase = true) || match.title.contains("Custom", ignoreCase = true)
                else -> true
            }
            
            matchesStatus && matchesSearch && matchesCategory
        }
        if (selectedTab == 2) {
            list.sortedByDescending { it.dateTimeMillis }
        } else {
            list.sortedBy { it.dateTimeMillis }
        }
    }

    Scaffold(
        containerColor = SlateDarkBg,
        modifier = modifier
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        
        // Convert header height DP to PX
        val density = androidx.compose.ui.platform.LocalDensity.current
        val headerHeightDp = if (announcement.isNotEmpty()) 230.dp else 176.dp
        val headerHeightPx = with(density) { headerHeightDp.toPx() }
        
        // Calculate yOffset and collapse fraction
        val yOffsetPx by remember(scrollState) {
            derivedStateOf {
                (-scrollState.value.toFloat()).coerceIn(-headerHeightPx, 0f)
            }
        }
        
        val collapseFraction by remember(scrollState) {
            derivedStateOf {
                if (headerHeightPx > 0f) {
                    (scrollState.value.toFloat() / headerHeightPx).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // 1. Scrollable List of Tournaments / Matches
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Spacer at the top to keep matches below the collapsing header when scrolled to top
                Spacer(
                    modifier = Modifier
                        .statusBarsPadding()
                        .height(headerHeightDp)
                )

                // "eSport Games" Category Grid
                GameCategoryGrid(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { cat ->
                        selectedCategory = cat
                        if (cat.equals("Tournament", ignoreCase = true)) {
                            onNavigateToTournamentMatches()
                        }
                    }
                )

                // Tab Bar
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SlateDarkBg,
                    contentColor = FireOrange,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = FireOrange
                        )
                    },
                    divider = { HorizontalDivider(color = SlateDarkBorder) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) FireOrange else TextSecondary,
                            label = "TabTextColor"
                        )
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }

                // Feature Banner (only shown on the first tab and if search is empty)
                if (selectedTab == 0 && searchQuery.isEmpty()) {
                    DashboardHeroBanner()
                }

                if (filteredMatches.isEmpty()) {
                    EmptyMatchesState(selectedTab)
                } else {
                    filteredMatches.forEach { match ->
                        val isRegistered = userBookings.any { it.matchId == match.id }
                        MatchCard(
                            match = match,
                            isRegistered = isRegistered,
                            getSyncedTime = { viewModel.getSyncedTime() },
                            onActionClick = {
                                if (match.status == "UPCOMING") {
                                    if (isRegistered) {
                                        onNavigateToBookingDetail(match.id)
                                    } else {
                                        matchToJoin = match
                                    }
                                } else if (match.status == "LIVE") {
                                    onNavigateToBookingDetail(match.id)
                                } else {
                                    // PAST: Show completed details
                                    onNavigateToBookingDetail(match.id)
                                }
                            },
                            selectedCategory = selectedCategory
                        )
                    }
                }

                // Add extra bottom spacing so matches are not clipped under navigation bar
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 2. Collapsing Header Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { androidx.compose.ui.unit.IntOffset(0, yOffsetPx.toInt()) }
                    .background(SlateDarkBg)
                    .statusBarsPadding()
                    .height(headerHeightDp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - collapseFraction
                            scaleX = 1f - (collapseFraction * 0.12f)
                            scaleY = 1f - (collapseFraction * 0.12f)
                        }
                ) {
                    // BR ESPORTS logo top-bar row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = FireOrange,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BR ESPORTS",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                ),
                                color = TextPrimary
                            )
                        }
                        IconButton(onClick = { showSupportOptionsSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = "Customer Support",
                                tint = FireOrange,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Emergency Live Marquee
                    if (emergencyNotification.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LiveRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Emergency Alert",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ALERT: $emergencyNotification",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Dynamic Alert Banner for admin announcements
                    if (announcement.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = FireOrange.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .border(1.dp, FireOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "Announcement",
                                    tint = FireOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = announcement,
                                    color = TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Quick-Action Navigation Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToLeaderboard,
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDarkSurface),
                            border = BorderStroke(1.dp, GoldBooyah.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldBooyah, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RANKING", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp)
                        }

                        Button(
                            onClick = { showWatchLiveStream = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDarkSurface),
                            border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.weight(1.3f).fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WATCH LIVE", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 0.5.sp)
                        }

                        Button(
                            onClick = onNavigateToRules,
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDarkSurface),
                            border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = FireOrange, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RULES", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp)
                        }
                    }

                    // Search & Filter
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search tournaments, maps...", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = FireOrange,
                            unfocusedBorderColor = SlateDarkBorder,
                            focusedContainerColor = SlateDarkSurface,
                            unfocusedContainerColor = SlateDarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (showInsufficientCoinsDialog) {
        AlertDialog(
            onDismissRequest = { showInsufficientCoinsDialog = false },
            title = {
                Text(
                    text = "Insufficient Balance",
                    fontWeight = FontWeight.Bold,
                    color = LiveRed
                )
            },
            text = {
                Text(
                    text = "Insufficient Balance! Please deposit coins to join.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showInsufficientCoinsDialog = false }
                ) {
                    Text("OK", color = FireOrange, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SlateDarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary
        )
    }

    if (matchToJoin != null) {
        val targetMatch = matchToJoin!!
        val isTargetClashSquad = targetMatch.title.contains("Clash", ignoreCase = true) || 
                                 targetMatch.title.contains("CS", ignoreCase = true) || 
                                 (selectedCategory == "Clash Squad")
        val isSquad = targetMatch.gameMode == "Squad" && !isTargetClashSquad
        val isTargetLoneWolf = targetMatch.title.contains("Lone Wolf", ignoreCase = true) || 
                               targetMatch.title.contains("One Tap", ignoreCase = true) || 
                               (selectedCategory == "Lone Wolf") || 
                               (selectedCategory == "One Tap")
        val isTargetLoneWolfOrClash = isTargetLoneWolf || isTargetClashSquad

        // State variables for form fields
        var teamName by remember { mutableStateOf("") }
        var p1Name by remember(user) { mutableStateOf(user?.gameName ?: "") }
        var p1Uid by remember(user) { mutableStateOf(user?.gameUid ?: "") }

        var p2Name by remember { mutableStateOf("") }
        var p2Uid by remember { mutableStateOf("") }

        var p3Name by remember { mutableStateOf("") }
        var p3Uid by remember { mutableStateOf("") }

        var p4Name by remember { mutableStateOf("") }
        var p4Uid by remember { mutableStateOf("") }

        val scrollState = rememberScrollState()

        Dialog(onDismissRequest = { matchToJoin = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 450.dp, max = 680.dp)
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTargetClashSquad) "CLASH SQUAD REGISTRATION" else if (isTargetLoneWolf) "CONFIRM MATCH JOIN" else if (isSquad) "SQUAD MATCH REGISTRATION" else "MATCH REGISTRATION",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = FireOrange,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSquad) FireOrange.copy(alpha = 0.15f) else GoldBooyah.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = targetMatch.gameMode.uppercase(),
                                color = if (isSquad) FireOrange else GoldBooyah,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = SlateDarkBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Match Specs Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Tournament: ${targetMatch.title}", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(text = "Map: ${targetMatch.map} • ${targetMatch.gameMode}", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Slots Fee per player:",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${targetMatch.entryFee} Coins",
                                    fontSize = 11.sp,
                                    color = FireOrangeLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isSquad) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total Entry Fee (Deducted from Captain):",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${targetMatch.entryFee * 4} Coins",
                                        fontSize = 11.sp,
                                        color = FireOrange,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    if (isSquad) {
                        // SQUAD REGISTRATION FIELDS
                        Text(
                            text = "TEAM DETAILS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = teamName,
                            onValueChange = { teamName = it },
                            label = { Text("Team Name", fontSize = 11.sp) },
                            placeholder = { Text("Enter Team Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedContainerColor = SlateDarkBg,
                                unfocusedContainerColor = SlateDarkBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Player 1 (Captain)
                        Text(
                            text = "PLAYER 1: CAPTAIN (YOUR PROFILE)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FireOrangeLight,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = p1Name,
                                onValueChange = { p1Name = it },
                                label = { Text("IGN (In-Game Name)", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = p1Uid,
                                onValueChange = { p1Uid = it },
                                label = { Text("Free Fire UID", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Player 2
                        Text(
                            text = "PLAYER 2",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = p2Name,
                                onValueChange = { p2Name = it },
                                label = { Text("IGN", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = p2Uid,
                                onValueChange = { p2Uid = it },
                                label = { Text("UID", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Player 3
                        Text(
                            text = "PLAYER 3",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = p3Name,
                                onValueChange = { p3Name = it },
                                label = { Text("IGN", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = p3Uid,
                                onValueChange = { p3Uid = it },
                                label = { Text("UID", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Player 4
                        Text(
                            text = "PLAYER 4",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = p4Name,
                                onValueChange = { p4Name = it },
                                label = { Text("IGN", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = p4Uid,
                                onValueChange = { p4Uid = it },
                                label = { Text("UID", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true
                            )
                        }
                    } else {
                        // SOLO MATCH PREVIEW
                        Text(
                            text = if (isTargetLoneWolfOrClash) "Verify details for '${targetMatch.title}':" else "SOLO CONFIRMATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTargetLoneWolfOrClash) GoldBooyah else TextPrimary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = if (isTargetClashSquad) {
                                "Verify or update your Squad Lead / Player details below to join this Clash Squad match."
                            } else if (isTargetLoneWolf) {
                                "Verify or update your IGN and UID below to proceed."
                            } else {
                                "Your saved Free Fire details are preloaded below. Feel free to verify or update them for this match registration:"
                            },
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (isTargetLoneWolfOrClash) {
                                Text(
                                    text = if (isTargetClashSquad) "1. Game Name (Squad Lead / Player)" else "1. Game Name (IGN)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = p1Name,
                                    onValueChange = { p1Name = it },
                                    placeholder = { Text("Enter Game Name") },
                                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(58.dp),
                                    singleLine = true
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = if (isTargetClashSquad) "2. Free Fire UID (Squad Lead / Player)" else "2. Free Fire UID",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = p1Uid,
                                    onValueChange = { p1Uid = it },
                                    placeholder = { Text("Enter Free Fire UID") },
                                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(58.dp),
                                    singleLine = true
                                )
                            } else {
                                OutlinedTextField(
                                    value = p1Name,
                                    onValueChange = { p1Name = it },
                                    label = { Text("1. GAME NAME", fontSize = 10.sp) },
                                    textStyle = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Start, color = TextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(55.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = p1Uid,
                                    onValueChange = { p1Uid = it },
                                    label = { Text("2. UID", fontSize = 10.sp) },
                                    textStyle = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Start, color = TextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(55.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { matchToJoin = null },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("CANCEL", color = TextSecondary, fontWeight = FontWeight.Bold)
                        }

                        val isFormValid = if (isSquad) {
                            teamName.isNotBlank() &&
                                    p1Name.isNotBlank() && p1Uid.isNotBlank() &&
                                    p2Name.isNotBlank() && p2Uid.isNotBlank() &&
                                    p3Name.isNotBlank() && p3Uid.isNotBlank() &&
                                    p4Name.isNotBlank() && p4Uid.isNotBlank()
                        } else {
                            p1Name.isNotBlank() && p1Uid.isNotBlank()
                        }

                        Button(
                            onClick = {
                                val finalMatch = targetMatch
                                val requiredCoins = if (isSquad) finalMatch.entryFee * 4 else finalMatch.entryFee
                                val userBalance = user?.coinBalance ?: 0
                                if (userBalance < requiredCoins) {
                                    showInsufficientCoinsDialog = true
                                    Toast.makeText(context, "Insufficient Balance! Please deposit coins to join.", Toast.LENGTH_LONG).show()
                                } else {
                                    matchToJoin = null
                                    viewModel.registerForMatch(
                                        matchId = finalMatch.id,
                                        bookingType = finalMatch.gameMode,
                                        teamName = if (isSquad) teamName else null,
                                        player1Name = p1Name,
                                        player1Uid = p1Uid,
                                        player2Name = if (isSquad) p2Name else null,
                                        player2Uid = if (isSquad) p2Uid else null,
                                        player3Name = if (isSquad) p3Name else null,
                                        player3Uid = if (isSquad) p3Uid else null,
                                        player4Name = if (isSquad) p4Name else null,
                                        player4Uid = if (isSquad) p4Uid else null,
                                        entryFee = requiredCoins
                                    )
                                }
                            },
                            enabled = isFormValid,
                            colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("CONFIRM JOIN", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showEmergencyDialog && emergencyNotification.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = LiveRed,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "EMERGENCY BROADCAST LIVE",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = LiveRed,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Text(
                    text = emergencyNotification,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { showEmergencyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) {
                    Text("UNDERSTOOD", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = SlateDarkSurface
        )
    }

    if (showWatchLiveStream) {
        val uriHandler = LocalUriHandler.current
        val context = LocalContext.current
        Dialog(onDismissRequest = { showWatchLiveStream = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BR ESPORTS LIVE CAST",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        IconButton(onClick = { showWatchLiveStream = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Video Placeholder Art
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .border(1.dp, LiveRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = LiveRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "FREE FIRE FINALS • LIVE STREAM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Click below to watch directly on YouTube App",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Join our community on YouTube to support the players! Subscribe for custom room credentials, daily giveaways, and live casting of major cups.",
                        fontSize = 10.5.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val streamUrl = "https://yt.openinapp.co/i7ww0"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    uriHandler.openUri(streamUrl)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "Unable to open live stream link.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LAUNCH YOUTUBE STREAM", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSupportOptionsSheet) {
        SupportOptionsModalSheet(
            onDismiss = { showSupportOptionsSheet = false },
            onNavigateToSupport = {
                showSupportOptionsSheet = false
                onNavigateToSupport()
            }
        )
    }
}

@Composable
fun DashboardHeroBanner() {
    val context = LocalContext.current
    // Safely attempt to load the generated drawable. Fallback if not found or testing.
    val bannerResId = remember {
        context.resources.getIdentifier("img_hero_banner_1783564367460", "drawable", context.packageName)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
    ) {
        if (bannerResId != 0) {
            Image(
                painter = painterResource(id = bannerResId),
                contentDescription = "Esports Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant Canvas / Gradient fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SlateDarkSurface, FireOrange.copy(alpha = 0.5f), GoldBooyah.copy(alpha = 0.2f))
                        )
                    )
            )
        }

        // Dark overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        // Text Content Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(FireOrange, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "HOT TOURNAMENTS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "BR ESPORTS SEASON 4 CLASH",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
            Text(
                text = "Join premium squads and win up to 10,000 Coins daily!",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    isRegistered: Boolean,
    getSyncedTime: () -> Long,
    onActionClick: () -> Unit,
    selectedCategory: String = ""
) {
    val isDailyMatch = remember(match.title) { match.title.contains("Daily Auto-Match") }

    val formattedTime = remember(match.dateTimeMillis, isDailyMatch) {
        val dateObj = Date(match.dateTimeMillis)
        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        if (isDailyMatch) {
            val datePart = SimpleDateFormat("dd/MMMM/yy", Locale.US).apply { timeZone = tz }.format(dateObj)
            val timePart = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = tz }.format(dateObj)
            "Date: $datePart • Time: $timePart"
        } else {
            val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.US).apply { timeZone = tz }
            sdf.format(dateObj)
        }
    }

    val formattedLoneWolfTime = remember(match.dateTimeMillis) {
        val dateObj = Date(match.dateTimeMillis)
        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val sdf = SimpleDateFormat("dd/MM/yyyy 'at' hh:mm a", Locale.US).apply { timeZone = tz }
        "📅 ${sdf.format(dateObj)}"
    }

    val formattedOneTapTime = remember(match.dateTimeMillis) {
        val dateObj = Date(match.dateTimeMillis)
        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val datePart = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = tz }.format(dateObj)
        val timePart = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = tz }.format(dateObj)
        "📅 Date: $datePart | ⏰ Time: $timePart"
    }

    val formattedClashSquadTime = remember(match.dateTimeMillis) {
        val dateObj = Date(match.dateTimeMillis)
        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val datePart = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = tz }.format(dateObj)
        val timePart = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = tz }.format(dateObj)
        "📅 Date: $datePart | ⏰ Time: $timePart"
    }

    var effectiveStatus by remember(match.dateTimeMillis, match.status) {
        val now = getSyncedTime()
        val diff = now - match.dateTimeMillis
        mutableStateOf(
            when {
                match.status == "LIVE" -> "LIVE"
                match.status == "PAST" -> "PAST"
                diff < 0 -> "UPCOMING"
                diff in 0..3600000 -> "LIVE"
                else -> "PAST"
            }
        )
    }

    var timeRemainingText by remember(match.dateTimeMillis, match.status) {
        mutableStateOf("")
    }

    LaunchedEffect(match.dateTimeMillis, match.status) {
        while (true) {
            val now = getSyncedTime()
            val diff = match.dateTimeMillis - now
            val elapsed = now - match.dateTimeMillis
            
            effectiveStatus = when {
                match.status == "LIVE" -> "LIVE"
                match.status == "PAST" -> "PAST"
                diff > 0 -> "UPCOMING"
                elapsed in 0..3600000 -> "LIVE"
                else -> "PAST"
            }
            
            if (effectiveStatus == "PAST") {
                timeRemainingText = ""
                break
            } else if (effectiveStatus == "LIVE") {
                if (elapsed < 0) {
                    timeRemainingText = "LIVE • Starting soon"
                } else {
                    val hours = (elapsed / (3600 * 1000)).toInt()
                    val minutes = ((elapsed % (3600 * 1000)) / (60 * 1000)).toInt()
                    val seconds = ((elapsed % (60 * 1000)) / 1000).toInt()
                    timeRemainingText = if (hours > 0) {
                        String.format(Locale.getDefault(), "LIVE • %02dh %02dm %02ds ago", hours, minutes, seconds)
                    } else {
                        String.format(Locale.getDefault(), "LIVE • %02dm %02ds ago", minutes, seconds)
                    }
                }
            } else {
                val hours = (diff / (3600 * 1000)).toInt()
                val minutes = ((diff % (3600 * 1000)) / (60 * 1000)).toInt()
                val seconds = ((diff % (60 * 1000)) / 1000).toInt()
                timeRemainingText = if (hours > 0) {
                    String.format(Locale.getDefault(), "STARTS IN: %02dh %02dm %02ds", hours, minutes, seconds)
                } else {
                    String.format(Locale.getDefault(), "STARTS IN: %02dm %02ds", minutes, seconds)
                }
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    val isLoneWolf = match.title.contains("Lone Wolf", ignoreCase = true) || selectedCategory == "Lone Wolf"
    val isOneTap = match.title.contains("One Tap", ignoreCase = true) || selectedCategory == "One Tap"
    val isClashSquad = match.title.contains("Clash", ignoreCase = true) || selectedCategory == "Clash Squad"

    val cardBorderColor = when {
        isRegistered -> FireOrange
        isDailyMatch -> FireOrange.copy(alpha = 0.65f)
        else -> SlateDarkBorder
    }
    val cardBorderSize = if (isRegistered || isDailyMatch) 1.5.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(cardBorderSize, cardBorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = SlateDarkSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isLoneWolf || isOneTap || isClashSquad) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Bermuda Zone Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1C0D02),
                                    Color(0xFF3A1601),
                                    Color(0xFF111827)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = FireOrange.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Title & Map
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text(
                                text = if (isOneTap) "ONE TAP 1V1" else if (isClashSquad) "CLASH SQUAD 4V4" else "FREE FIRE MAX - 1V1",
                                color = FireOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isOneTap) {
                                    if (match.title.contains("One Tap", ignoreCase = true)) match.title else "${match.title} - ONE TAP"
                                } else if (isClashSquad) {
                                    if (match.title.contains("CS", ignoreCase = true) || match.title.contains("Clash", ignoreCase = true)) match.title else "${match.title} - 4V4 CS"
                                } else match.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOneTap) "${match.map.uppercase()} (ONE TAP)" else if (isClashSquad) "${match.map.uppercase()} (4V4 CS)" else "BERMUDA ZONE",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Right: Static rules badge
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                            border = BorderStroke(1.dp, FireOrange.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .width(if (isOneTap || isClashSquad) 175.dp else 145.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (isOneTap) {
                                    OneTapRuleRow(text = "🎯 ONLY HEADSHOT")
                                    OneTapRuleRow(text = "🔫 DESERT EAGLE & M1887 ONLY")
                                    OneTapRuleRow(text = "🚫 NO GRENADE / NO BODY SHOT")
                                    OneTapRuleRow(text = "👤 1V1 SOLO BATTLE (2 SLOTS MAX)")
                                } else if (isClashSquad) {
                                    OneTapRuleRow(text = "⚔️ 4V4 SQUAD BATTLE (8 SLOTS)")
                                    OneTapRuleRow(text = "🎯 7 ROUNDS MATCH")
                                    OneTapRuleRow(text = "🔫 UNLIMITED AMMO / NO GRENADE")
                                    OneTapRuleRow(text = "⚡ CHARACTER SKILLS OFF")
                                } else {
                                    RuleRow(text = "LV 40+ ACTIVE", isCheck = true)
                                    RuleRow(text = "HUD POV ON", isCheck = true)
                                    RuleRow(text = "NO ACTIVE SKILL", isCheck = false)
                                    RuleRow(text = "D-VECTOR BAN", isCheck = false)
                                }
                            }
                        }
                    }
                }

                // Dynamic Schedule & Countdown Badge Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color(0xFF131A26), RoundedCornerShape(8.dp))
                        .border(1.dp, FireOrange.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Time & Date Badge
                    Text(
                        text = if (isOneTap) formattedOneTapTime else if (isClashSquad) formattedClashSquadTime else formattedLoneWolfTime,
                        fontSize = if (isOneTap || isClashSquad) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Countdown / Pulsing Live Badge
                    if (effectiveStatus == "LIVE") {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_alpha"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .graphicsLayer(alpha = alpha)
                                .background(LiveRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .border(1.dp, LiveRed, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(LiveRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE NOW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = LiveRed,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else if (effectiveStatus == "UPCOMING") {
                        val countdownText = timeRemainingText.replace("STARTS IN:", "").trim()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(FireOrange.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .border(1.dp, FireOrange.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = FireOrangeLight,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "⏰ STARTS IN: $countdownText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireOrangeLight
                            )
                        }
                    } else {
                        // PAST Match
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(SlateDarkBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🏁 COMPLETED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // 2. Main Parameters Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cell 1: Prize Pool
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF172030), RoundedCornerShape(6.dp))
                            .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Prize Pool", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldBooyah, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${match.prizePool} Coins", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Cell 2: Entry Fee
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF172030), RoundedCornerShape(6.dp))
                            .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Entry Fee", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = FireOrange, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (match.entryFee == 0) "FREE" else "${match.entryFee} Coins", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Cell 3: Match Format
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF172030), RoundedCornerShape(6.dp))
                            .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Match Format", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(if (isClashSquad) "4V4 CS" else "1V1 BATTLE", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Extra structured parameters (Map Mini Zone & Prize Pool note)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isClashSquad) "🏆 PRIZE: 4V4 WINNER SQUAD" else "🏆 PRIZE: WINNER TAKES ALL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldBooyah
                    )
                    Text(
                        text = if (isOneTap) "🗺️ MAP: ${match.map.uppercase()} (ONE TAP)" else if (isClashSquad) "🗺️ MAP: ${match.map.uppercase()} (4V4 CS)" else "🗺️ MAP: ${match.map.uppercase()} (MINI ZONE)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Display winner if match is past
                if (effectiveStatus == "PAST") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(SlateDarkBg, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = GoldBooyah,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Winner: ${match.winner1Name ?: "TBD"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldBooyah,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 3. Slots Tracker & Action Button Footer (styled together inside SlateDarkBg)
                if (effectiveStatus != "PAST") {
                    val slotsTotal = 2
                    val slotsBooked = if (match.slotsBooked >= 2) 2 else if (match.slotsBooked < 0) 0 else match.slotsBooked
                    val progress = slotsBooked.toFloat() / slotsTotal.toFloat()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1422))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Slots Booked",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (slotsBooked >= slotsTotal) "Slots Full: 2 / 2 Players" else "$slotsBooked / 2 Players",
                                    fontSize = 11.sp,
                                    color = if (slotsBooked >= slotsTotal) LiveRed else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                color = if (slotsBooked >= slotsTotal) LiveRed else FireOrange,
                                trackColor = SlateDarkBorder,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )
                        }

                        // Button
                        Button(
                            onClick = onActionClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isRegistered -> SlateDarkBorder
                                    effectiveStatus == "LIVE" -> LiveRed
                                    effectiveStatus == "PAST" -> SlateDarkBorder
                                    else -> FireOrange
                                },
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            val actionText = when {
                                isRegistered && effectiveStatus == "UPCOMING" -> "VIEW"
                                isRegistered && effectiveStatus == "LIVE" -> "LIVE"
                                effectiveStatus == "LIVE" -> "STATUS"
                                effectiveStatus == "PAST" -> "RESULTS"
                                else -> "JOIN"
                            }
                            Text(text = actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Action button for past matches (full width inside card padding)
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Button(
                            onClick = onActionClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlateDarkBorder,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = "VIEW COMPLETE RESULTS 🏆",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Match Header (Title & Badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (isDailyMatch) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = FireOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "🔥 DAILY ESPORTS SERIES",
                                    color = FireOrange,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Text(
                            text = match.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedTime,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        if (timeRemainingText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val isLive = effectiveStatus == "LIVE"
                            val timerBgColor = if (isLive) LiveRed.copy(alpha = 0.15f) else FireOrange.copy(alpha = 0.1f)
                            val timerTextColor = if (isLive) LiveRed else FireOrangeLight
                            val timerBorderColor = if (isLive) LiveRed.copy(alpha = 0.4f) else FireOrange.copy(alpha = 0.3f)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(timerBgColor)
                                    .border(1.dp, timerBorderColor, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(timerTextColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = timeRemainingText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = timerTextColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // Status Badge
                    StatusBadge(status = effectiveStatus)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row (Prize Pool, Entry Fee, Per Kill)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        imageVector = Icons.Default.EmojiEvents,
                        label = "PRIZE POOL",
                        value = "${match.prizePool} Coins",
                        color = GoldBooyah
                    )
                    StatItem(
                        imageVector = Icons.Default.MonetizationOn,
                        label = "ENTRY FEE",
                        value = if (match.entryFee == 0) "FREE" else "${match.entryFee} Coins",
                        color = if (match.entryFee == 0) Color.Green else FireOrange
                    )
                    StatItem(
                        imageVector = Icons.Default.MilitaryTech,
                        label = "PER KILL",
                        value = "${match.perKillPrize} Coins",
                        color = FireOrangeLight
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gameplay settings (Mode, Map)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (match.gameMode == "Solo") Icons.Default.Person else Icons.Default.Group,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MODE: ${match.gameMode.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MAP: ${match.map.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Slots Progress Bar (only for upcoming or live matches)
                if (effectiveStatus != "PAST") {
                    val progress = if (match.slotsTotal > 0) match.slotsBooked.toFloat() / match.slotsTotal else 0f
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Slots Booked",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${match.slotsBooked} / ${match.slotsTotal}",
                                fontSize = 11.sp,
                                color = if (match.slotsBooked >= match.slotsTotal) LiveRed else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            color = if (match.slotsBooked >= match.slotsTotal) LiveRed else FireOrange,
                            trackColor = SlateDarkBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                } else {
                    // For past matches, display winners preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDarkBg, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = GoldBooyah,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Winner: ${match.winner1Name ?: "TBD"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldBooyah,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isRegistered -> SlateDarkBorder
                            effectiveStatus == "LIVE" -> LiveRed
                            effectiveStatus == "PAST" -> SlateDarkBorder
                            else -> FireOrange
                        },
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    val actionText = when {
                        isRegistered && effectiveStatus == "UPCOMING" -> "VIEW REGISTRATION & ROOM 🎫"
                        isRegistered && effectiveStatus == "LIVE" -> "JOIN CUSTOM ROOM NOW 🔴"
                        effectiveStatus == "LIVE" -> "MATCH LIVE (VIEW STATUS)"
                        effectiveStatus == "PAST" -> "VIEW COMPLETE RESULTS 🏆"
                        else -> "JOIN MATCH"
                    }
                    
                    Text(
                        text = actionText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OneTapRuleRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RuleRow(text: String, isCheck: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isCheck) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isCheck) Color(0xFF22C55E) else Color(0xFFEF4444),
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun StatItem(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val (text, color) = when (status) {
        "LIVE" -> "LIVE" to LiveRed
        "PAST" -> "FINISHED" to TextSecondary
        else -> "UPCOMING" to FireOrange
    }

    val isLive = status == "LIVE"
    val infiniteTransition = rememberInfiniteTransition(label = "PulsingState")
    val liveOpacity by if (isLive) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "LiveOpacity"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Surface(
        color = color.copy(alpha = if (isLive) liveOpacity else 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (isLive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(LiveRed)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun EmptyMatchesState(selectedTab: Int) {
    val message = when (selectedTab) {
        0 -> "No upcoming tournaments found.\nCheck back later or add one via the Admin Panel!"
        1 -> "No tournaments are currently Live 🔴.\nRegistered players will see their credentials here when they begin!"
        else -> "No past tournaments found."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GameCategoryCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SlateDarkSurface else SlateDarkSurface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) FireOrange else SlateDarkBorder
        ),
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) FireOrange else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) TextPrimary else TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) FireOrangeLight.copy(alpha = 0.9f) else TextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GameCategoryGrid(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("FF MAX", "All Modes", Icons.Default.SportsEsports),
        Triple("Lone Wolf", "1v1 Solo", Icons.Default.Person),
        Triple("One Tap", "Headshots Only", Icons.Default.FlashOn),
        Triple("Clash Squad", "4v4 Squad", Icons.Default.Groups),
        Triple("Full Map", "Bermuda/Kalahari", Icons.Default.Map),
        Triple("Tournament", "Special Rules", Icons.Default.Tune)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "eSport Games".uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = FireOrange,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Grid Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0..2) {
                val (title, subtitle, icon) = items[i]
                GameCategoryCard(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    isSelected = selectedCategory == title,
                    onClick = { onCategorySelected(title) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Grid Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 3..5) {
                val (title, subtitle, icon) = items[i]
                GameCategoryCard(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    isSelected = selectedCategory == title,
                    onClick = { onCategorySelected(title) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
