package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraWorkoutScreen(
    challengeTarget: Int = -1,
    repository: HealthRepository,
    onFinishClicked: (Int) -> Unit,
    onSpeak: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xF2111726) else Color(0xF2FFFFFF)
    val cardBgAlt = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDCE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val trackColor = if (isDark) Color(0xFF1E2940) else Color(0xFFE2E8F0)

    val cyan = Color(0xFF06B6D4)
    val cyanLight = if (isDark) Color(0xFF22D3EE) else Color(0xFF0891B2)
    val gold = Color(0xFFF59E0B)
    val goldLight = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
    val emerald = Color(0xFF10B981)
    val emeraldLight = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val crimson = Color(0xFFF43F5E)
    val orange = Color(0xFFF97316)

    // Raw cumulative reps from CameraView + offsets for pause / reset
    var rawReps by remember { mutableIntStateOf(0) }
    var excludedReps by remember { mutableIntStateOf(0) }
    var rawAtPause by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    val reps = (rawReps - excludedReps).coerceAtLeast(0)

    var perfectForm by remember { mutableStateOf(false) }
    var currentFeedback by remember { mutableStateOf("Stand in frame") }
    var goodFormReps by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var voiceOn by remember { mutableStateOf(true) }

    val coachManager = remember { AiVoiceCoachManager.getInstance(context) }
    val coachName = remember { coachManager.getCurrentPersona().title }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    if (!hasCameraPermission) {
        val bg = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = 0.16f))
                        .border(2.dp, cyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = "Camera", tint = cyanLight, modifier = Modifier.size(38.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Camera Permission Required",
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Interactive Pose Tracking uses your front camera to detect reps and posture in real-time. Video stays private and is processed entirely on your device.",
                    color = textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = Color(0xFF0A0E17)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("GRANT CAMERA PERMISSION", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onFinishClicked(0) }) {
                    Text(strings.back, color = textSecondary, fontSize = 14.sp)
                }
            }
        }
        return
    }

    // Session timer (stops while paused)
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(1000)
            elapsedSeconds++
        }
    }

    // Voice count + good-form rep tracking
    LaunchedEffect(reps) {
        if (reps > 0 && !isPaused) {
            if (perfectForm) goodFormReps++
            if (voiceOn) onSpeak(reps.toString())
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val repsPerMin = if (elapsedSeconds >= 10) (reps * 60f / elapsedSeconds) else 0f
    val kcal = (reps * 0.5).toInt()
    val hasTarget = challengeTarget > 0
    val targetProgress = if (hasTarget) (reps.toFloat() / challengeTarget).coerceIn(0f, 1f) else 0f
    val goodFormPct = if (reps > 0) ((goodFormReps.coerceAtMost(reps) * 100f) / reps).toInt() else 0
    val remaining = if (hasTarget) (challengeTarget - reps).coerceAtLeast(0) else 0

    val coachLine = when {
        isPaused -> "Paused — saans lo, phir shuru karte hain."
        reps == 0 -> "Frame mein aao aur pehla squat shuru karo!"
        hasTarget && remaining == 0 -> "Target complete! Zabardast kaam!"
        hasTarget -> "Shabaash! $remaining aur, rukna nahi hai!"
        !perfectForm -> "Form par dhyan do — chest up, heels par push."
        else -> "Bahut badhiya! Rhythm banaye rakho."
    }

    fun finishAndSave() {
        scope.launch {
            if (reps > 0) {
                repository.saveWorkout("Squats", reps, kcal)
            }
            onFinishClicked(reps)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CameraView(
            onRepsUpdated = { rawReps = it },
            onFormUpdated = { perfectForm = it },
            onFeedbackUpdated = { currentFeedback = it }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ---------- TOP HUD ----------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = { finishAndSave() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isPaused) gold else crimson)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPaused) "TRACKING PAUSED" else "LIVE POSE TRACKING",
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Text(
                                text = "Smart Squat Counter • On-Device",
                                color = textSecondary,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(gold.copy(alpha = if (isDark) 0.20f else 0.15f))
                                .border(1.dp, gold, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                color = goldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Live Form Feedback Banner
                val fbAccent = if (perfectForm) emerald else crimson
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .background(fbAccent.copy(alpha = if (isDark) 0.18f else 0.14f))
                        .border(1.5.dp, fbAccent, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(fbAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (perfectForm) "OK" else "!",
                            color = Color(0xFF0A0E17),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (perfectForm) "PERFECT FORM" else "ADJUST FORM",
                            color = if (perfectForm) emeraldLight else crimson,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = currentFeedback,
                            color = textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right-side real metric chips
                Column(
                    modifier = Modifier.align(Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HudMetricChip("KCAL", "$kcal", orange, orange, cardBg, borderColor, textSecondary)
                    HudMetricChip("PACE", if (repsPerMin > 0f) "%.0f/m".format(repsPerMin) else "--", cyanLight, cyan, cardBg, borderColor, textSecondary)
                    HudMetricChip("GOOD", if (reps > 0) "$goodFormPct%" else "--", emeraldLight, emerald, cardBg, borderColor, textSecondary)
                }
            }

            // ---------- BOTTOM PANEL ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RepCounterRing(
                        reps = reps,
                        target = challengeTarget,
                        progress = if (hasTarget) targetProgress else ((reps % 20) / 20f),
                        trackColor = trackColor,
                        ringColor = cyan,
                        textPrimary = textPrimary,
                        labelColor = cyanLight,
                        targetColor = goldLight,
                        modifier = Modifier.size(132.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(gold.copy(alpha = if (isDark) 0.20f else 0.15f))
                                .border(1.dp, gold, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("SQUAT • LEG POWER", color = goldLight, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        Text(
                            text = if (hasTarget) "Challenge Target" else "Free Session",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        if (hasTarget) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(trackColor)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(targetProgress.coerceAtLeast(0.02f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (remaining == 0) emerald else cyan)
                                )
                            }
                            Text(
                                text = if (remaining == 0) "Target reached!" else "$remaining reps remaining",
                                color = if (remaining == 0) emeraldLight else textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "No target set — squat as many as you can.",
                                color = textSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "Good-form reps: ${goodFormReps.coerceAtMost(reps)} / $reps",
                            color = emeraldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Voice coach strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBgAlt)
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(gold.copy(alpha = 0.25f))
                            .border(1.5.dp, gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = coachName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString(""),
                            color = goldLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "$coachName: \"$coachLine\"",
                        color = textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (voiceOn) gold.copy(alpha = if (isDark) 0.20f else 0.15f) else cardBg)
                            .border(1.dp, if (voiceOn) gold else borderColor, RoundedCornerShape(8.dp))
                            .clickable { voiceOn = !voiceOn }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (voiceOn) "VOICE ON" else "VOICE OFF",
                            color = if (voiceOn) goldLight else textSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isPaused) gold.copy(alpha = 0.2f) else cardBgAlt)
                            .border(1.dp, if (isPaused) gold else borderColor, RoundedCornerShape(16.dp))
                            .clickable {
                                if (isPaused) {
                                    // Ignore any reps detected while paused
                                    excludedReps += (rawReps - rawAtPause).coerceAtLeast(0)
                                    isPaused = false
                                } else {
                                    rawAtPause = rawReps
                                    isPaused = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPaused) "RESUME" else "PAUSE",
                            color = if (isPaused) goldLight else textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cyan.copy(alpha = if (isDark) 0.18f else 0.14f))
                            .border(1.5.dp, cyan, RoundedCornerShape(16.dp))
                            .clickable {
                                excludedReps = rawReps
                                rawAtPause = rawReps
                                goodFormReps = 0
                                elapsedSeconds = 0
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("RESET", color = cyanLight, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(emerald)
                            .clickable { finishAndSave() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FINISH & SAVE", color = Color(0xFF0A0E17), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HudMetricChip(
    label: String,
    value: String,
    valueColor: Color,
    accent: Color,
    cardBg: Color,
    borderColor: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = textSecondary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun RepCounterRing(
    reps: Int,
    target: Int,
    progress: Float,
    trackColor: Color,
    ringColor: Color,
    textPrimary: Color,
    labelColor: Color,
    targetColor: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "RepRing"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val pad = stroke / 2f + 2.dp.toPx()
            val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            if (animated > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animated,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$reps", color = textPrimary, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = if (target > 0) "OF $target REPS" else "REPS",
                color = labelColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (target > 0) {
                Text("TARGET", color = targetColor, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
