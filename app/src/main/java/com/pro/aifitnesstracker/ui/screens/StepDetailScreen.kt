package com.pro.aifitnesstracker.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager
import com.pro.aifitnesstracker.StepCounterService
import com.pro.aifitnesstracker.data.step.DailyStepEntity
import com.pro.aifitnesstracker.data.step.StepAchievementEntity
import com.pro.aifitnesstracker.data.step.StepTrackerRepository
import com.pro.aifitnesstracker.data.step.WalkingSessionEntity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

enum class StepTrackerTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Tracker", Icons.Rounded.DirectionsWalk),
    HISTORY("History", Icons.Rounded.History),
    TRENDS("Trends", Icons.Rounded.BarChart),
    SESSIONS("Sessions", Icons.Rounded.Timeline),
    ACHIEVEMENTS("Badges", Icons.Rounded.EmojiEvents)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepDetailScreen(
    steps: Int,
    goal: Int,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    prefsManager: SharedPrefsManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val repo = remember { StepTrackerRepository.getInstance(context) }

    // Theme palette matching AuraHealth design
    val isDark = MaterialTheme.colorScheme.surface.let {
        (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f
    }
    val screenBg = if (isDark) Color(0xFF080D1A) else Color(0xFFF1F5F9)
    val cardBg = if (isDark) Color(0xFF0F1A2E) else Color(0xFFFFFFFF)
    val cardBorder = BorderStroke(1.dp, if (isDark) Color(0xFF1E3150) else Color(0xFFE2E8F0))
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    val emerald = Color(0xFF10B981)
    val teal = Color(0xFF14B8A6)
    val cyan = Color(0xFF06B6D4)
    val amber = Color(0xFFF59E0B)

    // Tab state
    var selectedTab by remember { mutableStateOf(StepTrackerTab.DASHBOARD) }

    // Date formats
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember { sdf.format(Date()) }
    var viewingDate by remember { mutableStateOf(todayStr) }

    // Live Step state synchronized with Repository & SharedPrefs
    var liveSteps by remember(steps) { mutableIntStateOf(if (steps > 0) steps else prefsManager.getStepsToday()) }
    var currentGoal by remember { mutableIntStateOf(prefsManager.getStepGoal().let { if (it > 0) it else 10000 }) }
    var distanceUnit by remember { mutableStateOf(prefsManager.getStepDistanceUnit()) }

    // Room reactive flows
    val todayRecord by repo.getDailyRecord(todayStr).collectAsState(initial = null)
    val viewingRecord by repo.getDailyRecord(viewingDate).collectAsState(initial = null)
    val todaySessions by repo.getSessionsForDate(todayStr).collectAsState(initial = emptyList())
    val viewingSessions by repo.getSessionsForDate(viewingDate).collectAsState(initial = emptyList())
    val recentSessions by repo.getAllRecentSessions().collectAsState(initial = emptyList())
    val achievements by repo.getAllAchievements().collectAsState(initial = emptyList())

    // Ensure achievements are seeded
    LaunchedEffect(Unit) {
        repo.ensureDefaultAchievements()
        // Sync today record initially
        repo.recordStepEvent(liveSteps, deltaSteps = 0)
    }

    // Keep liveSteps updated when parent steps prop changes
    LaunchedEffect(steps) {
        if (steps > 0) {
            liveSteps = steps
        }
    }

    // Goal celebration banner state
    var showGoalCelebrationModal by remember { mutableStateOf(false) }
    LaunchedEffect(liveSteps, currentGoal) {
        if (liveSteps >= currentGoal && currentGoal > 0) {
            val celebrated = prefsManager.getStepCelebrationDate()
            if (celebrated != todayStr) {
                showGoalCelebrationModal = true
                prefsManager.saveStepCelebrationDate(todayStr)
            }
        }
    }

    // Modals
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showManualSessionDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Permission launcher for ACTIVITY_RECOGNITION
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            snackbarMessage = "Activity Recognition permission granted! 🚶"
            try {
                val serviceIntent = Intent(context, StepCounterService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            snackbarMessage = "Permission denied. Step detection may be restricted."
        }
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    // Title
                    Text(
                        text = "${strings.steps.uppercase(Locale.US)} TRACKER",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Action buttons (Unit toggle + Settings)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Quick Distance Unit Toggle (KM / MI)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val nextUnit = if (distanceUnit.equals("km", true)) "mi" else "km"
                                    distanceUnit = nextUnit
                                    prefsManager.setStepDistanceUnit(nextUnit)
                                    snackbarMessage = "Distance unit switched to ${nextUnit.uppercase()}"
                                }
                        ) {
                            Text(
                                text = distanceUnit.uppercase(),
                                color = emerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Settings button
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Step Settings",
                                tint = textPrimary
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepTrackerTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val itemColor = if (isSelected) emerald else textSecondary
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedTab = tab },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) emerald.copy(alpha = if (isDark) 0.18f else 0.12f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = itemColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = when (tab) {
                                        StepTrackerTab.DASHBOARD -> strings.steps
                                        StepTrackerTab.HISTORY -> strings.history
                                        StepTrackerTab.TRENDS -> strings.analytics
                                        StepTrackerTab.SESSIONS -> tab.title
                                        StepTrackerTab.ACHIEVEMENTS -> strings.achievements.take(8)
                                    },
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    color = itemColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = (-0.2).sp
                                )
                            }
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                StepTrackerTab.DASHBOARD -> {
                    DashboardTabContent(
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        emerald = emerald,
                        cyan = cyan,
                        amber = amber,
                        steps = liveSteps,
                        goal = currentGoal,
                        distanceUnit = distanceUnit,
                        repo = repo,
                        prefsManager = prefsManager,
                        sessions = todaySessions,
                        onStartManualSession = { showManualSessionDialog = true },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
                StepTrackerTab.HISTORY -> {
                    HistoryTabContent(
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        emerald = emerald,
                        distanceUnit = distanceUnit,
                        repo = repo,
                        goal = currentGoal,
                        viewingDate = viewingDate,
                        onDateSelected = { viewingDate = it },
                        dailyRecord = viewingRecord,
                        sessions = viewingSessions
                    )
                }
                StepTrackerTab.TRENDS -> {
                    TrendsTabContent(
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        emerald = emerald,
                        distanceUnit = distanceUnit,
                        repo = repo,
                        goal = currentGoal
                    )
                }
                StepTrackerTab.SESSIONS -> {
                    SessionsTabContent(
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        emerald = emerald,
                        distanceUnit = distanceUnit,
                        repo = repo,
                        sessions = recentSessions,
                        onStartManualWalk = { showManualSessionDialog = true }
                    )
                }
                StepTrackerTab.ACHIEVEMENTS -> {
                    AchievementsTabContent(
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        emerald = emerald,
                        prefsManager = prefsManager,
                        achievements = achievements
                    )
                }
            }

            // Floating Snackbar
            AnimatedVisibility(
                visible = snackbarMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.Black,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, emerald.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = snackbarMessage ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { snackbarMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // ── Dialogs ──

    if (showSettingsDialog) {
        StepSettingsDialog(
            isDark = isDark,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            cardBg = cardBg,
            cardBorder = cardBorder,
            emerald = emerald,
            currentGoal = currentGoal,
            distanceUnit = distanceUnit,
            prefsManager = prefsManager,
            repo = repo,
            onGoalChanged = { newGoal ->
                currentGoal = newGoal
                prefsManager.saveStepGoal(newGoal)
                scope.launch { repo.recordStepEvent(liveSteps, deltaSteps = 0) }
            },
            onUnitChanged = { newUnit ->
                distanceUnit = newUnit
                prefsManager.setStepDistanceUnit(newUnit)
            },
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showManualSessionDialog) {
        ManualSessionDialog(
            isDark = isDark,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            cardBg = cardBg,
            emerald = emerald,
            onAddSession = { stepsCount, durationMin ->
                scope.launch {
                    repo.addManualWalkingSession(stepsCount, durationMin)
                    liveSteps = prefsManager.getStepsToday()
                    snackbarMessage = "Added walk: +$stepsCount steps in $durationMin min! 🚶"
                }
                showManualSessionDialog = false
            },
            onDismiss = { showManualSessionDialog = false }
        )
    }

    if (showGoalCelebrationModal) {
        GoalCelebrationDialog(
            isDark = isDark,
            steps = liveSteps,
            goal = currentGoal,
            emerald = emerald,
            onDismiss = { showGoalCelebrationModal = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1: DASHBOARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardTabContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    emerald: Color,
    cyan: Color,
    amber: Color,
    steps: Int,
    goal: Int,
    distanceUnit: String,
    repo: StepTrackerRepository,
    prefsManager: SharedPrefsManager,
    sessions: List<WalkingSessionEntity>,
    onStartManualSession: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val progressFraction = (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1.5f)
    val progressPct = ((steps.toFloat() / goal.toFloat()) * 100).roundToInt()
    val remainingSteps = (goal - steps).coerceAtLeast(0)

    val distanceMeters = repo.calculateDistanceMeters(steps)
    val formattedDistance = repo.formatDistance(distanceMeters, distanceUnit)
    val estimatedCalories = repo.calculateCalories(steps, distanceMeters)
    val activeSeconds = prefsManager.getActiveSecondsToday()
    val formattedDuration = repo.formatDuration(activeSeconds)

    val sensorStatus = remember { repo.getSensorStatus() }
    val isPaused = remember(prefsManager.isStepTrackingPaused()) { prefsManager.isStepTrackingPaused() }

    // Hourly activity
    val hourlyData = remember(steps) { prefsManager.getHourlyStepsArray() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Circular Step Ring Progress ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(205.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularStepGaugeCanvas(
                            progressFraction = progressFraction.coerceIn(0f, 1f),
                            emerald = emerald,
                            cyan = cyan,
                            isDark = isDark
                        )

                        // Center stats
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = NumberFormat.getNumberInstance(Locale.US).format(steps),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "/ ${NumberFormat.getNumberInstance(Locale.US).format(goal)} Steps",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0)
                            ) {
                                Text(
                                    text = if (steps >= goal) "🌟 GOAL ACHIEVED!" else "$remainingSteps steps left",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (steps >= goal) emerald else Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 2. Stat Summary Cards ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPillarCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    label = "Distance",
                    value = formattedDistance,
                    icon = "🚶",
                    color = emerald
                )
                StatPillarCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    label = "Active Calories",
                    value = "$estimatedCalories kcal",
                    icon = "🔥",
                    color = Color(0xFFEF4444)
                )
                StatPillarCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    label = "Active Time",
                    value = formattedDuration,
                    icon = "⏱️",
                    color = amber
                )
                StatPillarCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    label = "Sessions",
                    value = "${sessions.size}",
                    icon = "👟",
                    color = cyan
                )
            }
        }

        // ── 3. Sensor Diagnostics Pill & Pause Status ──
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF0E1A29) else Color(0xFFEBF3FA),
                border = BorderStroke(1.dp, if (isPaused) Color(0xFFEF4444).copy(alpha = 0.4f) else emerald.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isPaused) Color(0xFFEF4444) else emerald)
                        )
                        Column {
                            Text(
                                text = if (isPaused) "Step Tracking Paused" else sensorStatus.sensorType,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = if (isPaused) "Tap Settings to resume counting" else sensorStatus.description,
                                fontSize = 10.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    TextButton(onClick = onOpenSettings) {
                        Text("Configure", fontSize = 11.5.sp, color = emerald, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── 4. Hourly Activity Distribution Bar Chart ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = cardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S HOURLY CADENCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = textSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Peak: ${hourlyData.maxOrNull() ?: 0} steps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = emerald
                        )
                    }

                    HourlyStepBarChart(
                        hourlyData = hourlyData,
                        barColor = emerald,
                        trackColor = if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0),
                        labelColor = textSecondary
                    )
                }
            }
        }

        // ── 5. Today's Walking Sessions Header & Add Button ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S WALKING SESSIONS (${sessions.size})",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onStartManualSession) {
                    Text("+ Log Walk", color = emerald, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sessions List
        if (sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚶", fontSize = 38.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No Continuous Walks Recorded Yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Walk continuously for over 30 seconds to record automated sessions, or tap '+ Log Walk'.",
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp)
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                WalkingSessionCard(
                    session = session,
                    cardBg = cardBg,
                    border = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = emerald,
                    distanceUnit = distanceUnit,
                    repo = repo
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: HISTORY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryTabContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    emerald: Color,
    distanceUnit: String,
    repo: StepTrackerRepository,
    goal: Int,
    viewingDate: String,
    onDateSelected: (String) -> Unit,
    dailyRecord: DailyStepEntity?,
    sessions: List<WalkingSessionEntity>
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFmt = remember { SimpleDateFormat("EEE, dd MMMM yyyy", Locale.getDefault()) }

    val formattedDateStr = remember(viewingDate) {
        try {
            val parsed = sdf.parse(viewingDate)
            if (parsed != null) displayDateFmt.format(parsed) else viewingDate
        } catch (e: Exception) {
            viewingDate
        }
    }

    val isToday = viewingDate == sdf.format(Date())

    val steps = dailyRecord?.steps ?: 0
    val dayGoal = dailyRecord?.goal ?: goal
    val distanceM = dailyRecord?.distanceMeters ?: repo.calculateDistanceMeters(steps)
    val calories = dailyRecord?.calories ?: repo.calculateCalories(steps, distanceM)
    val activeSec = dailyRecord?.activeSeconds ?: 0

    val progressPct = if (dayGoal > 0) ((steps.toFloat() / dayGoal.toFloat()) * 100).roundToInt() else 0
    val isGoalMet = steps >= dayGoal && dayGoal > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Selector Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                border = cardBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            cal.time = sdf.parse(viewingDate) ?: Date()
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            onDateSelected(sdf.format(cal.time))
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous Day", tint = textPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isToday) {
                            Text("TODAY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = emerald, letterSpacing = 1.sp)
                        }
                        Text(formattedDateStr, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }

                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            cal.time = sdf.parse(viewingDate) ?: Date()
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            val next = sdf.format(cal.time)
                            val today = sdf.format(Date())
                            if (next <= today) {
                                onDateSelected(next)
                            }
                        },
                        enabled = !isToday
                    ) {
                        Icon(
                            Icons.Rounded.ArrowForward,
                            contentDescription = "Next Day",
                            tint = if (isToday) textSecondary.copy(alpha = 0.3f) else textPrimary
                        )
                    }
                }
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = cardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.US).format(steps),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = emerald
                    )
                    Text(
                        text = "Total Steps Recorded",
                        fontSize = 12.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isGoalMet) emerald.copy(alpha = 0.15f) else (if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = if (isGoalMet) "🌟 $progressPct% GOAL ACHIEVED" else "$progressPct% of $dayGoal step goal",
                            color = if (isGoalMet) emerald else textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = if (isDark) Color(0xFF1E3150) else Color(0xFFE2E8F0))
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚶", fontSize = 16.sp)
                            Text(repo.formatDistance(distanceM, distanceUnit), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("Distance", fontSize = 10.sp, color = textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥", fontSize = 16.sp)
                            Text("$calories kcal", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("Calories", fontSize = 10.sp, color = textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⏱️", fontSize = 16.sp)
                            Text(repo.formatDuration(activeSec), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("Active Time", fontSize = 10.sp, color = textSecondary)
                        }
                    }
                }
            }
        }

        // Day's Sessions
        item {
            Text(
                text = "RECORDED WALKS (${sessions.size})",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📬", fontSize = 32.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("No walk sessions recorded on this day.", fontSize = 13.sp, color = textSecondary)
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                WalkingSessionCard(
                    session = session,
                    cardBg = cardBg,
                    border = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = emerald,
                    distanceUnit = distanceUnit,
                    repo = repo
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: TRENDS (Analytics)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrendsTabContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    emerald: Color,
    distanceUnit: String,
    repo: StepTrackerRepository,
    goal: Int
) {
    var period by remember { mutableStateOf("WEEKLY") }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    // Fetch last 7 days
    val weeklyDates = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val startDate = weeklyDates.first()
    val endDate = weeklyDates.last()

    val records by repo.getRecordsInRange(startDate, endDate).collectAsState(initial = emptyList())
    val recordsMap = remember(records) { records.associateBy { it.date } }

    val weeklyData = remember(recordsMap) {
        weeklyDates.map { dateStr ->
            val rec = recordsMap[dateStr]
            val parsed = sdf.parse(dateStr)
            val label = if (parsed != null) dayFormat.format(parsed) else dateStr
            Triple(label, rec?.steps ?: 0, (rec?.steps ?: 0) >= goal)
        }
    }

    val totalWeeklySteps = weeklyData.sumOf { it.second }
    val avgDailySteps = if (weeklyData.isNotEmpty()) totalWeeklySteps / weeklyData.size else 0
    val bestDay = weeklyData.maxByOrNull { it.second }
    val lowestDay = weeklyData.minByOrNull { it.second }
    val daysGoalHit = weeklyData.count { it.third }
    val totalWeeklyDistanceM = repo.calculateDistanceMeters(totalWeeklySteps)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period Toggle (Weekly / Monthly)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf("WEEKLY", "MONTHLY").forEach { p ->
                        val isSelected = period == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) emerald else Color.Transparent)
                                .clickable { period = p }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 7-Day Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = cardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "7-DAY ACTIVITY TREND",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = textSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Daily Steps (Mon - Sun)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                        }
                        Text(
                            text = "Goal: ${NumberFormat.getNumberInstance(Locale.US).format(goal)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = emerald
                        )
                    }

                    WeeklyStepBarChart(
                        days = weeklyData,
                        goal = goal,
                        barColor = emerald,
                        trackColor = if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0),
                        labelColor = textSecondary
                    )
                }
            }
        }

        // Analytics Highlights
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnalyticsStatCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    title = "Daily Average",
                    value = "${NumberFormat.getNumberInstance(Locale.US).format(avgDailySteps)}",
                    subtitle = "steps / day",
                    icon = "📊",
                    accent = emerald
                )
                AnalyticsStatCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    title = "Best Day",
                    value = bestDay?.first ?: "-",
                    subtitle = "${NumberFormat.getNumberInstance(Locale.US).format(bestDay?.second ?: 0)} steps",
                    icon = "🏆",
                    accent = Color(0xFFF59E0B)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnalyticsStatCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    title = "Total Distance",
                    value = repo.formatDistance(totalWeeklyDistanceM, distanceUnit),
                    subtitle = "covered this week",
                    icon = "🚶",
                    accent = Color(0xFF06B6D4)
                )
                AnalyticsStatCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    title = "Goal Success",
                    value = "$daysGoalHit / 7 Days",
                    subtitle = "${(daysGoalHit * 100 / 7)}% achievement",
                    icon = "🎯",
                    accent = Color(0xFF10B981)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 4: SESSIONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SessionsTabContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    emerald: Color,
    distanceUnit: String,
    repo: StepTrackerRepository,
    sessions: List<WalkingSessionEntity>,
    onStartManualWalk: () -> Unit
) {
    val longestSession = remember(sessions) { sessions.maxByOrNull { it.durationSeconds } }
    val totalSeconds = remember(sessions) { sessions.sumOf { it.durationSeconds } }
    val avgSeconds = remember(sessions) { if (sessions.isNotEmpty()) totalSeconds / sessions.size else 0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = cardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WALKING SESSIONS OVERVIEW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = textSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${sessions.size} Recorded Sessions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                        }
                        Button(
                            onClick = onStartManualWalk,
                            colors = ButtonDefaults.buttonColors(containerColor = emerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Add Walk", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("⏱️ Longest Walk", fontSize = 11.sp, color = textSecondary)
                            Text(
                                text = if (longestSession != null) repo.formatDuration(longestSession.durationSeconds) else "0 min",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Column {
                            Text("📊 Average Duration", fontSize = 11.sp, color = textSecondary)
                            Text(
                                text = repo.formatDuration(avgSeconds),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Column {
                            Text("👟 Total Walks", fontSize = 11.sp, color = textSecondary)
                            Text(
                                text = "${sessions.size}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = emerald
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "RECENT WALKING SESSIONS",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏃", fontSize = 38.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("No walking sessions saved yet.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text(
                            "Sensor automatically records sessions during continuous walking.",
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                WalkingSessionCard(
                    session = session,
                    cardBg = cardBg,
                    border = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = emerald,
                    distanceUnit = distanceUnit,
                    repo = repo
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 5: ACHIEVEMENTS & BADGES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AchievementsTabContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    emerald: Color,
    prefsManager: SharedPrefsManager,
    achievements: List<StepAchievementEntity>
) {
    val currentStreak = remember { prefsManager.getStepStreak() }
    val bestStreak = remember { prefsManager.getStepBestStreak() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Streak Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = cardBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔥", fontSize = 24.sp)
                            Text("$currentStreak Days", fontSize = 24.sp, fontWeight = FontWeight.Black, color = textPrimary)
                        }
                        Text("Current Streak", fontSize = 11.5.sp, color = textSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(if (isDark) Color(0xFF1E3150) else Color(0xFFE2E8F0))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("👑", fontSize = 24.sp)
                            Text("$bestStreak Days", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                        }
                        Text("Personal Record", fontSize = 11.5.sp, color = textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Text(
                text = "STEP MILESTONES (${achievements.count { it.isUnlocked }}/${achievements.size} UNLOCKED)",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        items(achievements, key = { it.achievementId }) { item ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                border = if (item.isUnlocked) BorderStroke(1.dp, emerald.copy(alpha = 0.5f)) else cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isUnlocked) emerald.copy(alpha = 0.18f)
                                else (if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.icon, fontSize = 24.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                        if (item.isUnlocked && item.unlockedDate != null) {
                            Text(
                                text = "Unlocked on ${item.unlockedDate}",
                                fontSize = 10.sp,
                                color = emerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (item.isUnlocked) emerald.copy(alpha = 0.15f) else (if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = if (item.isUnlocked) "UNLOCKED" else "LOCKED",
                            color = if (item.isUnlocked) emerald else textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS & CANVASES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CircularStepGaugeCanvas(
    progressFraction: Float,
    emerald: Color,
    cyan: Color,
    isDark: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "stepProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val diameter = size.minDimension
        val strokeWidth = 14.dp.toPx()
        val radius = (diameter - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Background track
        drawCircle(
            color = if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Soft outer glow arc
        if (animatedProgress > 0f) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(emerald.copy(alpha = 0.35f), cyan.copy(alpha = 0.35f), emerald.copy(alpha = 0.35f))
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth * 1.4f, cap = StrokeCap.Round)
            )

            // Main Gradient Progress Arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(emerald, cyan, emerald)
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun StatPillarCard(
    modifier: Modifier = Modifier,
    cardBg: Color,
    border: BorderStroke,
    label: String,
    value: String,
    icon: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 9.5.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun WalkingSessionCard(
    session: WalkingSessionEntity,
    cardBg: Color,
    border: BorderStroke,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    distanceUnit: String,
    repo: StepTrackerRepository
) {
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val startTimeStr = remember(session.startTimeMs) { timeFmt.format(Date(session.startTimeMs)) }
    val endTimeStr = remember(session.endTimeMs) { timeFmt.format(Date(session.endTimeMs)) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🚶", fontSize = 16.sp)
                    Text(
                        text = "$startTimeStr - $endTimeStr",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = repo.formatDuration(session.durationSeconds),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${NumberFormat.getNumberInstance(Locale.US).format(session.stepsCount)} steps",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
                Text(
                    text = repo.formatDistance(session.distanceMeters, distanceUnit),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
                Text(
                    text = "${session.calories} kcal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
private fun HourlyStepBarChart(
    hourlyData: IntArray,
    barColor: Color,
    trackColor: Color,
    labelColor: Color
) {
    val maxSteps = remember(hourlyData) { (hourlyData.maxOrNull() ?: 1).coerceAtLeast(100) }
    val timeSlots = listOf(6, 8, 10, 12, 14, 16, 18, 20, 22)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        timeSlots.forEach { hour ->
            val count = hourlyData.getOrElse(hour) { 0 }
            val fraction = (count.toFloat() / maxSteps.toFloat()).coerceIn(0.08f, 1f)
            val label = when (hour) {
                12 -> "12p"
                in 1..11 -> "${hour}a"
                else -> "${hour - 12}p"
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height((fraction * 75).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (count > 0) barColor else trackColor)
                )
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 9.sp, color = labelColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WeeklyStepBarChart(
    days: List<Triple<String, Int, Boolean>>,
    goal: Int,
    barColor: Color,
    trackColor: Color,
    labelColor: Color
) {
    val maxSteps = remember(days, goal) {
        val maxVal = days.maxOfOrNull { it.second } ?: goal
        maxVal.coerceAtLeast(goal).coerceAtLeast(1000)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { (dayLabel, steps, isMet) ->
            val fraction = (steps.toFloat() / maxSteps.toFloat()).coerceIn(0.06f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (steps >= 1000) "${(steps / 1000f).roundToInt()}k" else "$steps",
                    fontSize = 8.5.sp,
                    color = if (isMet) barColor else labelColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height((fraction * 80).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMet) barColor else if (steps > 0) barColor.copy(alpha = 0.5f) else trackColor)
                )
                Spacer(Modifier.height(6.dp))
                Text(dayLabel, fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    modifier: Modifier = Modifier,
    cardBg: Color,
    border: BorderStroke,
    title: String,
    value: String,
    subtitle: String,
    icon: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Text(icon, fontSize = 15.sp)
            }
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = accent)
            Text(subtitle, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOGS & SETTINGS
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepSettingsDialog(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    emerald: Color,
    currentGoal: Int,
    distanceUnit: String,
    prefsManager: SharedPrefsManager,
    repo: StepTrackerRepository,
    onGoalChanged: (Int) -> Unit,
    onUnitChanged: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var goalVal by remember { mutableIntStateOf(currentGoal) }
    var weightStr by remember { mutableStateOf(prefsManager.getWeight().let { if (it > 0) it.toInt().toString() else "70" }) }
    var heightStr by remember { mutableStateOf(prefsManager.getHeight().let { if (it > 0) it.toInt().toString() else "175" }) }
    var customStrideStr by remember { mutableStateOf(prefsManager.getCustomStepLengthCm().toInt().toString()) }
    var customStrideEnabled by remember { mutableStateOf(prefsManager.isCustomStepLengthEnabled()) }
    var unitVal by remember { mutableStateOf(distanceUnit) }
    var isTrackingPaused by remember { mutableStateOf(prefsManager.isStepTrackingPaused()) }

    val sensorStatus = remember { repo.getSensorStatus() }
    val effectiveTextSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val sheetBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val itemCardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val itemBorder = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(emerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = emerald,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Step Tracker Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Targets, hardware sensor & metrics",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = effectiveTextSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // CARD 1: DAILY STEP GOAL
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = itemCardBg),
                        border = itemBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎯", fontSize = 16.sp)
                                    Text(
                                        "DAILY STEP GOAL",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = textPrimary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = emerald.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = when {
                                            goalVal < 5000 -> "Light"
                                            goalVal <= 8000 -> "Active"
                                            goalVal <= 10000 -> "Optimal"
                                            else -> "Pro"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = emerald,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Stepper Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(0xFF0F1A2E) else Color(0xFFF1F5F9))
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { goalVal = (goalVal - 500).coerceAtLeast(1000) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF1E293B) else Color.White)
                                        .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "-500", tint = textPrimary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = NumberFormat.getNumberInstance(Locale.US).format(goalVal),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "steps / day",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = effectiveTextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { goalVal = (goalVal + 500).coerceAtMost(50000) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF1E293B) else Color.White)
                                        .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "+500", tint = emerald)
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Quick Target Presets - 2 balanced rows of 3 buttons
                            Text(
                                text = "QUICK TARGET PRESETS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = effectiveTextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(3000, 5000, 8000).forEach { preset ->
                                    val isSelected = goalVal == preset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) emerald else (if (isDark) Color(0xFF0F1A2E) else Color(0xFFF1F5F9))
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) emerald else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { goalVal = preset }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${preset / 1000}k",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSelected) Color.White else textPrimary
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(10000, 12000, 15000).forEach { preset ->
                                    val isSelected = goalVal == preset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) emerald else (if (isDark) Color(0xFF0F1A2E) else Color(0xFFF1F5F9))
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) emerald else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { goalVal = preset }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${preset / 1000}k",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSelected) Color.White else textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // CARD 2: DISTANCE UNIT
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = itemCardBg),
                        border = itemBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📏", fontSize = 16.sp)
                                Text(
                                    "DISTANCE MEASUREMENT UNIT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = textPrimary
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color(0xFF0F1A2E) else Color(0xFFF1F5F9))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("km" to "Kilometers (km)", "mi" to "Miles (mi)").forEach { (code, name) ->
                                    val isSel = unitVal.equals(code, true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) emerald else Color.Transparent)
                                            .clickable { unitVal = code }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // CARD 3: BODY METRICS
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = itemCardBg),
                        border = itemBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("⚖️", fontSize = 16.sp)
                                Text(
                                    "BODY METRICS (CALORIES & STRIDE)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = textPrimary
                                )
                            }
                            Text(
                                text = "Personal stats improve calorie burn accuracy and stride calculation.",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = effectiveTextSecondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = weightStr,
                                    onValueChange = { weightStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    label = { Text("Weight (kg)", fontSize = 11.sp, color = textPrimary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary,
                                        focusedBorderColor = emerald,
                                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = heightStr,
                                    onValueChange = { heightStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    label = { Text("Height (cm)", fontSize = 11.sp, color = textPrimary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary,
                                        focusedBorderColor = emerald,
                                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // CARD 4: STRIDE LENGTH
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = itemCardBg),
                        border = itemBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏃", fontSize = 16.sp)
                                    Column {
                                        Text(
                                            "STRIDE LENGTH",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = textPrimary
                                        )
                                        Text(
                                            "Custom distance per step",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = effectiveTextSecondary
                                        )
                                    }
                                }
                                Switch(
                                    checked = customStrideEnabled,
                                    onCheckedChange = { customStrideEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = emerald
                                    )
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            if (customStrideEnabled) {
                                OutlinedTextField(
                                    value = customStrideStr,
                                    onValueChange = { customStrideStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    label = { Text("Custom Stride Length (cm)", fontSize = 11.sp, color = textPrimary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary,
                                        focusedBorderColor = emerald,
                                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            } else {
                                val estStride = ((heightStr.toFloatOrNull() ?: 175f) * 0.414f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF0F1A2E) else Color(0xFFF1F5F9))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Auto-estimated from height:", fontSize = 12.sp, color = textPrimary)
                                        Text(
                                            String.format(Locale.US, "%.1f cm", estStride),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = emerald
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // CARD 5: SENSOR HARDWARE DIAGNOSTICS
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = itemCardBg),
                        border = itemBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("⚡", fontSize = 16.sp)
                                    Text(
                                        "HARDWARE SENSOR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = textPrimary,
                                        maxLines = 1
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = emerald.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = if (sensorStatus.isHardwareSensor) "ONLINE" else "FALLBACK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = emerald,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color(0xFF0F1A2E) else Color(0xFFF1F5F9))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Sensor Type", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = effectiveTextSecondary)
                                        Text(sensorStatus.sensorType, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Hardware", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = effectiveTextSecondary)
                                        Text(sensorStatus.sensorName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Vendor", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = effectiveTextSecondary)
                                        Text(sensorStatus.vendor, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    }
                                    HorizontalDivider(
                                        color = if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFCBD5E1),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = emerald.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "STATUS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = emerald,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = sensorStatus.description,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = effectiveTextSecondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // CARD 6: TRACKING PAUSE / RESUME & PERMISSION
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = itemCardBg),
                        border = itemBorder
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pause Step Counting", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    Text("Temporarily halt background sensor counting", fontSize = 11.sp, fontWeight = FontWeight.Normal, color = effectiveTextSecondary)
                                }
                                Switch(
                                    checked = isTrackingPaused,
                                    onCheckedChange = {
                                        isTrackingPaused = it
                                        prefsManager.setStepTrackingPaused(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFEF4444)
                                    )
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onRequestPermission,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = emerald, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Verify Activity Recognition Permission", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            }
                        }
                    }
                }
            }

            // Bottom Sticky Save Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }

                    Button(
                        onClick = {
                            onGoalChanged(goalVal)
                            onUnitChanged(unitVal)
                            val w = weightStr.toFloatOrNull() ?: 70f
                            val h = heightStr.toFloatOrNull() ?: 175f
                            prefsManager.saveWeight(w)
                            prefsManager.saveHeight(h)
                            prefsManager.setCustomStepLengthEnabled(customStrideEnabled)
                            customStrideStr.toFloatOrNull()?.let { prefsManager.setCustomStepLengthCm(it) }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = emerald),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(2f)
                            .height(52.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualSessionDialog(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    emerald: Color,
    onAddSession: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var stepsInput by remember { mutableStateOf("1500") }
    var durationInput by remember { mutableStateOf("15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Log Walking Session", fontSize = 18.sp, fontWeight = FontWeight.Black, color = textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Record a dedicated walking workout or manual session.", fontSize = 12.sp, color = textSecondary)
                OutlinedTextField(
                    value = stepsInput,
                    onValueChange = { stepsInput = it },
                    label = { Text("Steps Walked") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = durationInput,
                    onValueChange = { durationInput = it },
                    label = { Text("Duration (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = stepsInput.toIntOrNull() ?: 1000
                    val d = durationInput.toIntOrNull() ?: 15
                    onAddSession(s, d)
                },
                colors = ButtonDefaults.buttonColors(containerColor = emerald)
            ) {
                Text("Save Walk", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondary)
            }
        }
    )
}

@Composable
private fun GoalCelebrationDialog(
    isDark: Boolean,
    steps: Int,
    goal: Int,
    emerald: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("🎉", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Daily Step Goal Achieved!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Incredible work! You reached ${NumberFormat.getNumberInstance(Locale.US).format(steps)} steps today, conquering your daily goal of ${NumberFormat.getNumberInstance(Locale.US).format(goal)} steps.",
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = emerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Keep It Up! 🚀", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}
