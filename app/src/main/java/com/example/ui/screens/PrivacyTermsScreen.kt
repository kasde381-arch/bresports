package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyTermsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedSection by remember { mutableStateOf(0) } // 0: All, 1: Privacy/Data, 2: Refunds, 3: Conduct

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = FireOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LEGAL & POLICIES",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 18.sp
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
            // Section Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedSection,
                containerColor = SlateDarkBg,
                contentColor = FireOrange,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = { Text("ALL", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    selectedContentColor = FireOrange,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = { Text("DATA USAGE", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    selectedContentColor = FireOrange,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    text = { Text("REFUNDS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    selectedContentColor = FireOrange,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedSection == 3,
                    onClick = { selectedSection = 3 },
                    text = { Text("CONDUCT", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    selectedContentColor = FireOrange,
                    unselectedContentColor = TextSecondary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // Intro Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "BR ESPORTS PLAYER COVENANT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = FireOrangeLight,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Welcome to BR ESPORTS. Our legal framework ensures fair competition, transparent transactions, and premium privacy protections for all Free Fire competitive matches.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                if (selectedSection == 0 || selectedSection == 1) {
                    LegalSectionCard(
                        icon = Icons.Default.PrivacyTip,
                        title = "PRIVACY & DATA USAGE POLICY",
                        lastUpdated = "Effective: July 2026",
                        items = listOf(
                            LegalItem(
                                "1. Free Fire Game UID & Profile Data",
                                "We exclusively collect and store your Free Fire In-Game Name (IGN) and unique Game UID. This data is strictly utilized for match verification, bracket assignments, prize credits, and leaderboard rankings."
                            ),
                            LegalItem(
                                "2. Local SQLite & Network Security",
                                "Your login credentials and account statistics are stored securely inside an encrypted local SQLite database (via Android Jetpack Room) and verified with secure network calls. We never sell, share, or lease your private data to third parties."
                            ),
                            LegalItem(
                                "3. Promotion & Referral Identifiers",
                                "To manage user referral programs safely, we collect account-linked promo codes and transaction references. Device metadata is used occasionally to prevent fraudulent multi-accounting and ensure fair play."
                            )
                        )
                    )
                }

                if (selectedSection == 0 || selectedSection == 2) {
                    LegalSectionCard(
                        icon = Icons.Default.Paid,
                        title = "REFUND & TRANSACTION POLICY",
                        lastUpdated = "Effective: July 2026",
                        items = listOf(
                            LegalItem(
                                "1. Match Registration Fees",
                                "Entry fees (denominated in coins) are strictly non-refundable once registered for a tournament. This policy ensures stable brackets, guaranteed prize pools, and prevents late-withdrawal disruptions."
                            ),
                            LegalItem(
                                "2. Tournament Cancellation",
                                "In the rare event that a tournament is cancelled by BR ESPORTS administration or fails to reach minimum required capacity, registered players will receive a 100% instant auto-refund of the entry fee directly to their coin balance."
                            ),
                            LegalItem(
                                "3. Deposit and Withdrawal Processing",
                                "Deposits made via the UPI simulator are processed instantly. Withdrawals are processed manually within 12-24 hours after admin verification of game recordings. Refund claims due to self-declared connection losses or mid-match game crashes are not entertained."
                            )
                        )
                    )
                }

                if (selectedSection == 0 || selectedSection == 3) {
                    LegalSectionCard(
                        icon = Icons.Default.Gavel,
                        title = "USER CONDUCT & FAIR PLAY TERMS",
                        lastUpdated = "Effective: July 2026",
                        items = listOf(
                            LegalItem(
                                "1. Anti-Cheat & Third-Party Plug-ins",
                                "Use of auto-aim, wall-hacks, speed boosters, or any unauthorized script modification inside Garena Free Fire is strictly forbidden. Players must participate using official game clients on mobile devices."
                            ),
                            LegalItem(
                                "2. Teaming and Collusion Rules",
                                "Teaming up with opponents in Solo tournaments, deliberate match-throwing, or sharing room passwords with unregistered players will lead to immediate match disqualification."
                            ),
                            LegalItem(
                                "3. Verification & Penalties",
                                "BR ESPORTS reserves the right to request proof-of-play (screen recording or screenshot of final kills) before releasing rewards. Any player found violating conduct will face an instant, permanent device ban and forfeiture of all accumulated coins."
                            )
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer
                Text(
                    text = "By participating in any BR ESPORTS tournament, you explicitly agree to the terms listed above. For disputes, contact live support.",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
fun LegalSectionCard(
    icon: ImageVector,
    title: String,
    lastUpdated: String,
    items: List<LegalItem>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(FireOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = FireOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = lastUpdated,
                        fontSize = 10.sp,
                        color = FireOrangeLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            items.forEach { item ->
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = item.header,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.body,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

data class LegalItem(
    val header: String,
    val body: String
)
