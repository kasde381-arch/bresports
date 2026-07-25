package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.TournamentViewModel
import com.example.data.model.SupportMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSupportScreen(
    viewModel: TournamentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val supportMessages by viewModel.supportMessages.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    var showChatArea by remember { mutableStateOf(true) }
    var selectedSupportTab by remember { mutableStateOf(0) }

    // Auto-scroll to the last message when message count changes
    LaunchedEffect(supportMessages.size) {
        if (supportMessages.isNotEmpty()) {
            listState.animateScrollToItem(supportMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = FireOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CUSTOMER SUPPORT",
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
                .padding(16.dp)
        ) {
            // 1. Support Timings Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(FireOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = FireOrange,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "SUPPORT TIMINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FireOrangeLight,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "10:00 AM to 10:00 PM",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Daily instant assistance for deposits, withdrawals, and tournaments.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 1.2 Telegram Support Channel/Group Direct Link
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F2B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF229ED9).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable {
                        val url = "https://t.me/BRESPORT"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Opening: $url", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF229ED9).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Telegram Support",
                            tint = Color(0xFF229ED9),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TELEGRAM SUPPORT CHANNEL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF229ED9)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF229ED9).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF229ED9),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "https://t.me/BRESPORT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap to open @BRESPORT on Telegram for instant live support",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFF229ED9),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 1.5 Frequently Asked Questions (FAQ) Section
            FaqSection()

            // 2. Dual Support Portal Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(SlateDarkSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("LIVE SUPPORT CHAT", "PLAYER SUPPORT TICKETS").forEachIndexed { index, title ->
                    val isSelected = selectedSupportTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) FireOrange else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedSupportTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.Chat else Icons.Default.SupportAgent,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) Color.White else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
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
            }

            if (selectedSupportTab == 0) {
                // 3. Live Support Chat Window
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Support Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDarkBorder.copy(alpha = 0.3f))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Live Support Chat Portal",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "ONLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Green,
                                letterSpacing = 1.sp
                            )
                        }

                        // Chat Messages Feed
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(supportMessages) { msg ->
                                MessageBubble(msg)
                            }
                        }

                        // Message Input Area
                        Surface(
                            color = SlateDarkSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, SlateDarkBorder), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    placeholder = { Text("Ask support a question...", fontSize = 13.sp, color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (messageText.isNotBlank()) {
                                            viewModel.sendSupportMessage(messageText)
                                            messageText = ""
                                        }
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (messageText.isNotBlank()) FireOrange else SlateDarkBorder)
                                        .clickable(enabled = messageText.isNotBlank()) {
                                            viewModel.sendSupportMessage(messageText)
                                            messageText = ""
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (messageText.isNotBlank()) Color.White else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Support Tickets Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val tickets by viewModel.supportTickets.collectAsState()
                    val activeUser by viewModel.user.collectAsState()
                    val activeEmail = activeUser?.email ?: "local_user"
                    val myTickets = remember(tickets, activeEmail) { tickets.filter { it.userId == activeEmail } }

                    var ticketName by remember { mutableStateOf(activeUser?.gameName ?: "") }
                    var ticketUid by remember { mutableStateOf(activeUser?.gameUid ?: "") }
                    var ticketDesc by remember { mutableStateOf("") }
                    var showRaiseForm by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MY SUPPORT TICKETS (${myTickets.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = FireOrange,
                            letterSpacing = 1.sp
                        )

                        Button(
                            onClick = { showRaiseForm = !showRaiseForm },
                            colors = ButtonDefaults.buttonColors(containerColor = if (showRaiseForm) SlateDarkSurface else FireOrange),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            border = if (showRaiseForm) BorderStroke(1.dp, SlateDarkBorder) else null
                        ) {
                            Icon(
                                imageVector = if (showRaiseForm) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (showRaiseForm) TextSecondary else Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showRaiseForm) "CANCEL" else "RAISE TICKET",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showRaiseForm) TextSecondary else Color.White
                            )
                        }
                    }

                    if (showRaiseForm) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "RAISE NEW SUPPORT TICKET",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldBooyah,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = ticketName,
                                    onValueChange = { ticketName = it },
                                    label = { Text("Your Game Name") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = ticketUid,
                                    onValueChange = { ticketUid = it },
                                    label = { Text("Your Game UID") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = FireOrange,
                                        unfocusedBorderColor = SlateDarkBorder,
                                        focusedContainerColor = SlateDarkBg,
                                        unfocusedContainerColor = SlateDarkBg
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = ticketDesc,
                                    onValueChange = { ticketDesc = it },
                                    label = { Text("Issue Description") },
                                    placeholder = { Text("Explain your issue in detail...") },
                                    minLines = 3,
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

                                Button(
                                    onClick = {
                                        if (ticketName.isNotBlank() && ticketUid.isNotBlank() && ticketDesc.isNotBlank()) {
                                            viewModel.raiseSupportTicket(ticketName.trim(), ticketUid.trim(), ticketDesc.trim())
                                            ticketDesc = ""
                                            showRaiseForm = false
                                            Toast.makeText(context, "Ticket submitted successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SUBMIT TICKET", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    if (myTickets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(SlateDarkSurface, RoundedCornerShape(12.dp))
                                .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "You haven't raised any support tickets yet. Tap 'Raise Ticket' to submit a query.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(myTickets) { ticket ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
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
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (ticket.isClosed) SlateDarkBorder else Color.Green.copy(alpha = 0.12f)
                                                ),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (ticket.isClosed) "CLOSED" else "OPEN",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (ticket.isClosed) TextSecondary else Color.Green,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "Issue: ${ticket.issueDescription}",
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )

                                        if (ticket.response.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SlateDarkBg, RoundedCornerShape(8.dp))
                                                    .border(1.dp, SlateDarkBorder, RoundedCornerShape(8.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "SUPPORT RESPONSE:",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = FireOrangeLight
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = ticket.response,
                                                        fontSize = 11.sp,
                                                        color = TextPrimary,
                                                        lineHeight = 15.sp
                                                    )
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
        }
    }
}

@Composable
fun MessageBubble(msg: SupportMessage) {
    val isUser = msg.senderId == "user"
    val isSystem = msg.senderId == "system_bot"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = when {
            isSystem -> Alignment.CenterHorizontally
            isUser -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        if (!isSystem) {
            Text(
                text = msg.senderName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUser) FireOrangeLight else GoldBooyah,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 12.dp
                    )
                )
                .background(
                    when {
                        isSystem -> SlateDarkBorder.copy(alpha = 0.5f)
                        isUser -> FireOrange.copy(alpha = 0.2f)
                        else -> SlateDarkBorder
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isSystem -> SlateDarkBorder.copy(alpha = 0.8f)
                        isUser -> FireOrange.copy(alpha = 0.5f)
                        else -> SlateDarkBorder
                    },
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 12.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = msg.text,
                fontSize = 13.sp,
                color = TextPrimary,
                textAlign = if (isSystem) TextAlign.Center else TextAlign.Start,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun FaqSection() {
    val faqList = remember {
        listOf(
            FaqItem(
                question = "💰 How do I deposit coins & get instant credit?",
                answer = "We support instant auto-UPI payments! Go to 'My Wallet' or tap 'Deposit' under profile, enter amount, and complete with GPay, PhonePe, or Paytm. For manual QR deposits, submit your correct 12-digit UTR ref; admins verify and approve within 5-15 mins."
            ),
            FaqItem(
                question = "🎮 How do I join a custom tournament?",
                answer = "Make sure your Game Name & UID are saved in your Profile. Go to 'Dashboard', select any UPCOMING match, tap 'Register', fill in details, and pay the coin entry fee. Your slot booking ticket will show up in 'My Bookings'."
            ),
            FaqItem(
                question = "🔑 Where and when is the Custom Room ID & Password revealed?",
                answer = "Custom Room credentials (ID & password) are revealed exactly 15 minutes before match start time. Go to 'My Bookings', find your active registered match ticket, and look under the credentials section."
            )
        )
    }

    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = "FREQUENTLY ASKED QUESTIONS (FAQ)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = FireOrange,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        faqList.forEachIndexed { index, item ->
            val isExpanded = expandedIndex == index
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .border(
                        1.dp,
                        if (isExpanded) FireOrange.copy(alpha = 0.5f) else SlateDarkBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { expandedIndex = if (isExpanded) null else index }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.question,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isExpanded) FireOrange else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = SlateDarkBorder.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.answer,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class FaqItem(val question: String, val answer: String)
