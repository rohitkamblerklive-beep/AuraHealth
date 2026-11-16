package com.pro.aifitnesstracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.HealthRepository
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Floating Elevation Card Modifier for Hub ──
@Composable
private fun Modifier.hubFloatingElevation(
    isDark: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(26.dp)
): Modifier {
    val cardBgGradient = Brush.verticalGradient(
        colors = if (isDark) listOf(Color(0xFF131B2E), Color(0xFF0C1220))
        else listOf(Color(0xFFFFFFFF), Color(0xFFFAFCFF))
    )
    val borderGradient = Brush.verticalGradient(
        colors = if (isDark) listOf(
            Color.White.copy(alpha = 0.18f),
            accentColor.copy(alpha = 0.35f),
            Color.Transparent
        ) else listOf(
            Color.White,
            Color(0xFFCBD5E1).copy(alpha = 0.7f)
        )
    )
    return this
        .shadow(
            elevation = if (isDark) 12.dp else 8.dp,
            shape = shape,
            ambientColor = if (isDark) Color.Black.copy(alpha = 0.75f) else Color(0x1F0F172A),
            spotColor = accentColor.copy(alpha = if (isDark) 0.30f else 0.12f)
        )
        .clip(shape)
        .background(cardBgGradient)
        .border(BorderStroke(1.2.dp, borderGradient), shape)
}

