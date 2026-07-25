package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Match
import com.example.ui.theme.*
import com.example.ui.viewmodel.TournamentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    matchId: Int,
    viewModel: TournamentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matches by viewModel.matches.collectAsState()
    val user by viewModel.user.collectAsState()
    val match = remember(matches, matchId) { matches.find { it.id == matchId } }

    if (match == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tournament not found.", color = TextPrimary)
        }
        return
    }

    // Form inputs state
    var teamName by remember { mutableStateOf("") }
    
    // Player 1 (Prepopulated from logged-in user profile)
    var p1Name by remember { mutableStateOf("") }
    var p1Uid by remember { mutableStateOf("") }
    
    // Players 2, 3, 4 (For Squad mode)
    var p2Name by remember { mutableStateOf("") }
    var p2Uid by remember { mutableStateOf("") }
    var p3Name by remember { mutableStateOf("") }
    var p3Uid by remember { mutableStateOf("") }
    var p4Name by remember { mutableStateOf("") }
    var p4Uid by remember { mutableStateOf("") }

    // Set defaults from profile when available
    LaunchedEffect(user) {
        user?.let {
            if (p1Name.isEmpty()) p1Name = it.gameName
            if (p1Uid.isEmpty()) p1Uid = it.gameUid
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val userBalance = user?.coinBalance ?: 0
    val canAfford = userBalance >= match.entryFee

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SLOT REGISTRATION", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = FireOrange)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Tournament Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = match.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mode: ${match.gameMode}  |  Map: ${match.map}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SlateDarkBorder)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Entry Fee", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = if (match.entryFee == 0) "FREE" else "${match.entryFee} Coins",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (match.entryFee == 0) Color.Green else FireOrange
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Your Balance", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "$userBalance Coins",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (canAfford) GoldBooyah else LiveRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Match Details and Rules Section
            MatchDetailsAndRulesSection(match = match)

            Spacer(modifier = Modifier.height(20.dp))

            // Squad Team Name (Only in Squad mode)
            if (match.gameMode == "Squad") {
                Text(
                    text = "TEAM DETAILS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FireOrange,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = FireOrange) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FireOrange,
                        unfocusedBorderColor = SlateDarkBorder,
                        focusedContainerColor = SlateDarkSurface,
                        unfocusedContainerColor = SlateDarkSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            // Player Details Section
            Text(
                text = "PLAYER REGISTRATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FireOrange,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Player 1 (Always Visible, pre-filled)
            PlayerInputBlock(
                playerIndex = 1,
                nameLabel = "Player 1 Game Name (Leader)",
                uidLabel = "Player 1 Free Fire UID",
                name = p1Name,
                onNameChange = { p1Name = it },
                uid = p1Uid,
                onUidChange = { p1Uid = it.filter { char -> char.isDigit() } }
            )

            if (match.gameMode == "Squad") {
                Spacer(modifier = Modifier.height(12.dp))
                PlayerInputBlock(
                    playerIndex = 2,
                    nameLabel = "Player 2 Game Name",
                    uidLabel = "Player 2 Free Fire UID",
                    name = p2Name,
                    onNameChange = { p2Name = it },
                    uid = p2Uid,
                    onUidChange = { p2Uid = it.filter { char -> char.isDigit() } }
                )
                Spacer(modifier = Modifier.height(12.dp))
                PlayerInputBlock(
                    playerIndex = 3,
                    nameLabel = "Player 3 Game Name",
                    uidLabel = "Player 3 Free Fire UID",
                    name = p3Name,
                    onNameChange = { p3Name = it },
                    uid = p3Uid,
                    onUidChange = { p3Uid = it.filter { char -> char.isDigit() } }
                )
                Spacer(modifier = Modifier.height(12.dp))
                PlayerInputBlock(
                    playerIndex = 4,
                    nameLabel = "Player 4 Game Name",
                    uidLabel = "Player 4 Free Fire UID",
                    name = p4Name,
                    onNameChange = { p4Name = it },
                    uid = p4Uid,
                    onUidChange = { p4Uid = it.filter { char -> char.isDigit() } }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message (If any)
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = LiveRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Balance Refill Button (If insufficient coins)
            if (!canAfford) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LiveRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LiveRed, RoundedCornerShape(8.dp))
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You do not have enough coins to register for this match.",
                            color = LiveRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.refillCoins() },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refill 1,000 Coins FREE")
                        }
                    }
                }
            }

            // Confirm Registration Button
            Button(
                onClick = {
                    // Validation
                    if (p1Name.isBlank() || p1Uid.isBlank()) {
                        errorMessage = "Player 1 Name and Free Fire UID are required!"
                        return@Button
                    }
                    if (p1Uid.length < 8 || p1Uid.length > 12) {
                        errorMessage = "Player 1 (Leader) Free Fire UID must be between 8 and 12 digits."
                        return@Button
                    }
                    if (match.gameMode == "Squad") {
                        if (teamName.isBlank()) {
                            errorMessage = "Team Name is required for Squad mode!"
                            return@Button
                        }
                        if (p2Name.isBlank() || p2Uid.isBlank() ||
                            p3Name.isBlank() || p3Uid.isBlank() ||
                            p4Name.isBlank() || p4Uid.isBlank()) {
                            errorMessage = "All 4 Player details are required for Squad tournaments!"
                            return@Button
                        }
                        if (p2Uid.length < 8 || p2Uid.length > 12) {
                            errorMessage = "Player 2 Free Fire UID must be between 8 and 12 digits."
                            return@Button
                        }
                        if (p3Uid.length < 8 || p3Uid.length > 12) {
                            errorMessage = "Player 3 Free Fire UID must be between 8 and 12 digits."
                            return@Button
                        }
                        if (p4Uid.length < 8 || p4Uid.length > 12) {
                            errorMessage = "Player 4 Free Fire UID must be between 8 and 12 digits."
                            return@Button
                        }
                    }
                    errorMessage = null

                    viewModel.registerForMatch(
                        matchId = match.id,
                        bookingType = match.gameMode,
                        teamName = if (match.gameMode == "Squad") teamName else null,
                        player1Name = p1Name,
                        player1Uid = p1Uid,
                        player2Name = if (match.gameMode == "Squad") p2Name else null,
                        player2Uid = if (match.gameMode == "Squad") p2Uid else null,
                        player3Name = if (match.gameMode == "Squad") p3Name else null,
                        player3Uid = if (match.gameMode == "Squad") p3Uid else null,
                        player4Name = if (match.gameMode == "Squad") p4Name else null,
                        player4Uid = if (match.gameMode == "Squad") p4Uid else null,
                        entryFee = match.entryFee
                    )
                },
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FireOrange,
                    disabledContainerColor = SlateDarkBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "CONFIRM & PAY ${match.entryFee} COINS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PlayerInputBlock(
    playerIndex: Int,
    nameLabel: String,
    uidLabel: String,
    name: String,
    onNameChange: (String) -> Unit,
    uid: String,
    onUidChange: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "PLAYER $playerIndex",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Game Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = FireOrange, modifier = Modifier.size(16.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FireOrange,
                        unfocusedBorderColor = SlateDarkBorder,
                        focusedContainerColor = SlateDarkBg,
                        unfocusedContainerColor = SlateDarkBg
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )

                OutlinedTextField(
                    value = uid,
                    onValueChange = onUidChange,
                    label = { Text("FF UID") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FireOrange,
                        unfocusedBorderColor = SlateDarkBorder,
                        focusedContainerColor = SlateDarkBg,
                        unfocusedContainerColor = SlateDarkBg
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
            }
        }
    }
}
