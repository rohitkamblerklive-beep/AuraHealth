package com.pro.aifitnesstracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.HealthRepository
import com.pro.aifitnesstracker.LocalAppStrings

data class UnifiedActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val date: String,
    val timestamp: Long,
    val category: String, // "Workouts", "Yoga", "Steps"
    val badge: String,
    val icon: ImageVector,
    val accentColor: Color,
    val calories: Int = 0,
    val durationMinutes: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    onBack: () -> Unit,
    repository: HealthRepository
) {
    val strings = LocalAppStrings.current
    val yogaHistory by repository.getYogaHistory().collectAsState(initial = emptyList())
    val workoutHistory by repository.getWorkoutHistory().collectAsState(initial = emptyList())
    val stepHistory by repository.getStepHistory().collectAsState(initial = emptyList())

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    val cyanAccent = Color(0xFF06B6D4)
    val limeAccent = Color(0xFF84CC16)
    val orangeAccent = Color(0xFFF97316)
    val purpleAccent = Color(0xFFA855F7)
    val emeraldAccent = Color(0xFF10B981)

    // Build unified chronological activity list
    val allActivities = remember(yogaHistory, workoutHistory, stepHistory) {
        val list = mutableListOf<UnifiedActivityItem>()

        // 1. Workouts (Runs, Walks, Squats, Strength)
        workoutHistory.forEach { rec ->
            val (icon, color, badgeTitle, formattedTitle) = when {
                rec.workoutType.contains("Run", ignoreCase = true) -> {
                    Tuple4(Icons.AutoMirrored.Filled.DirectionsRun, limeAccent, "GPS RUN", "Outdoor Running")
                }
                rec.workoutType.contains("Walk", ignoreCase = true) -> {
                    Tuple4(Icons.AutoMirrored.Filled.DirectionsWalk, cyanAccent, "GPS WALK", "Outdoor Walking")
                }
                rec.workoutType.contains("Squat", ignoreCase = true) -> {
                    Tuple4(Icons.Default.FitnessCenter, orangeAccent, "CAMERA HUD", "Squats Workout")
                }
                else -> {
                    Tuple4(Icons.Default.FitnessCenter, emeraldAccent, "WORKOUT", "${rec.workoutType} Session")
                }
            }

            val subtitle = if (rec.workoutType.contains("Squat", ignoreCase = true)) {
                "${rec.repsOrMinutes} reps completed • ${rec.caloriesBurned} cal"
            } else {
                "${rec.repsOrMinutes} mins • ${rec.caloriesBurned} cal"
            }

            list.add(
                UnifiedActivityItem(
                    id = "workout_${rec.id}",
                    title = formattedTitle,
                    subtitle = subtitle,
                    date = rec.date,
                    timestamp = rec.timestamp,
                    category = "Workouts",
                    badge = badgeTitle,
                    icon = icon,
                    accentColor = color,
                    calories = rec.caloriesBurned,
                    durationMinutes = rec.repsOrMinutes
                )
            )
        }

        // 2. Yoga Asanas
        yogaHistory.forEach { rec ->
            val durationMin = (rec.durationSeconds / 60).coerceAtLeast(1)
            val formInfo = if (rec.accuracy > 0) "${rec.accuracy}% Form" else "Zen Flow"
            list.add(
                UnifiedActivityItem(
                    id = "yoga_${rec.id}",
                    title = rec.poseName,
                    subtitle = "${rec.durationSeconds}s held • $formInfo",
                    date = rec.date,
                    timestamp = rec.timestamp,
                    category = "Yoga",
                    badge = "YOGA ASANA",
                    icon = Icons.Default.SelfImprovement,
                    accentColor = purpleAccent,
                    calories = (rec.durationSeconds * 0.08f).toInt(),
                    durationMinutes = durationMin
                )
            )
        }

        // 3. Step History
        stepHistory.forEach { rec ->
            val stepKcal = (rec.steps * 0.04f).toInt()
            list.add(
                UnifiedActivityItem(
                    id = "step_${rec.date}",
                    title = "Daily Steps Total",
                    subtitle = "${rec.steps} steps recorded • $stepKcal kcal",
                    date = rec.date,
                    timestamp = 0L,
                    category = "Steps",
                    badge = "PEDOMETER",
                    icon = Icons.Default.DirectionsWalk,
                    accentColor = cyanAccent,
                    calories = stepKcal,
                    durationMinutes = (rec.steps / 100).coerceAtLeast(1)
                )
            )
        }

        // Sort descending by date/timestamp
        list.sortedWith(compareByDescending<UnifiedActivityItem> { it.date }.thenByDescending { it.timestamp })
    }

    var selectedFilter by remember { mutableStateOf("All") }

    val filteredActivities = remember(selectedFilter, allActivities) {
        when (selectedFilter) {
            "Workouts" -> allActivities.filter { it.category == "Workouts" }
            "Yoga" -> allActivities.filter { it.category == "Yoga" }
            "Steps" -> allActivities.filter { it.category == "Steps" }
            else -> allActivities
        }
    }

    // Cumulative stats
    val totalSessionsCount = allActivities.size
    val totalCaloriesBurned = allActivities.sumOf { it.calories }
    val totalMinutes = allActivities.sumOf { it.durationMinutes }
    val durationFormatted = if (totalMinutes >= 60) {
        "${totalMinutes / 60}h ${totalMinutes % 60}m"
    } else {
        "${totalMinutes}m"
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Surface(
                color = backgroundColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    // Centered Title
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.history.uppercase(java.util.Locale.US),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(cyanAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "WORKOUT LOGS • TIMELINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = cyanAccent,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Right Badge: Count of sessions
                    Surface(
                        color = cardColor,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Text(
                            text = "$totalSessionsCount LOGS",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        bottomBar = { AdBanner(adKey = "ad_activity_history_banner") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            // Hero Metric Summary Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(22.dp),
                color = cardColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Stat 1: Total Sessions
                    SummaryStatBox(
                        modifier = Modifier.weight(1f),
                        label = "SESSIONS",
                        subLabel = "Completed",
                        value = "$totalSessionsCount",
                        unit = "total",
                        icon = Icons.Default.Speed,
                        accentColor = cyanAccent,
                        surfaceAlt = surfaceAltColor,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Stat 2: Active Calories
                    SummaryStatBox(
                        modifier = Modifier.weight(1f),
                        label = "CALORIES",
                        subLabel = "Burned",
                        value = if (totalCaloriesBurned > 1000) String.format(java.util.Locale.US, "%.1fk", totalCaloriesBurned / 1000f) else "$totalCaloriesBurned",
                        unit = "kcal",
                        icon = Icons.Default.LocalFireDepartment,
                        accentColor = orangeAccent,
                        surfaceAlt = surfaceAltColor,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Stat 3: Active Time
                    SummaryStatBox(
                        modifier = Modifier.weight(1f),
                        label = "ACTIVE TIME",
                        subLabel = "Duration",
                        value = durationFormatted,
                        unit = "",
                        icon = Icons.Default.Timer,
                        accentColor = emeraldAccent,
                        surfaceAlt = surfaceAltColor,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "All" to "All Logs ($totalSessionsCount)",
                    "Workouts" to "Workouts (${allActivities.count { it.category == "Workouts" }})",
                    "Yoga" to "Yoga Zen (${allActivities.count { it.category == "Yoga" }})",
                    "Steps" to "Step History (${allActivities.count { it.category == "Steps" }})"
                )

                chips.forEach { (catKey, catLabel) ->
                    val isSelected = selectedFilter == catKey
                    val chipAccent = when (catKey) {
                        "Workouts" -> orangeAccent
                        "Yoga" -> purpleAccent
                        "Steps" -> limeAccent
                        else -> cyanAccent
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) chipAccent else cardColor)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) chipAccent else borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilter = catKey }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = catLabel,
                            color = if (isSelected) Color(0xFF0F172A) else textSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Activity History List
            if (filteredActivities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(cyanAccent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = cyanAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No Workout Sessions Yet",
                                color = textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Complete an outdoor run, squat session, or yoga pose to see your activity logs here.",
                                color = textSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredActivities, key = { it.id }) { item ->
                        ActivityTimelineCard(
                            item = item,
                            cardColor = cardColor,
                            surfaceAltColor = surfaceAltColor,
                            borderColor = borderColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBox(
    modifier: Modifier = Modifier,
    label: String,
    subLabel: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    surfaceAlt: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = surfaceAlt,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textSecondary,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
            Text(
                text = subLabel,
                fontSize = 9.sp,
                color = textSecondary
            )
        }
    }
}

@Composable
private fun ActivityTimelineCard(
    item: UnifiedActivityItem,
    cardColor: Color,
    surfaceAltColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(item.accentColor.copy(alpha = 0.22f), surfaceAltColor)
                        )
                    )
                    .border(1.5.dp, item.accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.category,
                    tint = item.accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    // Badge tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(item.accentColor.copy(alpha = 0.15f))
                            .border(1.dp, item.accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = item.accentColor,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.date,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
