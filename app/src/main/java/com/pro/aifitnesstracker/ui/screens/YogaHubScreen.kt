package com.pro.aifitnesstracker.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.AppDatabase
import com.pro.aifitnesstracker.R
import com.pro.aifitnesstracker.data.yoga.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// ── Asset Image Loader Helper ────────────────────────────────────────────────
@Composable
fun rememberYogaPoseImage(poseId: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(poseId) {
        val extensions = listOf("jpg", "jpeg", "webp", "png")
        var bitmap: android.graphics.Bitmap? = null
        val fallbackMap = mapOf(
            "cat_cow" to "bhujangasana",
            "viparita_karani" to "balasana",
            "shavasana" to "balasana",
            "sukhasana" to "balasana",
            "vajrasana" to "balasana",
            "paschimottanasana" to "navasana",
            "ustrasana" to "bhujangasana",
            "bakasana" to "plank",
            "virabhadrasana_1" to "warrior_1",
            "virabhadrasana_2" to "warrior_2",
            "vrikshasana" to "vrksasana",
            "setu_bandhasana" to "setu_bandha"
        )
        val targetId = fallbackMap[poseId] ?: poseId
        for (ext in extensions) {
            try {
                val stream = context.assets.open("yoga_poses/$targetId.$ext")
                bitmap = BitmapFactory.decodeStream(stream)
                stream.close()
                if (bitmap != null) break
            } catch (_: Exception) {}
        }
        bitmap?.asImageBitmap()
    }
}

// ── Navigation State for Yoga Hub ─────────────────────────────────────────────
sealed class YogaHubNav {
    object Main : YogaHubNav()
    data class PoseDetail(val pose: DetailedYogaPose) : YogaHubNav()
    data class SessionRunner(
        val sessionTitle: String,
        val category: String,
        val poses: List<DetailedYogaPose>,
        val restSeconds: Int = 10,
        val onCompleteCallback: (() -> Unit)? = null
    ) : YogaHubNav()
    object Breathing : YogaHubNav()
}

