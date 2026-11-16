package com.pro.aifitnesstracker.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.HealthRepository
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BloodPressureRecordScreen(
    onBack: () -> Unit,
    prefsManager: SharedPrefsManager,
    repository: HealthRepository
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }
    val scope = rememberCoroutineScope()

    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FC)
    val cardBg = if (isDark) Color(0xFF121828) else Color(0xFFFFFFFF)
    val subCardBg = if (isDark) Color(0xFF192236) else Color(0xFFEEF3FA)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val accentRose = Color(0xFFFB7185)
    val accentCyan = Color(0xFF22D3EE)
    val accentEmerald = Color(0xFF10B981)
    val accentAmber = Color(0xFFF59E0B)
    val accentOrange = Color(0xFFF97316)
    val accentCrimson = Color(0xFFF43F5E)
    val borderColor = if (isDark) accentRose.copy(alpha = 0.48f) else Color(0xFFCBD5E1)

    val latestHistory = remember { prefsManager.getBloodPressureHistory().firstOrNull() }
    var systolic by remember { mutableIntStateOf(latestHistory?.systolic ?: 118) }
    var diastolic by remember { mutableIntStateOf(latestHistory?.diastolic ?: 78) }
    var pulse by remember { mutableIntStateOf(latestHistory?.pulse ?: 68) }

    val category = remember(systolic, diastolic) {
        when {
            systolic < 120 && diastolic < 80 -> "Normal"
            systolic < 130 && diastolic < 80 -> "Elevated"
            systolic < 140 || diastolic < 90 -> "Hypertension Stage 1"
            else -> "Hypertension Stage 2"
        }
    }
    val statusColor = remember(category) {
        when (category) {
            "Normal" -> accentEmerald
            "Elevated" -> accentAmber
            "Hypertension Stage 1" -> accentOrange
            else -> accentCrimson
        }
    }
    val mapPressure = remember(systolic, diastolic) {
        diastolic + ((systolic - diastolic) / 3f).roundToInt()
    }
    val pulsePressure = remember(systolic, diastolic) {
        (systolic - diastolic).coerceAtLeast(10)
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            text = strings.bloodPressure.uppercase(Locale.US),
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "MAP $mapPressure mmHg  •  Pulse Pressure $pulsePressure mmHg",
                            color = accentRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Delete Latest Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF281826) else Color(0xFFFEE2E8))
                            .border(1.dp, accentRose.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable {
                                scope.launch {
                                    val history = prefsManager.getBloodPressureHistory()
                                    if (history.isNotEmpty()) {
                                        val latest = history.first()
                                        repository.deleteBloodPressure(latest.systolic, latest.diastolic, latest.pulse)
                                        prefsManager.deleteBloodPressureEntry(latest)
                                        Toast.makeText(context, "Deleted latest reading", Toast.LENGTH_SHORT).show()
                                    }
                                    onBack()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete Latest",
                            tint = accentRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = { AdBanner(adKey = "ad_blood_pressure_banner") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3-Column Dial Pickers Card (Systolic, Diastolic, Pulse)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, borderColor, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = cardBg
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BPPickerColumn(
                        label = "Systolic",
                        unit = "mmHg",
                        value = systolic,
                        onValueChange = { systolic = it.coerceIn(70, 220) }
                    )
                    BPPickerColumn(
                        label = "Diastolic",
                        unit = "mmHg",
                        value = diastolic,
                        onValueChange = { diastolic = it.coerceIn(40, 140) }
                    )
                    BPPickerColumn(
                        label = "Pulse",
                        unit = "BPM",
                        value = pulse,
                        onValueChange = { pulse = it.coerceIn(40, 190) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live AHA Classification Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, borderColor, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = cardBg
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "AHA CLINICAL CLASSIFICATION",
                                color = textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category,
                                    color = statusColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(subCardBg)
                                .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$systolic / $diastolic mmHg",
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-Segment AHA Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        listOf(
                            accentEmerald to "Normal <120",
                            accentAmber to "Elevated 120-129",
                            accentOrange to "Stage 1 130-139",
                            accentCrimson to "Stage 2 140+"
                        ).forEach { (col, lbl) ->
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lbl,
                                    color = textSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (category == "Normal") {
                            "Optimal range (SYS <120 & DIA <80 mmHg). Maintain regular hydration, low sodium intake, and aerobic conditioning."
                        } else {
                            "Above optimal baseline. Rest for 5 minutes and re-measure, or track daily trends with your physician."
                        },
                        color = textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(10.dp, RoundedCornerShape(50), spotColor = accentRose)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentRose, accentCrimson, accentRose)
                        )
                    )
                    .clickable {
                        scope.launch {
                            repository.saveBloodPressure(systolic, diastolic, pulse)
                            prefsManager.saveBloodPressure(systolic, diastolic, pulse)
                            Toast
                                .makeText(
                                    context,
                                    "Saved Blood Pressure: $systolic/$diastolic mmHg • $pulse BPM",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                            onBack()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE HEMODYNAMIC READING",
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

@Composable
fun BloodPressureDetailScreen(
    prefsManager: SharedPrefsManager,
    repository: HealthRepository,
    onBack: () -> Unit,
    onLogNew: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }
    val scope = rememberCoroutineScope()

    // Palette matching blood_pressure_dark.png & blood_pressure_light.png
    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FC)
    val cardBg = if (isDark) Color(0xFF121828) else Color(0xFFFFFFFF)
    val subCardBg = if (isDark) Color(0xFF192236) else Color(0xFFEEF3FA)
    val rosePillBg = if (isDark) Color(0xFF241626) else Color(0xFFFFE4E6)
    val emeraldPillBg = if (isDark) Color(0xFF10302A) else Color(0xFFD1FAE5)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val accentRose = Color(0xFFFB7185)
    val accentCrimson = Color(0xFFF43F5E)
    val accentCyan = Color(0xFF22D3EE)
    val accentEmerald = Color(0xFF10B981)
    val accentAmber = Color(0xFFF59E0B)
    val accentOrange = Color(0xFFF97316)
    val borderColor = if (isDark) accentRose.copy(alpha = 0.48f) else Color(0xFFCBD5E1)

    var savedHistory by remember { mutableStateOf(prefsManager.getBloodPressureHistory()) }

    // Default showcase telemetry if no history logged yet
    val displayHistory = remember(savedHistory) {
        if (savedHistory.isNotEmpty()) {
            savedHistory
        } else {
            listOf(
                SharedPrefsManager.BloodPressureEntry("Today, 9:30 AM", 118, 78, 68),
                SharedPrefsManager.BloodPressureEntry("Yesterday, 8:15 PM", 121, 79, 71),
                SharedPrefsManager.BloodPressureEntry("Fri, 9:00 AM", 117, 76, 66),
                SharedPrefsManager.BloodPressureEntry("Thu, 8:45 AM", 120, 79, 70),
                SharedPrefsManager.BloodPressureEntry("Wed, 7:30 PM", 124, 82, 73),
                SharedPrefsManager.BloodPressureEntry("Tue, 9:10 AM", 119, 78, 69),
                SharedPrefsManager.BloodPressureEntry("Mon, 8:20 AM", 122, 80, 70)
            )
        }
    }

    val latest = displayHistory.first()
    val category = remember(latest) {
        when {
            latest.systolic < 120 && latest.diastolic < 80 -> "Normal"
            latest.systolic < 130 && latest.diastolic < 80 -> "Elevated"
            latest.systolic < 140 || latest.diastolic < 90 -> "Stage 1"
            else -> "Stage 2"
        }
    }
    val statusColor = remember(category) {
        when (category) {
            "Normal" -> accentEmerald
            "Elevated" -> accentAmber
            "Stage 1" -> accentOrange
            else -> accentCrimson
        }
    }

    val mapPressure = remember(latest) {
        latest.diastolic + ((latest.systolic - latest.diastolic) / 3f).roundToInt()
    }
    val pulsePressure = remember(latest) {
        (latest.systolic - latest.diastolic).coerceAtLeast(10)
    }

    val sysProgress by animateFloatAsState(
        targetValue = ((latest.systolic - 70f) / 100f).coerceIn(0.15f, 0.95f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "sysArc"
    )
    val diaProgress by animateFloatAsState(
        targetValue = ((latest.diastolic - 45f) / 65f).coerceIn(0.15f, 0.92f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "diaArc"
    )

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
                            text = "${strings.bloodPressure.uppercase(Locale.US)} & VITALS",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(rosePillBg)
                                .border(1.dp, accentRose, RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "•  AHA ${category.uppercase()} ZONE  •  MAP $mapPressure mmHg",
                                color = if (isDark) accentRose else Color(0xFFBE123C),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    // Right + Log Reading Icon Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF182134) else Color(0xFFE0E8F4))
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable { onLogNew() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Log New Reading",
                            tint = accentRose,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        bottomBar = { AdBanner(adKey = "ad_blood_pressure_banner") }
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
                            colors = listOf(accentCrimson.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.24f),
                            radius = size.width * 0.75f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentCyan.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.62f),
                            radius = size.width * 0.7f
                        )
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentRose.copy(alpha = 0.09f), Color.Transparent),
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
                // 2. HERO DUAL-RING HEMODYNAMIC GAUGE CARD
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isDark) 10.dp else 6.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = if (isDark) accentRose.copy(alpha = 0.22f) else Color(0x14000000),
                            spotColor = if (isDark) accentCyan.copy(alpha = 0.22f) else Color(0x14000000)
                        )
                        .border(1.3.dp, borderColor, RoundedCornerShape(26.dp)),
                    shape = RoundedCornerShape(26.dp),
                    color = cardBg
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Pill: CARDIOVASCULAR TELEMETRY • Latest: Today, 9:30 AM
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(subCardBg)
                                .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "CARDIOVASCULAR TELEMETRY   •   Latest: ${latest.date}",
                                color = if (isDark) accentRose else Color(0xFFBE123C),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dual-Ring Gauge + Center Readout + SYS/DIA Pills
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(225.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HemodynamicDualRingGauge(
                                sysProgress = sysProgress,
                                diaProgress = diaProgress,
                                isDark = isDark,
                                modifier = Modifier.size(218.dp)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.offset(y = 6.dp)
                            ) {
                                Text(
                                    text = "● OPTIMAL AHA RANGE",
                                    color = statusColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${latest.systolic} / ${latest.diastolic}",
                                    color = textPrimary,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )

                                Text(
                                    text = "mmHg   •   Pulse: ${latest.pulse} BPM",
                                    color = textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // SYS 118 mmHg & DIA 78 mmHg Pills
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(subCardBg)
                                            .border(1.dp, accentRose, RoundedCornerShape(50))
                                            .clickable { onLogNew() }
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "SYS ${latest.systolic} mmHg",
                                            color = accentRose,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(subCardBg)
                                            .border(1.dp, accentCyan, RoundedCornerShape(50))
                                            .clickable { onLogNew() }
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "DIA ${latest.diastolic} mmHg",
                                            color = accentCyan,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom 3-Metric Hemodynamic Strip
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
                            HemodynamicMetricItem(
                                dotColor = accentEmerald,
                                title = "MAP Pressure",
                                subtitle = "$mapPressure mmHg (Optimal)",
                                highlightSubtitle = true,
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
                            HemodynamicMetricItem(
                                dotColor = accentCyan,
                                title = "Pulse Pressure",
                                subtitle = "$pulsePressure mmHg (Healthy)",
                                highlightSubtitle = false,
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
                            HemodynamicMetricItem(
                                dotColor = accentRose,
                                title = "Resting Pulse",
                                subtitle = "${latest.pulse} BPM (Normal)",
                                highlightSubtitle = false,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 3. AHA CLINICAL SPECTRUM BAR CARD
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isDark) 6.dp else 4.dp,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .border(1.2.dp, borderColor, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
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
                                text = "AHA CLINICAL SPECTRUM",
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(emeraldPillBg)
                                    .border(1.dp, statusColor, RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (category == "Normal") "Normal (<120/80)" else category,
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val pointerFraction = remember(latest) {
                            when {
                                latest.systolic < 120 && latest.diastolic < 80 -> 0.18f
                                latest.systolic < 130 && latest.diastolic < 80 -> 0.38f
                                latest.systolic < 140 || latest.diastolic < 90 -> 0.62f
                                else -> 0.86f
                            }
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val totalWidth = maxWidth
                            val knobOffset = (totalWidth * pointerFraction) - 9.dp

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val zones = listOf(
                                    Triple(accentEmerald, "Normal <120", category == "Normal"),
                                    Triple(accentAmber, "Elevated 120-129", category == "Elevated"),
                                    Triple(accentOrange, "Stage 1 130-139", category == "Stage 1"),
                                    Triple(accentCrimson, "Stage 2 140+", category == "Stage 2")
                                )
                                zones.forEach { (col, lbl, isCurrent) ->
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
                                            text = lbl,
                                            color = if (isCurrent) col else textSecondary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .offset(x = knobOffset.coerceAtLeast(0.dp), y = (-3).dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.5.dp, Color(0xFF0F172A), CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 4. HEMODYNAMIC TREND (7 READINGS) CARD
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
                                text = "HEMODYNAMIC TREND (7 READINGS)",
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentRose)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "SYS",
                                    color = textPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentCyan)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "DIA",
                                    color = textPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(subCardBg)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BloodPressureChart(
                                history = displayHistory,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Today")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayNames.forEachIndexed { idx, label ->
                                Text(
                                    text = label,
                                    color = if (idx == dayNames.lastIndex) accentRose else textSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 5. RECENT HEMODYNAMIC LOGS + BOTTOM CTA
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
                                text = "RECENT HEMODYNAMIC LOGS",
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp
                            )
                            Text(
                                text = "${displayHistory.size} Entries Saved",
                                color = accentRose,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        displayHistory.take(4).forEachIndexed { idx, entry ->
                            val entryCategory = when {
                                entry.systolic < 120 && entry.diastolic < 80 -> "Normal"
                                entry.systolic < 130 && entry.diastolic < 80 -> "Elevated"
                                entry.systolic < 140 || entry.diastolic < 90 -> "Stage 1"
                                else -> "Stage 2"
                            }
                            val entryColor = when (entryCategory) {
                                "Normal" -> accentEmerald
                                "Elevated" -> accentAmber
                                "Stage 1" -> accentOrange
                                else -> accentCrimson
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(subCardBg)
                                    .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(entryColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${entry.systolic} / ${entry.diastolic} mmHg",
                                            color = textPrimary,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "${entry.date}   •   Pulse: ${entry.pulse} BPM",
                                            color = textSecondary,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (entryCategory == "Normal") emeraldPillBg else Color.Transparent
                                            )
                                            .border(1.dp, entryColor, RoundedCornerShape(50))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = entryCategory,
                                            color = entryColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    if (savedHistory.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    repository.deleteBloodPressure(
                                                        entry.systolic,
                                                        entry.diastolic,
                                                        entry.pulse
                                                    )
                                                    prefsManager.deleteBloodPressureEntry(entry)
                                                    savedHistory = prefsManager.getBloodPressureHistory()
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Delete Entry",
                                                tint = textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (idx < minOf(3, displayHistory.lastIndex)) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full-Width Glowing CTA Button: + LOG NEW BLOOD PRESSURE READING
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = accentRose.copy(alpha = 0.45f),
                                    spotColor = accentCrimson.copy(alpha = 0.5f)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(accentRose, accentCrimson, accentRose)
                                    )
                                )
                                .clickable { onLogNew() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+  LOG NEW BLOOD PRESSURE READING",
                                color = Color(0xFF0F172A),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HemodynamicDualRingGauge(
    sysProgress: Float,
    diaProgress: Float,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val accentRose = Color(0xFFFB7185)
    val accentCyan = Color(0xFF22D3EE)
    val trackColor = if (isDark) Color(0xFF232E48) else Color(0xFFE2E8F0)
    val inactiveTickColor = if (isDark) Color(0xFF64748B).copy(alpha = 0.55f) else Color(0xFF94A3B8)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerTickRadius = size.minDimension * 0.49f
        val sysRadius = size.minDimension * 0.42f
        val diaRadius = size.minDimension * 0.33f

        // 1. Perimeter Scale Ticks
        val totalTicks = 28
        for (i in 0..totalTicks) {
            val frac = i.toFloat() / totalTicks.toFloat()
            val angleRad = Math.toRadians((150f + frac * 240f).toDouble())
            val isMajor = i % 4 == 0
            val innerR = outerTickRadius - (if (isMajor) 9.dp.toPx() else 5.dp.toPx())
            val outerR = outerTickRadius + 1.dp.toPx()

            val startPt = Offset(
                center.x + (innerR * cos(angleRad)).toFloat(),
                center.y + (innerR * sin(angleRad)).toFloat()
            )
            val endPt = Offset(
                center.x + (outerR * cos(angleRad)).toFloat(),
                center.y + (outerR * sin(angleRad)).toFloat()
            )
            drawLine(
                color = if (frac <= sysProgress) accentRose else inactiveTickColor,
                start = startPt,
                end = endPt,
                strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 2. Outer Ring: Systolic (Rose)
        val sysStroke = 15.dp.toPx()
        drawArc(
            color = trackColor,
            startAngle = 150f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(center.x - sysRadius, center.y - sysRadius),
            size = Size(sysRadius * 2f, sysRadius * 2f),
            style = Stroke(width = sysStroke, cap = StrokeCap.Butt)
        )
        drawArc(
            color = accentRose,
            startAngle = 150f,
            sweepAngle = 240f * sysProgress,
            useCenter = false,
            topLeft = Offset(center.x - sysRadius, center.y - sysRadius),
            size = Size(sysRadius * 2f, sysRadius * 2f),
            style = Stroke(width = sysStroke, cap = StrokeCap.Butt)
        )

        // 3. Inner Ring: Diastolic (Cyan)
        val diaStroke = 13.dp.toPx()
        drawArc(
            color = trackColor,
            startAngle = 150f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(center.x - diaRadius, center.y - diaRadius),
            size = Size(diaRadius * 2f, diaRadius * 2f),
            style = Stroke(width = diaStroke, cap = StrokeCap.Butt)
        )
        drawArc(
            color = accentCyan,
            startAngle = 150f,
            sweepAngle = 240f * diaProgress,
            useCenter = false,
            topLeft = Offset(center.x - diaRadius, center.y - diaRadius),
            size = Size(diaRadius * 2f, diaRadius * 2f),
            style = Stroke(width = diaStroke, cap = StrokeCap.Butt)
        )
    }
}

@Composable
private fun HemodynamicMetricItem(
    dotColor: Color,
    title: String,
    subtitle: String,
    highlightSubtitle: Boolean,
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
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = subtitle,
            color = if (highlightSubtitle) Color(0xFF10B981) else textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BloodPressureChart(
    history: List<SharedPrefsManager.BloodPressureEntry>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Not enough data to plot chart.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        return
    }

    val data = history.take(7).reversed()
    val accentRose = Color(0xFFFB7185)
    val accentCyan = Color(0xFF22D3EE)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padX = 16.dp.toPx()
        val padY = 14.dp.toPx()
        val plotW = (width - padX * 2f).coerceAtLeast(1f)
        val plotH = (height - padY * 2f).coerceAtLeast(1f)

        val minP = 60f
        val maxP = 140f
        val range = maxP - minP

        // Dashed Reference Lines for 120 (SYS) and 80 (DIA)
        val y120 = height - padY - ((120f - minP) / range) * plotH
        val y80 = height - padY - ((80f - minP) / range) * plotH
        drawLine(
            color = accentRose.copy(alpha = 0.35f),
            start = Offset(0f, y120),
            end = Offset(width, y120),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
        drawLine(
            color = accentCyan.copy(alpha = 0.35f),
            start = Offset(0f, y80),
            end = Offset(width, y80),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )

        val stepX = if (data.size > 1) plotW / (data.size - 1) else plotW
        val sysPoints = mutableListOf<Offset>()
        val diaPoints = mutableListOf<Offset>()

        data.forEachIndexed { idx, entry ->
            val x = if (data.size > 1) padX + idx * stepX else width / 2f
            val sysNorm = ((entry.systolic.toFloat() - minP) / range).coerceIn(0.05f, 0.96f)
            val diaNorm = ((entry.diastolic.toFloat() - minP) / range).coerceIn(0.04f, 0.90f)
            val ySys = height - padY - sysNorm * plotH
            val yDia = height - padY - diaNorm * plotH

            sysPoints.add(Offset(x, ySys))
            diaPoints.add(Offset(x, yDia))

            // Vertical Pulse Pressure Range Bar connecting DIA to SYS
            drawLine(
                color = accentRose.copy(alpha = 0.55f),
                start = Offset(x, ySys),
                end = Offset(x, yDia),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Connected Systolic & Diastolic Lines
        for (i in 0 until sysPoints.size - 1) {
            drawLine(
                color = accentRose,
                start = sysPoints[i],
                end = sysPoints[i + 1],
                strokeWidth = 2.6.dp.toPx()
            )
            drawLine(
                color = accentCyan,
                start = diaPoints[i],
                end = diaPoints[i + 1],
                strokeWidth = 2.6.dp.toPx()
            )
        }

        // Glowing Data Dots
        for (i in sysPoints.indices) {
            drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = sysPoints[i])
            drawCircle(color = accentRose, radius = 4.dp.toPx(), center = sysPoints[i])

            drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = diaPoints[i])
            drawCircle(color = accentCyan, radius = 4.dp.toPx(), center = diaPoints[i])
        }
    }
}

@Composable
fun BPValueItem(value: String, label: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
fun BPPickerColumn(
    label: String,
    unit: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }
    val subCardBg = if (isDark) Color(0xFF192236) else Color(0xFFEEF3FA)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val accentColor = when (label) {
        "Systolic" -> Color(0xFFFB7185)
        "Diastolic" -> Color(0xFF22D3EE)
        else -> Color(0xFF10B981)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Text(
            text = unit,
            color = textSecondary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))

        IconButton(
            onClick = { onValueChange(value + 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase",
                tint = accentColor
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(subCardBg)
                .border(1.2.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                color = textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }

        IconButton(
            onClick = { onValueChange(value - 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease",
                tint = accentColor
            )
        }
    }
}
