package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import com.example.ui.theme.*
import com.example.ui.viewmodel.TournamentViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

enum class LoginState {
    SIGN_IN, SIGN_UP
}

private fun getGoogleSignInClient(context: Context): GoogleSignInClient {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val webClientId = if (resId != 0) context.getString(resId) else "550699681641-compute@developer.gserviceaccount.com"

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()

    return GoogleSignIn.getClient(context, gso)
}


@Composable
fun GoogleLogoRing(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_google_logo),
        contentDescription = "Google Logo",
        modifier = modifier.size(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: TournamentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var loginState by remember { mutableStateOf(LoginState.SIGN_IN) }
    var isAuthenticating by remember { mutableStateOf(false) }

    // Login Form State
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Sign Up Form State
    var signUpUsername by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPhone by remember { mutableStateOf("") }
    var signUpGameUid by remember { mutableStateOf("") }
    var signUpGameName by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpPasswordVisible by remember { mutableStateOf(false) }
    var signUpPromoCode by remember { mutableStateOf("") }

    var isPhoneVerified by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var sentVerificationCode by remember { mutableStateOf("") }
    var verificationError by remember { mutableStateOf<String?>(null) }

    // Error message display
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            val email = account?.email ?: ""
            val displayName = account?.displayName ?: ""
            val photoUrl = account?.photoUrl?.toString() ?: ""

            if (!idToken.isNullOrBlank()) {
                viewModel.signInWithGoogleIdToken(idToken, email, displayName, photoUrl) { success, msg ->
                    isAuthenticating = false
                    if (!success) {
                        errorMessage = msg
                    }
                }
            } else if (email.isNotBlank()) {
                viewModel.loginWithGoogle(email, displayName, photoUrl) { success, msg ->
                    isAuthenticating = false
                    if (!success) {
                        errorMessage = msg
                    }
                }
            } else {
                isAuthenticating = false
                errorMessage = "Google account selection was canceled."
            }
        } catch (e: ApiException) {
            isAuthenticating = false
            if (e.statusCode == 12501) { // SIGN_IN_CANCELLED
                errorMessage = "Google Sign-In canceled."
            } else {
                val email = try { task.result?.email } catch (_: Exception) { null }
                val name = try { task.result?.displayName } catch (_: Exception) { null }
                if (!email.isNullOrBlank()) {
                    viewModel.loginWithGoogle(email, name) { success, msg ->
                        if (!success) errorMessage = msg
                    }
                } else {
                    errorMessage = "Google Sign-In error (code ${e.statusCode}). Please try again."
                }
            }
        } catch (e: Exception) {
            isAuthenticating = false
            errorMessage = "Google Sign-In error: ${e.localizedMessage ?: "Unknown error"}"
        }
    }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }

    // Pulsing animation for aesthetic glow
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Gaming Arena Logo & Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp * glowScale)
                        .clip(CircleShape)
                        .background(FireOrange.copy(alpha = 0.15f))
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(FireOrange, Color(0xFFD84315))))
                        .border(3.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.br_esports_logo_1783577549992),
                        contentDescription = "BR ESPORTS Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            // App Titles
            Text(
                text = "BR ESPORTS",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your Elite Tournament Arena & Live Hub",
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Local Validation Error Banner
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFFEF5350),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { errorMessage = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (loginState == LoginState.SIGN_IN) {
                // --- SIGN IN FLOW ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SECURE ACCESS PORTAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = FireOrange,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Email Field
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { 
                                loginEmail = it 
                                errorMessage = null
                            },
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
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        // Password Field
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { 
                                loginPassword = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = FireOrange) },
                            trailingIcon = {
                                IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                    Icon(
                                        imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedContainerColor = SlateDarkSurface,
                                unfocusedContainerColor = SlateDarkSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        // Forgot Password Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    resetEmailInput = loginEmail
                                    showForgotPasswordDialog = true
                                }
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FireOrange
                                )
                            }
                        }

                        // Sign In Submit Button
                        Button(
                            onClick = {
                                if (loginEmail.isBlank() || loginPassword.isBlank()) {
                                    errorMessage = "Please enter both Email and Password"
                                } else {
                                    isAuthenticating = true
                                    errorMessage = null
                                    viewModel.loginWithEmailAndPassword(loginEmail, loginPassword) { success, msg ->
                                        isAuthenticating = false
                                        if (!success) {
                                            errorMessage = msg
                                        }
                                    }
                                }
                            },
                            enabled = !isAuthenticating,
                            colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("email_login_button")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "LOGIN TO BR ESPORTS",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        // Divider OR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SlateDarkBorder)
                            Text(
                                text = "OR CONNECT WITH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SlateDarkBorder)
                        }

                        // Google Sign In Option
                        Button(
                            onClick = {
                                isAuthenticating = true
                                errorMessage = null
                                try {
                                    val client = getGoogleSignInClient(context)
                                    client.signOut().addOnCompleteListener {
                                        val signInIntent = client.signInIntent
                                        googleSignInLauncher.launch(signInIntent)
                                    }
                                } catch (e: Exception) {
                                    isAuthenticating = false
                                    errorMessage = "Google Sign-In error: ${e.localizedMessage ?: "Failed to launch intent"}"
                                }
                            },
                            enabled = !isAuthenticating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1F1F1F)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_signin_button")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF1A73E8),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    GoogleLogoRing(modifier = Modifier.padding(end = 12.dp))
                                    Text(
                                        text = "Sign in with Google",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F1F1F)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Switch to Register Screen Toggle
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                loginState = LoginState.SIGN_UP
                                errorMessage = null
                            }
                        ) {
                            Text(
                                text = "New to the arena? ",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Sign Up Here",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireOrange
                            )
                        }
                    }
                }
            } else {
                // --- SIGN UP FLOW ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateDarkBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CREATE PLAYER ACCOUNT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = FireOrange,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 1. Account Username Field
                        OutlinedTextField(
                            value = signUpUsername,
                            onValueChange = { 
                                signUpUsername = it 
                                errorMessage = null
                            },
                            label = { Text("Account Username") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = FireOrange) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedContainerColor = SlateDarkSurface,
                                unfocusedContainerColor = SlateDarkSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        // 2. Email Address Field
                        OutlinedTextField(
                            value = signUpEmail,
                            onValueChange = { 
                                signUpEmail = it 
                                errorMessage = null
                            },
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
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        // 3. WhatsApp / Phone Number Field
                        OutlinedTextField(
                            value = signUpPhone,
                            onValueChange = { 
                                val filtered = it.filter { char -> char.isDigit() || char == '+' }
                                if (filtered != signUpPhone) {
                                    isPhoneVerified = false
                                }
                                signUpPhone = filtered
                                errorMessage = null
                            },
                            label = { Text("WhatsApp / Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = FireOrange) },
                            trailingIcon = {
                                if (isPhoneVerified) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color.Green
                                    )
                                } else {
                                    TextButton(
                                        onClick = {
                                            if (signUpPhone.length >= 10) {
                                                val code = (1000..9999).random().toString()
                                                sentVerificationCode = code
                                                verificationCode = ""
                                                showVerificationDialog = true
                                            } else {
                                                errorMessage = "Please enter a valid phone number (min 10 digits) first!"
                                            }
                                        }
                                    ) {
                                        Text("VERIFY", color = FireOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedContainerColor = SlateDarkSurface,
                                unfocusedContainerColor = SlateDarkSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        // 4. Free Fire UID Field
                        OutlinedTextField(
                            value = signUpGameUid,
                            onValueChange = { 
                                signUpGameUid = it.filter { char -> char.isDigit() } 
                                errorMessage = null
                            },
                            label = { Text("Free Fire UID (e.g., 12345678)") },
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
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        // 5. Free Fire In-Game Name (IGN) Field
                        OutlinedTextField(
                            value = signUpGameName,
                            onValueChange = { 
                                signUpGameName = it 
                                errorMessage = null
                            },
                            label = { Text("Free Fire In-Game Name (IGN)") },
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
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        // 6. Create Password Field
                        OutlinedTextField(
                            value = signUpPassword,
                            onValueChange = { 
                                signUpPassword = it 
                                errorMessage = null
                            },
                            label = { Text("Create Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = FireOrange) },
                            trailingIcon = {
                                IconButton(onClick = { signUpPasswordVisible = !signUpPasswordVisible }) {
                                    Icon(
                                        imageVector = if (signUpPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedContainerColor = SlateDarkSurface,
                                unfocusedContainerColor = SlateDarkSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        // 7. Promo / Referral Code (Optional)
                        OutlinedTextField(
                            value = signUpPromoCode,
                            onValueChange = { 
                                signUpPromoCode = it 
                                errorMessage = null
                            },
                            label = { Text("Promo / Referral Code (Optional)") },
                            leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = FireOrange) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedContainerColor = SlateDarkSurface,
                                unfocusedContainerColor = SlateDarkSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        )

                        // Sign Up Submit Button
                        Button(
                            onClick = {
                                if (signUpUsername.isBlank() || signUpEmail.isBlank() || signUpPhone.isBlank() || signUpGameUid.isBlank() || signUpGameName.isBlank() || signUpPassword.isBlank()) {
                                    errorMessage = "Please fill in all required fields"
                                } else if (!signUpEmail.contains("@") || !signUpEmail.contains(".")) {
                                    errorMessage = "Please enter a valid email address"
                                } else if (signUpPassword.length < 4) {
                                    errorMessage = "Password must be at least 4 characters"
                                } else if (!isPhoneVerified) {
                                    errorMessage = "Please verify your WhatsApp / Phone number first!"
                                } else if (signUpGameUid.length < 8 || signUpGameUid.length > 12) {
                                    errorMessage = "Free Fire UID must be between 8 and 12 digits."
                                } else if (viewModel.isUidRegistered(signUpGameUid)) {
                                    errorMessage = "This Free Fire UID is already linked to another account."
                                } else {
                                    isAuthenticating = true
                                    errorMessage = null
                                    viewModel.registerUser(
                                        username = signUpUsername,
                                        email = signUpEmail,
                                        phone = signUpPhone,
                                        gameUid = signUpGameUid,
                                        gameName = signUpGameName,
                                        password = signUpPassword,
                                        promoCode = signUpPromoCode
                                    ) { success, msg ->
                                        isAuthenticating = false
                                        if (success) {
                                            loginEmail = signUpEmail
                                            loginPassword = ""
                                            loginState = LoginState.SIGN_IN
                                            errorMessage = "Account Created Online in Firebase! Please login to continue."
                                            signUpUsername = ""
                                            signUpEmail = ""
                                            signUpPhone = ""
                                            signUpGameUid = ""
                                            signUpGameName = ""
                                            signUpPassword = ""
                                            signUpPromoCode = ""
                                            isPhoneVerified = false
                                        } else {
                                            errorMessage = msg
                                        }
                                    }
                                }
                            },
                            enabled = !isAuthenticating,
                            colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("email_signup_button")
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "REGISTER & SECURE PROFILE",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Switch to Login Screen Toggle
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                loginState = LoginState.SIGN_IN
                                errorMessage = null
                            }
                        ) {
                            Text(
                                text = "Already have an account? ",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Login Here",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FireOrange
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showVerificationDialog) {
            AlertDialog(
                onDismissRequest = { showVerificationDialog = false },
                title = { Text("Enter OTP Code", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "We have sent a simulated WhatsApp verification code to $signUpPhone.\n\nFor sandbox testing, use code: $sentVerificationCode",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { 
                                verificationCode = it.filter { c -> c.isDigit() }
                                verificationError = null
                            },
                            label = { Text("4-Digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder,
                                focusedContainerColor = SlateDarkSurface,
                                unfocusedContainerColor = SlateDarkSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (verificationError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(verificationError!!, color = LiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (verificationCode == sentVerificationCode) {
                                isPhoneVerified = true
                                showVerificationDialog = false
                                Toast.makeText(context, "Phone number verified successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                verificationError = "Invalid verification code! Try again."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
                    ) {
                        Text("Verify OTP")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVerificationDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SlateDarkSurface
            )
        }

        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSendingReset) showForgotPasswordDialog = false },
                title = {
                    Text("Reset Password via Firebase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Column {
                        Text(
                            "Enter your registered email address below. Firebase Auth will send you a password reset link:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = resetEmailInput,
                            onValueChange = { resetEmailInput = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = FireOrange,
                                unfocusedBorderColor = SlateDarkBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (resetEmailInput.isBlank()) return@Button
                            isSendingReset = true
                            viewModel.sendPasswordResetEmail(resetEmailInput) { _, _ ->
                                isSendingReset = false
                                showForgotPasswordDialog = false
                            }
                        },
                        enabled = !isSendingReset,
                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
                    ) {
                        if (isSendingReset) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Send Reset Link", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SlateDarkSurface
            )
        }
    }
}
