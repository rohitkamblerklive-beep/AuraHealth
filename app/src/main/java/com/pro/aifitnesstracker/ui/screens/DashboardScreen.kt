package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.draw.blur
import com.pro.aifitnesstracker.utils.MotivationalQuotes
import com.pro.aifitnesstracker.utils.findActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk

// ── Color tokens ────────────────────────────────────────────────────────────
private val BG @Composable get() = MaterialTheme.colorScheme.background
private val CARD @Composable get() = MaterialTheme.colorScheme.surface
private val CARD2 @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val ACCENT @Composable get() = MaterialTheme.colorScheme.primary
private val ACCENT2 @Composable get() = MaterialTheme.colorScheme.primary
// ── Floating Elevation Card Modifier (3D Depth, Specular Top Highlight & Ambient/Spot Shadows) ──
@Composable
fun Modifier.floatingElevationCard(
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(26.dp)
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBgGradient = Brush.verticalGradient(
        colors = if (isDark) listOf(Color(0xFF151C2F), Color(0xFF0C1222))
        else listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF))
    )
    val borderGradient = Brush.verticalGradient(
        colors = if (isDark) listOf(
            accentColor.copy(alpha = 0.55f),
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        ) else listOf(
            Color(0xFFE2E8F0),
            Color(0xFFCBD5E1).copy(alpha = 0.85f)
        )
    )
    return this
        .shadow(
            elevation = if (isDark) 10.dp else 10.dp,
            shape = shape,
            ambientColor = if (isDark) Color.Black.copy(alpha = 0.85f) else Color(0x2B0F172A),
            spotColor = if (isDark) accentColor.copy(alpha = 0.38f) else Color(0x400F172A)
        )
        .clip(shape)
        .background(cardBgGradient)
        .border(BorderStroke(1.2.dp, borderGradient), shape)
}

// ── Root Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    prefsManager: SharedPrefsManager,
    repository: HealthRepository,
    stepsToday: Int,
    onNavigate: (String) -> Unit,
    onExitOverlayVisible: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val adEnabledNavigate: (String) -> Unit = { route ->
        val activity = context as? android.app.Activity
        val isZeroAdScreen = route in listOf("GPS_RUN_TRACKER", "HEART_RATE", "INSTRUCTIONS", "SETTINGS")
        if (activity != null && !isZeroAdScreen) {
            AdInterstitialHelper.showInterstitial(activity) {
                onNavigate(route)
            }
        } else {
            onNavigate(route)
        }
    }
    
    LaunchedEffect(Unit) {
        AdInterstitialHelper.loadInterstitial(context, "ad_dashboard_interstitial")
        val hasActivityPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else true
        if (hasActivityPermission) {
            try {
                val serviceIntent = Intent(context, StepCounterService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val langCode = remember { prefsManager.getLanguage() }
    val isHindi = langCode == "hi"
    val todayQuote = remember { MotivationalQuotes.getQuoteForToday() }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showExitDialog) {
        onExitOverlayVisible(showExitDialog)
    }

    DisposableEffect(Unit) {
        onDispose {
            onExitOverlayVisible(false)
        }
    }

    // Intercept back button to show exit quote confirmation
    BackHandler(enabled = true) {
        if (showExitDialog) {
            showExitDialog = false
        } else {
            showExitDialog = true
        }
    }

    val strings  = LocalAppStrings.current
    val stepGoal = prefsManager.getStepGoal()
    val progress = (stepsToday.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)

    // Three stackers metrics calculation
    val workouts by repository.getWorkoutHistory().collectAsState(initial = emptyList())
    val yogaSessions by repository.getYogaHistory().collectAsState(initial = emptyList())
    val todayStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }

    var showStepCelebration by remember { mutableStateOf(false) }
    LaunchedEffect(stepsToday) {
        if (stepsToday >= stepGoal && stepGoal > 0 && prefsManager.getStepCelebrationDate() != todayStr) {
            showStepCelebration = true
            prefsManager.saveStepCelebrationDate(todayStr)
        }
    }

    val todayWorkouts = workouts.filter { it.date == todayStr }
    val workoutCalories = todayWorkouts.sumOf { it.caloriesBurned }
    val workoutDurationMins = todayWorkouts.sumOf { it.repsOrMinutes }

    val todayYoga = yogaSessions.filter { it.date == todayStr }
    val yogaDurationMins = todayYoga.sumOf { it.durationSeconds } / 60

    val stepCalories = (stepsToday * 0.045f).toInt()
    val totalCalories = stepCalories + workoutCalories
    val walkingActiveMins = stepsToday / 100
    val totalActiveMins = walkingActiveMins + workoutDurationMins + yogaDurationMins

    val calorieGoal = 500
    val activeTimeGoal = 30

    val stepsHistory by repository.getStepHistory().collectAsState(initial = emptyList())
    val sleepHistory by repository.getSleepHistory().collectAsState(initial = emptyList())
    val streak = remember(stepsHistory, workouts, yogaSessions, stepsToday, stepGoal) {
        val activeDates = mutableSetOf<String>()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        
        stepsHistory.forEach { if (it.steps >= stepGoal) activeDates.add(it.date) }
        workouts.forEach { activeDates.add(it.date) }
        yogaSessions.forEach { activeDates.add(it.date) }
        
        if (stepsToday >= stepGoal || todayWorkouts.isNotEmpty() || todayYoga.isNotEmpty()) {
            activeDates.add(todayStr)
        }
        
        val cal = Calendar.getInstance()
        var currentDayStr = sdf.format(cal.time)
        if (!activeDates.contains(currentDayStr)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            currentDayStr = sdf.format(cal.time)
        }
        
        var count = 0
        while (activeDates.contains(currentDayStr)) {
            count++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            currentDayStr = sdf.format(cal.time)
        }
        
        count
    }

    LaunchedEffect(streak) {
        prefsManager.saveCurrentStreak(streak)
    }

    val isOverlayVisible = showExitDialog
    val blurRadius = if (isOverlayVisible) 16.dp else 0.dp

    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val canvasBgGradient = Brush.verticalGradient(
        colors = if (isDarkTheme) listOf(
            Color(0xFF0F1523),
            Color(0xFF080B14),
            Color(0xFF05070E)
        ) else listOf(
            Color(0xFFF1F5F9),
            Color(0xFFEEF2F6),
            Color(0xFFE2E8F0)
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius),
            containerColor = if (isDarkTheme) Color(0xFF080B14) else Color(0xFFEEF2F6),
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                HeroHeader(prefsManager, streak, onNavigate = adEnabledNavigate)
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .background(canvasBgGradient),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 2 ─ Step Progress Hero Card (Activity Hero Card)
                item {
                    val context = LocalContext.current
                    var isStepPaused by remember { mutableStateOf(prefsManager.isStepTrackingPaused()) }
                    StepHeroCard(
                        steps = stepsToday,
                        goal = stepGoal,
                        progress = progress,
                        activeCalories = totalCalories,
                        calorieGoal = calorieGoal,
                        activeMins = totalActiveMins,
                        activeGoal = activeTimeGoal,
                        onNavigate = adEnabledNavigate,
                        isPaused = isStepPaused,
                        onPauseToggle = { paused ->
                            isStepPaused = paused
                            prefsManager.setStepTrackingPaused(paused)
                            if (paused) {
                                context.sendBroadcast(android.content.Intent("com.pro.aifitnesstracker.PAUSE_TRACKING").apply { setPackage(context.packageName) })
                            } else {
                                context.sendBroadcast(android.content.Intent("com.pro.aifitnesstracker.RESUME_TRACKING").apply { setPackage(context.packageName) })
                            }
                        }
                    )
                }

                // 3 ─ Health Vitals 2x2 Section
                item {
                    Column {
                        Text(
                            text = "Health Vitals",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        HealthVitalsGrid(prefsManager, repository, todayStr, adEnabledNavigate, strings)
                    }
                }

                // 4 ─ Health Score Card
                item {
                    HealthScoreCard(
                        stepsToday = stepsToday,
                        stepGoal = stepGoal,
                        waterGlasses = prefsManager.getWaterGlassesToday(),
                        sleepHours = prefsManager.getSleepHoursToday(),
                        activeCalories = totalCalories,
                        calorieGoal = calorieGoal,
                        hasWorkoutToday = todayWorkouts.isNotEmpty() || todayYoga.isNotEmpty(),
                        heartRate = prefsManager.getLastBpm(),
                        onNavigate = adEnabledNavigate
                    )
                }

                // 6 ─ Today's Quick Stats
                item {
                    Column {
                        SectionLabel("TODAY'S STATS")
                        Spacer(Modifier.height(8.dp))
                        QuickStatsRow(prefsManager, adEnabledNavigate, repository)
                    }
                }


                // 5 ─ AI Suite Quick Actions
                val showFoodScanner = prefsManager.isFoodScannerDashboardEnabled()
                val showMealPlanner = prefsManager.isMealPlannerDashboardEnabled()
                val showYoga = prefsManager.isYogaDashboardEnabled()
                val showAlgoLab = prefsManager.isAlgorithmLabDashboardEnabled()
                val showAISuite = showFoodScanner || showMealPlanner || showYoga || showAlgoLab

                if (showAISuite) {
                    item {
                        Column {
                            SectionLabel("SMART FITNESS SUITE")
                            Spacer(Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    AiIntelligenceHubBannerCard(modifier = Modifier.width(315.dp), onClick = { adEnabledNavigate("AI_INTELLIGENCE_HUB") })
                                }
                                item {
                                    AiCoachBannerCard(modifier = Modifier.width(315.dp), onClick = { adEnabledNavigate("AI_COACH") })
                                }
                                if (showFoodScanner) {
                                    item {
                                        AiFoodScannerBannerCard(modifier = Modifier.width(315.dp), onClick = { adEnabledNavigate("AI_FOOD_SCANNER") })
                                    }
                                }
                                if (showMealPlanner) {
                                    item {
                                        AiMealPlannerBannerCard(modifier = Modifier.width(315.dp), onClick = { adEnabledNavigate("AI_MEAL_PLANNER") })
                                    }
                                }
                                if (showYoga) {
                                    item {
                                        AiYogaBannerCard(modifier = Modifier.width(300.dp), onClick = { adEnabledNavigate("YOGA") })
                                    }
                                }
                                if (showAlgoLab) {
                                    item {
                                        AiAlgorithmBannerCard(modifier = Modifier.width(300.dp), onClick = { adEnabledNavigate("ALGORITHM_LAB") })
                                    }
                                }
                            }
                        }
                    }
                }

                // 6 ─ More Tools
                item {
                    Column {
                        SectionLabel("MORE TOOLS")
                        Spacer(Modifier.height(8.dp))
                        MoreToolsRow(adEnabledNavigate)
                    }
                }
            }
        }
    }




    // Animated Exit Quote Overlay
    AnimatedVisibility(
        visible = showExitDialog,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
            initialOffsetY = { it }, 
            animationSpec = tween(600, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
        ),
        exit = fadeOut(animationSpec = tween(350)) + slideOutVertically(
            targetOffsetY = { it }, 
            animationSpec = tween(350)
        )
    ) {
        ExitQuoteOverlay(
            quote = todayQuote,
            langCode = langCode,
            onKeepGoing = { showExitDialog = false },
            onExit = { context.findActivity()?.finish() }
        )
    }

    if (showStepCelebration) {
        GoalCelebrationDialog(
            title = "🏆 Step Goal Reached!",
            message = "Incredible job! You walked $stepsToday steps and completed your step goal today!",
            icon = Icons.Rounded.DirectionsWalk,
            iconColor = Color(0xFF4CAF50),
            onDismiss = { showStepCelebration = false }
        )
    }
}

