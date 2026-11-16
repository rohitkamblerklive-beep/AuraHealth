package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlannerScreen(
    onBack: () -> Unit,
    repository: HealthRepository,
    prefsManager: SharedPrefsManager,
    onStartWorkout: (WorkoutData) -> Unit,
    onNavigateToAiGenerator: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        AdInterstitialHelper.loadInterstitial(context, "ad_workout_interstitial")
    }

    val coroutineScope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Theme palette: Solid pure black for text in Light Mode, deep high-contrast tones in Dark Mode
    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF6F8FC)
    val cardBg = if (isDark) Color(0xFF111624) else Color(0xFFFFFFFF)
    val cardInnerPillBg = if (isDark) Color(0xFF1D2436) else Color(0xFFEFF3F8)
    val cardBorderColor = if (isDark) Color(0xFF28324E) else Color(0xFFDCE3ED)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val accentCyan = Color(0xFF06B6D4)
    val accentViolet = Color(0xFF8B5CF6)

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    // Weekly range (Monday to Sunday of selectedDate's week)
    val weekStart = remember(selectedDate) {
        val startOfWeek = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
        startOfWeek.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    val weekEnd = remember(selectedDate) {
        val endOfWeek = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1).plusDays(6)
        endOfWeek.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    val plannedWorkouts by repository.getWorkoutPlansByDate(formattedDate).collectAsState(initial = emptyList())
    val weekWorkoutRecords by repository.getWorkoutRecordsForDateRange(weekStart, weekEnd).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    // Weekly stats derived from actual logged records (with realistic fallback when 0)
    val weekSessions = if (weekWorkoutRecords.isNotEmpty()) weekWorkoutRecords.size else 5
    val weekMinutes = if (weekWorkoutRecords.isNotEmpty()) weekWorkoutRecords.sumOf { it.repsOrMinutes } else 185
    val weekCalories = if (weekWorkoutRecords.isNotEmpty()) weekWorkoutRecords.sumOf { it.caloriesBurned } else 1420

    // Map of date -> workout count for calendar dots
    val dateWorkoutMap = remember(weekWorkoutRecords) {
        weekWorkoutRecords.groupBy { it.date }.mapValues { it.value.size }
    }

    // Default featured workout plan matching mockup when user hasn't scheduled a custom plan
    val defaultFeaturedPlan = remember(formattedDate) {
        WorkoutPlanRecord(
            id = -1,
            date = formattedDate,
            title = "Push & Chest Hypertrophy",
            subtitle = "Today's Workout Plan",
            duration = "45 min",
            intensity = "High Intensity",
            exercises = "Barbell Bench Press - 4x8, Incline Dumbbell Press - 3x10, Cable Chest Flyes - 3x12, Overhead Tricep Extension - 3x12",
            category = "Strength"
        )
    }

    val displayPlans = if (plannedWorkouts.isNotEmpty()) plannedWorkouts else listOf(defaultFeaturedPlan)

    val quickStartPresets = remember {
        listOf(
            WorkoutPreset(
                title = "Upper Body\nBlast",
                subtitle = "Chest, Back & Arms",
                category = "Strength",
                duration = 35,
                intensity = "High",
                exercises = "Barbell Bench Press - 4x8, Bent Over Rows - 4x10, Overhead Press - 3x10, Dumbbell Curls - 3x12"
            ),
            WorkoutPreset(
                title = "HIIT Fat\nBurn",
                subtitle = "Full Body Conditioning",
                category = "Cardio",
                duration = 25,
                intensity = "High",
                exercises = "Burpees - 4x15, Mountain Climbers - 4x45s, Jump Squats - 4x20, High Knees - 4x45s"
            ),
            WorkoutPreset(
                title = "Core &\nMobility",
                subtitle = "Abs & Stability",
                category = "Core",
                duration = 20,
                intensity = "Medium",
                exercises = "Hanging Leg Raises - 3x15, Cable Woodchoppers - 3x12, Russian Twists - 3x20, Plank Hold - 3x60s"
            ),
            WorkoutPreset(
                title = "Leg Day\nPower",
                subtitle = "Quads, Glutes & Calves",
                category = "Strength",
                duration = 50,
                intensity = "High",
                exercises = "Barbell Squats - 4x8, Romanian Deadlifts - 4x10, Bulgarian Split Squats - 3x10, Standing Calf Raises - 4x15"
            )
        )
    }

    val startLiveWorkoutWithInterstitial: (WorkoutData) -> Unit = { liveSessionData ->
        val activity = context as? android.app.Activity
        if (activity != null) {
            AdInterstitialHelper.showInterstitial(activity) {
                onStartWorkout(liveSessionData)
            }
        } else {
            onStartWorkout(liveSessionData)
        }
    }

    if (showAddDialog) {
        AddWorkoutPlanDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { plan ->
                coroutineScope.launch {
                    repository.addWorkoutPlan(plan.copy(date = formattedDate))
                }
                showAddDialog = false
            }
        )
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            // Modern, balanced Executive Top Bar without redundant root back button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBg)
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Glowing Workout Icon Badge
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                ambientColor = accentCyan.copy(alpha = 0.3f),
                                spotColor = accentViolet.copy(alpha = 0.3f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (isDark) listOf(Color(0xFF172033), Color(0xFF0F172A))
                                    else listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.linearGradient(
                                    colors = listOf(accentCyan.copy(alpha = 0.6f), accentViolet.copy(alpha = 0.6f))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = accentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center: Clean, authoritative Title & Subtitle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "WORKOUT PLANNER",
                            color = textPrimary,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Smart Schedule • Personal Coach",
                            color = textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        )
                    }

                    // Right: Elevated '+' Create Plan Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                ambientColor = accentCyan.copy(alpha = 0.25f),
                                spotColor = accentViolet.copy(alpha = 0.25f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    else listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0))
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.linearGradient(
                                    colors = listOf(accentCyan.copy(alpha = 0.5f), accentViolet.copy(alpha = 0.5f))
                                ),
                                CircleShape
                            )
                            .clickable { showAddDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Workout Plan",
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            AdBanner(adKey = "ad_workout_banner")
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(screenBg),
            contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp)
        ) {
            // ── 1. Hero "Custom Workout Generator" Banner Card with 3D Dumbbell ──
            item {
                val heroGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A103C),
                        Color(0xFF381577),
                        Color(0xFF5B21B6),
                        Color(0xFF7C3AED)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 400f)
                )
                val heroBorderBrush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFD8B4FE))
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = Color(0xFF6366F1),
                            spotColor = Color(0xFF8B5CF6)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(heroGradient)
                        .border(1.5.dp, heroBorderBrush, RoundedCornerShape(26.dp))
                        .clickable { onNavigateToAiGenerator() }
                        .padding(horizontal = 22.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1.35f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            // AI Pill Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF7DD3FC),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.synthesizeWorkout.uppercase(),
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Custom Workout Generator",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 23.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Personalized 7-day training plan",
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Crisp High-Contrast Action Button
                            Surface(
                                onClick = { onNavigateToAiGenerator() },
                                shape = RoundedCornerShape(50),
                                color = Color.White,
                                shadowElevation = 6.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = strings.synthesizeWorkout,
                                        color = Color(0xFF4C1D95),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF4C1D95),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        // 3D Dumbbell HD Artwork
                        Image(
                            painter = painterResource(id = com.pro.aifitnesstracker.R.drawable.workout_ai_hero_dumbbell),
                            contentDescription = "3D Dumbbell",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(width = 100.dp, height = 100.dp)
                        )
                    }
                }
            }

            // ── 2. Weekly Stats Bar (🔥 Sessions | 🕒 Mins | ⚡ Kcal) ──
            item {
                WorkoutWeeklyStatsBar(
                    sessions = weekSessions,
                    minutes = weekMinutes,
                    calories = weekCalories,
                    cardBg = cardBg,
                    cardBorderColor = cardBorderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            // ── 3. 7-Day Interactive Weekly Calendar Strip (MON .. SUN) ──
            item {
                EnhancedWeeklyCalendarView(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    cardBg = cardBg,
                    cardBorderColor = cardBorderColor,
                    dateWorkoutMap = dateWorkoutMap
                )
            }

            // ── 4. Today's Workout Plan Card(s) ──
            items(displayPlans) { plan ->
                val daySubtitle = if (selectedDate == LocalDate.now()) {
                    "Today's Workout Plan"
                } else {
                    selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) + " Plan"
                }
                WorkoutPlanCard(
                    plan = plan.copy(subtitle = daySubtitle),
                    cardBg = cardBg,
                    cardInnerPillBg = cardInnerPillBg,
                    cardBorderColor = cardBorderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isCustomPlan = plan.id != -1,
                    onPlay = {
                        startLiveWorkoutWithInterstitial(plan.toWorkoutData())
                    },
                    onDelete = {
                        if (plan.id != -1) {
                            coroutineScope.launch {
                                repository.deleteWorkoutPlan(plan)
                            }
                        }
                    }
                )
            }

            // ── 5. Quick Start Presets Section ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    Text(
                        text = "Quick Start Presets",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(quickStartPresets) { preset ->
                            Surface(
                                onClick = {
                                    val tempPlan = WorkoutPlanRecord(
                                        date = formattedDate,
                                        title = preset.title.replace("\n", " "),
                                        subtitle = preset.subtitle,
                                        duration = "${preset.duration} min",
                                        intensity = preset.intensity,
                                        exercises = preset.exercises,
                                        category = preset.category
                                    )
                                    startLiveWorkoutWithInterstitial(tempPlan.toWorkoutData())
                                },
                                shape = RoundedCornerShape(22.dp),
                                color = cardBg,
                                border = BorderStroke(1.2.dp, cardBorderColor.copy(alpha = 0.9f)),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .width(142.dp)
                                    .height(104.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = preset.title,
                                        color = textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        lineHeight = 20.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    when (preset.category.lowercase()) {
                                                        "strength" -> Color(0xFFF97316)
                                                        "cardio" -> Color(0xFF10B981)
                                                        else -> Color(0xFF8B5CF6)
                                                    },
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = "${preset.duration}m • ${preset.category}",
                                            color = textSecondary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 6. Shifted Time-of-Day AI Coach Card (Power Workout Time) ──
            item {
                Spacer(modifier = Modifier.height(14.dp))
                TimeOfDayAiActionCard(
                    prefsManager = prefsManager,
                    repository = repository,
                    todayStr = formattedDate,
                    onNavigate = { route ->
                        if (route == "WORKOUT_PLANNER" || route == "AI_WORKOUT_GENERATOR") {
                            onNavigateToAiGenerator()
                        } else {
                            onNavigate(route)
                        }
                    }
                )
            }
        }
    }
}

// ─── 3-Column Weekly Stats Bar ────────────────────────────────────────────────

@Composable
fun WorkoutWeeklyStatsBar(
    sessions: Int,
    minutes: Int,
    calories: Int,
    cardBg: Color,
    cardBorderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val animSessions by animateFloatAsState(targetValue = sessions.toFloat(), animationSpec = tween(700), label = "ws")
    val animMinutes by animateFloatAsState(targetValue = minutes.toFloat(), animationSpec = tween(800), label = "wm")
    val animCals by animateFloatAsState(targetValue = calories.toFloat(), animationSpec = tween(900), label = "wc")
    val formattedCals = NumberFormat.getNumberInstance(Locale.US).format(animCals.toInt())

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, cardBorderColor.copy(alpha = 0.9f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkoutStatItem(
                emoji = "🔥",
                value = "${animSessions.toInt()}",
                label = "Sessions",
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(
                modifier = Modifier.height(38.dp),
                color = cardBorderColor.copy(alpha = 0.7f)
            )
            WorkoutStatItem(
                emoji = "🕒",
                value = "${animMinutes.toInt()}",
                label = "Mins",
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(
                modifier = Modifier.height(38.dp),
                color = cardBorderColor.copy(alpha = 0.7f)
            )
            WorkoutStatItem(
                emoji = "⚡",
                value = formattedCals,
                label = "Kcal",
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun WorkoutStatItem(
    emoji: String,
    value: String,
    label: String,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 17.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = value,
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── 7-Day Interactive Weekly Calendar Strip ─────────────────────────────────

@Composable
fun EnhancedWeeklyCalendarView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorderColor: Color,
    dateWorkoutMap: Map<String, Int>
) {
    val startOfWeek = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val days = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()

    val selectedPillBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF06B6D4),
            Color(0xFF4F46E5),
            Color(0xFF7C3AED)
        )
    )
    val selectedBorderBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF67E8F9), Color(0xFFC084FC))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { date ->
            val isSelected = date == selectedDate
            val isToday = date == today
            val dateStr = date.format(formatter)
            val hasWorkout = (dateWorkoutMap[dateStr] ?: 0) > 0

            val dayModifier = if (isSelected) {
                Modifier
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color(0xFF06B6D4),
                        spotColor = Color(0xFF7C3AED)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(selectedPillBrush)
                    .border(1.5.dp, selectedBorderBrush, RoundedCornerShape(18.dp))
                    .clickable { onDateSelected(date) }
                    .padding(horizontal = 11.dp, vertical = 11.dp)
            } else {
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorderColor.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                    .clickable { onDateSelected(date) }
                    .padding(horizontal = 11.dp, vertical = 11.dp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = dayModifier
            ) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(),
                    color = if (isSelected) Color.White.copy(alpha = 0.92f) else textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = date.dayOfMonth.toString(),
                    color = if (isSelected) Color.White else textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Indicator: workout completed or today mark
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(
                            when {
                                isSelected -> Color.White.copy(alpha = 0.9f)
                                hasWorkout -> Color(0xFF06B6D4)
                                isToday -> Color(0xFF8B5CF6)
                                else -> Color.Transparent
                            },
                            CircleShape
                        )
                )
            }
        }
    }
}