@Composable
fun PerformanceTrendsScreen(
    prefsManager: SharedPrefsManager,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    repository: HealthRepository? = null
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // ── Authenticated Data Connection (Firebase Realtime Database) ──
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val userId = auth.currentUser?.uid ?: ""
    val db = remember { com.google.firebase.database.FirebaseDatabase.getInstance().reference }

    var fbStepRecords by remember { mutableStateOf<Map<String, Int>?>(null) }
    var fbWaterRecords by remember { mutableStateOf<Map<String, Int>?>(null) }
    var fbWorkoutRecords by remember { mutableStateOf<List<Map<String, Any>>?>(null) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            db.child("users").child(userId).child("step_records").get()
                .addOnSuccessListener { snapshot ->
                    val map = mutableMapOf<String, Int>()
                    snapshot.children.forEach { child ->
                        val date = child.key ?: return@forEach
                        val stepsVal = child.child("steps").value as? Long ?: 0L
                        map[date] = stepsVal.toInt()
                    }
                    fbStepRecords = map
                }

            db.child("users").child(userId).child("water_records").get()
                .addOnSuccessListener { snapshot ->
                    val map = mutableMapOf<String, Int>()
                    snapshot.children.forEach { child ->
                        val date = child.key ?: return@forEach
                        val glassesVal = child.child("glasses").value as? Long ?: 0L
                        map[date] = glassesVal.toInt()
                    }
                    fbWaterRecords = map
                }

            db.child("users").child(userId).child("workout_records").get()
                .addOnSuccessListener { snapshot ->
                    val list = mutableListOf<Map<String, Any>>()
                    snapshot.children.forEach { child ->
                        @Suppress("UNCHECKED_CAST")
                        val data = child.value as? Map<String, Any>
                        if (data != null) list.add(data)
                    }
                    fbWorkoutRecords = list
                }
        }
    }

    // Workouts from Room Database
    val roomWorkouts = repository?.let {
        it.getWorkoutHistory().collectAsState(initial = emptyList()).value
    } ?: emptyList()

    // Date generation for past 7 days and previous 7 days (trend comparison)
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val currentWeekNumber = remember {
        val cal = Calendar.getInstance()
        cal.get(Calendar.WEEK_OF_YEAR)
    }

    fun getPastNDays(n: Int): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 0 until n) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return list.reversed()
    }

    fun getPreviousNDays(n: Int): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -n)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 0 until n) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return list.reversed()
    }

    // ── Authenticated Metrics Computation ──
    val dailyStepGoal = remember { prefsManager.getStepGoal().coerceAtLeast(1000) }
    val dailyWaterGoal = remember { prefsManager.getWaterGoal().coerceAtLeast(1) }

    val hubState = remember(fbStepRecords, fbWaterRecords, fbWorkoutRecords, roomWorkouts) {
        val stepMap = fbStepRecords ?: emptyMap()
        val waterMap = fbWaterRecords ?: emptyMap()
        val past7Days = getPastNDays(7)
        val prev7Days = getPreviousNDays(7)

        val localSteps7 = prefsManager.getHistoryList(7, "STEPS")
        val localSteps14 = prefsManager.getHistoryList(14, "STEPS")
        val stepsToday = prefsManager.getStepsToday()

        val localWater7 = prefsManager.getHistoryList(7, "WATER")
        val waterToday = prefsManager.getWaterGlassesToday()

        // 1. STEPS (7 Days)
        val weekSteps = past7Days.mapIndexed { idx, d ->
            val isToday = (d == todayStr)
            val fbVal = stepMap[d] ?: 0
            val localVal = if (isToday) stepsToday else localSteps7.getOrElse(idx) { 0 }
            maxOf(fbVal, localVal)
        }
        val prevWeekSteps = prev7Days.mapIndexed { idx, d ->
            val fbVal = stepMap[d] ?: 0
            val localVal = localSteps14.getOrElse(idx) { 0 }
            maxOf(fbVal, localVal)
        }
        val totalWeekSteps = weekSteps.sum()
        val avgSteps = if (weekSteps.isNotEmpty()) totalWeekSteps / weekSteps.size else 0
        val prevTotalSteps = prevWeekSteps.sum()
        val stepGrowthPct = if (prevTotalSteps > 0) {
            ((totalWeekSteps - prevTotalSteps).toFloat() / prevTotalSteps) * 100f
        } else if (totalWeekSteps > 0) 14.8f else 0f
        val daysGoalMet = weekSteps.count { it >= dailyStepGoal }

        // 2. CALORIES (7 Days: steps burn + workouts burn)
        val weekCalories = past7Days.mapIndexed { idx, d ->
            val st = weekSteps.getOrElse(idx) { 0 }
            val roomKcal = roomWorkouts.filter { it.date == d }.sumOf { it.caloriesBurned }
            val fbKcal = fbWorkoutRecords?.filter { it["date"] == d }?.sumOf { (it["caloriesBurned"] as? Number)?.toInt() ?: 0 } ?: 0
            val workoutKcal = maxOf(roomKcal, fbKcal)
            (st * 0.04f + workoutKcal).toInt()
        }
        val avgCalories = if (weekCalories.isNotEmpty()) weekCalories.average().toInt() else 0
        val calGrowthPct = (stepGrowthPct * 0.75f).coerceIn(-50f, 100f)

        // 3. DISTANCE (7 Days)
        val totalDistanceKm = totalWeekSteps * 0.00075f
        val weekDistances = weekSteps.map { it * 0.00075f }

        // 4. ACTIVE TIME (7 Days: steps / 100 = active minutes)
        val weekMinutes = weekSteps.map { it / 100 }
        val avgDailyMins = if (weekMinutes.isNotEmpty()) weekMinutes.average().toInt() else 0

        // 5. HYDRATION (7 Days)
        val weekWater = past7Days.mapIndexed { idx, d ->
            val isToday = (d == todayStr)
            val fbVal = waterMap[d] ?: 0
            val localVal = if (isToday) waterToday else localWater7.getOrElse(idx) { 0 }
            maxOf(fbVal, localVal)
        }
        val avgWater = if (weekWater.isNotEmpty()) weekWater.average() else 0.0
        val waterGoalPct = ((avgWater * 100f) / dailyWaterGoal).toInt().coerceAtMost(200)

        // Consistency Score Calculation (0-100)
        val stepRatio = (avgSteps.toFloat() / dailyStepGoal).coerceIn(0.1f, 1f)
        val waterRatio = (avgWater.toFloat() / dailyWaterGoal).coerceIn(0.1f, 1f)
        val timeRatio = (avgDailyMins.toFloat() / 30f).coerceIn(0.1f, 1f)
        val score = ((stepRatio * 45f) + (waterRatio * 30f) + (timeRatio * 25f)).toInt().coerceIn(10, 100)

        // Personal Best in 30 Days
        val history30 = prefsManager.getHistoryList(30, "STEPS")
        val maxStepDay = maxOf(stepMap.values.maxOrNull() ?: 0, history30.maxOrNull() ?: 0, stepsToday)

        PerformanceHubData(
            score = score,
            daysGoalMet = daysGoalMet,
            streakDays = prefsManager.getCurrentStreak(),
            weeklyGrowthPct = stepGrowthPct,
            avgSteps = avgSteps,
            stepSparkline = weekSteps.map { it.toFloat() },
            avgCalories = avgCalories,
            calorieSparkline = weekCalories.map { it.toFloat() },
            calGrowthPct = calGrowthPct,
            totalDistanceKm = totalDistanceKm,
            distanceSparkline = weekDistances,
            distanceGrowthPct = stepGrowthPct,
            avgDailyMins = avgDailyMins,
            timeSparkline = weekMinutes.map { it.toFloat() },
            avgWater = avgWater.toFloat(),
            waterSparkline = weekWater.map { it.toFloat() },
            waterGoalPct = waterGoalPct,
            maxStepRecord = maxStepDay
        )
    }

    // ── Screen Background Gradient ──
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF111827), Color(0xFF080B12), Color(0xFF030508))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF1F5F9), Color(0xFFEEF2F6), Color(0xFFE2E8F0))
        )
    }

    val topBarBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        topBar = {
            Surface(
                color = topBarBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color(0xFFF8FAFC) else Color.Black
                        )
                    }

                    // Centered Titles
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PERFORMANCE HUB",
                            color = if (isDark) Color(0xFF94A3B8) else Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Analytics & Trends",
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Current Week Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.White)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Week",
                                tint = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Week $currentWeekNumber",
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))

            // ── AI Comparison & Intelligence Hub Banner Card ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("AI_INTELLIGENCE_HUB") },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF131D33) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(
                    1.2.dp,
                    if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF16233B), Color(0xFF10192B))
                                else listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
                            )
                        )
                        .padding(18.dp)
                ) {
                    // 1. Top Header: Gradient Icon + Category Title + Action Arrow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF06B6D4), Color(0xFF10B981))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "AI INTELLIGENCE ENGINE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp,
                                        color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF06B6D4).copy(alpha = if (isDark) 0.25f else 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Tap to launch full diagnosis",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Arrow Forward Action Button
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Open Hub",
                                    tint = if (isDark) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 2. Main Title
                    Text(
                        text = "Performance Insights Hub",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color.Black,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(Modifier.height(4.dp))

                    // 3. Description text with comfortable reading layout
                    Text(
                        text = "Week-on-Week delta matrix & predictive goal simulator to optimize your athletic progress.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(Modifier.height(14.dp))

                    // 4. Feature Highlights Micro-Tags Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HubFeatureChip(
                            icon = Icons.Rounded.QueryStats,
                            label = "WoW Delta",
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        HubFeatureChip(
                            icon = Icons.Rounded.Insights,
                            label = "Goal Sim",
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        HubFeatureChip(
                            icon = Icons.Rounded.Psychology,
                            label = "Diagnostics",
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 2. 3D Floating Hero Score Card (Consistency Ring) ────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .hubFloatingElevation(
                        isDark = isDark,
                        accentColor = Color(0xFF6366F1),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // SVG-style Canvas Circular Progress Ring Gauge
                        Box(
                            modifier = Modifier.size(88.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(88.dp)) {
                                val strokeW = 8.dp.toPx()
                                val radius = (size.minDimension - strokeW) / 2
                                val centerOffset = Offset(size.width / 2, size.height / 2)

                                // Track background circle
                                drawCircle(
                                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                                    radius = radius,
                                    center = centerOffset,
                                    style = Stroke(width = strokeW)
                                )

                                // Progress sweep arc
                                val sweepAngle = (hubState.score / 100f) * 360f
                                drawArc(
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                                    topLeft = Offset(strokeW / 2, strokeW / 2),
                                    size = Size(radius * 2, radius * 2)
                                )
                            }

                            // Center score text
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${hubState.score}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color.Black,
                                    lineHeight = 26.sp
                                )
                                Text(
                                    text = "/100",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color.Black
                                )
                            }
                        }

                        Spacer(Modifier.width(18.dp))

                        // Score Info
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isDark) {
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF6366F1).copy(alpha = 0.30f),
                                                    Color(0xFFA855F7).copy(alpha = 0.20f)
                                                )
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                listOf(Color(0xFFF3E8FF), Color(0xFFEDE9FE))
                                            )
                                        }
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isDark) Color(0xFFA855F7).copy(alpha = 0.50f)
                                            else Color(0xFFD8B4FE)
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                val tierText = if (hubState.score >= 85) "★ PRO TIER PERFORMER" else if (hubState.score >= 70) "★ CONSISTENT ACHIEVER" else "★ PROGRESSING ATHLETE"
                                Text(
                                    text = tierText,
                                    color = if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = if (hubState.score >= 80) "Optimal Consistency" else "Building Momentum",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color.Black
                            )

                            Text(
                                text = "Aap is hafte top fitness users mein hain. ${hubState.daysGoalMet}/7 din target goals complete huye hain!",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // 3 Summary Quick Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(
                                    0.5.dp,
                                    if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)
                                )
                            )
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Goals Met Chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFF8FAFC))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0)
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${hubState.daysGoalMet} / 7 Days",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                )
                                Text(
                                    text = "Goals Met",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF64748B) else Color.Black,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Streak Chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFF8FAFC))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0)
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🔥 ${hubState.streakDays} Days",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFFF8A65) else Color(0xFFEA580C)
                                )
                                Text(
                                    text = "Active Streak",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF64748B) else Color.Black,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Growth Chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFF8FAFC))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0)
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val isPositive = hubState.weeklyGrowthPct >= 0
                                Text(
                                    text = "${if (isPositive) "+" else ""}%.1f%%".format(hubState.weeklyGrowthPct),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                )
                                Text(
                                    text = "Weekly Growth",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF64748B) else Color.Black,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // ── Section Title ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deep Dive Analytics Categories",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFE2E8F0) else Color.Black
                )
                Text(
                    text = "Tap to inspect",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color.Black
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── 3. Category Breakdown with 3D Floating Tiles ────────────────
            // Tile 1: STEPS TRACKING
            HubCategoryTile(
                icon = "🚶",
                iconColor = Color(0xFFFF5722),
                title = "Steps Tracking",
                subtitle = "Daily step count & target",
                valueText = NumberFormat.getIntegerInstance().format(hubState.avgSteps),
                valueSub = "avg",
                trendText = "▲ +%.1f%%".format(hubState.weeklyGrowthPct),
                trendColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                sparklineData = hubState.stepSparkline,
                isDark = isDark,
                onClick = { onNavigate("STEP_REPORT") }
            )

            // Tile 2: CALORIE REPORT
            HubCategoryTile(
                icon = "🔥",
                iconColor = Color(0xFFFF9800),
                title = "Calorie Report",
                subtitle = "Active energy expenditure",
                valueText = "${NumberFormat.getIntegerInstance().format(hubState.avgCalories)}",
                valueSub = "kcal",
                trendText = "▲ +%.1f%%".format(hubState.calGrowthPct),
                trendColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                sparklineData = hubState.calorieSparkline,
                isDark = isDark,
                onClick = { onNavigate("CALORIE_REPORT") }
            )

            // Tile 3: DISTANCE TRACKING
            HubCategoryTile(
                icon = "📍",
                iconColor = Color(0xFF2196F3),
                title = "Distance Covered",
                subtitle = "Walked & run distance",
                valueText = "%.1f".format(hubState.totalDistanceKm),
                valueSub = "km",
                trendText = "▲ +%.1f%%".format(hubState.distanceGrowthPct),
                trendColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                sparklineData = hubState.distanceSparkline,
                isDark = isDark,
                onClick = { onNavigate("DISTANCE_REPORT") }
            )

            // Tile 4: ACTIVE DURATION
            HubCategoryTile(
                icon = "⏱️",
                iconColor = Color(0xFF4CAF50),
                title = "Active Duration",
                subtitle = "Exercise & brisk walking",
                valueText = "${hubState.avgDailyMins / 60}h ${hubState.avgDailyMins % 60}m",
                valueSub = "/day",
                trendText = if (hubState.avgDailyMins >= 30) "🎯 Optimal" else "Progressing",
                trendColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                sparklineData = hubState.timeSparkline,
                isDark = isDark,
                onClick = { onNavigate("TIME_REPORT") }
            )

            // Tile 5: HYDRATION LEVEL
            HubCategoryTile(
                icon = "💧",
                iconColor = Color(0xFF03A9F4),
                title = "Hydration Level",
                subtitle = "Daily water consumption",
                valueText = "%.1f".format(hubState.avgWater),
                valueSub = "gls",
                trendText = "✓ ${hubState.waterGoalPct}% Goal",
                trendColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                sparklineData = hubState.waterSparkline,
                isDark = isDark,
                onClick = { onNavigate("HYDRATION_REPORT") }
            )

            // Tile 6: WEIGHT & BODY METRICS
            val currentW = prefsManager.getWeight().let { if (it <= 0f) 68.4f else it }
            val targetW = prefsManager.getTargetWeight().let { if (it <= 0f) 65.0f else it }
            val diffW = currentW - targetW
            HubCategoryTile(
                icon = "⚖️",
                iconColor = Color(0xFF2DD4BF),
                title = "Weight & Body Metrics",
                subtitle = "Target goal %.1f kg".format(targetW),
                valueText = "%.1f".format(currentW),
                valueSub = "kg",
                trendText = if (diffW > 0.05f) "-%.1f kg to go".format(diffW) else "Goal Reached!",
                trendColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488),
                sparklineData = listOf(0.85f, 0.80f, 0.74f, 0.68f, 0.62f, 0.58f, 0.50f),
                isDark = isDark,
                onClick = { onNavigate("WEIGHT_DETAIL") }
            )

            Spacer(Modifier.height(14.dp))

            // ── 3.5. Health Visualizations Card (Shifted from Dashboard) ─────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interactive Health Visualizations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFE2E8F0) else Color.Black
                )
                Text(
                    text = "6 Live Charts",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color.Black
                )
            }

            Spacer(Modifier.height(10.dp))

            VisualizationsPreviewCard(
                onNavigate = onNavigate,
                modifier = Modifier
            )


            Spacer(Modifier.height(18.dp))

            // 4. Personal Record Milestone Banner (Elite Design)
            val milestoneSteps = if (hubState.maxStepRecord > 0)
                NumberFormat.getIntegerInstance().format(hubState.maxStepRecord) else "---"
            val hasRecord = hubState.maxStepRecord > 0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color(0xFFEAB308).copy(alpha = if (isDark) 0.45f else 0.30f),
                        ambientColor = Color(0xFFF59E0B).copy(alpha = if (isDark) 0.25f else 0.15f)
                    )
                    .clip(RoundedCornerShape(28.dp))
            ) {
                // Background gradient
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            if (isDark)
                                Brush.linearGradient(
                                    0f to Color(0xFF1A1200),
                                    0.45f to Color(0xFF2D1E00),
                                    1f to Color(0xFF1C1400),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(1000f, 600f)
                                )
                            else
                                Brush.linearGradient(
                                    0f to Color(0xFFFBBF24),
                                    0.40f to Color(0xFFF59E0B),
                                    0.75f to Color(0xFFD97706),
                                    1f to Color(0xFFB45309),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(1000f, 600f)
                                )
                        )
                )
                // Shimmer overlay
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                0f to Color.White.copy(alpha = if (isDark) 0.04f else 0.22f),
                                0.5f to Color.White.copy(alpha = if (isDark) 0.0f else 0.06f),
                                1f to Color.White.copy(alpha = 0f),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(600f, 400f)
                            )
                        )
                )
                // Border overlay
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (isDark)
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFCD34D).copy(0.55f), Color(0xFFF59E0B).copy(0.20f))
                                    )
                                else
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFEF3C7).copy(0.80f), Color(0xFFFBBF24).copy(0.30f))
                                    )
                            ),
                            RoundedCornerShape(28.dp)
                        )
                )

                Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {

                    // Top Row: PERSONAL BEST chip + NEW RECORD badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(
                                    if (isDark) Color(0xFFFCD34D).copy(0.15f)
                                    else Color.White.copy(0.28f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text("\uD83C\uDFC6", fontSize = 11.sp)
                                Text(
                                    text = "PERSONAL BEST",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    color = if (isDark) Color(0xFFFCD34D) else Color.White
                                )
                            }
                        }

                        if (hasRecord) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isDark) Color(0xFF16A34A).copy(0.30f)
                                        else Color(0xFF15803D).copy(0.80f)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "NEW RECORD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp,
                                    color = Color(0xFF86EFAC)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Centre: Big Trophy Icon + Step Count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(
                                    12.dp,
                                    CircleShape,
                                    spotColor = Color(0xFFFCD34D).copy(0.70f),
                                    ambientColor = Color(0xFFF59E0B).copy(0.40f)
                                )
                                .clip(CircleShape)
                                .background(
                                    if (isDark)
                                        Brush.radialGradient(
                                            listOf(Color(0xFFFCD34D).copy(0.30f), Color(0xFFF59E0B).copy(0.10f))
                                        )
                                    else
                                        Brush.radialGradient(
                                            listOf(Color(0xFFFEF3C7), Color(0xFFFBBF24).copy(0.60f))
                                        )
                                )
                                .border(
                                    2.dp,
                                    if (isDark) Color(0xFFFCD34D).copy(0.45f)
                                    else Color.White.copy(0.70f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("\uD83C\uDFC6", fontSize = 32.sp)
                        }

                        Column {
                            Text(
                                text = milestoneSteps,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                color = if (isDark) Color(0xFFFCD34D) else Color.White,
                                lineHeight = 38.sp
                            )
                            Text(
                                text = "STEPS  -  THIS WEEK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = if (isDark) Color(0xFFFCD34D).copy(0.65f)
                                else Color.White.copy(0.78f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                if (isDark) Color(0xFFFCD34D).copy(0.15f)
                                else Color.White.copy(0.30f)
                            )
                    )

                    Spacer(Modifier.height(14.dp))

                    // Bottom: motivational message
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (hasRecord)
                                "Aapne apna best record banaya!\nMomentum banaye rakhein"
                            else
                                "8,000 steps ka target\nreach karke record banao!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            color = if (isDark) Color(0xFFFEF3C7).copy(0.90f)
                            else Color.White.copy(0.95f),
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDark) Color(0xFFFCD34D).copy(0.12f)
                                    else Color.White.copy(0.22f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (hasRecord) "\u2B50" else "\uD83D\uDD25", fontSize = 20.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}


// Hub Feature Chip Composable
@Composable
private fun HubFeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
            )
            .border(
                BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
            )
        }
    }
}

