package com.pro.aifitnesstracker.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager

// ─────────────────────────────────────────────────
// 👟 STEP GOAL SCREEN
// ─────────────────────────────────────────────────
@Composable
fun StepGoalScreen(onBack: () -> Unit, prefsManager: SharedPrefsManager) {
    var goalSteps by remember { mutableIntStateOf(prefsManager.getStepGoal()) }
    val presetGoals = listOf(3000, 5000, 8000, 10000, 12000, 15000)
    val emeraldAccent = Color(0xFF10B981)

    val stepStatus = when {
        goalSteps < 5000 -> "⚠️ Sedentary Activity"
        goalSteps <= 8000 -> "🚶 Active Daily Baseline"
        goalSteps <= 10000 -> "⚡ Recommended Daily Target"
        else -> "🏆 High Athletic Volume"
    }

    GoalScreenTemplate(
        title = "Daily Step Goal",
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        iconColor = emeraldAccent,
        gradientColors = listOf(emeraldAccent.copy(alpha = 0.25f), Color.Transparent),
        currentValueText = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(goalSteps),
        unitText = "steps",
        subtitleText = stepStatus,
        presetLabels = presetGoals.map { "${it / 1000}k" },
        presetValues = presetGoals,
        selectedPreset = goalSteps,
        onPresetSelected = { goalSteps = it },
        onIncrease1 = { if (goalSteps < 50000) goalSteps += 500 },
        onDecrease1 = { if (goalSteps > 1000) goalSteps -= 500 },
        onIncreaseBig = { if (goalSteps < 48000) goalSteps += 2000 },
        onDecreaseBig = { if (goalSteps > 3000) goalSteps -= 2000 },
        smallLabel = "+500 / -500",
        bigLabel = "+2k / -2k",
        onSave = {
            prefsManager.saveStepGoal(goalSteps)
            onBack()
        },
        onBack = onBack
    )
}

// ─────────────────────────────────────────────────
// 💧 WATER GOAL SCREEN
// ─────────────────────────────────────────────────
@Composable
fun WaterGoalScreen(onBack: () -> Unit, prefsManager: SharedPrefsManager) {
    var goalGlasses by remember { mutableIntStateOf(prefsManager.getWaterGoal()) }
    val presetGoals = listOf(4, 6, 8, 10, 12, 14, 16)
    val cyanAccent = Color(0xFF06B6D4)

    val hydrationStatus = when {
        goalGlasses < 6 -> "⚠️ Below Recommended"
        goalGlasses <= 10 -> "💧 Optimal Daily Hydration"
        goalGlasses <= 14 -> "⚡ High Activity Hydration"
        else -> "🌊 Maximum Intake"
    }

    GoalScreenTemplate(
        title = "Daily Water Goal",
        icon = Icons.Default.LocalDrink,
        iconColor = cyanAccent,
        gradientColors = listOf(cyanAccent.copy(alpha = 0.25f), Color.Transparent),
        currentValueText = "$goalGlasses",
        unitText = "glasses",
        subtitleText = "$hydrationStatus • ${goalGlasses * 250} ml/day",
        presetLabels = presetGoals.map { "${it}G" },
        presetValues = presetGoals,
        selectedPreset = goalGlasses,
        onPresetSelected = { goalGlasses = it },
        onIncrease1 = { if (goalGlasses < 30) goalGlasses++ },
        onDecrease1 = { if (goalGlasses > 1) goalGlasses-- },
        onIncreaseBig = { if (goalGlasses < 28) goalGlasses += 2 },
        onDecreaseBig = { if (goalGlasses > 2) goalGlasses -= 2 },
        smallLabel = "+1 / -1",
        bigLabel = "+2 / -2",
        onSave = {
            prefsManager.saveWaterGoal(goalGlasses)
            onBack()
        },
        onBack = onBack
    )
}