// ── Master Yoga Hub Screen ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YogaHubScreen(
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Database & Repository
    val repository = remember {
        val db = AppDatabase.getDatabase(context)
        YogaRepository(db.yogaDao(), db.healthDao(), context)
    }

    // Live Flow Collections
    val todaySessions by repository.getTodaySessions().collectAsState(initial = emptyList())
    val todayMins by repository.getTodayCompletedMinutes().collectAsState(initial = 0)
    val todayCals by repository.getTodayCaloriesBurned().collectAsState(initial = 0)
    val todayRemainingMins by repository.getTodayRemainingMinutes().collectAsState(initial = repository.getDailyGoalMinutes())
    val currentStreak by repository.getCurrentStreakFlow().collectAsState(initial = 0)
    val favoritePoseIds by repository.getAllFavoritePoseIds().collectAsState(initial = emptyList())
    val allPlansProgress by repository.getAllPlanProgress().collectAsState(initial = emptyList())
    val allAchievements by repository.getAllAchievements().collectAsState(initial = emptyList())
    val customRoutines by repository.getAllCustomRoutines().collectAsState(initial = emptyList())

    // Weekly Analytics State
    var weeklyAnalytics by remember { mutableStateOf<YogaRepository.WeeklyAnalyticsData?>(null) }
    var monthlyAnalytics by remember { mutableStateOf<YogaRepository.MonthlyAnalyticsData?>(null) }

    LaunchedEffect(todaySessions) {
        withContext(Dispatchers.IO) {
            weeklyAnalytics = repository.getWeeklyAnalytics()
            monthlyAnalytics = repository.getMonthlyAnalytics()
            repository.checkAndUnlockAchievements()
        }
    }

    // Top-Level Navigation State
    var currentNav by remember { mutableStateOf<YogaHubNav>(YogaHubNav.Main) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Categories, 2: Poses, 3: Plans, 4: Analytics

    // Modals & Dialogs State
    var showGoalSettingsDialog by remember { mutableStateOf(false) }
    var showReminderSettingsDialog by remember { mutableStateOf(false) }
    var showCreateRoutineDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    val screenBg = if (isDark) Color(0xFF090D16) else Color(0xFFF4F7FB)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    // Animated view switcher
    AnimatedContent(
        targetState = currentNav,
        transitionSpec = {
            if (targetState is YogaHubNav.SessionRunner || targetState is YogaHubNav.PoseDetail || targetState is YogaHubNav.Breathing) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "yoga_hub_nav"
    ) { navState ->
        when (navState) {
            is YogaHubNav.PoseDetail -> {
                YogaPoseDetailView(
                    pose = navState.pose,
                    isFavorite = favoritePoseIds.contains(navState.pose.id),
                    onToggleFavorite = {
                        coroutineScope.launch {
                            repository.toggleFavorite(navState.pose.id, favoritePoseIds.contains(navState.pose.id))
                        }
                    },
                    onStartSinglePose = {
                        currentNav = YogaHubNav.SessionRunner(
                            sessionTitle = navState.pose.name,
                            category = navState.pose.category,
                            poses = listOf(navState.pose),
                            restSeconds = 5
                        )
                    },
                    onBack = { currentNav = YogaHubNav.Main }
                )
            }

            is YogaHubNav.SessionRunner -> {
                GuidedYogaSessionRunner(
                    sessionTitle = navState.sessionTitle,
                    category = navState.category,
                    poses = navState.poses,
                    restSeconds = navState.restSeconds,
                    onSaveSession = { title, cat, planned, completed, posesDone, total, isDone ->
                        coroutineScope.launch {
                            repository.recordYogaSession(title, cat, planned, completed, posesDone, total, isDone)
                            navState.onCompleteCallback?.invoke()
                        }
                    },
                    onExit = { currentNav = YogaHubNav.Main }
                )
            }

            is YogaHubNav.Breathing -> {
                YogaBreathingExerciseView(
                    onBack = { currentNav = YogaHubNav.Main }
                )
            }

            is YogaHubNav.Main -> {
                Scaffold(
                    containerColor = screenBg,
                    topBar = {
                        Surface(
                            color = cardBg,
                            shadowElevation = 3.dp,
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = onBack) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = textPrimary
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${strings.yoga.uppercase(java.util.Locale.US)} & WELLNESS",
                                            color = textPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.4.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Chime Bell Action
                                        IconButton(onClick = { playTibetanBell(context) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.VolumeUp,
                                                contentDescription = "Tibetan Bell",
                                                tint = Color(0xFF10B981)
                                            )
                                        }

                                        // Goal Settings Button
                                        IconButton(onClick = { showGoalSettingsDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tune,
                                                contentDescription = "Yoga Settings",
                                                tint = textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        val tabs = listOf(
                            Pair(strings.home, Icons.Rounded.Dashboard),
                            Pair("Categories", Icons.Rounded.GridView),
                            Pair(strings.yoga, Icons.Rounded.SelfImprovement),
                            Pair("Plans", Icons.Rounded.Assignment),
                            Pair(strings.analytics, Icons.Rounded.BarChart)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = cardBg,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
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
                                    tabs.forEachIndexed { index, pair ->
                                        val isSelected = selectedTab == index
                                        val itemColor = if (isSelected) Color(0xFF10B981) else textSecondary
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
                                                        if (isSelected) Color(0xFF10B981).copy(alpha = if (isDark) 0.18f else 0.12f)
                                                        else Color.Transparent
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = pair.second,
                                                    contentDescription = pair.first,
                                                    tint = itemColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = pair.first,
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
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        when (selectedTab) {
                            0 -> YogaDashboardTab(
                                todayMinutes = todayMins,
                                goalMinutes = repository.getDailyGoalMinutes(),
                                remainingMinutes = todayRemainingMins,
                                sessionsCount = todaySessions.size,
                                caloriesBurned = todayCals,
                                weeklyPracticedDays = weeklyAnalytics?.daysPracticedCount ?: 0,
                                currentStreak = currentStreak,
                                activePlan = allPlansProgress.find { it.isStarted && !it.isPaused },
                                onStartSession = { title, cat, poseIds ->
                                    val posesList = poseIds.mapNotNull { YogaContentRepository.getPoseById(it) }
                                    if (posesList.isNotEmpty()) {
                                        currentNav = YogaHubNav.SessionRunner(title, cat, posesList)
                                    }
                                },
                                onSelectCategory = { catName ->
                                    selectedCategoryFilter = catName
                                    selectedTab = 1
                                },
                                onExplorePoses = { selectedTab = 2 },
                                onExplorePlans = { selectedTab = 3 },
                                onOpenBreathing = { currentNav = YogaHubNav.Breathing }
                            )

                            1 -> YogaCategoriesTab(
                                categories = YogaContentRepository.categories,
                                selectedCategory = selectedCategoryFilter,
                                onSelectCategory = { selectedCategoryFilter = it },
                                onStartCategorySession = { cat ->
                                    val session = YogaContentRepository.guidedSessions.find { it.category == cat.name }
                                        ?: YogaContentRepository.guidedSessions.first()
                                    val posesList = session.poseIds.mapNotNull { YogaContentRepository.getPoseById(it) }
                                    currentNav = YogaHubNav.SessionRunner(session.title, session.category, posesList)
                                },
                                onBrowseCategoryPoses = { catName ->
                                    selectedCategoryFilter = catName
                                    selectedTab = 2
                                }
                            )

                            2 -> YogaPoseLibraryTab(
                                allPoses = YogaContentRepository.poses,
                                favoritePoseIds = favoritePoseIds,
                                initialCategory = selectedCategoryFilter,
                                onSelectPose = { currentNav = YogaHubNav.PoseDetail(it) },
                                onToggleFavorite = { poseId ->
                                    coroutineScope.launch {
                                        repository.toggleFavorite(poseId, favoritePoseIds.contains(poseId))
                                    }
                                },
                                onStartFavoritesSession = {
                                    val favPoses = favoritePoseIds.mapNotNull { YogaContentRepository.getPoseById(it) }
                                    if (favPoses.isNotEmpty()) {
                                        currentNav = YogaHubNav.SessionRunner("Favorite Poses Routine", YogaCategoryNames.FULL_BODY, favPoses)
                                    }
                                }
                            )

                            3 -> YogaPlansAndRoutinesTab(
                                plans = YogaContentRepository.personalizedPlans,
                                plansProgress = allPlansProgress,
                                customRoutines = customRoutines,
                                onStartPlan = { planId ->
                                    coroutineScope.launch { repository.startPlan(planId) }
                                },
                                onTogglePausePlan = { planId ->
                                    coroutineScope.launch { repository.togglePausePlan(planId) }
                                },
                                onResetPlan = { planId ->
                                    coroutineScope.launch { repository.resetPlan(planId) }
                                },
                                onStartPlanDay = { plan, dayRoutine ->
                                    val posesList = dayRoutine.poseIds.mapNotNull { YogaContentRepository.getPoseById(it) }
                                    currentNav = YogaHubNav.SessionRunner(
                                        sessionTitle = "${plan.title} - ${dayRoutine.title}",
                                        category = plan.title,
                                        poses = posesList,
                                        onCompleteCallback = {
                                            coroutineScope.launch {
                                                repository.completePlanDay(plan.id, dayRoutine.dayNumber)
                                            }
                                        }
                                    )
                                },
                                onCreateCustomRoutine = { showCreateRoutineDialog = true },
                                onStartCustomRoutine = { routine ->
                                    val poseIdList = routine.poseIds.split(",").filter { it.isNotBlank() }
                                    val posesList = poseIdList.mapNotNull { YogaContentRepository.getPoseById(it) }
                                    if (posesList.isNotEmpty()) {
                                        currentNav = YogaHubNav.SessionRunner(routine.name, routine.category, posesList, routine.restIntervalSeconds)
                                    }
                                },
                                onDeleteCustomRoutine = { id ->
                                    coroutineScope.launch { repository.deleteCustomRoutine(id) }
                                }
                            )

                            4 -> YogaAnalyticsTab(
                                weeklyAnalytics = weeklyAnalytics,
                                monthlyAnalytics = monthlyAnalytics,
                                achievements = allAchievements,
                                onOpenReminders = { showReminderSettingsDialog = true }
                            )
                        }
                    }
                }

                // Dialog: Goal Settings
                if (showGoalSettingsDialog) {
                    YogaGoalSettingsDialog(
                        currentGoal = repository.getDailyGoalMinutes(),
                        onSave = { newGoal ->
                            repository.setDailyGoalMinutes(newGoal)
                            showGoalSettingsDialog = false
                        },
                        onDismiss = { showGoalSettingsDialog = false }
                    )
                }

                // Dialog: Reminder Settings
                if (showReminderSettingsDialog) {
                    YogaReminderSettingsDialog(
                        isEnabled = repository.isReminderEnabled(),
                        reminderTime = repository.getReminderTime(),
                        onSave = { enabled, h, m ->
                            repository.setReminderEnabled(enabled)
                            repository.setReminderTime(h, m)
                            showReminderSettingsDialog = false
                        },
                        onDismiss = { showReminderSettingsDialog = false }
                    )
                }

                // Dialog: Create Custom Routine
                if (showCreateRoutineDialog) {
                    CreateCustomRoutineDialog(
                        allPoses = YogaContentRepository.poses,
                        onSave = { name, cat, selectedIds, durations, restSecs ->
                            coroutineScope.launch {
                                repository.saveCustomRoutine(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    category = cat,
                                    poseIds = selectedIds,
                                    durations = durations,
                                    restSecs = restSecs
                                )
                                showCreateRoutineDialog = false
                            }
                        },
                        onDismiss = { showCreateRoutineDialog = false }
                    )
                }
            }
        }
    }
}

// ── 1. Yoga Dashboard Tab ────────────────────────────────────────────────────
@Composable
private fun YogaDashboardTab(
    todayMinutes: Int,
    goalMinutes: Int,
    remainingMinutes: Int,
    sessionsCount: Int,
    caloriesBurned: Int,
    weeklyPracticedDays: Int,
    currentStreak: Int,
    activePlan: YogaPlanProgressEntity?,
    onStartSession: (title: String, category: String, poseIds: List<String>) -> Unit,
    onSelectCategory: (String) -> Unit,
    onExplorePoses: () -> Unit,
    onExplorePlans: () -> Unit,
    onOpenBreathing: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today's Mindful Goal Circular Gauge Card
        item {
            val progress = (todayMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(1200, easing = FastOutSlowInEasing),
                label = "yoga_gauge"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "DAILY YOGA PRACTICE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Today's Mindful Goal",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Professional Streak Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentStreak > 0) Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                            shadowElevation = 2.dp,
                            border = BorderStroke(
                                1.dp,
                                if (currentStreak > 0) Color(0xFFF59E0B).copy(alpha = 0.40f)
                                else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = if (currentStreak > 0) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (currentStreak > 0) "$currentStreak Day Streak" else "0 Day Streak",
                                    color = if (currentStreak > 0) Color(0xFFF59E0B) else textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Circular Progress Arc
                    Box(
                        modifier = Modifier.size(190.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(175.dp)) {
                            val strokeWidth = 15.dp.toPx()
                            // Background track
                            drawArc(
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                startAngle = 140f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            // Animated Gradient Arc
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF8B5CF6))
                                ),
                                startAngle = 140f,
                                sweepAngle = 260f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$todayMinutes",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                            Text(
                                text = "/ $goalMinutes Minutes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (remainingMinutes == 0) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF06B6D4).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (remainingMinutes == 0) "Goal Achieved! ✨" else "$remainingMinutes mins left",
                                    color = if (remainingMinutes == 0) Color(0xFF10B981) else Color(0xFF06B6D4),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // 3 Pillars Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        YogaMiniPillar(
                            icon = Icons.Rounded.SelfImprovement,
                            iconTint = Color(0xFF10B981),
                            label = "Sessions",
                            value = "$sessionsCount",
                            modifier = Modifier.weight(1f)
                        )
                        YogaMiniPillar(
                            icon = Icons.Rounded.LocalFireDepartment,
                            iconTint = Color(0xFFEF4444),
                            label = "Calories",
                            value = "$caloriesBurned kcal",
                            modifier = Modifier.weight(1f)
                        )
                        YogaMiniPillar(
                            icon = Icons.Rounded.CalendarToday,
                            iconTint = Color(0xFF06B6D4),
                            label = "Weekly",
                            value = "$weeklyPracticedDays/7 Days",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Quick Actions Row (Quick Start, Breathing)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Start Flow CTA
                Surface(
                    onClick = {
                        val flow = YogaContentRepository.guidedSessions.first()
                        onStartSession(flow.title, flow.category, flow.poseIds)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10B981),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Quick Start",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Morning Flow • 15m",
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = 9.8.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Breathing Exercise Button
                Surface(
                    onClick = onOpenBreathing,
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF06B6D4).copy(alpha = 0.35f) else Color(0xFF06B6D4).copy(alpha = 0.25f)
                    ),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF06B6D4).copy(alpha = if (isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Spa,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Breathing",
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Box • Deep • 4-7-8",
                                color = textSecondary,
                                fontSize = 9.8.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Active Personalized Plan (if started)
        if (activePlan != null) {
            val planSpec = YogaContentRepository.getPlanById(activePlan.planId)
            if (planSpec != null) {
                item {
                    val currentRoutine = planSpec.dailyRoutines.getOrNull(activePlan.currentDay - 1)
                        ?: planSpec.dailyRoutines.first()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = BorderStroke(1.dp, Color(planSpec.colorHex).copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(planSpec.emoji, fontSize = 24.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("CONTINUE PRACTICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(planSpec.colorHex))
                                        Text(planSpec.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(planSpec.colorHex).copy(0.2f)
                                ) {
                                    Text(
                                        "Day ${activePlan.currentDay} of ${planSpec.totalDays}",
                                        color = Color(planSpec.colorHex),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = currentRoutine.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = "${currentRoutine.durationMinutes} min • ${currentRoutine.focusDescription}",
                                fontSize = 12.sp,
                                color = textSecondary
                            )

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    onStartSession("${planSpec.title} - ${currentRoutine.title}", planSpec.title, currentRoutine.poseIds)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(planSpec.colorHex))
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Practice Today's Session", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Daily Challenge Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎯", fontSize = 28.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DAILY YOGA CHALLENGE", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                        Text("10-Min Stress Buster Flow", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("Child's Pose, Cat-Cow & Bridge", fontSize = 12.sp, color = textSecondary)
                    }
                    Button(
                        onClick = {
                            val challenge = YogaContentRepository.guidedSessions.find { it.category == YogaCategoryNames.STRESS_RELIEF }
                                ?: YogaContentRepository.guidedSessions.first()
                            onStartSession("Daily Challenge: Stress Buster", challenge.category, challenge.poseIds)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recommended Yoga Sessions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recommended Sessions", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(
                    "View All Poses",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.clickable { onExplorePoses() }
                )
            }
        }

        // Recommended Yoga Sessions Carousel / List
        items(YogaContentRepository.guidedSessions.take(4)) { session ->
            YogaSessionPreviewCard(
                session = session,
                onStart = { onStartSession(session.title, session.category, session.poseIds) }
            )
        }

        // Categories Quick Row
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Yoga Categories", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        "All 10 Categories",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.clickable { onSelectCategory(YogaCategoryNames.BEGINNER) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(YogaContentRepository.categories) { cat ->
                        Surface(
                            onClick = { onSelectCategory(cat.name) },
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            shadowElevation = 5.dp,
                            border = BorderStroke(1.dp, Color(cat.colorHex).copy(alpha = 0.35f)),
                            modifier = Modifier.width(140.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(cat.emoji, fontSize = 24.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${cat.poseCount} Poses • ${cat.difficulty}", fontSize = 10.5.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YogaMiniPillar(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF0F172A) else Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1)
            Text(label, fontSize = 9.5.sp, color = textSecondary)
        }
    }
}

@Composable
private fun YogaMiniPillar(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF0F172A) else Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1)
            Text(label, fontSize = 9.5.sp, color = textSecondary)
        }
    }
}

@Composable
private fun YogaSessionPreviewCard(
    session: PredefinedYogaSession,
    onStart: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(session.emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(session.subtitle, fontSize = 11.5.sp, color = textSecondary)
                Text("${session.poseIds.size} Poses • ${session.category}", fontSize = 10.5.sp, color = Color(0xFF10B981))
            }
            FilledTonalButton(
                onClick = onStart,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF10B981).copy(0.15f), contentColor = Color(0xFF10B981)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── 2. Yoga Categories Tab ───────────────────────────────────────────────────
@Composable
private fun YogaCategoriesTab(
    categories: List<YogaCategory>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    onStartCategorySession: (YogaCategory) -> Unit,
    onBrowseCategoryPoses: (String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))

        // Filter Pills (All + 10 Categories)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("All Categories (${categories.size})") }
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat.name,
                    onClick = { onSelectCategory(if (selectedCategory == cat.name) null else cat.name) },
                    label = { Text("${cat.emoji} ${cat.name}") }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val visibleCategories = remember(selectedCategory) {
            if (selectedCategory == null) categories
            else categories.filter { it.name == selectedCategory }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(visibleCategories) { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.2.dp, Color(cat.colorHex).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex).copy(0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat.emoji, fontSize = 22.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(cat.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    Text("${cat.poseCount} Poses • ${cat.difficulty} • ~${cat.estimatedDurationMinutes}m", fontSize = 11.5.sp, color = Color(cat.colorHex))
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(cat.description, fontSize = 12.5.sp, color = textSecondary, lineHeight = 17.sp)

                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onBrowseCategoryPoses(cat.name) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(cat.colorHex))
                            ) {
                                Text("Explore Poses", color = Color(cat.colorHex), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onStartCategorySession(cat) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(cat.colorHex))
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Start Flow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 3. Yoga Pose Library Tab ─────────────────────────────────────────────────
@Composable
private fun YogaPoseLibraryTab(
    allPoses: List<DetailedYogaPose>,
    favoritePoseIds: List<String>,
    initialCategory: String?,
    onSelectPose: (DetailedYogaPose) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onStartFavoritesSession: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    var searchQuery by remember { mutableStateOf("") }
    var selectedLevelFilter by remember { mutableStateOf("All") } // "All", "Beginner", "Intermediate", "Advanced", "Favorites"
    var activeCategoryFilter by remember { mutableStateOf(initialCategory) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by English, Sanskrit or Body part...", fontSize = 12.5.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF10B981)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1),
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg
            ),
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        // Level & Favorites Filter Chips
        val levelOptions = listOf("All", "Favorites (${favoritePoseIds.size})", "Beginner", "Intermediate", "Advanced")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(levelOptions) { opt ->
                val isSelected = selectedLevelFilter == opt
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedLevelFilter = opt },
                    label = { Text(opt, fontSize = 11.5.sp) }
                )
            }
        }

        // Favorites Action Banner if filtered to Favorites
        if (selectedLevelFilter.startsWith("Favorites") && favoritePoseIds.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onStartFavoritesSession,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
            ) {
                Text("💖 Practice All Favorites as Guided Flow", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        val filteredPoses = remember(searchQuery, selectedLevelFilter, activeCategoryFilter, favoritePoseIds) {
            allPoses.filter { pose ->
                val matchesSearch = searchQuery.isBlank() ||
                        pose.name.contains(searchQuery, ignoreCase = true) ||
                        pose.sanskritName.contains(searchQuery, ignoreCase = true) ||
                        pose.targetBodyAreas.contains(searchQuery, ignoreCase = true)

                val matchesLevel = when {
                    selectedLevelFilter.startsWith("Favorites") -> favoritePoseIds.contains(pose.id)
                    selectedLevelFilter == "Beginner" -> pose.level == "Beginner"
                    selectedLevelFilter == "Intermediate" -> pose.level == "Intermediate"
                    selectedLevelFilter == "Advanced" -> pose.level == "Advanced"
                    else -> true
                }

                val matchesCat = activeCategoryFilter == null || pose.category.equals(activeCategoryFilter, ignoreCase = true)

                matchesSearch && matchesLevel && matchesCat
            }
        }

        if (filteredPoses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧘", fontSize = 42.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No poses match your criteria", fontSize = 14.sp, color = textSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredPoses) { pose ->
                    YogaPoseCard(
                        pose = pose,
                        isFavorite = favoritePoseIds.contains(pose.id),
                        onSelect = { onSelectPose(pose) },
                        onToggleFavorite = { onToggleFavorite(pose.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun YogaPoseCard(
    pose: DetailedYogaPose,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val poseImage = rememberYogaPoseImage(pose.id)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pose Thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (poseImage != null) {
                    Image(
                        bitmap = poseImage,
                        contentDescription = pose.name,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(pose.emoji, fontSize = 34.sp)
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pose.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    // Level badge
                    val lvlColor = when (pose.level) {
                        "Beginner" -> Color(0xFF10B981)
                        "Intermediate" -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = lvlColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = pose.level,
                            color = lvlColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = pose.sanskritName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF10B981)
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏱️ ${pose.holdSeconds}s", fontSize = 11.sp, color = textSecondary)
                    Spacer(Modifier.width(10.dp))
                    Text("🔥 ~${(pose.caloriesPerMin * pose.holdSeconds / 60).toInt().coerceAtLeast(2)} kcal", fontSize = 11.sp, color = textSecondary)
                    Spacer(Modifier.width(10.dp))
                    Text("📍 ${pose.targetBodyAreas.split(',').firstOrNull() ?: ""}", fontSize = 10.5.sp, color = textSecondary, maxLines = 1)
                }
            }

            // Favorite Button
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── 4. Yoga Plans & Custom Routines Tab ──────────────────────────────────────
@Composable
private fun YogaPlansAndRoutinesTab(
    plans: List<PersonalizedYogaPlan>,
    plansProgress: List<YogaPlanProgressEntity>,
    customRoutines: List<CustomYogaRoutineEntity>,
    onStartPlan: (String) -> Unit,
    onTogglePausePlan: (String) -> Unit,
    onResetPlan: (String) -> Unit,
    onStartPlanDay: (PersonalizedYogaPlan, PlanDailyRoutine) -> Unit,
    onCreateCustomRoutine: () -> Unit,
    onStartCustomRoutine: (CustomYogaRoutineEntity) -> Unit,
    onDeleteCustomRoutine: (String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    var subTab by remember { mutableIntStateOf(0) } // 0: Personalized Plans, 1: Custom Routines

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))

        // Switcher between Plans & Custom Routines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (subTab == 0) Color(0xFF10B981) else Color.Transparent)
                    .clickable { subTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Personalized Plans (${plans.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (subTab == 0) Color.White else textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (subTab == 1) Color(0xFF10B981) else Color.Transparent)
                    .clickable { subTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Custom Routines (${customRoutines.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (subTab == 1) Color.White else textSecondary
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (subTab == 0) {
            // Personalized Plans List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(plans) { plan ->
                    val progress = plansProgress.find { it.planId == plan.id }
                    PersonalizedPlanCard(
                        plan = plan,
                        progress = progress,
                        onStartPlan = { onStartPlan(plan.id) },
                        onTogglePause = { onTogglePausePlan(plan.id) },
                        onResetPlan = { onResetPlan(plan.id) },
                        onStartDay = { routine -> onStartPlanDay(plan, routine) }
                    )
                }
            }
        } else {
            // Custom Routines List + Create Routine Button
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Button(
                        onClick = onCreateCustomRoutine,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create New Custom Routine", fontWeight = FontWeight.Bold)
                    }
                }

                if (customRoutines.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🧘‍♀️", fontSize = 38.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("No custom routines yet", fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("Build your personalized flow by choosing poses and hold times.", fontSize = 12.sp, color = textSecondary, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(customRoutines) { routine ->
                        CustomRoutineCard(
                            routine = routine,
                            onStart = { onStartCustomRoutine(routine) },
                            onDelete = { onDeleteCustomRoutine(routine.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalizedPlanCard(
    plan: PersonalizedYogaPlan,
    progress: YogaPlanProgressEntity?,
    onStartPlan: () -> Unit,
    onTogglePause: () -> Unit,
    onResetPlan: () -> Unit,
    onStartDay: (PlanDailyRoutine) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    var expanded by remember { mutableStateOf(false) }
    val isStarted = progress?.isStarted == true
    val isPaused = progress?.isPaused == true
    val currentDay = progress?.currentDay ?: 1
    val completedDaysSet = remember(progress?.completedDays) {
        progress?.completedDays?.split(",")?.filter { it.isNotBlank() }?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.2.dp, Color(plan.colorHex).copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(plan.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("${plan.totalDays} Days • ${plan.difficulty} • ${plan.dailyGoalMinutes} min/day", fontSize = 11.5.sp, color = Color(plan.colorHex))
                    }
                }

                if (isStarted) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isPaused) Color(0xFFF59E0B).copy(0.15f) else Color(0xFF10B981).copy(0.15f)
                    ) {
                        Text(
                            text = if (isPaused) "Paused" else "Active",
                            color = if (isPaused) Color(0xFFF59E0B) else Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(plan.description, fontSize = 12.sp, color = textSecondary)

            if (isStarted) {
                Spacer(Modifier.height(10.dp))
                // Progress Bar
                val progressFrac = (completedDaysSet.size.toFloat() / plan.totalDays.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressFrac },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(plan.colorHex),
                    trackColor = Color(plan.colorHex).copy(0.2f)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${completedDaysSet.size}/${plan.totalDays} Days Completed", fontSize = 11.sp, color = textSecondary)
                    Text("${(progressFrac * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(plan.colorHex))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isStarted) {
                    Button(
                        onClick = onStartPlan,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(plan.colorHex))
                    ) {
                        Text("Start Plan", fontWeight = FontWeight.Bold)
                    }
                } else {
                    val todayRoutine = plan.dailyRoutines.getOrNull(currentDay - 1) ?: plan.dailyRoutines.first()
                    Button(
                        onClick = { onStartDay(todayRoutine) },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(plan.colorHex))
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Day $currentDay Practice", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onTogglePause,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isPaused) "Resume" else "Pause", fontSize = 11.sp)
                    }

                    IconButton(onClick = onResetPlan) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Restart Plan", tint = textSecondary)
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand days"
                    )
                }
            }

            // Expandable Daily Routine Tasks
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                Spacer(Modifier.height(8.dp))
                Text("Daily Tasks Roadmap:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(Modifier.height(6.dp))

                plan.dailyRoutines.take(14).forEach { routine ->
                    val isDayDone = completedDaysSet.contains(routine.dayNumber)
                    val isToday = routine.dayNumber == currentDay

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) Color(plan.colorHex).copy(0.12f) else Color.Transparent)
                            .clickable { onStartDay(routine) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDayDone) Icons.Filled.CheckCircle else if (isToday) Icons.Filled.PlayCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isDayDone) Color(0xFF10B981) else if (isToday) Color(plan.colorHex) else textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routine.title,
                                fontSize = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = textPrimary
                            )
                            Text("${routine.durationMinutes} min • ${routine.poseIds.size} Poses", fontSize = 10.5.sp, color = textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomRoutineCard(
    routine: CustomYogaRoutineEntity,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val poseCount = routine.poseIds.split(",").filter { it.isNotBlank() }.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10B981).copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🧘‍♂️", fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(routine.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("$poseCount Poses • ${routine.category} • Rest ${routine.restIntervalSeconds}s", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(
                onClick = onStart,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── 5. Yoga Analytics & Achievements Tab ─────────────────────────────────────
@Composable
private fun YogaAnalyticsTab(
    weeklyAnalytics: YogaRepository.WeeklyAnalyticsData?,
    monthlyAnalytics: YogaRepository.MonthlyAnalyticsData?,
    achievements: List<YogaAchievementEntity>,
    onOpenReminders: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Reminders & Notification CTA
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenReminders() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF10B981).copy(0.15f) else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF10B981).copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏰", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Practice Reminders", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("Set daily mindfulness reminder schedule", fontSize = 11.5.sp, color = textSecondary)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color(0xFF10B981))
                }
            }
        }

        // Weekly Mon-Sun Practice Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("WEEKLY PRACTICE TREND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("Monday – Sunday Minutes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Text(
                            "${weeklyAnalytics?.totalMinutes ?: 0} Total Mins",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Bar Chart
                    val dayMinutes = weeklyAnalytics?.dayMinutes ?: mapOf(
                        "Mon" to 0, "Tue" to 0, "Wed" to 0, "Thu" to 0, "Fri" to 0, "Sat" to 0, "Sun" to 0
                    )
                    val maxMins = (dayMinutes.values.maxOrNull() ?: 1).coerceAtLeast(30)

                    Row(
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayMinutes.forEach { (day, mins) ->
                            val heightFraction = (mins.toFloat() / maxMins.toFloat()).coerceIn(0.08f, 1f)
                            val isTopDay = mins == weeklyAnalytics?.bestDayMinutes && mins > 0

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (mins > 0) "${mins}m" else "-",
                                    fontSize = 9.sp,
                                    color = if (isTopDay) Color(0xFF10B981) else textSecondary,
                                    fontWeight = if (isTopDay) FontWeight.Bold else FontWeight.Normal
                                )
                                val barBrush = if (isTopDay) {
                                    Brush.verticalGradient(listOf(Color(0xFF06B6D4), Color(0xFF10B981)))
                                } else if (mins > 0) {
                                    Brush.verticalGradient(listOf(Color(0xFF10B981).copy(0.7f), Color(0xFF10B981).copy(0.3f)))
                                } else {
                                    val inactiveColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                    Brush.verticalGradient(listOf(inactiveColor, inactiveColor))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(barBrush)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = if (isTopDay) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTopDay) textPrimary else textSecondary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                    Spacer(Modifier.height(12.dp))

                    // Weekly Summary Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Daily Average", fontSize = 10.5.sp, color = textSecondary)
                            Text("${weeklyAnalytics?.averageDailyMinutes ?: 0} min", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Best Day", fontSize = 10.5.sp, color = textSecondary)
                            Text(weeklyAnalytics?.bestDayName ?: "None", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Completion Rate", fontSize = 10.5.sp, color = textSecondary)
                            Text("${weeklyAnalytics?.completionRatePercent ?: 0}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }
                }
            }
        }

        // Monthly Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("MONTHLY CONSISTENCY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                    Text("Practice Summary This Month", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        YogaMiniPillar("🕒", "Total Time", "${monthlyAnalytics?.totalMinutes ?: 0}m", Modifier.weight(1f))
                        YogaMiniPillar("🧘", "Sessions", "${monthlyAnalytics?.totalSessions ?: 0}", Modifier.weight(1f))
                        YogaMiniPillar("📈", "Consistency", "${monthlyAnalytics?.consistencyPercent ?: 0}%", Modifier.weight(1f))
                        YogaMiniPillar("🔥", "Max Streak", "${monthlyAnalytics?.longestStreak ?: 0}d", Modifier.weight(1f))
                    }
                }
            }
        }

        // Achievements Header
        val unlockedCount = achievements.count { it.isUnlocked }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Yoga Milestones", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("$unlockedCount / ${achievements.size} Unlocked", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
        }

        // Achievements List
        items(achievements) { ach ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (ach.isUnlocked) Color(0xFF10B981).copy(0.10f) else cardBg
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                border = BorderStroke(
                    1.dp,
                    if (ach.isUnlocked) Color(0xFF10B981).copy(0.5f) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (ach.isUnlocked) Color(0xFF10B981).copy(0.2f) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ach.icon, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ach.title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text(ach.description, fontSize = 11.5.sp, color = textSecondary)
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (ach.isUnlocked) Color(0xFF10B981) else Color.Gray.copy(0.2f)
                    ) {
                        Text(
                            text = if (ach.isUnlocked) "UNLOCKED" else "LOCKED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (ach.isUnlocked) Color.White else textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Pose Detail View ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YogaPoseDetailView(
    pose: DetailedYogaPose,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onStartSinglePose: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val screenBg = if (isDark) Color(0xFF090D16) else Color(0xFFF4F7FB)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    val poseImage = rememberYogaPoseImage(pose.id)
    var currentStepIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = { Text(pose.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFEC4899) else textPrimary
                        )
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Yoga Pose: ${pose.name} (${pose.sanskritName})")
                            putExtra(Intent.EXTRA_TEXT, "Learn ${pose.name} (${pose.sanskritName}) on AuraHealth!\n\nBenefits:\n${pose.benefits.joinToString("\n• ", prefix = "• ")}\n\nInstructions:\n${pose.steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Pose"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
            )
        },
        bottomBar = {
            Surface(
                color = cardBg,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartSinglePose,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start This Pose (${pose.holdSeconds}s)", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Large Pose Image / Illustration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (poseImage != null) {
                            Image(
                                bitmap = poseImage,
                                contentDescription = pose.name,
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(pose.emoji, fontSize = 84.sp)
                        }
                    }
                }
            }

            // Pose Header & Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(pose.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                                Text(pose.sanskritName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
                            }

                            val lvlColor = when (pose.level) {
                                "Beginner" -> Color(0xFF10B981)
                                "Intermediate" -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                            Surface(shape = RoundedCornerShape(50), color = lvlColor.copy(0.15f)) {
                                Text(pose.level, color = lvlColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(pose.description, fontSize = 13.sp, color = textSecondary, lineHeight = 18.sp)

                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            YogaMiniPillar("⏱️", "Hold Time", "${pose.holdSeconds}s", Modifier.weight(1f))
                            YogaMiniPillar("🔥", "Calories", "${(pose.caloriesPerMin * pose.holdSeconds / 60).toInt().coerceAtLeast(2)} kcal", Modifier.weight(1f))
                            YogaMiniPillar("📍", "Target Areas", pose.targetBodyAreas.split(',').firstOrNull() ?: "", Modifier.weight(1f))
                        }
                    }
                }
            }

            // Step-by-Step Instructions with step selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Step-by-Step Instructions", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Spacer(Modifier.height(12.dp))

                        // Step Buttons
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pose.steps.forEachIndexed { i, _ ->
                                val isSelected = currentStepIndex == i
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF10B981) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                        .clickable { currentStepIndex = i }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Step ${i + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF10B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${currentStepIndex + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = pose.steps.getOrElse(currentStepIndex) { "" },
                                    fontSize = 13.sp,
                                    color = textPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Breathing Instructions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF06B6D4).copy(0.12f) else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌬️", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("Pranayama & Breathing Guidance", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(pose.breathingInstructions, fontSize = 12.5.sp, color = textPrimary, lineHeight = 17.sp)
                    }
                }
            }

            // Benefits Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Key Benefits", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Spacer(Modifier.height(10.dp))
                        pose.benefits.forEach { b ->
                            Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(b, fontSize = 12.5.sp, color = textPrimary)
                            }
                        }
                    }
                }
            }

            // Safety Precautions & Beginner Modifications
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(0.35f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 18.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("Safety & Precautions", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                        Spacer(Modifier.height(8.dp))
                        pose.precautions.forEach { p ->
                            Text("• $p", fontSize = 12.sp, color = textSecondary, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Beginner Modification:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text(pose.beginnerModifications, fontSize = 12.sp, color = textSecondary)
                    }
                }
            }
        }
    }
}

// ── Guided Yoga Session Runner (Accurate Lifecycle-Aware Timer) ──────────────
@Composable
fun GuidedYogaSessionRunner(
    sessionTitle: String,
    category: String,
    poses: List<DetailedYogaPose>,
    restSeconds: Int = 10,
    onSaveSession: (title: String, category: String, plannedSecs: Int, completedSecs: Int, posesDone: Int, totalPoses: Int, isDone: Boolean) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    if (poses.isEmpty()) {
        LaunchedEffect(Unit) { onExit() }
        return
    }

    var currentPoseIndex by remember { mutableIntStateOf(0) }
    val currentPose = poses.getOrNull(currentPoseIndex) ?: poses.first()
    val nextPose = poses.getOrNull(currentPoseIndex + 1)

    val currentPoseDuration = currentPose.holdSeconds
    var currentPoseRemaining by remember(currentPoseIndex) { mutableIntStateOf(currentPoseDuration) }

    var isResting by remember { mutableStateOf(false) }
    var restRemaining by remember { mutableIntStateOf(restSeconds) }

    var isPaused by remember { mutableStateOf(false) }
    var isSessionComplete by remember { mutableStateOf(false) }
    var showQuitConfirmation by remember { mutableStateOf(false) }

    var totalElapsedSeconds by remember { mutableIntStateOf(0) }
    val totalPlannedSeconds = remember(poses) { poses.sumOf { it.holdSeconds } }

    val poseImage = rememberYogaPoseImage(currentPose.id)

    // Accurate Timer loop
    LaunchedEffect(isPaused, isSessionComplete, isResting, currentPoseIndex) {
        while (!isPaused && !isSessionComplete) {
            delay(1000)
            if (!isPaused && !isSessionComplete) {
                if (isResting) {
                    if (restRemaining > 1) {
                        restRemaining--
                    } else {
                        // Rest finished -> move to next pose
                        isResting = false
                        playTibetanBell(context)
                        if (currentPoseIndex < poses.size - 1) {
                            currentPoseIndex++
                        } else {
                            isSessionComplete = true
                        }
                    }
                } else {
                    totalElapsedSeconds++
                    if (currentPoseRemaining > 1) {
                        currentPoseRemaining--
                    } else {
                        // Current pose finished!
                        playTibetanBell(context)
                        if (currentPoseIndex < poses.size - 1) {
                            isResting = true
                            restRemaining = restSeconds
                        } else {
                            isSessionComplete = true
                            onSaveSession(sessionTitle, category, totalPlannedSeconds, totalElapsedSeconds, poses.size, poses.size, true)
                        }
                    }
                }
            }
        }
    }

    val screenBg = if (isDark) Color(0xFF090D16) else Color(0xFFF4F7FB)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    if (isSessionComplete) {
        // Session Complete Celebration View
        YogaSessionCompleteCelebration(
            sessionTitle = sessionTitle,
            completedSeconds = totalElapsedSeconds,
            posesCount = poses.size,
            caloriesBurned = ((totalElapsedSeconds / 60f) * 3.5f).toInt().coerceAtLeast(5),
            onClose = onExit
        )
    } else {
        Scaffold(
            containerColor = screenBg,
            topBar = {
                Surface(color = cardBg, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showQuitConfirmation = true }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = textPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(sessionTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1)
                            Text("Pose ${currentPoseIndex + 1} of ${poses.size}", fontSize = 11.5.sp, color = Color(0xFF10B981))
                        }
                        IconButton(onClick = { playTibetanBell(context) }) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = "Chime", tint = Color(0xFF10B981))
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Overall Session Progress Bar
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    val overallProgress = (totalElapsedSeconds.toFloat() / totalPlannedSeconds.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF10B981),
                        trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Elapsed: ${formatSecs(totalElapsedSeconds)}", fontSize = 10.5.sp, color = textSecondary)
                        val remainingTotal = (totalPlannedSeconds - totalElapsedSeconds).coerceAtLeast(0)
                        Text("Remaining: ${formatSecs(remainingTotal)}", fontSize = 10.5.sp, color = textSecondary)
                    }
                }

                // Pose Artwork / Animation Center Box
                Card(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.2.dp, if (isResting) Color(0xFF06B6D4).copy(0.4f) else Color(0xFF10B981).copy(0.4f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isResting) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🧘", fontSize = 56.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("REST & BREATHE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                                Text("Next: ${nextPose?.name ?: "Finish"}", fontSize = 12.sp, color = textSecondary)
                            }
                        } else {
                            if (poseImage != null) {
                                Image(
                                    bitmap = poseImage,
                                    contentDescription = currentPose.name,
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(currentPose.emoji, fontSize = 72.sp)
                            }
                        }
                    }
                }

                // Pose Title & Sanskrit
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isResting) {
                        Text("Transitioning...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("Prepare for next pose", fontSize = 13.sp, color = textSecondary)
                    } else {
                        Text(currentPose.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                        Text(currentPose.sanskritName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
                    }
                }

                // Current Pose Countdown Circle Timer
                val poseProgress = if (isResting) {
                    restRemaining.toFloat() / restSeconds.toFloat()
                } else {
                    currentPoseRemaining.toFloat() / currentPoseDuration.toFloat()
                }
                val animPoseProgress by animateFloatAsState(targetValue = poseProgress, animationSpec = tween(500), label = "pose_timer")

                Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(140.dp)) {
                        drawArc(
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = if (isResting) Color(0xFF06B6D4) else Color(0xFF10B981),
                            startAngle = -90f,
                            sweepAngle = 360f * animPoseProgress,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isResting) "$restRemaining" else "$currentPoseRemaining",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                        Text(
                            text = if (isResting) "Rest Seconds" else "Seconds Hold",
                            fontSize = 10.5.sp,
                            color = textSecondary
                        )
                    }
                }

                // Next Pose Preview Card
                if (nextPose != null && !isResting) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏭️", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("UP NEXT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                Text("${nextPose.name} (${nextPose.holdSeconds}s)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            }
                        }
                    }
                }

                // Timer Controls (Previous, Play/Pause, Next)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Pose
                    IconButton(
                        onClick = {
                            if (currentPoseIndex > 0) {
                                isResting = false
                                currentPoseIndex--
                                currentPoseRemaining = poses[currentPoseIndex].holdSeconds
                            }
                        },
                        enabled = currentPoseIndex > 0
                    ) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous Pose", modifier = Modifier.size(36.dp), tint = textPrimary)
                    }

                    // Play / Pause Button
                    Surface(
                        onClick = { isPaused = !isPaused },
                        shape = CircleShape,
                        color = Color(0xFF10B981),
                        modifier = Modifier.size(64.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Skip to Next Pose
                    IconButton(
                        onClick = {
                            if (currentPoseIndex < poses.size - 1) {
                                isResting = false
                                currentPoseIndex++
                                currentPoseRemaining = poses[currentPoseIndex].holdSeconds
                            } else {
                                isSessionComplete = true
                                onSaveSession(sessionTitle, category, totalPlannedSeconds, totalElapsedSeconds, poses.size, poses.size, true)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next Pose", modifier = Modifier.size(36.dp), tint = textPrimary)
                    }
                }
            }
        }

        // Confirmation Dialog before quitting incomplete session
        if (showQuitConfirmation) {
            AlertDialog(
                onDismissRequest = { showQuitConfirmation = false },
                title = { Text("End Session Early?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("You've practiced for ${formatSecs(totalElapsedSeconds)}. Would you like to save your progress or discard this session?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showQuitConfirmation = false
                            onSaveSession(sessionTitle, category, totalPlannedSeconds, totalElapsedSeconds, currentPoseIndex + 1, poses.size, false)
                            onExit()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Save & Exit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuitConfirmation = false; onExit() }) {
                        Text("Discard & Exit", color = Color(0xFFEF4444))
                    }
                }
            )
        }
    }
}

