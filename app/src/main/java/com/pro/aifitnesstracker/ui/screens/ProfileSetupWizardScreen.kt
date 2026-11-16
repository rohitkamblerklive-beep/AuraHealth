package com.pro.aifitnesstracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.SharedPrefsManager
import kotlin.math.roundToInt

@Composable
fun ProfileSetupWizardScreen(
    prefsManager: SharedPrefsManager,
    onLanguageChanged: (String) -> Unit,
    onFinish: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0A0E17) else Color(0xFFF8F9FE)

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    // Step 0: Language Selection
    var userLanguage by remember { mutableStateOf(prefsManager.getLanguage()) }

    // Step 1: Personal Info
    var userName by remember { mutableStateOf(prefsManager.getUserName().ifEmpty { "" }) }
    var userAge by remember { mutableIntStateOf(prefsManager.getUserAge().coerceIn(12, 90)) }
    var userGender by remember { mutableStateOf(prefsManager.getUserGender().ifEmpty { "Male" }) }

    // Step 2: Body Stats
    var userHeight by remember { mutableIntStateOf(prefsManager.getUserHeight().coerceIn(120, 220)) }
    var userWeight by remember { mutableIntStateOf(prefsManager.getUserWeight().coerceIn(30, 200)) }

    // Step 3: Fitness Goal
    var fitnessGoal by remember { mutableStateOf(prefsManager.getUserFitnessGoal().ifEmpty { "Stay Fit" }) }

    // Step 4: Activity Level
    var activityLevel by remember { mutableStateOf(prefsManager.getUserActivityLevel().ifEmpty { "Lightly Active" }) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Progress & Step Dots
            WizardHeader(
                currentStep = currentStep,
                totalSteps = totalSteps,
                lang = userLanguage,
                onStepClick = { step ->
                    if (step < currentStep) {
                        currentStep = step
                    }
                }
            )

            // Step Content (Animated slide & fade)
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 2 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { it / 2 } + fadeOut()
                    }
                },
                label = "step_anim",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> StepLanguage(
                        selectedLanguage = userLanguage,
                        onLanguageSelected = { code ->
                            userLanguage = code
                            prefsManager.saveLanguage(code)
                            onLanguageChanged(code)
                        },
                        lang = userLanguage
                    )
                    1 -> StepPersonalInfo(
                        name = userName,
                        onNameChange = { userName = it },
                        age = userAge,
                        onAgeChange = { userAge = it },
                        gender = userGender,
                        onGenderChange = { userGender = it },
                        lang = userLanguage
                    )
                    2 -> StepBodyStats(
                        height = userHeight,
                        onHeightChange = { userHeight = it },
                        weight = userWeight,
                        onWeightChange = { userWeight = it },
                        gender = userGender,
                        lang = userLanguage
                    )
                    3 -> StepFitnessGoal(
                        selected = fitnessGoal,
                        onSelect = { fitnessGoal = it },
                        lang = userLanguage
                    )
                    4 -> StepActivityLevel(
                        selected = activityLevel,
                        onSelect = { activityLevel = it },
                        weight = userWeight,
                        height = userHeight,
                        age = userAge,
                        gender = userGender,
                        lang = userLanguage
                    )
                }
            }

            // Bottom Navigation with system bar clearance
            WizardBottomNav(
                currentStep = currentStep,
                totalSteps = totalSteps,
                canProceed = when (currentStep) {
                    1 -> userName.trim().isNotEmpty()
                    else -> true
                },
                lang = userLanguage,
                onBack = { if (currentStep > 0) currentStep-- },
                onNext = {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        // Persist all data
                        prefsManager.saveUserName(userName.trim())
                        prefsManager.saveUserAge(userAge)
                        prefsManager.saveUserGender(userGender)
                        prefsManager.saveUserHeight(userHeight)
                        prefsManager.saveUserWeight(userWeight)
                        prefsManager.saveUserFitnessGoal(fitnessGoal)
                        prefsManager.saveUserActivityLevel(activityLevel)
                        prefsManager.setProfileSetupDone()
                        prefsManager.setGoalsSelected()

                        // Sync to Firebase if logged in
                        try {
                            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            if (firebaseUser != null) {
                                com.pro.aifitnesstracker.FirebaseSyncManager.syncProfileToCloud(
                                    userId = firebaseUser.uid,
                                    age = userAge,
                                    gender = userGender,
                                    height = userHeight,
                                    weight = userWeight,
                                    goal = fitnessGoal,
                                    activityLevel = activityLevel,
                                    name = userName.trim()
                                )
                            }
                        } catch (_: Exception) {}

                        onFinish()
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────
// WIZARD HEADER & PROGRESS INDICATOR
// ─────────────────────────────────────────────
@Composable
private fun WizardHeader(
    currentStep: Int,
    totalSteps: Int,
    lang: String,
    onStepClick: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accent = MaterialTheme.colorScheme.primary

    val stepTitles = listOf(
        getWizString("language", lang),
        getWizString("personal_info", lang),
        getWizString("body_stats", lang),
        getWizString("your_goal", lang),
        getWizString("activity_level", lang)
    )
    val stepIcons = listOf("🌐", "👤", "⚖️", "🎯", "⚡")

    val progressAnim by animateFloatAsState(
        targetValue = (currentStep + 1).toFloat() / totalSteps,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "progress_bar"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Step pill & Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1E2640) else Color(0xFFEEF2FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFC7D2FE))
            ) {
                Text(
                    text = "${getWizString("step", lang).uppercase()} ${currentStep + 1} / $totalSteps",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Text(
                text = stepTitles[currentStep],
                color = textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        // Big Main Header
        Text(
            text = getWizString("setup_profile", lang),
            color = textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )

        Spacer(Modifier.height(14.dp))

        // Linear Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressAnim)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent,
                                Color(0xFF38BDF8)
                            )
                        )
                    )
            )
        }

        Spacer(Modifier.height(14.dp))

        // Step Dots / Nodes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { i ->
                val isDone = i < currentStep
                val isCurr = i == currentStep

                val nodeBg = when {
                    isCurr -> accent
                    isDone -> Color(0xFF10B981) // Green check
                    isDark -> Color(0xFF1E293B)
                    else -> Color(0xFFE2E8F0)
                }

                val nodeContentColor = when {
                    isCurr || isDone -> Color.White
                    else -> textSecondary
                }

                Box(
                    modifier = Modifier
                        .size(if (isCurr) 32.dp else 26.dp)
                        .clip(CircleShape)
                        .background(nodeBg)
                        .clickable(enabled = isDone) { onStepClick(i) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = nodeContentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = stepIcons[i],
                            fontSize = if (isCurr) 14.sp else 11.sp
                        )
                    }
                }

                if (i < totalSteps - 1) {
                    val lineColor = if (i < currentStep) Color(0xFF10B981) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 4.dp)
                            .background(lineColor)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// WIZARD BOTTOM NAVIGATION