// ─────────────────────────────────────────────────
// 😴 SLEEP GOAL SCREEN
// ─────────────────────────────────────────────────
@Composable
fun SleepGoalScreen(onBack: () -> Unit, prefsManager: SharedPrefsManager) {
    var goalHoursRaw by remember { mutableIntStateOf((prefsManager.getSleepGoal() * 2).toInt()) }
    val goalHours = goalHoursRaw / 2f
    val presetRaw = listOf(10, 12, 13, 14, 15, 16, 18)
    val presetLabels = listOf("5h", "6h", "6.5h", "7h", "7.5h", "8h", "9h")
    val purpleAccent = Color(0xFFA855F7)

    val sleepQuality = when {
        goalHours < 6 -> "⚠️ Insufficient Rest"
        goalHours < 7 -> "😐 Below Average Rest"
        goalHours <= 9 -> "✅ Optimal Athletic Recovery"
        else -> "😴 Prolonged Rest"
    }

    GoalScreenTemplate(
        title = "Daily Sleep Goal",
        icon = Icons.Default.Bedtime,
        iconColor = purpleAccent,
        gradientColors = listOf(purpleAccent.copy(alpha = 0.25f), Color.Transparent),
        currentValueText = "${goalHoursRaw / 2}h ${if (goalHoursRaw % 2 == 1) "30m" else "00m"}",
        unitText = "target",
        subtitleText = sleepQuality,
        presetLabels = presetLabels,
        presetValues = presetRaw,
        selectedPreset = goalHoursRaw,
        onPresetSelected = { goalHoursRaw = it },
        onIncrease1 = { if (goalHoursRaw < 24) goalHoursRaw++ },
        onDecrease1 = { if (goalHoursRaw > 4) goalHoursRaw-- },
        onIncreaseBig = { if (goalHoursRaw < 22) goalHoursRaw += 2 },
        onDecreaseBig = { if (goalHoursRaw > 6) goalHoursRaw -= 2 },
        smallLabel = "+30m / -30m",
        bigLabel = "+1h / -1h",
        onSave = {
            prefsManager.saveSleepGoal(goalHours)
            onBack()
        },
        onBack = onBack
    )
}

// ─────────────────────────────────────────────────
// 🔥 CALORIE GOAL SCREEN
// ─────────────────────────────────────────────────
@Composable
fun CalorieGoalScreen(onBack: () -> Unit, prefsManager: SharedPrefsManager) {
    var showSmartWizard by remember { mutableStateOf(false) }

    if (showSmartWizard) {
        SmartTDEEWizard(
            onCalculated = { newGoal ->
                prefsManager.saveCalorieGoal(newGoal)
                showSmartWizard = false
            },
            onCancel = { showSmartWizard = false }
        )
    } else {
        var goalCalories by remember { mutableIntStateOf(prefsManager.getCalorieGoal()) }
        val presets = listOf(1200, 1500, 1800, 2000, 2200, 2500, 3000, 3500)
        val orangeAccent = Color(0xFFF97316)

        val activityLabel = when {
            goalCalories < 1500 -> "🥗 Aggressive Fat Loss"
            goalCalories < 1800 -> "⚖️ Moderate Deficit"
            goalCalories < 2200 -> "💪 Athletic Maintenance"
            goalCalories < 2700 -> "📈 Hypertrophy Lean Gain"
            else -> "🏋️ Bulking & High Output"
        }

        GoalScreenTemplate(
            title = "Daily Calorie Goal",
            icon = Icons.Default.LocalFireDepartment,
            iconColor = orangeAccent,
            gradientColors = listOf(orangeAccent.copy(alpha = 0.25f), Color.Transparent),
            currentValueText = "$goalCalories",
            unitText = "kcal / day",
            subtitleText = activityLabel,
            presetLabels = presets.map { "${it / 1000}.${(it % 1000) / 100}k" },
            presetValues = presets,
            selectedPreset = goalCalories,
            onPresetSelected = { goalCalories = it },
            onIncrease1 = { if (goalCalories < 5000) goalCalories += 50 },
            onDecrease1 = { if (goalCalories > 500) goalCalories -= 50 },
            onIncreaseBig = { if (goalCalories < 4500) goalCalories += 500 },
            onDecreaseBig = { if (goalCalories > 1000) goalCalories -= 500 },
            smallLabel = "+50 / -50",
            bigLabel = "+500 / -500",
            smartFeatureContent = {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
                val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
                val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
                val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSmartWizard = true },
                    shape = RoundedCornerShape(18.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF06B6D4).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart TDEE Auto-Calculator",
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Calibrate target using Mifflin-St Jeor formula",
                                color = textSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF06B6D4).copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "CALC",
                                color = Color(0xFF06B6D4),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            },
            onSave = {
                prefsManager.saveCalorieGoal(goalCalories)
                onBack()
            },
            onBack = onBack
        )
    }
}