@Composable
private fun YogaSessionCompleteCelebration(
    sessionTitle: String,
    completedSeconds: Int,
    posesCount: Int,
    caloriesBurned: Int,
    onClose: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val screenBg = if (isDark) Color(0xFF090D16) else Color(0xFFF4F7FB)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    Box(
        modifier = Modifier.fillMaxSize().background(screenBg).statusBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✨ Namaste ✨", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                Spacer(Modifier.height(6.dp))
                Text("Session Completed!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(sessionTitle, fontSize = 13.sp, color = textSecondary)

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    YogaMiniPillar("⏱️", "Duration", "${completedSeconds / 60}m ${completedSeconds % 60}s", Modifier.weight(1f))
                    YogaMiniPillar("🧘", "Poses", "$posesCount", Modifier.weight(1f))
                    YogaMiniPillar("🔥", "Calories", "$caloriesBurned kcal", Modifier.weight(1f))
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Return to Sanctuary", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── 6. Dedicated Breathing Exercise View ─────────────────────────────────────
@Composable
fun YogaBreathingExerciseView(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val screenBg = if (isDark) Color(0xFF090D16) else Color(0xFFF4F7FB)
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    // Breathing Patterns
    data class Pattern(val name: String, val inhale: Int, val holdIn: Int, val exhale: Int, val holdOut: Int, val desc: String)
    val patterns = listOf(
        Pattern("Box Breathing", 4, 4, 4, 4, "Equal rhythm to lower stress and cultivate razor-sharp focus."),
        Pattern("Deep Breathing", 4, 2, 6, 0, "Calming diaphragmatic respiration activating parasympathetic tone."),
        Pattern("Relaxation (4-7-8)", 4, 7, 8, 0, "Dr. Weil's relaxing breath for bedtime and rapid anxiety relief."),
        Pattern("Equal Flow", 5, 0, 5, 0, "Balanced inhale & exhale for gentle mental stabilization.")
    )

    var selectedPattern by remember { mutableStateOf(patterns[0]) }
    var isRunning by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf("Inhale") } // Inhale, Hold, Exhale, Hold Out
    var secondsInPhase by remember { mutableIntStateOf(selectedPattern.inhale) }
    var totalCycles by remember { mutableIntStateOf(0) }

    // Breathing circle scale animation
    val targetScale = when (currentPhase) {
        "Inhale" -> 1.35f
        "Hold", "Hold In" -> 1.35f
        "Exhale" -> 0.85f
        else -> 0.85f
    }
    val animScale by animateFloatAsState(
        targetValue = if (isRunning) targetScale else 1.0f,
        animationSpec = tween(
            durationMillis = (secondsInPhase * 1000).coerceAtLeast(1000),
            easing = LinearOutSlowInEasing
        ),
        label = "breath_scale"
    )

    // Breathing Timer Loop
    LaunchedEffect(isRunning, selectedPattern) {
        while (isRunning) {
            // Phase 1: Inhale
            currentPhase = "Inhale"
            for (s in selectedPattern.inhale downTo 1) {
                secondsInPhase = s
                delay(1000)
            }
            // Phase 2: Hold In (if > 0)
            if (selectedPattern.holdIn > 0) {
                currentPhase = "Hold In"
                for (s in selectedPattern.holdIn downTo 1) {
                    secondsInPhase = s
                    delay(1000)
                }
            }
            // Phase 3: Exhale
            currentPhase = "Exhale"
            for (s in selectedPattern.exhale downTo 1) {
                secondsInPhase = s
                delay(1000)
            }
            // Phase 4: Hold Out (if > 0)
            if (selectedPattern.holdOut > 0) {
                currentPhase = "Hold Out"
                for (s in selectedPattern.holdOut downTo 1) {
                    secondsInPhase = s
                    delay(1000)
                }
            }
            totalCycles++
            playTibetanBell(context)
        }
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            Surface(color = cardBg, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BREATHING EXERCISE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                        Text(selectedPattern.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }
                    IconButton(onClick = { playTibetanBell(context) }) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = "Chime", tint = Color(0xFF06B6D4))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Pattern Selector Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(patterns) { p ->
                    FilterChip(
                        selected = selectedPattern == p,
                        onClick = {
                            selectedPattern = p
                            isRunning = false
                            currentPhase = "Inhale"
                            secondsInPhase = p.inhale
                        },
                        label = { Text(p.name, fontSize = 12.sp) }
                    )
                }
            }

            // Pattern Description
            Text(
                text = selectedPattern.desc,
                fontSize = 12.5.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // Animated Breathing Orb Centerpiece
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Expanding / Contracting Gradient Orb
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(animScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF06B6D4).copy(alpha = 0.5f),
                                    Color(0xFF10B981).copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Inner Orb Center with Phase & Count
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .border(2.dp, Color(0xFF06B6D4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isRunning) currentPhase else "Ready",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF06B6D4)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isRunning) "$secondsInPhase" else "Start",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                    }
                }
            }

            // Rhythm Pill (Inhale - Hold - Exhale - Hold)
            Surface(
                shape = RoundedCornerShape(50),
                color = cardBg,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("In: ${selectedPattern.inhale}s", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                    if (selectedPattern.holdIn > 0) Text("Hold: ${selectedPattern.holdIn}s", fontSize = 11.5.sp, color = textSecondary)
                    Text("Out: ${selectedPattern.exhale}s", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    if (selectedPattern.holdOut > 0) Text("Empty: ${selectedPattern.holdOut}s", fontSize = 11.5.sp, color = textSecondary)
                }
            }

            // Safety Warning Notice
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Safety Note: If you experience any dizziness, discomfort, or shortness of breath, please stop immediately and breathe naturally.",
                        fontSize = 10.5.sp,
                        color = textPrimary,
                        lineHeight = 14.sp
                    )
                }
            }

            // Start / Pause Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { isRunning = !isRunning },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) Color(0xFFF59E0B) else Color(0xFF06B6D4)),
                    modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)
                ) {
                    Icon(if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRunning) "Pause Breathing" else "Begin Breathwork", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Goal Settings Dialog ─────────────────────────────────────────────────────
@Composable
fun YogaGoalSettingsDialog(
    currentGoal: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var goalMinutes by remember { mutableIntStateOf(currentGoal) }
    val presets = listOf(10, 15, 20, 30, 45, 60)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.4f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Daily Practice Goal ⚙️", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { if (goalMinutes > 5) goalMinutes -= 5 },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = Color(0xFF10B981))
                    }
                    Text("$goalMinutes min", fontSize = 28.sp, fontWeight = FontWeight.Black)
                    IconButton(
                        onClick = { if (goalMinutes < 120) goalMinutes += 5 },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = Color(0xFF10B981))
                    }
                }

                Spacer(Modifier.height(14.dp))
                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presets.forEach { p ->
                        OutlinedButton(
                            onClick = { goalMinutes = p },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (goalMinutes == p) Color(0xFF10B981) else Color.Gray.copy(0.4f))
                        ) {
                            Text("${p}m", fontSize = 11.sp, fontWeight = if (goalMinutes == p) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(goalMinutes) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Save Goal")
                    }
                }
            }
        }
    }
}

