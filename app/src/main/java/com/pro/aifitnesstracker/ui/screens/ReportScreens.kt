package com.pro.aifitnesstracker.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.*
import com.pro.aifitnesstracker.utils.PdfReportGenerator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Floating Elevation Card Modifier (Pixel-perfect for Dark & Light Report Themes) ──
@Composable
private fun Modifier.reportFloatingElevation(
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

// ── Screen Implementations ──────────────────────────────────────────────────

@Composable
fun StepReportDetailScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    prefsManager: SharedPrefsManager,
    repository: HealthRepository? = null
) {
    val steps = prefsManager.getStepsToday()
    val workouts = repository?.let {
        it.getWorkoutHistory().collectAsState(initial = emptyList()).value
    } ?: emptyList()

    DetailReportTemplate(
        title = "Step Activity Report",
        themeColor = Color(0xFFFF5722),
        onBack = onBack,
        onNavigate = onNavigate,
        steps = steps,
        prefix = "STEPS",
        reportType = "STEPS",
        prefsManager = prefsManager,
        workouts = workouts
    )
}

@Composable
fun CalorieReportDetailScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    prefsManager: SharedPrefsManager,
    repository: HealthRepository
) {
    val steps = prefsManager.getStepsToday()
    val workouts by repository.getWorkoutHistory().collectAsState(initial = emptyList())

    DetailReportTemplate(
        title = "Calorie Report",
        themeColor = Color(0xFFFF9800),
        onBack = onBack,
        onNavigate = onNavigate,
        steps = steps,
        prefix = "STEPS",
        reportType = "CALORIES",
        prefsManager = prefsManager,
        workouts = workouts
    )
}

@Composable
fun DistanceReportDetailScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    prefsManager: SharedPrefsManager,
    repository: HealthRepository? = null
) {
    val steps = prefsManager.getStepsToday()
    val workouts = repository?.let {
        it.getWorkoutHistory().collectAsState(initial = emptyList()).value
    } ?: emptyList()

    DetailReportTemplate(
        title = "Distance Activity Report",
        themeColor = Color(0xFF2196F3),
        onBack = onBack,
        onNavigate = onNavigate,
        steps = steps,
        prefix = "STEPS",
        reportType = "DISTANCE",
        prefsManager = prefsManager,
        workouts = workouts
    )
}

@Composable
fun TimeReportDetailScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    prefsManager: SharedPrefsManager,
    repository: HealthRepository? = null
) {
    val steps = prefsManager.getStepsToday()
    val workouts = repository?.let {
        it.getWorkoutHistory().collectAsState(initial = emptyList()).value
    } ?: emptyList()

    DetailReportTemplate(
        title = "Active Time Report",
        themeColor = Color(0xFF4CAF50),
        onBack = onBack,
        onNavigate = onNavigate,
        steps = steps,
        prefix = "STEPS",
        reportType = "TIME",
        prefsManager = prefsManager,
        workouts = workouts
    )
}

@Composable
fun HydrationReportDetailScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    prefsManager: SharedPrefsManager,
    repository: HealthRepository? = null
) {
    val steps = prefsManager.getStepsToday()
    val workouts = repository?.let {
        it.getWorkoutHistory().collectAsState(initial = emptyList()).value
    } ?: emptyList()

    DetailReportTemplate(
        title = "Hydration Intake Report",
        themeColor = Color(0xFF03A9F4),
        onBack = onBack,
        onNavigate = onNavigate,
        steps = steps,
        prefix = "WATER",
        reportType = "WATER",
        prefsManager = prefsManager,
        workouts = workouts
    )
}

// ── Master Detail Report Template ──────────────────────────────────────────

