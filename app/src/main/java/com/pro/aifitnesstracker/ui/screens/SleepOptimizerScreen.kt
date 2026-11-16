package com.pro.aifitnesstracker.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import com.pro.aifitnesstracker.*
import com.pro.aifitnesstracker.data.sleep.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

data class SleepPalette(
    val isDark: Boolean,
    val screenBg: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val cardSecondaryBg: Color,
    val sleepPurple: Color,
    val sleepIndigo: Color,
    val sleepCyan: Color,
    val sleepGold: Color,
    val sleepGreen: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

val LocalSleepPalette = staticCompositionLocalOf {
    SleepPalette(
        isDark = true,
        screenBg = Color(0xFF070B19),
        cardBg = Color(0xFF10172A),
        cardBorder = Color(0xFF1E293B),
        cardSecondaryBg = Color(0xFF1E293B),
        sleepPurple = Color(0xFF8B5CF6),
        sleepIndigo = Color(0xFF6366F1),
        sleepCyan = Color(0xFF38BDF8),
        sleepGold = Color(0xFFFBBF24),
        sleepGreen = Color(0xFF10B981),
        textPrimary = Color(0xFFF1F5F9),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF64748B)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepOptimizerScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    prefsManager: SharedPrefsManager,
    repository: HealthRepository
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val db = remember { AppDatabase.getDatabase(context) }
    val hcManager = remember { HealthConnectManager(context) }
    val sleepRepo = remember {
        SleepRepository(
            sleepDao = db.sleepTrackerDao(),
            healthDao = db.healthDao(),
            prefsManager = prefsManager,
            healthConnectManager = hcManager,
            context = context
        )
    }

    val audioManager = remember { SleepAudioManager.getInstance() }

    // State collections
    val allSessions by sleepRepo.allSessions.collectAsState(initial = emptyList())
    val schedule by sleepRepo.sleepSchedule.collectAsState(initial = SleepScheduleEntity())
    val activeState by sleepRepo.activeSleepState.collectAsState()
    val achievements by sleepRepo.achievements.collectAsState(initial = emptyList())

    val isAudioPlaying by audioManager.isPlaying.collectAsState()
    val currentSound by audioManager.currentSound.collectAsState()
    val soundVolume by audioManager.volume.collectAsState()
    val timerRemaining by audioManager.timerRemainingSeconds.collectAsState()

    // Screen Tabs
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(strings.home, "Schedule", strings.history, strings.analytics, strings.breathing)

    // Dialogs
    var showManualAddDialog by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<SleepSessionEntity?>(null) }
    var sessionToDelete by remember { mutableStateOf<SleepSessionEntity?>(null) }
    var showWakeUpSummaryDialog by remember { mutableStateOf(false) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var showHealthConnectDialog by remember { mutableStateOf(false) }

    // Calculated analytics and streak
    val analytics = remember(allSessions, schedule) {
        sleepRepo.calculateAnalytics(allSessions, schedule)
    }
    val streak = remember(allSessions, schedule) {
        sleepRepo.calculateStreak(allSessions, schedule.sleepGoalHours)
    }
    val recommendations = remember(analytics, schedule, allSessions) {
        sleepRepo.generateRecommendations(analytics, schedule, allSessions)
    }

    // Dynamic Sleep Palette adapting to App Theme (Light Clean vs Dark)
    val currentTheme = prefsManager.getAppTheme()
    val isDark = currentTheme != "LightClean" && currentTheme != "LightMode" && currentTheme != "Light" &&
            (MaterialTheme.colorScheme.surface.luminance() < 0.5f)

    val palette = remember(isDark) {
        if (isDark) {
            SleepPalette(
                isDark = true,
                screenBg = Color(0xFF070B19),
                cardBg = Color(0xFF10172A),
                cardBorder = Color(0xFF1E293B),
                cardSecondaryBg = Color(0xFF1E293B),
                sleepPurple = Color(0xFF8B5CF6),
                sleepIndigo = Color(0xFF6366F1),
                sleepCyan = Color(0xFF38BDF8),
                sleepGold = Color(0xFFFBBF24),
                sleepGreen = Color(0xFF10B981),
                textPrimary = Color(0xFFF1F5F9),
                textSecondary = Color(0xFF94A3B8),
                textMuted = Color(0xFF64748B)
            )
        } else {
            SleepPalette(
                isDark = false,
                screenBg = Color(0xFFF4F7FB),
                cardBg = Color(0xFFFFFFFF),
                cardBorder = Color(0xFFE2E8F0),
                cardSecondaryBg = Color(0xFFF1F5F9),
                sleepPurple = Color(0xFF6366F1),
                sleepIndigo = Color(0xFF4F46E5),
                sleepCyan = Color(0xFF0284C7),
                sleepGold = Color(0xFFD97706),
                sleepGreen = Color(0xFF059669),
                textPrimary = Color.Black,
                textSecondary = Color.Black,
                textMuted = Color.Black
            )
        }
    }

    val nightBg = palette.screenBg
    val cardBg = palette.cardBg
    val cardBorder = palette.cardBorder
    val sleepIndigo = palette.sleepIndigo
    val sleepPurple = palette.sleepPurple
    val sleepCyan = palette.sleepCyan
    val sleepGold = palette.sleepGold
    val textPrimary = palette.textPrimary
    val textSecondary = palette.textSecondary

    CompositionLocalProvider(LocalSleepPalette provides palette) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.sleepOptimizer.uppercase(Locale.US),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    // Achievements Trophy
                    IconButton(onClick = { showAchievementsDialog = true }) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            contentDescription = "Achievements",
                            tint = sleepGold
                        )
                    }
                    // Health Connect Sync
                    IconButton(onClick = { showHealthConnectDialog = true }) {
                        Icon(
                            Icons.Rounded.Sync,
                            contentDescription = "Sync Health Connect",
                            tint = sleepCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = nightBg
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, if (palette.isDark) Color(0xFF2E2452) else Color(0xFFE2E8F0))
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
                        val navItems = listOf(
                            Triple("Sleep", Icons.Rounded.Bedtime, 0),
                            Triple("Schedule", Icons.Rounded.Alarm, 1),
                            Triple("History", Icons.Rounded.History, 2),
                            Triple("Trends", Icons.Rounded.BarChart, 3),
                            Triple("Sounds", Icons.Rounded.Spa, 4)
                        )
                        navItems.forEach { (label, icon, index) ->
                            val isSelected = selectedTab == index
                            val itemColor = if (isSelected) sleepPurple else textSecondary
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedTab = index },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) sleepPurple.copy(alpha = if (palette.isDark) 0.18f else 0.12f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = itemColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = label,
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = nightBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main Tab Content
            when (selectedTab) {
                0 -> SleepDashboardTab(
                    analytics = analytics,
                    streak = streak,
                    schedule = schedule,
                    activeState = activeState,
                    latestSession = allSessions.firstOrNull(),
                    recommendations = recommendations,
                    isAudioPlaying = isAudioPlaying,
                    currentSound = currentSound,
                    onStartSleep = { sleepRepo.startSleep() },
                    onWakeUp = { showWakeUpSummaryDialog = true },
                    onCancelSleep = { sleepRepo.cancelActiveSleep() },
                    onOpenSchedule = { selectedTab = 1 },
                    onOpenHistory = { selectedTab = 2 },
                    onOpenRelaxation = { selectedTab = 4 },
                    onManualAdd = { showManualAddDialog = true },
                    onQuickPlaySound = { sound -> audioManager.play(sound) }
                )
                1 -> SleepScheduleTab(
                    schedule = schedule,
                    onSaveSchedule = { updated ->
                        coroutineScope.launch {
                            sleepRepo.saveSchedule(updated)
                            Toast.makeText(context, "Sleep Schedule Saved & Alarms Updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                2 -> SleepHistoryTab(
                    sessions = allSessions,
                    schedule = schedule,
                    onAddRecord = { showManualAddDialog = true },
                    onEditRecord = { sessionToEdit = it },
                    onDeleteRecord = { sessionToDelete = it }
                )
                3 -> SleepAnalyticsTab(
                    analytics = analytics,
                    streak = streak,
                    schedule = schedule,
                    recommendations = recommendations
                )
                4 -> SleepRelaxationTab(
                    audioManager = audioManager,
                    isPlaying = isAudioPlaying,
                    currentSound = currentSound,
                    volume = soundVolume,
                    timerRemaining = timerRemaining
                )
            }
        }
    }

    // --- MODALS & DIALOGS ---

    // 1. Wake Up Summary Rating Dialog
    if (showWakeUpSummaryDialog) {
        WakeUpRatingDialog(
            activeState = activeState,
            onDismiss = { showWakeUpSummaryDialog = false },
            onSave = { quality, awakenings, latency, notes ->
                coroutineScope.launch {
                    val session = sleepRepo.wakeUp(
                        wakeTimeMs = System.currentTimeMillis(),
                        qualityRating = quality,
                        awakenings = awakenings,
                        latencyMinutes = latency,
                        notes = notes
                    )
                    showWakeUpSummaryDialog = false
                    Toast.makeText(context, "Sleep Session Saved: ${session.durationHours}h (${session.goalCompletionPercent}% goal)", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // 2. Manual Add Sleep Session Dialog
    if (showManualAddDialog) {
        ManualAddSleepDialog(
            defaultGoal = schedule.sleepGoalHours,
            onDismiss = { showManualAddDialog = false },
            onSave = { dateStr, bedMs, wakeMs, quality, latency, awakenings, notes ->
                coroutineScope.launch {
                    val session = sleepRepo.addManualSleepSession(
                        dateStr = dateStr,
                        bedTimeMs = bedMs,
                        wakeTimeMs = wakeMs,
                        qualityRating = quality,
                        latencyMinutes = latency,
                        awakenings = awakenings,
                        notes = notes
                    )
                    showManualAddDialog = false
                    Toast.makeText(context, "Added: ${session.durationHours}h for ${session.date}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 3. Edit Sleep Session Dialog
    sessionToEdit?.let { session ->
        EditSleepDialog(
            session = session,
            onDismiss = { sessionToEdit = null },
            onSave = { updated ->
                coroutineScope.launch {
                    sleepRepo.updateSleepSession(updated)
                    sessionToEdit = null
                    Toast.makeText(context, "Sleep Record Updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 4. Delete Confirmation Dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Sleep Record?", color = textPrimary) },
            text = {
                Text(
                    "Are you sure you want to permanently delete the sleep session of ${session.durationHours}h on ${session.date}?",
                    color = textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            sleepRepo.deleteSleepSession(session)
                            sessionToDelete = null
                            Toast.makeText(context, "Sleep record deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg
        )
    }

    // 5. Achievements Dialog
    if (showAchievementsDialog) {
        SleepAchievementsDialog(
            achievements = achievements,
            streak = streak,
            onDismiss = { showAchievementsDialog = false }
        )
    }

    // 6. Health Connect Sync Dialog
    if (showHealthConnectDialog) {
        HealthConnectSyncDialog(
            hcManager = hcManager,
            onDismiss = { showHealthConnectDialog = false },
            onSyncNow = {
                coroutineScope.launch {
                    val count = sleepRepo.syncHealthConnectSleep()
                    Toast.makeText(context, "Health Connect Sync: $count new sleep sessions imported", Toast.LENGTH_LONG).show()
                    showHealthConnectDialog = false
                }
            }
        )
    }
    }
}

// ==========================================
// 1. SLEEP DASHBOARD TAB
// ==========================================
@Composable
fun SleepDashboardTab(
    analytics: SleepAnalyticsSummary,
    streak: SleepStreakSummary,
    schedule: SleepScheduleEntity,
    activeState: ActiveSleepState,
    latestSession: SleepSessionEntity?,
    recommendations: List<SleepRecommendation>,
    isAudioPlaying: Boolean,
    currentSound: SleepSoundType?,
    onStartSleep: () -> Unit,
    onWakeUp: () -> Unit,
    onCancelSleep: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRelaxation: () -> Unit,
    onManualAdd: () -> Unit,
    onQuickPlaySound: (SleepSoundType) -> Unit
) {
    val palette = LocalSleepPalette.current
    val cardBg = palette.cardBg
    val cardBorder = palette.cardBorder
    val sleepPurple = palette.sleepPurple
    val sleepIndigo = palette.sleepIndigo
    val sleepCyan = palette.sleepCyan
    val sleepGreen = palette.sleepGreen
    val sleepGold = palette.sleepGold
    val textPrimary = palette.textPrimary
    val textSecondary = palette.textSecondary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. HERO ACTIVE TRACKING CARD ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.2.dp, if (activeState.isTracking) sleepPurple else cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (activeState.isTracking) {
                        // Pulsating Sleeping Orb Animation
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.94f,
                            targetValue = 1.06f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(sleepPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((64 * scale).dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(sleepPurple, sleepIndigo)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NightlightRound,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        val startTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(activeState.startTimeMs))
                        val elapsedMinutes = ((System.currentTimeMillis() - activeState.startTimeMs) / 60_000L).coerceAtLeast(0)
                        val elapsedH = elapsedMinutes / 60
                        val elapsedM = elapsedMinutes % 60

                        Text(
                            text = "SLEEP TRACKING ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = sleepPurple,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Resting since $startTimeStr",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Elapsed: ${elapsedH}h ${elapsedM}m",
                            fontSize = 13.sp,
                            color = textSecondary
                        )

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onWakeUp,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = sleepGold)
                            ) {
                                Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Wake Up ☀️", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onCancelSleep,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                            ) {
                                Text("Cancel", color = Color(0xFFEF4444), fontSize = 12.sp)
                            }
                        }
                    } else {
                        // IDLE STATE: Circular Sleep Gauge & Start Sleep CTA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(sleepPurple)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "LAST NIGHT SLEEP",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = sleepPurple,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "${analytics.todayHours}h",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Goal: ${analytics.todayGoalHours}h (${analytics.todayCompletionPercent.toInt()}%)",
                                    fontSize = 12.sp,
                                    color = if (analytics.todayHours >= analytics.todayGoalHours) sleepGreen else textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Modern Circular Sleep Ring Gauge
                            val targetCompletion = analytics.todayCompletionPercent.coerceIn(0f, 200f).toInt()
                            val progressFraction = (analytics.todayHours / analytics.todayGoalHours.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                            val animatedProgress by animateFloatAsState(
                                targetValue = progressFraction,
                                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                label = "sleep_ring"
                            )

                            Box(
                                modifier = Modifier.size(96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(86.dp)) {
                                    val strokeWidth = 7.5.dp.toPx()
                                    // Clean subtle background track
                                    drawArc(
                                        color = if (palette.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                    )
                                    // Glowing Gradient Arc
                                    if (animatedProgress > 0f) {
                                        drawArc(
                                            brush = Brush.sweepGradient(
                                                listOf(
                                                    Color(0xFF6366F1),
                                                    Color(0xFF8B5CF6),
                                                    Color(0xFF06B6D4),
                                                    Color(0xFF6366F1)
                                                )
                                            ),
                                            startAngle = -90f,
                                            sweepAngle = animatedProgress * 360f,
                                            useCenter = false,
                                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$targetCompletion%",
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = textPrimary,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(Modifier.height(1.dp))
                                    Text(
                                        text = if (targetCompletion >= 100) "ACHIEVED" else "OF GOAL",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (targetCompletion >= 100) sleepGreen else sleepPurple,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Start Sleep & Log Manually Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Start Sleep Button - Clean White Card with NO icons
                            Surface(
                                onClick = onStartSleep,
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = if (palette.isDark) Color(0xFF1E293B) else Color.White,
                                border = BorderStroke(1.5.dp, if (palette.isDark) sleepIndigo.copy(alpha = 0.6f) else Color(0xFF6366F1)),
                                shadowElevation = if (palette.isDark) 0.dp else 2.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Start Sleep",
                                        color = if (palette.isDark) Color.White else Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }

                            // Log Entry Button
                            OutlinedButton(
                                onClick = onManualAdd,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (palette.isDark) Color(0xFF10172A) else Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = sleepCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Log Entry", color = textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // --- 2. THREE PILLARS: Bedtime, Wake-Up, Streak ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bedtime Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Bedtime", fontSize = 11.sp, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = analytics.lastNightBedtime,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = sleepPurple
                        )
                    }
                }

                // Wake-Up Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Wake-up", fontSize = 11.sp, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = analytics.lastNightWakeTime,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = sleepGold
                        )
                    }
                }

                // Streak Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Sleep Streak", fontSize = 11.sp, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "🔥 ${streak.currentStreak} Days",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = sleepGreen
                        )
                    }
                }
            }
        }

        // --- 3. SLEEP SCHEDULE CARD ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSchedule() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Configured Sleep Schedule",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val bedStr = formatHourMinute(schedule.weekdayBedtimeHour, schedule.weekdayBedtimeMinute)
                        val wakeStr = formatHourMinute(schedule.weekdayWakeupHour, schedule.weekdayWakeupMinute)
                        Text(
                            text = "Weekdays: $bedStr - $wakeStr (${schedule.sleepGoalHours}h target)",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Edit Schedule",
                        tint = sleepPurple
                    )
                }
            }
        }

        // --- 4. WEEKLY SLEEP BAR PREVIEW ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "7-Day Sleep Pattern",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Avg: ${analytics.weeklyAvgHours}h / night",
                            fontSize = 12.sp,
                            color = sleepCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // 7 Bar Rows
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxTarget = (schedule.sleepGoalHours * 1.3f).coerceAtLeast(10f)
                        analytics.weeklyDays.forEach { day ->
                            val barHeightFraction = (day.durationHours / maxTarget).coerceIn(0.08f, 1f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (day.durationHours > 0f) {
                                    Text(
                                        text = "${day.durationHours}h",
                                        fontSize = 9.sp,
                                        color = if (day.isGoalMet) sleepGreen else textSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .fillMaxHeight(barHeightFraction)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (day.isGoalMet) Brush.verticalGradient(listOf(sleepPurple, sleepIndigo))
                                            else if (day.durationHours > 0f) Brush.verticalGradient(listOf(sleepCyan, cardBorder))
                                            else Brush.verticalGradient(listOf(cardBorder, cardBorder))
                                        )
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = day.dayLabel,
                                    fontSize = 11.sp,
                                    color = if (day.isGoalMet) textPrimary else textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. RELAXATION SOUNDS QUICK ROW ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎧", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Relaxation Soundscapes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Text(
                            text = "View All",
                            fontSize = 12.sp,
                            color = sleepPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onOpenRelaxation() }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(SleepSoundType.values()) { sound ->
                            val isThisPlaying = isAudioPlaying && currentSound == sound
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isThisPlaying) sleepPurple.copy(alpha = if (palette.isDark) 0.25f else 0.12f) else palette.cardSecondaryBg,
                                border = BorderStroke(1.dp, if (isThisPlaying) sleepPurple else palette.cardBorder),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .clickable { onQuickPlaySound(sound) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sound.iconEmoji, fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = sound.title,
                                        fontSize = 12.sp,
                                        color = if (isThisPlaying) sleepPurple else textPrimary,
                                        fontWeight = if (isThisPlaying) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isThisPlaying) {
                                        Spacer(Modifier.width(6.dp))
                                        Text("▶", fontSize = 10.sp, color = sleepPurple)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 6. RECOMMENDATION HIGHLIGHT ---
        if (recommendations.isNotEmpty()) {
            item {
                val rec = recommendations.first()
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, sleepIndigo.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(rec.iconEmoji, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = rec.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = rec.message,
                                fontSize = 12.sp,
                                color = textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. SLEEP SCHEDULE TAB
// ==========================================
@Composable
fun SleepScheduleTab(
    schedule: SleepScheduleEntity,
    onSaveSchedule: (SleepScheduleEntity) -> Unit
) {
    val context = LocalContext.current
    var goalHours by remember(schedule) { mutableFloatStateOf(schedule.sleepGoalHours) }

    var weekdayBedHour by remember(schedule) { mutableIntStateOf(schedule.weekdayBedtimeHour) }
    var weekdayBedMinute by remember(schedule) { mutableIntStateOf(schedule.weekdayBedtimeMinute) }
    var weekdayWakeHour by remember(schedule) { mutableIntStateOf(schedule.weekdayWakeupHour) }
    var weekdayWakeMinute by remember(schedule) { mutableIntStateOf(schedule.weekdayWakeupMinute) }

    var weekendBedHour by remember(schedule) { mutableIntStateOf(schedule.weekendBedtimeHour) }
    var weekendBedMinute by remember(schedule) { mutableIntStateOf(schedule.weekendBedtimeMinute) }
    var weekendWakeHour by remember(schedule) { mutableIntStateOf(schedule.weekendWakeupHour) }
    var weekendWakeMinute by remember(schedule) { mutableIntStateOf(schedule.weekendWakeupMinute) }

    var isWeekendDifferent by remember(schedule) { mutableStateOf(schedule.isWeekendDifferent) }
    var bedtimeReminder by remember(schedule) { mutableStateOf(schedule.bedtimeReminderEnabled) }
    var wakeupReminder by remember(schedule) { mutableStateOf(schedule.wakeupReminderEnabled) }
    var leadMinutes by remember(schedule) { mutableIntStateOf(schedule.reminderLeadMinutes) }

    val palette = LocalSleepPalette.current
    val cardBg = palette.cardBg
    val cardBorder = palette.cardBorder
    val cardSecondaryBg = palette.cardSecondaryBg
    val sleepPurple = palette.sleepPurple
    val sleepGold = palette.sleepGold
    val sleepGreen = palette.sleepGreen
    val textPrimary = palette.textPrimary
    val textSecondary = palette.textSecondary

    // Calculate Planned Duration across midnight
    val weekdayDurationHours = calculateDurationHours(weekdayBedHour, weekdayBedMinute, weekdayWakeHour, weekdayWakeMinute)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. SLEEP GOAL STEPPER ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Target Sleep Goal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Adults typically benefit from 7 to 9 hours of uninterrupted sleep.",
                        fontSize = 12.sp,
                        color = textSecondary
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (goalHours > 5f) goalHours -= 0.5f },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(cardSecondaryBg)
                        ) {
                            Text("-", fontSize = 22.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${goalHours}h",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = sleepPurple
                            )
                            Text("per night", fontSize = 11.sp, color = textSecondary)
                        }

                        IconButton(
                            onClick = { if (goalHours < 12f) goalHours += 0.5f },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(cardSecondaryBg)
                        ) {
                            Text("+", fontSize = 20.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 2. WEEKDAY SCHEDULE ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Weekday Schedule (Mon - Fri)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bedtime Picker Button
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(context, { _, h, m ->
                                    weekdayBedHour = h
                                    weekdayBedMinute = m
                                }, weekdayBedHour, weekdayBedMinute, false).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("🌙 Bedtime", fontSize = 11.sp, color = textSecondary)
                                Text(formatHourMinute(weekdayBedHour, weekdayBedMinute), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = sleepPurple)
                            }
                        }

                        // Wake-up Picker Button
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(context, { _, h, m ->
                                    weekdayWakeHour = h
                                    weekdayWakeMinute = m
                                }, weekdayWakeHour, weekdayWakeMinute, false).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("☀️ Wake-up", fontSize = 11.sp, color = textSecondary)
                                Text(formatHourMinute(weekdayWakeHour, weekdayWakeMinute), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = sleepGold)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Planned sleep opportunity: ${weekdayDurationHours}h",
                        fontSize = 12.sp,
                        color = if (weekdayDurationHours >= goalHours) sleepGreen else sleepGold
                    )
                }
            }
        }

        // --- 3. WEEKEND SCHEDULE (DIFFERENT) ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Weekend Schedule",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Switch(
                            checked = isWeekendDifferent,
                            onCheckedChange = { isWeekendDifferent = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = sleepPurple)
                        )
                    }

                    if (isWeekendDifferent) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(context, { _, h, m ->
                                        weekendBedHour = h
                                        weekendBedMinute = m
                                    }, weekendBedHour, weekendBedMinute, false).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text("Sat/Sun Bed", fontSize = 11.sp, color = textSecondary)
                                    Text(formatHourMinute(weekendBedHour, weekendBedMinute), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = sleepPurple)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(context, { _, h, m ->
                                        weekendWakeHour = h
                                        weekendWakeMinute = m
                                    }, weekendWakeHour, weekendWakeMinute, false).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text("Sat/Sun Wake", fontSize = 11.sp, color = textSecondary)
                                    Text(formatHourMinute(weekendWakeHour, weekendWakeMinute), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = sleepGold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. REMINDER NOTIFICATIONS ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Sleep Notifications & Reminders",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bedtime Wind-Down Alert", fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                            Text("Alert $leadMinutes min before planned sleep", fontSize = 11.sp, color = textSecondary)
                        }
                        Switch(
                            checked = bedtimeReminder,
                            onCheckedChange = { bedtimeReminder = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = sleepPurple)
                        )
                    }

                    if (bedtimeReminder) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 30, 45, 60).forEach { mins ->
                                FilterChip(
                                    selected = leadMinutes == mins,
                                    onClick = { leadMinutes = mins },
                                    label = { Text("${mins}m lead") }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Morning Wake-Up Notification", fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                            Text("Gentle wake-up call to greet the sunrise", fontSize = 11.sp, color = textSecondary)
                        }
                        Switch(
                            checked = wakeupReminder,
                            onCheckedChange = { wakeupReminder = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = sleepGold)
                        )
                    }
                }
            }
        }

        // --- 5. SAVE BUTTON ---
        item {
            Button(
                onClick = {
                    val updated = schedule.copy(
                        sleepGoalHours = goalHours,
                        weekdayBedtimeHour = weekdayBedHour,
                        weekdayBedtimeMinute = weekdayBedMinute,
                        weekdayWakeupHour = weekdayWakeHour,
                        weekdayWakeupMinute = weekdayWakeMinute,
                        weekendBedtimeHour = weekendBedHour,
                        weekendBedtimeMinute = weekendBedMinute,
                        weekendWakeupHour = weekendWakeHour,
                        weekendWakeupMinute = weekendWakeMinute,
                        isWeekendDifferent = isWeekendDifferent,
                        bedtimeReminderEnabled = bedtimeReminder,
                        wakeupReminderEnabled = wakeupReminder,
                        reminderLeadMinutes = leadMinutes
                    )
                    onSaveSchedule(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sleepPurple)
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Save Schedule & Sync Reminders", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ==========================================
// 3. SLEEP HISTORY TAB
// ==========================================
@Composable
fun SleepHistoryTab(
    sessions: List<SleepSessionEntity>,
    schedule: SleepScheduleEntity,
    onAddRecord: () -> Unit,
    onEditRecord: (SleepSessionEntity) -> Unit,
    onDeleteRecord: (SleepSessionEntity) -> Unit
) {
    val palette = LocalSleepPalette.current
    val cardBg = palette.cardBg
    val cardBorder = palette.cardBorder
    val sleepPurple = palette.sleepPurple
    val sleepGreen = palette.sleepGreen
    val textPrimary = palette.textPrimary
    val textSecondary = palette.textSecondary

    var filterPeriod by remember { mutableStateOf("ALL") } // "ALL", "WEEK", "MONTH"

    val filteredSessions = remember(sessions, filterPeriod) {
        when (filterPeriod) {
            "WEEK" -> {
                val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                sessions.filter { it.wakeTimeMs >= sevenDaysAgo }
            }
            "MONTH" -> {
                val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
                sessions.filter { it.wakeTimeMs >= thirtyDaysAgo }
            }
            else -> sessions
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Filter & Add Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL" to "All Time", "WEEK" to "7 Days", "MONTH" to "30 Days").forEach { (key, label) ->
                        FilterChip(
                            selected = filterPeriod == key,
                            onClick = { filterPeriod = key },
                            label = { Text(label, fontSize = 11.5.sp) }
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onAddRecord,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp)
                }
            }
        }

        if (filteredSessions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("😴", fontSize = 42.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No sleep records logged yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Use 'Start Sleep' tonight or tap 'Add' to enter past nights manually.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredSessions, key = { it.id }) { session ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = session.date,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                val bedStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(session.bedTimeMs))
                                val wakeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(session.wakeTimeMs))
                                Text(
                                    text = "$bedStr → $wakeStr",
                                    fontSize = 11.5.sp,
                                    color = textSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (session.qualityRating) {
                                        "Excellent" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        "Good" -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                        "Average" -> Color(0xFFFBBF24).copy(alpha = 0.2f)
                                        else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                    }
                                ) {
                                    Text(
                                        text = session.qualityRating,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (session.qualityRating) {
                                            "Excellent" -> Color(0xFF10B981)
                                            "Good" -> Color(0xFF38BDF8)
                                            "Average" -> Color(0xFFFBBF24)
                                            else -> Color(0xFFEF4444)
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Spacer(Modifier.width(6.dp))

                                IconButton(onClick = { onEditRecord(session) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = textSecondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { onDeleteRecord(session) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${session.durationHours} Hours",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (session.durationHours >= session.sleepGoalHours) sleepGreen else sleepPurple
                            )
                            Text(
                                text = "${session.goalCompletionPercent.toInt()}% of ${session.sleepGoalHours}h Goal",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }

                        if (session.notes.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "“${session.notes}”",
                                fontSize = 11.5.sp,
                                color = textSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }

                        // Source Tag
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Source: ${session.source.replace("_", " ")}",
                            fontSize = 9.5.sp,
                            color = textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. SLEEP ANALYTICS TAB
// ==========================================
@Composable
fun SleepAnalyticsTab(
    analytics: SleepAnalyticsSummary,
    streak: SleepStreakSummary,
    schedule: SleepScheduleEntity,
    recommendations: List<SleepRecommendation>
) {
    val palette = LocalSleepPalette.current
    val cardBg = palette.cardBg
    val cardBorder = palette.cardBorder
    val sleepPurple = palette.sleepPurple
    val sleepCyan = palette.sleepCyan
    val sleepGreen = palette.sleepGreen
    val sleepGold = palette.sleepGold
    val textPrimary = palette.textPrimary
    val textSecondary = palette.textSecondary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. WEEKLY MON-SUN BAR CHART ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Weekly Sleep Duration (Last 7 Days)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Target Goal Line: ${schedule.sleepGoalHours} Hours",
                        fontSize = 11.5.sp,
                        color = sleepPurple
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxScale = 12f
                        analytics.weeklyDays.forEach { day ->
                            val heightFraction = (day.durationHours / maxScale).coerceIn(0.08f, 1f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (day.durationHours > 0f) {
                                    Text(
                                        text = "${day.durationHours}h",
                                        fontSize = 9.sp,
                                        color = if (day.isGoalMet) sleepGreen else textSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (day.isGoalMet) Brush.verticalGradient(listOf(sleepGreen, sleepCyan))
                                            else if (day.durationHours > 0f) Brush.verticalGradient(listOf(sleepPurple, cardBorder))
                                            else Brush.verticalGradient(listOf(cardBorder, cardBorder))
                                        )
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = day.dayLabel,
                                    fontSize = 11.sp,
                                    color = if (day.isGoalMet) textPrimary else textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. BENCHMARKS GRID ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Average
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Weekly Average", fontSize = 11.sp, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("${analytics.weeklyAvgHours}h", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = sleepCyan)
                        Text("per night", fontSize = 10.sp, color = textSecondary)
                    }
                }

                // Consistency
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Consistency", fontSize = 11.sp, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("${analytics.weeklyScheduleConsistencyPercent}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = sleepGreen)
                        Text("circadian sync", fontSize = 10.sp, color = textSecondary)
                    }
                }

                // Best Day
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Best Day", fontSize = 11.sp, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(analytics.bestDayLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sleepGold)
                        Text("most rested", fontSize = 10.sp, color = textSecondary)
                    }
                }
            }
        }

        // --- 3. MONTHLY METRICS CARD ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Monthly Overview (30 Days)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Sleep", fontSize = 11.sp, color = textSecondary)
                            Text("${analytics.monthlyTotalHours}h", fontSize = 22.sp, fontWeight = FontWeight.Black, color = textPrimary)
                        }
                        Column {
                            Text("Monthly Average", fontSize = 11.sp, color = textSecondary)
                            Text("${analytics.monthlyAvgHours}h", fontSize = 22.sp, fontWeight = FontWeight.Black, color = sleepPurple)
                        }
                        Column {
                            Text("Goal-Met Nights", fontSize = 11.sp, color = textSecondary)
                            Text("${analytics.monthlyGoalMetDays} Days", fontSize = 22.sp, fontWeight = FontWeight.Black, color = sleepGreen)
                        }
                    }
                }
            }
        }

        // --- 4. QUALITY DISTRIBUTION ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Reported Sleep Quality Distribution",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(14.dp))

                    analytics.qualityDistribution.forEach { (rating, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rating, fontSize = 13.sp, color = textPrimary)
                            Text("$count nights", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = sleepCyan)
                        }
                    }
                }
            }
        }

        // --- 5. PERSONALIZED RECOMMENDATIONS ---
        item {
            Text(
                text = "Sleep Optimization Insights",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        items(recommendations) { rec ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(rec.iconEmoji, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = rec.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = rec.message,
                            fontSize = 12.5.sp,
                            color = textSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. RELAXATION SOUNDS TAB
// ==========================================
@Composable
fun SleepRelaxationTab(
    audioManager: SleepAudioManager,
    isPlaying: Boolean,
    currentSound: SleepSoundType?,
    volume: Float,
    timerRemaining: Int
) {
    val palette = LocalSleepPalette.current
    val cardBg = palette.cardBg
    val cardBorder = palette.cardBorder
    val cardSecondaryBg = palette.cardSecondaryBg
    val sleepPurple = palette.sleepPurple
    val sleepCyan = palette.sleepCyan
    val textPrimary = palette.textPrimary
    val textSecondary = palette.textSecondary

    var selectedTimerOption by remember { mutableIntStateOf(0) } // 0, 15, 30, 60

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- MASTER AUDIO PLAYER CARD ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.2.dp, if (isPlaying) sleepPurple else cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isPlaying) (currentSound?.iconEmoji ?: "🎵") else "🌙",
                        fontSize = 44.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = currentSound?.title ?: "Select a Relaxation Sound",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = if (isPlaying) (currentSound?.subtitle ?: "Playing soundscape...") else "Tap a sound below to play continuously",
                        fontSize = 12.5.sp,
                        color = textSecondary
                    )

                    Spacer(Modifier.height(18.dp))

                    // Player Controls Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Pause
                        IconButton(
                            onClick = {
                                if (isPlaying) audioManager.pause()
                                else if (currentSound != null) audioManager.resume()
                                else audioManager.play(SleepSoundType.RAIN)
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(sleepPurple)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Stop Button
                        if (isPlaying || currentSound != null) {
                            IconButton(
                                onClick = { audioManager.stop() },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(cardSecondaryBg)
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Slider(
                            value = volume,
                            onValueChange = { audioManager.setVolume(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = sleepPurple,
                                activeTrackColor = sleepPurple,
                                inactiveTrackColor = cardBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${(volume * 100).toInt()}%", fontSize = 12.sp, color = textSecondary)
                    }

                    // Sleep Timer Display
                    if (timerRemaining > 0) {
                        val mins = timerRemaining / 60
                        val secs = timerRemaining % 60
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = sleepCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "⏳ Auto-off timer: ${mins}m ${secs}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = sleepCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- SLEEP TIMER CHIPS ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sleep Timer (Fade out & Stop)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m").forEach { (duration, label) ->
                            FilterChip(
                                selected = selectedTimerOption == duration,
                                onClick = {
                                    selectedTimerOption = duration
                                    audioManager.setSleepTimer(duration)
                                },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // --- SOUNDSCAPES LIST ---
        item {
            Text(
                text = "Procedural Soothing Soundscapes",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        items(SleepSoundType.values()) { sound ->
            val isCurrent = currentSound == sound
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                border = BorderStroke(1.dp, if (isCurrent && isPlaying) sleepPurple else cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { audioManager.play(sound) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(sound.iconEmoji, fontSize = 28.sp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = sound.title,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent && isPlaying) sleepPurple else textPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = sound.subtitle,
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isCurrent && isPlaying) audioManager.pause()
                            else audioManager.play(sound)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isCurrent && isPlaying) sleepPurple else cardSecondaryBg)
                    ) {
                        Icon(
                            if (isCurrent && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = if (isCurrent && isPlaying) Color.White else textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// DIALOGS & SHEET MODALS
// ==========================================

@Composable
fun WakeUpRatingDialog(
    activeState: ActiveSleepState,
    onDismiss: () -> Unit,
    onSave: (quality: String, awakenings: Int, latency: Int, notes: String) -> Unit
) {
    val palette = LocalSleepPalette.current
    var selectedQuality by remember { mutableStateOf("Good") }
    var awakenings by remember { mutableIntStateOf(0) }
    var latency by remember { mutableIntStateOf(15) }
    var notes by remember { mutableStateOf("") }

    val qualities = listOf("Excellent", "Good", "Average", "Poor", "Very Poor")
    val elapsedMinutes = ((System.currentTimeMillis() - activeState.startTimeMs) / 60_000L).coerceAtLeast(0)
    val elapsedH = elapsedMinutes / 60
    val elapsedM = elapsedMinutes % 60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Good Morning! ☀️", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Total Sleep Duration: ${elapsedH}h ${elapsedM}m",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.sleepGold
                )

                Text("How would you rate your sleep quality?", fontSize = 13.sp, color = palette.textSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    qualities.forEach { q ->
                        FilterChip(
                            selected = selectedQuality == q,
                            onClick = { selectedQuality = q },
                            label = { Text(q, fontSize = 10.5.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Awakenings during night:", fontSize = 12.sp, color = palette.textSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (awakenings > 0) awakenings-- }, modifier = Modifier.size(32.dp)) {
                            Text("-", fontSize = 18.sp, color = palette.textPrimary)
                        }
                        Text("$awakenings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        IconButton(onClick = { awakenings++ }, modifier = Modifier.size(32.dp)) {
                            Text("+", fontSize = 16.sp, color = palette.textPrimary)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Dreams, room temperature, etc.)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedQuality, awakenings, latency, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.sleepPurple)
            ) {
                Text("Save Session", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textSecondary) }
        },
        containerColor = palette.cardBg
    )
}

@Composable
fun ManualAddSleepDialog(
    defaultGoal: Float,
    onDismiss: () -> Unit,
    onSave: (dateStr: String, bedMs: Long, wakeMs: Long, quality: String, latency: Int, awakenings: Int, notes: String) -> Unit
) {
    val palette = LocalSleepPalette.current
    val context = LocalContext.current
    val cal = Calendar.getInstance()

    var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)) }
    var bedHour by remember { mutableIntStateOf(22) }
    var bedMinute by remember { mutableIntStateOf(30) }
    var wakeHour by remember { mutableIntStateOf(6) }
    var wakeMinute by remember { mutableIntStateOf(30) }
    var selectedQuality by remember { mutableStateOf("Good") }
    var notes by remember { mutableStateOf("") }

    val qualities = listOf("Excellent", "Good", "Average", "Poor", "Very Poor")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Past Sleep Record", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Waking Date: $dateStr", fontSize = 13.sp, color = palette.sleepCyan, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(context, { _, h, m -> bedHour = h; bedMinute = m }, bedHour, bedMinute, false).show()
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, palette.cardBorder)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bedtime", fontSize = 11.sp, color = palette.textSecondary)
                            Text(formatHourMinute(bedHour, bedMinute), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.sleepPurple)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(context, { _, h, m -> wakeHour = h; wakeMinute = m }, wakeHour, wakeMinute, false).show()
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, palette.cardBorder)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Wake-up", fontSize = 11.sp, color = palette.textSecondary)
                            Text(formatHourMinute(wakeHour, wakeMinute), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.sleepGold)
                        }
                    }
                }

                val duration = calculateDurationHours(bedHour, bedMinute, wakeHour, wakeMinute)
                Text("Calculated Duration: ${duration}h", fontSize = 12.sp, color = palette.sleepGreen, fontWeight = FontWeight.SemiBold)

                Text("Sleep Quality:", fontSize = 12.sp, color = palette.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    qualities.forEach { q ->
                        FilterChip(
                            selected = selectedQuality == q,
                            onClick = { selectedQuality = q },
                            label = { Text(q, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bedCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, bedHour)
                        set(Calendar.MINUTE, bedMinute)
                        set(Calendar.SECOND, 0)
                    }
                    val wakeCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, wakeHour)
                        set(Calendar.MINUTE, wakeMinute)
                        set(Calendar.SECOND, 0)
                    }
                    if (wakeCal.before(bedCal)) {
                        bedCal.add(Calendar.DAY_OF_YEAR, -1)
                    }
                    onSave(dateStr, bedCal.timeInMillis, wakeCal.timeInMillis, selectedQuality, 15, 0, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.sleepPurple)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textSecondary) }
        },
        containerColor = palette.cardBg
    )
}

@Composable
fun EditSleepDialog(
    session: SleepSessionEntity,
    onDismiss: () -> Unit,
    onSave: (SleepSessionEntity) -> Unit
) {
    val palette = LocalSleepPalette.current
    var selectedQuality by remember { mutableStateOf(session.qualityRating) }
    var notes by remember { mutableStateOf(session.notes) }
    val qualities = listOf("Excellent", "Good", "Average", "Poor", "Very Poor")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Sleep Record", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Date: ${session.date} (${session.durationHours}h)", color = palette.sleepCyan, fontWeight = FontWeight.Bold)

                Text("Adjust Quality Rating:", fontSize = 12.sp, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    qualities.forEach { q ->
                        FilterChip(
                            selected = selectedQuality == q,
                            onClick = { selectedQuality = q },
                            label = { Text(q, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(session.copy(qualityRating = selectedQuality, notes = notes))
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.sleepPurple)
            ) {
                Text("Update", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textSecondary) }
        },
        containerColor = palette.cardBg
    )
}

@Composable
fun SleepAchievementsDialog(
    achievements: List<SleepAchievementEntity>,
    streak: SleepStreakSummary,
    onDismiss: () -> Unit
) {
    val palette = LocalSleepPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text("Sleep Milestones", color = palette.textPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = "Current Streak: ${streak.currentStreak} Days • Longest: ${streak.longestStreak} Days",
                        fontSize = 12.sp,
                        color = palette.sleepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                }

                items(achievements) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isUnlocked) {
                                if (palette.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            } else {
                                if (palette.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        border = BorderStroke(1.dp, if (item.isUnlocked) palette.sleepPurple else palette.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.icon, fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isUnlocked) palette.textPrimary else palette.textMuted
                                )
                                Text(
                                    text = item.description,
                                    fontSize = 11.5.sp,
                                    color = palette.textSecondary
                                )
                            }
                            if (item.isUnlocked) {
                                Text("✓", color = palette.sleepGreen, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = palette.sleepPurple) }
        },
        containerColor = palette.cardBg
    )
}

@Composable
fun HealthConnectSyncDialog(
    hcManager: HealthConnectManager,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit
) {
    val palette = LocalSleepPalette.current
    val isAvailable = hcManager.isAvailable()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Health Connect Integration", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Android Health Connect allows you to safely import sleep sessions logged from smartwatches and certified health accessories without fabricating artificial sleep stages.",
                    fontSize = 12.5.sp,
                    color = palette.textSecondary,
                    lineHeight = 17.sp
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isAvailable) palette.sleepGreen.copy(alpha = 0.15f) else palette.sleepGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isAvailable) "✓ Health Connect SDK Available" else "⚠️ Health Connect App Not Installed",
                        fontSize = 11.5.sp,
                        color = if (isAvailable) palette.sleepGreen else palette.sleepGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (isAvailable) {
                Button(
                    onClick = onSyncNow,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.sleepCyan)
                ) {
                    Text("Sync Recent Sleep", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { hcManager.openHealthConnectStore() },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.sleepGold)
                ) {
                    Text("Install Health Connect", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss", color = palette.textSecondary) }
        },
        containerColor = palette.cardBg
    )
}

// ==========================================
// UTILITY FUNCTIONS
// ==========================================

fun formatHourMinute(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
}

fun calculateDurationHours(bedH: Int, bedM: Int, wakeH: Int, wakeM: Int): Float {
    val bedMinutes = bedH * 60 + bedM
    val wakeMinutes = wakeH * 60 + wakeM
    val diff = if (wakeMinutes >= bedMinutes) {
        wakeMinutes - bedMinutes
    } else {
        (24 * 60 - bedMinutes) + wakeMinutes
    }
    return (diff / 60f * 10f).roundToInt() / 10f
}