@Composable
fun AnimatedQuoteIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")
    
    when (icon) {
        Icons.Rounded.Bedtime -> {
            val rotation by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "moon_sway"
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "moon_pulse"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = modifier.graphicsLayer(
                    rotationZ = rotation,
                    scaleX = scale,
                    scaleY = scale
                )
            )
        }
        Icons.Rounded.DirectionsRun -> {
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "run_forward"
            )
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "run_bounce"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = modifier.graphicsLayer(
                    translationX = offsetX,
                    translationY = offsetY
                )
            )
        }
        Icons.Rounded.FitnessCenter -> {
            val scaleY by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "lift_stretch"
            )
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 3f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "lift_move"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = modifier.graphicsLayer(
                    scaleY = scaleY,
                    translationY = offsetY
                )
            )
        }
        Icons.Rounded.LocalDrink -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "water_wave"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "water_sway"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = modifier.graphicsLayer(
                    translationY = offsetY,
                    rotationZ = rotation
                )
            )
        }
        Icons.Rounded.DirectionsWalk -> {
            val rotation by infiniteTransition.animateFloat(
                initialValue = -12f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "walk_swing"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = modifier.graphicsLayer(
                    rotationZ = rotation
                )
            )
        }
        Icons.Rounded.TrendingUp -> {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 6f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "arrow_rise"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "arrow_fade"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = modifier.graphicsLayer(
                    translationY = offsetY,
                    alpha = alpha
                )
            )
        }
        Icons.Rounded.Favorite -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        0.9f at 0 with FastOutSlowInEasing
                        1.15f at 150 with FastOutSlowInEasing
                        1.0f at 300 with FastOutSlowInEasing
                        1.2f at 450 with FastOutSlowInEasing
                        0.9f at 700 with FastOutSlowInEasing
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "heart_pulse"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = modifier.graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
            )
        }
        else -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "default_pulse"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale)
            )
        }
    }
}

