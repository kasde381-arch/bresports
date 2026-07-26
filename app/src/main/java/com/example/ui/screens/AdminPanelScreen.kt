package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Match
import com.example.ui.theme.*
import com.example.ui.viewmodel.TournamentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: TournamentViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.user.collectAsState()
    val matches by viewModel.matches.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val latestVersionCode by viewModel.latestVersionCode.collectAsState()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsState()
    val remoteConfigUrl by viewModel.remoteConfigUrl.collectAsState()
    var configUrlInput by remember(remoteConfigUrl) { mutableStateOf(remoteConfigUrl) }
    val announcement by viewModel.announcement.collectAsState()
    var announcementInput by remember(announcement) { mutableStateOf(announcement) }
    var isEditingAnnouncement by remember { mutableStateOf(false) }
    
    val pendingWithdrawals = remember(transactions) {
        transactions.filter { it.type == "WITHDRAWAL" && it.status == "PENDING" }
    }
    val pendingDeposits = remember(transactions) {
        transactions.filter { it.type == "DEPOSIT" && it.status == "PENDING" }
    }
    val context = LocalContext.current

    var showCreateForm by remember { mutableStateOf(false) }

    // Create Match Form State
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tournament") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var dateInput by remember { mutableStateOf("") }
    var timeInput by remember { mutableStateOf("") }
    var gameMode by remember { mutableStateOf("Solo") } // Solo, Duo or Squad
    var mapName by remember { mutableStateOf("Bermuda") } // Bermuda, Purgatory, Kalahari
    var prizePool by remember { mutableStateOf("1000") }
    var entryFee by remember { mutableStateOf("50") }
    var perKillPrize by remember { mutableStateOf("5") }
    var slotsTotal by remember { mutableStateOf("48") }
    var roomIdInput by remember { mutableStateOf("") }
    var roomPasswordInput by remember { mutableStateOf("") }

    // Dialog controllers
    var activeRoomDialogMatch by remember { mutableStateOf<Match?>(null) }
    var activeWinnersDialogMatch by remember { mutableStateOf<Match?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = FireOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADMIN TOURNAMENT MANAGER",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 18.sp
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
        var selectedTab by remember { mutableStateOf(0) }
        val registeredUsers by viewModel.registeredUsers.collectAsState()
        var searchQuery by remember { mutableStateOf("") }

        val filteredUsers = remember(registeredUsers, searchQuery) {
            registeredUsers.filter { u ->
                searchQuery.isBlank() || 
                u.phone.contains(searchQuery, ignoreCase = true) ||
                u.gameUid.contains(searchQuery, ignoreCase = true) ||
                u.username.contains(searchQuery, ignoreCase = true) ||
                u.gameName.contains(searchQuery, ignoreCase = true)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Modern premium visual tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    .background(SlateDarkSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("TOURNAMENTS & REQUESTS", "REGISTERED PLAYERS").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) FireOrange else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // ------------------ PENDING WITHDRAWALS SECTION ------------------
                    item {
                        Text(
                            text = "PENDING WITHDRAWALS (${pendingWithdrawals.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = FireOrange,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .testTag("pending_withdrawals_header")
                                .padding(bottom = 8.dp)
                        )
                    }

                    if (pendingWithdrawals.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, SlateDarkBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "All Clear!",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "No pending user withdrawals currently require payout.",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(pendingWithdrawals, key = { it.id }) { txn ->
                            val userForTxn = registeredUsers.find { it.id == txn.userId }
                            val playerGameName = userForTxn?.gameName ?: "Unknown"
                            val playerUid = userForTxn?.gameUid ?: "Unknown"

                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Withdrawal Request #${txn.id}",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                        
                                        Text(
                                            text = "${txn.amount} Coins",
                                            fontWeight = FontWeight.Black,
                                            color = FireOrangeLight,
                                            fontSize = 16.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = GoldBooyah,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Player Game Name: $playerGameName",
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = GoldBooyah,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Player UID: $playerUid",
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payment,
                                            contentDescription = null,
                                            tint = GoldBooyah,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "UPI ID: ${txn.accountDetail}",
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ref: ${txn.transactionRef} • ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(txn.timestamp))}",
                                        fontSize = 10.sp,
                                        color = TextSecondary.copy(alpha = 0.6f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val upiId = txn.accountDetail
                                                val amount = txn.amount
                                                val note = "BR Esports Payout ${txn.transactionRef}"
                                                val upiUri = "upi://pay?pa=$upiId&pn=EsportsWinner&am=$amount&cu=INR&tn=${Uri.encode(note)}&tr=${txn.transactionRef}"
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        data = Uri.parse(upiUri)
                                                    }
                                                    val chooser = Intent.createChooser(intent, "Approve & Pay via UPI")
                                                    context.startActivity(chooser)
                                                    
                                                    // Automatically approve transaction in local DB
                                                    viewModel.approveTransaction(txn.id)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to launch UPI. Opening App Store.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .testTag("mark_as_paid_button")
                                                .weight(1.3f)
                                                .height(38.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("MARK AS PAID", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        
                                        OutlinedButton(
                                            onClick = { viewModel.rejectTransaction(txn.id) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                                            border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .testTag("reject_withdrawal_button")
                                                .weight(0.7f)
                                                .height(38.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("REJECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ------------------ PENDING DEPOSIT REQUESTS (RECEIVED) SECTION ------------------
                    item {
                        Text(
                            text = "PENDING DEPOSIT REQUESTS (RECEIVED) (${pendingDeposits.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldBooyah,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .testTag("pending_deposits_header")
                                .padding(bottom = 8.dp)
                        )
                    }

                    if (pendingDeposits.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .border(1.dp, SlateDarkBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "All Clear!",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "No pending coin deposit requests currently require verification.",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(pendingDeposits, key = { it.id }) { txn ->
                            val userForTxn = registeredUsers.find { it.id == txn.userId }
                            val playerGameName = userForTxn?.gameName ?: "Unknown"
                            val playerUid = userForTxn?.gameUid ?: "Unknown"

                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Deposit Request #${txn.id}",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                        
                                        Text(
                                            text = "+${txn.amount} Coins",
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF4CAF50),
                                            fontSize = 16.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = GoldBooyah,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Player Game Name: $playerGameName",
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = GoldBooyah,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Player UID: $playerUid",
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Receipt,
                                            contentDescription = null,
                                            tint = GoldBooyah,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Ref / UTR: ${txn.transactionRef}",
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Method: ${txn.paymentMethod} • Detail: ${txn.accountDetail}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Submitted: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(txn.timestamp))}",
                                        fontSize = 10.sp,
                                        color = TextSecondary.copy(alpha = 0.6f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.approveTransaction(txn.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .testTag("approve_coins_button")
                                                .weight(1.3f)
                                                .height(38.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("APPROVE COINS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        
                                        OutlinedButton(
                                            onClick = { viewModel.rejectTransaction(txn.id) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                                            border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .testTag("decline_deposit_button")
                                                .weight(0.7f)
                                                .height(38.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("DECLINE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ------------------ OWNER CONSOLE: TRUSTED ADMIN MANAGEMENT ------------------
            if (user?.email == "kasde381@gmail.com") {
                item {
                    var showAddAdminDialog by remember { mutableStateOf(false) }
                    val trustedAdminsStr by viewModel.trustedAdmins.collectAsState()
                    val adminList = remember(trustedAdminsStr) {
                        trustedAdminsStr.split(";").filter { it.isNotBlank() }.map {
                            val parts = it.split("|")
                            val valStr = parts.getOrNull(0) ?: ""
                            val nameStr = parts.getOrNull(1) ?: "User ($valStr)"
                            valStr to nameStr
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .border(1.dp, GoldBooyah.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = GoldBooyah,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "OWNER CONSOLE: TRUSTED ADMINS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GoldBooyah,
                                        letterSpacing = 1.sp
                                    )
                                }
                                
                                Button(
                                    onClick = { showAddAdminDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldBooyah),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = SlateDarkBg,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ADD ADMIN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateDarkBg
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Instantly grant Moderator/Admin access to any user by entering their registered Phone Number or User UID. Admins can create matches, update Room details, and distribute rewards.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )

                            if (adminList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "ACTIVE TRUSTED ADMINS (${adminList.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    adminList.forEach { (valStr, nameStr) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(SlateDarkBg, RoundedCornerShape(8.dp))
                                                .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(GoldBooyah.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SupervisorAccount,
                                                        contentDescription = null,
                                                        tint = GoldBooyah,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = nameStr,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "UID/Phone: $valStr",
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            
                                            TextButton(
                                                onClick = { viewModel.revokeTrustedAdmin(valStr) },
                                                colors = ButtonDefaults.textButtonColors(contentColor = LiveRed),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Revoke Access",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "REVOKE",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showAddAdminDialog) {
                        var inputValue by remember { mutableStateOf("") }
                        var errorMsg by remember { mutableStateOf<String?>(null) }

                        AlertDialog(
                            onDismissRequest = { showAddAdminDialog = false },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = GoldBooyah,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Trusted Admin", fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Enter your friend's registered phone number or User UID below to instantly grant them full Moderator/Admin privileges.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    
                                    OutlinedTextField(
                                        value = inputValue,
                                        onValueChange = { 
                                            inputValue = it 
                                            errorMsg = null
                                        },
                                        label = { Text("Phone Number or User UID") },
                                        placeholder = { Text("e.g. 9876543210 or 84930192") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = GoldBooyah,
                                            unfocusedBorderColor = SlateDarkBorder,
                                            focusedContainerColor = SlateDarkBg,
                                            unfocusedContainerColor = SlateDarkBg
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (errorMsg != null) {
                                        Text(
                                            text = errorMsg ?: "",
                                            color = LiveRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val trimmedInput = inputValue.trim()
                                        if (trimmedInput.isBlank()) {
                                            errorMsg = "Please enter a valid Phone Number or User UID"
                                        } else {
                                            val added = viewModel.addTrustedAdmin(trimmedInput)
                                            if (added) {
                                                showAddAdminDialog = false
                                            } else {
                                                errorMsg = "Admin already added or invalid request"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldBooyah)
                                ) {
                                    Text("GRANT PRIVILEGES", color = SlateDarkBg, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddAdminDialog = false }) {
                                    Text("CANCEL", color = TextSecondary)
                                }
                            },
                            containerColor = SlateDarkSurface
                        )
                    }
                }
            }

            // ------------------ MANAGE HOME ANNOUNCEMENT BANNER ------------------
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = FireOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "HOME ANNOUNCEMENT BANNER",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FireOrange,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            if (!isEditingAnnouncement) {
                                TextButton(
                                    onClick = { isEditingAnnouncement = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = GoldBooyah)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EDIT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isEditingAnnouncement) {
                            OutlinedTextField(
                                value = announcementInput,
                                onValueChange = { announcementInput = it },
                                label = { Text("Banner Announcement Text") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        announcementInput = announcement
                                        isEditingAnnouncement = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                                ) {
                                    Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.updateAnnouncement(announcementInput)
                                        isEditingAnnouncement = false
                                        Toast.makeText(context, "Announcement updated successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("SAVE BANNER", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SlateDarkBg, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = announcement.ifEmpty { "No active announcements. Tap Edit to broadcast one!" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (announcement.isEmpty()) TextSecondary else TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // ------------------ BROADCAST EMERGENCY NOTIFICATION PANEL ------------------
            item {
                val emergencyNotification by viewModel.emergencyNotification.collectAsState()
                var emergencyInput by remember(emergencyNotification) { mutableStateOf(emergencyNotification) }
                val context = LocalContext.current

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.dp, LiveRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = LiveRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BROADCAST EMERGENCY NOTIFICATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = LiveRed,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = emergencyInput,
                            onValueChange = { emergencyInput = it },
                            label = { Text("Emergency / Update Alert Message") },
                            placeholder = { Text("e.g. Server under maintenance for next 2 hours.") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = LiveRed,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedContainerColor = SlateDarkBg,
                                unfocusedContainerColor = SlateDarkBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (emergencyNotification.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        viewModel.updateEmergencyNotification("")
                                        Toast.makeText(context, "Emergency broadcast cleared!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                                ) {
                                    Text("CLEAR BROADCAST", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Button(
                                onClick = {
                                    if (emergencyInput.isNotBlank()) {
                                        viewModel.updateEmergencyNotification(emergencyInput)
                                        Toast.makeText(context, "Emergency notification broadcasted live!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please enter some text to send.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SEND", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                            }
                        }

                        if (emergencyNotification.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "LIVE STATUS: Currently broadcasting emergency alert to all users.",
                                color = Color.Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ------------------ PLAYER SUPPORT TICKETS SECTION ------------------
            item {
                val tickets by viewModel.supportTickets.collectAsState()
                val openTickets = remember(tickets) { tickets.filter { !it.isClosed } }
                val context = LocalContext.current

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = FireOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PLAYER SUPPORT TICKETS (${openTickets.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = FireOrange,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (openTickets.isEmpty()) {
                            Text(
                                text = "No active player support tickets! Excellent work.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            openTickets.forEach { ticket ->
                                var responseText by remember(ticket.id) { mutableStateOf("") }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "ID: ${ticket.id}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldBooyah
                                            )
                                            Text(
                                                text = "Status: OPEN",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.Green
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "Name: ${ticket.name}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "UID: ${ticket.uid}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "Issue: ${ticket.issueDescription}",
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )

                                        if (ticket.response.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SlateDarkBorder.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Column {
                                                    Text("Current Admin Response:", fontSize = 10.sp, color = GoldBooyah, fontWeight = FontWeight.Bold)
                                                    Text(ticket.response, fontSize = 11.sp, color = TextPrimary)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        OutlinedTextField(
                                            value = responseText,
                                            onValueChange = { responseText = it },
                                            placeholder = { Text("Type reply to player...", fontSize = 11.sp, color = TextSecondary) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = FireOrange,
                                                unfocusedBorderColor = SlateDarkBorder,
                                                focusedContainerColor = SlateDarkSurface,
                                                unfocusedContainerColor = SlateDarkSurface
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.closeSupportTicket(ticket.id)
                                                    Toast.makeText(context, "Ticket closed successfully!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp)
                                            ) {
                                                Text("CLOSE TICKET", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = {
                                                    if (responseText.isNotBlank()) {
                                                        viewModel.replyToSupportTicket(ticket.id, responseText)
                                                        responseText = ""
                                                        Toast.makeText(context, "Reply sent to player!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Please enter a reply message.", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp)
                                            ) {
                                                Text("SEND REPLY", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ------------------ APP VERSION CONFIG & UPDATE SIMULATOR ------------------
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = FireOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "IN-APP VERSION CHECKER (HOSTED JSON)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = FireOrange,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Automatic online version check via hosted JSON / GitHub Releases config. Fetches latest version code, release notes, and direct APK download URL.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Remote JSON Config URL Input
                        OutlinedTextField(
                            value = configUrlInput,
                            onValueChange = { configUrlInput = it },
                            label = { Text("Hosted JSON Config URL", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedLabelColor = FireOrange
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateRemoteConfigUrl(configUrlInput)
                                    viewModel.checkAppVersionOnline(currentLocalVersionCode = 1, showToastOnUpToDate = true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SAVE & CHECK ONLINE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.simulateUpdateAvailable(targetVersionCode = 2, isForce = true)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, FireOrange),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("TRIGGER v2.0 DIALOG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FireOrange)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Current Status Card
                        Surface(
                            color = SlateDarkBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Current App Version:", fontSize = 11.sp, color = TextSecondary)
                                    Text("v1.0 (Code 1)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Online Version Code:", fontSize = 11.sp, color = TextSecondary)
                                    Text("v${appUpdateInfo.latestVersionName} (Code ${appUpdateInfo.latestVersionCode})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FireOrange)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Check Status:", fontSize = 11.sp, color = TextSecondary)
                                    Text(appUpdateInfo.checkStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (appUpdateInfo.checkStatus == "SUCCESS") Color.Green else Color.Yellow)
                                }
                                if (appUpdateInfo.errorMessage != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Error: ${appUpdateInfo.errorMessage}", fontSize = 10.sp, color = Color.Red)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateLatestVersionCode("1")
                                    viewModel.checkAppVersionOnline(currentLocalVersionCode = 1)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (latestVersionCode == "1") SlateDarkBorder else Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("RESET TO v1.0", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                    }
                }
            }

            // ------------------ DAILY AUTO-MATCH GENERATOR CONTROL ------------------
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = FireOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DAILY AUTO-MATCH SCHEDULER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = FireOrange,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generate or reset the daily series matches automatically with increasing stakes (10 to 200 Coins entry fees). This is safe to trigger on a daily basis. Any existing registrations for today's series will be reset.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { 
                                viewModel.forceRefreshDailyMatches()
                                Toast.makeText(context, "Daily Matches successfully regenerated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "RUN AUTO-GENERATOR LOOP NOW",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Header Action to Toggle Create Form
            item {
                Button(
                    onClick = { showCreateForm = !showCreateForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showCreateForm) SlateDarkSurface else FireOrange
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = if (showCreateForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showCreateForm) "COLLAPSE CREATOR" else "CREATE NEW TOURNAMENT MATCH",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expandable Create Form
            item {
                AnimatedVisibility(visible = showCreateForm) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                            .padding(bottom = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "NEW TOURNAMENT SPECIFICATIONS",
                                fontWeight = FontWeight.Bold,
                                color = FireOrange,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // 1. Category Dropdown
                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                OutlinedTextField(
                                    value = selectedCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Match Category") },
                                    trailingIcon = {
                                        IconButton(onClick = { categoryExpanded = !categoryExpanded }) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = FireOrange
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { categoryExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(SlateDarkSurface)
                                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                                ) {
                                    listOf("FF MAX", "Lone Wolf", "One Tap", "Clash Squad", "Full Map", "Tournament").forEach { cat ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (cat == selectedCategory) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = FireOrange, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }
                                                    Text(
                                                        text = cat,
                                                        color = if (cat == selectedCategory) FireOrange else TextPrimary,
                                                        fontWeight = if (cat == selectedCategory) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedCategory = cat
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. Title Input
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Match / Tournament Title") },
                                placeholder = { Text("e.g. BR Esports Mega Tournament #01", color = TextSecondary.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )

                            // 3. Date & Time Inputs
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                OutlinedTextField(
                                    value = dateInput,
                                    onValueChange = { dateInput = it },
                                    label = { Text("Date (DD/MM/YYYY)") },
                                    placeholder = { Text("25/07/2026", color = TextSecondary.copy(alpha = 0.4f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                                )
                                OutlinedTextField(
                                    value = timeInput,
                                    onValueChange = { timeInput = it },
                                    label = { Text("Time (HH:MM AM/PM)") },
                                    placeholder = { Text("08:00 PM", color = TextSecondary.copy(alpha = 0.4f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                                )
                            }

                            // 4. Game Mode Selector
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Game Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextSecondary)
                                Row {
                                    listOf("Solo", "Duo", "Squad").forEach { mode ->
                                        ElevatedFilterChip(
                                            selected = gameMode == mode,
                                            onClick = {
                                                gameMode = mode
                                                slotsTotal = when (mode) {
                                                    "Solo" -> "48"
                                                    "Duo" -> "24"
                                                    else -> "12"
                                                }
                                            },
                                            label = { Text(mode) },
                                            colors = SelectableChipColors(),
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }

                            // 5. Map Selector
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Map Selection", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextSecondary)
                                Row {
                                    listOf("Bermuda", "Purgatory", "Kalahari").forEach { map ->
                                        ElevatedFilterChip(
                                            selected = mapName == map,
                                            onClick = { mapName = map },
                                            label = { Text(map) },
                                            colors = SelectableChipColors(),
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }

                            // 6. Numeric fields (Prize, Entry, Per Kill, Slots)
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                OutlinedTextField(
                                    value = prizePool,
                                    onValueChange = { prizePool = it.filter { c -> c.isDigit() } },
                                    label = { Text("Prize (Coins)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).padding(end = 2.dp)
                                )

                                OutlinedTextField(
                                    value = entryFee,
                                    onValueChange = { entryFee = it.filter { c -> c.isDigit() } },
                                    label = { Text("Fee (Coins)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                )

                                OutlinedTextField(
                                    value = perKillPrize,
                                    onValueChange = { perKillPrize = it.filter { c -> c.isDigit() } },
                                    label = { Text("Per Kill") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                )

                                OutlinedTextField(
                                    value = slotsTotal,
                                    onValueChange = { slotsTotal = it.filter { c -> c.isDigit() } },
                                    label = { Text("Slots") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).padding(start = 2.dp)
                                )
                            }

                            // 7. Room ID & Password Fields
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                OutlinedTextField(
                                    value = roomIdInput,
                                    onValueChange = { roomIdInput = it },
                                    label = { Text("Room ID") },
                                    placeholder = { Text("e.g. 849201", color = TextSecondary.copy(alpha = 0.4f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                                )
                                OutlinedTextField(
                                    value = roomPasswordInput,
                                    onValueChange = { roomPasswordInput = it },
                                    label = { Text("Room Password") },
                                    placeholder = { Text("e.g. 1234", color = TextSecondary.copy(alpha = 0.4f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (title.isBlank()) return@Button
                                    
                                    val startTime = parseDateTimeInputs(dateInput, timeInput) 
                                        ?: (System.currentTimeMillis() + (90 * 60 * 1000))

                                    viewModel.createNewMatch(
                                        title = title,
                                        gameMode = gameMode,
                                        map = mapName,
                                        dateTimeMillis = startTime,
                                        prizePool = prizePool.toIntOrNull() ?: 1000,
                                        entryFee = entryFee.toIntOrNull() ?: 0,
                                        perKillPrize = perKillPrize.toIntOrNull() ?: 5,
                                        slotsTotal = slotsTotal.toIntOrNull() ?: 48,
                                        category = selectedCategory,
                                        roomId = roomIdInput,
                                        roomPassword = roomPasswordInput
                                    )

                                    // Reset fields
                                    title = ""
                                    dateInput = ""
                                    timeInput = ""
                                    roomIdInput = ""
                                    roomPasswordInput = ""
                                    perKillPrize = "5"
                                    showCreateForm = false
                                },
                                enabled = title.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("LAUNCH ${selectedCategory.uppercase()} MATCH")
                            }
                        }
                    }
                }
            }

            // Existing Tournaments List for Admins
            item {
                Text(
                    text = "ACTIVE TOURNAMENTS REGISTRY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = FireOrange,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (matches.isEmpty()) {
                item {
                    Text(
                        "No tournaments currently in the database.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            } else {
                items(matches, key = { it.id }) { match ->
                    AdminMatchCard(
                        match = match,
                        onDelete = { viewModel.deleteMatch(match.id) },
                        onUpdateStatus = { status -> viewModel.updateMatchStatus(match, status) },
                        onConfigureRoomClick = { activeRoomDialogMatch = match },
                        onDeclareWinnersClick = { activeWinnersDialogMatch = match }
                    )
                }
            }
        }
            } else {
                // TAB 1: REGISTERED PLAYERS LIST
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        // Total users count card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .border(1.dp, FireOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "BR ESPORTS COMMUNITY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = FireOrange,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Total Registered Players",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Monitor player accounts, game credentials, and wallet tokens.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(FireOrange.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                        .border(1.dp, FireOrange.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${filteredUsers.size}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = FireOrange,
                                            modifier = Modifier.testTag("total_users_count")
                                        )
                                        Text(
                                            text = "PLAYERS",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FireOrange
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by Phone Number or Game UID...", color = TextSecondary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search icon",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
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
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("registered_users_search")
                        )
                    }

                    if (filteredUsers.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .border(1.dp, SlateDarkBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = "No results",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No players found",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Try searching with a different Phone Number or UID.",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(filteredUsers) { index, u ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                                    .testTag("user_item_card_${u.email}")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(FireOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = FireOrange,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = u.username.ifBlank { "Player" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Joined: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(u.joinedAtMillis))}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        
                                        // Coin balance badge
                                        Box(
                                            modifier = Modifier
                                                .background(GoldBooyah.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                .border(1.dp, GoldBooyah.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.MonetizationOn,
                                                    contentDescription = "Coins",
                                                    tint = GoldBooyah,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${u.coinBalance} Coins",
                                                    color = GoldBooyah,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = SlateDarkBorder, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Dynamic Grid Details (Phone, IGN/UID, Referral)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            LabelValueRow(label = "PHONE NUMBER", value = u.phone.ifBlank { "N/A" })
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LabelValueRow(label = "REFERRED BY", value = u.referredByCode.ifBlank { "Direct Sign-up" })
                                        }
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            LabelValueRow(label = "FREE FIRE IGN", value = u.gameName.ifBlank { "N/A" })
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LabelValueRow(label = "GAME UID", value = u.gameUid.ifBlank { "N/A" })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Room ID & Password Configuration Dialog
    if (activeRoomDialogMatch != null) {
        RoomConfigDialog(
            match = activeRoomDialogMatch!!,
            onDismiss = { activeRoomDialogMatch = null },
            onSave = { roomId, roomPass ->
                viewModel.updateMatchRoomCredentials(activeRoomDialogMatch!!, roomId, roomPass)
                activeRoomDialogMatch = null
            }
        )
    }

    // Declaring Winners Dialog
    if (activeWinnersDialogMatch != null) {
        DeclareWinnersDialog(
            match = activeWinnersDialogMatch!!,
            onDismiss = { activeWinnersDialogMatch = null },
            onSave = { w1, w2, w3 ->
                viewModel.declareMatchWinners(activeWinnersDialogMatch!!, w1, w2, w3)
                activeWinnersDialogMatch = null
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun AdminMatchCard(
    match: Match,
    onDelete: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onConfigureRoomClick: () -> Unit,
    onDeclareWinnersClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.title,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID: ${match.id} | Mode: ${match.gameMode} | Map: ${match.map}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LiveRed)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Credentials status preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (match.roomId != null) "Room: Active (${match.roomId})" else "Room: Pending Creds",
                    fontSize = 11.sp,
                    color = if (match.roomId != null) Color.Green else GoldBooyah,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Status: ${match.status}",
                    fontSize = 11.sp,
                    color = when (match.status) {
                        "LIVE" -> LiveRed
                        "PAST" -> TextSecondary
                        else -> FireOrange
                    },
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Admin Action Buttons Flow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Configure Room Credentials (applicable to upcoming and live)
                if (match.status != "PAST") {
                    Button(
                        onClick = onConfigureRoomClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SlateDarkBg),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldBooyah)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Config Room", fontSize = 11.sp, color = GoldBooyah, fontWeight = FontWeight.Bold)
                    }
                }

                // Dynamic Status Controls
                when (match.status) {
                    "UPCOMING" -> {
                        Button(
                            onClick = { onUpdateStatus("LIVE") },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Go Live 🔴", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "LIVE" -> {
                        Button(
                            onClick = onDeclareWinnersClick,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldBooyah, contentColor = SlateDarkBg),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Winners 🏆", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "PAST" -> {
                        Button(
                            onClick = onDeclareWinnersClick,
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDarkBg),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Winners", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomConfigDialog(
    match: Match,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var rId by remember { mutableStateOf(match.roomId ?: "") }
    var rPass by remember { mutableStateOf(match.roomPassword ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Room Credentials", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Text(
                    text = "Post Room ID and Password for '${match.title}'. Registered players will immediately see this on their bookings tab.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = rId,
                    onValueChange = { rId = it },
                    label = { Text("Room ID (Numeric / Text)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FireOrange,
                        unfocusedBorderColor = SlateDarkBorder,
                        focusedContainerColor = SlateDarkBg,
                        unfocusedContainerColor = SlateDarkBg
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = rPass,
                    onValueChange = { rPass = it },
                    label = { Text("Room Password") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FireOrange,
                        unfocusedBorderColor = SlateDarkBorder,
                        focusedContainerColor = SlateDarkBg,
                        unfocusedContainerColor = SlateDarkBg
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(rId, rPass) },
                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                enabled = rId.isNotBlank() && rPass.isNotBlank()
            ) {
                Text("Broadcast Credentials")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SlateDarkSurface
    )
}

@Composable
fun DeclareWinnersDialog(
    match: Match,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    viewModel: TournamentViewModel
) {
    var winner1 by remember { mutableStateOf(match.winner1Name ?: "") }
    var winner2 by remember { mutableStateOf(match.winner2Name ?: "") }
    var winner3 by remember { mutableStateOf(match.winner3Name ?: "") }

    // Automated Result Processing fields
    var playerUid by remember { mutableStateOf("") }
    var killsInput by remember { mutableStateOf("0") }
    var placementInput by remember { mutableStateOf("0") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Declare Match Champions", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                item {
                    Text(
                        text = "Input the winners for '${match.title}'. This will set the tournament status to COMPLETED.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = winner1,
                        onValueChange = { winner1 = it },
                        label = { Text("🏆 1st Place Champion") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = FireOrange,
                            unfocusedBorderColor = SlateDarkBorder,
                            focusedContainerColor = SlateDarkBg,
                            unfocusedContainerColor = SlateDarkBg
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = winner2,
                        onValueChange = { winner2 = it },
                        label = { Text("🥈 2nd Place") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = FireOrange,
                            unfocusedBorderColor = SlateDarkBorder,
                            focusedContainerColor = SlateDarkBg,
                            unfocusedContainerColor = SlateDarkBg
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = winner3,
                        onValueChange = { winner3 = it },
                        label = { Text("🥉 3rd Place") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = FireOrange,
                            unfocusedBorderColor = SlateDarkBorder,
                            focusedContainerColor = SlateDarkBg,
                            unfocusedContainerColor = SlateDarkBg
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }

                // ------------------ AUTOMATED RESULT PROCESSING SECTION ------------------
                item {
                    HorizontalDivider(color = SlateDarkBorder, modifier = Modifier.padding(vertical = 12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = GoldBooyah,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AUTOMATED RESULT PROCESSING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldBooyah,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Input player statistics to automatically calculate prize coins and update the player's wallet instantly.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = playerUid,
                        onValueChange = { playerUid = it },
                        label = { Text("Player Game UID") },
                        placeholder = { Text("e.g., 842910485") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldBooyah,
                            unfocusedBorderColor = SlateDarkBorder,
                            focusedContainerColor = SlateDarkBg,
                            unfocusedContainerColor = SlateDarkBg
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = killsInput,
                            onValueChange = { killsInput = it },
                            label = { Text("Kills") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GoldBooyah,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedContainerColor = SlateDarkBg,
                                unfocusedContainerColor = SlateDarkBg
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = placementInput,
                            onValueChange = { placementInput = it },
                            label = { Text("Placement") },
                            placeholder = { Text("e.g. 1") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GoldBooyah,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedContainerColor = SlateDarkBg,
                                unfocusedContainerColor = SlateDarkBg
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).padding(bottom = 12.dp)
                        )
                    }
                }

                item {
                    // Calculator preview details
                    val kills = killsInput.toIntOrNull() ?: 0
                    val placement = placementInput.toIntOrNull() ?: 0
                    val killReward = kills * match.perKillPrize
                    val placementReward = when (placement) {
                        1 -> (match.prizePool * 0.50).toInt()
                        2 -> (match.prizePool * 0.30).toInt()
                        3 -> (match.prizePool * 0.20).toInt()
                        else -> 0
                    }
                    val totalReward = killReward + placementReward

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "CALCULATION PREVIEW:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Kill Reward ($kills x ${match.perKillPrize}):", fontSize = 11.sp, color = TextSecondary)
                                Text("$killReward Coins", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Placement Reward (Pl: $placement):", fontSize = 11.sp, color = TextSecondary)
                                Text("$placementReward Coins", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = SlateDarkBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Calculated Coins:", fontSize = 12.sp, color = GoldBooyah, fontWeight = FontWeight.Bold)
                                Text("$totalReward Coins", fontSize = 12.sp, color = GoldBooyah, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val targetUid = playerUid.trim()
                            val kills = killsInput.toIntOrNull() ?: 0
                            val placement = placementInput.toIntOrNull() ?: 0

                            if (targetUid.isBlank()) {
                                Toast.makeText(context, "Please enter a valid Player UID.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.processAutomatedTournamentResult(match.id, targetUid, kills, placement)
                                playerUid = ""
                                killsInput = "0"
                                placementInput = "0"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldBooyah, contentColor = SlateDarkBg),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Calculate & Credit Player Wallet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(winner1, winner2, winner3) },
                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                enabled = winner1.isNotBlank()
            ) {
                Text("Publish Winners & Close Match", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SlateDarkSurface
    )
}

@Composable
fun SelectableChipColors() = FilterChipDefaults.filterChipColors(
        containerColor = SlateDarkBg,
        labelColor = TextSecondary,
        selectedContainerColor = FireOrange,
        selectedLabelColor = Color.White
    )

@Composable
fun LabelValueRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun parseDateTimeInputs(dateStr: String, timeStr: String): Long? {
    if (dateStr.isBlank() && timeStr.isBlank()) return null
    return try {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        }
        val cleanDate = dateStr.trim()
        val cleanTime = timeStr.trim().ifEmpty { "08:00 PM" }
        sdf.parse("$cleanDate $cleanTime")?.time
    } catch (e: Exception) {
        null
    }
}
