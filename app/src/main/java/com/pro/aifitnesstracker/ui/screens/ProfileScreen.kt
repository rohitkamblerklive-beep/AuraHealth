package com.pro.aifitnesstracker.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.pro.aifitnesstracker.HealthRepository
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager
import com.pro.aifitnesstracker.utils.PdfReportGenerator
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    prefsManager: SharedPrefsManager,
    repository: HealthRepository,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onScheduleReminders: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // ─── THEME PALETTE (Matches Mockup 1:1) ────────────────────────
    val canvasBg = if (isDark) Color(0xFF0A0E17) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF131927) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF1F293D) else Color(0xFFE2E8F0)
    val cardBgAlt = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val dividerColor = if (isDark) Color(0xFF1F293D).copy(alpha = 0.6f) else Color(0xFFF1F5F9)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    // Vibrant Accents
    val cyanAccent = Color(0xFF06B6D4)
    val tealAccent = Color(0xFF14B8A6)
    val emeraldAccent = Color(0xFF10B981)
    val amberAccent = Color(0xFFF59E0B)
    val goldAccent = Color(0xFFD97706)
    val roseAccent = Color(0xFFEF4444)

    // ─── AUTHENTICATED ATHLETE STATE ───────────────────────────────
    val firebaseUser = remember { FirebaseAuth.getInstance().currentUser }
    val authDisplayName = firebaseUser?.displayName?.takeIf { it.isNotBlank() }
    val authEmail = firebaseUser?.email?.takeIf { it.isNotBlank() } ?: "rohit.fit@fitsphere.pro"

    var nameStr by remember {
        mutableStateOf(prefsManager.getUserName().ifBlank { authDisplayName ?: "Rohit Sharma" })
    }
    var userAge by remember { mutableIntStateOf(prefsManager.getUserAge().coerceAtLeast(18)) }
    var userGender by remember { mutableStateOf(prefsManager.getUserGender().ifBlank { "Male" }) }
    var heightNum by remember { mutableFloatStateOf(prefsManager.getUserHeight().toFloat().coerceAtLeast(140f)) }
    var weightNum by remember { mutableFloatStateOf(prefsManager.getWeight().coerceAtLeast(40f)) }
    var targetWeightNum by remember {
        val tw = prefsManager.getTargetWeight()
        mutableFloatStateOf(if (tw > 0f) tw else (weightNum - 4.4f).coerceAtLeast(40f))
    }
    var fitnessGoal by remember { mutableStateOf(prefsManager.getUserFitnessGoal().ifBlank { "Fat Loss & Lean Muscle" }) }

    val currentStreak = remember { prefsManager.getCurrentStreak().coerceAtLeast(14) }
    val athleteLevel = remember(currentStreak) { (currentStreak / 2).coerceAtLeast(8) }
    val isPremium = remember { prefsManager.isPremiumUser() }

    // Avatar Handling
    var avatarUriStr by remember { mutableStateOf(prefsManager.getProfileImageUri()) }
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (_: Exception) {}
            avatarUriStr = uri.toString()
            prefsManager.saveProfileImageUri(uri.toString())
        }
    }

    LaunchedEffect(avatarUriStr) {
        if (avatarUriStr.isNotBlank()) {
            try {
                val uri = Uri.parse(avatarUriStr)
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                avatarBitmap = bmp
            } catch (_: Exception) {
                avatarBitmap = null
            }
        }
    }

    val userInitials = remember(nameStr) {
        val parts = nameStr.trim().split(" ").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            else -> "RS"
        }
    }

    // Biometrics Calculations
    val startingWeightNum = remember(weightNum) {
        val sw = prefsManager.getStartingWeight()
        if (sw > 0f) sw else (weightNum + 4.4f)
    }
    val hM = heightNum / 100f
    val bmiVal = remember(weightNum, hM) {
        if (hM > 0.5f) weightNum / (hM * hM) else 22.7f
    }
    val bmiCategory = remember(bmiVal) {
        when {
            bmiVal < 18.5f -> "Underweight"
            bmiVal < 25f -> "Normal"
            bmiVal < 30f -> "Overweight"
            else -> "Obese"
        }
    }
    val bmiColor = remember(bmiVal) {
        when {
            bmiVal < 18.5f -> amberAccent
            bmiVal < 25f -> emeraldAccent
            bmiVal < 30f -> amberAccent
            else -> roseAccent
        }
    }

    val totalWeightToLose = (startingWeightNum - targetWeightNum).coerceAtLeast(0.1f)
    val weightLostSoFar = (startingWeightNum - weightNum).coerceAtLeast(0f)
    val goalProgressPct = remember(weightLostSoFar, totalWeightToLose) {
        ((weightLostSoFar / totalWeightToLose) * 100f).coerceIn(0f, 100f)
    }
    val remainingKgToGoal = (weightNum - targetWeightNum).coerceAtLeast(0f)

    // Edit Dialog States
    var showEditBiometricsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editDialogTitle by remember { mutableStateOf("") }
    var editDialogValue by remember { mutableStateOf("") }
    var editDialogAction by remember { mutableStateOf<(String) -> Unit>({}) }

    Scaffold(
        containerColor = canvasBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.profile,
                        color = textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
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
                actions = {
                    // Edit Profile Pencil Icon
                    IconButton(onClick = { showEditBiometricsDialog = true }) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(cardBgAlt),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Profile",
                                tint = cyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    // Settings Gear Button
                    IconButton(onClick = { onNavigate("SETTINGS") }) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(cardBgAlt),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ─── 1. CLEAN HERO AVATAR SECTION (Match Mockup 1:1 - NO HEAVY OUTER BOX) ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sleek Circular Avatar with Thin Gradient Story Ring
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF22D3EE), Color(0xFF14B8A6), Color(0xFF10B981))
                                )
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF))
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap!!.asImageBitmap(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = userInitials,
                                    color = cyanAccent,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Floating Minimal Camera Edit Pill
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(cyanAccent)
                            .border(2.dp, canvasBg, CircleShape)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Upload Photo",
                            tint = Color(0xFF0A0E17),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Athlete Name & Verified Badge
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clickable {
                            editDialogTitle = "Edit Athlete Name"
                            editDialogValue = nameStr
                            editDialogAction = {
                                nameStr = it
                                prefsManager.saveUserName(it)
                            }
                            showEditDialog = true
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = nameStr,
                        color = textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black
                    )
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Verified Athlete",
                        tint = cyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Email & Age Handle
                Text(
                    text = "$authEmail • Age $userAge",
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Level & Membership Pill
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(emeraldAccent.copy(alpha = if (isDark) 0.12f else 0.08f))
                        .border(1.dp, emeraldAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⚡", fontSize = 11.sp)
                        Text(
                            text = "Level $athleteLevel • ${if (isPremium) "Elite Pro" else "Elite Athlete"}",
                            color = emeraldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ─── 2. 4 COMPACT SQUIRCLE METRIC TILES (Matches Mockup) ─────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tile 1: Height
                ProfileMetricTile(
                    modifier = Modifier.weight(1f),
                    emoji = "📏",
                    label = strings.height,
                    value = "${heightNum.toInt()}",
                    unit = "cm",
                    valueColor = textPrimary,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textSecondary = textSecondary,
                    onClick = {
                        editDialogTitle = "Edit Height (cm)"
                        editDialogValue = heightNum.toInt().toString()
                        editDialogAction = { it.toFloatOrNull()?.let { v -> heightNum = v; prefsManager.saveUserHeight(v.toInt()) } }
                        showEditDialog = true
                    }
                )

                // Tile 2: Weight
                ProfileMetricTile(
                    modifier = Modifier.weight(1f),
                    emoji = "⚖️",
                    label = strings.weight,
                    value = String.format(Locale.US, "%.1f", weightNum),
                    unit = "kg",
                    valueColor = textPrimary,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textSecondary = textSecondary,
                    onClick = {
                        onNavigate("WEIGHT_DETAIL")
                    }
                )

                // Tile 3: BMI
                ProfileMetricTile(
                    modifier = Modifier.weight(1f),
                    emoji = "🎯",
                    label = "BMI",
                    value = String.format(Locale.US, "%.1f", bmiVal),
                    unit = "",
                    valueColor = bmiColor,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textSecondary = textSecondary,
                    onClick = { showEditBiometricsDialog = true }
                )

                // Tile 4: Streak
                ProfileMetricTile(
                    modifier = Modifier.weight(1f),
                    emoji = "🔥",
                    label = strings.streakCounter,
                    value = "$currentStreak",
                    unit = "d",
                    valueColor = amberAccent,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textSecondary = textSecondary,
                    onClick = { onNavigate("ACHIEVEMENTS") }
                )
            }

            // ─── 3. TARGET GOAL SLIM CARD (Matches Mockup) ───────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(22.dp))
                    .clickable { onNavigate("WEIGHT_GOAL") },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(cyanAccent)
                            )
                            Text(
                                text = "Goal: $fitnessGoal",
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (remainingKgToGoal > 0f) "-${String.format(Locale.US, "%.1f", remainingKgToGoal)} kg left" else "Goal Reached!",
                            color = cyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(cardBgAlt)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (goalProgressPct / 100f).coerceIn(0.08f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(cyanAccent, tealAccent, emeraldAccent)
                                    )
                                )
                        )
                    }

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Start: ${String.format(Locale.US, "%.1f", startingWeightNum)} kg",
                            color = textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Current: ${String.format(Locale.US, "%.1f", weightNum)} kg",
                            color = textPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target: ${String.format(Locale.US, "%.1f", targetWeightNum)} kg",
                            color = emeraldAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ─── 4. PROFILE ACTIONS (GROUPED INSET LIST - Matches Mockup) ─────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Badges & Achievements
                    ProfileNavRowItem(
                        icon = Icons.Rounded.EmojiEvents,
                        iconBg = cyanAccent.copy(alpha = 0.15f),
                        iconTint = cyanAccent,
                        title = "Badges & Achievements",
                        subtitle = "${prefsManager.getUnlockedBadges().size} Unlocked • Active Streak",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        showDivider = true,
                        dividerColor = dividerColor,
                        onClick = { onNavigate("ACHIEVEMENTS") }
                    )

                    // Comprehensive Activity Log
                    ProfileNavRowItem(
                        icon = Icons.Rounded.Insights,
                        iconBg = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                        iconTint = Color(0xFFA78BFA),
                        title = "Comprehensive Activity Log",
                        subtitle = "Full step, workout, and sleep history",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        showDivider = true,
                        dividerColor = dividerColor,
                        onClick = { onNavigate("ACTIVITY_HISTORY") }
                    )

                    // Export Health PDF Report
                    ProfileNavRowItem(
                        icon = Icons.Rounded.PictureAsPdf,
                        iconBg = emeraldAccent.copy(alpha = 0.15f),
                        iconTint = emeraldAccent,
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
            }

            // ─── 5. PRO UPGRADE BANNER (If not premium) ──────────────────────
            if (!isPremium) {
                OutlinedButton(
                    onClick = { onNavigate("PREMIUM") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, goldAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = goldAccent)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = goldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UPGRADE TO ELITE PRO MEMBERSHIP",
                        color = goldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ─── BIOMETRICS EDIT DIALOG ──────────────────────────────────────────
    if (showEditBiometricsDialog) {
        var tempAge by remember { mutableStateOf(userAge.toString()) }
        var tempHeight by remember { mutableStateOf(heightNum.toInt().toString()) }
        var tempWeight by remember { mutableStateOf(weightNum.toString()) }
        var tempTargetWeight by remember { mutableStateOf(targetWeightNum.toString()) }
        var tempGoal by remember { mutableStateOf(fitnessGoal) }

        AlertDialog(
            onDismissRequest = { showEditBiometricsDialog = false },
            title = {
                Text(
                    text = "Edit Athlete Biometrics",
                    color = textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = tempAge,
                        onValueChange = { tempAge = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempHeight,
                        onValueChange = { tempHeight = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempWeight,
                        onValueChange = { tempWeight = it },
                        label = { Text("Current Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempTargetWeight,
                        onValueChange = { tempTargetWeight = it },
                        label = { Text("Target Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempGoal,
                        onValueChange = { tempGoal = it },
                        label = { Text("Fitness Goal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        tempAge.toIntOrNull()?.let { userAge = it; prefsManager.saveUserAge(it) }
                        tempHeight.toIntOrNull()?.let { heightNum = it.toFloat(); prefsManager.saveUserHeight(it) }
                        tempWeight.toFloatOrNull()?.let { weightNum = it; prefsManager.saveWeight(it) }
                        tempTargetWeight.toFloatOrNull()?.let { targetWeightNum = it; prefsManager.setTargetWeight(it) }
                        if (tempGoal.isNotBlank()) {
                            fitnessGoal = tempGoal
                            prefsManager.saveUserFitnessGoal(tempGoal)
                        }
                        showEditBiometricsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cyanAccent)
                ) {
                    Text("Save", color = Color(0xFF0A0E17), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBiometricsDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg
        )
    }

    // ─── SIMPLE EDIT DIALOG ──────────────────────────────────────────────
    if (showEditDialog) {
        var tempVal by remember { mutableStateOf(editDialogValue) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = editDialogTitle,
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = tempVal,
                    onValueChange = { tempVal = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        editDialogAction(tempVal)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cyanAccent)
                ) {
                    Text("Save", color = Color(0xFF0A0E17), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg
        )
    }
}

// ─── SQUIRCLE METRIC TILE COMPOSABLE (Matches Mockup 1:1) ──────────────
@Composable
private fun ProfileMetricTile(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    unit: String,
    valueColor: Color,
    cardBg: Color,
    cardBorder: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Text(
                text = label,
                color = textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        color = textSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ─── PROFILE NAV ROW ITEM (Matches Mockup 1:1) ──────────────────────────
@Composable
private fun ProfileNavRowItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    textPrimary: Color,
    textSecondary: Color,
    showDivider: Boolean = false,
    dividerColor: Color = Color.Transparent,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Colored Squircle Icon Container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
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
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
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