@Composable
fun StartupQuoteOverlay(
    quote: com.pro.aifitnesstracker.utils.Quote,
    langCode: String,
    journeyDay: Int,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full screen background image
        Image(
            painter = painterResource(id = quote.bgResId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Dark vertical gradient mask over the entire screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Push everything to the bottom
            Spacer(modifier = Modifier.weight(1f))

            // Day Badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF9FA8DA).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF9FA8DA).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${strings.day.uppercase(java.util.Locale.getDefault())} $journeyDay",
                    color = Color(0xFF9FA8DA),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quote text (small size)
            Text(
                text = "“${when (langCode) {
                    "hi" -> quote.hindi
                    "mr" -> quote.marathi
                    else -> quote.english
                }}”",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            var isDismissed by remember { mutableStateOf(false) }
            // Start My Day Button
            Button(
                onClick = {
                    if (!isDismissed) {
                        isDismissed = true
                        onDismiss()
                    }
                },
                enabled = !isDismissed,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9FA8DA)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = strings.startMyDay,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ExitQuoteOverlay(
    quote: com.pro.aifitnesstracker.utils.Quote,
    langCode: String,
    onKeepGoing: () -> Unit,
    onExit: () -> Unit
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full screen background image
        Image(
            painter = painterResource(id = quote.bgResId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Dark vertical gradient mask
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Push everything to the bottom
            Spacer(modifier = Modifier.weight(1f))

            // Quote text (small size)
            Text(
                text = "“${when (langCode) {
                    "hi" -> quote.hindi
                    "mr" -> quote.marathi
                    else -> quote.english
                }}”",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom area - Action Buttons (Keep Going & Exit App)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onKeepGoing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9FA8DA)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = strings.keepGoing,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                OutlinedButton(
                    onClick = onExit,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = strings.exitApp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// ── 1.5. Health Score Card ───────────────────────────────────────────────────
@Composable
fun HealthScoreCard(
    stepsToday: Int,
    stepGoal: Int,
    waterGlasses: Int,
    sleepHours: Float,
    activeCalories: Int,
    calorieGoal: Int,
    hasWorkoutToday: Boolean,
    heartRate: Int,
    onNavigate: (String) -> Unit = {}
) {
    // Scoring Algorithm (0 - 100)
    val stepFactor = if (stepGoal > 0) (stepsToday.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f) else 0f
    val stepsScore = stepFactor * 25f

    val waterFactor = (waterGlasses.toFloat() / 8f).coerceIn(0f, 1f)
    val waterScore = waterFactor * 20f

    val sleepFactor = (sleepHours / 8f).coerceIn(0f, 1f)
    val sleepScore = sleepFactor * 20f

    val workoutScore = if (hasWorkoutToday) 15f else 0f
    val calorieFactor = if (calorieGoal > 0) (activeCalories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f) else 0f
    val calScore = calorieFactor * 10f
    val exerciseScore = (workoutScore + calScore).coerceIn(0f, 25f)

    val hrScore = when {
        heartRate in 60..100 -> 10f
        heartRate in 50..59 || heartRate in 101..120 -> 6f
        else -> 3f
    }

    val totalScore = (stepsScore + waterScore + sleepScore + exerciseScore + hrScore).toInt().coerceIn(0, 100)

    // Animated Score - starts from 0 on first appearance
    var scoreTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(totalScore) { scoreTarget = totalScore.toFloat() }
    val animScore by animateFloatAsState(
        targetValue = scoreTarget,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "health_score"
    )

    // AI Recommendation message
    val recommendation = remember(stepsToday, stepGoal, waterGlasses, sleepHours, hasWorkoutToday) {
        when {
            waterGlasses < 6 -> "Dehydration warning! Drink more water to boost your focus and increase your score."
            stepsToday < stepGoal / 2 -> "Step count is low. A quick 15-minute walk will significantly improve your cardiovascular score."
            sleepHours < 6.5f -> "Rest is crucial. Plan for an early sleep tonight to aid muscle recovery and boost tomorrow's score."
            !hasWorkoutToday -> "You haven't logged a workout today. Try a quick yoga flow or active stretch session!"
            totalScore >= 85 -> "Outstanding! You are in peak athletic shape today. Keep up the high-energy streak!"
            else -> "Excellent progress today! Stay consistent to keep your body at its peak performance."
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .floatingElevationCard(accentColor = Color(0xFF10B981), shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side - Premium Gauge/Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    val trackArcColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    val gradientColors = listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF10B981))
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawArc(
                            color = trackArcColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(
                                width = 8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                        drawArc(
                            brush = Brush.sweepGradient(gradientColors),
                            startAngle = -90f,
                            sweepAngle = (animScore / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(
                                width = 8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${animScore.toInt()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "HEALTH SCORE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Right Side - Status Grid
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VITAL STATUS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (isDark) Color(0xFF132E1B) else Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "Optimal",
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HealthFactorRow(
                        label = "Activity",
                        color = Color(0xFF10B981),
                        value = if (hasWorkoutToday) "Workout logged" else "${stepsToday} steps"
                    )
                    HealthFactorRow(
                        label = "Hydration",
                        color = Color(0xFF0EA5E9),
                        value = "${waterGlasses}/8 glasses"
                    )
                    HealthFactorRow(
                        label = "Recovery",
                        color = Color(0xFF8B5CF6),
                        value = "${String.format(java.util.Locale.US, "%.1f", sleepHours)}h sleep"
                    )
                    HealthFactorRow(
                        label = "Vitals",
                        color = Color(0xFFEF4444),
                        value = "${heartRate} BPM"
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Spacer(
                Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            )
            Spacer(Modifier.height(12.dp))

            // AI Insight box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f)
                        else Color(0xFF10B981).copy(alpha = 0.08f)
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFF10B981).copy(alpha = 0.2f)
                        ),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = recommendation,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigate("PERFORMANCE") },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF10B981).copy(alpha = if (isDark) 0.18f else 0.10f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = if (isDark) 0.35f else 0.25f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "View Health Insights & Score",
                            color = Color(0xFF10B981),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HealthFactorRow(label: String, color: Color, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp
        )
    }
}


// ── Streak Heatmap Card ──────────────────────────────────────────────────────
@Composable
fun StreakHeatmapCard(
    stepsHistory: List<StepRecord>,
    workouts: List<WorkoutRecord>,
    yogaSessions: List<YogaRecord>,
    sleepHistory: List<SleepRecord>,
    stepGoal: Int,
    stepsToday: Int,
    hasWorkoutToday: Boolean,
    streak: Int,
    onNavigate: (String) -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val strings = LocalAppStrings.current
    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    val displaySdf = remember { java.text.SimpleDateFormat("EEE, MMM dd", java.util.Locale.getDefault()) }
    val todayCal = Calendar.getInstance()
    val todayStr = sdf.format(todayCal.time)
    val currentDayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK)

    // Days to subtract to get to Monday of the current week
    val daysToMonday = (currentDayOfWeek - Calendar.MONDAY + 7) % 7
    val startCal = remember(daysToMonday) {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToMonday)
            add(Calendar.WEEK_OF_YEAR, -9)
        }
    }

    // Generate 70 dates
    val dates = remember(startCal) {
        List(70) { index ->
            Calendar.getInstance().apply {
                time = startCal.time
                add(Calendar.DAY_OF_YEAR, index)
            }.time
        }
    }

    // Selected Date Details State
    var selectedDateStr by remember { mutableStateOf<String?>(null) }
    var selectedDetails by remember { mutableStateOf<String?>(null) }

    // Pre-process metrics map for O(1) lookups during render
    val stepsMap = remember(stepsHistory, stepsToday, todayStr) {
        val map = stepsHistory.associate { it.date to it.steps }.toMutableMap()
        map[todayStr] = stepsToday
        map
    }
    
    val workoutCountMap = remember(workouts, yogaSessions, hasWorkoutToday, todayStr) {
        val map = mutableMapOf<String, Int>()
        workouts.forEach { map[it.date] = (map[it.date] ?: 0) + 1 }
        yogaSessions.forEach { map[it.date] = (map[it.date] ?: 0) + 1 }
        if (hasWorkoutToday) {
            map[todayStr] = (map[todayStr] ?: 0) + 1
        }
        map
    }

    val sleepMap = remember(sleepHistory) {
        sleepHistory.associate { it.date to it.durationHours }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .floatingElevationCard(accentColor = ACCENT, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Streak Flame
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONSISTENCY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ACCENT,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Activity Heatmap",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Flame streak indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(ACCENT.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = ACCENT,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$streak DAY STREAK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ACCENT
                    )
                }
            }

            // Heatmap Grid Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day of Week Labels (Aligned to match 16.dp size and 4.dp spacing exactly)
                Column(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val labels = listOf("M", "", "W", "", "F", "", "S")
                    labels.forEach { label ->
                        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.CenterStart) {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Horizontal scrollable grid
                val scrollState = rememberScrollState()
                
                LaunchedEffect(Unit) {
                    scrollState.scrollTo(scrollState.maxValue)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                        .padding(vertical = 4.dp), // Prevents clipping of borders & touch feedback
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 10) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (row in 0 until 7) {
                                val dateIndex = col * 7 + row
                                val dateVal = dates[dateIndex]
                                val dateStr = sdf.format(dateVal)

                                val isFuture = dateVal.after(todayCal.time) && dateStr != todayStr
                                
                                val daySteps = stepsMap[dateStr] ?: 0
                                val workoutsCount = workoutCountMap[dateStr] ?: 0
                                val sleepHours = sleepMap[dateStr] ?: 0f

                                // Calculate Level
                                var points = 0
                                if (daySteps >= stepGoal) points += 2
                                else if (daySteps >= stepGoal / 2) points += 1
                                if (workoutsCount > 0) points += 2
                                if (sleepHours >= 7f) points += 1

                                val level = when {
                                    isFuture -> -1
                                    points == 0 -> 0
                                    points <= 2 -> 1
                                    points <= 4 -> 2
                                    else -> 3
                                }

                                val cellColor = when (level) {
                                    -1 -> Color.Transparent
                                    0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    1 -> ACCENT.copy(alpha = 0.25f)
                                    2 -> ACCENT.copy(alpha = 0.6f)
                                    else -> ACCENT
                                }

                                val isSelected = selectedDateStr == dateStr

                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(cellColor, RoundedCornerShape(3.dp))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                        .clickable(enabled = !isFuture) {
                                            selectedDateStr = dateStr
                                            val formattedDate = displaySdf.format(dateVal)
                                            val workoutText = if (workoutsCount > 0) "• $workoutsCount Workout(s)" else ""
                                            val sleepText = if (sleepHours > 0) "• ${String.format("%.1f", sleepHours)}h Sleep" else ""
                                            selectedDetails = "$formattedDate: $daySteps steps $workoutText $sleepText"
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Legend and detail info area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.less, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ACCENT.copy(alpha = 0.25f),
                        ACCENT.copy(alpha = 0.6f),
                        ACCENT
                    ).forEach { color ->
                        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(1.5.dp)))
                    }
                    Text(strings.moreText, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = strings.tapCellForDetails,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Selected Detail Box
            selectedDetails?.let { details ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = ACCENT,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = details,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigate("PROFILE") },
                shape = RoundedCornerShape(12.dp),
                color = ACCENT.copy(alpha = if (isDark) 0.18f else 0.10f),
                border = BorderStroke(1.dp, ACCENT.copy(alpha = if (isDark) 0.35f else 0.25f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = ACCENT,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "View Streak Badges & Milestones",
                            color = ACCENT,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = ACCENT,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

// ── 1. Hero Header ───────────────────────────────────────────────────────────
@Composable
fun HeroHeader(prefsManager: SharedPrefsManager, streak: Int, onNavigate: (String) -> Unit = {}) {
    var showNotificationCenter by remember { mutableStateOf(false) }
    val strings = LocalAppStrings.current
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else      -> "Good Evening"
    }
    val name = prefsManager.getUserName().ifBlank { "Athlete" }
    val isPremium = prefsManager.isPremiumUser()
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val topBarBg = if (isDarkTheme) Color(0xFF080B14) else Color(0xFFEEF2F6)

    Surface(
        color = topBarBg,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // FS Brand circular logo / avatar badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(
                                2.dp,
                                ACCENT
                            ),
                            CircleShape
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onNavigate("SETTINGS") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FS",
                        color = ACCENT,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$greeting, $name",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isPremium) "👑 Premium Active" else "Daily Movement Tracker",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Streak flame pill
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = ACCENT.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ACCENT.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = ACCENT,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$streak Days",
                            color = ACCENT,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                // Bell Icon Button with dynamic unread dot indicator
                val hasPendingReminders = remember {
                    prefsManager.getStepsToday() < prefsManager.getStepGoal() ||
                    prefsManager.getWaterGlassesToday() < prefsManager.getWaterGoal()
                }
                IconButton(
                    onClick = { showNotificationCenter = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = "Notification Center",
                            tint = ACCENT,
                            modifier = Modifier.size(22.dp)
                        )
                        if (hasPendingReminders) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 1.dp, end = 1.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5252))
                                    .border(1.dp, topBarBg, CircleShape)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(2.dp))
                IconButton(
                    onClick = { onNavigate("SETTINGS") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = ACCENT,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    if (showNotificationCenter) {
        InAppNotificationCenter(
            onDismiss = { showNotificationCenter = false },
            onNavigate = onNavigate,
            prefsManager = prefsManager
        )
    }
}

// ── 1.5. Dynamic Time-of-Day AI Action Card ─────────────────────────────────
@Composable
fun TimeOfDayAiActionCard(
    prefsManager: SharedPrefsManager,
    repository: HealthRepository,
    todayStr: String,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val name = prefsManager.getUserName().ifBlank { "Athlete" }
    val unifiedAccent = MaterialTheme.colorScheme.primary

    data class AiTimeConfig(
        val badge: String,
        val title: String,
        val subtitle: String,
        val primaryBtnText: String,
        val primaryBtnRoute: String,
        val secondaryBtnText: String,
        val isSecondaryWater: Boolean = false,
        val secondaryBtnRoute: String = "",
        val icon: ImageVector
    )

    val config = remember(hour, name) {
        when {
            hour in 5..11 -> AiTimeConfig(
                badge = "⚡ MORNING ROUTINE",
                title = "Energize Your Day, $name!",
                subtitle = "Kickstart your metabolism with a 10-min morning mobility flow and 500ml of hydration.",
                primaryBtnText = "Start Morning Flow",
                primaryBtnRoute = "YOGA",
                secondaryBtnText = "+250ml Water",
                isSecondaryWater = true,
                icon = Icons.Rounded.WbSunny
            )
            hour in 12..16 -> AiTimeConfig(
                badge = "🥗 NUTRITION & POSTURE",
                title = "Midday Vitality Boost!",
                subtitle = "Check your lunch macros with instant camera scanner or take a 15-min post-meal stroll.",
                primaryBtnText = "Scan Food",
                primaryBtnRoute = "AI_FOOD_SCANNER",
                secondaryBtnText = "Outdoor Walk",
                isSecondaryWater = false,
                secondaryBtnRoute = "RUN_TRACKER",
                icon = Icons.Rounded.CameraAlt
            )
            hour in 17..21 -> AiTimeConfig(
                badge = "🔥 PEAK ATHLETIC WINDOW",
                title = "Power Workout Time!",
                subtitle = "Your body temperature and strength are peaked right now. Perfect for an interactive posture workout.",
                primaryBtnText = "Start Workout",
                primaryBtnRoute = "WORKOUT_PLANNER",
                secondaryBtnText = "Outdoor Run",
                isSecondaryWater = false,
                secondaryBtnRoute = "RUN_TRACKER",
                icon = Icons.Rounded.FitnessCenter
            )
            else -> AiTimeConfig(
                badge = "🌙 WIND DOWN & RECOVERY",
                title = "Sleep & Muscle Recovery",
                subtitle = "Lower stress hormone levels with mindful breathing and check your heart rate before bed.",
                primaryBtnText = "Sleep Optimizer",
                primaryBtnRoute = "SLEEP",
                secondaryBtnText = "Check Heart Rate",
                isSecondaryWater = false,
                secondaryBtnRoute = "HEART_RATE",
                icon = Icons.Rounded.Bedtime
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .floatingElevationCard(accentColor = unifiedAccent, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            unifiedAccent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(unifiedAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = config.badge,
                            color = unifiedAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(unifiedAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = config.icon,
                            contentDescription = null,
                            tint = unifiedAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = config.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = config.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigate(config.primaryBtnRoute) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = unifiedAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = config.primaryBtnText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (config.isSecondaryWater) {
                                val currentGlasses = prefsManager.getWaterGlassesToday()
                                val newGlasses = currentGlasses + 1
                                prefsManager.saveWaterGlassesToday(newGlasses)
                                coroutineScope.launch {
                                    if (todayStr.isNotBlank()) {
                                        repository.updateWater(todayStr, newGlasses)
                                    }
                                }
                                android.widget.Toast.makeText(context, "💧 +250ml Logged! (${newGlasses * 250} ml today)", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                onNavigate(config.secondaryBtnRoute)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, unifiedAccent.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = config.secondaryBtnText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ── 2. Step Hero Card ────────────────────────────────────────────────────────
@Composable
fun StepHeroCard(
    steps: Int,
    goal: Int,
    progress: Float = 0f,
    activeCalories: Int = 0,
    calorieGoal: Int = 500,
    activeMins: Int = 0,
    activeGoal: Int = 30,
    onNavigate: (String) -> Unit,
    isPaused: Boolean = false,
    onPauseToggle: (Boolean) -> Unit = {}
) {
    val stepProgress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    val calorieProgress = if (calorieGoal > 0) (activeCalories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f) else 0f
    val activeProgress = if (activeGoal > 0) (activeMins.toFloat() / activeGoal.toFloat()).coerceIn(0f, 1f) else 0f

    val stepPercent = if (goal > 0) (steps.toFloat() / goal * 100) else 0f
    val caloriePercent = if (calorieGoal > 0) (activeCalories.toFloat() / calorieGoal * 100) else 0f
    val activePercent = if (activeGoal > 0) (activeMins.toFloat() / activeGoal * 100) else 0f

    // Animate from 0 on first appearance
    var stepTarget by remember { mutableStateOf(0f) }
    var calorieTarget by remember { mutableStateOf(0f) }
    var activeTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(stepProgress) { stepTarget = stepProgress }
    LaunchedEffect(calorieProgress) { calorieTarget = calorieProgress }
    LaunchedEffect(activeProgress) { activeTarget = activeProgress }

    val animStepProgress by animateFloatAsState(
        targetValue = stepTarget,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "steps_progress"
    )
    val animCalorieProgress by animateFloatAsState(
        targetValue = calorieTarget,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "calorie_progress"
    )
    val animActiveProgress by animateFloatAsState(
        targetValue = activeTarget,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "active_progress"
    )

    val stepColor = Color(0xFFFF4757) // Coral Red for Steps
    val calorieColor = Color(0xFFFF9233) // Sunset Amber for Calories
    val activeColor = Color(0xFF10B981) // Emerald Mint for Active Time
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .floatingElevationCard(accentColor = Color(0xFF6366F1), shape = RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title & Tracking Active Glowing Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity Hero Card",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (isPaused) Color(0xFFFF5252).copy(alpha = 0.12f) else Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isPaused) Color(0xFFFF5252).copy(0.35f) else Color(0xFF10B981).copy(0.35f)
                    ),
                    modifier = Modifier.clickable { onPauseToggle(!isPaused) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isPaused) Color(0xFFFF5252) else Color(0xFF10B981))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) "Tracking Paused" else "Tracking Active",
                            color = if (isPaused) Color(0xFFFF5252) else Color(0xFF10B981),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Concentric Rings on Left & Tracking Activities on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Top arc percentages + Concentric Rings with Shoe, Steps, Flame & Kcal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    // Top arc percentages matching mockup
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%%", stepPercent),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = stepColor
                        )
                        Text(
                            text = "${caloriePercent.toInt()}%",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = calorieColor
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%%", activePercent),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeColor
                        )
                    }

                    // Triple Concentric Rings
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(136.dp)
                    ) {
                        // Outer Ring (Steps)
                        CircularProgressIndicator(
                            progress = { animStepProgress },
                            modifier = Modifier.size(136.dp),
                            color = stepColor,
                            strokeWidth = 9.dp,
                            trackColor = stepColor.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        // Middle Ring (Calories)
                        CircularProgressIndicator(
                            progress = { animCalorieProgress },
                            modifier = Modifier.size(106.dp),
                            color = calorieColor,
                            strokeWidth = 7.5.dp,
                            trackColor = calorieColor.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        // Inner Ring (Active Time)
                        CircularProgressIndicator(
                            progress = { animActiveProgress },
                            modifier = Modifier.size(78.dp),
                            color = activeColor,
                            strokeWidth = 6.dp,
                            trackColor = activeColor.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )

                        // Center Data Content
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Shoe icon at top of center stack
                            Icon(
                                painter = painterResource(com.pro.aifitnesstracker.R.drawable.ic_shoe),
                                contentDescription = "Step Icon",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%,d", steps),
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "STEPS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                letterSpacing = 0.8.sp
                            )
                            Spacer(Modifier.height(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "Calories",
                                    tint = calorieColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "$activeCalories kcal",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Right side: Activity Metric Rows matching mockup
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // 1. Step Tracking Row
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Step Tracking",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(${String.format(java.util.Locale.US, "%,d", steps)} / ${String.format(java.util.Locale.US, "%,d", goal)})",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                painter = painterResource(com.pro.aifitnesstracker.R.drawable.ic_shoe),
                                contentDescription = "Step Tracking",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animStepProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.5.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = stepColor,
                            trackColor = stepColor.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(stepProgress * 100).toInt()}%",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%,d", goal),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 2. Calories Row
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Calories",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "($activeCalories / $calorieGoal kcal)",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = "Calories",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animCalorieProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.5.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = calorieColor,
                            trackColor = calorieColor.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(calorieProgress * 100).toInt()}%",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = calorieGoal.toString(),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 3. Active Time Row
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Active Time",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "($activeMins / $activeGoal min)",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (activeMins >= activeGoal) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Goal met",
                                            tint = activeColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = "Goal met",
                                            color = activeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.WatchLater,
                                contentDescription = "Active Time",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animActiveProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.5.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = activeColor,
                            trackColor = activeColor.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeMins} min",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f%%", activePercent),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Action Button: View Analytics & History
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigate("STEP_DETAIL") },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.25f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "View Analytics & History",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom GPS Outdoor Walk / Run Tracker Banner with Map Icon, Running Icon & START button
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onNavigate("RUN_TRACKER") }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = "GPS Outdoor Map",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "GPS Walk & Run Tracker",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.DirectionsRun,
                        contentDescription = "Run",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFF057A55))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "START",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MetricProgressRow(label: String, value: String, color: Color, progress: Float) {
    var progressTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(progress) { progressTarget = progress }
    val animProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "metric_progress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { animProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.5.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun MiniStat(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = ACCENT.copy(0.7f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.height(2.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    }
}

// ── 3. Section Label ─────────────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(ACCENT, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}

// ── 4. Quick Stats (2x2 Daily Habits & Actions Grid) ────────────────────────
@Composable
fun QuickStatsRow(
    prefsManager: SharedPrefsManager,
    onNavigate: (String) -> Unit,
    repository: HealthRepository? = null
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // 1. Fasting Data
    val isFasting = prefsManager.isFastingActive()
    val fastingStartTime = prefsManager.getFastingStartTime()
    val fastingGoalHours = prefsManager.getFastingGoalHours().coerceAtLeast(1)
    val fastingElapsedMillis = if (isFasting && fastingStartTime > 0) (System.currentTimeMillis() - fastingStartTime).coerceAtLeast(0L) else 0L
    val elHours = (fastingElapsedMillis / 3600000L).toInt()
    val elMins = ((fastingElapsedMillis % 3600000L) / 60000L).toInt()
    val fastingVal = if (isFasting) "${elHours}h ${elMins}m" else "${fastingGoalHours}h Fast"
    val fastingSub = if (isFasting) "🔥 Active Session" else "Ready to start"
    val fastingAction = if (isFasting) "Active" else "Start"

    // 2. Nutrition Data
    val todayDateStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }
    val todayMealsFlow = remember(repository, todayDateStr) { repository?.getMealsByDate(todayDateStr) }
    val todayMeals by (todayMealsFlow?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<MealRecord>()) })
    val calorieTarget = prefsManager.getCalorieGoal().let { if (it > 0) it else 2000 }
    val caloriesConsumed = todayMeals.sumOf { it.calories }
    val caloriesRemaining = (calorieTarget - caloriesConsumed).coerceAtLeast(0)
    val dietVal = "$caloriesRemaining"
    val dietSub = "$caloriesConsumed / $calorieTarget kcal"

    // 3. Mindful Breathing Data
    val workoutsFlow = remember(repository) { repository?.getWorkoutHistory() }
    val allWorkouts by (workoutsFlow?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<WorkoutRecord>()) })
    val breathingSessions = remember(allWorkouts) {
        allWorkouts.filter { it.workoutType.startsWith("Breathing (") }
    }
    val todayBreathingMinutes = remember(breathingSessions, todayDateStr) {
        breathingSessions.filter { it.date == todayDateStr }.sumOf { it.repsOrMinutes }
    }

    // 4. Workout Plan Data
    val plannedWorkoutsFlow = remember(repository, todayDateStr) { repository?.getWorkoutPlansByDate(todayDateStr) }
    val plannedWorkouts by (plannedWorkoutsFlow?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<WorkoutPlanRecord>()) })
    val currentPlan = remember(plannedWorkouts) {
        plannedWorkouts.firstOrNull() ?: WorkoutPlanRecord(
            id = -1,
            date = todayDateStr,
            title = "Push & Chest",
            subtitle = "Today's Routine",
            duration = "45 min",
            intensity = "High",
            exercises = "Bench Press - 4x8",
            category = "Strength"
        )
    }
    val workoutVal = currentPlan.title.take(15)
    val workoutSub = "${currentPlan.duration} • ${currentPlan.intensity.take(10)}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Fasting & Nutrition
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DailyHabitTile(
                modifier = Modifier.weight(1f),
                title = "Fasting",
                icon = Icons.Rounded.HourglassEmpty,
                accentColor = Color(0xFFF97316),
                valueText = fastingVal,
                subtitle = fastingSub,
                actionText = fastingAction,
                onAction = { onNavigate("FASTING_TRACKER") },
                isDark = isDark
            )
            DailyHabitTile(
                modifier = Modifier.weight(1f),
                title = "Nutrition",
                icon = Icons.Rounded.Restaurant,
                accentColor = Color(0xFF0EA5E9),
                valueText = dietVal,
                valueSuffix = "kcal left",
                subtitle = dietSub,
                actionText = "Log Meal",
                onAction = { onNavigate("DIET_TRACKER") },
                isDark = isDark
            )
        }

        // Row 2: Mindful Breathing & Workout Routine
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DailyHabitTile(
                modifier = Modifier.weight(1f),
                title = "Mindfulness",
                icon = Icons.Rounded.SelfImprovement,
                accentColor = Color(0xFF2DD4BF),
                valueText = "$todayBreathingMinutes",
                valueSuffix = "/ 10m",
                subtitle = "Daily Breathwork",
                actionText = "Breathe",
                onAction = { onNavigate("BREATHING") },
                isDark = isDark
            )
            DailyHabitTile(
                modifier = Modifier.weight(1f),
                title = "Workout",
                icon = Icons.Rounded.FitnessCenter,
                accentColor = Color(0xFF8B5CF6),
                valueText = workoutVal,
                subtitle = workoutSub,
                actionText = "Workout",
                onAction = { onNavigate("WORKOUT_PLANNER") },
                isDark = isDark
            )
        }
    }
}

