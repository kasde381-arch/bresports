package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.WalletTransaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.TournamentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWalletScreen(
    viewModel: TournamentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val user by viewModel.user.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    // Withdraw form states
    var withdrawAmount by remember { mutableStateOf("") }
    var withdrawUpiId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = FireOrange,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MY ESPORTS WALLET",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ------------------ COIN BALANCE DISPLAY CARD ------------------
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(FireOrange.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AVAILABLE COIN BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = GoldBooyah,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${user?.coinBalance ?: 0}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1 Coin = ₹1.00 INR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldBooyah
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ------------------ PROMINENT DEPOSIT & WITHDRAW BUTTONS (SIDE-BY-SIDE) ------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Deposit Button
                Button(
                    onClick = { showDepositDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FireOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .border(1.dp, GoldBooyah.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEPOSIT",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Withdraw Button
                Button(
                    onClick = { showWithdrawDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateDarkSurface,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = GoldBooyah,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WITHDRAW",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ------------------ ALL TRANSACTIONS HISTORY (LEDGER) ------------------
            val allSortedTransactions = remember(transactions) {
                transactions.sortedByDescending { it.timestamp }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = FireOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ALL TRANSACTIONS HISTORY",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                        }
                        if (allSortedTransactions.isNotEmpty()) {
                            Text(
                                text = "${allSortedTransactions.size} Records",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (allSortedTransactions.isEmpty()) {
                        Text(
                            text = "No transaction history found.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    } else {
                        allSortedTransactions.forEachIndexed { index, txn ->
                            TransactionItem(transaction = txn)
                            if (index < allSortedTransactions.lastIndex) {
                                HorizontalDivider(color = SlateDarkBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))
                            }
                        }
                    }
                }
            }

            if (showDepositDialog) {
                Dialog(onDismissRequest = { showDepositDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SlateDarkSurface,
                        border = BorderStroke(1.dp, FireOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        var dialogAmount by remember { mutableStateOf("100") }
                        var dialogUtr by remember { mutableStateOf("") }
                        var showManualQrSection by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Title and Close Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = null,
                                        tint = FireOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "DEPOSIT COINS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = TextPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                IconButton(onClick = { showDepositDialog = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Key ID Information Badge
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GoldBooyah,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "RAZORPAY GATEWAY CONNECTED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldBooyah
                                        )
                                        Text(
                                            "Key: rzp_test_TIWak1NzU449DM • 1 Coin = ₹1.00",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Input field: Amount
                            OutlinedTextField(
                                value = dialogAmount,
                                onValueChange = { dialogAmount = it.filter { c -> c.isDigit() } },
                                label = { Text("Enter Deposit Amount (Coins / ₹)") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = GoldBooyah
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                            // Preset Chips for Dialog amount
                            val dialogPresetAmounts = listOf(50, 100, 200, 500, 1000)
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(dialogPresetAmounts) { amt ->
                                    SuggestionChip(
                                        onClick = { dialogAmount = amt.toString() },
                                        label = { Text("+$amt Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            labelColor = GoldBooyah,
                                            containerColor = SlateDarkBg
                                        )
                                    )
                                }
                            }

                            // PROMINENT RAZORPAY PAYMENT BUTTON
                            val currentAmt = dialogAmount.toIntOrNull() ?: 0
                            Button(
                                onClick = {
                                    if (currentAmt <= 0) {
                                        Toast.makeText(context, "Please enter a valid deposit amount", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val activity = context as? com.example.MainActivity
                                    if (activity != null) {
                                        activity.startRazorpayPayment(
                                            amount = currentAmt,
                                            userEmail = user?.email ?: "",
                                            userPhone = user?.phone ?: "",
                                            viewModel = viewModel
                                        )
                                        showDepositDialog = false
                                    } else {
                                        Toast.makeText(context, "Unable to launch payment modal", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = currentAmt > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .border(1.dp, GoldBooyah.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = GoldBooyah,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (currentAmt > 0) "PAY ₹$currentAmt VIA RAZORPAY" else "PAY VIA RAZORPAY",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Manual QR Code Toggle
                            TextButton(
                                onClick = { showManualQrSection = !showManualQrSection }
                            ) {
                                Text(
                                    text = if (showManualQrSection) "Hide Manual UPI QR Option ▲" else "Alternative: Pay via UPI QR / UTR Manual Deposit ▼",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (showManualQrSection) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .border(2.dp, FireOrange, RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_payment_qr_1783579165503),
                                        contentDescription = "Payment QR Code",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val upiId = "anil612@fam"
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "OFFICIAL UPI ID",
                                                fontSize = 9.sp,
                                                color = TextSecondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                upiId,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextPrimary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(upiId))
                                                Toast.makeText(context, "UPI ID Copied!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy UPI ID",
                                                tint = GoldBooyah
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = dialogUtr,
                                    onValueChange = { dialogUtr = it },
                                    label = { Text("UTR / Transaction ID (12 digits)") },
                                    placeholder = { Text("e.g. 618392019483") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Receipt,
                                            contentDescription = null,
                                            tint = FireOrange
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                                Button(
                                    onClick = {
                                        if (currentAmt < 10) {
                                            Toast.makeText(context, "Minimum deposit is 10 coins", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (dialogUtr.isBlank() || dialogUtr.length < 6) {
                                            Toast.makeText(context, "Please enter a valid Transaction ID / UTR", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        viewModel.depositViaQr(currentAmt, dialogUtr, "anil612@fam")
                                        showDepositDialog = false
                                    },
                                    enabled = currentAmt > 0 && dialogUtr.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateDarkSurface),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        "SUBMIT UTR FOR MANUAL VERIFICATION",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showWithdrawDialog) {
                Dialog(onDismissRequest = { showWithdrawDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SlateDarkSurface,
                        border = BorderStroke(1.dp, LiveRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Title and Close Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "REQUEST PAYOUT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = LiveRed,
                                    letterSpacing = 0.5.sp
                                )
                                IconButton(onClick = { showWithdrawDialog = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Deducted instantly from balance. Payout will be transferred to your UPI ID once approved by the admin.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Withdraw Amount Input
                            OutlinedTextField(
                                value = withdrawAmount,
                                onValueChange = { withdrawAmount = it.filter { c -> c.isDigit() } },
                                label = { Text("Withdraw Amount (Coins / ₹)") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = GoldBooyah
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                            // Withdraw UPI ID Input
                            OutlinedTextField(
                                value = withdrawUpiId,
                                onValueChange = { withdrawUpiId = it },
                                label = { Text("Enter Your UPI ID for payout") },
                                placeholder = { Text("e.g. username@ybl or phone@paytm") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = LiveRed
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = LiveRed,
                                    unfocusedBorderColor = SlateDarkBorder,
                                    focusedContainerColor = SlateDarkBg,
                                    unfocusedContainerColor = SlateDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                            )

                            // Submit Button
                            Button(
                                onClick = {
                                    val amount = withdrawAmount.toIntOrNull() ?: 0
                                    if (amount < 100) {
                                        Toast.makeText(context, "Minimum withdrawal is 100 coins", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (withdrawUpiId.isBlank() || !withdrawUpiId.contains("@")) {
                                        Toast.makeText(context, "Please enter a valid UPI ID", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val currentBalance = user?.coinBalance ?: 0
                                    if (currentBalance < amount) {
                                        Toast.makeText(context, "Insufficient balance!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val success = viewModel.withdrawCoins(amount, "UPI Cashout", withdrawUpiId)
                                    if (success) {
                                        withdrawAmount = ""
                                        withdrawUpiId = ""
                                        showWithdrawDialog = false
                                    }
                                },
                                enabled = withdrawAmount.isNotBlank() && withdrawUpiId.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    "SUBMIT WITHDRAWAL REQUEST",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: WalletTransaction) {
    val isCredit = transaction.type == "DEPOSIT" || transaction.type == "MATCH_WINNING"
    val badgeBg = if (isCredit) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFC62828).copy(alpha = 0.2f)
    val badgeIconTint = if (isCredit) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val badgeIcon = if (isCredit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.displayTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                if (transaction.transactionRef.isNotBlank()) {
                    Text(
                        text = "Ref: ${transaction.transactionRef}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                Text(
                    text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp)),
                    fontSize = 9.sp,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isCredit) "+" else "-"}${transaction.amount}",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = if (isCredit) Color(0xFF4CAF50) else Color(0xFFEF5350)
            )
            
            val (statusText, statusColor) = when (transaction.status) {
                "SUCCESS", "APPROVED" -> "SUCCESS" to Color(0xFF4CAF50)
                "PENDING" -> "PENDING" to GoldBooyah
                else -> "FAILED" to Color(0xFFEF5350)
            }
            Text(
                text = statusText,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = statusColor
            )
        }
    }
}

@Composable
fun DailyBonusCard(viewModel: TournamentViewModel) {
    val context = LocalContext.current
    var lastClaimTime by remember { mutableStateOf(viewModel.getLastClaimTimestamp()) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // 24 hours in milliseconds = 86400000
    val cooldownPeriod = 24 * 60 * 60 * 1000L
    val timePassed = currentTime - lastClaimTime
    val isAvailable = timePassed >= cooldownPeriod || lastClaimTime == 0L

    var isOpening by remember { mutableStateOf(false) }
    var rollValue by remember { mutableStateOf(10) }
    var wonAmountResult by remember { mutableStateOf<Int?>(null) }

    // Live countdown update
    LaunchedEffect(lastClaimTime) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isAvailable) FireOrange.copy(alpha = 0.5f) else SlateDarkBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = FireOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "🎁 DAILY LUCKY LOOTBOX",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Claim free coins every 24 hours",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!isAvailable && lastClaimTime > 0) {
                    // Reset simulator helper
                    IconButton(
                        onClick = {
                            viewModel.resetDailyLuckyBonusCooldown()
                            lastClaimTime = 0L
                            wonAmountResult = null
                            Toast.makeText(context, "Bonus cooldown reset! Ready to spin again.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Simulate Reset",
                            tint = GoldBooyah.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isOpening) {
                // Spinning Animation UI
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(FireOrange.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, FireOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$rollValue",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldBooyah
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Unlocking Golden Lootbox... ⚡",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    LaunchedEffect(Unit) {
                        val rewards = listOf(5, 10, 15, 20, 25, 30, 50)
                        for (i in 1..20) {
                            rollValue = rewards.random()
                            kotlinx.coroutines.delay(100)
                        }
                        val finalReward = listOf(10, 15, 20, 25, 50).random()
                        rollValue = finalReward
                        wonAmountResult = finalReward
                        isOpening = false
                        viewModel.claimDailyLuckyBonus(finalReward)
                        lastClaimTime = viewModel.getLastClaimTimestamp()
                    }
                }
            } else if (wonAmountResult != null) {
                // Success claim state
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, Color(0xFF4CAF50), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "LOOTBOX CLAIMED SUCCESSFULLY!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "You won +$wonAmountResult Coins! Added to wallet.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            } else if (isAvailable) {
                // Claim Available State
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your free daily lootbox is ready!",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { isOpening = true },
                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPEN LUCKY LOOTBOX 🎁",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // Cooldown active state
                val timeLeft = cooldownPeriod - timePassed
                val hours = if (timeLeft > 0) timeLeft / (1000 * 60 * 60) else 0L
                val minutes = if (timeLeft > 0) (timeLeft % (1000 * 60 * 60)) / (1000 * 60) else 0L
                val seconds = if (timeLeft > 0) (timeLeft % (1000 * 60)) / 1000 else 0L
                val countdownStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Next lootbox unlocks in:",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = countdownStr,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = FireOrangeLight,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Come back tomorrow to test your luck again!",
                        fontSize = 10.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