// Master Category Tile Composable with Live Sparkline
@Composable
private fun HubCategoryTile(
    icon: String,
    iconColor: Color,
    title: String,
    subtitle: String,
    valueText: String,
    valueSub: String,
    trendText: String,
    trendColor: Color,
    sparklineData: List<Float>,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 4.dp else 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isDark) Color.Black.copy(0.3f) else Color(0xFF64748B).copy(0.12f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Icon + Titles + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconColor.copy(alpha = if (isDark) 0.18f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 22.sp)
                }

                // Title + Subtitle - fills available width
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                        maxLines = 2
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 2
                    )
                }

                // Chevron
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp).align(Alignment.CenterVertically)
                )
            }

            // Row 2: Sparkline + Value + Trend
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mini sparkline bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.width(60.dp).height(28.dp)
                ) {
                    sparklineData.forEach { v ->
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight(v.coerceIn(0.15f, 1f))
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(trendColor.copy(alpha = 0.65f))
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Value + unit
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = valueText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                        )
                        if (valueSub.isNotEmpty()) {
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = valueSub,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = trendText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }
        }
    }
}

private data class PerformanceHubData(
    val score: Int = 0,
    val daysGoalMet: Int = 0,
    val streakDays: Int = 0,
    val weeklyGrowthPct: Float = 0f,
    val avgSteps: Int = 0,
    val stepSparkline: List<Float> = emptyList(),
    val avgCalories: Int = 0,
    val calorieSparkline: List<Float> = emptyList(),
    val calGrowthPct: Float = 0f,
    val totalDistanceKm: Float = 0f,
    val distanceSparkline: List<Float> = emptyList(),
    val distanceGrowthPct: Float = 0f,
    val avgDailyMins: Int = 0,
    val timeSparkline: List<Float> = emptyList(),
    val avgWater: Float = 0f,
    val waterSparkline: List<Float> = emptyList(),
    val waterGoalPct: Int = 0,
    val maxStepRecord: Int = 0
)
