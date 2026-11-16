package com.pro.aifitnesstracker.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import com.pro.aifitnesstracker.AiVoiceCoachManager
import com.pro.aifitnesstracker.HealthRepository
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager
import com.pro.aifitnesstracker.data.backup.BackupManager
import com.pro.aifitnesstracker.data.backup.GoogleDriveBackupHelper
import com.pro.aifitnesstracker.utils.PdfReportGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefsManager: SharedPrefsManager,
    repository: HealthRepository,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onScheduleReminders: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // ─── THEME PALETTE (Match Mockup Pixel-Perfect) ───────────────
    val canvasBg = if (isDark) Color(0xFF0A0E17) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF131927) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF1F293D) else Color(0xFFE2E8F0)
    val dividerColor = if (isDark) Color(0xFF1F293D).copy(alpha = 0.6f) else Color(0xFFF1F5F9)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val sectionHeaderColor = if (isDark) Color(0xFF22D3EE) else Color(0xFF0E7490)
    val cyanAccent = Color(0xFF06B6D4)

    // ─── USER & AUTH STATE ─────────────────────────────────────────
    val firebaseUser = remember { FirebaseAuth.getInstance().currentUser }
    val isLoggedIn = firebaseUser != null
    val userName = remember {
        prefsManager.getUserName().ifBlank { firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: "Athlete Rohit" }
    }
    val userEmail = remember {
        firebaseUser?.email?.takeIf { it.isNotBlank() } ?: "rohit.fit@fitsphere.pro"
    }

    val userInitials = remember(userName) {
        val parts = userName.trim().split(" ").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            else -> "RS"
        }
    }

    // ─── PREFERENCE STATES ─────────────────────────────────────────
    var currentTheme by remember { mutableStateOf(prefsManager.getAppTheme()) }
    var currentLanguageCode by remember { mutableStateOf(prefsManager.getLanguage()) }
    val coachManager = remember { AiVoiceCoachManager.getInstance(context) }
    var isVoiceCoachOn by remember { mutableStateOf(coachManager.isEnabled()) }

    var isWaterReminderOn by remember { mutableStateOf(prefsManager.isWaterReminderEnabled()) }
    var isDailySummaryAlarmOn by remember { mutableStateOf(prefsManager.isDailyReminderEnabled()) }

    // Dialog & Sync States
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isGDriveSyncing by remember { mutableStateOf(false) }

    // Backup Helpers
    val backupManager = remember { BackupManager(context) }
    val gDriveHelper = remember { GoogleDriveBackupHelper(context) }
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).runCatching {
            getResult(com.google.android.gms.common.api.ApiException::class.java)
        }.getOrNull()

        if (account != null) {
            isGDriveSyncing = true
            scope.launch {
                val token = gDriveHelper.getAccessToken(account)
                if (token != null) {
                    val ok = gDriveHelper.backupToDrive(token)
                    Toast.makeText(context, if (ok) "Cloud backup uploaded to Google Drive!" else "Drive upload failed", Toast.LENGTH_SHORT).show()
                }
                isGDriveSyncing = false
            }
        }
    }

    Scaffold(
        containerColor = canvasBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Settings",
                            color = textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        // Version badge pill
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(cyanAccent.copy(alpha = 0.15f))
                                .border(1.dp, cyanAccent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "v2.4.0",
                                color = cyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = canvasBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(canvasBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ─── ACCOUNT SUMMARY MINI CARD (Match Mockup) ─────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(22.dp))
                    .clickable { onNavigate("PROFILE") },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar Thumbnail with Thin Gradient Story Ring
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF22D3EE), Color(0xFF14B8A6), Color(0xFF10B981))
                                    )
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userInitials,
                                    color = cyanAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Column {
                            Text(
                                text = userName,
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isLoggedIn) "Google Account Synced" else "Athlete Profile (Local)",
                                color = textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Manage Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(cyanAccent.copy(alpha = 0.12f))
                            .border(1.dp, cyanAccent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Manage",
                            color = cyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ─── SECTION 1: PREFERENCES ────────────────────────────────
            SettingsGroupSection(
                title = "PREFERENCES",
                titleColor = sectionHeaderColor,
                cardBg = cardBg,
                cardBorder = cardBorder,
                dividerColor = dividerColor
            ) {
                // Theme Item with live Dark / Light quick pills
                SettingsRowItem(
                    icon = Icons.Rounded.Palette,
                    iconBgColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                    iconTint = Color(0xFF818CF8),
                    title = "App Theme",
                    subtitle = if (isDark) "Dark Neon (AMOLED)" else "Clean Light Mode",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = true,
                    dividerColor = dividerColor,
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) cyanAccent else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                    .clickable {
                                        currentTheme = "ObsidianPro"
                                        prefsManager.saveAppTheme("ObsidianPro")
                                        onThemeChange("ObsidianPro")
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Dark",
                                    color = if (isDark) Color(0xFF0A0E17) else textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isDark) cyanAccent else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                    .clickable {
                                        currentTheme = "LightClean"
                                        prefsManager.saveAppTheme("LightClean")
                                        onThemeChange("LightClean")
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Light",
                                    color = if (!isDark) Color(0xFFFFFFFF) else textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )

                // Language
                SettingsRowItem(
                    icon = Icons.Rounded.Language,
                    iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    iconTint = Color(0xFFFBBF24),
                    title = "Language",
                    subtitle = getLanguageDisplayName(currentLanguageCode),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = true,
                    dividerColor = dividerColor,
                    onClick = { showLanguageDialog = true }
                )

                // Voice Coach Switch
                SettingsRowItem(
                    icon = Icons.Rounded.VolumeUp,
                    iconBgColor = Color(0xFFEC4899).copy(alpha = 0.15f),
                    iconTint = Color(0xFFF472B6),
                    title = "Audio Voice Coach (TTS)",
                    subtitle = "Live pace and rep announcements",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = false,
                    dividerColor = dividerColor,
                    trailingContent = {
                        Switch(
                            checked = isVoiceCoachOn,
                            onCheckedChange = {
                                isVoiceCoachOn = it
                                coachManager.setEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = cyanAccent
                            )
                        )
                    }
                )
            }

            // ─── SECTION 2: GOALS & REMINDERS ──────────────────────────
            SettingsGroupSection(
                title = "GOALS & REMINDERS",
                titleColor = sectionHeaderColor,
                cardBg = cardBg,
                cardBorder = cardBorder,
                dividerColor = dividerColor
            ) {
                // Step Goal
                SettingsRowItem(
                    icon = Icons.Rounded.DirectionsWalk,
                    iconBgColor = Color(0xFF14B8A6).copy(alpha = 0.15f),
                    iconTint = Color(0xFF2DD4BF),
                    title = "Daily Step Goal",
                    subtitle = "Currently set to ${prefsManager.getStepGoal()} steps",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = true,
                    dividerColor = dividerColor,
                    onClick = { onNavigate("STEP_GOAL") }
                )

                // Target Weight & Loss Goal
                val userWeight = prefsManager.getWeight().let { if (it <= 0f) 68.4f else it }
                val targetWeight = prefsManager.getTargetWeight().let { if (it <= 0f) 65.0f else it }
                SettingsRowItem(
                    icon = Icons.Rounded.MonitorWeight,
                    iconBgColor = Color(0xFF06B6D4).copy(alpha = 0.15f),
                    iconTint = Color(0xFF22D3EE),
                    title = "Target Weight & Loss Goal",
                    subtitle = "Current %.1f kg • Target %.1f kg".format(userWeight, targetWeight),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = true,
                    dividerColor = dividerColor,
                    onClick = { onNavigate("WEIGHT_GOAL") }
                )

                // Hydration Reminder Switch
                SettingsRowItem(
                    icon = Icons.Rounded.WaterDrop,
                    iconBgColor = Color(0xFF0EA5E9).copy(alpha = 0.15f),
                    iconTint = Color(0xFF38BDF8),
                    title = "Hydration Reminders",
                    subtitle = "Every ${prefsManager.getWaterReminderInterval()} hours with quick-log",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = true,
                    dividerColor = dividerColor,
                    trailingContent = {
                        Switch(
                            checked = isWaterReminderOn,
                            onCheckedChange = {
                                isWaterReminderOn = it
                                prefsManager.setWaterReminderEnabled(it)
                                onScheduleReminders()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = cyanAccent
                            )
                        )
                    }
                )

                // Evening Goal Summary Alarm
                SettingsRowItem(
                    icon = Icons.Rounded.Alarm,
                    iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                    iconTint = Color(0xFFA78BFA),
                    title = "Evening Goal Check-in",
                    subtitle = "${prefsManager.getReminderTime()} daily summary alarm",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = false,
                    dividerColor = dividerColor,
                    onClick = { onNavigate("REMINDER_SETTINGS") }
                )
            }

            // ─── SECTION 3: DATA & CLOUD ───────────────────────────────
            SettingsGroupSection(
                title = "DATA & CLOUD",
                titleColor = sectionHeaderColor,
                cardBg = cardBg,
                cardBorder = cardBorder,
                dividerColor = dividerColor
            ) {
                // Cloud Backup & Restore
                SettingsRowItem(
                    icon = Icons.Rounded.CloudUpload,
                    iconBgColor = Color(0xFF10B981).copy(alpha = 0.15f),
                    iconTint = Color(0xFF34D399),
                    title = "Cloud Backup & Restore",
                    subtitle = if (isGDriveSyncing) "Syncing..." else "Synced with Google Drive / Firebase",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = true,
                    dividerColor = dividerColor,
                    onClick = {
                        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                        if (lastAccount != null && lastAccount.grantedScopes.contains(Scope("https://www.googleapis.com/auth/drive.appdata"))) {
                            isGDriveSyncing = true
                            scope.launch {
                                val token = gDriveHelper.getAccessToken(lastAccount)
                                if (token != null) {
                                    val ok = gDriveHelper.backupToDrive(token)
                                    Toast.makeText(context, if (ok) "Cloud backup uploaded to Google Drive!" else "Drive upload failed", Toast.LENGTH_SHORT).show()
                                }
                                isGDriveSyncing = false
                            }
                        } else {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    }
                )

                // Export Medical / Health PDF Report
                SettingsRowItem(
                    icon = Icons.Rounded.PictureAsPdf,
                    iconBgColor = Color(0xFFF97316).copy(alpha = 0.15f),
                    iconTint = Color(0xFFFB923C),
                    title = "Export Health PDF Report",
                    subtitle = "Doctor-ready 30-day comprehensive summary",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = false,
                    dividerColor = dividerColor,
                    onClick = {
                        scope.launch {
                            try {
                                val uri = PdfReportGenerator.generateWeeklyReport(context, prefsManager)
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share AuraHealth Report"))
                                } else {
                                    Toast.makeText(context, "Failed to generate PDF report", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // ─── SECTION 4: SUPPORT & ABOUT ────────────────────────────
            SettingsGroupSection(
                title = "SUPPORT & LEGAL",
                titleColor = sectionHeaderColor,
                cardBg = cardBg,
                cardBorder = cardBorder,
                dividerColor = dividerColor
            ) {
                // Instructions & Sensor Guide
                SettingsRowItem(
                    icon = Icons.Rounded.MenuBook,
                    iconBgColor = Color(0xFF38BDF8).copy(alpha = 0.15f),
                    iconTint = Color(0xFF38BDF8),
                    title = "Instructions & Sensor Guide",
                    subtitle = "How to calibrate sensors and camera monitor",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = true,
                    dividerColor = dividerColor,
                    onClick = { onNavigate("INSTRUCTIONS") }
                )

                // Privacy Policy
                SettingsRowItem(
                    icon = Icons.Rounded.Security,
                    iconBgColor = Color(0xFF10B981).copy(alpha = 0.15f),
                    iconTint = Color(0xFF34D399),
                    title = "Privacy Policy",
                    subtitle = "Your health data stays private on-device",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    showDivider = true,
                    dividerColor = dividerColor,
                    onClick = { onNavigate("PRIVACY_POLICY") }
                )

                // Sign Out
                SettingsRowItem(
                    icon = Icons.Rounded.Logout,
                    iconBgColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                    iconTint = Color(0xFFF87171),
                    title = "Sign Out",
                    subtitle = "Safe offline local storage preserved",
                    textPrimary = Color(0xFFF87171),
                    textSecondary = textSecondary,
                    showDivider = false,
                    dividerColor = dividerColor,
                    onClick = { showLogoutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ─── DIALOGS ─────────────────────────────────────────────────────────

    // Language Dialog
    if (showLanguageDialog) {
        val languages = listOf(
            "en" to "English (United States)",
            "hi" to "Hindi (हिंदी)",
            "mr" to "Marathi (मराठी)",
            "gu" to "Gujarati (ગુજરાતી)",
            "ta" to "Tamil (தமிழ்)"
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select App Language", fontWeight = FontWeight.Bold, color = textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.forEach { (code, name) ->
                        val isSelected = currentLanguageCode == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) cyanAccent.copy(alpha = 0.15f) else cardBg)
                                .clickable {
                                    currentLanguageCode = code
                                    prefsManager.saveLanguage(code)
                                    onLanguageChange(code)
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = name, color = textPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = cyanAccent)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close", color = cyanAccent)
                }
            },
            containerColor = cardBg
        )
    }

    // Sign Out Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out of AuraHealth?", fontWeight = FontWeight.Bold, color = textPrimary) },
            text = {
                Text("Your local fitness data and daily streaks are preserved safely on your device.", color = textSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                        showLogoutDialog = false
                        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg
        )
    }
}

// ─── REUSABLE GROUP SECTION CONTAINER ──────────────────────────────────
@Composable
private fun SettingsGroupSection(
    title: String,
    titleColor: Color,
    cardBg: Color,
    cardBorder: Color,
    dividerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = titleColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorder, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

// ─── REUSABLE ROW ITEM (Matches Mockup with Squircle Icons) ───────────
@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    textPrimary: Color,
    textSecondary: Color,
    showDivider: Boolean = false,
    dividerColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Colored Squircle Icon Container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        color = textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 62.dp, end = 14.dp),
                color = dividerColor,
                thickness = 0.8.dp
            )
        }
    }
}

private fun getLanguageDisplayName(code: String): String {
    return when (code) {
        "hi" -> "Hindi (हिंदी)"
        "mr" -> "Marathi (मराठी)"
        "gu" -> "Gujarati (ગુજરાતી)"
        "ta" -> "Tamil (தமிழ்)"
        else -> "English (United States)"
    }
}
