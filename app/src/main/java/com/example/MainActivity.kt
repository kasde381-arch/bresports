package com.example

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.example.data.model.AppUpdateInfo
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.FireOrange
import com.example.ui.theme.SlateDarkBg
import com.example.ui.theme.SlateDarkSurface
import com.example.ui.theme.FireOrangeLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TournamentViewModel
import org.json.JSONObject
import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setProjectId("br-esports-c9691")
                    .setApplicationId("1:550699681641:android:15a5e611ffd806c8f25ff8")
                    .setApiKey("AIzaSyDcwOCTFs2yKRu9Nxms1vBTaHAzDvzInAY")
                    .setStorageBucket("br-esports-c9691.firebasestorage.app")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize the ViewModel with our custom factory
                val viewModel: TournamentViewModel = viewModel(
                    factory = TournamentViewModel.Factory(application)
                )
                
                val isAuthChecking by viewModel.isAuthChecking.collectAsState()
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val updateInfo by viewModel.appUpdateInfo.collectAsState()
                val isUpdateDismissed by viewModel.isUpdateDismissed.collectAsState()
                val currentVersionCode = 1

                if (updateInfo.latestVersionCode > currentVersionCode && !isUpdateDismissed) {
                    AppUpdateDialog(
                        currentVersionCode = currentVersionCode,
                        updateInfo = updateInfo,
                        onDismiss = {
                            viewModel.dismissUpdateDialog()
                        },
                        onSimulateUpdateComplete = {
                            viewModel.updateLatestVersionCode(currentVersionCode.toString())
                            viewModel.dismissUpdateDialog()
                        }
                    )
                }
                
                if (isAuthChecking) {
                    SplashScreen()
                } else if (!isLoggedIn) {
                    LoginScreen(viewModel = viewModel)
                } else {
                    MainShell(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(viewModel: TournamentViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to ViewModel events like booking success or coin balance refills
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is TournamentViewModel.UIEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is TournamentViewModel.UIEvent.BookingSuccess -> {
                    snackbarHostState.showSnackbar(event.message)
                    // On booking success, navigate directly to My Registrations so they can see their ticket and Room Section!
                    navController.navigate("my_bookings") {
                        popUpTo("dashboard") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    val user by viewModel.user.collectAsState()
    val activeUserId by viewModel.activeUserId.collectAsState()
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()

    val effectiveIsAdmin = isAdmin && activeUserId.trim().lowercase() == "kasde381@gmail.com"

    val navigationItems = remember(effectiveIsAdmin, activeUserId) {
        val base = listOf(
            NavigationItem("Dashboard", "dashboard", Icons.Default.SportsEsports),
            NavigationItem("My Bookings", "my_bookings", Icons.Default.ConfirmationNumber),
            NavigationItem("Leaderboard", "leaderboard", Icons.Default.EmojiEvents),
            NavigationItem("My Wallet", "wallet", Icons.Default.AccountBalanceWallet),
            NavigationItem("Profile", "profile", Icons.Default.Person)
        )
        if (effectiveIsAdmin) {
            base + NavigationItem("Admin Panel", "admin", Icons.Default.AdminPanelSettings)
        } else {
            base
        }
    }

    LaunchedEffect(effectiveIsAdmin, currentRoute) {
        if (!effectiveIsAdmin && currentRoute == "admin") {
            navController.navigate("dashboard") {
                popUpTo("dashboard") { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            // Only show bottom navigation on core tabs
            val isCoreTab = navigationItems.any { it.route == currentRoute }
            if (isCoreTab) {
                NavigationBar(
                    containerColor = SlateDarkSurface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    navigationItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FireOrange,
                                selectedTextColor = FireOrange,
                                indicatorColor = SlateDarkBg,
                                unselectedIconColor = LocalContentColor.current.copy(alpha = 0.6f),
                                unselectedTextColor = LocalContentColor.current.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Tab 1: Dashboard
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = { matchId ->
                        navController.navigate("register/$matchId")
                    },
                    onNavigateToBookingDetail = { matchId ->
                        // Redirecting to My Registrations highlights the correct tab and opens their tickets
                        navController.navigate("my_bookings") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSupport = {
                        navController.navigate("support")
                    },
                    onNavigateToLeaderboard = {
                        navController.navigate("leaderboard")
                    },
                    onNavigateToRules = {
                        navController.navigate("rules")
                    },
                    onNavigateToTournamentMatches = {
                        navController.navigate("tournament_matches")
                    }
                )
            }

            // Sub-Screen: Dedicated Tournament Category Matches Screen
            composable("tournament_matches") {
                com.example.ui.screens.TournamentMatchesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRegister = { matchId ->
                        navController.navigate("register/$matchId")
                    },
                    onNavigateToWallet = {
                        navController.navigate("wallet")
                    }
                )
            }

            // Tab 2: My Bookings
            composable("my_bookings") {
                MyBookingsScreen(viewModel = viewModel)
            }

            // Tab 2.2: Leaderboard
            composable("leaderboard") {
                LeaderboardScreen(viewModel = viewModel)
            }

            // Tab 2.5: My Wallet
            composable("wallet") {
                MyWalletScreen(viewModel = viewModel)
            }

            // Sub-Screen: Rules & Regulations
            composable("rules") {
                RulesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Tab 3: Profile
            composable("profile") {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToSupport = {
                        navController.navigate("support")
                    },
                    onNavigateToWallet = {
                        navController.navigate("wallet")
                    },
                    onNavigateToPrivacyTerms = {
                        navController.navigate("privacy_terms")
                    },
                    onNavigateToReferEarn = {
                        navController.navigate("refer_earn")
                    }
                )
            }

            // Sub-Screen: Refer & Earn
            composable("refer_earn") {
                ReferEarnScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Sub-Screen: Privacy Policy & Terms Screen
            composable("privacy_terms") {
                PrivacyTermsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Tab 4: Admin Panel
            composable("admin") {
                if (isAdmin) {
                    AdminPanelScreen(viewModel = viewModel)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SlateDarkBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = FireOrange,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ACCESS RESTRICTED",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Admin Panel is strictly reserved for tournament administrators.\nLogged in as regular player.",
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { navController.navigate("dashboard") },
                                colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
                            ) {
                                Text("GO TO DASHBOARD", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Sub-Screen: Customer Support Screen
            composable("support") {
                CustomerSupportScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Sub-Screen: Slot Registration Form
            composable(
                route = "register/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.IntType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getInt("matchId") ?: 0
                RegistrationScreen(
                    matchId = matchId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateDialog(
    currentVersionCode: Int,
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit,
    onSimulateUpdateComplete: () -> Unit
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate,
            dismissOnClickOutside = !updateInfo.isForceUpdate,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(2.dp, FireOrange.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Update Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(FireOrange.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = FireOrange,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (updateInfo.isForceUpdate) "CRITICAL UPDATE REQUIRED" else "NEW UPDATE AVAILABLE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "v$currentVersionCode.0 → v${updateInfo.latestVersionName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FireOrangeLight
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Release Notes
                Surface(
                    color = SlateDarkBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "WHAT'S NEW:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FireOrange
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = updateInfo.releaseNotes,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isDownloading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            color = FireOrange,
                            trackColor = SlateDarkBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Downloading update... ${(downloadProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Simulate download progress
                    LaunchedEffect(Unit) {
                        while (downloadProgress < 1f) {
                            kotlinx.coroutines.delay(100)
                            downloadProgress += 0.05f
                        }
                        kotlinx.coroutines.delay(400)
                        onSimulateUpdateComplete()
                    }
                } else {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.apkUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UPDATE NOW (DOWNLOAD APK)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { isDownloading = true },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FireOrange.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, tint = FireOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SIMULATE DIRECT IN-APP INSTALL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = FireOrange
                        )
                    }

                    if (!updateInfo.isForceUpdate) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "REMIND ME LATER",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Source: Online GitHub Hosted Config JSON",
                    fontSize = 10.sp,
                    color = TextSecondary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(FireOrange.copy(alpha = 0.15f))
                    .border(2.dp, FireOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "App Logo",
                    tint = FireOrange,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BATTLE ROYALE TOURNAMENTS",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Verifying authentication session...",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = FireOrange,
                strokeWidth = 3.dp
            )
        }
    }
}
