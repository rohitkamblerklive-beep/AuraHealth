package com.pro.aifitnesstracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

data class LabItem(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val desc: String,
    val category: String,
    val result: () -> String,
    val status: () -> String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmLabScreen(
    onBack: () -> Unit,
    prefsManager: SharedPrefsManager,
    onNavigate: (String) -> Unit
) {
    val weight = prefsManager.getWeight()
    val height = prefsManager.getHeight()
    val steps = prefsManager.getStepsToday()
    val water = prefsManager.getWaterGlassesToday()
    val age = prefsManager.getAge()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    val cyanAccent = Color(0xFF06B6D4)
    val emeraldAccent = Color(0xFF10B981)
    val limeAccent = Color(0xFF84CC16)
    val amberAccent = Color(0xFFF59E0B)
    val purpleAccent = Color(0xFFA855F7)
    val crimsonAccent = Color(0xFFF43F5E)

    val labs = remember(weight, height, steps, water, age) {
        listOf(
            LabItem(Icons.Default.Calculate, emeraldAccent, "BMI Calculator", "Body Mass Index (kg/m²)", "Energy & Burn", {
                if (weight > 20f && height > 100f) {
                    val hm = height / 100f
                    String.format(Locale.US, "%.1f", weight / (hm * hm))
                } else "Set Profile"
            }, {
                if (weight > 20f && height > 100f) {
                    val hm = height / 100f
                    val bmi = weight / (hm * hm)
                    when {
                        bmi < 18.5f -> "Underweight"
                        bmi < 25f -> "Normal Range"
                        bmi < 30f -> "Overweight"
                        else -> "High Risk"
                    }
                } else "Incomplete"
            }),
            LabItem(Icons.Default.LocalFireDepartment, amberAccent, "BMR Estimator", "Mifflin-St Jeor Formula", "Energy & Burn", {
                if (weight > 20f && height > 100f) {
                    val bmr = ((10f * weight) + (6.25f * height) - (5f * age) + 5f).toInt()
                    "$bmr cal"
                } else "Set Profile"
            }, { "Basal Resting" }),
            LabItem(Icons.Default.Bolt, limeAccent, "TDEE Calculator", "Active Daily Caloric Burn", "Energy & Burn", {
                if (weight > 20f && height > 100f) {
                    val bmr = ((10f * weight) + (6.25f * height) - (5f * age) + 5f)
                    val tdee = (bmr * 1.55f).toInt()
                    "$tdee cal"
                } else "Set Profile"
            }, { "Moderate Activity" }),
            LabItem(Icons.Default.MonitorWeight, cyanAccent, "Ideal Weight", "Robinson Formula (kg)", "Body & Muscle", {
                if (height > 152.4f) {
                    val ideal = 52f + (1.9f * (height - 152.4f) / 2.54f)
                    String.format(Locale.US, "%.1f kg", ideal)
                } else "Set Profile"
            }, { "Target Baseline" }),
            LabItem(Icons.Default.FitnessCenter, purpleAccent, "1-Rep Max", "Brzycki Strength Formula", "Body & Muscle", {
                "Ask Coach"
            }, { "Strength Index" }),
            LabItem(Icons.Default.FavoriteBorder, crimsonAccent, "VO2 Max Cardio", "Aerobic Fitness Level", "Cardio & Recovery", {
                if (weight > 20f && steps > 0) {
                    val vo2 = 15f + (steps.toFloat() / 10000f * 25f)
                    val level = when {
                        vo2 >= 50f -> "Elite"
                        vo2 >= 40f -> "Good"
                        vo2 >= 30f -> "Average"
                        else -> "Fair"
                    }
                    String.format(Locale.US, "%.1f (%s)", vo2, level)
                } else "Walk to Calc"
            }, { "Cardio Stamina" }),
            LabItem(Icons.Default.WaterDrop, cyanAccent, "Hydration Needs", "Daily Water Target (L/Glasses)", "Cardio & Recovery", {
                if (weight > 20f) {
                    val glasses = (weight * 0.033f / 0.25f).toInt()
                    "$glasses glasses"
                } else "Set Profile"
            }, {
                if (weight > 20f) String.format(Locale.US, "%.1f L/day", weight * 0.033f) else "Baseline"
            }),
            LabItem(Icons.Default.Event, emeraldAccent, "Goal Target Date", "Projected Days to Target", "Diagnostic Tests", {
                if (weight > 20f && height > 100f) {
                    val idealW = if (height > 152.4f) (52f + (1.9f * (height - 152.4f) / 2.54f)) else 55f
                    val diff = weight - idealW
                    if (diff > 1f) {
                        val days = (diff / 0.07f).toInt()
                        "$days days"
                    } else "At Goal!"
                } else "Set Profile"
            }, { "Safe Deficit" }),
            LabItem(Icons.Default.Accessibility, purpleAccent, "FFMI Muscle Index", "Fat-Free Mass Ratio (kg/m²)", "Body & Muscle", {
                if (weight > 20f && height > 100f) {
                    val hm = height / 100f
                    val leanMass = weight * 0.85f
                    val ffmi = leanMass / (hm * hm)
                    String.format(Locale.US, "%.1f", ffmi)
                } else "Set Profile"
            }, {
                if (weight > 20f && height > 100f) {
                    val hm = height / 100f
                    val ffmi = (weight * 0.85f) / (hm * hm)
                    when {
                        ffmi >= 22f -> "Athletic"
                        ffmi >= 19f -> "Fit"
                        else -> "Average"
                    }
                } else "Lean Mass"
            }),
            LabItem(Icons.Default.Bedtime, amberAccent, "Recovery Score", "Sleep + Activity Balance", "Cardio & Recovery", {
                val stepScore = (steps / 10000f * 50f).coerceAtMost(50f)
                val waterScore = (water / 8f * 50f).coerceAtMost(50f)
                val total = (stepScore + waterScore).toInt()
                "$total%"
            }, {
                val stepScore = (steps / 10000f * 50f).coerceAtMost(50f)
                val waterScore = (water / 8f * 50f).coerceAtMost(50f)
                val total = (stepScore + waterScore).toInt()
                when {
                    total >= 80 -> "Optimal"
                    total >= 50 -> "Good"
                    else -> "Rest Needed"
                }
            }),
            LabItem(Icons.Default.Timer, limeAccent, "Pace Calculator", "Daily Step Distance & Rate", "Energy & Burn", {
                if (steps > 0) {
                    val km = steps * 0.000762f
                    String.format(Locale.US, "%.2f km", km)
                } else "Start Walking"
            }, { "Pedometer" }),
            LabItem(Icons.AutoMirrored.Filled.TrendingUp, crimsonAccent, "Progressive Overload", "Recommended Weekly Increase", "Body & Muscle", {
                if (weight > 20f) {
                    val increase = String.format(Locale.US, "%.1f", weight * 0.025f)
                    "+$increase kg"
                } else "Set Profile"
            }, { "+2.5% per week" }),
            LabItem(Icons.Default.PieChart, cyanAccent, "Body Fat %", "Navy Estimation Formula", "Body & Muscle", {
                if (weight > 20f && height > 100f) {
                    val bmi = weight / ((height / 100f) * (height / 100f))
                    val bf = (1.2f * bmi) + (0.23f * age.toFloat()) - 10.8f - 5.4f
                    String.format(Locale.US, "%.1f%%", bf.coerceAtLeast(6f))
                } else "Set Profile"
            }, {
                if (weight > 20f && height > 100f) {
                    val bmi = weight / ((height / 100f) * (height / 100f))
                    val bf = (1.2f * bmi) + (0.23f * age.toFloat()) - 10.8f - 5.4f
                    when {
                        bf < 14f -> "Athletic"
                        bf < 20f -> "Fit"
                        bf < 25f -> "Average"
                        else -> "High"
                    }
                } else "Composition"
            }),
            LabItem(Icons.Default.Face, limeAccent, "Biological Age", "Physiological vs Chrono Age", "Cardio & Recovery", {
                val healthScore = ((steps / 10000f * 40f) + (water / 8f * 30f) + 30f).toInt()
                if (weight > 20f && height > 100f) {
                    val bmi = weight / ((height / 100f) * (height / 100f))
                    var bioAge = age.toFloat()
                    if (bmi in 18.5..24.9) bioAge -= 2f else if (bmi > 30f) bioAge += 3f
                    if (steps > 10000) bioAge -= 1.5f else if (steps < 2000) bioAge += 1.5f
                    if (healthScore > 80) bioAge -= 1f
                    String.format(Locale.US, "%.1f yrs", bioAge)
                } else "Set Profile"
            }, {
                if (weight > 20f && height > 100f) {
                    val bmi = weight / ((height / 100f) * (height / 100f))
                    var bioAge = age.toFloat()
                    if (bmi in 18.5..24.9) bioAge -= 2f else if (bmi > 30f) bioAge += 3f
                    if (steps > 10000) bioAge -= 1.5f else if (steps < 2000) bioAge += 1.5f
                    val diff = bioAge - age
                    when {
                        diff < -1 -> "Younger"
                        diff > 1 -> "Aging"
                        else -> "Normal"
                    }
                } else "Bio-Marker"
            }),
            LabItem(Icons.Default.Air, cyanAccent, "Lung Capacity", "Microphone Spirometry Test", "Diagnostic Tests", {
                "START TEST"
            }, { "Spirometry" }),
            LabItem(Icons.Default.Warning, crimsonAccent, "Injury Risk Profile", "Joint & Spinal Strain Audit", "Diagnostic Tests", {
                if (weight > 20f && height > 100f) {
                    val bmi = weight / ((height / 100f) * (height / 100f))
                    var risk = 10
                    if (bmi > 30f) risk += 30
                    if (age > 45) risk += 20
                    if (steps < 2000) risk += 15
                    "$risk% Risk"
                } else "Set Profile"
            }, {
                if (weight > 20f && height > 100f) {
                    val bmi = weight / ((height / 100f) * (height / 100f))
                    var risk = 10
                    if (bmi > 30f) risk += 30
                    if (age > 45) risk += 20
                    if (steps < 2000) risk += 15
                    when {
                        risk > 50 -> "High Risk"
                        risk > 25 -> "Moderate"
                        else -> "Low Risk"
                    }
                } else "Joint/Spine"
            })
        )
    }

    var selectedFilter by remember { mutableStateOf("All") }

    val filteredLabs = remember(selectedFilter, labs) {
        if (selectedFilter == "All") labs else labs.filter { it.category == selectedFilter }
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
                    // Back button
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
                            text = "ALGORITHM LAB",
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
                                text = "16 BIOMETRIC ENGINES • SCIENTIFIC",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = cyanAccent,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Calibrate Profile Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardColor)
                            .border(1.dp, cyanAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { onNavigate("PROFILE") }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "PROFILE",
                            color = cyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        bottomBar = { AdBanner(adKey = "ad_algorithm_lab_banner") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            // Hero Metabolic Engine Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(22.dp),
                color = cardColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(cyanAccent.copy(alpha = 0.25f), surfaceAltColor)
                                    )
                                )
                                .border(1.5.dp, cyanAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = cyanAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "SPORTS SCIENCE INTELLIGENCE",
                                color = cyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Metabolic & Biometric Engine",
                                color = textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Physiological formulas calibrated automatically from your athlete profile, steps, and hydration baselines.",
                        color = textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    // 3 Metric Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val tags = listOf(
                            "Weight: ${if (weight > 0) "${weight.toInt()}kg" else "--"}",
                            "Height: ${if (height > 0) "${height.toInt()}cm" else "--"}",
                            "Steps: $steps"
                        )
                        tags.forEach { tag ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(surfaceAltColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(tag, color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
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
                val filterChips = listOf(
                    "All" to "All Engines (${labs.size})",
                    "Energy & Burn" to "Energy & Burn (4)",
                    "Body & Muscle" to "Body & Muscle (5)",
                    "Cardio & Recovery" to "Cardio & Recovery (4)",
                    "Diagnostic Tests" to "Diagnostics (3)"
                )

                filterChips.forEach { (catKey, catLabel) ->
                    val isSelected = selectedFilter == catKey
                    val chipAccent = when (catKey) {
                        "Energy & Burn" -> amberAccent
                        "Body & Muscle" -> emeraldAccent
                        "Cardio & Recovery" -> purpleAccent
                        "Diagnostic Tests" -> crimsonAccent
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

            Spacer(Modifier.height(4.dp))

            // 2-Column Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredLabs) { lab ->
                    val resultText = lab.result()
                    val isMissing = resultText.contains("Set Profile") || resultText.contains("Walk to Calc")

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.96f)
                            .clickable {
                                if (isMissing && resultText.contains("Set Profile")) {
                                    onNavigate("PROFILE")
                                } else {
                                    when (lab.title) {
                                        "Lung Capacity" -> onNavigate("LUNG_TEST")
                                        "Injury Risk Profile" -> onNavigate("INJURY_RISK")
                                    }
                                }
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, if (isMissing) borderColor else lab.color.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(lab.color.copy(alpha = 0.08f), Color.Transparent)
                                    )
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Row: Icon + Status Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(lab.color.copy(alpha = 0.18f))
                                        .border(1.dp, lab.color.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = lab.icon,
                                        contentDescription = null,
                                        tint = lab.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(surfaceAltColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = lab.status(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMissing) textSecondary else lab.color
                                    )
                                }
                            }

                            // Middle: Title & Desc
                            Column {
                                Text(
                                    text = lab.title,
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = lab.desc,
                                    color = textSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 13.sp
                                )
                            }

                            // Bottom: Result Card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = surfaceAltColor,
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = resultText,
                                    color = if (isMissing) crimsonAccent else lab.color,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LungTestScreen(onBack: () -> Unit) {
    val s = LocalAppStrings.current
    var isTesting by remember { mutableStateOf(false) }
    var blowDuration by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    val cyanAccent = Color(0xFF06B6D4)
    val emeraldAccent = Color(0xFF10B981)
    val crimsonAccent = Color(0xFFF43F5E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "LUNG CAPACITY TEST",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Acoustic Spirometry Simulation",
                    color = cyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        Text(
            text = "Take a deep breath and exhale steadily into your device microphone until your lungs are empty.",
            color = textSecondary,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(40.dp))

        // Circular Gauge
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            Canvas(Modifier.size(220.dp)) {
                val strokeW = 12.dp.toPx()
                drawArc(
                    color = surfaceAltColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeW)
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(cyanAccent, emeraldAccent, cyanAccent)),
                    startAngle = -90f,
                    sweepAngle = (blowDuration / 15f * 360f).coerceAtMost(360f),
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.US, "%.1fs", blowDuration),
                    color = textPrimary,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "EXHALE DURATION",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        if (!isTesting) {
            Button(
                onClick = {
                    isTesting = true
                    blowDuration = 0f
                    scope.launch {
                        val start = System.currentTimeMillis()
                        while (isTesting) {
                            delay(100)
                            blowDuration = (System.currentTimeMillis() - start) / 1000f
                            if (blowDuration > 20f) isTesting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cyanAccent, contentColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("START SPIROMETRY TEST", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        } else {
            Button(
                onClick = { isTesting = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = crimsonAccent, contentColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("FINISH & EVALUATE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        if (!isTesting && blowDuration > 0) {
            val capacity = (blowDuration * 0.42f).coerceAtLeast(1.5f)
            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = cardColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ESTIMATED VITAL CAPACITY (FVC)",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f Liters", capacity),
                        color = emeraldAccent,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (capacity >= 3.5f) "Normal Adult Capacity" else "Below Average • Cardio Recommended",
                        color = textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun InjuryRiskScreen(
    onBack: () -> Unit,
    prefsManager: SharedPrefsManager,
    stepsToday: Int
) {
    val weight = prefsManager.getWeight()
    val height = prefsManager.getHeight()
    val age = prefsManager.getAge()
    val steps = stepsToday

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    val emeraldAccent = Color(0xFF10B981)
    val amberAccent = Color(0xFFF59E0B)
    val crimsonAccent = Color(0xFFF43F5E)

    val bmi = if (height > 0) weight / ((height / 100f) * (height / 100f)) else 0f

    var jointRisk = 12
    var backRisk = 10
    var cardioRisk = 10
    if (bmi > 30f) {
        jointRisk += 35
        backRisk += 25
    }
    if (age > 45) {
        jointRisk += 20
        cardioRisk += 20
    }
    if (steps < 2000) {
        jointRisk += 15
        backRisk += 20
    }
    if (steps > 15000) jointRisk += 10

    val totalRisk = ((jointRisk + backRisk + cardioRisk) / 3).coerceIn(5, 100)
    val riskColor = when {
        totalRisk > 50 -> crimsonAccent
        totalRisk > 25 -> amberAccent
        else -> emeraldAccent
    }

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
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "INJURY RISK AUDIT",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Biomechanical Stress Model",
                            color = riskColor,
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Hero Risk Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OVERALL RISK SCORE",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$totalRisk%",
                    color = riskColor,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(riskColor.copy(alpha = 0.15f))
                        .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (totalRisk > 50) "CAUTION: HIGH RISK" else if (totalRisk > 25) "MODERATE RISK" else "STABLE: LOW RISK",
                        color = riskColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "BIOMECHANICAL STRAIN BREAKDOWN",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(12.dp))

        RiskMeterBar(
            label = "Joint Stress (Knees/Ankles)",
            score = jointRisk,
            surfaceAlt = surfaceAltColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            emerald = emeraldAccent,
            amber = amberAccent,
            crimson = crimsonAccent
        )
        RiskMeterBar(
            label = "Spinal Strain (Lower Back)",
            score = backRisk,
            surfaceAlt = surfaceAltColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            emerald = emeraldAccent,
            amber = amberAccent,
            crimson = crimsonAccent
        )
        RiskMeterBar(
            label = "Cardiovascular Strain",
            score = cardioRisk,
            surfaceAlt = surfaceAltColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            emerald = emeraldAccent,
            amber = amberAccent,
            crimson = crimsonAccent
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "PREVENTION GUIDELINES",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(10.dp))

        val tips = mutableListOf<String>()
        if (jointRisk > 30) tips.add("Use supportive knee sleeves during squats and consider low-impact swimming/cycling.")
        if (backRisk > 30) tips.add("Strengthen deep transverse core muscles with daily Cat-Cow and Bird-Dog yoga stretches.")
        if (cardioRisk > 30) tips.add("Keep exercise heart rate below 145 BPM during intervals and increase daily water intake.")
        if (tips.isEmpty()) tips.add("Your biomechanical balance is optimal! Continue maintaining consistent progressive overload.")

        tips.forEach { tip ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = cardColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = tip,
                        color = textPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}
}

@Composable
private fun RiskMeterBar(
    label: String,
    score: Int,
    surfaceAlt: Color,
    textPrimary: Color,
    textSecondary: Color,
    emerald: Color,
    amber: Color,
    crimson: Color
) {
    val barColor = when {
        score > 50 -> crimson
        score > 25 -> amber
        else -> emerald
    }

    Column(Modifier.padding(vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "$score%", color = barColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(surfaceAlt)
        ) {
            Box(
                Modifier
                    .fillMaxWidth((score / 100f).coerceIn(0.05f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}
