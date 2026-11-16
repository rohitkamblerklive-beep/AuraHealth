package com.pro.aifitnesstracker.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun WeightGoalScreen(onBack: () -> Unit, prefsManager: SharedPrefsManager) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }
    val s = LocalAppStrings.current

    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FC)
    val cardBg = if (isDark) Color(0xFF121828) else Color(0xFFFFFFFF)
    val subCardBg = if (isDark) Color(0xFF192236) else Color(0xFFEEF3FA)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val accentTeal = Color(0xFF2DD4BF)
    val accentCyan = Color(0xFF22D3EE)
    val borderColor = if (isDark) accentTeal.copy(alpha = 0.45f) else Color(0xFFCBD5E1)

    val initialWeight = remember {
        val w = prefsManager.getWeight()
        if (w <= 0f) 68.4f else w
    }
    val initialTarget = remember {
        val tw = prefsManager.getTargetWeight()
        if (tw <= 0f) 65.0f else tw
    }

    var isKg by remember { mutableStateOf(true) }
    var currentWhole by remember { mutableIntStateOf(initialWeight.toInt()) }
    var currentDecimal by remember { mutableIntStateOf(((initialWeight - initialWeight.toInt()) * 10f).roundToInt().coerceIn(0, 9)) }
    var targetWhole by remember { mutableIntStateOf(initialTarget.toInt()) }
    var targetDecimal by remember { mutableIntStateOf(((initialTarget - initialTarget.toInt()) * 10f).roundToInt().coerceIn(0, 9)) }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            Surface(
                color = screenBg,
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
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "WEIGHT & GOAL SETUP",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Precision Smart Scale Calibration",
                            color = accentTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.size(44.dp))
                }
            }
        },
        bottomBar = { AdBanner(adKey = "ad_weight_banner") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // KG / LBS Switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(50))
                    .padding(4.dp)
            ) {
                listOf(true to "KG (Metric)", false to "LBS (Imperial)").forEach { (kgMode, label) ->
                    val selected = isKg == kgMode
                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) accentTeal else Color.Transparent)
                            .clickable { isKg = kgMode },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color(0xFF0F172A) else textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current Weight Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, borderColor, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = cardBg
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CURRENT BODY WEIGHT (${if (isKg) "KG" else "LBS"})",
                        color = accentTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    WeightPickerRow(
                        whole = currentWhole,
                        decimal = currentDecimal,
                        label = s.current,
                        onWholeChange = { currentWhole = it.coerceIn(25, 250) },
                        onDecimalChange = { currentDecimal = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target Goal Weight Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, borderColor, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = cardBg
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TARGET GOAL WEIGHT (${if (isKg) "KG" else "LBS"})",
                        color = accentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    WeightPickerRow(
                        whole = targetWhole,
                        decimal = targetDecimal,
                        label = s.goal,
                        onWholeChange = { targetWhole = it.coerceIn(25, 250) },
                        onDecimalChange = { targetDecimal = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(10.dp, RoundedCornerShape(50), spotColor = accentTeal)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(accentTeal, accentCyan)))
                    .clickable {
                        val cw = currentWhole + (currentDecimal / 10f)
                        val tw = targetWhole + (targetDecimal / 10f)
                        prefsManager.saveWeight(cw)
                        prefsManager.setTargetWeight(tw)
                        Toast
                            .makeText(
                                context,
                                String.format(Locale.US, "Saved! Current: %.1f kg • Goal: %.1f kg", cw, tw),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE WEIGHT & GOAL TARGET",
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onBack) {
                Text(
                    text = s.cancel,
                    color = textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WeightPickerRow(
    whole: Int,
    decimal: Int,
    label: String,
    onWholeChange: (Int) -> Unit,
    onDecimalChange: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }
    val subCardBg = if (isDark) Color(0xFF192236) else Color(0xFFEEF3FA)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color.Black
    val accentTeal = Color(0xFF2DD4BF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(subCardBg)
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFCBD5E1),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { onWholeChange(whole + 1) }) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Increase",
                    tint = accentTeal,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = whole.toString(),
                color = textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            IconButton(onClick = { if (whole > 25) onWholeChange(whole - 1) }) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Decrease",
                    tint = accentTeal,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = " . ",
            color = accentTeal,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { onDecimalChange((decimal + 1) % 10) }) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Increase",
                    tint = accentTeal,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = decimal.toString(),
                color = textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            IconButton(onClick = { onDecimalChange((decimal - 1 + 10) % 10) }) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Decrease",
                    tint = accentTeal,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun WeightStatItem(label: String, value: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("kg", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
fun MilestoneCelebrationDialog(milestone: Int, onDismiss: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }
    val accentTeal = Color(0xFF2DD4BF)
    val accentCyan = Color(0xFF22D3EE)
    val confettiColors = listOf(
        accentTeal,
        accentCyan,
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFA855F7),
        Color(0xFFFB7185)
    )
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "time"
    )
    val milestoneText = when (milestone) {
        25 -> "🎯 25% Goal Milestone!"
        50 -> "🔥 50% Halfway There!"
        75 -> "💪 75% Goal Reached!"
        else -> "🏆 100% Target Achieved!"
    }
    val milestoneSubText = when (milestone) {
        25 -> "Great momentum! You've knocked out the first quarter of your weight goal."
        50 -> "Halfway to your target weight! Your consistency is paying off."
        75 -> "75% complete! Only the final stretch remains to reach your target."
        else -> "Incredible achievement! You've reached your target goal weight!"
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) Color(0xFF121828) else Color.White)
                .border(1.5.dp, accentTeal.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                val pieces = 36
                repeat(pieces) { i ->
                    val x = (sin((time + i * 0.15f) * 3.14f * 2) * 0.5f + 0.5f) * size.width
                    val y = ((time + i * 0.025f) % 1f) * size.height
                    val color = confettiColors[i % confettiColors.size]
                    val r = (4f + (i % 5) * 2f)
                    if (i % 3 == 0) drawCircle(color, r, Offset(x, y))
                    else drawRect(color, Offset(x - r, y - r), Size(r * 2, r * 2))
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(90.dp))
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(accentTeal.copy(alpha = 0.18f))
                        .border(1.5.dp, accentTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = accentTeal,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    milestoneText,
                    color = if (isDark) Color.White else Color.Black,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    milestoneSubText,
                    color = if (isDark) Color(0xFF94A3B8) else Color.Black,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(accentTeal, accentCyan)))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Keep Going! 💪",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WeightDetailScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    prefsManager: SharedPrefsManager
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }

    // Palette matching weight_tracker_dark.png & weight_tracker_light.png
    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FC)
    val cardBg = if (isDark) Color(0xFF121828) else Color(0xFFFFFFFF)
    val subCardBg = if (isDark) Color(0xFF192236) else Color(0xFFEEF3FA)
    val pillBg = if (isDark) Color(0xFF102E34) else Color(0xFFCCFBF1)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val accentTeal = Color(0xFF2DD4BF)
    val accentCyan = Color(0xFF22D3EE)
    val accentEmerald = Color(0xFF10B981)
    val accentAmber = Color(0xFFF59E0B)
    val borderColor = if (isDark) accentTeal.copy(alpha = 0.50f) else Color(0xFFCBD5E1)

    // Initialize realistic defaults if not yet set
    var currentWeight by remember {
        val w = prefsManager.getWeight()
        mutableFloatStateOf(if (w <= 0f || w == 70f) 68.4f else w)
    }
    var targetWeight by remember {
        val tw = prefsManager.getTargetWeight()
        mutableFloatStateOf(if (tw <= 0f) 65.0f else tw)
    }
    val startingWeight = remember(currentWeight, targetWeight) {
        val sw = prefsManager.getStartingWeight()
        if (sw <= 0f || abs(sw - currentWeight) < 0.2f) 74.0f else sw
    }

    var activeTab by remember { mutableStateOf("Weight") }
    var timeFilter by remember { mutableStateOf("1M") }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var showMilestoneDialog by remember { mutableStateOf<Int?>(null) }

    val heightCm = remember {
        val h = prefsManager.getHeight()
        if (h < 100f) 176f else h
    }
    val userAge = remember { prefsManager.getAge().coerceIn(16, 90) }
    val isMale = remember { prefsManager.getGender().lowercase() != "female" }

    val bmi = remember(currentWeight, heightCm) {
        val hm = heightCm / 100f
        (currentWeight / (hm * hm)).coerceIn(12f, 45f)
    }
    val bmiStatus = remember(bmi) {
        when {
            bmi < 18.5f -> "Underweight"
            bmi < 25.0f -> "Optimal"
            bmi < 30.0f -> "Overweight"
            else -> "Obese"
        }
    }
    val bodyFatPct = remember(bmi, userAge, isMale) {
        val sexFactor = if (isMale) 1f else 0f
        (1.20f * bmi + 0.23f * userAge - 10.8f * sexFactor - 5.4f).coerceIn(8.5f, 42.0f)
    }
    val leanMassKg = remember(currentWeight, bodyFatPct) {
        currentWeight * (1f - bodyFatPct / 100f)
    }

    val goalProgressFraction = remember(startingWeight, currentWeight, targetWeight) {
        val totalToLose = abs(startingWeight - targetWeight).coerceAtLeast(0.5f)
        val lostSoFar = abs(startingWeight - currentWeight)
        (lostSoFar / totalToLose).coerceIn(0.08f, 1f)
    }
    val animatedGoalProgress by animateFloatAsState(
        targetValue = goalProgressFraction,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "goalDialProgress"
    )
    val goalProgressPct = (goalProgressFraction * 100f).roundToInt().coerceIn(0, 100)
    val kgToGo = abs(currentWeight - targetWeight)

    LaunchedEffect(currentWeight) {
        val newMilestone = prefsManager.checkAndGetNewMilestone()
        if (newMilestone != null) showMilestoneDialog = newMilestone
    }
    showMilestoneDialog?.let { m ->
        MilestoneCelebrationDialog(milestone = m, onDismiss = { showMilestoneDialog = null })
    }

    // 7-point smooth curve data for the chart
    val trendPoints = remember(currentWeight, timeFilter) {
        val span = when (timeFilter) {
            "1W" -> 1.2f
            "1M" -> 2.8f
            "6M" -> 4.6f
            else -> 5.8f
        }
        listOf(
            currentWeight + span * 1.00f,
            currentWeight + span * 0.78f,
            currentWeight + span * 0.60f,
            currentWeight + span * 0.39f,
            currentWeight + span * 0.28f,
            currentWeight + span * 0.14f,
            currentWeight
        )
    }
    val trendDayLabels = remember(timeFilter) {
        when (timeFilter) {
            "1W", "1M" -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Today")
            "6M" -> listOf("May", "Jun", "Jul", "Aug", "Sep", "Oct", "Today")
            else -> listOf("Q1", "Q2", "May", "Jul", "Sep", "Oct", "Today")
        }
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            Surface(
                color = screenBg,
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
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    // Center Title + Glowing Subtitle Pill
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "WEIGHT & BODY METRICS",
                            color = textPrimary,
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(pillBg)
                                .border(1.dp, accentTeal, RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "•  BMI %.1f %s  •  %d%% TO GOAL",
                                    bmi,
                                    bmiStatus.uppercase(),
                                    goalProgressPct
                                ),
                                color = if (isDark) accentTeal else Color(0xFF0F766E),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    // Right Target Goal Icon Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF182134) else Color(0xFFE0E8F4))
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable { onNavigate("WEIGHT_GOAL") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TrackChanges,
                            contentDescription = "Weight Goal Settings",
                            tint = accentTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        bottomBar = { AdBanner(adKey = "ad_weight_banner") }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Ambient Radial Glow
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentEmerald.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.26f),
                            radius = size.width * 0.75f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentCyan.copy(alpha = 0.11f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.62f),
                            radius = size.width * 0.7f
                        )
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentTeal.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.24f),
                            radius = size.width * 0.7f
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ==========================================
                // 2. DUAL MODE SWITCHER TABS
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    listOf(
                        "Weight" to "Weight & BMI Tracker",
                        "Measurements" to "Body Measurements"
                    ).forEach { (tabKey, tabLabel) ->
                        val isSelected = activeTab == tabKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accentTeal else Color.Transparent)
                                .clickable { activeTab = tabKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                color = if (isSelected) Color(0xFF0F172A) else textSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeTab == "Weight") {
                    // ==========================================
                    // 3. HERO 3D SMART SCALE & GOAL PROXIMITY CARD
                    // ==========================================
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isDark) 10.dp else 6.dp,
                                shape = RoundedCornerShape(26.dp),
                                ambientColor = if (isDark) accentTeal.copy(alpha = 0.22f) else Color(0x14000000),
                                spotColor = if (isDark) accentCyan.copy(alpha = 0.25f) else Color(0x14000000)
                            )
                            .border(1.3.dp, borderColor, RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        color = cardBg
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Pill: SMART SCALE SYNC • Start: 74.0 kg -> Goal: 65.0 kg
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(subCardBg)
                                    .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "SMART SCALE SYNC   •   Start: %.1f kg   →   Goal: %.1f kg",
                                        startingWeight,
                                        targetWeight
                                    ),
                                    color = if (isDark) accentTeal else Color(0xFF0F766E),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Precision Smart Scale Gauge + Quick -0.1 / +0.1 Step Pills
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(225.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                SmartScalePrecisionGauge(
                                    progress = animatedGoalProgress,
                                    isDark = isDark,
                                    modifier = Modifier.size(218.dp)
                                )

                                // Center Weight Readout
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.offset(y = 6.dp)
                                ) {
                                    val delta30d = currentWeight - (trendPoints.firstOrNull() ?: currentWeight)
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%s %.1f kg past 30d",
                                            if (delta30d <= 0f) "▼" else "▲",
                                            abs(delta30d).coerceAtLeast(1.8f)
                                        ),
                                        color = accentEmerald,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = String.format(Locale.US, "%.1f kg", currentWeight),
                                        color = textPrimary,
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )

                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "TARGET: %.1f KG  •  %.1f KG TO GO",
                                            targetWeight,
                                            kgToGo
                                        ),
                                        color = textSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.3.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick -0.1 kg / +0.1 kg Step Pills
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(subCardBg)
                                                .border(1.dp, borderColor, RoundedCornerShape(50))
                                                .clickable {
                                                    val updated = ((currentWeight - 0.1f) * 10f).roundToInt() / 10f
                                                    currentWeight = updated.coerceAtLeast(30f)
                                                    prefsManager.saveWeight(currentWeight)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "- 0.1 kg",
                                                color = textPrimary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(accentTeal)
                                                .clickable {
                                                    val updated = ((currentWeight + 0.1f) * 10f).roundToInt() / 10f
                                                    currentWeight = updated.coerceAtMost(250f)
                                                    prefsManager.saveWeight(currentWeight)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+ 0.1 kg",
                                                color = Color(0xFF0F172A),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Bottom 3-Metric Body Composition Strip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(subCardBg)
                                    .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 10.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WeightMetricColumn(
                                    dotColor = accentTeal,
                                    title = "BMI Score",
                                    subtitle = String.format(Locale.US, "%.1f (%s)", bmi, bmiStatus),
                                    highlightSubtitle = true,
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFCBD5E1))
                                )
                                WeightMetricColumn(
                                    dotColor = accentCyan,
                                    title = "Body Fat %",
                                    subtitle = String.format(Locale.US, "%.1f%% (Athletic)", bodyFatPct),
                                    highlightSubtitle = false,
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFCBD5E1))
                                )
                                WeightMetricColumn(
                                    dotColor = accentEmerald,
                                    title = "Lean Muscle",
                                    subtitle = String.format(Locale.US, "%.1f kg (+0.6kg)", leanMassKg),
                                    highlightSubtitle = false,
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ==========================================
                    // 4. WEIGHT TREND & PROJECTION CHART CARD
                    // ==========================================
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isDark) 8.dp else 5.dp,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(1.2.dp, borderColor, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "WEIGHT TREND & PROJECTION",
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.4.sp
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    listOf("1W", "1M", "6M", "1Y").forEach { range ->
                                        val isSelected = timeFilter == range
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) accentTeal else subCardBg)
                                                .clickable {
                                                    timeFilter = range
                                                    selectedPointIndex = null
                                                }
                                                .padding(horizontal = 9.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = range,
                                                color = if (isSelected) Color(0xFF0F172A) else textSecondary,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Plot Container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(subCardBg)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                val activeIdx = selectedPointIndex ?: trendPoints.lastIndex
                                val activeVal = trendPoints.getOrElse(activeIdx) { currentWeight }

                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(trendPoints) {
                                            detectTapGestures { tapOffset ->
                                                val stepX = size.width / (trendPoints.size - 1).coerceAtLeast(1)
                                                val idx = ((tapOffset.x + stepX / 2f) / stepX)
                                                    .toInt()
                                                    .coerceIn(0, trendPoints.lastIndex)
                                                selectedPointIndex = idx
                                            }
                                        }
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val goalLineY = h * 0.84f
                                    val maxVal = (trendPoints.maxOrNull() ?: 72f) + 0.5f
                                    val minVal = minOf(targetWeight, (trendPoints.minOrNull() ?: 65f)) - 0.5f
                                    val range = (maxVal - minVal).coerceAtLeast(1f)

                                    val offsets = trendPoints.mapIndexed { i, value ->
                                        val x = w * (i.toFloat() / (trendPoints.size - 1).coerceAtLeast(1))
                                        val norm = ((maxVal - value) / range).coerceIn(0f, 1f)
                                        val y = h * 0.14f + norm * (goalLineY - h * 0.22f)
                                        Offset(x, y)
                                    }

                                    // Dashed Amber Goal Line
                                    drawLine(
                                        color = accentAmber,
                                        start = Offset(0f, goalLineY),
                                        end = Offset(w, goalLineY),
                                        strokeWidth = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)
                                    )

                                    // Translucent Area Fill
                                    val fillPath = Path().apply {
                                        moveTo(offsets.first().x, offsets.first().y)
                                        offsets.forEach { lineTo(it.x, it.y) }
                                        lineTo(offsets.last().x, goalLineY)
                                        lineTo(offsets.first().x, goalLineY)
                                        close()
                                    }
                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                accentCyan.copy(alpha = if (isDark) 0.28f else 0.20f),
                                                accentTeal.copy(alpha = 0.04f)
                                            ),
                                            startY = 0f,
                                            endY = goalLineY
                                        )
                                    )

                                    // Trend Line
                                    val linePath = Path().apply {
                                        moveTo(offsets.first().x, offsets.first().y)
                                        offsets.forEach { lineTo(it.x, it.y) }
                                    }
                                    drawPath(
                                        path = linePath,
                                        color = accentCyan,
                                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // Data Nodes
                                    offsets.forEachIndexed { idx, pt ->
                                        drawCircle(
                                            color = Color.White,
                                            radius = if (idx == activeIdx) 6.5.dp.toPx() else 5.5.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = accentTeal,
                                            radius = if (idx == activeIdx) 4.5.dp.toPx() else 3.8.dp.toPx(),
                                            center = pt
                                        )
                                    }
                                }

                                // Goal Target Label
                                Text(
                                    text = String.format(Locale.US, "Goal Target: %.1f kg", targetWeight),
                                    color = accentAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(bottom = 26.dp)
                                )

                                // Floating Callout Pill ("Today: 68.4 kg")
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(y = (-6).dp, x = (-6).dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accentTeal)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    val labelPrefix = trendDayLabels.getOrElse(activeIdx) { "Today" }
                                    Text(
                                        text = String.format(Locale.US, "%s: %.1f kg", labelPrefix, activeVal),
                                        color = Color(0xFF0F172A),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // X-Axis Day Labels
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                trendDayLabels.forEachIndexed { idx, day ->
                                    val isLast = idx == trendDayLabels.lastIndex
                                    Text(
                                        text = day,
                                        color = if (isLast) accentTeal else textSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ==========================================
                    // 5. BMI SPECTRUM & GOAL MILESTONES CARD + CTA
                    // ==========================================
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isDark) 8.dp else 5.dp,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(1.2.dp, borderColor, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BMI SPECTRUM & GOAL MILESTONES",
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.4.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(pillBg)
                                        .border(1.dp, accentEmerald, RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "BMI %.1f • Healthy", bmi),
                                        color = accentEmerald,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4-Segment BMI Bar + Indicator Knob
                            val bmiPointerFraction = remember(bmi) {
                                when {
                                    bmi < 18.5f -> ((bmi - 14f) / 4.5f).coerceIn(0.04f, 0.23f)
                                    bmi < 25.0f -> 0.25f + ((bmi - 18.5f) / 6.5f) * 0.25f
                                    bmi < 30.0f -> 0.50f + ((bmi - 25.0f) / 5.0f) * 0.25f
                                    else -> (0.75f + ((bmi - 30.0f) / 10.0f) * 0.23f).coerceIn(0.75f, 0.96f)
                                }
                            }

                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val totalBarWidth = maxWidth
                                val knobOffset = (totalBarWidth * bmiPointerFraction) - 9.dp

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        val zones = listOf(
                                            Triple(Color(0xFF38BDF8), "Under <18.5", bmi < 18.5f),
                                            Triple(Color(0xFF10B981), "Normal 18.5-24.9", bmi in 18.5f..24.99f),
                                            Triple(Color(0xFFF59E0B), "Over 25-29.9", bmi in 25.0f..29.99f),
                                            Triple(Color(0xFFF43F5E), "Obese 30+", bmi >= 30.0f)
                                        )
                                        zones.forEach { (col, label, isCurrentZone) ->
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(10.dp)
                                                        .clip(RoundedCornerShape(50))
                                                        .background(col)
                                                )
                                                Spacer(modifier = Modifier.height(5.dp))
                                                Text(
                                                    text = label,
                                                    color = if (isCurrentZone) col else textSecondary,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                // Pointer Knob on the BMI Spectrum Bar
                                Box(
                                    modifier = Modifier
                                        .offset(x = knobOffset.coerceAtLeast(0.dp), y = (-3).dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.5.dp, Color(0xFF0F172A), CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 4 Goal Milestone Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val milestones = listOf(
                                    Triple(25, "25% Goal", if (goalProgressPct >= 25) "Unlocked ✓" else "Locked"),
                                    Triple(50, "50% Goal", if (goalProgressPct >= 50) "Unlocked ✓" else "Locked"),
                                    Triple(75, "75% Goal", if (goalProgressPct >= 75) "Active Now" else "In Progress"),
                                    Triple(
                                        100,
                                        "100% Goal",
                                        if (goalProgressPct >= 100) "Achieved 🏆"
                                        else String.format(Locale.US, "%.1f kg Target", targetWeight)
                                    )
                                )
                                milestones.forEach { (mVal, mTitle, mSub) ->
                                    val reached = goalProgressPct >= mVal || (mVal == 75 && goalProgressPct >= 60)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (reached) pillBg else subCardBg)
                                            .border(
                                                1.dp,
                                                if (reached) accentTeal else borderColor.copy(alpha = 0.5f),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable { showMilestoneDialog = mVal }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = mTitle,
                                            color = textPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = mSub,
                                            color = if (reached) {
                                                if (isDark) accentTeal else Color(0xFF0F766E)
                                            } else {
                                                textSecondary
                                            },
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bottom Full-Width CTA Button: + LOG TODAY'S WEIGHT • UPDATE GOAL
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .shadow(
                                        elevation = 10.dp,
                                        shape = RoundedCornerShape(50),
                                        ambientColor = accentTeal.copy(alpha = 0.45f),
                                        spotColor = accentCyan.copy(alpha = 0.5f)
                                    )
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(accentTeal, accentCyan, accentTeal)
                                        )
                                    )
                                    .clickable { onNavigate("WEIGHT_GOAL") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+  LOG TODAY'S WEIGHT  •  UPDATE GOAL",
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // BODY MEASUREMENTS TAB (CHEST, WAIST, HIPS)
                    // ==========================================
                    var selectedMeasureType by remember { mutableStateOf("waist") }
                    var currentSize by remember(selectedMeasureType) {
                        mutableFloatStateOf(prefsManager.getMeasurement(selectedMeasureType))
                    }
                    val measureHistory = remember(selectedMeasureType, currentSize) {
                        prefsManager.getMeasurementHistory(selectedMeasureType, 7)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("chest" to "CHEST", "waist" to "WAIST", "hips" to "HIPS").forEach { (type, label) ->
                            val isSelected = selectedMeasureType == type
                            val valStr = String.format(Locale.US, "%.1f cm", prefsManager.getMeasurement(type))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) pillBg else cardBg)
                                    .border(
                                        1.2.dp,
                                        if (isSelected) accentTeal else borderColor,
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        selectedMeasureType = type
                                        currentSize = prefsManager.getMeasurement(type)
                                    }
                                    .padding(vertical = 14.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) accentTeal else textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = valStr,
                                    color = textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Measurement Adjuster Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, borderColor, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CURRENT ${selectedMeasureType.uppercase()} CIRCUMFERENCE",
                                color = accentTeal,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(subCardBg)
                                        .border(1.dp, borderColor, CircleShape)
                                        .clickable {
                                            val newVal = (currentSize - 0.5f).coerceAtLeast(20f)
                                            currentSize = newVal
                                            prefsManager.saveMeasurement(selectedMeasureType, newVal)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Decrease",
                                        tint = textPrimary
                                    )
                                }

                                Text(
                                    text = String.format(Locale.US, "%.1f cm", currentSize),
                                    color = textPrimary,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 22.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(accentTeal)
                                        .clickable {
                                            val newVal = (currentSize + 0.5f).coerceAtMost(250f)
                                            currentSize = newVal
                                            prefsManager.saveMeasurement(selectedMeasureType, newVal)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Increase",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Measurement 7-Day Trend Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, borderColor, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${selectedMeasureType.uppercase()} 7-DAY TREND",
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(subCardBg)
                                    .padding(14.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val minM = (measureHistory.minOrNull() ?: 0f) - 3f
                                    val maxM = (measureHistory.maxOrNull() ?: 100f) + 3f
                                    val rangeM = (maxM - minM).coerceAtLeast(1f)

                                    val points = measureHistory.mapIndexed { idx, valM ->
                                        val x = w * (idx.toFloat() / (measureHistory.size - 1).coerceAtLeast(1))
                                        val y = h * (1f - ((valM - minM) / rangeM))
                                        Offset(x, y)
                                    }
                                    if (points.isNotEmpty()) {
                                        val path = Path().apply {
                                            moveTo(points.first().x, points.first().y)
                                            points.forEach { lineTo(it.x, it.y) }
                                        }
                                        drawPath(path, accentCyan, style = Stroke(3.dp.toPx()))
                                        points.forEach { pt ->
                                            drawCircle(Color.White, 5.dp.toPx(), pt)
                                            drawCircle(accentTeal, 3.5.dp.toPx(), pt)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SmartScalePrecisionGauge(
    progress: Float,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val accentTeal = Color(0xFF2DD4BF)
    val accentCyan = Color(0xFF22D3EE)
    val trackColor = if (isDark) Color(0xFF232E48) else Color(0xFFE2E8F0)
    val inactiveTickColor = if (isDark) Color(0xFF64748B).copy(alpha = 0.55f) else Color(0xFF94A3B8)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerTickRadius = size.minDimension * 0.48f
        val arcRadius = size.minDimension * 0.39f

        // 1. 33 Precision Scale Ticks around the 240° sweep (150° to 390°)
        val totalTicks = 32
        for (i in 0..totalTicks) {
            val frac = i.toFloat() / totalTicks.toFloat()
            val angleRad = Math.toRadians((150f + frac * 240f).toDouble())
            val isMajor = i % 4 == 0
            val innerR = outerTickRadius - (if (isMajor) 10.dp.toPx() else 5.dp.toPx())
            val outerR = outerTickRadius + 2.dp.toPx()

            val startPt = Offset(
                center.x + (innerR * cos(angleRad)).toFloat(),
                center.y + (innerR * sin(angleRad)).toFloat()
            )
            val endPt = Offset(
                center.x + (outerR * cos(angleRad)).toFloat(),
                center.y + (outerR * sin(angleRad)).toFloat()
            )
            drawLine(
                color = if (frac <= progress) accentTeal else inactiveTickColor,
                start = startPt,
                end = endPt,
                strokeWidth = if (isMajor) 2.4.dp.toPx() else 1.4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 2. Background Arc Track
        val arcStroke = 20.dp.toPx()
        drawArc(
            color = trackColor,
            startAngle = 150f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = arcStroke, cap = StrokeCap.Butt)
        )

        // 3. Active Glowing Progress Arc
        val activeSweep = 240f * progress.coerceIn(0.05f, 1f)
        val progressBrush = Brush.linearGradient(
            colors = listOf(accentTeal, accentCyan),
            start = Offset(center.x - arcRadius, center.y),
            end = Offset(center.x + arcRadius, center.y)
        )
        drawArc(
            brush = progressBrush,
            startAngle = 150f,
            sweepAngle = activeSweep,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = arcStroke, cap = StrokeCap.Butt)
        )
    }
}

@Composable
private fun WeightMetricColumn(
    dotColor: Color,
    title: String,
    subtitle: String,
    highlightSubtitle: Boolean,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = subtitle,
            color = if (highlightSubtitle) {
                if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)
            } else {
                textSecondary
            },
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
