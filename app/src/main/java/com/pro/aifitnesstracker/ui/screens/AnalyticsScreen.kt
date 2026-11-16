package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath

private val BG @Composable get() = MaterialTheme.colorScheme.background
private val CARD @Composable get() = MaterialTheme.colorScheme.surface
private val ACCENT @Composable get() = MaterialTheme.colorScheme.primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    prefsManager: SharedPrefsManager,
    stepsToday: Int,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    repository: HealthRepository
) {
    val strings = LocalAppStrings.current

    Scaffold(
        containerColor = BG,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        strings.performance,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = ACCENT)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BG)
            )
        },
        bottomBar = {
            AdBanner(adKey = "ad_analytics_banner")
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BG)
        ) {
            item {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                var isGenerating by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CARD),
                    onClick = {
                        isGenerating = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val uri = com.pro.aifitnesstracker.utils.PdfReportGenerator.generateWeeklyReport(context, prefsManager)
                            withContext(Dispatchers.Main) {
                                isGenerating = false
                                uri?.let {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(android.content.Intent.EXTRA_STREAM, it)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Health Report"))
                                } ?: run {
                                    android.widget.Toast.makeText(context, "Error generating report", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ACCENT.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = ACCENT)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export PDF Report", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Premium Health Analysis", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ACCENT, strokeWidth = 2.dp)
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.week,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(24.dp)
                )
            }

            item {
                val stepHistory = prefsManager.getWeeklyStepsHistory()
                ChartCard(
                    title = strings.totalSteps,
                    data = stepHistory,
                    color = ACCENT,
                    unit = strings.steps
                )
            }

            item {
                val waterHistory = prefsManager.getWeeklyWaterHistory()
                ChartCard(
                    title = strings.hydration,
                    data = waterHistory,
                    color = ACCENT,
                    unit = "ml"
                )
            }

            item {
                Text(
                    text = strings.history,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(24.dp)
                )
            }

            item {
                HistoryItem(strings.heartRate, "${prefsManager.getLastBpm()} BPM", ACCENT) { onNavigate("HEART_RATE") }
                HistoryItem(strings.weight, "${prefsManager.getWeight()} kg", ACCENT) { onNavigate("WEIGHT_DETAIL") }
                HistoryItem(strings.bloodPressure, "120/80", ACCENT) { onNavigate("BP_DETAIL") }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(24.dp)
    )
}

@Composable
fun ChartCard(title: String, data: List<Number>, color: Color, unit: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CARD)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                CustomAreaChart(data = data, color = color)
            }
            Spacer(modifier = Modifier.height(12.dp))
            val lastValue = data.lastOrNull()?.toString() ?: "0"
            Text(
                text = "Today: $lastValue $unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CustomAreaChart(data: List<Number>, color: Color) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (data.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        val maxVal = (data.maxOfOrNull { it.toFloat() } ?: 1f).coerceAtLeast(1f)
        
        val points = mutableListOf<Offset>()
        if (data.size == 1) {
            val y = height - (data[0].toFloat() / maxVal) * (height * 0.8f) - (height * 0.1f)
            points.add(Offset(0f, y))
            points.add(Offset(width, y))
        } else {
            val xSpacing = width / (data.size - 1)
            data.forEachIndexed { index, value ->
                val x = index * xSpacing
                val y = height - (value.toFloat() / maxVal) * (height * 0.75f) - (height * 0.15f)
                points.add(Offset(x, y))
            }
        }
        
        val strokePath = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                    val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                    cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        p2.x, p2.y
                    )
                }
            }
        }
        
        val fillPath = android.graphics.Path(strokePath.asAndroidPath()).asComposePath().apply {
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        // Draw grid lines
        val gridCount = 4
        for (i in 1..gridCount) {
            val yGrid = (height / (gridCount + 1)) * i
            drawLine(
                color = gridColor,
                start = Offset(0f, yGrid),
                end = Offset(width, yGrid),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw vertical gradient fill under line
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.3f), Color.Transparent),
                startY = points.minOf { it.y },
                endY = height
            )
        )
        
        // Draw the smooth bezier line
        drawPath(
            path = strokePath,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Draw highlighted anchors
        points.forEach { pt ->
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = 8.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = color,
                radius = 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = surfaceColor,
                radius = 1.5.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
fun HistoryItem(title: String, value: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = CARD
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}


