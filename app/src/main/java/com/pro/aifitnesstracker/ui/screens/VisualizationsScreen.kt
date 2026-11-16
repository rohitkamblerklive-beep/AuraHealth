package com.pro.aifitnesstracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.SharedPrefsManager
import kotlin.math.*
import com.pro.aifitnesstracker.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizationsScreen(
    prefsManager: SharedPrefsManager,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val topBarBg = if (isDark) Color(0xFF080B14) else Color(0xFFEEF2F6)
    val canvasBgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF080B14), Color(0xFF0D121F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEEF2F6), Color(0xFFE2E8F0)))
    }
    val textPrimary = if (isDark) Color.White else Color.Black

    Scaffold(
        containerColor = topBarBg,
        topBar = {
            Surface(
                color = topBarBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            strings.visualizations,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(canvasBgGradient)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Radar Chart
            VisualizationCard(title = strings.fitnessBalance.uppercase(), subtitle = strings.radarOverview) {
                val stepGoal = prefsManager.getStepGoal().coerceAtLeast(1)
                val calorieGoal = prefsManager.getCalorieGoal().coerceAtLeast(1)
                val stepFactor = (prefsManager.getStepsToday().toFloat() / stepGoal).coerceIn(0f, 1f)
                val waterFactor = (prefsManager.getWaterGlassesToday() / 8f).coerceIn(0f, 1f)
                val sleepFactor = (prefsManager.getSleepHoursToday() / 8f).coerceIn(0f, 1f)
                val hrFactor = if (prefsManager.getLastBpm() in 60..100) 1f else 0.5f
                val calFactor = (prefsManager.getStepsToday() * 0.04f / calorieGoal).coerceIn(0f, 1f)

                RadarChart(
                    values = listOf(stepFactor, waterFactor, sleepFactor, hrFactor, calFactor),
                    labels = listOf("Steps", "Water", "Sleep", "Heart", "Calories"),
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 16.dp)
                )
            }

            // 2. ECG Heart Rate
            VisualizationCard(title = strings.liveHeartRate.uppercase(), subtitle = strings.ecgPulseAnimation) {
                val bpm = prefsManager.getLastBpm().let { if (it <= 0) 72 else it }
                ECGHeartRateWave(
                    bpm = bpm,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .padding(horizontal = 8.dp)
                )
            }

            // 3. Blood Pressure Gauge
            VisualizationCard(title = strings.bloodPressure.uppercase(), subtitle = strings.gaugeMeter) {
                val bp = prefsManager.getLastBloodPressure()
                val systolic = if (bp.first > 0) bp.first else 120
                val diastolic = if (bp.second > 0) bp.second else 80
                BloodPressureGauge(
                    systolic = systolic,
                    diastolic = diastolic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp)
                )
            }

            // 4. BMI Meter
            VisualizationCard(title = strings.bmiIndex.uppercase(), subtitle = strings.bodyMassIndex) {
                val weight = prefsManager.getWeight()
                val heightCm = prefsManager.getHeight()
                val bmi = if (heightCm > 0) {
                    weight / ((heightCm / 100f) * (heightCm / 100f))
                } else 22f
                BMIMeter(
                    bmi = bmi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // 5. Line Area Chart
            VisualizationCard(title = strings.sevenDayStepsTrend.uppercase(), subtitle = strings.activityLineChart) {
                val history = prefsManager.getHistoryList(7, "STEPS")
                LineAreaChart(
                    data = history.map { it.toFloat() },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 8.dp)
                )
            }

            // 6. Weekly Heatmap
            VisualizationCard(title = strings.weeklyActivity.uppercase(), subtitle = strings.heatmapGrid) {
                val stepsHistory = prefsManager.getHistoryList(28, "STEPS")
                val stepGoal = prefsManager.getStepGoal()
                WeeklyHeatmapGrid(
                    stepsHistory = stepsHistory,
                    stepGoal = stepGoal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared Card Wrapper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VisualizationCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF131A2A) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF1F293D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val accent = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(14.dp)
                        .background(accent, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        title,
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        color = textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  1. Radar Chart
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RadarChart(
    values: List<Float>,
    labels: List<String>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "radar_anim"
    )
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = minOf(cx, cy) * 0.72f
            val n = values.size
            val angleStep = (2 * PI / n).toFloat()
            val startAngle = -PI.toFloat() / 2f

            // Grid rings
            for (ring in 1..4) {
                val r = radius * ring / 4f
                val path = Path()
                for (i in 0 until n) {
                    val angle = startAngle + i * angleStep
                    val x = cx + r * cos(angle)
                    val y = cy + r * sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color = onSurfaceColor.copy(alpha = 0.1f), style = Stroke(1.dp.toPx()))
            }

            // Axis lines
            for (i in 0 until n) {
                val angle = startAngle + i * angleStep
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.12f),
                    start = Offset(cx, cy),
                    end = Offset(cx + radius * cos(angle), cy + radius * sin(angle)),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Data polygon
            val dataPath = Path()
            for (i in 0 until n) {
                val angle = startAngle + i * angleStep
                val r = radius * values[i] * animProgress
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            drawPath(
                dataPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.35f),
                        accentColor.copy(alpha = 0.05f)
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                )
            )
            drawPath(dataPath, color = accentColor, style = Stroke(2.dp.toPx()))

            // Vertex dots
            for (i in 0 until n) {
                val angle = startAngle + i * angleStep
                val r = radius * values[i] * animProgress
                val vx = cx + r * cos(angle)
                val vy = cy + r * sin(angle)
                drawCircle(color = accentColor, radius = 5.dp.toPx(), center = Offset(vx, vy))
                drawCircle(color = surfaceColor, radius = 3.dp.toPx(), center = Offset(vx, vy))
            }
        }

        // Label overlay
        Box(modifier = Modifier.fillMaxSize()) {
            val n = values.size
            val angleStep = (2 * PI / n).toFloat()
            val startAngle = -PI.toFloat() / 2f
            values.indices.forEach { i ->
                val angle = startAngle + i * angleStep
                val labelFrac = 0.88f
                val xFrac = (0.5f + labelFrac * cos(angle).toFloat() * 0.48f).coerceIn(0.05f, 0.95f)
                val yFrac = (0.5f + labelFrac * sin(angle).toFloat() * 0.48f).coerceIn(0.05f, 0.95f)
                Text(
                    text = labels[i],
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(BiasAlignment(xFrac * 2 - 1, yFrac * 2 - 1))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  2. ECG Heart Rate Wave
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ECGHeartRateWave(
    bpm: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val beatMs = (60000 / bpm.coerceIn(40, 200))
    val infiniteTransition = rememberInfiniteTransition(label = "ecg")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(beatMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ecg_phase"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val segmentCount = 200
            val path = Path()

            for (i in 0..segmentCount) {
                val t = i / segmentCount.toFloat()
                val x = t * w
                val adj = (t + phase) % 1f
                val y = midY + when {
                    adj < 0.05f -> 0f
                    adj < 0.10f -> -h * 0.10f * sin((adj - 0.05f) / 0.05f * PI).toFloat()
                    adj < 0.15f -> h * 0.05f * sin((adj - 0.10f) / 0.05f * PI).toFloat()
                    adj < 0.20f -> -h * 0.45f * sin((adj - 0.15f) / 0.05f * PI).toFloat()
                    adj < 0.25f -> h * 0.08f * sin((adj - 0.20f) / 0.05f * PI).toFloat()
                    adj < 0.40f -> -h * 0.07f * sin((adj - 0.25f) / 0.15f * PI).toFloat()
                    else -> 0f
                }
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path,
                brush = Brush.horizontalGradient(
                    listOf(color.copy(0f), color.copy(0.4f), color.copy(0.7f), color.copy(0.4f), color.copy(0f))
                ),
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(path, color = color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }

        Spacer(Modifier.height(8.dp))
        Text("$bpm BPM", color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(
            text = if (bpm in 60..100) strings.normal else if (bpm < 60) strings.low else strings.high,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  3. Blood Pressure Gauge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BloodPressureGauge(
    systolic: Int,
    diastolic: Int,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    data class BPZone(val label: String, val color: Color, val fraction: Float)

    val zone = when {
        systolic < 90 -> BPZone(strings.low, Color(0xFF3B82F6), 0.10f)
        systolic in 90..119 -> BPZone(strings.normal, Color(0xFF4ADE80), 0.35f)
        systolic in 120..139 -> BPZone(strings.elevated, Color(0xFFFFB300), 0.60f)
        systolic in 140..179 -> BPZone(strings.high, Color(0xFFFF5252), 0.80f)
        else -> BPZone(strings.critical, Color(0xFFDC2626), 0.97f)
    }

    val animNeedle by animateFloatAsState(
        targetValue = zone.fraction,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "bp_gauge"
    )
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height * 0.9f
            val radius = minOf(size.width * 0.4f, size.height * 0.9f)
            val startAngle = 180f
            val sweepTotal = 180f

            val zoneColors = listOf(
                Color(0xFF3B82F6) to (0f..36f),
                Color(0xFF4ADE80) to (36f..72f),
                Color(0xFFFFB300) to (72f..108f),
                Color(0xFFFF5252) to (108f..144f),
                Color(0xFFDC2626) to (144f..180f)
            )

            zoneColors.forEach { (clr, range) ->
                drawArc(
                    color = clr.copy(alpha = 0.35f),
                    startAngle = startAngle + range.start,
                    sweepAngle = range.endInclusive - range.start,
                    useCenter = false,
                    style = Stroke(18.dp.toPx(), cap = StrokeCap.Butt),
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2f, radius * 2f)
                )
            }

            drawArc(
                color = zone.color,
                startAngle = startAngle,
                sweepAngle = animNeedle * sweepTotal,
                useCenter = false,
                style = Stroke(18.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f)
            )

            val needleAngle = Math.toRadians((startAngle + animNeedle * sweepTotal).toDouble())
            val needleLength = radius - 20.dp.toPx()
            drawLine(
                color = onSurfaceColor,
                start = Offset(cx, cy),
                end = Offset(
                    (cx + needleLength * cos(needleAngle)).toFloat(),
                    (cy + needleLength * sin(needleAngle)).toFloat()
                ),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = onSurfaceColor, radius = 7.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color = surfaceColor, radius = 5.dp.toPx(), center = Offset(cx, cy))
        }

        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$systolic", color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(strings.systolic, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 24.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$diastolic", color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(strings.diastolic, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .background(zone.color.copy(0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(zone.label, color = zone.color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  4. BMI Meter
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BMIMeter(
    bmi: Float,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val clampedBmi = bmi.coerceIn(10f, 40f)
    val fraction = (clampedBmi - 10f) / 30f

    val category = when {
        bmi < 18.5f -> Pair(strings.underweight, Color(0xFF3B82F6))
        bmi < 25f -> Pair(strings.normal, Color(0xFF4ADE80))
        bmi < 30f -> Pair(strings.overweight, Color(0xFFFFB300))
        else -> Pair(strings.obese, Color(0xFFFF5252))
    }

    val animFrac by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "bmi_anim"
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF3B82F6),
                            Color(0xFF4ADE80),
                            Color(0xFFFFB300),
                            Color(0xFFFF5252)
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val markerX = (animFrac * size.width).coerceIn(4.dp.toPx(), size.width - 4.dp.toPx())
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(markerX - 2.dp.toPx(), 3.dp.toPx()),
                    size = Size(4.dp.toPx(), size.height - 6.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("10", "18.5", "25", "30", "40").forEach { label ->
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BMI: ${"%.1f".format(bmi)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .background(category.second.copy(0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(category.first, color = category.second, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  5. Line Area Chart
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LineAreaChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "line_chart_anim"
    )
    val surfaceColor = MaterialTheme.colorScheme.surface

    if (data.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(strings.notEnoughData, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        return
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maxVal = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val n = data.size
        val xStep = w / (n - 1).toFloat()

        val points = data.mapIndexed { i, v ->
            Offset(i * xStep, h - (v / maxVal) * h * 0.85f)
        }

        val drawCount = (points.size * animProgress).toInt().coerceIn(2, points.size)

        // Bezier path
        val linePath = Path()
        linePath.moveTo(points[0].x, points[0].y)
        for (i in 1 until drawCount) {
            val prev = points[i - 1]
            val curr = points[i]
            val cpx = (prev.x + curr.x) / 2f
            linePath.cubicTo(cpx, prev.y, cpx, curr.y, curr.x, curr.y)
        }

        // Area fill
        val areaPath = Path()
        areaPath.addPath(linePath)
        areaPath.lineTo(points[drawCount - 1].x, h)
        areaPath.lineTo(points[0].x, h)
        areaPath.close()

        drawPath(
            areaPath,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
                startY = 0f,
                endY = h
            )
        )
        drawPath(linePath, color = color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Dots
        for (i in 0 until drawCount) {
            drawCircle(color = color, radius = 4.dp.toPx(), center = points[i])
            drawCircle(color = surfaceColor, radius = 2.5.dp.toPx(), center = points[i])
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  6. Weekly Heatmap Grid
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WeeklyHeatmapGrid(
    stepsHistory: List<Int>,
    stepGoal: Int,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val goal = stepGoal.coerceAtLeast(1)
    val padded = List(28) { idx -> stepsHistory.getOrElse(idx) { 0 } }
    val primaryColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day ->
                Text(
                    text = day,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        for (week in 0..3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (day in 0..6) {
                    val idx = week * 7 + day
                    val steps = padded.getOrElse(idx) { 0 }
                    val ratio = (steps.toFloat() / goal).coerceIn(0f, 1f)
                    val cellColor = when {
                        ratio == 0f -> emptyColor
                        ratio < 0.25f -> primaryColor.copy(alpha = 0.25f)
                        ratio < 0.50f -> primaryColor.copy(alpha = 0.5f)
                        ratio < 0.75f -> primaryColor.copy(alpha = 0.75f)
                        else -> primaryColor
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(5.dp))
                            .background(cellColor)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.less, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Spacer(Modifier.width(4.dp))
            listOf(
                emptyColor,
                primaryColor.copy(alpha = 0.25f),
                primaryColor.copy(alpha = 0.5f),
                primaryColor.copy(alpha = 0.75f),
                primaryColor
            ).forEach { clr ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(clr)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(strings.moreText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}
