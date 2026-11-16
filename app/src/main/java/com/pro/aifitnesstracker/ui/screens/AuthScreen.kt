package com.pro.aifitnesstracker.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.pro.aifitnesstracker.FirebaseSyncManager
import com.pro.aifitnesstracker.R
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager
import com.pro.aifitnesstracker.utils.findActivity
import java.util.concurrent.TimeUnit

private val BG @Composable get() = MaterialTheme.colorScheme.background
private val CARD @Composable get() = MaterialTheme.colorScheme.surface
private val ACCENT @Composable get() = MaterialTheme.colorScheme.primary
private val TEXT_SECONDARY @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val GOOGLE_RED = Color(0xFFEA4335)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    prefsManager: SharedPrefsManager,
    onSuccessLogin: (Boolean) -> Unit,
    onSkip: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    var isLoginMode by remember { mutableStateOf(true) }

    // Email/Password fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // ---- After login success — fetch profile & sync ----
    fun handlePostLogin() {
        val userId = auth.currentUser?.uid ?: run {
            isLoading = false
            onSuccessLogin(false)
            return
        }
        FirebaseSyncManager.fetchProfileFromCloud(
            userId,
            onSuccess = { data ->
                if (data != null) {
                    val isPremium = data["isPremium"] as? Boolean ?: false
                    val premiumExpiry = (data["premiumExpiry"] as? Number)?.toLong() ?: 0L
                    prefsManager.setPremiumUser(isPremium, premiumExpiry)

                    prefsManager.saveUserAge((data["age"] as? Number)?.toInt() ?: 25)
                    prefsManager.saveUserGender((data["gender"] as? String) ?: "Male")
                    prefsManager.saveUserHeight((data["height"] as? Number)?.toInt() ?: 170)
                    prefsManager.saveUserWeight((data["weight"] as? Number)?.toInt() ?: 70)
                    prefsManager.saveUserFitnessGoal((data["goal"] as? String) ?: "Stay Fit")
                    prefsManager.saveUserActivityLevel((data["activityLevel"] as? String) ?: "Lightly Active")
                    prefsManager.saveUserName((data["name"] as? String) ?: "")
                    prefsManager.setProfileSetupDone()

                    val appDatabase = com.pro.aifitnesstracker.AppDatabase.getDatabase(context)
                    FirebaseSyncManager.restoreFullBackup(userId, appDatabase, prefsManager) {
                        FirebaseSyncManager.fetchAchievementsFromCloud(userId, onSuccess = { badges ->
                            badges.forEach { prefsManager.unlockBadge(it) }
                        })
                        isLoading = false
                        onSuccessLogin(true)
                    }
                } else {
                    if (prefsManager.isProfileSetupDone()) {
                        // Sync existing local profile to cloud
                        FirebaseSyncManager.syncProfileToCloud(
                            userId = userId,
                            age = prefsManager.getUserAge(),
                            gender = prefsManager.getUserGender(),
                            height = prefsManager.getUserHeight(),
                            weight = prefsManager.getUserWeight(),
                            goal = prefsManager.getUserFitnessGoal(),
                            activityLevel = prefsManager.getUserActivityLevel(),
                            name = prefsManager.getUserName().ifBlank { auth.currentUser?.displayName ?: auth.currentUser?.email ?: "User" }
                        )
                        // Sync achievements
                        FirebaseSyncManager.syncAchievementsToCloud(userId, prefsManager.getUnlockedBadges())
                        
                        // Sync local database records to cloud for all logged in users
                        val appDatabase = com.pro.aifitnesstracker.AppDatabase.getDatabase(context)
                        FirebaseSyncManager.uploadFullBackup(userId, appDatabase)

                        isLoading = false
                        onSuccessLogin(true)
                    } else {
                        isLoading = false
                        onSuccessLogin(false)
                    }
                }
            },
            onFailure = {
                isLoading = false
                onSuccessLogin(false)
            }
        )
    }

    // ---- Email Auth ----
    fun handleEmailAuth() {
        if (email.isBlank() || password.isBlank()) { errorMessage = strings.pleaseFillAllFields; return }
        if (password.length < 6) { errorMessage = strings.passwordTooShort; return }
        errorMessage = null; isLoading = true
        if (isLoginMode) {
            auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) handlePostLogin()
                    else { isLoading = false; errorMessage = task.exception?.localizedMessage ?: "Login failed" }
                }
        } else {
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        auth.currentUser?.uid?.let { uid ->
                            val appDb = com.pro.aifitnesstracker.AppDatabase.getDatabase(context)
                            FirebaseSyncManager.uploadFullBackup(uid, appDb)
                        }
                        isLoading = false
                        onSuccessLogin(false)
                    } else {
                        isLoading = false
                        errorMessage = task.exception?.localizedMessage ?: "Registration failed"
                    }
                }
        }
    }

    // ---- Google Sign-In ----
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            isLoading = true
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) handlePostLogin()
                    else { isLoading = false; errorMessage = authTask.exception?.localizedMessage ?: "Google sign-in failed" }
                }
        } catch (e: ApiException) {
            isLoading = false
            errorMessage = "Google sign-in cancelled or failed: ${e.statusCode}"
        }
    }

    fun launchGoogleSignIn() {
        errorMessage = null
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    // ---- UI ----
    Scaffold(containerColor = BG) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BG, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))))
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header (Logo + Title + Subtitle) grouped together
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(ACCENT.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .border(1.dp, ACCENT.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = ACCENT, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isLoginMode) strings.welcomeBack else strings.createAccount,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isLoginMode) strings.signInSubtitle else "Start your premium fitness journey",
                        fontSize = 13.sp,
                        color = TEXT_SECONDARY,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CARD)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Error / Success
                    errorMessage?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF5252).copy(alpha = 0.1f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(it, color = Color(0xFFFF5252), fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    successMessage?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ACCENT.copy(alpha = 0.1f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = ACCENT, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(it, color = ACCENT, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    // ===== EMAIL / PASSWORD FIELDS =====
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = strings.emailAddress,
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = ACCENT) },
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(Modifier.height(10.dp))
                    AuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = strings.password,
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = ACCENT) },
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible }
                    )
                    Spacer(Modifier.height(16.dp))
                    if (isLoading) {
                        CircularProgressIndicator(color = ACCENT)
                    } else {
                        AuthButton(
                            text = if (isLoginMode) strings.signIn else strings.createAccount,
                            color = ACCENT,
                            textColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = { handleEmailAuth() }
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = { isLoginMode = !isLoginMode; errorMessage = null },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = if (isLoginMode) "New here? Create Account" else "Have account? Sign In",
                                color = ACCENT, fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ---- Google Sign-In ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    Text("  OR  ", color = TEXT_SECONDARY, fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }
                Spacer(Modifier.height(16.dp))
                // Google Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CARD)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .clickable { if (!isLoading) launchGoogleSignIn() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("G", fontSize = 20.sp, fontWeight = FontWeight.Black, color = ACCENT)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        strings.continueWithGoogle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Guest mode
                TextButton(onClick = {
                    prefsManager.setGuestMode(true)
                    onSkip()
                }) {
                    Text(
                        strings.skipOffline,
                        color = TEXT_SECONDARY,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ---- Reusable Composables ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onPasswordToggle?.invoke() }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null,
                        tint = ACCENT
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = ACCENT,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            cursorColor = ACCENT,
            focusedLabelColor = ACCENT,
            unfocusedLabelColor = TEXT_SECONDARY,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AuthButton(
    text: String,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(text, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