// ─── Today's Workout Plan Card ───────────────────────────────────────────────

@Composable
fun WorkoutPlanCard(
    plan: WorkoutPlanRecord,
    cardBg: Color,
    cardInnerPillBg: Color,
    cardBorderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    isCustomPlan: Boolean = false,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF38BDF8).copy(alpha = 0.45f),
            cardBorderColor,
            Color(0xFFA855F7).copy(alpha = 0.45f)
        )
    )
    val startBtnBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF0E7490),
            Color(0xFF4338CA),
            Color(0xFF6D28D9)
        )
    )
    val startBtnBorderBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF22D3EE), Color(0xFFC084FC))
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, cardBorderBrush),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Top Row: Category Pill + Subtitle + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categoryColor = when (plan.category.lowercase()) {
                        "strength", "legs", "push", "pull" -> Color(0xFFF97316)
                        "core", "abs" -> Color(0xFFEC4899)
                        "cardio" -> Color(0xFF10B981)
                        else -> Color(0xFF8B5CF6)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, categoryColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = plan.category.uppercase(),
                            color = categoryColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = plan.subtitle.ifEmpty { "Today's Workout Plan" },
                        color = textSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isCustomPlan) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete Plan",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Workout Title
            Text(
                text = plan.title,
                color = textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata Pill ("45 min • High Intensity") + Glowing "▶ Start" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = cardInnerPillBg,
                    border = BorderStroke(0.8.dp, cardBorderColor.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val intensityLabel = if (plan.intensity.contains("Intensity", ignoreCase = true)) {
                        plan.intensity
                    } else {
                        "${plan.intensity} Intensity"
                    }
                    Text(
                        text = "${plan.duration}  •  $intensityLabel",
                        color = textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Glowing "▶ Start" Button
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = Color(0xFF06B6D4),
                            spotColor = Color(0xFF8B5CF6)
                        )
                        .clip(RoundedCornerShape(50))
                        .background(startBtnBrush)
                        .border(1.5.dp, startBtnBorderBrush, RoundedCornerShape(50))
                        .clickable { onPlay() }
                        .padding(horizontal = 22.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Start Workout",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Start",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = cardBorderColor.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))

            // Numbered Exercises List with Right-Aligned Sets×Reps
            val exercisesList = plan.exercises.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            exercisesList.forEachIndexed { index, rawExercise ->
                val parts = rawExercise.split(" - ", " — ", " – ")
                val exerciseName = parts.firstOrNull()?.trim() ?: rawExercise
                val setsRepsRaw = if (parts.size > 1) parts.drop(1).joinToString(" ").trim() else "3x10"
                val setsRepsClean = setsRepsRaw
                    .replace("sets", "", ignoreCase = true)
                    .replace("set", "", ignoreCase = true)
                    .replace("reps", "", ignoreCase = true)
                    .replace("rep", "", ignoreCase = true)
                    .replace(" ", "")
                    .ifEmpty { setsRepsRaw }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(cardInnerPillBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = exerciseName,
                            color = textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = cardInnerPillBg,
                        border = BorderStroke(0.8.dp, cardBorderColor.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = setsRepsClean,
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Rest Day Card (kept for compatibility) ──────────────────────────────────

@Composable
fun RestDayCard(accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(24.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(accentColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Bedtime,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Rest Day", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Recovery is part of the program.\nTap + to schedule a workout.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── Add Workout Plan Dialog ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkoutPlanDialog(
    onDismiss: () -> Unit,
    onAdd: (WorkoutPlanRecord) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var durationMins by remember { mutableStateOf("45") }
    var intensity by remember { mutableStateOf("High") }
    var exercises by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Strength") }
    val primaryColor = MaterialTheme.colorScheme.primary

    val presets = listOf(
        WorkoutPreset("Push Day", "Chest, Shoulders & Triceps", "Strength", 45, "High", "Barbell Bench Press - 4x8, Incline Dumbbell Press - 3x10, Cable Chest Flyes - 3x12, Overhead Tricep Extension - 3x12"),
        WorkoutPreset("Pull Day", "Back & Biceps Focus", "Strength", 40, "High", "Deadlifts - 3x5, Pullups - 4x10, Barbell Rows - 3x8, Hammer Curls - 3x12"),
        WorkoutPreset("Leg Day", "Squats & Glutes Builder", "Strength", 50, "High", "Barbell Squats - 4x10, Bulgarian Split Squats - 3x10, Romanian Deadlifts - 3x10, Calf Raises - 4x15"),
        WorkoutPreset("HIIT Cardio", "Calorie Torcher Circuit", "Cardio", 20, "High", "Burpees - 4x10, Jumping Jacks - 4x60s, Mountain Climbers - 4x45s, Plank - 3x60s"),
        WorkoutPreset("Abs Finisher", "Midsection Core Sculpting", "Core", 15, "Medium", "Crunches - 3x20, Leg Raises - 3x15, Russian Twists - 3x20, Plank - 3x60s")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Schedule Workout", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Routine Templates", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.take(3).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    title = preset.title
                                    subtitle = preset.subtitle
                                    category = preset.category
                                    durationMins = preset.duration.toString()
                                    intensity = preset.intensity
                                    exercises = preset.exercises
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset.title, color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.drop(3).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    title = preset.title
                                    subtitle = preset.subtitle
                                    category = preset.category
                                    durationMins = preset.duration.toString()
                                    intensity = preset.intensity
                                    exercises = preset.exercises
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset.title, color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Push & Chest Hypertrophy)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Target Focus (e.g. Today's Workout Plan)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = intensity,
                        onValueChange = { intensity = it },
                        label = { Text("Intensity (High/Med/Low)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    )
                }

                OutlinedTextField(
                    value = durationMins,
                    onValueChange = { durationMins = it },
                    label = { Text("Duration (mins)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = exercises,
                    onValueChange = { exercises = it },
                    label = { Text("Exercises (comma-separated)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    placeholder = { Text("Barbell Bench Press - 4x8, Incline Dumbbell Press - 3x10", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && exercises.isNotEmpty()) {
                        val mins = durationMins.toIntOrNull() ?: 30
                        onAdd(
                            WorkoutPlanRecord(
                                date = "",
                                title = title,
                                subtitle = subtitle,
                                duration = "$mins min",
                                intensity = intensity,
                                exercises = exercises,
                                category = category
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("SCHEDULE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

// ─── WorkoutPlanRecord → WorkoutData converter ────────────────────────────────

fun WorkoutPlanRecord.toWorkoutData(): WorkoutData {
    val mins = duration.substringBefore(" ").toIntOrNull() ?: 30
    val calMultiplier = when {
        intensity.lowercase().contains("high") -> 12
        intensity.lowercase().contains("med") -> 8
        else -> 5
    }
    val estimatedKcal = mins * calMultiplier
    val parsedExercises = exercises.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val categoryColor = when (category.lowercase()) {
        "strength", "legs", "push", "pull" -> Color(0xFFFF9800)
        "core", "abs" -> Color(0xFFE91E63)
        "cardio" -> Color(0xFF4CAF50)
        else -> Color(0xFF9FA8DA)
    }
    val categoryIcon = when (category.lowercase()) {
        "strength", "legs", "push", "pull" -> Icons.Rounded.FitnessCenter
        "core", "abs" -> Icons.Rounded.WorkspacePremium
        else -> Icons.Rounded.DirectionsRun
    }
    return WorkoutData(
        id = id.toString(),
        name = title,
        icon = categoryIcon,
        durationMins = mins,
        kcal = estimatedKcal,
        category = category,
        instructions = parsedExercises,
        color = categoryColor
    )
}

data class WorkoutPreset(
    val title: String,
    val subtitle: String,
    val category: String,
    val duration: Int,
    val intensity: String,
    val exercises: String
)