// ─────────────────────────────────────────────────
// 🎨 SHARED GOAL SCREEN TEMPLATE (ZERO OVERLAP GUARANTEED)
// ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreenTemplate(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    gradientColors: List<Color>,
    currentValueText: String,
    unitText: String,
    subtitleText: String,
    presetLabels: List<String>,
    presetValues: List<Int>,
    selectedPreset: Int,
    onPresetSelected: (Int) -> Unit,
    onIncrease1: () -> Unit,
    onDecrease1: () -> Unit,
    onIncreaseBig: () -> Unit,
    onDecreaseBig: () -> Unit,
    smallLabel: String,
    bigLabel: String,
    smartFeatureContent: @Composable () -> Unit = {},
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

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

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DAILY TARGET HUB",
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
                                .background(iconColor.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = title.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = iconColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardColor)
                            .border(1.dp, iconColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onSave)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SAVE",
                            color = iconColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            // Hero Circular Dial Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(210.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background track & active glowing arc
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 10.dp.toPx()
                            val arcRadius = (size.minDimension - strokeW) / 2
                            val centerPt = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                            val arcTopLeft = androidx.compose.ui.geometry.Offset(centerPt.x - arcRadius, centerPt.y - arcRadius)
                            val arcSize = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2)

                            // Track
                            drawArc(
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )

                            // Active Arc
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(iconColor, iconColor.copy(alpha = 0.6f), iconColor)
                                ),
                                startAngle = 135f,
                                sweepAngle = 210f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }

                        // Center circle content
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(iconColor.copy(alpha = 0.18f), surfaceAltColor)
                                    )
                                )
                                .border(1.5.dp, iconColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = currentValueText,
                                    color = textPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = unitText.uppercase(),
                                    color = textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Subtitle Feedback Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconColor.copy(alpha = 0.12f))
                            .border(1.dp, iconColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = subtitleText,
                            color = iconColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Multi-Step Precision Stepper Card (Zero overlap layout)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = cardColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Big decrease
                    StepperButton(
                        label = bigLabel.split(" / ").getOrElse(1) { "-2" },
                        subLabel = "MACRO",
                        icon = Icons.Default.KeyboardArrowDown,
                        accentColor = iconColor,
                        surfaceAlt = surfaceAltColor,
                        borderColor = borderColor,
                        textSecondary = textSecondary,
                        onClick = onDecreaseBig
                    )

                    // Small decrease
                    StepperButton(
                        label = smallLabel.split(" / ").getOrElse(1) { "-1" },
                        subLabel = "FINE",
                        icon = Icons.Default.KeyboardArrowDown,
                        accentColor = iconColor,
                        surfaceAlt = surfaceAltColor,
                        borderColor = borderColor,
                        textSecondary = textSecondary,
                        onClick = onDecrease1
                    )

                    // Current Value Display (Center)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentValueText,
                            color = iconColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = unitText,
                            color = textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Small increase
                    StepperButton(
                        label = smallLabel.split(" / ").getOrElse(0) { "+1" },
                        subLabel = "FINE",
                        icon = Icons.Default.KeyboardArrowUp,
                        accentColor = iconColor,
                        surfaceAlt = surfaceAltColor,
                        borderColor = borderColor,
                        textSecondary = textSecondary,
                        onClick = onIncrease1
                    )

                    // Big increase
                    StepperButton(
                        label = bigLabel.split(" / ").getOrElse(0) { "+2" },
                        subLabel = "MACRO",
                        icon = Icons.Default.KeyboardArrowUp,
                        accentColor = iconColor,
                        surfaceAlt = surfaceAltColor,
                        borderColor = borderColor,
                        textSecondary = textSecondary,
                        onClick = onIncreaseBig
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quick Presets Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUICK TARGET PRESETS",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "One-Tap Select",
                    color = textSecondary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            val rows = presetLabels.indices.chunked(4)
            rows.forEach { rowIndices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowIndices.forEach { idx ->
                        val isSelected = selectedPreset == presetValues[idx]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) iconColor else cardColor)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) iconColor else borderColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onPresetSelected(presetValues[idx]) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = presetLabels[idx],
                                color = if (isSelected) Color(0xFF0F172A) else textPrimary,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(10.dp))

            // Optional Smart Feature Content (e.g. Smart Auto-Calculate for calories)
            smartFeatureContent()

            // Save CTA Button
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = iconColor,
                    contentColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${strings.save.uppercase(java.util.Locale.US)} TARGET",
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Cancel & Return",
                    color = textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    label: String,
    subLabel: String,
    icon: ImageVector,
    accentColor: Color,
    surfaceAlt: Color,
    borderColor: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = surfaceAlt,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Text(
                text = subLabel,
                fontSize = 8.sp,
                color = textSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SmartTDEEWizard(
    onCalculated: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var age by remember { mutableStateOf("25") }
    var height by remember { mutableStateOf("170") }
    var weight by remember { mutableStateOf("70") }
    var isMale by remember { mutableStateOf(true) }

    val activityLevels = listOf("Sedentary (1.2x)", "Light (1.375x)", "Moderate (1.55x)", "Very Active (1.725x)")
    var selectedActivityIdx by remember { mutableIntStateOf(1) }

    val goals = listOf("Lose Weight (-500)", "Maintain (0)", "Gain Muscle (+500)")
    var selectedGoalIdx by remember { mutableIntStateOf(1) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val cyanAccent = Color(0xFF06B6D4)

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Surface(
                color = backgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "SMART TDEE WIZARD",
                            color = textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Mifflin-St Jeor Energy Equation",
                            color = cyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            // Metrics Inputs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ATHLETE PARAMETERS",
                    color = textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age", color = textSecondary) },
                        suffix = { Text("yr", color = cyanAccent, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cyanAccent,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height", color = textSecondary) },
                        suffix = { Text("cm", color = cyanAccent, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cyanAccent,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight", color = textSecondary) },
                        suffix = { Text("kg", color = cyanAccent, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cyanAccent,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Biological Gender Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(surfaceAltColor)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMale) cyanAccent else Color.Transparent)
                            .clickable { isMale = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Male",
                            color = if (isMale) Color(0xFF0F172A) else textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isMale) cyanAccent else Color.Transparent)
                            .clickable { isMale = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Female",
                            color = if (!isMale) Color(0xFF0F172A) else textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Activity Level Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DAILY ACTIVITY LEVEL",
                    color = textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))
                activityLevels.forEachIndexed { index, level ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedActivityIdx == index) surfaceAltColor else Color.Transparent)
                            .clickable { selectedActivityIdx = index }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = selectedActivityIdx == index,
                            onClick = { selectedActivityIdx = index },
                            colors = RadioButtonDefaults.colors(selectedColor = cyanAccent)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = level,
                            color = if (selectedActivityIdx == index) textPrimary else textSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (selectedActivityIdx == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Fitness Goal
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FITNESS TARGET DIRECTION",
                    color = textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))
                goals.forEachIndexed { index, goal ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedGoalIdx == index) surfaceAltColor else Color.Transparent)
                            .clickable { selectedGoalIdx = index }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = selectedGoalIdx == index,
                            onClick = { selectedGoalIdx = index },
                            colors = RadioButtonDefaults.colors(selectedColor = cyanAccent)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = goal,
                            color = if (selectedGoalIdx == index) textPrimary else textSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (selectedGoalIdx == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Calculate and Save CTA
        Button(
            onClick = {
                val a = age.toIntOrNull() ?: 25
                val h = height.toFloatOrNull() ?: 170f
                val w = weight.toFloatOrNull() ?: 70f

                var bmr = (10 * w) + (6.25f * h) - (5 * a)
                bmr += if (isMale) 5f else -161f

                val multiplier = when (selectedActivityIdx) {
                    0 -> 1.2f
                    1 -> 1.375f
                    2 -> 1.55f
                    3 -> 1.725f
                    else -> 1.2f
                }

                var tdee = bmr * multiplier

                when (selectedGoalIdx) {
                    0 -> tdee -= 500f
                    2 -> tdee += 500f
                }

                onCalculated(tdee.toInt().coerceAtLeast(1200))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = cyanAccent,
                contentColor = Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = "CALCULATE & APPLY GOAL",
                color = Color(0xFF0F172A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.height(10.dp))

        TextButton(onClick = onCancel) {
            Text("Cancel", color = textSecondary, fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))
    }
}
}
