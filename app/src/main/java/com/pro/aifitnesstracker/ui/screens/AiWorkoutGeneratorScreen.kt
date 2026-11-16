package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.LocalAppStrings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pro.aifitnesstracker.HealthRepository
import com.pro.aifitnesstracker.utils.AiWorkoutGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AiCoachChatMessage(
    val id: Long,
    val isUser: Boolean,
    val timestampLabel: String = "Just now",
    val headline: String,
    val bulletItems: List<String> = emptyList()
)

@Composable
fun AiCoachScreen(
    onBack: () -> Unit,
    repository: HealthRepository
) {
    AiWorkoutGeneratorScreen(onBack = onBack, repository = repository)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWorkoutGeneratorScreen(
    onBack: () -> Unit,
    repository: HealthRepository
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Exact Dark & Light Mode palette matching high-end fitness AI coaches
    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF101726) else Color.White
    val chipBg = if (isDark) Color(0xFF161E2E) else Color(0xFFF1F5F9)
    val cardBorderColor = if (isDark) Color(0xFF26334D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val accentCyan = Color(0xFF06B6D4)
    val accentViolet = Color(0xFF8B5CF6)
    val emerald = Color(0xFF10B981)

    // Live Biometrics from HealthRepository & SharedPrefsManager
    val context = androidx.compose.ui.platform.LocalContext.current
    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) }
    val weekStartStr = remember { LocalDate.now().minusDays(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) }
    val weekWorkouts by repository.getWorkoutRecordsForDateRange(weekStartStr, todayStr).collectAsState(initial = emptyList())
    val todayWaterMl = remember { com.pro.aifitnesstracker.SharedPrefsManager(context).getWaterGlassesToday() * 250 }

    val liveKcal = remember(weekWorkouts) {
        val total = weekWorkouts.sumOf { it.caloriesBurned }
        if (total > 0) total else 1420
    }
    val formattedKcal = remember(liveKcal) {
        NumberFormat.getNumberInstance(Locale.US).format(liveKcal)
    }
    val liveWaterLiters = remember(todayWaterMl) {
        if (todayWaterMl > 0) String.format(Locale.US, "%.1fL", todayWaterMl / 1000f) else "2.1L"
    }
    val recoveryPct = remember(weekWorkouts) {
        if (weekWorkouts.isEmpty()) 88 else (92 - (weekWorkouts.size * 2)).coerceIn(72, 96)
    }

    val defaultMessages = remember {
        listOf(
            AiCoachChatMessage(
                id = 1L,
                isUser = true,
                headline = "What should I eat after my Push & Chest workout today?"
            ),
            AiCoachChatMessage(
                id = 2L,
                isUser = false,
                timestampLabel = "Just now",
                headline = "Great Push session! Aim for 40g fast-digesting protein + 60g complex carbs within 45 mins:",
                bulletItems = listOf(
                    "🥤 Whey Isolate + 1 Banana (38g P • 32g C • 280 kcal)",
                    "🍗 Grilled Chicken Breast & Jasmine Rice (45g P • 55g C • 440 kcal)",
                    "💧 +500ml Electrolyte Water"
                )
            )
        )
    }

    var messages by remember { mutableStateOf(defaultMessages) }
    var inputText by remember { mutableStateOf("") }
    var isAiTyping by remember { mutableStateOf(false) }
    var showPlanWizardDialog by remember { mutableStateOf(false) }
    var showEngineConfigDialog by remember { mutableStateOf(false) }

    val prefs = remember { com.pro.aifitnesstracker.SharedPrefsManager(context) }
    var selectedEngine by remember { mutableStateOf(prefs.getSelectedAiEngine()) }

    fun sendUserPrompt(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isEmpty()) return
        val userMsg = AiCoachChatMessage(
            id = System.currentTimeMillis(),
            isUser = true,
            headline = trimmed
        )
        messages = messages + userMsg
        inputText = ""
        isAiTyping = true

        coroutineScope.launch {
            val currentWeight = prefs.getWeight().let { if (it <= 0f) 68.4f else it }
            val targetWeight = prefs.getTargetWeight().let { if (it <= 0f) 65.0f else it }
            val stepsToday = prefs.getStepsToday()
            val userGoal = prefs.getUserFitnessGoal()
            val summary = "Weight: ${currentWeight}kg, Target: ${targetWeight}kg, Steps Today: $stepsToday, Goal: $userGoal, Recovery: $recoveryPct%, Hydration: $liveWaterLiters"

            val result = com.pro.aifitnesstracker.network.PuterAiManager.askCoach(
                userPrompt = trimmed,
                userContextSummary = summary,
                prefsManager = prefs
            )

            isAiTyping = false
            val reply = AiCoachChatMessage(
                id = System.currentTimeMillis() + 1,
                isUser = false,
                timestampLabel = "⚡ " + result.engineBadge,
                headline = result.headline,
                bulletItems = result.bulletItems
            )
            messages = messages + reply
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
        }
    }

    if (showPlanWizardDialog) {
        AiPlanGeneratorModal(
            onDismiss = { showPlanWizardDialog = false },
            onGenerate = { goal, equipment, level, area, duration, freq, injuries ->
                showPlanWizardDialog = false
                coroutineScope.launch {
                    val generatedPlan = AiWorkoutGenerator.generate7DayPlan(
                        goal = goal,
                        equipment = equipment,
                        level = level,
                        targetArea = area,
                        duration = duration,
                        frequency = freq,
                        injuries = injuries
                    )
                    generatedPlan.forEach { plan ->
                        repository.addWorkoutPlan(plan)
                    }
                    val confirmMsg = AiCoachChatMessage(
                        id = System.currentTimeMillis(),
                        isUser = false,
                        timestampLabel = "Just now",
                        headline = "✅ Your personalized 7-Day $goal ($area • $duration) plan has been scheduled to your Workout Planner!",
                        bulletItems = generatedPlan.take(3).mapIndexed { idx, p ->
                            "${idx + 1}) 📅 ${p.title} (${p.duration} • ${p.intensity} Intensity)"
                        }
                    )
                    messages = messages + confirmMsg
                    listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
                }
            }
        )
    }

    if (showEngineConfigDialog) {
        PuterEngineConfigModal(
            currentEngine = selectedEngine,
            prefsManager = prefs,
            onDismiss = { showEngineConfigDialog = false },
            onSave = { engine ->
                selectedEngine = engine
                prefs.saveSelectedAiEngine(engine)
                showEngineConfigDialog = false
            },
            onLaunchPlanWizard = {
                showEngineConfigDialog = false
                showPlanWizardDialog = true
            }
        )
    }

    Scaffold(
        containerColor = screenBg,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBg)
            ) {
                // Floating Bottom Chat Input Bar + Glowing Send Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Glassmorphic Input Pill
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.85f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = "Ask your Fitness Coach anything...",
                                        color = textSecondary.copy(alpha = if (isDark) 0.7f else 0.6f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = textPrimary,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(accentCyan),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = { sendUserPrompt(inputText) }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (inputText.isEmpty()) {
                                        inputText = "How do I maximize chest hypertrophy this week?"
                                    } else {
                                        sendUserPrompt(inputText)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MicNone,
                                    contentDescription = "Voice Input",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Glowing Cyan-to-Violet Circular Send Button
                    val sendBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF06B6D4),
                            Color(0xFF3B82F6),
                            Color(0xFF9333EA)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(120f, 120f)
                    )
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = accentCyan,
                                spotColor = accentViolet
                            )
                            .clip(CircleShape)
                            .background(sendBrush)
                            .border(
                                1.2.dp,
                                Brush.linearGradient(listOf(Color(0xFF67E8F9), Color(0xFFD8B4FE))),
                                CircleShape
                            )
                            .clickable {
                                if (inputText.isNotBlank()) {
                                    sendUserPrompt(inputText)
                                } else {
                                    sendUserPrompt("Give me today's recovery & nutrition plan")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AdBanner(adKey = "ad_workout_generator_banner")
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Coach Avatar / Icon Badge & Titles
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AI FITNESS COACH",
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Live Biometrics & Workout Advisor",
                                color = textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }

                    // Action Icons: Configure Coaching Engine & Reset Chat
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showEngineConfigDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Configure Coaching Engine",
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { messages = defaultMessages },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reset Chat",
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Glowing Engine Status Subtitle Pill
                val onlineBorderBrush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF06B6D4), Color(0xFF6366F1), Color(0xFFC084FC))
                )
                val onlineBgBrush = Brush.horizontalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF0C2636), Color(0xFF211A3E))
                    } else {
                        listOf(Color(0xFFE0F2FE), Color(0xFFEDE9FE))
                    }
                )
                val hasPuterToken = remember(prefs) { prefs.getPuterToken().isNotBlank() }
                val engineLabel = when (selectedEngine) {
                    "PUTER_CLAUDE" -> "⚡ Puter Claude 3.5 • Pro Coaching Active"
                    "GEMINI_FLASH" -> "✦ Gemini 2.5 Flash • Coaching Active"
                    else -> if (hasPuterToken) "⚡ Puter GPT-4o • Pro Coaching Active" else "⚡ Pro Coaching • Active"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(onlineBgBrush)
                        .border(1.2.dp, onlineBorderBrush, RoundedCornerShape(50))
                        .clickable { showEngineConfigDialog = true }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = engineLabel,
                            color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        )
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Configure Coaching Engine",
                            tint = if (isDark) Color(0xFF67E8F9) else Color(0xFF0284C7),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(screenBg),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // ── 1. Hero 3D AI Holographic Orb & Live Biometrics Card ──
            item {
                val heroBorderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF06B6D4).copy(alpha = 0.85f),
                        cardBorderColor,
                        Color(0xFFA855F7).copy(alpha = 0.85f)
                    )
                )
                val heroBgBrush = if (isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0E1D2F),
                            Color(0xFF111728),
                            Color(0xFF18142C)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE2EBF5),
                            Color(0xFFECEFF8),
                            Color(0xFFE9E5F7)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(heroBgBrush)
                        .border(1.3.dp, heroBorderBrush, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: 3D Glowing AI Holographic Orb
                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .padding(end = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = com.pro.aifitnesstracker.R.drawable.ai_coach_hero_orb),
                                    contentDescription = "3D Holographic Coach Orb",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(104.dp)
                                )
                            }

                            // Right: 3 Live Biometric Pills
                            Column(
                                modifier = Modifier.weight(1.1f),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                BiometricContextPill(
                                    emoji = "🔥",
                                    boldValue = formattedKcal,
                                    unitLabel = "Kcal Burned",
                                    pillBg = chipBg,
                                    borderColor = cardBorderColor,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                                BiometricContextPill(
                                    emoji = "💪",
                                    boldValue = "$recoveryPct%",
                                    unitLabel = "Recovery",
                                    pillBg = chipBg,
                                    borderColor = cardBorderColor,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                                BiometricContextPill(
                                    emoji = "💧",
                                    boldValue = liveWaterLiters,
                                    unitLabel = "Water Intake",
                                    pillBg = chipBg,
                                    borderColor = cardBorderColor,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Analyzing live workouts, macros & muscle recovery",
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── 2. Quick Prompt Chips (Horizontal Scrolling LazyRow - No word-wrapping bug!) ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 QUICK COACHING PROMPTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = textSecondary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val suggestions = listOf(
                            Triple("⚡", "Post-Workout Meal", "What should I eat after my Push & Chest workout today?"),
                            Triple("💪", "Chest Hypertrophy Tips", "Give me Chest Hypertrophy Tips for my next session"),
                            Triple("😴", "Deep Sleep & Recovery", "How can I improve my Deep Sleep & recovery tonight?"),
                            Triple("✨", "7-Day Workout Plan", "OPEN_PLAN_WIZARD"),
                            Triple("🔥", "Fat Loss Macro Targets", "How many calories and macros should I eat for clean fat loss?"),
                            Triple("💧", "Hydration Schedule", "How much water and electrolytes should I drink around workouts?")
                        )
                        items(suggestions) { (emoji, label, prompt) ->
                            QuickPromptChip(
                                emoji = emoji,
                                label = label,
                                chipBg = chipBg,
                                borderColor = cardBorderColor,
                                textPrimary = textPrimary,
                                onClick = {
                                    if (prompt == "OPEN_PLAN_WIZARD") {
                                        showPlanWizardDialog = true
                                    } else {
                                        sendUserPrompt(prompt)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ── 3. Chat Conversation Feed (User Bubbles & Coach Cards) ──
            items(messages, key = { it.id }) { msg ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
                ) {
                    if (msg.isUser) {
                        UserChatBubble(message = msg)
                    } else {
                        AiCoachResponseCard(
                            message = msg,
                            cardBg = cardBg,
                            cardBorderColor = cardBorderColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }

            if (isAiTyping) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorderColor),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = accentCyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Coach is analyzing your biometrics...",
                                    color = textSecondary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Biometric Context Pill inside Hero Card ─────────────────────────────────

@Composable
private fun BiometricContextPill(
    emoji: String,
    boldValue: String,
    unitLabel: String,
    pillBg: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = pillBg.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = boldValue,
                color = textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unitLabel,
                color = textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Quick Prompt Chip ────────────────────────────────────────────────────────

@Composable
private fun QuickPromptChip(
    emoji: String,
    label: String,
    chipBg: Color,
    borderColor: Color,
    textPrimary: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = chipBg,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.85f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = textPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// ─── Right-Aligned User Message Bubble ────────────────────────────────────────

@Composable
private fun UserChatBubble(message: AiCoachChatMessage) {
    val userBubbleGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF4338CA),
            Color(0xFF5B21B6),
            Color(0xFF6D28D9)
        )
    )
    val userBubbleBorder = Brush.horizontalGradient(
        colors = listOf(Color(0xFF818CF8), Color(0xFFC084FC))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                    ambientColor = Color(0xFF6366F1),
                    spotColor = Color(0xFF8B5CF6)
                )
                .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .background(userBubbleGradient)
                .border(1.2.dp, userBubbleBorder, RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.headline,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── Left-Aligned AI Coach Response Card ──────────────────────────────────────

@Composable
private fun AiCoachResponseCard(
    message: AiCoachChatMessage,
    cardBg: Color,
    cardBorderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val aiCardBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF06B6D4).copy(alpha = 0.85f),
            cardBorderColor,
            Color(0xFFA855F7).copy(alpha = 0.75f)
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            color = cardBg,
            border = BorderStroke(1.2.dp, aiCardBorderBrush),
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Header: Cyan AI icon + "Fitness Coach • Just now"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF06B6D4).copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fitness Coach • ${message.timestampLabel}",
                            color = textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main coaching intro text
                Text(
                    text = message.headline,
                    color = textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp
                )

                if (message.bulletItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        message.bulletItems.forEach { itemText ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF161F32) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF26334D) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = itemText,
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 19.sp
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

// ─── Interactive 7-Day AI Plan Generator Modal (Preserves Full Plan Builder) ──

@Composable
private fun AiPlanGeneratorModal(
    onDismiss: () -> Unit,
    onGenerate: (
        goal: String,
        equipment: String,
        level: String,
        area: String,
        duration: String,
        freq: String,
        injuries: String
    ) -> Unit
) {
    var goal by remember { mutableStateOf("Muscle Gain") }
    var equipment by remember { mutableStateOf("Full Gym") }
    var level by remember { mutableStateOf("Intermediate") }
    var area by remember { mutableStateOf("Upper Body") }
    var duration by remember { mutableStateOf("45 min") }
    var freq by remember { mutableStateOf("5 days") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✨ Generate 7-Day Workout Plan",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Select your preferences and your Coach will build & schedule your 7-day routine:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                PlanOptionRow("Goal", listOf("Muscle Gain", "Weight Loss", "Keep Fit"), goal) { goal = it }
                PlanOptionRow("Equipment", listOf("Full Gym", "Dumbbells", "No Equipment"), equipment) { equipment = it }
                PlanOptionRow("Target", listOf("Full Body", "Upper Body", "Lower Body"), area) { area = it }
                PlanOptionRow("Duration", listOf("30 min", "45 min", "60 min"), duration) { duration = it }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onGenerate(goal, equipment, level, area, duration, freq, "None")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("GENERATE & SCHEDULE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanOptionRow(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { opt ->
                val isSel = opt == selected
                Surface(
                    onClick = { onSelect(opt) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) Color(0xFF6366F1).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isSel) Color(0xFF06B6D4) else Color.Transparent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = opt,
                        color = if (isSel) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PuterEngineConfigModal(
    currentEngine: String,
    prefsManager: com.pro.aifitnesstracker.SharedPrefsManager,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onLaunchPlanWizard: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF101726) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val borderColor = if (isDark) Color(0xFF26334D) else Color(0xFFCBD5E1)
    val accentCyan = Color(0xFF06B6D4)
    val accentEmerald = Color(0xFF10B981)

    var tempEngine by remember { mutableStateOf(currentEngine) }
    var puterTokenText by remember { mutableStateOf(prefsManager.getPuterToken()) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(26.dp))
                .background(cardBg)
                .border(1.5.dp, accentCyan.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚡ Coaching Engine & Puter Cloud",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                        Text(
                            text = "Dual-Engine Cloud Gateway",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentCyan
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Select your coaching engine for conversational coaching, workout plans, and macro tracking:",
                    fontSize = 12.sp,
                    color = textSecondary
                )

                // Engine Options
                val engines = listOf(
                    Triple("PUTER_GPT4O", "⚡ Puter GPT-4o (OpenAI)", "World's most capable engine for sports nutrition & progressive overload"),
                    Triple("PUTER_CLAUDE", "🧠 Puter Claude 3.5 (Anthropic)", "Deep reasoning & empathetic conversational guidance"),
                    Triple("GEMINI_FLASH", "✦ Google Gemini 2.5 Flash", "Instant zero-key default cloud model")
                )

                engines.forEach { (key, title, subtitle) ->
                    val isSelected = tempEngine == key
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) accentCyan.copy(alpha = 0.12f) else (if (isDark) Color(0xFF161F32) else Color(0xFFF1F5F9)))
                            .border(1.2.dp, if (isSelected) accentCyan else borderColor, RoundedCornerShape(14.dp))
                            .clickable { tempEngine = key }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { tempEngine = key },
                                colors = RadioButtonDefaults.colors(selectedColor = accentCyan)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text(subtitle, fontSize = 10.5.sp, color = textSecondary)
                            }
                        }
                    }
                }

                // Optional Puter Token Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Puter Cloud Token (Optional for Uncapped Pro):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    OutlinedTextField(
                        value = puterTokenText,
                        onValueChange = { puterTokenText = it },
                        placeholder = { Text("Paste token or leave empty for free tier", fontSize = 11.sp, color = textSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentCyan,
                            unfocusedBorderColor = borderColor
                        )
                    )
                }

                // Action Buttons
                Button(
                    onClick = {
                        prefsManager.savePuterToken(puterTokenText.trim())
                        onSave(tempEngine)
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentCyan)
                ) {
                    Text("Apply & Save Engine", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                }

                OutlinedButton(
                    onClick = onLaunchPlanWizard,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accentEmerald)
                ) {
                    Text("🛠️ Open 7-Day Plan Wizard", color = accentEmerald, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }
    }
}