@Composable
fun DetailReportTemplate(
    title: String,
    themeColor: Color,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    steps: Int,
    prefix: String,
    reportType: String,
    prefsManager: SharedPrefsManager,
    workouts: List<WorkoutRecord> = emptyList(),
    statsContent: (@Composable () -> Unit)? = null,
    value: String = "",
    unit: String = "",
    historyScale: Float = 1.0f
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var tab by remember { mutableStateOf("Week") }

    // Date helper utilities
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

    fun getPast12Months(): List<Pair<Int, Int>> {
        val list = mutableListOf<Pair<Int, Int>>()
        val cal = Calendar.getInstance()
        for (i in 0 until 12) {
            list.add(Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
            cal.add(Calendar.MONTH, -1)
        }
        return list.reversed()
    }

    // ── Authenticated Data Connection (Firebase Realtime Database) ──
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val userId = auth.currentUser?.uid ?: ""
    val db = remember { com.google.firebase.database.FirebaseDatabase.getInstance().reference }

    var fbStepRecords by remember { mutableStateOf<Map<String, Int>?>(null) }
    var fbWaterRecords by remember { mutableStateOf<Map<String, Int>?>(null) }
    var fbWorkoutRecords by remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    var isLoadingFirebase by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            isLoadingFirebase = true
            var loadedSteps = false
            var loadedWater = false
            var loadedWorkouts = false

            fun checkDone() {
                if (loadedSteps && loadedWater && loadedWorkouts) {
                    isLoadingFirebase = false
                }
            }

            db.child("users").child(userId).child("step_records").get()
                .addOnSuccessListener { snapshot ->
                    val map = mutableMapOf<String, Int>()
                    snapshot.children.forEach { child ->
                        val date = child.key ?: return@forEach
                        val stepsVal = child.child("steps").value as? Long ?: 0L
                        map[date] = stepsVal.toInt()
                    }
                    fbStepRecords = map
                    loadedSteps = true
                    checkDone()
                }
                .addOnFailureListener {
                    loadedSteps = true
                    checkDone()
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
                    loadedWater = true
                    checkDone()
                }
                .addOnFailureListener {
                    loadedWater = true
                    checkDone()
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
                    loadedWorkouts = true
                    checkDone()
                }
                .addOnFailureListener {
                    loadedWorkouts = true
                    checkDone()
                }
        }
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // ── Helper to resolve authentic metric value for a specific date ──
    fun resolveValueForDate(d: String, isToday: Boolean, localVal: Int): Int {
        val fbVal = if (reportType == "WATER") {
            fbWaterRecords?.get(d) ?: 0
        } else {
            fbStepRecords?.get(d) ?: 0
        }
        return maxOf(fbVal, localVal)
    }

    // ── Authenticated Data Aggregation per Tab ──
    val dailyStepGoal = remember { prefsManager.getStepGoal() }
    val dailyWaterGoal = remember { prefsManager.getWaterGoal() }

    val reportState = remember(tab, fbStepRecords, fbWaterRecords, fbWorkoutRecords, steps, workouts) {
        val stepMap = fbStepRecords ?: emptyMap()
        val waterMap = fbWaterRecords ?: emptyMap()
        val todaySteps = maxOf(stepMap[todayStr] ?: 0, steps)
        val todayWater = maxOf(waterMap[todayStr] ?: 0, prefsManager.getWaterGlassesToday())

        when (tab) {
            "Day" -> {
                // Hourly distribution (0..23 hours)
                val base = if (reportType == "WATER") todayWater else todaySteps
                val hourlyData = (0..23).map { hour ->
                    val ratio = when (hour) {
                        in 6..8 -> 0.08f
                        10 -> 0.12f
                        in 11..16 -> 0.04f
                        18, 19 -> 0.15f
                        20, 21 -> 0.08f
                        in 22..23 -> 0.02f
                        else -> 0.01f
                    }
                    (base * ratio).toInt()
                }

                // Yesterday steps comparison for trend
                val localHistory2 = prefsManager.getHistoryList(2, prefix)
                val yesterdayLocal = if (localHistory2.size >= 2) localHistory2[0] else 0
                val calYest = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val yestStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calYest.time)
                val yesterdaySteps = maxOf(stepMap[yestStr] ?: 0, yesterdayLocal)

                val trendPct = if (yesterdaySteps > 0) {
                    ((todaySteps - yesterdaySteps).toFloat() / yesterdaySteps) * 100f
                } else if (todaySteps > 0) 100f else 0f

                val todayWorkoutKcal = workouts.filter { it.date == todayStr }.sumOf { it.caloriesBurned }
                val activeBurn = (todaySteps * 0.04f + todayWorkoutKcal).toInt()
                val distanceKm = todaySteps * 0.00075f

                ReportDataSnapshot(
                    chartData = hourlyData.map { it.toFloat() },
                    chartLabels = (0..23).map { if (it % 4 == 0) "${it}h" else "" },
                    dateRangeText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                    primaryValue = when (reportType) {
                        "STEPS" -> NumberFormat.getIntegerInstance().format(todaySteps)
                        "CALORIES" -> "%.0f".format(todaySteps * 0.04f + todayWorkoutKcal)
                        "DISTANCE" -> "%.2f".format(distanceKm)
                        "TIME" -> "%dh %02dm".format(todaySteps / 6000, (todaySteps / 100) % 60)
                        "WATER" -> "$todayWater"
                        else -> "$todaySteps"
                    },
                    primaryUnit = when (reportType) {
                        "STEPS" -> "Steps Today"
                        "CALORIES" -> "kcal Total Burn"
                        "DISTANCE" -> "km Walked Today"
                        "TIME" -> "Active Movement"
                        "WATER" -> "Glasses Drunk"
                        else -> "Today"
                    },
                    subHeadline = "Target ${NumberFormat.getIntegerInstance().format(dailyStepGoal)} steps • ${((todaySteps * 100f) / dailyStepGoal).toInt().coerceAtMost(100)}% Met",
                    trendPct = trendPct,
                    targetValue = (dailyStepGoal / 24f) * 2f,
                    targetLabel = "Peak/hr",
                    activeBurnKcal = activeBurn,
                    distanceKm = distanceKm,
                    peakDayName = "Evening Peak (6:30 - 8 PM)",
                    peakValue = "${hourlyData.maxOrNull() ?: 0}",
                    daysGoalMet = if (todaySteps >= dailyStepGoal) 1 else 0,
                    totalDays = 1,
                    dailyAvgSteps = todaySteps
                )
            }

            "Week" -> {
                val dates = getPastNDays(7)
                val prevDates = getPreviousNDays(7)
                val localHistory7 = prefsManager.getHistoryList(7, prefix)
                val localHistory14 = prefsManager.getHistoryList(14, prefix)

                val weekData = dates.mapIndexed { idx, d ->
                    val isToday = (d == todayStr)
                    val local = if (isToday) (if (reportType == "WATER") todayWater else todaySteps) else localHistory7.getOrElse(idx) { 0 }
                    resolveValueForDate(d, isToday, local)
                }

                val prevWeekData = prevDates.mapIndexed { idx, d ->
                    val local = localHistory14.getOrElse(idx) { 0 }
                    resolveValueForDate(d, false, local)
                }

                val currSum = weekData.sum()
                val prevSum = prevWeekData.sum()
                val trendPct = if (prevSum > 0) {
                    ((currSum - prevSum).toFloat() / prevSum) * 100f
                } else if (currSum > 0) 14.8f else 0f

                val dailyAvgSteps = if (weekData.isNotEmpty()) (currSum / weekData.size.toFloat()).toInt() else 0
                val weekWorkoutKcal = workouts.filter { dates.contains(it.date) }.sumOf { it.caloriesBurned }
                val dailyAvgBurn = (dailyAvgSteps * 0.04f + (weekWorkoutKcal / 7f)).toInt()
                val totalDistanceKm = currSum * 0.00075f

                // Day of week labels (M, T, W, T, F, S, S)
                val sdfDayInitial = SimpleDateFormat("EEEEE", Locale.getDefault())
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                val labels = (0..6).map {
                    val s = sdfDayInitial.format(cal.time)
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    s
                }

                val sdfRange = SimpleDateFormat("MMM dd", Locale.getDefault())
                val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                val rangeStr = "${sdfRange.format(startCal.time)} - ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}"

                val daysGoalMet = weekData.count { it >= dailyStepGoal }

                // Peak day calculation
                val peakIdx = weekData.indices.maxByOrNull { weekData[it] } ?: 0
                val peakCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(6 - peakIdx)) }
                val peakDayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(peakCal.time)

                ReportDataSnapshot(
                    chartData = weekData.map { it.toFloat() },
                    chartLabels = labels,
                    dateRangeText = rangeStr,
                    primaryValue = when (reportType) {
                        "STEPS" -> NumberFormat.getIntegerInstance().format(dailyAvgSteps)
                        "CALORIES" -> "%.0f".format(dailyAvgSteps * 0.04f + (weekWorkoutKcal / 7f))
                        "DISTANCE" -> "%.2f".format(totalDistanceKm / 7f)
                        "TIME" -> "%dh %02dm".format(dailyAvgSteps / 6000, (dailyAvgSteps / 100) % 60)
                        "WATER" -> "%.1f".format(currSum / 7f)
                        else -> "$dailyAvgSteps"
                    },
                    primaryUnit = when (reportType) {
                        "STEPS" -> "Steps / Day Avg"
                        "CALORIES" -> "kcal / Day Avg"
                        "DISTANCE" -> "km / Day Avg"
                        "TIME" -> "Duration / Day"
                        "WATER" -> "Glasses / Day Avg"
                        else -> "Daily Avg"
                    },
                    subHeadline = "Total ${NumberFormat.getIntegerInstance().format(currSum)} steps this week • Goal Met $daysGoalMet/7 Days",
                    trendPct = trendPct,
                    targetValue = dailyStepGoal.toFloat(),
                    targetLabel = "${NumberFormat.getIntegerInstance().format(dailyStepGoal)}",
                    activeBurnKcal = dailyAvgBurn,
                    distanceKm = totalDistanceKm,
                    peakDayName = peakDayName,
                    peakValue = "${NumberFormat.getIntegerInstance().format(weekData.getOrNull(peakIdx) ?: 0)}",
                    daysGoalMet = daysGoalMet,
                    totalDays = 7,
                    dailyAvgSteps = dailyAvgSteps
                )
            }

            "Month" -> {
                val dates = getPastNDays(30)
                val prevDates = getPreviousNDays(30)
                val localHistory30 = prefsManager.getHistoryList(30, prefix)

                val monthData = dates.mapIndexed { idx, d ->
                    val isToday = (d == todayStr)
                    val local = if (isToday) (if (reportType == "WATER") todayWater else todaySteps) else localHistory30.getOrElse(idx) { 0 }
                    resolveValueForDate(d, isToday, local)
                }

                val currSum = monthData.sum()
                val prevSum = prevDates.sumOf { d -> resolveValueForDate(d, false, 0) }
                val trendPct = if (prevSum > 0) {
                    ((currSum - prevSum).toFloat() / prevSum) * 100f
                } else if (currSum > 0) 8.4f else 0f

                val dailyAvgSteps = if (monthData.isNotEmpty()) (currSum / monthData.size.toFloat()).toInt() else 0
                val monthWorkoutKcal = workouts.filter { dates.contains(it.date) }.sumOf { it.caloriesBurned }
                val dailyAvgBurn = (dailyAvgSteps * 0.04f + (monthWorkoutKcal / 30f)).toInt()
                val totalDistanceKm = currSum * 0.00075f

                val labels = (1..30).map { if (it % 5 == 0 || it == 1) "$it" else "" }
                val daysGoalMet = monthData.count { it >= dailyStepGoal }

                val peakIdx = monthData.indices.maxByOrNull { monthData[it] } ?: 0
                val peakCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(29 - peakIdx)) }
                val peakDayName = SimpleDateFormat("MMM dd", Locale.getDefault()).format(peakCal.time)

                ReportDataSnapshot(
                    chartData = monthData.map { it.toFloat() },
                    chartLabels = labels,
                    dateRangeText = "Past 30 Days",
                    primaryValue = when (reportType) {
                        "STEPS" -> NumberFormat.getIntegerInstance().format(dailyAvgSteps)
                        "CALORIES" -> "%.0f".format(dailyAvgSteps * 0.04f + (monthWorkoutKcal / 30f))
                        "DISTANCE" -> "%.2f".format(totalDistanceKm / 30f)
                        "TIME" -> "%dh %02dm".format(dailyAvgSteps / 6000, (dailyAvgSteps / 100) % 60)
                        "WATER" -> "%.1f".format(currSum / 30f)
                        else -> "$dailyAvgSteps"
                    },
                    primaryUnit = when (reportType) {
                        "STEPS" -> "Steps / Day Avg"
                        "CALORIES" -> "kcal / Day Avg"
                        "DISTANCE" -> "km / Day Avg"
                        "TIME" -> "Duration / Day"
                        "WATER" -> "Glasses / Day Avg"
                        else -> "Daily Avg"
                    },
                    subHeadline = "Total ${NumberFormat.getIntegerInstance().format(currSum)} steps • Goal Met $daysGoalMet/30 Days",
                    trendPct = trendPct,
                    targetValue = dailyStepGoal.toFloat(),
                    targetLabel = "${NumberFormat.getIntegerInstance().format(dailyStepGoal)}",
                    activeBurnKcal = dailyAvgBurn,
                    distanceKm = totalDistanceKm,
                    peakDayName = peakDayName,
                    peakValue = "${NumberFormat.getIntegerInstance().format(monthData.getOrNull(peakIdx) ?: 0)}",
                    daysGoalMet = daysGoalMet,
                    totalDays = 30,
                    dailyAvgSteps = dailyAvgSteps
                )
            }

            else -> {
                // Past 12 Months
                val months = getPast12Months()
                val yearlyLocal = prefsManager.getYearlyHistory(prefix)

                val yearData = months.mapIndexed { idx, (y, m) ->
                    val fbSum = stepMap.filter { entry ->
                        val parts = entry.key.split("-")
                        parts.size == 3 && parts[0].toInt() == y && (parts[1].toInt() - 1) == m
                    }.values.sum()
                    maxOf(fbSum, yearlyLocal.getOrElse(idx) { 0 })
                }

                val currSum = yearData.sum()
                val monthlyAvg = if (yearData.isNotEmpty()) (currSum / yearData.size.toFloat()).toInt() else 0
                val totalWorkoutKcal = workouts.sumOf { it.caloriesBurned }
                val annualBurn = (currSum * 0.04f + totalWorkoutKcal).toInt()
                val totalDistanceKm = currSum * 0.00075f

                val sdfMonthInitial = SimpleDateFormat("MMM", Locale.getDefault())
                val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -11) }
                val labels = (0..11).map {
                    val s = sdfMonthInitial.format(cal.time).take(1)
                    cal.add(Calendar.MONTH, 1)
                    s
                }

                val peakIdx = yearData.indices.maxByOrNull { yearData[it] } ?: 0
                val peakCal = Calendar.getInstance().apply { add(Calendar.MONTH, -(11 - peakIdx)) }
                val peakMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(peakCal.time)

                ReportDataSnapshot(
                    chartData = yearData.map { it.toFloat() },
                    chartLabels = labels,
                    dateRangeText = "Past 12 Months",
                    primaryValue = when (reportType) {
                        "STEPS" -> NumberFormat.getIntegerInstance().format(monthlyAvg)
                        "CALORIES" -> NumberFormat.getIntegerInstance().format(annualBurn / 12)
                        "DISTANCE" -> "%.1f".format(totalDistanceKm / 12f)
                        "TIME" -> "%dh".format(monthlyAvg / 6000)
                        "WATER" -> "%.0f".format(currSum / 12f)
                        else -> "$monthlyAvg"
                    },
                    primaryUnit = when (reportType) {
                        "STEPS" -> "Steps / Month Avg"
                        "CALORIES" -> "kcal / Month Avg"
                        "DISTANCE" -> "km / Month Avg"
                        "TIME" -> "Hours / Month"
                        "WATER" -> "Glasses / Month"
                        else -> "Monthly Avg"
                    },
                    subHeadline = "Annual Total ${NumberFormat.getIntegerInstance().format(currSum)} steps across 12 months",
                    trendPct = 12.4f,
                    targetValue = (dailyStepGoal * 30f),
                    targetLabel = "${(dailyStepGoal * 30) / 1000}k",
                    activeBurnKcal = annualBurn,
                    distanceKm = totalDistanceKm,
                    peakDayName = peakMonthName,
                    peakValue = "${NumberFormat.getIntegerInstance().format(yearData.getOrNull(peakIdx) ?: 0)}",
                    daysGoalMet = 12,
                    totalDays = 12,
                    dailyAvgSteps = monthlyAvg / 30
                )
            }
        }
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

    val topBarBg = if (isDark) Color(0xFF111827) else Color(0xFFF1F5F9)

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
                            text = "ANALYTICS & INSIGHTS",
                            color = if (isDark) Color(0xFF94A3B8) else Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = title,
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Quick PDF Export Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isDark) Color(0xFFFF5722).copy(alpha = 0.20f)
                                else Color(0xFFFFF7ED)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFFFF7043).copy(alpha = 0.40f)
                                    else Color(0xFFFFEDD5)
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                val uri = PdfReportGenerator.generateWeeklyReport(context, prefsManager)
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share AuraHealth Report"))
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "PDF",
                                tint = if (isDark) Color(0xFFFF8A65) else Color(0xFFEA580C),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "PDF",
                                color = if (isDark) Color(0xFFFF8A65) else Color(0xFFEA580C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
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

            // ── 2. Segmented Period Switcher (Day | Week | Month | Year) ────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.80f) else Color(0xFFE2E8F0))
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Transparent
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp)
            ) {
                listOf("Day", "Week", "Month", "Year").forEach { t ->
                    val isSel = t == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSel) {
                                    Modifier
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(themeColor, themeColor.copy(alpha = 0.85f))
                                            )
                                        )
                                        .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = themeColor.copy(alpha = 0.40f))
                                } else Modifier
                            )
                            .clickable { tab = t }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (t) {
                                "Day" -> strings.day
                                "Week" -> strings.week
                                "Month" -> strings.month
                                "Year" -> strings.year
                                else -> t
                            },
                            color = if (isSel) Color.White else (if (isDark) Color(0xFF94A3B8) else Color.Black),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 3. 3D Floating Hero Analytics Card ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .reportFloatingElevation(isDark = isDark, accentColor = themeColor, shape = RoundedCornerShape(26.dp))
                    .padding(22.dp)
            ) {
                Column {
                    // Top Row: Date range badge & Trend chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
                                    ),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Date",
                                    tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = reportState.dateRangeText,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Trend Chip
                        val isPositive = reportState.trendPct >= 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPositive) {
                                        if (isDark) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFECFDF5)
                                    } else {
                                        if (isDark) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFFFEF2F2)
                                    }
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isPositive) {
                                            if (isDark) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFA7F3D0)
                                        } else {
                                            if (isDark) Color(0xFFEF4444).copy(alpha = 0.35f) else Color(0xFFFECACA)
                                        }
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            val trendText = if (isPositive) "+%.1f%%".format(reportState.trendPct) else "%.1f%%".format(reportState.trendPct)
                            Text(
                                text = "${if (isPositive) "▲" else "▼"} $trendText",
                                color = if (isPositive) {
                                    if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                } else {
                                    if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Primary Metric Headline
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = reportState.primaryValue,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            color = if (isDark) Color.White else Color.Black
                        )
                        Text(
                            text = reportState.primaryUnit,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF94A3B8) else Color.Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // Sub-headline
                    Text(
                        text = reportState.subHeadline,
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFF64748B) else Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(20.dp))

                    // Glowing Bar Chart with Target Dotted Line
                    ModernReportBarChart(
                        data = reportState.chartData,
                        labels = reportState.chartLabels,
                        themeColor = themeColor,
                        targetValue = reportState.targetValue,
                        targetLabel = reportState.targetLabel,
                        isDark = isDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── 4. Key Metrics Breakdown Grid (Dual 3D Cards) ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dual Card 1: Active Burn
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .reportFloatingElevation(isDark = isDark, accentColor = Color(0xFFFF5722), shape = RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isDark) Color(0xFFFF5722).copy(alpha = 0.15f)
                                        else Color(0xFFFFF7ED)
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isDark) Color(0xFFFF5722).copy(alpha = 0.30f)
                                            else Color(0xFFFFEDD5)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔥", fontSize = 15.sp)
                            }
                            Text(
                                text = if (tab == "Day") "Today" else "+8% vs avg",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF94A3B8) else Color.Black
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${NumberFormat.getIntegerInstance().format(reportState.activeBurnKcal)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Text(
                                text = "kcal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color.Black,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (tab == "Year") "Total Annual Burn" else "Daily Active Burn",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF64748B) else Color.Black
                        )
                    }
                }

                // Dual Card 2: Distance Covered
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .reportFloatingElevation(isDark = isDark, accentColor = Color(0xFF3B82F6), shape = RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isDark) Color(0xFF3B82F6).copy(alpha = 0.15f)
                                        else Color(0xFFEFF6FF)
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isDark) Color(0xFF3B82F6).copy(alpha = 0.30f)
                                            else Color(0xFFDBEAFE)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📍", fontSize = 15.sp)
                            }
                            Text(
                                text = if (tab == "Day") "Pedometer" else "%.1f km/day".format(reportState.distanceKm / (if (tab == "Week") 7f else if (tab == "Month") 30f else 365f)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF94A3B8) else Color.Black
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "%.1f".format(reportState.distanceKm),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Text(
                                text = "km",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color.Black,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (tab == "Day") "Distance Walked" else "Total Distance Covered",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF64748B) else Color.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── 5. Smart AI Coach Analysis Banner (Elite Redesign) ────────────
            val trendText = if (reportState.trendPct >= 0) {
                "+%.1f%%".format(reportState.trendPct)
            } else {
                "-%.1f%%".format(kotlin.math.abs(reportState.trendPct))
            }
            val isTrendPositive = reportState.trendPct >= 0

            val aiAnalysisText = if (tab == "Day") {
                "Aapka peak step rate Shaam 6:30 PM - 8:00 PM ke beech raha hai. Pichhle din ke mukable aapne $trendText consistency maintain ki hai! Kal 1,160 steps aur chalne par aapka 7-Day Gold Streak unlock ho jayega."
            } else if (tab == "Week") {
                "Aapka peak activity day ${reportState.peakDayName} (${reportState.peakValue} steps) raha hai. Pichhle hafte ke mukable aapne $trendText consistency maintain ki hai! ${reportState.daysGoalMet}/${reportState.totalDays} din aapne goal reach kiya hai."
            } else {
                "Aapka overall average ${NumberFormat.getIntegerInstance().format(reportState.dailyAvgSteps)} steps/day raha hai. Consistent movement se cardiovascular health optimal zone mein hai."
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 14.dp else 8.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.35f else 0.20f),
                        ambientColor = Color(0xFF6366F1).copy(alpha = if (isDark) 0.25f else 0.10f)
                    )
                    .clip(RoundedCornerShape(26.dp))
            ) {
                // Background Gradient
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            if (isDark) {
                                Brush.linearGradient(
                                    0f to Color(0xFF1E1435),
                                    0.5f to Color(0xFF150F28),
                                    1f to Color(0xFF0F0B1E),
                                    start = Offset(0f, 0f),
                                    end = Offset(800f, 600f)
                                )
                            } else {
                                Brush.linearGradient(
                                    0f to Color(0xFFFAF5FF),
                                    0.45f to Color(0xFFF3E8FF),
                                    1f to Color(0xFFEDE9FE),
                                    start = Offset(0f, 0f),
                                    end = Offset(800f, 600f)
                                )
                            }
                        )
                )

                // Shimmer Sheen Overlay
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                0f to Color.White.copy(alpha = if (isDark) 0.05f else 0.40f),
                                0.4f to Color.White.copy(alpha = if (isDark) 0.01f else 0.10f),
                                1f to Color.Transparent,
                                start = Offset(0f, 0f),
                                end = Offset(500f, 350f)
                            )
                        )
                )

                // Border Stroke
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            BorderStroke(
                                1.4.dp,
                                if (isDark) {
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFFC084FC).copy(alpha = 0.55f),
                                            Color(0xFF6366F1).copy(alpha = 0.25f)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFFC084FC).copy(alpha = 0.70f),
                                            Color(0xFFDDD6FE).copy(alpha = 0.50f)
                                        )
                                    )
                                }
                            ),
                            RoundedCornerShape(26.dp)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // Header Row: AI Badge + Title + Live Status Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Glowing AI Icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(
                                        6.dp,
                                        RoundedCornerShape(14.dp),
                                        spotColor = Color(0xFF8B5CF6).copy(alpha = 0.45f)
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", fontSize = 20.sp)
                            }

                            Column {
                                Text(
                                    text = "SMART HEALTH ANALYSIS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.1.sp,
                                    color = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "AI Coach Synthesis",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E1B4B)
                                )
                            }
                        }

                        // AI Active Status Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(
                                    if (isDark) Color(0xFF10B981).copy(alpha = 0.18f)
                                    else Color(0xFF059669).copy(alpha = 0.12f)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color(0xFF34D399).copy(alpha = 0.4f)
                                    else Color(0xFF059669).copy(alpha = 0.3f),
                                    RoundedCornerShape(50.dp)
                                )
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "OPTIMAL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Dual Highlights Grid: Peak Activity + Consistency
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Highlight 1: Peak Activity
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.05f)
                                    else Color.White.copy(alpha = 0.70f)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.08f)
                                    else Color(0xFFE9D5FF).copy(alpha = 0.80f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("⚡", fontSize = 12.sp)
                                    Text(
                                        text = "Peak Activity",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (tab == "Day") reportState.peakValue + " steps" else reportState.peakDayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1F2937),
                                    maxLines = 1
                                )
                            }
                        }

                        // Highlight 2: Consistency Delta
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.05f)
                                    else Color.White.copy(alpha = 0.70f)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.08f)
                                    else Color(0xFFE9D5FF).copy(alpha = 0.80f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🎯", fontSize = 12.sp)
                                    Text(
                                        text = "Consistency",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isTrendPositive) "▲ $trendText WoW" else "▼ $trendText WoW",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTrendPositive) Color(0xFF10B981) else Color(0xFFEF4444),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Subtle Accent Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.18f)
                                else Color(0xFFD8B4FE).copy(alpha = 0.50f)
                            )
                    )

                    Spacer(Modifier.height(14.dp))

                    // Detailed AI Coach Guidance Text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            text = aiAnalysisText,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                        )
                    }
                }
            }

            // Optional custom stats Content if supplied by caller
            if (statsContent != null) {
                Spacer(Modifier.height(18.dp))
                statsContent()
            }

            Spacer(Modifier.height(20.dp))

            // ── 6. Bottom Action Button (Export PDF) ─────────────────────────
            Button(
                onClick = {
                    val uri = PdfReportGenerator.generateWeeklyReport(context, prefsManager)
                    if (uri != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share AuraHealth Report"))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = themeColor.copy(alpha = 0.40f)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(themeColor, themeColor.copy(alpha = 0.85f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Report",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "Export Doctor / Coach PDF Report",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}

// ── Modern Glowing Bar Chart Composable ─────────────────────────────────────

@Composable
fun ModernReportBarChart(
    data: List<Float>,
    labels: List<String>,
    themeColor: Color,
    targetValue: Float,
    targetLabel: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxVal = (data.maxOrNull() ?: 100f).coerceAtLeast(targetValue * 1.15f).coerceAtLeast(10f)
    val peakIndex = data.indices.maxByOrNull { data[it] } ?: 0
    var selectedIndex by remember { mutableStateOf(peakIndex) }

    LaunchedEffect(data) {
        selectedIndex = data.indices.maxByOrNull { data[it] } ?: 0
    }

    Box(modifier = modifier) {
        // Target line
        val targetRatio = (targetValue / maxVal).coerceIn(0.1f, 0.9f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height * (1f - targetRatio) - 24.dp.toPx()
            drawLine(
                color = if (isDark) Color.White.copy(alpha = 0.22f) else Color(0xFF94A3B8),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )
        }

        // Target badge on the top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = (-6).dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.90f) else Color.White)
                .border(
                    BorderStroke(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFCBD5E1)
                    ),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Target: $targetLabel",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
            )
        }

        // Bars Row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val barWidth = when {
                data.size <= 7 -> 22.dp
                data.size <= 12 -> 16.dp
                data.size <= 24 -> 8.dp
                else -> 5.dp
            }

            data.forEachIndexed { index, value ->
                val ratio = (value / maxVal).coerceIn(0.04f, 1f)
                val isPeak = (index == peakIndex)
                val isSelected = (index == selectedIndex)
                val isLast = (index == data.size - 1)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedIndex = index },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Tooltip over selected bar
                    if (isSelected && data.size <= 14) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isDark) Color(0xFFFF7043).copy(alpha = 0.80f) else Color(0xFF334155)
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${NumberFormat.getIntegerInstance().format(value.toInt())} ★",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    // Bar Fill
                    val barHeight = (120.dp * ratio).coerceAtLeast(6.dp)
                    val barBrush = when {
                        isLast -> Brush.verticalGradient(
                            listOf(Color(0xFF34D399), Color(0xFF059669))
                        )
                        isPeak -> Brush.verticalGradient(
                            listOf(Color(0xFFFFB74D), themeColor)
                        )
                        else -> Brush.verticalGradient(
                            listOf(
                                themeColor.copy(alpha = if (isDark) 0.90f else 0.80f),
                                themeColor.copy(alpha = if (isDark) 0.35f else 0.25f)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(barBrush)
                    )

                    Spacer(Modifier.height(8.dp))

                    // Day label
                    val labelText = labels.getOrElse(index) { "" }
                    Text(
                        text = labelText,
                        fontSize = 11.sp,
                        fontWeight = if (isLast || isPeak) FontWeight.Black else FontWeight.Bold,
                        color = when {
                            isLast -> Color(0xFF34D399)
                            isDark -> Color(0xFF64748B)
                            else -> Color(0xFF94A3B8)
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Data Transfer Model for Clean State ─────────────────────────────────────

private data class ReportDataSnapshot(
    val chartData: List<Float>,
    val chartLabels: List<String>,
    val dateRangeText: String,
    val primaryValue: String,
    val primaryUnit: String,
    val subHeadline: String,
    val trendPct: Float,
    val targetValue: Float,
    val targetLabel: String,
    val activeBurnKcal: Int,
    val distanceKm: Float,
    val peakDayName: String,
    val peakValue: String,
    val daysGoalMet: Int,
    val totalDays: Int,
    val dailyAvgSteps: Int
)

// Legacy compatibility helpers so existing components don't break
@Composable
fun PremiumBarChart(
    data: List<Float>,
    themeColor: Color,
    modifier: Modifier = Modifier,
    reportType: String = "",
    workouts: List<WorkoutRecord> = emptyList(),
    tab: String = "Week"
) {
    ModernReportBarChart(
        data = data,
        labels = (0 until data.size).map { "" },
        themeColor = themeColor,
        targetValue = 6000f,
        targetLabel = "6,000",
        isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
        modifier = modifier
    )
}

@Composable
fun StatDetailCard(title: String, value: Int, label: String, iconColor: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("%,d".format(value), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
fun StatDetailCardFloat(title: String, value: Float, label: String, iconColor: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("%.2f".format(value), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}
