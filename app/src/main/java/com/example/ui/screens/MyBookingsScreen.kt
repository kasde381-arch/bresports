package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.data.model.Match
import com.example.ui.theme.*
import com.example.ui.viewmodel.TournamentViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    viewModel: TournamentViewModel,
    modifier: Modifier = Modifier
) {
    val userBookings by viewModel.userBookings.collectAsState()
    val matches by viewModel.matches.collectAsState()

    var selectedBookingId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = FireOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MY REGISTRATIONS",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 20.sp
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
        if (userBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You have no registrations yet.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Go book a slot on the dashboard tab!",
                        color = FireOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(userBookings, key = { it.id }) { booking ->
                    val match = matches.find { it.id == booking.matchId }
                    if (match != null) {
                        val isExpanded = selectedBookingId == booking.id
                        BookingCard(
                            booking = booking,
                            match = match,
                            isExpanded = isExpanded,
                            viewModel = viewModel,
                            onCardClick = {
                                selectedBookingId = if (isExpanded) null else booking.id
                            },
                            onCancelBooking = {
                                viewModel.cancelRegistration(match.id, match.entryFee)
                            },
                            onUploadScreenshot = { uri ->
                                viewModel.updateBookingScreenshot(booking, uri)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    booking: Booking,
    match: Match,
    isExpanded: Boolean,
    viewModel: TournamentViewModel,
    onCardClick: () -> Unit,
    onCancelBooking: () -> Unit,
    onUploadScreenshot: (String) -> Unit
) {
    val context = LocalContext.current
    var currentSyncedTime by remember { mutableStateOf(viewModel.getSyncedTime()) }
    LaunchedEffect(match.dateTimeMillis) {
        while (true) {
            currentSyncedTime = viewModel.getSyncedTime()
            delay(10000)
        }
    }
    val isDailyMatch = remember(match.title) { match.title.contains("Daily Auto-Match") }
    val formattedTime = remember(match.dateTimeMillis, isDailyMatch) {
        val dateObj = Date(match.dateTimeMillis)
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        if (isDailyMatch) {
            val datePart = SimpleDateFormat("dd/MMMM/yy", Locale.US).apply { timeZone = tz }.format(dateObj)
            val timePart = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = tz }.format(dateObj)
            "Date: $datePart • Time: $timePart"
        } else {
            val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.US).apply { timeZone = tz }
            sdf.format(dateObj)
        }
    }

    var showCancelDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(
                1.dp,
                if (match.status == "LIVE") LiveRed.copy(alpha = 0.5f) else SlateDarkBorder,
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCardClick() }
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                    Text(
                        text = formattedTime,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                StatusBadge(status = match.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-details (Mode, Map, Team Name)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (match.gameMode == "Solo") Icons.Default.Person else Icons.Default.Group,
                        contentDescription = null,
                        tint = FireOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (match.gameMode == "Squad") "Squad: ${booking.teamName ?: "Unnamed"}" else "Solo Match",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Details Section
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = SlateDarkBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    // CRITICAL SECTION: CUSTOM ROOM ID AND PASSWORD DISPLAY SECTION
                    Text(
                        text = "ROOM ACCESS DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = FireOrange,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (match.status == "PAST") {
                        // For past tournaments, display podium details
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDarkBg, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GoldBooyah, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Match Completed!", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🏆 1st: ${match.winner1Name ?: "TBD"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldBooyah
                                )
                                Text(
                                    text = "🥈 2nd: ${match.winner2Name ?: "TBD"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "🥉 3rd: ${match.winner3Name ?: "TBD"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    } else if (match.roomId != null && match.roomPassword != null && currentSyncedTime >= (match.dateTimeMillis - 15 * 60 * 1000)) {
                        // Credentials are ready! Display clearly
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(SlateDarkBg, FireOrange.copy(alpha = 0.1f))
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, FireOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "⚠️ Room has been configured! Copy details below and enter them in the Free Fire custom room panel.",
                                    fontSize = 11.sp,
                                    color = FireOrangeLight,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("ROOM ID", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = match.roomId,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = GoldBooyah
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            copyToClipboard(context, "Room ID", match.roomId)
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Room ID", tint = GoldBooyah)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("PASSWORD", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = match.roomPassword,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            copyToClipboard(context, "Password", match.roomPassword)
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        // Credentials are not ready
                        val infiniteTransition = rememberInfiniteTransition(label = "RoomPulsing")
                        val opacity by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "PulseOpacity"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDarkBg, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = opacity),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Awaiting Room Credentials",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary.copy(alpha = opacity)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Room ID and Password will be posted here 15 minutes before start. Stay tuned!",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Match Details and Rules Section
                    MatchDetailsAndRulesSection(match = match)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Team Rosters Details
                    Text(
                        text = "REGISTERED SQUAD / ROSTER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = FireOrange,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    RosterItem(label = "Player 1 (Leader)", name = booking.player1Name, uid = booking.player1Uid)
                    
                    if (match.gameMode == "Squad") {
                        booking.player2Name?.let { name ->
                            RosterItem(label = "Player 2", name = name, uid = booking.player2Uid ?: "")
                        }
                        booking.player3Name?.let { name ->
                            RosterItem(label = "Player 3", name = name, uid = booking.player3Uid ?: "")
                        }
                        booking.player4Name?.let { name ->
                            RosterItem(label = "Player 4", name = name, uid = booking.player4Uid ?: "")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LobbySection(match = match)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "GAMEPLAY SCREENSHOT PROOF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = FireOrange,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            onUploadScreenshot(it.toString())
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDarkBg, RoundedCornerShape(8.dp))
                            .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Submit an end-match scoreboard or continuous real gameplay screenshot as proof of compliance and claim verification.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (booking.screenshotUri != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Green.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color.Green.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Screenshot Proof Submitted", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Uri: ${booking.screenshotUri}", fontSize = 9.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    TextButton(onClick = { launcher.launch("image/*") }) {
                                        Text("RE-UPLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FireOrangeLight)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { launcher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange.copy(alpha = 0.1f), contentColor = FireOrange),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, FireOrange, RoundedCornerShape(8.dp)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("UPLOAD MATCH RESULT SCREENSHOT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Cancel Booking / Slot Refund Flow
                    if (match.status == "UPCOMING") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCancelDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed.copy(alpha = 0.1f), contentColor = LiveRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LiveRed, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CancelPresentation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CANCEL REGISTRATION (REFUND ${match.entryFee} COINS)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Tournament Registration?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to cancel your slot in '${match.title}'? You will receive a full refund of ${match.entryFee} coins instantly.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelBooking()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Go Back", color = TextSecondary)
                }
            },
            containerColor = SlateDarkSurface
        )
    }
}

@Composable
fun RosterItem(label: String, name: String, uid: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(SlateDarkBg.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Text("UID: $uid", fontSize = 11.sp, color = FireOrange, fontWeight = FontWeight.Medium)
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
}

@Composable
fun MatchDetailsAndRulesSection(match: Match) {
    val isDailyMatch = remember(match.title) { match.title.contains("Daily Auto-Match") }
    val formattedTime = remember(match.dateTimeMillis, isDailyMatch) {
        val dateObj = Date(match.dateTimeMillis)
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        if (isDailyMatch) {
            val datePart = SimpleDateFormat("dd/MMMM/yy", Locale.US).apply { timeZone = tz }.format(dateObj)
            val timePart = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = tz }.format(dateObj)
            "Date: $datePart • Time: $timePart"
        } else {
            val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.US).apply { timeZone = tz }
            sdf.format(dateObj)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- MATCH DETAILS SECTION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = FireOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MATCH DETAILS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = FireOrange,
                        letterSpacing = 1.sp
                    )
                }

                HorizontalDivider(color = SlateDarkBorder, modifier = Modifier.padding(bottom = 12.dp))

                // Grid/Details list
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRowItem(label = "Title", value = match.title, icon = Icons.Default.Subtitles)
                    DetailRowItem(label = "Schedule Time", value = formattedTime, icon = Icons.Default.AccessTime)
                    DetailRowItem(label = "Battlefield Map", value = match.map, icon = Icons.Default.Map)
                    DetailRowItem(label = "Game Mode", value = match.gameMode, icon = if (match.gameMode == "Solo") Icons.Default.Person else Icons.Default.Group)
                    DetailRowItem(
                        label = "Entry Fee",
                        value = if (match.entryFee == 0) "FREE" else "${match.entryFee} Coins",
                        valueColor = if (match.entryFee == 0) Color.Green else FireOrangeLight,
                        icon = Icons.Default.MonetizationOn
                    )
                    DetailRowItem(
                        label = "Total Prize Pool",
                        value = "${match.prizePool} Coins",
                        valueColor = GoldBooyah,
                        icon = Icons.Default.EmojiEvents
                    )
                    DetailRowItem(
                        label = "Per Kill Bonus",
                        value = "${match.perKillPrize} Coins per Kill",
                        valueColor = FireOrangeLight,
                        icon = Icons.Default.MilitaryTech
                    )
                }
            }
        }

        // --- MATCH RULES SECTION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = LiveRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TOURNAMENT RULES & GUIDELINES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = LiveRed,
                        letterSpacing = 1.sp
                    )
                }

                HorizontalDivider(color = SlateDarkBorder, modifier = Modifier.padding(bottom = 12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RuleRowItem(
                        title = "No Hacks or Skin Tools",
                        description = "Use of third-party hacks, mod menus, or skin tools is strictly prohibited and will lead to an immediate ban.",
                        icon = Icons.Default.Block
                    )
                    RuleRowItem(
                        title = "Mobile Devices Only",
                        description = "This is a mobile-only tournament. Playing on emulators, iPads/tablets, or keyboards/controllers is not allowed.",
                        icon = Icons.Default.PhonelinkRing
                    )
                    RuleRowItem(
                        title = "Be On Time",
                        description = "Be in the room lobby at least 5 minutes before the match start time. Late entries will not be accommodated.",
                        icon = Icons.Default.Timer
                    )
                    RuleRowItem(
                        title = "Screenshot Proof Required",
                        description = "Always record your gameplay or take screenshot proof of the final scoreboard to claim dispute prizes and verify results.",
                        icon = Icons.Default.Screenshot
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRowItem(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            fontSize = 12.sp,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun RuleRowItem(
    title: String,
    description: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FireOrangeLight,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun LobbySection(match: Match) {
    // We can define a list of players
    val initialPlayers = remember {
        listOf(
            LobbyPlayer("〆BOOYAH_KING〆", "UID: 48593859", "SQUAD - BOOYAH", "READY"),
            LobbyPlayer("Mortal_God", "UID: 93820485", "SOLO", "IN ROOM"),
            LobbyPlayer("TSG_Slayer", "UID: 85930284", "SQUAD - TSG", "READY"),
            LobbyPlayer("Aura_Esports", "UID: 19485038", "SQUAD - AURA", "IN LOBBY"),
            LobbyPlayer("BR_Raptor", "UID: 74859302", "SOLO", "READY"),
            LobbyPlayer("Sensi_Pro", "UID: 29485039", "SOLO", "IN ROOM"),
            LobbyPlayer("Ninja_FF", "UID: 19485029", "SQUAD - NINJA", "READY"),
            LobbyPlayer("V_B_Bhai", "UID: 58493028", "SOLO", "IN LOBBY"),
            LobbyPlayer("DEAD_SHOOTER", "UID: 49583021", "SQUAD - DEAD", "READY"),
            LobbyPlayer("ALPHA_STRIKER", "UID: 29583920", "SOLO", "IN ROOM")
        )
    }

    var players by remember { mutableStateOf(initialPlayers) }

    // Live state updates to make the room feel fully active!
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000) // update a player's connection status every 4s
            val index = players.indices.random()
            val statuses = listOf("READY", "IN ROOM", "IN LOBBY", "CONNECTING")
            val nextStatus = statuses.random()
            players = players.toMutableList().apply {
                this[index] = this[index].copy(status = nextStatus)
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ROOM LOBBY LATEST (LIVE)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                Text(
                    text = "${players.count { it.status == "READY" || it.status == "IN ROOM" } + 1}/${if (match.gameMode == "Solo") 48 else 12} Joined",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Below are the other participants currently verified and assigned slots in this room lobby. Access credentials above to join them.",
                fontSize = 10.sp,
                color = TextSecondary,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Display players in a scrollable list inside card or column
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                players.take(if (match.gameMode == "Solo") 6 else 4).forEach { player ->
                    val statusColor = when (player.status) {
                        "READY" -> Color(0xFF4CAF50)
                        "IN ROOM" -> FireOrange
                        "IN LOBBY" -> Color(0xFFFFC107)
                        else -> Color(0xFF9E9E9E)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDarkBg.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .border(1.dp, SlateDarkBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.name.firstOrNull()?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = statusColor
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = player.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Mode: ${player.mode} • ${player.uid}",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = player.status,
                                color = statusColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class LobbyPlayer(
    val name: String,
    val uid: String,
    val mode: String,
    val status: String
)
