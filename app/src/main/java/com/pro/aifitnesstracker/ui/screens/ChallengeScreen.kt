package com.pro.aifitnesstracker.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class ChallengeTrackInfo(
    val id: String,
    val title: String,
    val actionTitlePrefix: String,
    val unitLabel: String,
    val baseTarget: Int,
    val stepPerDay: Int,
    val accentColor: Color
)

private data class TrophyBadgeInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val progress: Float,
    val statusText: String,
    val accent: Color
)

@Composable
fun ChallengeScreen(
    prefsManager: SharedPrefsManager,
    onBack: () -> Unit,
    onStartChallengeDay: (Int, Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Theme Palette matching Challenge Hub Dark & Light Mockups
    val bgColor = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
    val cardBg = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val cardBgAlt = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDCE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val textMuted = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val gold = Color(0xFFF59E0B)
    val goldLight = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
    val orange = Color(0xFFF97316)
    val emerald = Color(0xFF10B981)
    val emeraldLight = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val cyan = Color(0xFF06B6D4)
    val purple = Color(0xFF8B5CF6)
    val purpleLight = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
    val crimson = Color(0xFFF43F5E)

    val tracks = remember {
        listOf(
            ChallengeTrackInfo(
                id = "SQUAT",
                title = "Squat & Leg Power",
                actionTitlePrefix = "Camera-Tracked Squats",
                unitLabel = "Squats",
                baseTarget = 20,
                stepPerDay = 5,
                accentColor = Color(0xFFF59E0B)
            ),
            ChallengeTrackInfo(
                id = "HIIT",
                title = "Full Body HIIT",
                actionTitlePrefix = "HIIT Burpees & Squats",
                unitLabel = "Reps",
                baseTarget = 18,
                stepPerDay = 4,
                accentColor = Color(0xFFF43F5E)
            ),
            ChallengeTrackInfo(
                id = "CORE",
                title = "Core & Plank",
                actionTitlePrefix = "Core Crunch & Squat Hold",
                unitLabel = "Reps",
                baseTarget = 25,
                stepPerDay = 5,
                accentColor = Color(0xFF06B6D4)
            ),
            ChallengeTrackInfo(
                id = "STEPS",
                title = "10K Steps Pro",
                actionTitlePrefix = "Power Squat & Step Burn",
                unitLabel = "Reps",
                baseTarget = 30,
                stepPerDay = 5,
                accentColor = Color(0xFF10B981)
            )
        )
    }

    var selectedTrackIndex by remember { mutableIntStateOf(0) }
    val activeTrack = tracks[selectedTrackIndex]

    // Revision counter to trigger recomposition when days are completed
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val completedDaysSet = remember(refreshTrigger) {
        (1..30).filter { prefsManager.isChallengeDayCompleted(it) }.toSet()
    }

    val currentActiveDay = remember(completedDaysSet) {
        (1..30).firstOrNull { !completedDaysSet.contains(it) } ?: 30
    }

    var inspectedDay by remember(currentActiveDay) { mutableIntStateOf(currentActiveDay) }

    val completedCount = completedDaysSet.size
    val completionFraction = (completedCount / 30f).coerceIn(0f, 1f)
    val completionPct = (completionFraction * 100f).roundToInt()

    // Calculate Total Reps & XP
    val totalRepsDone = remember(completedDaysSet, activeTrack, refreshTrigger) {
        val fromDays = completedDaysSet.sumOf { d -> activeTrack.baseTarget + (d - 1) * activeTrack.stepPerDay }
        maxOf(fromDays, prefsManager.getTotalSquats())
    }
    val totalXp = remember(completedCount, totalRepsDone) {
        completedCount * 100 + (totalRepsDone / 5) * 5
    }
    val userLevel = remember(totalXp) { (totalXp / 450) + 1 }
    val nextLevelXp = userLevel * 500
    val levelProgress = (totalXp.toFloat() / nextLevelXp.toFloat()).coerceIn(0.08f, 1f)

    // Inspected Day Mission metrics
    val inspectedDayTarget = activeTrack.baseTarget + (inspectedDay - 1) * activeTrack.stepPerDay
    val setsCount = 3
    val repsPerSet = (inspectedDayTarget / 3f).roundToInt().coerceAtLeast(5)
    val estMinutes = (4 + inspectedDay / 3).coerceIn(5, 18)
    val dayXpReward = if (inspectedDay in listOf(7, 14, 21, 30)) 250 else 150
    val isInspectedDayDone = completedDaysSet.contains(inspectedDay)

    // Achievements / Trophy Cabinet data
    val badges = remember(completedCount, totalRepsDone) {
        val b1Prog = (completedCount / 7f).coerceIn(0f, 1f)
        val b2Prog = (totalRepsDone / 500f).coerceIn(0f, 1f)
        val b3Prog = (completedCount / 14f).coerceIn(0f, 1f)
        val b4Prog = (completedCount / 30f).coerceIn(0f, 1f)

        if (b1Prog >= 1f) prefsManager.unlockBadge("BRONZE_STREAK_7")
        if (b2Prog >= 1f) prefsManager.unlockBadge("SQUAT_MASTER")
        if (b3Prog >= 1f) prefsManager.unlockBadge("SILVER_TITAN_14")
        if (b4Prog >= 1f) prefsManager.unlockBadge("IMMORTAL_CROWN_30")

        listOf(
            TrophyBadgeInfo(
                id = "BRONZE_STREAK_7",
                title = "7-Day Bronze Streak",
                subtitle = "Complete first 7 days in a row",
                tag = "DAY 7",
                progress = b1Prog,
                statusText = if (b1Prog >= 1f) "UNLOCKED" else "${(b1Prog * 100).roundToInt()}% • $completedCount/7d",
                accent = emerald
            ),
            TrophyBadgeInfo(
                id = "SQUAT_MASTER",
                title = "Squat Master 500+",
                subtitle = "Complete 500+ total recorded squats",
                tag = "500 REPS",
                progress = b2Prog,
                statusText = if (b2Prog >= 1f) "UNLOCKED" else "${(b2Prog * 100).roundToInt()}% • $totalRepsDone/500",
                accent = gold
            ),
            TrophyBadgeInfo(
                id = "SILVER_TITAN_14",
                title = "14-Day Silver Titan",
                subtitle = "Reach Day 14 mid-challenge boss",
                tag = "DAY 14",
                progress = b3Prog,
                statusText = if (b3Prog >= 1f) "UNLOCKED" else "${(b3Prog * 100).roundToInt()}% • $completedCount/14d",
                accent = cyan
            ),
            TrophyBadgeInfo(
                id = "IMMORTAL_CROWN_30",
                title = "30-Day Immortal Crown",
                subtitle = "Finish all 30 days & 2,775 reps",
                tag = "DAY 30",
                progress = b4Prog,
                statusText = if (b4Prog >= 1f) "UNLOCKED" else "${(b4Prog * 100).roundToInt()}% • $completedCount/30d",
                accent = purple
            )
        )
    }
    val unlockedBadgeCount = badges.count { it.progress >= 1f }

    val bossDays = remember {
        mapOf(
            7 to "BRONZE",
            14 to "SILVER",
            21 to "GOLD",
            30 to "CROWN"
        )
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Surface(
                color = bgColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "30-DAY ${strings.workoutArchitect.uppercase(java.util.Locale.US)}",
                            color = textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(gold.copy(alpha = if (isDark) 0.16f else 0.14f))
                                .border(1.dp, gold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "DAY $currentActiveDay OF 30  •  %,d XP  •  ${completedCount}D STREAK".format(totalXp),
                                color = goldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Level Badge Pill
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(gold.copy(alpha = if (isDark) 0.16f else 0.14f))
                            .border(1.dp, gold, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LVL $userLevel",
                            color = goldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(padding)
        ) {
            // Subtle Top Ambient Glow
            Canvas(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeTrack.accentColor.copy(alpha = if (isDark) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.35f),
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.5f, size.height * 0.35f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 2. Challenge Track Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tracks.forEachIndexed { idx, track ->
                    val isSelected = idx == selectedTrackIndex
                    val pillBg = if (isSelected) {
                        track.accentColor.copy(alpha = if (isDark) 0.20f else 0.15f)
                    } else {
                        cardBg
                    }
                    val pillBorder = if (isSelected) track.accentColor else borderColor
                    val pillTextCol = if (isSelected) {
                        if (isDark) track.accentColor else track.accentColor.copy(alpha = 0.95f)
                    } else {
                        textSecondary
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .background(pillBg)
                            .border(if (isSelected) 1.5.dp else 1.dp, pillBorder, RoundedCornerShape(13.dp))
                            .clickable { selectedTrackIndex = idx }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = track.title,
                            color = pillTextCol,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Hero Card: Championship Progress Gauge & Active Day Mission
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Left Circular Trophy Progress Ring Gauge
                    ChampionshipTrophyRingGauge(
                        progress = completionFraction,
                        completedDays = completedCount,
                        isDark = isDark,
                        cardBgAlt = cardBgAlt,
                        textPrimary = textPrimary,
                        gold = gold,
                        emerald = emerald,
                        modifier = Modifier.size(136.dp)
                    )

                    // Right Mission Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Badges Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(orange.copy(alpha = if (isDark) 0.18f else 0.14f))
                                    .border(1.dp, orange, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (inspectedDay == currentActiveDay) "DAY $inspectedDay • TODAY MISSION" else "DAY $inspectedDay • SELECTED",
                                    color = orange,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(emerald.copy(alpha = if (isDark) 0.18f else 0.14f))
                                    .border(1.dp, emerald, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+$dayXpReward XP",
                                    color = emeraldLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Workout Title & Breakdown
                        Text(
                            text = "$inspectedDayTarget ${activeTrack.actionTitlePrefix}",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$setsCount Sets × $repsPerSet ${activeTrack.unitLabel} • 60s Rest • Est. $estMinutes Mins",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // XP Level Bar Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBgAlt)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LEVEL $userLevel • ELITE CONTENDER",
                                    color = goldLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "%,d / %,d XP".format(totalXp, nextLevelXp),
                                    color = cyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isDark) Color(0xFF1E2940) else Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(levelProgress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(gold, orange)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                // Dual Action Buttons Row: START AI CAMERA WORKOUT + MARK DONE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onStartChallengeDay(inspectedDay, inspectedDayTarget) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gold,
                            contentColor = Color(0xFF0A0E17)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "START CAMERA WORKOUT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isInspectedDayDone) emerald.copy(alpha = if (isDark) 0.28f else 0.22f)
                                else emerald.copy(alpha = if (isDark) 0.16f else 0.12f)
                            )
                            .border(1.5.dp, emerald, RoundedCornerShape(14.dp))
                            .clickable {
                                if (!isInspectedDayDone) {
                                    prefsManager.completeChallengeDay(inspectedDay)
                                    prefsManager.addTotalSquats(inspectedDayTarget)
                                    prefsManager.checkAndSaveBestSession(inspectedDayTarget)
                                    val newStreak = (completedCount + 1).coerceAtLeast(prefsManager.getCurrentStreak())
                                    prefsManager.saveCurrentStreak(newStreak)
                                    refreshTrigger++
                                    Toast.makeText(
                                        context,
                                        "Day $inspectedDay Completed! +$dayXpReward XP Earned 🏆",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Day $inspectedDay is already completed! Tap Start to replay.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = emeraldLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isInspectedDayDone) "COMPLETED" else "MARK DONE",
                                color = emeraldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Bottom 3-Metric Summary Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChallengeMiniStatBox(
                        label = "COMPLETED",
                        value = "$completedCount / 30 Days",
                        valueColor = emeraldLight,
                        cardBgAlt = cardBgAlt,
                        borderColor = borderColor,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    ChallengeMiniStatBox(
                        label = "ACTIVE STREAK",
                        value = "$completedCount Days Fire",
                        valueColor = goldLight,
                        cardBgAlt = cardBgAlt,
                        borderColor = borderColor,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    ChallengeMiniStatBox(
                        label = "TOTAL REPS",
                        value = "$totalRepsDone ${activeTrack.unitLabel}",
                        valueColor = cyan,
                        cardBgAlt = cardBgAlt,
                        borderColor = borderColor,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. 30-Day Interactive Championship Roadmap Grid (6 Columns × 5 Rows)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "30-DAY CHAMPIONSHIP ROADMAP",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "TAP ANY DAY TO INSPECT",
                        color = cyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Legend Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoadmapLegendDot("Completed ($completedCount)", emerald, textSecondary)
                    RoadmapLegendDot("Today (Day $currentActiveDay)", gold, textSecondary)
                    RoadmapLegendDot("Boss Milestone (7/14/21/30)", purple, textSecondary)
                    RoadmapLegendDot("Upcoming", textMuted, textSecondary)
                }

                // 6x5 Non-lazy Grid for smooth nested scrolling
                val cols = 6
                val rows = 5
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (c in 0 until cols) {
                                val day = r * cols + c + 1
                                val reps = activeTrack.baseTarget + (day - 1) * activeTrack.stepPerDay
                                val isDone = completedDaysSet.contains(day)
                                val isToday = day == currentActiveDay
                                val isSelected = day == inspectedDay
                                val bossTag = bossDays[day]
                                val isBoss = bossTag != null

                                val cellBg = when {
                                    isDone -> emerald.copy(alpha = if (isDark) 0.16f else 0.15f)
                                    isToday -> gold.copy(alpha = if (isDark) 0.24f else 0.20f)
                                    isBoss -> purple.copy(alpha = if (isDark) 0.18f else 0.14f)
                                    else -> cardBgAlt
                                }
                                val cellBorder = when {
                                    isSelected -> activeTrack.accentColor
                                    isDone -> emerald
                                    isToday -> gold
                                    isBoss -> purple
                                    else -> borderColor
                                }
                                val borderWidth = if (isToday || isSelected) 2.dp else if (isDone || isBoss) 1.5.dp else 1.dp

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(74.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(cellBg)
                                        .border(borderWidth, cellBorder, RoundedCornerShape(14.dp))
                                        .clickable {
                                            inspectedDay = day
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Top Pill / Day Header
                                    if (isToday) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(gold)
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "TODAY",
                                                color = Color(0xFF0A0E17),
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    } else if (bossTag != null) {
                                        val bCol = if (isDone) emeraldLight else purpleLight
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(bCol.copy(alpha = 0.20f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = bossTag,
                                                color = bCol,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "DAY $day",
                                            color = if (isDone) emeraldLight else textSecondary,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    // Center Target Number
                                    Text(
                                        text = if (isDone) "✓ $reps" else "$reps",
                                        color = when {
                                            isDone -> emeraldLight
                                            isToday -> goldLight
                                            isBoss -> purpleLight
                                            else -> textPrimary
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )

                                    // Bottom Subtitle
                                    Text(
                                        text = when {
                                            isDone -> "DONE"
                                            isToday -> "START"
                                            else -> "REPS"
                                        },
                                        color = when {
                                            isDone -> emeraldLight
                                            isToday -> orange
                                            else -> textMuted
                                        },
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Championship Trophy Cabinet & Achievements Showcase (2x2 Grid)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHAMPIONSHIP TROPHY CABINET",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(gold.copy(alpha = if (isDark) 0.18f else 0.14f))
                            .border(1.dp, gold, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$unlockedBadgeCount / ${badges.size} UNLOCKED",
                            color = goldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // 2x2 Badge Cards
                for (r in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (c in 0..1) {
                            val badge = badges[r * 2 + c]
                            val isUnlocked = badge.progress >= 1f
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isUnlocked) badge.accent.copy(alpha = if (isDark) 0.14f else 0.10f)
                                        else cardBgAlt
                                    )
                                    .border(
                                        width = if (isUnlocked) 1.5.dp else 1.dp,
                                        color = if (isUnlocked) badge.accent else borderColor,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(badge.accent.copy(alpha = if (isDark) 0.22f else 0.16f))
                                            .border(1.5.dp, badge.accent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = badge.tag,
                                            color = badge.accent,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = badge.title,
                                            color = textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = badge.subtitle,
                                            color = textSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isDark) Color(0xFF1E2940) else Color(0xFFE2E8F0))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(badge.progress.coerceIn(0.06f, 1f))
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(badge.accent)
                                        )
                                    }

                                    Text(
                                        text = badge.statusText,
                                        color = badge.accent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
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
}

@Composable
private fun ChampionshipTrophyRingGauge(
    progress: Float,
    completedDays: Int,
    isDark: Boolean,
    cardBgAlt: Color,
    textPrimary: Color,
    gold: Color,
    emerald: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "TrophyGaugeProgress"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 12.dp.toPx()
            val pad = strokeW / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
            val topLeft = Offset(pad, pad)

            // Background Track Arc (135° to 405°, sweep = 270°)
            drawArc(
                color = if (isDark) Color(0xFF1E2940) else Color(0xFFE2E8F0),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Active Sweep Arc
            val sweep = (270f * animatedProgress).coerceAtLeast(8f)
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(gold, emerald, gold)
                ),
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Inner Circle
            val innerRadius = (size.minDimension / 2f) - strokeW - 8.dp.toPx()
            drawCircle(
                color = cardBgAlt,
                radius = innerRadius,
                center = center
            )
            drawCircle(
                color = gold.copy(alpha = 0.55f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Vector Trophy Cup in top half of inner circle
            val cupTop = center.y - innerRadius * 0.55f
            val cupBottom = center.y - innerRadius * 0.10f
            val cupHalfW = innerRadius * 0.32f
            val trophyPath = Path().apply {
                moveTo(center.x - cupHalfW, cupTop)
                lineTo(center.x + cupHalfW, cupTop)
                lineTo(center.x + cupHalfW * 0.72f, cupBottom)
                lineTo(center.x - cupHalfW * 0.72f, cupBottom)
                close()
            }
            drawPath(trophyPath, color = gold)
            // Stem
            drawRect(
                color = Color(0xFFFBBF24),
                topLeft = Offset(center.x - 3.dp.toPx(), cupBottom),
                size = Size(6.dp.toPx(), 7.dp.toPx())
            )
            // Base
            drawRoundRect(
                color = gold,
                topLeft = Offset(center.x - cupHalfW * 0.85f, cupBottom + 7.dp.toPx()),
                size = Size(cupHalfW * 1.7f, 5.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 38.dp)
        ) {
            Text(
                text = "${(animatedProgress * 100).roundToInt()}%",
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "$completedDays/30 DAYS",
                color = gold,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ChallengeMiniStatBox(
    label: String,
    value: String,
    valueColor: Color,
    cardBgAlt: Color,
    borderColor: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cardBgAlt)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            color = textSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RoadmapLegendDot(
    label: String,
    dotColor: Color,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