// ── Reminder Settings Dialog ─────────────────────────────────────────────────
@Composable
fun YogaReminderSettingsDialog(
    isEnabled: Boolean,
    reminderTime: Pair<Int, Int>,
    onSave: (Boolean, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var enabled by remember { mutableStateOf(isEnabled) }
    var hour by remember { mutableIntStateOf(reminderTime.first) }
    var minute by remember { mutableIntStateOf(reminderTime.second) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.4f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Practice Reminders ⏰", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Daily Reminder", fontSize = 14.sp)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                if (enabled) {
                    Spacer(Modifier.height(14.dp))
                    Text("Reminder Time: ${String.format("%02d:%02d", hour, minute)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Pair(6, 30), Pair(7, 0), Pair(8, 0), Pair(18, 0), Pair(20, 0)).forEach { (h, m) ->
                            OutlinedButton(
                                onClick = { hour = h; minute = m },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(String.format("%02d:%02d", h, m), fontSize = 10.5.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(enabled, hour, minute) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ── Create Custom Routine Dialog ─────────────────────────────────────────────
@Composable
fun CreateCustomRoutineDialog(
    allPoses: List<DetailedYogaPose>,
    onSave: (name: String, category: String, poseIds: List<String>, durations: List<Int>, restSecs: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var routineName by remember { mutableStateOf("My Custom Flow") }
    var selectedCategory by remember { mutableStateOf(YogaCategoryNames.FULL_BODY) }
    val selectedPoseIds = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.4f)),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Create Custom Routine 🧘", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    label = { Text("Routine Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))
                Text("Select Poses (${selectedPoseIds.size} selected):", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allPoses) { pose ->
                        val isSelected = selectedPoseIds.contains(pose.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedPoseIds.remove(pose.id)
                                    else selectedPoseIds.add(pose.id)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = {
                                if (it) selectedPoseIds.add(pose.id) else selectedPoseIds.remove(pose.id)
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(pose.emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(pose.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${pose.holdSeconds}s • ${pose.level}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedPoseIds.isNotEmpty()) {
                                val durations = selectedPoseIds.map { id ->
                                    allPoses.find { it.id == id }?.holdSeconds ?: 45
                                }
                                onSave(routineName, selectedCategory, selectedPoseIds, durations, 10)
                            }
                        },
                        enabled = selectedPoseIds.isNotEmpty() && routineName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Save Routine")
                    }
                }
            }
        }
    }
}

// ── Time Formatter Helper ────────────────────────────────────────────────────
private fun formatSecs(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