@Composable
private fun DailyHabitTile(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    accentColor: Color,
    valueText: String,
    valueSuffix: String = "",
    subtitle: String,
    actionText: String,
    onAction: () -> Unit,
    isDark: Boolean
) {
    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 142.dp)
            .floatingElevationCard(accentColor = accentColor, shape = RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Label & Glowing Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Value + Subtitle
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = valueText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (valueSuffix.isNotEmpty()) {
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = valueSuffix,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Pill Button (Clearly clickable button per user instructions)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onAction() },
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = if (isDark) 0.18f else 0.10f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.35f else 0.22f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = actionText,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}


// ── 5. Health Vitals Grid (2x2 Professional Cards) ───────────────────────────
@Composable
fun HealthVitalsGrid(
    prefsManager: SharedPrefsManager,
    repository: HealthRepository? = null,
    todayStr: String = "",
    onNavigate: (String) -> Unit,
    strings: AppStrings
) {
    val coroutineScope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // 1. Heart Rate Data
    val lastBpm = prefsManager.getLastBpm()
    val bpmVal = if (lastBpm > 0) lastBpm else 72
    val hrStatus = when {
        bpmVal < 60 -> "Low"
        bpmVal in 60..100 -> "Normal"
        else -> "Elevated"
    }

    // 2. Blood Pressure Data
    val bp = prefsManager.getLastBloodPressure()
    val systolic = if (bp.first > 0) bp.first else 120
    val diastolic = if (bp.second > 0) bp.second else 80
    val bpStatus = when {
        systolic < 120 && diastolic < 80 -> "Optimal"
        systolic in 120..129 && diastolic < 80 -> "Normal"
        systolic in 130..139 || diastolic in 80..89 -> "Elevated"
        else -> "High"
    }

    // Gauge needle animation based on actual systolic blood pressure (range 80 - 180 mmHg)
    val gaugeFraction = ((systolic.toFloat() - 80f) / (180f - 80f)).coerceIn(0.08f, 0.92f)
    val targetNeedleAngle = gaugeFraction * 180f
    var needleTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(targetNeedleAngle) {
        needleTarget = targetNeedleAngle
    }
    val animNeedleAngle by animateFloatAsState(
        targetValue = needleTarget,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "bp_gauge_needle"
    )

    // 3. Water Hydration Data
    var waterGlassesState by remember { mutableStateOf(prefsManager.getWaterGlassesToday()) }
    val waterLiters = waterGlassesState * 0.25f
    val waterGoalGlasses = if (prefsManager.getWaterGoal() > 0) prefsManager.getWaterGoal() else 10
    val waterGoalLiters = waterGoalGlasses * 0.25f

    // 4. Sleep Optimizer Data
    val sleepHoursTotal = prefsManager.getSleepHoursToday()
    val sleepH = if (sleepHoursTotal > 0f) sleepHoursTotal.toInt() else 7
    val sleepM = if (sleepHoursTotal > 0f) ((sleepHoursTotal - sleepH) * 60).toInt() else 45
    val sleepGoal = if (prefsManager.getSleepGoal() > 0f) prefsManager.getSleepGoal() else 8f
    val sleepEfficiency = ((sleepHoursTotal / sleepGoal) * 100).toInt().coerceIn(40, 98).takeIf { sleepHoursTotal > 0 } ?: 94

    // Dynamic badge colors for Light and Dark modes
    val greenBadgeBg = if (isDark) Color(0xFF132E1B) else Color(0xFFE8F5E9)
    val greenBadgeText = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
    val purpleBadgeBg = if (isDark) Color(0xFF2E1065) else Color(0xFFF3E8FF)
    val purpleBadgeText = if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Row 1: Heart Rate & Blood Pressure ───────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Heart Rate Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 176.dp)
                    .floatingElevationCard(accentColor = Color(0xFFFF3366), shape = RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Row: Label & Glowing Red Heart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Heart Rate",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Heart",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Value: 72 BPM
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = bpmVal.toString(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "BPM",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Bottom Row: "Normal" Badge & Crisp Red ECG Pulse Wave Line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = greenBadgeBg
                        ) {
                            Text(
                                text = hrStatus,
                                color = greenBadgeText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // ECG Heartbeat Canvas
                        val ecgPath = remember { Path() }
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .padding(start = 8.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val midY = h * 0.62f
                            ecgPath.reset()
                            ecgPath.moveTo(0f, midY)
                            ecgPath.lineTo(w * 0.12f, midY)
                            ecgPath.lineTo(w * 0.18f, midY - h * 0.15f)
                            ecgPath.lineTo(w * 0.24f, midY)
                            ecgPath.lineTo(w * 0.32f, midY)
                            ecgPath.lineTo(w * 0.38f, midY + h * 0.28f)
                            ecgPath.lineTo(w * 0.44f, midY - h * 0.85f)
                            ecgPath.lineTo(w * 0.50f, midY + h * 0.65f)
                            ecgPath.lineTo(w * 0.55f, midY)
                            ecgPath.lineTo(w * 0.64f, midY - h * 0.35f)
                            ecgPath.lineTo(w * 0.72f, midY)
                            ecgPath.lineTo(w * 0.82f, midY + h * 0.50f)
                            ecgPath.lineTo(w * 0.88f, midY - h * 0.80f)
                            ecgPath.lineTo(w * 0.94f, midY)
                            ecgPath.lineTo(w, midY)
                            drawPath(
                                path = ecgPath,
                                color = Color(0xFFEF4444),
                                style = Stroke(
                                    width = 2.2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFF3366).copy(alpha = if (isDark) 0.20f else 0.10f),
                        border = BorderStroke(1.dp, Color(0xFFFF3366).copy(alpha = if (isDark) 0.35f else 0.22f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clickable { onNavigate("HEART_RATE") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Check Pulse",
                                color = Color(0xFFFF3366),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFFFF3366),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // 2. Blood Pressure Card with Real Moving Gauge Needle
            Card(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 176.dp)
                    .floatingElevationCard(accentColor = Color(0xFF6366F1), shape = RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header: Label
                    Text(
                        text = "Blood Pressure",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Value: 120/80 mmHg
                    Column {
                        Text(
                            text = "$systolic/$diastolic",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "mmHg",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bottom Row: "Optimal" Badge & Speedometer Gauge with Animated Needle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = greenBadgeBg
                        ) {
                            Text(
                                text = bpStatus,
                                color = greenBadgeText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Speedometer Radial Arc Gauge with Colored Segments and Moving Needle
                        Box(
                            modifier = Modifier
                                .size(64.dp, 36.dp)
                                .padding(end = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val needleColor = MaterialTheme.colorScheme.onSurface
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val arcRadius = size.width / 2f - 3.dp.toPx()
                                val gaugeCenter = Offset(size.width / 2f, size.height - 1.dp.toPx())
                                val strokeW = 4.5.dp.toPx()

                                val arcRectTopLeft = Offset(gaugeCenter.x - arcRadius, gaugeCenter.y - arcRadius)
                                val arcRectSize = Size(arcRadius * 2f, arcRadius * 2f)

                                // Segment 1: Green (Normal/Optimal 80-120 mmHg)
                                drawArc(
                                    color = Color(0xFF22C55E),
                                    startAngle = 180f,
                                    sweepAngle = 72f,
                                    useCenter = false,
                                    topLeft = arcRectTopLeft,
                                    size = arcRectSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                                // Segment 2: Yellow/Amber (Elevated 120-135 mmHg)
                                drawArc(
                                    color = Color(0xFFF59E0B),
                                    startAngle = 252f,
                                    sweepAngle = 54f,
                                    useCenter = false,
                                    topLeft = arcRectTopLeft,
                                    size = arcRectSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                                )
                                // Segment 3: Red/Coral (High 135-180+ mmHg)
                                drawArc(
                                    color = Color(0xFFEF4444),
                                    startAngle = 306f,
                                    sweepAngle = 54f,
                                    useCenter = false,
                                    topLeft = arcRectTopLeft,
                                    size = arcRectSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )

                                // Animated Moving Needle Line pointing accurately based on data
                                val needleRadian = Math.toRadians((180f + animNeedleAngle).toDouble())
                                val needleLength = arcRadius * 0.88f
                                val needleTip = Offset(
                                    (gaugeCenter.x + needleLength * Math.cos(needleRadian)).toFloat(),
                                    (gaugeCenter.y + needleLength * Math.sin(needleRadian)).toFloat()
                                )

                                drawLine(
                                    color = needleColor,
                                    start = gaugeCenter,
                                    end = needleTip,
                                    strokeWidth = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = needleColor,
                                    radius = 3.2.dp.toPx(),
                                    center = gaugeCenter
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF6366F1).copy(alpha = if (isDark) 0.20f else 0.10f),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = if (isDark) 0.35f else 0.22f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clickable { onNavigate("BP_DETAIL") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BP Report",
                                color = Color(0xFF6366F1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Row 2: Water Hydration & Sleep Optimizer ─────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3. Water Hydration Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 176.dp)
                    .floatingElevationCard(accentColor = Color(0xFF0EA5E9), shape = RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Row: Label & Glowing Cyan Water Drop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Water Hydration",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0EA5E9).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = "Water Drop",
                                tint = Color(0xFF0EA5E9),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Value: 1.8L / 2.5L
                    Text(
                        text = String.format(java.util.Locale.US, "%.1fL / %.1fL", waterLiters, waterGoalLiters),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )

                    // Wave container with +250ml quick action button pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0EA5E9).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        // Fluid Wave Line
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val wavePath = Path().apply {
                                moveTo(0f, h * 0.45f)
                                cubicTo(w * 0.25f, h * 0.20f, w * 0.50f, h * 0.75f, w * 0.75f, h * 0.35f)
                                cubicTo(w * 0.90f, h * 0.15f, w, h * 0.40f, w, h * 0.45f)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            }
                            drawPath(
                                path = wavePath,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0EA5E9).copy(alpha = 0.25f),
                                        Color(0xFF0EA5E9).copy(alpha = 0.50f)
                                    )
                                )
                            )
                        }

                        // +250ml Pill Action Button
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (isDark) Color(0xFF0284C7) else Color(0xFFE0F2FE),
                            border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clickable {
                                    val newGlasses = waterGlassesState + 1
                                    waterGlassesState = newGlasses
                                    prefsManager.saveWaterGlassesToday(newGlasses)
                                    coroutineScope.launch {
                                        if (repository != null && todayStr.isNotBlank()) {
                                            repository.updateWater(todayStr, newGlasses)
                                        }
                                    }
                                    if (newGlasses >= 8) {
                                        prefsManager.unlockBadge("WATER_HERO")
                                    }
                                }
                        ) {
                            Text(
                                text = "+250ml",
                                color = if (isDark) Color.White else Color(0xFF0284C7),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0EA5E9).copy(alpha = if (isDark) 0.20f else 0.10f),
                        border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = if (isDark) 0.35f else 0.22f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clickable { onNavigate("WATER") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Open Tracker",
                                color = Color(0xFF0EA5E9),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF0EA5E9),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // 4. Sleep Optimizer Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 176.dp)
                    .floatingElevationCard(accentColor = Color(0xFF8B5CF6), shape = RoundedCornerShape(22.dp))
                    .clickable { onNavigate("SLEEP") },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Row: Label & Purple Moon with Stars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sleep Optimizer",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = "Sleep Moon",
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Value: 7h 45m (Guaranteed Single-Line Horizontal)
                    Text(
                        text = "${sleepH}h ${sleepM}m",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false
                    )

                    // Bottom Row: 94% Efficiency Badge & Nocturnal Sleep Wave Curve
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = purpleBadgeBg
                        ) {
                            Text(
                                text = "$sleepEfficiency%",
                                color = purpleBadgeText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Sleep Cycle Nocturnal Wave Curve
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .padding(start = 8.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(0f, h * 0.70f)
                                cubicTo(w * 0.15f, h * 0.95f, w * 0.25f, h * 0.20f, w * 0.40f, h * 0.55f)
                                cubicTo(w * 0.55f, h * 0.85f, w * 0.70f, h * 0.05f, w * 0.85f, h * 0.45f)
                                cubicTo(w * 0.92f, h * 0.70f, w * 0.98f, h * 0.25f, w, h * 0.55f)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF8B5CF6),
                                style = Stroke(
                                    width = 2.2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.20f else 0.10f),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.35f else 0.22f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clickable { onNavigate("SLEEP") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sleep Analysis",
                                color = Color(0xFF8B5CF6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── 6. AI Suite Grid ─────────────────────────────────────────────────────────
@Composable
fun AISuiteGrid(onNavigate: (String) -> Unit, strings: AppStrings) {
    val items = listOf(
        Triple(Icons.Default.Accessibility, strings.yoga,      "YOGA"),
        Triple(Icons.Default.Science,       strings.algorithm, "ALGORITHM_LAB"),
    )
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // AI Intelligence Hub - Featured Comparison & Trends Banner Card
        AiIntelligenceHubBannerCard(onClick = { onNavigate("AI_INTELLIGENCE_HUB") })

        // AI Coach & Workout Generator - Featured Banner Card
        AiCoachBannerCard(onClick = { onNavigate("AI_COACH") })

        // AI Food Scanner - Featured Banner Card
        AiFoodScannerBannerCard(onClick = { onNavigate("AI_FOOD_SCANNER") })

        // AI Meal Planner - Featured Banner Card
        AiMealPlannerBannerCard(onClick = { onNavigate("AI_MEAL_PLANNER") })

        // Existing AI Suite Row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { (icon, label, route) ->
                AISuiteCard(
                    icon = icon,
                    label = label,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(route) }
                )
            }
        }
    }
}

@Composable
fun AISuiteCard(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 96.dp)
            .floatingElevationCard(accentColor = ACCENT, shape = RoundedCornerShape(22.dp)),
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 96.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(ACCENT.copy(alpha = 0.05f), Color.Transparent)
                    )
                )
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(ACCENT.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ACCENT, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── AI Intelligence Hub Banner Card ───────────────────────────────────────
@Composable
fun AiIntelligenceHubBannerCard(modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Card(
        modifier = modifier
            .floatingElevationCard(accentColor = Color(0xFF06B6D4), shape = RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Icon + Title + Action Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF818CF8)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "Performance Hub",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF06B6D4),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Compare",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Row 2: Full-width Subtitle
            Text(
                text = "⚡ Period-by-period delta matrix & predictive goal simulator.",
                color = Color(0xFF06B6D4),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

// ── AI Coach Banner Card ───────────────────────────────────────────────────
@Composable
fun AiCoachBannerCard(modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Card(
        modifier = modifier
            .floatingElevationCard(accentColor = Color(0xFF6366F1), shape = RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Icon + Title + Action Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF6366F1)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Fitness Coach",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF6366F1),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Coach",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Row 2: Full-width Subtitle
            Text(
                text = "⚡ Pro Coaching Active • Live personal fitness & training guidance.",
                color = Color(0xFF06B6D4),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

// ── AI Food Scanner Banner Card ───────────────────────────────────────────────
@Composable
fun AiFoodScannerBannerCard(modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Card(
        modifier = modifier
            .floatingElevationCard(accentColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Icon + Title + Action Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "Food Calorie Scanner",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Scan",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Row 2: Full-width Subtitle
            Text(
                text = "Instant camera scanner to calculate calories, macros and nutrition insights.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

// ── AI Meal Planner Banner Card ───────────────────────────────────────────────
@Composable
fun AiMealPlannerBannerCard(modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Card(
        modifier = modifier
            .floatingElevationCard(accentColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Icon + Title + Action Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestaurantMenu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "Custom Meal Planner",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Plan",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Row 2: Full-width Subtitle
            Text(
                text = "Personalized 7-day meal plans and calorie-balanced nutrition routines.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

// ── AI Yoga Banner Card ────────────────────────────────────────────────────────
@Composable
fun AiYogaBannerCard(modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Card(
        modifier = modifier
            .floatingElevationCard(accentColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Yoga Icon circle box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Yoga & Posture Guide",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Real-time posture assessment, guided yoga sessions and breathing tools.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Yoga",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

// ── AI Algorithm Banner Card ───────────────────────────────────────────────────
@Composable
fun AiAlgorithmBannerCard(modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Card(
        modifier = modifier
            .floatingElevationCard(accentColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Algorithm / Science Icon circle box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Science,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Health Analytics Lab",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Bio-circuit computations, predictive health models and fitness science.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Lab",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MoreToolsRow(onNavigate: (String) -> Unit) {
    val strings = LocalAppStrings.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val itemWidth = (screenWidth - 40.dp) / 3

    val tools = listOf(
        Triple(Icons.Default.Bedtime,       strings.sleep,     "SLEEP"),
        Triple(Icons.Default.SelfImprovement, strings.yoga,      "YOGA"),
        Triple(Icons.Default.Timer,         strings.fasting,   "FASTING_TRACKER"),
        Triple(Icons.Default.Restaurant,    strings.diet,      "DIET_TRACKER"),
        Triple(Icons.Default.MonitorWeight, strings.weight,    "WEIGHT_DETAIL"),
        Triple(Icons.Default.FitnessCenter, strings.workout,   "WORKOUT_PLANNER"),
        Triple(Icons.Default.DirectionsRun, strings.run,       "RUN_TRACKER"),
        Triple(Icons.Default.Analytics,     strings.analytics, "ANALYTICS"),
        Triple(Icons.Default.Air,           strings.breathing, "BREATHING"),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tools.forEach { (icon, label, route) ->
            item {
                ToolPill(icon = icon, label = label, width = itemWidth, onClick = { onNavigate(route) })
            }
        }
    }
}

@Composable
fun ToolPill(icon: ImageVector, label: String, width: Dp, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier.width(width)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label, 
                color = MaterialTheme.colorScheme.onSurface, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

// ── Bottom Nav (5 Professional Tabs) ──────────────────────────────────────────
@Composable
fun BottomNavigationBar(
    currentRoute: String? = "DASHBOARD",
    onNavigate: (String) -> Unit
) {
    val isHome = currentRoute == "DASHBOARD"
    val isWorkout = currentRoute in listOf("WORKOUT_PLANNER", "WORKOUT_LIBRARY")
    val isAiCoach = currentRoute in listOf("AI_COACH", "AI_WORKOUT_GENERATOR")
    val isAnalytics = currentRoute in listOf("PERFORMANCE", "ANALYTICS", "STEP_REPORT", "CALORIE_REPORT", "DISTANCE_REPORT", "TIME_REPORT", "HYDRATION_REPORT")
    val isProfile = currentRoute in listOf("PROFILE", "SETTINGS", "MORE_SETTINGS", "REMINDER_SETTINGS", "PREMIUM")

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        val selectedColor = MaterialTheme.colorScheme.primary
        val unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        val indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
            selected = isHome,
            onClick = { onNavigate("DASHBOARD") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                selectedTextColor = selectedColor,
                unselectedIconColor = unselectedIconColor,
                unselectedTextColor = unselectedTextColor,
                indicatorColor = indicatorColor
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Workouts") },
            label = { Text("Workouts", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
            selected = isWorkout,
            onClick = { onNavigate("WORKOUT_PLANNER") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                selectedTextColor = selectedColor,
                unselectedIconColor = unselectedIconColor,
                unselectedTextColor = unselectedTextColor,
                indicatorColor = indicatorColor
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Psychology, contentDescription = "Coach") },
            label = { Text("Coach", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
            selected = isAiCoach,
            onClick = { onNavigate("AI_COACH") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                selectedTextColor = selectedColor,
                unselectedIconColor = unselectedIconColor,
                unselectedTextColor = unselectedTextColor,
                indicatorColor = indicatorColor
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
            label = { Text("Analytics", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
            selected = isAnalytics,
            onClick = { onNavigate("PERFORMANCE") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                selectedTextColor = selectedColor,
                unselectedIconColor = unselectedIconColor,
                unselectedTextColor = unselectedTextColor,
                indicatorColor = indicatorColor
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
            selected = isProfile,
            onClick = { onNavigate("PROFILE") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                selectedTextColor = selectedColor,
                unselectedIconColor = unselectedIconColor,
                unselectedTextColor = unselectedTextColor,
                indicatorColor = indicatorColor
            )
        )
    }
}

// ── Legacy composables (still used by other screens) ─────────────────────────
@Composable
fun HeaderSection(title: String) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(strings.elitePerformance, color = ACCENT, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = {}, modifier = Modifier.background(CARD, RoundedCornerShape(12.dp))) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = ACCENT)
        }
    }
}

@Composable
fun StepProgressCard(steps: Int, goal: Int, progress: Float, stepsLabel: String, goalLabel: String) {
    StepHeroCard(steps = steps, goal = goal, progress = progress, onNavigate = {})
}

@Composable
fun StatsGrid(prefsManager: SharedPrefsManager, onNavigate: (String) -> Unit, strings: AppStrings) {
    HealthVitalsGrid(prefsManager = prefsManager, onNavigate = onNavigate, strings = strings)
}



@Composable
fun QuickActionsSection(onNavigate: (String) -> Unit, strings: AppStrings) {
    AISuiteGrid(onNavigate = onNavigate, strings = strings)
}

@Composable
fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AISuiteCard(icon = icon, label = label, modifier = modifier, onClick = onClick)
}

// ── Visualizations Preview Card ───────────────────────────────────────────────
@Composable
fun VisualizationsPreviewCard(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier.padding(horizontal = 20.dp)
) {
    val cardBg = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .floatingElevationCard(accentColor = accent, shape = RoundedCornerShape(20.dp)),
        onClick = { onNavigate("VISUALIZATIONS") },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(0.06f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Health Visualizations",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(accent.copy(0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("VIEW ALL", color = accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(Icons.Default.Favorite, "ECG", accent),
                    Triple(Icons.Default.Speed, "BP Gauge", accent),
                    Triple(Icons.Default.ShowChart, "Trend", accent),
                    Triple(Icons.Default.GridView, "Heatmap", accent)
                ).forEach { (icon, label, color) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(color.copy(0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

