package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.WalletTransaction
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.testTag
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.viewmodel.TournamentViewModel
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: TournamentViewModel,
    onNavigateToSupport: () -> Unit,
    onNavigateToWallet: () -> Unit = {},
    onNavigateToPrivacyTerms: () -> Unit = {},
    onNavigateToReferEarn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localContext = context
    val user by viewModel.user.collectAsState()
    val userBookings by viewModel.userBookings.collectAsState()

    var gameUid by remember { mutableStateOf("") }
    var gameName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("avatar_red") }
    var showSupportOptionsSheet by remember { mutableStateOf(false) }

    // Update form fields when user state is loaded
    LaunchedEffect(user) {
        user?.let {
            gameUid = it.gameUid
            gameName = it.gameName
            email = it.email
            selectedAvatar = it.avatar
        }
    }

    val avatarsList = listOf(
        AvatarOption("avatar_red", "Red Slayer", Color(0xFFE53935)),
        AvatarOption("avatar_amber", "Amber Strike", Color(0xFFFF8F00)),
        AvatarOption("avatar_gold", "Gold Booyah", Color(0xFFFFD600)),
        AvatarOption("avatar_cyan", "Cyan Sniper", Color(0xFF00ACC1)),
        AvatarOption("avatar_purple", "Purple Shadow", Color(0xFF8E24AA)),
        AvatarOption("avatar_green", "Green Survival", Color(0xFF43A047))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PLAYER PROFILE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
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
            // Hero Profile Header (With selected Avatar color)
            val activeAvatarColor = avatarsList.find { it.id == selectedAvatar }?.color ?: FireOrange
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(activeAvatarColor.copy(alpha = 0.2f), SlateDarkSurface)
                        )
                    )
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Large Avatar Badge
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(activeAvatarColor)
                            .border(4.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (gameName.isNotBlank()) gameName else "NEW PLAYER",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    )
                    
                    Text(
                        text = if (gameUid.isNotBlank()) "UID: $gameUid" else "No UID Set",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireOrangeLight
                    )
                }
            }



            Spacer(modifier = Modifier.height(4.dp))

            // Player Stats Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatMetricBlock(
                    icon = Icons.Default.ConfirmationNumber,
                    title = "Registered",
                    value = "${userBookings.size}",
                    color = FireOrange,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )

                val finishedCount = userBookings.size // Simple count of historic items for metrics
                StatMetricBlock(
                    icon = Icons.Default.EmojiEvents,
                    title = "Matches Played",
                    value = "$finishedCount",
                    color = GoldBooyah,
                    modifier = Modifier.weight(1f).padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Edit Profile Form Fields
            Text(
                text = "AVATAR SELECTION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FireOrange,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(avatarsList) { avatar ->
                    val isSelected = selectedAvatar == avatar.id
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(avatar.color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedAvatar = avatar.id },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = avatar.name,
                            tint = Color.White.copy(alpha = if (isSelected) 1.0f else 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Text(
                text = "GAME CREDENTIALS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FireOrange,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = gameName,
                onValueChange = { gameName = it },
                label = { Text("Free Fire Game Name") },
                leadingIcon = { Icon(Icons.Default.SportsEsports, contentDescription = null, tint = FireOrange) },
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
                    .padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = gameUid,
                onValueChange = { gameUid = it.filter { char -> char.isDigit() } }, // numbers only
                label = { Text("Free Fire Game UID (numbers only)") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = FireOrange) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    .padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = FireOrange) },
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
                    .padding(bottom = 24.dp)
            )

            Button(
                onClick = {
                    if (gameName.isBlank() || gameUid.isBlank() || email.isBlank()) {
                        return@Button
                    }
                    if (gameUid.length < 8 || gameUid.length > 12) {
                        Toast.makeText(localContext, "Free Fire UID must be between 8 and 12 digits.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (viewModel.isUidRegistered(gameUid, currentEmail = user?.email)) {
                        Toast.makeText(localContext, "This Free Fire UID is already linked to another account.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.saveProfile(
                        gameUid = gameUid,
                        gameName = gameName,
                        email = email,
                        avatar = selectedAvatar
                    )
                },
                enabled = gameName.isNotBlank() && gameUid.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FireOrange,
                    disabledContainerColor = SlateDarkBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "SAVE PROFILE CHANGES",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToReferEarn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlateDarkSurface,
                    contentColor = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateDarkBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("refer_earn_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = FireOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REFER & EARN REWARDS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showSupportOptionsSheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlateDarkSurface,
                    contentColor = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateDarkBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = null,
                    tint = FireOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HELP & CUSTOMER SUPPORT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToPrivacyTerms,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlateDarkSurface,
                    contentColor = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateDarkBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = FireOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PRIVACY POLICY & TERMS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFEF5350)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "SIGN OUT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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
fun StatMetricBlock(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

data class AvatarOption(
    val id: String,
    val name: String,
    val color: Color
)

@Composable
fun TransactionRowItem(
    transaction: WalletTransaction,
    onApprove: (Int) -> Unit = {},
    onReject: (Int) -> Unit = {}
) {
    val isDeposit = transaction.type == "DEPOSIT"
    val badgeBg = if (isDeposit) Color(0xFF1B5E20).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f)
    val badgeIconTint = if (isDeposit) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val badgeIcon = if (isDeposit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
    
    val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = format.format(Date(transaction.timestamp))

    val statusBg = when (transaction.status) {
        "SUCCESS" -> Color(0xFF2E7D32).copy(alpha = 0.15f)
        "PENDING" -> Color(0xFFFF6D00).copy(alpha = 0.15f)
        else -> Color(0xFFC62828).copy(alpha = 0.15f)
    }
    val statusColor = when (transaction.status) {
        "SUCCESS" -> Color(0xFF4CAF50)
        "PENDING" -> Color(0xFFFF9100)
        else -> Color(0xFFEF5350)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeIconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDeposit) "COIN DEPOSIT" else "COIN WITHDRAWAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.paymentMethod} • ${transaction.accountDetail}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Ref: ${transaction.transactionRef} • $dateStr",
                    fontSize = 10.sp,
                    color = TextSecondary.copy(alpha = 0.6f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isDeposit) "+${transaction.amount}" else "-${transaction.amount}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDeposit) Color(0xFF4CAF50) else Color(0xFFEF5350)
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = transaction.status,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
        
        if (transaction.status == "PENDING") {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkBg.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp)
                    .border(1.dp, SlateDarkBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "ADMIN SIMULATOR CONTROL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireOrangeLight,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Esports admin checks this deposit proof manually. You can instantly approve or reject it to test.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onApprove(transaction.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("APPROVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        
                        OutlinedButton(
                            onClick = { onReject(transaction.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("REJECT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositCoinsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String) -> Unit,
    onQrConfirm: (Int, String, String, Boolean) -> Unit,
    onUpiSuccess: (Int, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("100") }
    
    var isProcessing by remember { mutableStateOf(false) }
    var isSuccessState by remember { mutableStateOf(false) }
    var generatedRef by remember { mutableStateOf("") }
    
    // Sandbox states
    var showSandboxGateway by remember { mutableStateOf(false) }
    var sandboxUpiApp by remember { mutableStateOf("PhonePe") } // PhonePe, GPay, Paytm
    var pinText by remember { mutableStateOf("") }
    var forceSandbox by remember { mutableStateOf(true) } // default true for emulator smoothness!

    val presetAmounts = listOf(100, 500, 1000, 2000, 5000)
    val context = LocalContext.current

    // Launcher for starting UPI Payment Activities
    val upiPayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val response = data?.getStringExtra("response") ?: data?.data?.toString() ?: ""
        val amount = amountStr.toIntOrNull() ?: 100
        
        if (response.isNotEmpty()) {
            val isSuccess = response.contains("Status=SUCCESS", ignoreCase = true) || 
                            response.contains("Status=Completed", ignoreCase = true) ||
                            response.contains("SUCCESS", ignoreCase = true)
            val txnRef = response.split("&")
                .firstOrNull { it.startsWith("txnRef=", ignoreCase = true) }
                ?.split("=")?.getOrNull(1) ?: "TXN" + (100000..999999).random()
            
            if (isSuccess) {
                onUpiSuccess(amount, txnRef, "Auto UPI Gateway (Success)")
                generatedRef = txnRef
                isSuccessState = true
            } else {
                Toast.makeText(context, "UPI Payment Failed or Cancelled.", Toast.LENGTH_LONG).show()
            }
        } else {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val txnRef = "TXN" + (100000..999999).random()
                onUpiSuccess(amount, txnRef, "Auto UPI Gateway (Success)")
                generatedRef = txnRef
                isSuccessState = true
            } else {
                Toast.makeText(context, "No UPI app returned response. Checking with bank...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val startUpiPayment = { amount: Int ->
        if (forceSandbox) {
            showSandboxGateway = true
        } else {
            val txnId = "TXN" + System.currentTimeMillis()
            val merchantUpiId = "anil612@fam"
            val merchantName = "Anil Kasde"
            val note = "BR Esports Coin Deposit"
            val upiUri = Uri.parse("upi://pay?pa=$merchantUpiId&pn=${Uri.encode(merchantName)}&mc=&tr=$txnId&tn=${Uri.encode(note)}&am=$amount&cu=INR")
            
            val intent = Intent(Intent.ACTION_VIEW, upiUri)
            try {
                upiPayLauncher.launch(intent)
            } catch (e: Exception) {
                // If no UPI app exists (e.g. emulator), auto fall back to sandbox gateway
                showSandboxGateway = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp)),
        confirmButton = {},
        dismissButton = {},
        containerColor = SlateDarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(16.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSuccessState) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "DEPOSIT INSTANTLY CREDITED",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your esports wallet balance has been automatically updated via automated UPI settlement.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                        modifier = Modifier.fillMaxWidth().border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount Credited:", fontSize = 11.sp, color = TextSecondary)
                                Text("${amountStr} Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldBooyah)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gateway:", fontSize = 11.sp, color = TextSecondary)
                                Text("Auto UPI Settlement", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Transaction Ref / UTR:", fontSize = 11.sp, color = TextSecondary)
                                Text(generatedRef, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FireOrangeLight)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("DONE", fontWeight = FontWeight.Bold)
                    }
                } else if (isProcessing) {
                    CircularProgressIndicator(color = FireOrange, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Securing automated gateway...", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("Setting up secure peer-to-peer tunnels...", fontSize = 11.sp, color = TextSecondary)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = GoldBooyah, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AUTOMATED UPI DEPOSIT",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Instant credit. Automatically opens GPay, PhonePe, Paytm and updates your balance on successful payment.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Amount input field
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Enter Amount (Coins / ₹)") },
                        placeholder = { Text("Minimum 10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireOrange,
                            unfocusedBorderColor = SlateDarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldBooyah) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presetAmounts) { amt ->
                            SuggestionChip(
                                onClick = { amountStr = amt.toString() },
                                label = { Text("+$amt", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = GoldBooyah,
                                    containerColor = SlateDarkBg
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector for Simulation / Real Gateway Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateDarkBg)
                            .border(1.dp, SlateDarkBorder, RoundedCornerShape(10.dp))
                            .clickable { forceSandbox = !forceSandbox }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = forceSandbox,
                            onCheckedChange = { forceSandbox = it },
                            colors = CheckboxDefaults.colors(checkedColor = FireOrange)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("UPI Sandbox Simulator Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Recommended for Emulator/Chrome testing", fontSize = 9.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(46.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateDarkBorder)
                        ) {
                            Text("CANCEL", fontWeight = FontWeight.Bold)
                        }
                        
                        val parsedAmt = amountStr.toIntOrNull() ?: 0
                        val isEnabled = parsedAmt >= 10
                        
                        Button(
                            onClick = {
                                isProcessing = true
                                startUpiPayment(parsedAmt)
                                isProcessing = false
                            },
                            enabled = isEnabled,
                            modifier = Modifier.weight(1f).height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldBooyah,
                                disabledContainerColor = SlateDarkBorder,
                                contentColor = SlateDarkBg
                            )
                        ) {
                            Text("PAY NOW", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )

    // Sandboxed UPI Payment Gateway Dialog Simulator
    if (showSandboxGateway) {
        val parsedAmt = amountStr.toIntOrNull() ?: 100
        AlertDialog(
            onDismissRequest = { showSandboxGateway = false },
            containerColor = Color(0xFF13151A),
            modifier = Modifier.border(1.dp, Color(0xFF2C3240), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            confirmButton = {},
            dismissButton = {},
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title Header showing PhonePe style visual top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF5F259F), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SECURE SANDBOX UPI",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VERIFIED BY NPCI", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("UPI Payment Simulator", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹$parsedAmt.00",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // App Selector Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1A1D24))
                                .padding(4.dp)
                        ) {
                            listOf("PhonePe", "Google Pay", "Paytm").forEach { app ->
                                val isSel = sandboxUpiApp == app
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFF5F259F) else Color.Transparent)
                                        .clickable { sandboxUpiApp = app }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        app,
                                        fontSize = 11.sp,
                                        color = if (isSel) Color.White else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Payee:", fontSize = 11.sp, color = TextSecondary)
                                    Text("ANIL KASDE (Booyah Esports)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("UPI Address:", fontSize = 11.sp, color = TextSecondary)
                                    Text("anil612@fam", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FireOrangeLight)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Simulated UPI Pin Entry
                        Text("ENTER 4/6 DIGIT UPI PIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..4).forEach { i ->
                                val filled = pinText.length >= i
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (filled) Color(0xFF5F259F) else Color(0xFF1A1D24))
                                        .border(1.dp, Color(0xFF2C3240), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (filled) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Sandbox Numeric Keypad for full realism
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            val rows = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("CLR", "0", "DEL")
                            )
                            rows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { char ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF1A1D24))
                                                .clickable {
                                                    if (char == "CLR") {
                                                        pinText = ""
                                                    } else if (char == "DEL") {
                                                        if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                                                    } else {
                                                        if (pinText.length < 4) pinText += char
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                char,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSandboxGateway = false },
                                modifier = Modifier.weight(1f).height(44.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3240)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("CANCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    showSandboxGateway = false
                                    isProcessing = true
                                    val reference = "UPI" + (100000000000..999999999999).random()
                                    onUpiSuccess(parsedAmt, reference, "Sandbox simulator: $sandboxUpiApp")
                                    generatedRef = reference
                                    isSuccessState = true
                                    isProcessing = false
                                },
                                enabled = pinText.length >= 4,
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5F259F))
                            ) {
                                Text("CONFIRM PAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawCoinsDialog(
    currentBalance: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var payoutMethod by remember { mutableStateOf("UPI Payout") }
    var targetAddress by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    
    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var generatedRef by remember { mutableStateOf("") }

    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            kotlinx.coroutines.delay(1500)
            generatedRef = "TXN" + (100000..999999).random()
            isProcessing = false
            isSuccess = true
        }
    }

    val parsedAmt = amountStr.toIntOrNull() ?: 0
    val validationError = when {
        amountStr.isEmpty() -> null
        parsedAmt < 100 -> "Minimum withdrawal is 100 Coins"
        parsedAmt > currentBalance -> "Insufficient balance!"
        else -> null
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp)),
        confirmButton = {},
        dismissButton = {},
        containerColor = SlateDarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(16.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSuccess) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = FireOrange,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "WITHDRAWAL SUBMITTED",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your withdrawal request has been submitted for admin approval. Once the admin approves, your payment will be transferred.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                        modifier = Modifier.fillMaxWidth().border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount Withdrawn:", fontSize = 11.sp, color = TextSecondary)
                                Text("${amountStr} Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FireOrange)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payout Channel:", fontSize = 11.sp, color = TextSecondary)
                                Text(payoutMethod, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Target Account:", fontSize = 11.sp, color = TextSecondary)
                                Text(targetAddress, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Reference ID:", fontSize = 11.sp, color = TextSecondary)
                                Text(generatedRef, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FireOrangeLight)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            val detailSummary = if (payoutMethod == "UPI Payout") targetAddress else "$targetAddress (IFSC: $ifscCode)"
                            onConfirm(parsedAmt, payoutMethod, detailSummary)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("DONE", fontWeight = FontWeight.Bold)
                    }
                } else if (isProcessing) {
                    CircularProgressIndicator(color = FireOrange, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Processing Instant Payout...", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("Connecting directly to instant settlement network", fontSize = 11.sp, color = TextSecondary)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = FireOrange, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WITHDRAW COINS",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Convert coins to instant digital cash.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Wallet: $currentBalance",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldBooyah
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Amount to Withdraw (Coins)") },
                        placeholder = { Text("Min 100") },
                        isError = validationError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FireOrange,
                            unfocusedBorderColor = SlateDarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = FireOrange) }
                    )
                    
                    validationError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(100, 200, 500, 1000).forEach { amt ->
                            SuggestionChip(
                                onClick = { amountStr = amt.toString() },
                                label = { Text("$amt", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = FireOrangeLight,
                                    containerColor = SlateDarkBg
                                )
                            )
                        }
                        
                        SuggestionChip(
                            onClick = { amountStr = currentBalance.toString() },
                            label = { Text("Max", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = GoldBooyah,
                                containerColor = SlateDarkBg
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "CHOOSE PAYOUT DESTINATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireOrange,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("UPI Payout", "Bank Transfer").forEach { ch ->
                            val isSel = payoutMethod == ch
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) FireOrange.copy(alpha = 0.15f) else SlateDarkBg)
                                    .border(
                                        width = if (isSel) 1.5.dp else 1.dp,
                                        color = if (isSel) FireOrange else SlateDarkBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { payoutMethod = ch }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ch,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) FireOrangeLight else TextSecondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (payoutMethod == "UPI Payout") {
                        OutlinedTextField(
                            value = targetAddress,
                            onValueChange = { targetAddress = it },
                            label = { Text("Enter UPI ID (GPay / Paytm / PhonePe)") },
                            placeholder = { Text("username@upi") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = targetAddress,
                            onValueChange = { targetAddress = it.filter { char -> char.isDigit() } },
                            label = { Text("Bank Account Number") },
                            placeholder = { Text("918239012359") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ifscCode,
                                onValueChange = { ifscCode = it.uppercase() },
                                label = { Text("IFSC Code") },
                                placeholder = { Text("SBIN0012345") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = accountHolder,
                                onValueChange = { accountHolder = it },
                                label = { Text("Holder Name") },
                                placeholder = { Text("John Doe") },
                                modifier = Modifier.weight(1.2f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FireOrange,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(46.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateDarkBorder)
                        ) {
                            Text("CANCEL", fontWeight = FontWeight.Bold)
                        }
                        
                        val formValid = if (payoutMethod == "UPI Payout") {
                            targetAddress.isNotBlank()
                        } else {
                            targetAddress.isNotBlank() && ifscCode.isNotBlank() && accountHolder.isNotBlank()
                        }
                        val canWithdraw = validationError == null && parsedAmt > 0 && formValid
                        
                        Button(
                            onClick = { isProcessing = true },
                            enabled = canWithdraw,
                            modifier = Modifier.weight(1f).height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FireOrange,
                                disabledContainerColor = SlateDarkBorder,
                                contentColor = Color.White
                            )
                        ) {
                            Text("WITHDRAW NOW", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportOptionsModalSheet(
    onDismiss: () -> Unit,
    onNavigateToSupport: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SlateDarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(SlateDarkBorder, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(FireOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = FireOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "HELP & CUSTOMER SUPPORT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Select a support channel to get assistance",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(color = SlateDarkBorder.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 16.dp))

            // a) Telegram Support Channel/Group
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F2B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF229ED9).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .clickable {
                        onDismiss()
                        val url = "https://t.me/BRESPORT"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Opening: $url", Toast.LENGTH_SHORT).show()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF229ED9).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Telegram Support",
                            tint = Color(0xFF229ED9),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Telegram Support Channel/Group",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF229ED9).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "OFFICIAL",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF229ED9),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "https://t.me/BRESPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF229ED9)
                        )
                        Text(
                            text = "Join @BRESPORT on Telegram for 24/7 instant live help",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFF229ED9),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // b) Email Support
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(14.dp))
                    .clickable {
                        onDismiss()
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@bresports.com")
                            putExtra(Intent.EXTRA_SUBJECT, "BR Esports Customer Support Query")
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send Email"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Contact: support@bresports.com", Toast.LENGTH_LONG).show()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(FireOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Support",
                            tint = FireOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Email Support",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "support@bresports.com",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FireOrangeLight
                        )
                        Text(
                            text = "Official mail helpdesk for account queries",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // c) Frequently Asked Questions (FAQ)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(14.dp))
                    .clickable {
                        onDismiss()
                        onNavigateToSupport()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GoldBooyah.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = "FAQ",
                            tint = GoldBooyah,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Frequently Asked Questions (FAQ)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Quick answers & live chat tickets portal",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
