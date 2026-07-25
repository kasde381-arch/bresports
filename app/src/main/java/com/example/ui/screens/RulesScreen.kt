package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.ui.theme.*
import com.example.ui.viewmodel.TournamentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    viewModel: TournamentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasAgreed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = FireOrange,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RULES & REGULATIONS",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back", tint = TextPrimary)
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
            // High-Contrast Fair Play Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = LiveRed.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LiveRed, RoundedCornerShape(12.dp))
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = LiveRed,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "STRICT ZERO TOLERANCE POLICY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = LiveRed,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Any player caught violating rules will face instant, permanent hardware bans and forfeit all wallet balances.",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = "BR ESPORTS FAIR PLAY CONTRACT",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = GoldBooyah,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Rule #1: Anti-Cheat Policies
            RuleCard(
                icon = Icons.Default.Security,
                iconColor = GoldBooyah,
                title = "1. STRICT ANTI-CHEAT & MODS POLICY",
                description = "Usage of third-party modifications, scripts, graphics enhancers, cheats (aimbots, wallhacks, ESP), or playing on rooted/jailbroken devices is strictly prohibited. Our automated game-hook checks verify players at custom room entry. If flagged, you will be permanently banned immediately."
            )

            // Rule #2: No Teaming Up
            RuleCard(
                icon = Icons.Default.Group,
                iconColor = FireOrange,
                title = "2. NO TEAMING UP & COLLUSION",
                description = "Teaming up with opponents in Solo matches or colluding with rival squads is prohibited. If we find matching telemetry coordinates or coordinated gameplay, both players/squads are disqualified instantly, forfeit entry fees, and face permanent wallet freeze."
            )

            // Rule #3: Mandatory Screenshot Upload
            RuleCard(
                icon = Icons.Default.PhotoCamera,
                iconColor = Color(0xFF4CAF50),
                title = "3. MANDATORY END-MATCH SCREENSHOT UPLOAD",
                description = "To claim prizes and receive coin allocations, players MUST take a clear screenshot of their final placement and kill counts in the game lobby immediately after completion. Upload this screenshot under your registered match in 'My Bookings' within 15 minutes of the match ending. No screenshot, no prize."
            )

            // Rule #4: Room Entry Rules
            RuleCard(
                icon = Icons.AutoMirrored.Filled.Login,
                iconColor = TextSecondary,
                title = "4. ROOM LOBBY TIMINGS & INTEGRITY",
                description = "Room IDs and Passwords automatically unlock 15 minutes before the scheduled start under 'My Bookings'. You must join the custom room within 10 minutes. Do NOT share room details with unregistered players. Unlisted players will be kicked immediately."
            )

            // Rule #5: Dispute Resolution
            RuleCard(
                icon = Icons.Default.SupportAgent,
                iconColor = Color(0xFF29B6F6),
                title = "5. DISPUTES & RECORDINGS",
                description = "In case of disputes, we recommend recording your gameplay. If you suspect an opponent is hacking, clip the gameplay and raise a ticket under 'Customer Support' immediately. Decision of BR Esports admin is final."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Acknowledgment Checkbox
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable { hasAgreed = !hasAgreed }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasAgreed,
                        onCheckedChange = { hasAgreed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = FireOrange,
                            uncheckedColor = TextSecondary,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.testTag("agreement_checkbox")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "I have read, understood, and fully agree to follow the BR Esports Rules, Anti-Cheat system, and Screenshot requirements.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            // Acknowledge Button
            Button(
                onClick = {
                    if (hasAgreed) {
                        Toast.makeText(context, "Thank you for confirming. Good luck on the battleground!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    } else {
                        Toast.makeText(context, "Please agree to the Fair Play Contract to continue.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasAgreed) FireOrange else SlateDarkBorder,
                    contentColor = if (hasAgreed) Color.White else TextSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("acknowledge_rules_button")
            ) {
                Text(
                    text = "ACKNOWLEDGE & RETURN",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun RuleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
