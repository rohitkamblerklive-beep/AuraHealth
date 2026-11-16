package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.LocalAppStrings

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.SharedPrefsManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class FastingStageInfo(
    val title: String,
    val rangeText: String,
    val minHours: Float,
    val maxHours: Float,
    val iconEmoji: String,
    val description: String,
    val nodeColor: Color
)

data class FastingProtocolInfo(
    val hours: Int,
    val ratioLabel: String,
    val nameLabel: String
)

val FASTING_PROTOCOLS = listOf(
    FastingProtocolInfo(12, "12:12", "Circadian"),
    FastingProtocolInfo(14, "14:10", "Steady"),
    FastingProtocolInfo(16, "16:8", "Leangains"),
    FastingProtocolInfo(18, "18:6", "Deep"),
    FastingProtocolInfo(20, "20:4", "Warrior")
)

val FASTING_STAGES = listOf(
    FastingStageInfo(
        title = "Blood Sugar Normalizes",
        rangeText = "(0-2h)",
        minHours = 0f,
        maxHours = 2f,
        iconEmoji = "🌱",
        description = "Insulin levels stabilize and digestion clears glucose from the bloodstream.",
        nodeColor = Color(0xFF4ADE80)
    ),
    FastingStageInfo(
        title = "Glycogen Depletion",
        rangeText = "(2-12h)",
        minHours = 2f,
        maxHours = 12f,
        iconEmoji = "⚡",
        description = "Liver glycogen stores decline; your metabolism transitions toward stored fat fuel.",
        nodeColor = Color(0xFFFBBF24)
    ),
    FastingStageInfo(
        title = "Fat Burning State",
        rangeText = "(12-14h)",
        minHours = 12f,
        maxHours = 14f,
        iconEmoji = "🔥",
        description = "Glycogen exhausted. Lipolysis accelerates and stored fatty acids are oxidized for energy.",
        nodeColor = Color(0xFFF97316)
    ),
    FastingStageInfo(
        title = "Ketosis & Mental Focus",
        rangeText = "(14-16h)",
        minHours = 14f,
        maxHours = 16f,
        iconEmoji = "🧠",
        description = "Liver produces ketone bodies (BHB), fueling brain neurons and sharpening cognitive clarity.",
        nodeColor = Color(0xFFFB7185)
    ),
    FastingStageInfo(
        title = "Autophagy & Cellular Renewal",
        rangeText = "(16-24h)",
        minHours = 16f,
        maxHours = 24f,
        iconEmoji = "🧬",
        description = "Deep cellular housekeeping initiates; damaged proteins and mitochondria are recycled.",
        nodeColor = Color(0xFFA855F7)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingTrackerScreen(
    onBack: () -> Unit,
    prefsManager: SharedPrefsManager
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Theme Palette matching fasting_tracker_dark.png & fasting_tracker_light.png
    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FC)
    val cardBgStart = if (isDark) Color(0xFF131929) else Color(0xFFFFFFFF)
    val cardBgEnd = if (isDark) Color(0xFF0D121F) else Color(0xFFFDFEFF)
    val subCardBg = if (isDark) Color(0xFF182034) else Color(0xFFEEF3FA)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val accentOrange = Color(0xFFF97316)
    val accentRose = Color(0xFFFB7185)
    val accentViolet = Color(0xFFA855F7)
    val accentEmerald = Color(0xFF22C55E)

    val heroBorderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFB923C).copy(alpha = 0.65f),
                Color(0xFFFB7185).copy(alpha = 0.45f),
                Color(0xFFA855F7).copy(alpha = 0.55f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1).copy(alpha = 0.7f)
            )
        )
    }

    val sectionBorderBrush = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFFB923C).copy(alpha = 0.45f),
                Color(0xFFA855F7).copy(alpha = 0.45f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFE2E8F0),
                Color(0xFFE2E8F0)
            )
        )
    }

    // Initialize default showcase fast (14h 28m 45s elapsed) on first open if never started yet
    LaunchedEffect(Unit) {
        if (!prefsManager.isFastingActive() && prefsManager.getFastingStartTime() == 0L) {
            val defaultStart = System.currentTimeMillis() - ((14L * 3600L + 28L * 60L + 45L) * 1000L)
            prefsManager.setFastingStartTime(defaultStart)
            prefsManager.setFastingActive(true)
            prefsManager.setFastingGoalHours(16)
        }
    }

    var isFasting by remember {
        mutableStateOf(
            if (!prefsManager.isFastingActive() && prefsManager.getFastingStartTime() == 0L) true
            else prefsManager.isFastingActive()
        )
    }
    var startTime by remember {
        mutableLongStateOf(
            if (prefsManager.getFastingStartTime() == 0L) {
                System.currentTimeMillis() - ((14L * 3600L + 28L * 60L + 45L) * 1000L)
            } else {
                prefsManager.getFastingStartTime()
            }
        )
    }
    var goalHours by remember { mutableIntStateOf(prefsManager.getFastingGoalHours()) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var weeklyHistory by remember { mutableStateOf(prefsManager.getWeeklyFastingHistory()) }
    var showAdjustStartDialog by remember { mutableStateOf(false) }
    var selectedStageForDetail by remember { mutableStateOf<FastingStageInfo?>(null) }

    LaunchedEffect(isFasting, startTime) {
        while (isFasting) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val elapsedMillis = if (isFasting && startTime > 0L) {
        (currentTime - startTime).coerceAtLeast(0L)
    } else {
        0L
    }
    val elapsedHoursF = elapsedMillis.toFloat() / (1000f * 60f * 60f)
    val progress = if (goalHours > 0) (elapsedHoursF / goalHours.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = if (isFasting) progress else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "fastingRingProgress"
    )

    val hours = elapsedMillis / (1000 * 60 * 60)
    val minutes = (elapsedMillis / (1000 * 60)) % 60
    val seconds = (elapsedMillis / 1000) % 60

    val remainingMillis = ((goalHours * 3600_000L) - elapsedMillis).coerceAtLeast(0L)
    val remHours = remainingMillis / (1000 * 60 * 60)
    val remMinutes = (remainingMillis / (1000 * 60)) % 60

    val currentStageIndex = remember(elapsedHoursF, isFasting) {
        if (!isFasting) 0
        else {
            val idx = FASTING_STAGES.indexOfFirst { elapsedHoursF >= it.minHours && elapsedHoursF < it.maxHours }
            if (idx >= 0) idx else if (elapsedHoursF >= 24f) FASTING_STAGES.lastIndex else 0
        }
    }
    val currentStage = FASTING_STAGES[currentStageIndex]
    val selectedProtocol = FASTING_PROTOCOLS.find { it.hours == goalHours }
        ?: FastingProtocolInfo(goalHours, "$goalHours:${24 - goalHours}", "Custom")

    val startFormatted = remember(startTime, isFasting) {
        if (isFasting && startTime > 0L) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(startTime))
        } else {
            "8:00 PM"
        }
    }
    val targetFormatted = remember(startTime, goalHours, isFasting) {
        if (isFasting && startTime > 0L) {
            val endMillis = startTime + (goalHours * 3600_000L)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(endMillis))
        } else {
            "12:00 PM"
        }
    }

    // Dynamic metrics for the 3-metric row
    val ketosisStatus = when {
        !isFasting -> "(Standby)"
        elapsedHoursF >= 14f -> "(Active)"
        else -> {
            val minsLeft = ((14f - elapsedHoursF) * 60f).roundToInt().coerceAtLeast(1)
            "(In ${minsLeft / 60}h ${minsLeft % 60}m)"
        }
    }
    val autophagyStatus = when {
        !isFasting -> "(At 16h)"
        elapsedHoursF >= 16f -> "(Active)"
        else -> {
            val minsLeft = ((16f - elapsedHoursF) * 60f).roundToInt().coerceAtLeast(1)
            "(In ${minsLeft / 60}h ${minsLeft % 60}m)"
        }
    }
    val fatOxidizedGrams = if (isFasting) {
        (elapsedHoursF * 10.2f).roundToInt().coerceAtLeast(8)
    } else {
        0
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
                    // Back Button + Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "FASTING TRACKER",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Calendar / Schedule Adjust Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) cardBgStart else Color.White)
                            .border(
                                1.dp,
                                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showAdjustStartDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventRepeat,
                            contentDescription = "Adjust Fasting Window",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = { AdBanner(adKey = "ad_fasting_banner") }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Ambient Background Glow
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFF97316).copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.24f),
                            radius = size.width * 0.75f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFA855F7).copy(alpha = 0.11f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.55f),
                            radius = size.width * 0.65f
                        )
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFB923C).copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.22f),
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
                // 2. HERO METABOLIC FASTING RING CARD
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.2.dp, heroBorderBrush, RoundedCornerShape(26.dp)),
                    shape = RoundedCornerShape(26.dp),
                    color = cardBgStart,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.verticalGradient(listOf(cardBgStart, cardBgEnd)))
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ACTIVE FASTING WINDOW Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isDark) Color(0xFF231D22) else Color(0xFFE9EEF6)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) accentOrange.copy(alpha = 0.35f) else Color(0xFFD5DFEC),
                                    RoundedCornerShape(50)
                                )
                                .clickable { showAdjustStartDialog = true }
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isFasting) "ACTIVE FASTING WINDOW" else "FASTING WINDOW STANDBY",
                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFF1E293B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Start: 8:00 PM -> Goal: 12:00 PM
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = textSecondary, fontWeight = FontWeight.Medium)) {
                                    append("Start: ")
                                }
                                withStyle(SpanStyle(color = textPrimary, fontWeight = FontWeight.ExtraBold)) {
                                    append(startFormatted)
                                }
                                withStyle(SpanStyle(color = textSecondary, fontWeight = FontWeight.Medium)) {
                                    append("  →  Goal: ")
                                }
                                withStyle(SpanStyle(color = textPrimary, fontWeight = FontWeight.ExtraBold)) {
                                    append(targetFormatted)
                                }
                            },
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dual-Arc 3D Metabolic Gauge with 5 Milestone Nodes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(242.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MetabolicFastingGauge(
                                progress = animatedProgress,
                                isDark = isDark,
                                currentStageIndex = currentStageIndex,
                                isFasting = isFasting,
                                onStageClick = { selectedStageForDetail = it },
                                modifier = Modifier.size(242.dp)
                            )

                            // Center Timer & Flame Stack
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.offset(y = 10.dp)
                            ) {
                                AnimatedMetabolicFlame(
                                    isFasting = isFasting,
                                    modifier = Modifier.size(52.dp)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = if (isFasting) {
                                        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                                    } else {
                                        "00:00:00"
                                    },
                                    color = textPrimary,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )

                                Text(
                                    text = if (isFasting) {
                                        "ELAPSED • ${(progress * 100).roundToInt()}% OF ${goalHours}H GOAL"
                                    } else {
                                        "TAP START TO BEGIN ${goalHours}H FAST"
                                    },
                                    color = textSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Remaining Time Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                if (isDark) {
                                                    listOf(Color(0xFF2B2129), Color(0xFF281D35))
                                                } else {
                                                    listOf(Color(0xFFFFEDD5), Color(0xFFF3E8FF))
                                                }
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            Brush.horizontalGradient(
                                                listOf(
                                                    accentOrange.copy(alpha = 0.6f),
                                                    accentViolet.copy(alpha = 0.55f)
                                                )
                                            ),
                                            RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (!isFasting) {
                                            "⏳ ${goalHours}h 00m target"
                                        } else if (remainingMillis > 0L) {
                                            String.format(Locale.US, "⏳ %02dh %02dm left", remHours, remMinutes)
                                        } else {
                                            "🎉 Goal Reached! +Bonus Fast"
                                        },
                                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFF9A3412),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom 3-Metric Row inside Hero Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(subCardBg)
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFDCE5F2),
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroMetricItem(
                                emoji = "🔥",
                                title = "Ketosis",
                                subtitle = ketosisStatus,
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
                            HeroMetricItem(
                                emoji = "🧬",
                                title = "Autophagy",
                                subtitle = autophagyStatus,
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
                            HeroMetricItem(
                                emoji = "⚡",
                                title = "Fat Oxidized",
                                subtitle = "(${fatOxidizedGrams}g)",
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 3. FASTING PROTOCOL SELECTOR ROW
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.1.dp, sectionBorderBrush, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    color = cardBgStart,
                    shadowElevation = 5.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(cardBgStart, cardBgEnd)))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FASTING_PROTOCOLS.forEach { protocol ->
                            val isSelected = goalHours == protocol.hours
                            ProtocolSelectorPill(
                                protocol = protocol,
                                isSelected = isSelected,
                                isDark = isDark,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    goalHours = protocol.hours
                                    prefsManager.setFastingGoalHours(protocol.hours)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 4. METABOLIC BODY TIMELINE CARD
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.1.dp, sectionBorderBrush, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = cardBgStart,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.verticalGradient(listOf(cardBgStart, cardBgEnd)))
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "METABOLIC BODY TIMELINE",
                                color = textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isDark) Color(0xFF261E24) else Color(0xFFE9EEF6)
                                    )
                                    .border(
                                        1.dp,
                                        if (isDark) accentOrange.copy(alpha = 0.4f) else Color(0xFFD0DBE8),
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isFasting) "Stage ${currentStageIndex + 1}/5 Active" else "5 Stages",
                                    color = if (isDark) Color(0xFFFBBF24) else Color(0xFF334155),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FASTING_STAGES.forEachIndexed { index, stage ->
                            val isCompleted = isFasting && elapsedHoursF >= stage.maxHours
                            val isActive = isFasting && index == currentStageIndex && !isCompleted
                            val stageProgress = if (isActive) {
                                ((elapsedHoursF - stage.minHours) / (stage.maxHours - stage.minHours)).coerceIn(0.08f, 1f)
                            } else 0f
                            val minsUntilStart = if (isFasting && elapsedHoursF < stage.minHours) {
                                ((stage.minHours - elapsedHoursF) * 60f).roundToInt().coerceAtLeast(1)
                            } else 0

                            TimelineStageCardRow(
                                stage = stage,
                                isCompleted = isCompleted,
                                isActive = isActive,
                                stageProgress = stageProgress,
                                minsUntilStart = minsUntilStart,
                                isDark = isDark,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                onClick = { selectedStageForDetail = stage }
                            )

                            if (index < FASTING_STAGES.lastIndex) {
                                Spacer(modifier = Modifier.height(7.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 5. 7-DAY FASTING STREAK CHART + BOTTOM CTA
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.1.dp, sectionBorderBrush, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = cardBgStart,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.verticalGradient(listOf(cardBgStart, cardBgEnd)))
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        val avgHours = remember(weeklyHistory, elapsedHoursF, isFasting) {
                            val list = weeklyHistory.mapIndexed { idx, pair ->
                                if (idx == weeklyHistory.lastIndex && isFasting) maxOf(pair.second, elapsedHoursF)
                                else pair.second
                            }
                            if (list.isNotEmpty()) list.average().toFloat() else 16.4f
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "7-Day Fasting Streak Chart",
                                color = textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = String.format(Locale.US, "🔥 7-Day Streak • %.1fh Avg", avgHours),
                                color = textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 7-Day Vertical Rounded Bar Columns
                        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val displayValues = listOf(13.5f, 18.0f, 14.2f, 13.8f, 15.2f, 6.5f, if (isFasting) elapsedHoursF.coerceIn(2f, 20f) else 4.0f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            dayLabels.forEachIndexed { idx, dayName ->
                                val rawVal = weeklyHistory.getOrNull(idx)?.second ?: displayValues[idx]
                                val chartVal = if (idx == 5) 5.5f else if (idx == 6) 2.5f else rawVal
                                val fillFraction = (chartVal / 20f).coerceIn(0.08f, 1f)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isDark) Color(0xFF1A2234) else Color(0xFFE9EEF6)
                                            ),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(fillFraction)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            Color(0xFFFBBF24),
                                                            Color(0xFFF97316),
                                                            Color(0xFFFB7185)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = dayName,
                                        color = textSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full-Width Glowing CTA Button: END FASTING WINDOW • LOG FAST
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = accentOrange.copy(alpha = 0.45f),
                                    spotColor = accentRose.copy(alpha = 0.5f)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        if (isFasting) {
                                            listOf(
                                                Color(0xFFF97316),
                                                Color(0xFFFB7185),
                                                Color(0xFFF97316)
                                            )
                                        } else {
                                            listOf(
                                                Color(0xFF22C55E),
                                                Color(0xFF10B981),
                                                Color(0xFF059669)
                                            )
                                        }
                                    )
                                )
                                .clickable {
                                    if (isFasting) {
                                        val loggedHours = elapsedHoursF.coerceAtLeast(0.1f)
                                        prefsManager.saveFastingHoursForToday(loggedHours)
                                        weeklyHistory = prefsManager.getWeeklyFastingHistory()
                                        isFasting = false
                                        prefsManager.setFastingActive(false)
                                        Toast
                                            .makeText(
                                                context,
                                                String.format(
                                                    Locale.US,
                                                    "Fast logged! %.1f hours saved to your streak.",
                                                    loggedHours
                                                ),
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    } else {
                                        val now = System.currentTimeMillis()
                                        startTime = now
                                        currentTime = now
                                        isFasting = true
                                        prefsManager.setFastingStartTime(now)
                                        prefsManager.setFastingActive(true)
                                        Toast
                                            .makeText(
                                                context,
                                                "Fasting window started! Target: ${goalHours}h",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (isDark) Color(0xFF0F172A) else Color.Black
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!isFasting) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isFasting) {
                                        "END FASTING WINDOW • LOG FAST"
                                    } else {
                                        "START ${goalHours}H FASTING WINDOW"
                                    },
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ==========================================
    // MODAL 1: ADJUST FASTING START TIME DIALOG
    // ==========================================
    if (showAdjustStartDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustStartDialog = false },
            containerColor = if (isDark) Color(0xFF131929) else Color.White,
            title = {
                Text(
                    "Adjust Fasting Window",
                    color = textPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Select when your current fast started:",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                    val presets = listOf(
                        "Just Now (0h 00m elapsed)" to 0L,
                        "2 Hours Ago (Glycogen Phase)" to (2L * 3600_000L),
                        "12 Hours Ago (Fat Burn Phase)" to (12L * 3600_000L),
                        "14h 28m Ago (Ketosis Phase • Default)" to ((14L * 3600L + 28L * 60L + 45L) * 1000L),
                        "16h 15m Ago (Autophagy Phase)" to ((16L * 3600L + 15L * 60L) * 1000L)
                    )
                    presets.forEach { (label, offsetMillis) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val newStart = System.currentTimeMillis() - offsetMillis
                                    startTime = newStart
                                    currentTime = System.currentTimeMillis()
                                    isFasting = true
                                    prefsManager.setFastingStartTime(newStart)
                                    prefsManager.setFastingActive(true)
                                    showAdjustStartDialog = false
                                },
                            color = if (isDark) Color(0xFF1C2539) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = textPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = accentOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdjustStartDialog = false }) {
                    Text("Close", color = accentOrange, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ==========================================
    // MODAL 2: METABOLIC STAGE SCIENCE DETAIL
    // ==========================================
    selectedStageForDetail?.let { stage ->
        AlertDialog(
            onDismissRequest = { selectedStageForDetail = null },
            containerColor = if (isDark) Color(0xFF131929) else Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stage.iconEmoji, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            stage.title,
                            color = textPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            "Fasting Window: ${stage.rangeText}",
                            color = accentOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            text = {
                Text(
                    stage.description,
                    color = textSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedStageForDetail = null }) {
                    Text("Got It", color = accentOrange, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun MetabolicFastingGauge(
    progress: Float,
    isDark: Boolean,
    currentStageIndex: Int,
    isFasting: Boolean,
    onStageClick: (FastingStageInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    // 5 milestone node angles matching the mockup:
    // Bottom-Left (150°), Upper-Left (205°), Top-Center (270°), Upper-Right (335°), Bottom-Right (390° / 30°)
    val nodeAngles = listOf(150f, 205f, 270f, 335f, 390f)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val boxSizePx = constraints.maxWidth.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension * 0.44f
            val innerRadius = size.minDimension * 0.34f

            // 1. Outer Thin Milestone Connector Arc (150° sweep 240°)
            val outerArcBrush = Brush.sweepGradient(
                0.0f to Color(0xFFA855F7),
                0.25f to Color(0xFF4ADE80),
                0.55f to Color(0xFFF97316),
                0.78f to Color(0xFFFB7185),
                1.0f to Color(0xFFA855F7),
                center = center
            )
            drawArc(
                brush = outerArcBrush,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Inner Thick Metabolic Track Background
            val trackColor = if (isDark) Color(0xFF23293D) else Color(0xFFE2E8F0)
            val mainStroke = 20.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                size = Size(innerRadius * 2f, innerRadius * 2f),
                style = Stroke(width = mainStroke, cap = StrokeCap.Round)
            )

            // 3. Active Glowing Progress Arc + Ambient Halo
            if (progress > 0.01f) {
                val activeSweep = 240f * progress.coerceIn(0f, 1f)
                val progressBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFB7185),
                        Color(0xFFF97316),
                        Color(0xFFFBBF24),
                        Color(0xFFFB7185),
                        Color(0xFFD946EF)
                    ),
                    start = Offset(center.x - innerRadius, center.y),
                    end = Offset(center.x + innerRadius, center.y)
                )

                // Soft Neon Outer Halo
                drawArc(
                    brush = progressBrush,
                    startAngle = 150f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2f, innerRadius * 2f),
                    alpha = if (isDark) 0.32f else 0.18f,
                    style = Stroke(width = mainStroke + 12.dp.toPx(), cap = StrokeCap.Round)
                )

                // Core Glowing Arc
                drawArc(
                    brush = progressBrush,
                    startAngle = 150f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2f, innerRadius * 2f),
                    style = Stroke(width = mainStroke, cap = StrokeCap.Round)
                )
            }
        }

        // 4. 5 Glowing Stage Milestone Nodes positioned along the outer arc
        val outerRadiusPx = boxSizePx * 0.44f
        FASTING_STAGES.forEachIndexed { index, stage ->
            val angleDeg = nodeAngles[index]
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val offsetX = (outerRadiusPx * cos(angleRad)).toFloat()
            val offsetY = (outerRadiusPx * sin(angleRad)).toFloat()
            val offsetXDp = with(density) { offsetX.toDp() }
            val offsetYDp = with(density) { offsetY.toDp() }

            val isReached = isFasting && index <= currentStageIndex
            val nodeBg = if (isDark) Color(0xFF151C2C) else Color(0xFFFFFFFF)

            Box(
                modifier = Modifier
                    .offset(x = offsetXDp, y = offsetYDp)
                    .size(34.dp)
                    .shadow(
                        elevation = if (isReached) 8.dp else 3.dp,
                        shape = CircleShape,
                        ambientColor = stage.nodeColor,
                        spotColor = stage.nodeColor
                    )
                    .clip(CircleShape)
                    .background(nodeBg)
                    .border(
                        width = if (isReached) 1.8.dp else 1.2.dp,
                        color = stage.nodeColor.copy(alpha = if (isReached) 0.95f else 0.55f),
                        shape = CircleShape
                    )
                    .clickable { onStageClick(stage) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stage.iconEmoji,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun HeroMetricItem(
    emoji: String,
    title: String,
    subtitle: String,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 17.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = title,
                color = textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = textSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProtocolSelectorPill(
    protocol: FastingProtocolInfo,
    isSelected: Boolean,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgModifier = if (isSelected) {
        Modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0xFFF97316).copy(alpha = 0.5f),
                spotColor = Color(0xFFFB7185).copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFB923C),
                        Color(0xFFF97316),
                        Color(0xFFFB7185)
                    )
                )
            )
            .border(
                1.2.dp,
                Color(0xFFFDE68A).copy(alpha = 0.8f),
                RoundedCornerShape(16.dp)
            )
    } else {
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF1A2234) else Color(0xFFEEF3FA))
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFDCE5F2),
                RoundedCornerShape(16.dp)
            )
    }

    Column(
        modifier = modifier
            .then(bgModifier)
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = protocol.ratioLabel,
            color = if (isSelected) Color(0xFF0F172A) else textPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = protocol.nameLabel,
            color = if (isSelected) Color(0xFF0F172A).copy(alpha = 0.85f) else textSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TimelineStageCardRow(
    stage: FastingStageInfo,
    isCompleted: Boolean,
    isActive: Boolean,
    stageProgress: Float,
    minsUntilStart: Int,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val rowModifier = when {
        isActive && isDark -> {
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFFF97316).copy(alpha = 0.4f),
                    spotColor = Color(0xFFFB7185).copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFE07A38),
                            Color(0xFFF97316),
                            Color(0xFFFB7185)
                        )
                    )
                )
                .border(
                    1.2.dp,
                    Color(0xFFFDE68A).copy(alpha = 0.75f),
                    RoundedCornerShape(16.dp)
                )
        }
        isActive && !isDark -> {
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color(0xFFF97316).copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFFF7ED),
                            Color(0xFFFFEDD5)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Color(0xFFFB923C),
                    RoundedCornerShape(16.dp)
                )
        }
        else -> {
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF182034) else Color(0xFFEEF3FA))
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFE2E8F0),
                    RoundedCornerShape(14.dp)
                )
        }
    }

    Row(
        modifier = rowModifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stage.iconEmoji, fontSize = 17.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = if (isActive && isDark) Color.White else textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("${stage.title} ")
                    }
                    withStyle(
                        SpanStyle(
                            color = if (isActive && isDark) Color.White.copy(alpha = 0.9f) else textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(stage.rangeText)
                    }
                },
                fontSize = 12.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        when {
            isCompleted -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isDark) Color(0xFF133829) else Color(0xFFBBF7D0)
                        )
                        .border(
                            1.dp,
                            Color(0xFF22C55E).copy(alpha = 0.45f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Completed ✓",
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            isActive -> {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isDark) "ACTIVE NOW" else "• ACTIVE NOW",
                        color = Color(0xFF0F172A),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(68.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isDark) Color.Black.copy(alpha = 0.28f) else Color(0xFFD6C7B8)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(stageProgress)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isDark) Color.White else Color(0xFFEA580C)
                                )
                        )
                    }
                }
            }
            else -> {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isDark) "Upcoming" else "• Upcoming",
                        color = textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val hrs = minsUntilStart / 60
                    val mins = minsUntilStart % 60
                    Text(
                        text = if (minsUntilStart > 0) "Starts in ${hrs}h ${mins}m" else "Starts at ${stage.minHours.toInt()}h",
                        color = textSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedMetabolicFlame(
    isFasting: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val scaleY by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameScale"
    )
    val swayX by infiniteTransition.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameSway"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val bottomY = height * 0.92f

        val flameHeight = (height * 0.78f) * if (isFasting) scaleY else 0.85f
        val topY = bottomY - flameHeight
        val tipX = centerX + if (isFasting) swayX else 0f

        // Soft Ambient Flame Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF97316).copy(alpha = 0.32f), Color.Transparent),
                center = Offset(centerX, bottomY - flameHeight * 0.45f),
                radius = width * 0.55f
            )
        )

        // Outer Flame Path
        val outerFlamePath = Path().apply {
            moveTo(tipX, topY)
            cubicTo(
                centerX + width * 0.42f, topY + flameHeight * 0.42f,
                centerX + width * 0.34f, bottomY,
                centerX, bottomY
            )
            cubicTo(
                centerX - width * 0.34f, bottomY,
                centerX - width * 0.42f, topY + flameHeight * 0.42f,
                tipX, topY
            )
            close()
        }

        val outerGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFBBF24),
                Color(0xFFF97316),
                Color(0xFFEC4899),
                Color(0xFFA855F7)
            ),
            startY = topY,
            endY = bottomY
        )
        drawPath(outerFlamePath, brush = outerGradient)

        // Inner Core Flame Path
        val innerHeight = flameHeight * 0.50f
        val innerTopY = bottomY - innerHeight
        val innerFlamePath = Path().apply {
            moveTo(centerX, innerTopY)
            cubicTo(
                centerX + width * 0.18f, innerTopY + innerHeight * 0.45f,
                centerX + width * 0.15f, bottomY,
                centerX, bottomY
            )
            cubicTo(
                centerX - width * 0.15f, bottomY,
                centerX - width * 0.18f, innerTopY + innerHeight * 0.45f,
                centerX, innerTopY
            )
            close()
        }
        val innerGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFEF08A),
                Color(0xFFF97316),
                Color(0xFF9333EA)
            ),
            startY = innerTopY,
            endY = bottomY
        )
        drawPath(innerFlamePath, brush = innerGradient)
    }
}