// ─────────────────────────────────────────────
@Composable
private fun WizardBottomNav(
    currentStep: Int,
    totalSteps: Int,
    canProceed: Boolean,
    lang: String,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val borderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = cardBg,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = getWizString("back", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(Modifier.width(20.dp))
            }

            Button(
                onClick = onNext,
                enabled = canProceed,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White,
                    disabledContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1),
                    disabledContentColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                val isLast = currentStep == totalSteps - 1
                Text(
                    text = if (isLast) getWizString("lets_go", lang) else getWizString("continue", lang),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (isLast) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// STEP 0: LANGUAGE SELECTION
// ─────────────────────────────────────────────
@Composable
private fun StepLanguage(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    lang: String
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val accent = MaterialTheme.colorScheme.primary

    var searchQuery by remember { mutableStateOf("") }

    val languages = remember {
        listOf(
            Triple("en", "English", "🇺🇸 English"),
            Triple("hi", "हिंदी", "🇮🇳 Hindi"),
            Triple("mr", "मराठी", "🇮🇳 Marathi"),
            Triple("gu", "ગુજરાતી", "🇮🇳 Gujarati"),
            Triple("ta", "தமிழ்", "🇮🇳 Tamil"),
            Triple("te", "తెలుగు", "🇮🇳 Telugu"),
            Triple("kn", "ಕನ್ನಡ", "🇮🇳 Kannada"),
            Triple("bn", "বাংলা", "🇮🇳 Bengali"),
            Triple("pa", "ਪੰਜਾਬੀ", "🇮🇳 Punjabi"),
            Triple("es", "Español", "🇪🇸 Spanish"),
            Triple("fr", "Français", "🇫🇷 French"),
            Triple("de", "Deutsch", "🇩🇪 German"),
            Triple("it", "Italiano", "🇮🇹 Italian"),
            Triple("pt", "Português", "🇵🇹 Portuguese"),
            Triple("zh", "中文", "🇨🇳 Chinese"),
            Triple("ja", "日本語", "🇯🇵 Japanese"),
            Triple("ar", "العربية", "🇸🇦 Arabic"),
            Triple("ru", "Русский", "🇷🇺 Russian")
        )
    }

    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) languages
        else languages.filter {
            it.second.contains(searchQuery, ignoreCase = true) ||
            it.third.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // Title and description
        Text(
            text = getWizString("select_language", lang),
            color = textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Select your comfortable language for the entire app experience",
            color = textSecondary,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 14.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search language / भाषा खोजें...", color = textSecondary, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = textSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = cardBorder,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedContainerColor = if (isDark) Color(0xFF131B2E) else Color.White,
                unfocusedContainerColor = if (isDark) Color(0xFF131B2E) else Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // Language List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filtered.forEach { (code, nativeName, fullName) ->
                val isSelected = selectedLanguage == code
                // PURE SOLID COLORS ONLY (NO SEMI-TRANSPARENCY SHADOW ARTIFACTS)
                val cardBgColor = if (isSelected) {
                    if (isDark) Color(0xFF1E2640) else Color(0xFFF3F6FF)
                } else {
                    if (isDark) Color(0xFF131B2E) else Color.White
                }
                val cardBorderColor = if (isSelected) accent else cardBorder

                Surface(
                    onClick = { onLanguageSelected(code) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = cardBgColor,
                    border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorderColor),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flag & Name
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fullName,
                                color = if (isSelected) (if (isDark) Color.White else Color(0xFF1E1B4B)) else textPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                            )
                            Text(
                                text = nativeName,
                                color = if (isSelected) accent else textSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }

                        // Selection Radio Checkmark
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accent else Color.Transparent)
                                .border(
                                    1.5.dp,
                                    if (isSelected) accent else if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ─────────────────────────────────────────────
// STEP 1: PERSONAL INFO (Name, Age, Gender)
// ─────────────────────────────────────────────
@Composable
private fun StepPersonalInfo(
    name: String, onNameChange: (String) -> Unit,
    age: Int, onAgeChange: (Int) -> Unit,
    gender: String, onGenderChange: (String) -> Unit,
    lang: String
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val accent = MaterialTheme.colorScheme.primary
    val focusManager = LocalFocusManager.current

    val maleLabel = getWizString("male", lang)
    val femaleLabel = getWizString("female", lang)
    val otherLabel = getWizString("other", lang)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        // Avatar hero with glowing border
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1E2640) else Color(0xFFEEF2FF))
                .border(2.5.dp, Brush.linearGradient(listOf(accent, Color(0xFF38BDF8))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val emoji = when (gender) {
                femaleLabel -> "👩"
                otherLabel -> "🧑"
                else -> "👨"
            }
            Text(emoji, fontSize = 46.sp)
        }

        Spacer(Modifier.height(16.dp))

        // Card 1: Your Name
        WizardSectionCard(
            title = getWizString("whats_your_name", lang),
            icon = "✏️",
            subtitle = "Used to personalize your daily fitness coach & reports"
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text(getWizString("enter_name", lang), color = textSecondary) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = accent) },
                trailingIcon = {
                    if (name.isNotEmpty()) {
                        IconButton(onClick = { onNameChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = textSecondary)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    cursorColor = accent,
                    focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                    unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        // Card 2: Gender Selection
        WizardSectionCard(
            title = getWizString("gender", lang),
            icon = "👤",
            subtitle = "Helps calibrate metabolic calculations & target heart rates"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val genders = listOf(
                    Triple(maleLabel, "👨", "Male"),
                    Triple(femaleLabel, "👩", "Female"),
                    Triple(otherLabel, "🧑", "Other")
                )

                genders.forEach { (label, emoji, raw) ->
                    val isSelected = gender == label || gender == raw
                    val cardBg = if (isSelected) {
                        if (isDark) Color(0xFF1E2640) else Color(0xFFF3F6FF)
                    } else {
                        if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                    }
                    val borderCol = if (isSelected) accent else cardBorder

                    Surface(
                        onClick = { onGenderChange(label) },
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderCol),
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(emoji, fontSize = 26.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = label,
                                color = if (isSelected) accent else textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Card 3: Age Selector with precision steppers
        WizardSectionCard(
            title = getWizString("how_old_are_you", lang),
            icon = "🎂",
            subtitle = "Used to determine maximum heart rate & calorie burn"
        ) {
            // Large Age Display + Stepper buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus Button
                FilledIconButton(
                    onClick = { if (age > 12) onAgeChange(age - 1) },
                    enabled = age > 12,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = textPrimary
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Age")
                }

                // Center Age Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$age",
                        color = accent,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = getWizString("years", lang).uppercase(),
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Plus Button
                FilledIconButton(
                    onClick = { if (age < 90) onAgeChange(age + 1) },
                    enabled = age < 90,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = textPrimary
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Age")
                }
            }

            Spacer(Modifier.height(12.dp))

            Slider(
                value = age.toFloat(),
                onValueChange = { onAgeChange(it.roundToInt()) },
                valueRange = 12f..90f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                )
            )

            // Quick Age Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(18, 22, 25, 30, 40, 50).forEach { presetAge ->
                    val isSel = age == presetAge
                    Surface(
                        onClick = { onAgeChange(presetAge) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) accent else if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSel) accent else cardBorder
                        ),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "$presetAge",
                            color = if (isSel) Color.White else textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────
// STEP 2: BODY STATS (Height, Weight, Live BMI)
// ─────────────────────────────────────────────
@Composable
private fun StepBodyStats(
    height: Int, onHeightChange: (Int) -> Unit,
    weight: Int, onWeightChange: (Int) -> Unit,
    gender: String,
    lang: String
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val accent = MaterialTheme.colorScheme.primary

    var isHeightFt by remember { mutableStateOf(false) }
    var isWeightLbs by remember { mutableStateOf(false) }

    val heightM = height / 100.0
    val bmi = weight / (heightM * heightM)

    val (bmiCategory, bmiColor, bmiAdvice) = when {
        bmi < 18.5 -> Triple(
            "Underweight",
            Color(0xFF38BDF8),
            "Focus on nutrient-dense meals & progressive strength building"
        )
        bmi < 25.0 -> Triple(
            "Healthy Weight",
            Color(0xFF10B981),
            "Great job! Maintain your balanced nutrition and regular activity"
        )
        bmi < 30.0 -> Triple(
            "Overweight",
            Color(0xFFF59E0B),
            "Target a moderate calorie deficit and 150+ active mins weekly"
        )
        else -> Triple(
            "Obese",
            Color(0xFFEF4444),
            "Structured low-impact cardio & balanced diet will help you shed weight"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        // 🌟 LIVE BMI SPECTRUM CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, bmiColor),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = getWizString("bmi", lang).uppercase(),
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = bmiCategory,
                            color = bmiColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Large BMI Value Badge (SOLID TINT)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1E2640) else Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, bmiColor)
                    ) {
                        Text(
                            text = "%.1f".format(bmi),
                            color = bmiColor,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // BMI Multi-color Spectrum Bar
                BmiSpectrumGauge(bmi = bmi.toFloat(), activeColor = bmiColor)

                Spacer(Modifier.height(10.dp))

                // Advice pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = bmiAdvice,
                        color = textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // HEIGHT INPUT CARD
        WizardSectionCard(
            title = getWizString("height", lang),
            icon = "📏",
            subtitle = "Used for stride length & BMI calculation",
            trailingContent = {
                // Unit Switcher: CM vs FT
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        .padding(2.dp)
                ) {
                    listOf("CM" to false, "FT" to true).forEach { (unit, isFt) ->
                        val isSel = isHeightFt == isFt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) accent else Color.Transparent)
                                .clickable { isHeightFt = isFt }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unit,
                                color = if (isSel) Color.White else textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) {
            // Steppers & Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { if (height > 120) onHeightChange(height - 1) },
                    enabled = height > 120,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = textPrimary
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Height")
                }

                // Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isHeightFt) {
                        val totalInches = (height / 2.54).roundToInt()
                        val feet = totalInches / 12
                        val inches = totalInches % 12
                        Text(
                            text = "$feet ft $inches in",
                            color = accent,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "($height cm)",
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "$height",
                            color = accent,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "CENTIMETERS",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                FilledIconButton(
                    onClick = { if (height < 220) onHeightChange(height + 1) },
                    enabled = height < 220,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = textPrimary
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Height")
                }
            }

            Spacer(Modifier.height(10.dp))

            Slider(
                value = height.toFloat(),
                onValueChange = { onHeightChange(it.roundToInt()) },
                valueRange = 120f..220f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                )
            )

            // Height Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(155, 165, 170, 175, 180, 185).forEach { presetH ->
                    val isSel = height == presetH
                    Surface(
                        onClick = { onHeightChange(presetH) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) accent else if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) accent else cardBorder),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "$presetH",
                            color = if (isSel) Color.White else textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // WEIGHT INPUT CARD
        WizardSectionCard(
            title = getWizString("weight", lang),
            icon = "⚖️",
            subtitle = "Used to calibrate calorie burn & training intensity",
            trailingContent = {
                // Unit Switcher: KG vs LBS
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        .padding(2.dp)
                ) {
                    listOf("KG" to false, "LBS" to true).forEach { (unit, isLbs) ->
                        val isSel = isWeightLbs == isLbs
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) accent else Color.Transparent)
                                .clickable { isWeightLbs = isLbs }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unit,
                                color = if (isSel) Color.White else textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) {
            // Steppers & Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { if (weight > 30) onWeightChange(weight - 1) },
                    enabled = weight > 30,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = textPrimary
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Weight")
                }

                // Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isWeightLbs) {
                        val lbs = (weight * 2.20462).roundToInt()
                        Text(
                            text = "$lbs",
                            color = accent,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "POUNDS ($weight kg)",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    } else {
                        Text(
                            text = "$weight",
                            color = accent,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "KILOGRAMS",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                FilledIconButton(
                    onClick = { if (weight < 200) onWeightChange(weight + 1) },
                    enabled = weight < 200,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = textPrimary
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Weight")
                }
            }

            Spacer(Modifier.height(10.dp))

            Slider(
                value = weight.toFloat(),
                onValueChange = { onWeightChange(it.roundToInt()) },
                valueRange = 30f..200f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                )
            )

            // Weight Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(50, 60, 70, 75, 80, 90).forEach { presetW ->
                    val isSel = weight == presetW
                    Surface(
                        onClick = { onWeightChange(presetW) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) accent else if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) accent else cardBorder),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "$presetW",
                            color = if (isSel) Color.White else textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────
// BMI SPECTRUM GAUGE COMPOSABLE
// ─────────────────────────────────────────────
@Composable
private fun BmiSpectrumGauge(bmi: Float, activeColor: Color) {
    val minBmi = 15f
    val maxBmi = 35f
    val clampedBmi = bmi.coerceIn(minBmi, maxBmi)
    val fraction = (clampedBmi - minBmi) / (maxBmi - minBmi)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Pointer Arrow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val arrowX = size.width * fraction
                val arrowPath = Path().apply {
                    moveTo(arrowX, size.height)
                    lineTo(arrowX - 10f, 0f)
                    lineTo(arrowX + 10f, 0f)
                    close()
                }
                drawPath(arrowPath, color = activeColor, style = Fill)
            }
        }

        // Multi-segment Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(modifier = Modifier.weight(0.175f).fillMaxHeight().background(Color(0xFF38BDF8)))
            Box(modifier = Modifier.weight(0.325f).fillMaxHeight().background(Color(0xFF10B981)))
            Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(Color(0xFFF59E0B)))
            Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(Color(0xFFEF4444)))
        }

        Spacer(Modifier.height(4.dp))

        // Spectrum Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("< 18.5 Under", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("18.5 - 24.9 Normal", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("25 - 29.9 Over", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("30+ Obese", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────
// STEP 3: FITNESS GOAL
// ─────────────────────────────────────────────
@Composable
private fun StepFitnessGoal(
    selected: String,
    onSelect: (String) -> Unit,
    lang: String
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val goals = listOf(
        Triple("Weight Loss", "🔥", "Burn body fat & shed extra weight with high-calorie deficit cardio & HIIT workouts"),
        Triple("Muscle Gain", "💪", "Build lean muscle mass, tone physique & increase overall strength with resistance training"),
        Triple("Stay Fit", "🏃", "Maintain healthy weight, boost stamina & establish a consistent daily active lifestyle"),
        Triple("Endurance", "⚡", "Elevate cardiovascular endurance, VO2 max & running speed for athletic performance")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        Text(
            text = getWizString("your_goal", lang),
            color = textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Your AI Coach will adapt custom workout plans and calorie targets according to this goal",
            color = textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        goals.forEach { (label, emoji, desc) ->
            GoalOptionCard(
                emoji = emoji,
                label = label,
                desc = desc,
                selected = selected == label,
                onClick = { onSelect(label) }
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────
// STEP 4: ACTIVITY LEVEL
// ─────────────────────────────────────────────
@Composable
private fun StepActivityLevel(
    selected: String,
    onSelect: (String) -> Unit,
    weight: Int,
    height: Int,
    age: Int,
    gender: String,
    lang: String
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accent = MaterialTheme.colorScheme.primary

    // Calculate baseline BMR using Mifflin-St Jeor formula
    val isFemale = gender.contains("female", ignoreCase = true) || gender.contains("महिला", ignoreCase = true)
    val baseBmr = if (isFemale) {
        (10 * weight) + (6.25 * height) - (5 * age) - 161
    } else {
        (10 * weight) + (6.25 * height) - (5 * age) + 5
    }

    val levels = listOf(
        Triple("Sedentary", "🛋️", "Little or no formal exercise, desk job (1.2x BMR)"),
        Triple("Lightly Active", "🚶", "Light exercise or brisk walks 1-3 days / week (1.375x BMR)"),
        Triple("Active", "🏋️", "Moderate gym workouts or sports 3-5 days / week (1.55x BMR)"),
        Triple("Very Active", "🔥", "Hard physical training 6-7 days / week or intense sports (1.725x BMR)")
    )

    val multiplier = when (selected) {
        "Sedentary" -> 1.2
        "Lightly Active" -> 1.375
        "Active" -> 1.55
        "Very Active" -> 1.725
        else -> 1.375
    }
    val estimatedTdee = (baseBmr * multiplier).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        Text(
            text = getWizString("activity_level", lang),
            color = textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Select your typical weekly activity so we can calculate your daily energy expenditure",
            color = textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        // Estimated Daily Calorie Burn Hero Preview (SOLID TINT)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1E2640) else Color(0xFFF3F6FF),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, accent),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡", fontSize = 32.sp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ESTIMATED DAILY EXPENDITURE",
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$estimatedTdee kcal / day",
                        color = textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        levels.forEach { (label, emoji, desc) ->
            GoalOptionCard(
                emoji = emoji,
                label = label,
                desc = desc,
                selected = selected == label,
                onClick = { onSelect(label) }
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────
// REUSABLE COMPONENTS
// ─────────────────────────────────────────────

@Composable
private fun WizardSectionCard(
    title: String,
    icon: String,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(icon, fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (trailingContent != null) {
                    trailingContent()
                }
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            } else {
                Spacer(Modifier.height(12.dp))
            }

            content()
        }
    }
}

@Composable
private fun GoalOptionCard(
    emoji: String,
    label: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val accent = MaterialTheme.colorScheme.primary

    // PURE SOLID COLORS (ABSOLUTELY NO TRANSPARENT SHADOW ARTIFACTS)
    val cardBgColor = if (selected) {
        if (isDark) Color(0xFF1E2640) else Color(0xFFF3F6FF)
    } else {
        if (isDark) Color(0xFF131B2E) else Color.White
    }
    val cardBorderColor = if (selected) accent else cardBorder

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardBgColor,
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, cardBorderColor),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji circle
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            if (isDark) Color(0xFF2E3856) else Color(0xFFE0E7FF)
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = if (selected) (if (isDark) Color.White else Color(0xFF1E1B4B)) else textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = desc,
                    color = textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Check circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) accent else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (selected) accent else if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// LOCALIZATION STRINGS
// ─────────────────────────────────────────────
private fun getWizString(key: String, lang: String): String {
    return when (lang) {
        "hi" -> when (key) {
            "setup_profile" -> "प्रोफ़ाइल सेटअप"
            "personal_info" -> "व्यक्तिगत जानकारी"
            "body_stats" -> "शारीरिक आँकड़े"
            "your_goal" -> "आपका फिटनेस लक्ष्य"
            "activity_level" -> "सक्रियता स्तर"
            "language" -> "भाषा"
            "select_language" -> "अपनी पसंदीदा भाषा चुनें"
            "whats_your_name" -> "आपका शुभ नाम?"
            "enter_name" -> "अपना नाम लिखें..."
            "how_old_are_you" -> "आपकी आयु कितनी है?"
            "years" -> "वर्ष"
            "gender" -> "लिंग"
            "male" -> "पुरुष"
            "female" -> "महिला"
            "other" -> "अन्य"
            "height" -> "ऊंचाई (Height)"
            "weight" -> "वजन (Weight)"
            "bmi" -> "बॉडी मास इंडेक्स (BMI)"
            "lets_go" -> "चलो शुरू करें! 🚀"
            "continue" -> "आगे बढ़ें"
            "back" -> "पीछे"
            "step" -> "कदम"
            else -> key
        }
        "mr" -> when (key) {
            "setup_profile" -> "प्रोफाइल सेटअप"
            "personal_info" -> "वैयक्तिक माहिती"
            "body_stats" -> "शरीर मोजमाप"
            "your_goal" -> "तुमचे ध्येय"
            "activity_level" -> "सक्रियता पातळी"
            "language" -> "भाषा"
            "select_language" -> "तुमची आवडती भाषा निवडा"
            "whats_your_name" -> "तुमचे नाव काय आहे?"
            "enter_name" -> "तुमचे नाव प्रविष्ट करा..."
            "how_old_are_you" -> "तुमचे वय किती आहे?"
            "years" -> "वर्षे"
            "gender" -> "लिंग"
            "male" -> "पुरुष"
            "female" -> "महिला"
            "other" -> "इतर"
            "height" -> "उंची"
            "weight" -> "वजन"
            "bmi" -> "तुमचा बीएमआय"
            "lets_go" -> "चला सुरू करूया! 🚀"
            "continue" -> "पुढे जा"
            "back" -> "मागे"
            "step" -> "पायरी"
            else -> key
        }
        "gu" -> when (key) {
            "setup_profile" -> "પ્રોફાઇલ સેટઅપ"
            "personal_info" -> "વ્યક્તિગત માહિતી"
            "body_stats" -> "શારીરિક આંકડા"
            "your_goal" -> "તમારો ધ્યેય"
            "activity_level" -> "પ્રવૃત્તિ સ્તર"
            "language" -> "ભાષા"
            "select_language" -> "તમારી પસંદગીની ભાષા પસંદ કરો"
            "whats_your_name" -> "તમારું નામ શું છે?"
            "enter_name" -> "નામ લખો..."
            "how_old_are_you" -> "તમારી ઉંમર કેટલી છે?"
            "years" -> "વર્ષ"
            "gender" -> "લિંગ"
            "male" -> "પુરુષ"
            "female" -> "મહિલા"
            "other" -> "અન્ય"
            "height" -> "ઊંચાઈ"
            "weight" -> "વજન"
            "bmi" -> "તમારો BMI"
            "lets_go" -> "ચાલો શરૂ કરીએ! 🚀"
            "continue" -> "આગળ વધો"
            "back" -> "પાછળ"
            "step" -> "પગલું"
            else -> key
        }
        "ta" -> when (key) {
            "setup_profile" -> "சுயவிவர அமைப்பு"
            "personal_info" -> "தனிப்பட்ட தகவல்"
            "body_stats" -> "உடல் அளவுகள்"
            "your_goal" -> "உங்கள் உடற்பயிற்சி இலக்கு"
            "activity_level" -> "செயல்பாட்டு நிலை"
            "language" -> "பயன்பாட்டு மொழி"
            "select_language" -> "விருப்பமான மொழியைத் தேர்ந்தெடுக்கவும்"
            "whats_your_name" -> "உங்கள் பெயர் என்ன?"
            "enter_name" -> "உங்கள் பெயரை உள்ளிடவும்..."
            "how_old_are_you" -> "உங்கள் வயது என்ன?"
            "years" -> "ஆண்டுகள்"
            "gender" -> "பாலினம்"
            "male" -> "ஆண்"
            "female" -> "பெண்"
            "other" -> "மற்றவை"
            "height" -> "உயரம்"
            "weight" -> "எடை"
            "bmi" -> "உடல் நிறை குறியீட்டெண் (BMI)"
            "lets_go" -> "தொடங்குவோம்! 🚀"
            "continue" -> "தொடரவும்"
            "back" -> "பின்செல்"
            "step" -> "படி"
            else -> key
        }
        "te" -> when (key) {
            "setup_profile" -> "ప్రొఫైల్ సెటప్"
            "personal_info" -> "వ్యక్తిగత సమాచారం"
            "body_stats" -> "శరీర కొలతలు"
            "your_goal" -> "మీ ఫిట్‌నెస్ లక్ష్యం"
            "activity_level" -> "కార్యాచరణ స్థాయి"
            "language" -> "యాప్ భాష"
            "select_language" -> "మీ ప్రాధాన్యత భాషను ఎంచుకోండి"
            "whats_your_name" -> "మీ పేరేంటి?"
            "enter_name" -> "మీ పేరును నమోదు చేయండి..."
            "how_old_are_you" -> "మీ వయస్సు ఎంత?"
            "years" -> "సంవత్సరాలు"
            "gender" -> "లింగం"
            "male" -> "పురుషుడు"
            "female" -> "స్త్రీ"
            "other" -> "ఇతర"
            "height" -> "ఎత్తు"
            "weight" -> "బరువు"
            "bmi" -> "బాడీ మాస్ ఇండెక్స్ (BMI)"
            "lets_go" -> "ప్రారంభిద్దాం! 🚀"
            "continue" -> "కొనసాగించండి"
            "back" -> "వెనుకకు"
            "step" -> "దశ"
            else -> key
        }
        "kn" -> when (key) {
            "setup_profile" -> "ಪ್ರೊಫೈಲ್ ಸೆಟಪ್"
            "personal_info" -> "ವೈಯಕ್ತಿಕ ಮಾಹಿತಿ"
            "body_stats" -> "ದೇಹದ ಅಂಕಿಅಂಶಗಳು"
            "your_goal" -> "ನಿಮ್ಮ ಫಿಟ್ನೆಸ್ ಗುರಿ"
            "activity_level" -> "ಚಟುವಟಿಕೆ ಮಟ್ಟ"
            "language" -> "ಅಪ್ಲಿಕೇಶನ್ ಭಾಷೆ"
            "select_language" -> "ಆದ್ಯತೆಯ ಭಾಷೆಯನ್ನು ಆಯ್ಕೆಮಾಡಿ"
            "whats_your_name" -> "ನಿಮ್ಮ ಹೆಸರೇನು?"
            "enter_name" -> "ನಿಮ್ಮ ಹೆಸರನ್ನು ನಮೂದಿಸಿ..."
            "how_old_are_you" -> "ನಿಮ್ಮ ವಯಸ್ಸು ಎಷ್ಟು?"
            "years" -> "ವರ್ಷಗಳು"
            "gender" -> "ಲಿಂಗ"
            "male" -> "ಪುರುಷ"
            "female" -> "ಮಹಿಳೆ"
            "other" -> "ಇತರ"
            "height" -> "ಎತ್ತರ"
            "weight" -> "ತೂಕ"
            "bmi" -> "ಬಾಡಿ ಮಾಸ್ ಇಂಡೆಕ್ಸ್ (BMI)"
            "lets_go" -> "ಪ್ರಾರಂಭಿಸೋಣ! 🚀"
            "continue" -> "ಮುಂದುವರಿಸಿ"
            "back" -> "ಹಿಂದಕ್ಕೆ"
            "step" -> "ಹಂತ"
            else -> key
        }
        "bn" -> when (key) {
            "setup_profile" -> "প্রোফাইল সেটআপ"
            "personal_info" -> "ব্যক্তিগত তথ্য"
            "body_stats" -> "শারীরিক পরিসংখ্যান"
            "your_goal" -> "আপনার ফিটনেস লক্ষ্য"
            "activity_level" -> "কার্যকলাপের স্তর"
            "language" -> "অ্যাপের ভাষা"
            "select_language" -> "পছন্দের ভাষা নির্বাচন করুন"
            "whats_your_name" -> "আপনার নাম কি?"
            "enter_name" -> "আপনার নাম লিখুন..."
            "how_old_are_you" -> "আপনার বয়স কত?"
            "years" -> "বছর"
            "gender" -> "লিঙ্গ"
            "male" -> "পুরুষ"
            "female" -> "মহিলা"
            "other" -> "অন্যান্য"
            "height" -> "উচ্চতা"
            "weight" -> "ওজন"
            "bmi" -> "বডি মাস ইনডেক্স (BMI)"
            "lets_go" -> "শুরু করা যাক! 🚀"
            "continue" -> "এগিয়ে যান"
            "back" -> "ফিরে যান"
            "step" -> "ধাপ"
            else -> key
        }
        "pa" -> when (key) {
            "setup_profile" -> "ਪ੍ਰੋਫਾਈਲ ਸੈੱਟਅੱਪ"
            "personal_info" -> "ਨਿੱਜੀ ਜਾਣਕਾਰੀ"
            "body_stats" -> "ਸਰੀਰ ਦੇ ਅੰਕੜੇ"
            "your_goal" -> "ਤੁਹਾਡਾ ਫਿਟਨੈੱਸ ਟੀਚਾ"
            "activity_level" -> "ਸਰਗਰਮੀ ਪੱਧਰ"
            "language" -> "ਐਪ ਭਾਸ਼ਾ"
            "select_language" -> "ਆਪਣੀ ਪਸੰਦੀਦਾ ਭਾਸ਼ਾ ਚੁਣੋ"
            "whats_your_name" -> "ਤੁਹਾਡਾ ਨਾਂ ਕੀ ਹੈ?"
            "enter_name" -> "ਆਪਣਾ ਨਾਂ ਦਰਜ ਕਰੋ..."
            "how_old_are_you" -> "ਤੁਹਾਡੀ ਉਮਰ ਕਿੰਨੀ ਹੈ?"
            "years" -> "ਸਾਲ"
            "gender" -> "ਲਿੰਗ"
            "male" -> "ਪੁਰਸ਼"
            "female" -> "ਔਰਤ"
            "other" -> "ਹੋਰ"
            "height" -> "ਕੱਦ"
            "weight" -> "ਭਾਰ"
            "bmi" -> "ਬਾਡੀ ਮਾਸ ਇੰਡੈਕਸ (BMI)"
            "lets_go" -> "ਚਲੋ ਸ਼ੁਰੂ ਕਰੀਏ! 🚀"
            "continue" -> "ਜਾਰੀ ਰੱਖੋ"
            "back" -> "ਪਿੱਛੇ"
            "step" -> "ਕਦਮ"
            else -> key
        }
        "es" -> when (key) {
            "setup_profile" -> "Configura tu perfil"
            "personal_info" -> "Información personal"
            "body_stats" -> "Estadísticas corporales"
            "your_goal" -> "Tu objetivo fitness"
            "activity_level" -> "Nivel de actividad"
            "language" -> "Idioma de la app"
            "select_language" -> "Selecciona el idioma preferido"
            "whats_your_name" -> "¿Cómo te llamas?"
            "enter_name" -> "Ingresa tu nombre..."
            "how_old_are_you" -> "¿Cuántos años tienes?"
            "years" -> "Años"
            "gender" -> "Género"
            "male" -> "Hombre"
            "female" -> "Mujer"
            "other" -> "Otro"
            "height" -> "Altura"
            "weight" -> "Peso"
            "bmi" -> "Índice de masa corporal (IMC)"
            "lets_go" -> "¡Empecemos! 🚀"
            "continue" -> "Continuar"
            "back" -> "Atrás"
            "step" -> "Paso"
            else -> key
        }
        "fr" -> when (key) {
            "setup_profile" -> "Configurez votre profil"
            "personal_info" -> "Infos personnelles"
            "body_stats" -> "Mensurations"
            "your_goal" -> "Votre objectif forme"
            "activity_level" -> "Niveau d'activité"
            "language" -> "Langue de l'app"
            "select_language" -> "Choisissez votre langue"
            "whats_your_name" -> "Comment vous appelez-vous ?"
            "enter_name" -> "Entrez votre nom..."
            "how_old_are_you" -> "Quel est votre âge ?"
            "years" -> "Ans"
            "gender" -> "Genre"
            "male" -> "Homme"
            "female" -> "Femme"
            "other" -> "Autre"
            "height" -> "Taille"
            "weight" -> "Poids"
            "bmi" -> "Indice de masse corporelle (IMC)"
            "lets_go" -> "C'est parti ! 🚀"
            "continue" -> "Continuer"
            "back" -> "Retour"
            "step" -> "Étape"
            else -> key
        }
        "de" -> when (key) {
            "setup_profile" -> "Profil einrichten"
            "personal_info" -> "Persönliche Daten"
            "body_stats" -> "Körperdaten"
            "your_goal" -> "Dein Fitnessziel"
            "activity_level" -> "Aktivitätslevel"
            "language" -> "App-Sprache"
            "select_language" -> "Bevorzugte Sprache wählen"
            "whats_your_name" -> "Wie heißt du?"
            "enter_name" -> "Namen eingeben..."
            "how_old_are_you" -> "Wie alt bist du?"
            "years" -> "Jahre"
            "gender" -> "Geschlecht"
            "male" -> "Männlich"
            "female" -> "Weiblich"
            "other" -> "Divers"
            "height" -> "Größe"
            "weight" -> "Gewicht"
            "bmi" -> "Body-Mass-Index (BMI)"
            "lets_go" -> "Los geht's! 🚀"
            "continue" -> "Weiter"
            "back" -> "Zurück"
            "step" -> "Schritt"
            else -> key
        }
        "it" -> when (key) {
            "setup_profile" -> "Configura profilo"
            "personal_info" -> "Dati personali"
            "body_stats" -> "Statistiche corporee"
            "your_goal" -> "Il tuo obiettivo fitness"
            "activity_level" -> "Livello di attività"
            "language" -> "Lingua dell'app"
            "select_language" -> "Seleziona lingua preferita"
            "whats_your_name" -> "Come ti chiami?"
            "enter_name" -> "Inserisci il tuo nome..."
            "how_old_are_you" -> "Quanti anni hai?"
            "years" -> "Anni"
            "gender" -> "Genere"
            "male" -> "Maschio"
            "female" -> "Femmina"
            "other" -> "Altro"
            "height" -> "Altezza"
            "weight" -> "Peso"
            "bmi" -> "Indice di massa corporea (BMI)"
            "lets_go" -> "Iniziamo! 🚀"
            "continue" -> "Continua"
            "back" -> "Indietro"
            "step" -> "Passo"
            else -> key
        }
        "pt" -> when (key) {
            "setup_profile" -> "Configurar Perfil"
            "personal_info" -> "Informações Pessoais"
            "body_stats" -> "Estatísticas Corporais"
            "your_goal" -> "Seu Objetivo de Fitness"
            "activity_level" -> "Nível de Atividade"
            "language" -> "Idioma do Aplicativo"
            "select_language" -> "Selecione o Idioma Preferido"
            "whats_your_name" -> "Qual é o seu nome?"
            "enter_name" -> "Digite seu nome..."
            "how_old_are_you" -> "Quantos anos você tem?"
            "years" -> "Anos"
            "gender" -> "Gênero"
            "male" -> "Masculino"
            "female" -> "Feminino"
            "other" -> "Outro"
            "height" -> "Altura"
            "weight" -> "Peso"
            "bmi" -> "Índice de Massa Corporal (IMC)"
            "lets_go" -> "Vamos lá! 🚀"
            "continue" -> "Continuar"
            "back" -> "Voltar"
            "step" -> "Etapa"
            else -> key
        }
        "zh" -> when (key) {
            "setup_profile" -> "个人资料设置"
            "personal_info" -> "个人信息"
            "body_stats" -> "身体数据"
            "your_goal" -> "您的健身目标"
            "activity_level" -> "活动水平"
            "language" -> "应用语言"
            "select_language" -> "选择首选语言"
            "whats_your_name" -> "您叫什么名字？"
            "enter_name" -> "输入您的姓名..."
            "how_old_are_you" -> "您多大了？"
            "years" -> "岁"
            "gender" -> "性别"
            "male" -> "男"
            "female" -> "女"
            "other" -> "其他"
            "height" -> "身高"
            "weight" -> "体重"
            "bmi" -> "身体质量指数 (BMI)"
            "lets_go" -> "开始使用！🚀"
            "continue" -> "继续"
            "back" -> "返回"
            "step" -> "第"
            else -> key
        }
        "ja" -> when (key) {
            "setup_profile" -> "プロフィール設定"
            "personal_info" -> "個人情報"
            "body_stats" -> "身体データ"
            "your_goal" -> "フィットネス目標"
            "activity_level" -> "活動レベル"
            "language" -> "アプリの言語"
            "select_language" -> "言語を選択してください"
            "whats_your_name" -> "お名前は何ですか？"
            "enter_name" -> "お名前を入力..."
            "how_old_are_you" -> "ご年齢はおいくつですか？"
            "years" -> "歳"
            "gender" -> "性別"
            "male" -> "男性"
            "female" -> "女性"
            "other" -> "その他"
            "height" -> "身長"
            "weight" -> "体重"
            "bmi" -> "体格指数 (BMI)"
            "lets_go" -> "始めましょう！🚀"
            "continue" -> "続ける"
            "back" -> "戻る"
            "step" -> "ステップ"
            else -> key
        }
        "ar" -> when (key) {
            "setup_profile" -> "إعداد الملف الشخصي"
            "personal_info" -> "المعلومات الشخصية"
            "body_stats" -> "قياسات الجسم"
            "your_goal" -> "هدفك اللياقي"
            "activity_level" -> "مستوى النشاط"
            "language" -> "لغة التطبيق"
            "select_language" -> "اختر لغتك المفضلة"
            "whats_your_name" -> "ما اسمك؟"
            "enter_name" -> "أدخل اسمك..."
            "how_old_are_you" -> "كم عمرك؟"
            "years" -> "سنة"
            "gender" -> "الجنس"
            "male" -> "ذكر"
            "female" -> "أنثى"
            "other" -> "آخر"
            "height" -> "الطول"
            "weight" -> "الوزن"
            "bmi" -> "مؤشر كتلة الجسم (BMI)"
            "lets_go" -> "هيا بنا! 🚀"
            "continue" -> "متابعة"
            "back" -> "رجوع"
            "step" -> "خطوة"
            else -> key
        }
        "ru" -> when (key) {
            "setup_profile" -> "Настройка профиля"
            "personal_info" -> "Личные данные"
            "body_stats" -> "Параметры тела"
            "your_goal" -> "Ваша цель"
            "activity_level" -> "Уровень активности"
            "language" -> "Язык приложения"
            "select_language" -> "Выберите язык"
            "whats_your_name" -> "Как вас зовут?"
            "enter_name" -> "Введите ваше имя..."
            "how_old_are_you" -> "Сколько вам лет?"
            "years" -> "лет"
            "gender" -> "Пол"
            "male" -> "Мужской"
            "female" -> "Женский"
            "other" -> "Другой"
            "height" -> "Рост"
            "weight" -> "Вес"
            "bmi" -> "Индекс массы тела (ИМТ)"
            "lets_go" -> "Поехали! 🚀"
            "continue" -> "Продолжить"
            "back" -> "Назад"
            "step" -> "Шаг"
            else -> key
        }
        else -> when (key) {
            "setup_profile" -> "Setup Your Profile"
            "personal_info" -> "Personal Info"
            "body_stats" -> "Body Stats"
            "your_goal" -> "Your Fitness Goal"
            "activity_level" -> "Activity Level"
            "language" -> "App Language"
            "select_language" -> "Select Preferred Language"
            "whats_your_name" -> "What's Your Name?"
            "enter_name" -> "Enter your full name..."
            "how_old_are_you" -> "How Old Are You?"
            "years" -> "Years"
            "gender" -> "Gender"
            "male" -> "Male"
            "female" -> "Female"
            "other" -> "Other"
            "height" -> "Height"
            "weight" -> "Weight"
            "bmi" -> "Live Body Mass Index (BMI)"
            "lets_go" -> "Let's Go! 🚀"
            "continue" -> "Continue"
            "back" -> "Back"
            "step" -> "Step"
            else -> key
        }
    }
}
