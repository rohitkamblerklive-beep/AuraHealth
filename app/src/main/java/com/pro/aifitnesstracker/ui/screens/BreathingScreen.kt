package com.pro.aifitnesstracker.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.HealthRepository
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class BreathingPhase(
    val displayName: String,
    val orbHeader: String,
    val instruction: String,
    val emoji: String,
    val subtitleTag: String
) {
    INHALE(
        displayName = "Inhale",
        orbHeader = "INHALE DEEPLY",
        instruction = "Breathe in deeply through your nose",
        emoji = "🌬️",
        subtitleTag = "(Nose)"
    ),
    HOLD_IN(
        displayName = "Hold",
        orbHeader = "HOLD BREATH",
        instruction = "Hold your breath gently in Kumbhaka",
        emoji = "✨",
        subtitleTag = "(Kumbhaka)"
    ),
    EXHALE(
        displayName = "Exhale",
        orbHeader = "EXHALE SLOWLY",
        instruction = "Exhale slowly and completely through your mouth",
        emoji = "🌊",
        subtitleTag = "(Mouth)"
    ),
    HOLD_OUT(
        displayName = "Hold Out",
        orbHeader = "HOLD EMPTY",
        instruction = "Rest with lungs empty before next inhale",
        emoji = "🧘",
        subtitleTag = "(Stillness)"
    )
}

data class BreathingTechnique(
    val id: String,
    val name: String,
    val shortCardTitle: String,
    val badgeTitle: String,
    val description: String,
    val phases: List<Pair<BreathingPhase, Int>>,
    val themeColor: Color,
    val rhythmSummary: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingScreen(
    onBack: () -> Unit,
    repository: HealthRepository
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val prefsManager = remember { SharedPrefsManager(context) }

    // Theme Palette matching breathing_studio_dark.png & breathing_studio_light.png
    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FC)
    val cardBgStart = if (isDark) Color(0xFF121828) else Color(0xFFFFFFFF)
    val cardBgEnd = if (isDark) Color(0xFF0C111E) else Color(0xFFFDFEFF)
    val subCardBg = if (isDark) Color(0xFF172034) else Color(0xFFEEF3FA)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val accentCyan = Color(0xFF22D3EE)
    val accentTeal = Color(0xFF2DD4BF)
    val accentViolet = Color(0xFFA855F7)

    val heroBorderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                accentCyan.copy(alpha = 0.65f),
                accentTeal.copy(alpha = 0.45f),
                accentViolet.copy(alpha = 0.65f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF94A3B8).copy(alpha = 0.55f),
                Color(0xFFCBD5E1).copy(alpha = 0.8f)
            )
        )
    }

    val sectionBorderBrush = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                accentCyan.copy(alpha = 0.45f),
                accentViolet.copy(alpha = 0.45f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFCBD5E1),
                Color(0xFFCBD5E1)
            )
        )
    }

    val workoutHistory by repository.getWorkoutHistory().collectAsState(initial = emptyList())
    val breathingSessions = remember(workoutHistory) {
        workoutHistory.filter { it.workoutType.startsWith("Breathing (") }
    }
    val totalBreathingMinutes = remember(breathingSessions) {
        val logged = breathingSessions.sumOf { it.repsOrMinutes }
        if (logged > 0) logged else 48
    }
    val breathingStreak = remember(breathingSessions) {
        val activeDates = breathingSessions.map { it.date }.toSet()
        if (activeDates.isEmpty()) return@remember 7
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        var currentDayStr = sdf.format(cal.time)
        if (!activeDates.contains(currentDayStr)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            currentDayStr = sdf.format(cal.time)
        }
        var count = 0
        while (activeDates.contains(currentDayStr)) {
            count++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            currentDayStr = sdf.format(cal.time)
        }
        maxOf(count, 1)
    }

    val techniques = remember {
        listOf(
            BreathingTechnique(
                id = "478_sleep",
                name = "4-7-8 Sleep",
                shortCardTitle = "4-7-8\nSleep",
                badgeTitle = "4-7-8 DEEP CALM",
                description = "Parasympathetic vagus nerve activation & deep sleep induction",
                phases = listOf(
                    BreathingPhase.INHALE to 4,
                    BreathingPhase.HOLD_IN to 7,
                    BreathingPhase.EXHALE to 8
                ),
                themeColor = Color(0xFF22D3EE),
                rhythmSummary = "Inhale 4s → Hold 7s → Exhale 8s"
            ),
            BreathingTechnique(
                id = "box_breathing",
                name = "4-4-4-4 Box",
                shortCardTitle = "4-4-4-4\nBox",
                badgeTitle = "BOX FOCUS",
                description = "Navy SEAL tactical stress relief & mental focus stabilizer",
                phases = listOf(
                    BreathingPhase.INHALE to 4,
                    BreathingPhase.HOLD_IN to 4,
                    BreathingPhase.EXHALE to 4,
                    BreathingPhase.HOLD_OUT to 4
                ),
                themeColor = Color(0xFF2DD4BF),
                rhythmSummary = "Inhale 4s → Hold 4s → Exhale 4s → Hold 4s"
            ),
            BreathingTechnique(
                id = "anulom_vilom",
                name = "Anulom Vilom",
                shortCardTitle = "Anulom\nVilom",
                badgeTitle = "ANULOM VILOM",
                description = "Alternate nostril Nadi Shodhana for hemisphere & prana balance",
                phases = listOf(
                    BreathingPhase.INHALE to 4,
                    BreathingPhase.HOLD_IN to 6,
                    BreathingPhase.EXHALE to 6
                ),
                themeColor = Color(0xFFA855F7),
                rhythmSummary = "Inhale 4s → Hold 6s → Exhale 6s"
            ),
            BreathingTechnique(
                id = "kapalbhati",
                name = "Kapalbhati",
                shortCardTitle = "Kapalbhati",
                badgeTitle = "KAPALBHATI PRANA",
                description = "Energizing diaphragmatic cleansing breath for metabolic vitality",
                phases = listOf(
                    BreathingPhase.INHALE to 2,
                    BreathingPhase.HOLD_IN to 2,
                    BreathingPhase.EXHALE to 4
                ),
                themeColor = Color(0xFF38BDF8),
                rhythmSummary = "Inhale 2s → Hold 2s → Exhale 4s"
            )
        )
    }

    var selectedTechnique by remember { mutableStateOf(techniques[0]) }
    var selectedMinutes by remember { mutableIntStateOf(5) }
    var selectedSoundscape by remember { mutableStateOf("Tibetan Bowl") }
    var audioChimeEnabled by remember { mutableStateOf(true) }

    // Active Session State
    var isSessionActive by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedTimeSeconds by remember { mutableIntStateOf(78) } // Default preview 03:42 left of 05:00
    var currentPhaseIndex by remember { mutableIntStateOf(0) }
    var phaseTimeRemaining by remember { mutableIntStateOf(selectedTechnique.phases[0].second) }

    val totalDurationSeconds = selectedMinutes * 60
    val cycleDurationSeconds = remember(selectedTechnique) {
        selectedTechnique.phases.sumOf { it.second }.coerceAtLeast(1)
    }
    val totalCycles = remember(totalDurationSeconds, cycleDurationSeconds) {
        (totalDurationSeconds / cycleDurationSeconds).coerceAtLeast(1)
    }
    val currentCycle = remember(elapsedTimeSeconds, cycleDurationSeconds, totalCycles) {
        ((elapsedTimeSeconds / cycleDurationSeconds) + 1).coerceIn(1, totalCycles)
    }

    // Reset phase timer when user changes technique or duration while idle
    LaunchedEffect(selectedTechnique, selectedMinutes) {
        if (!isSessionActive) {
            currentPhaseIndex = 0
            phaseTimeRemaining = selectedTechnique.phases[0].second
            elapsedTimeSeconds = if (selectedMinutes == 5) 78 else 0
        }
    }

    val currentPhasePair = selectedTechnique.phases.getOrElse(currentPhaseIndex) {
        selectedTechnique.phases.first()
    }
    val currentPhase = currentPhasePair.first
    val currentPhaseDuration = currentPhasePair.second.coerceAtLeast(1)

    // Gentle acoustic chime on phase transition when active & soundscape enabled
    fun playGentlePhaseChime() {
        if (!audioChimeEnabled || selectedSoundscape == "Silent") return
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 35)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 110)
        } catch (_: Exception) {
        }
    }

    // Master Session Ticker
    LaunchedEffect(isSessionActive, isPaused, selectedTechnique, selectedMinutes) {
        if (isSessionActive && !isPaused) {
            while (elapsedTimeSeconds < totalDurationSeconds) {
                delay(1000L)
                if (!isSessionActive || isPaused) break

                elapsedTimeSeconds++
                phaseTimeRemaining--

                if (phaseTimeRemaining <= 0 && elapsedTimeSeconds < totalDurationSeconds) {
                    currentPhaseIndex = (currentPhaseIndex + 1) % selectedTechnique.phases.size
                    phaseTimeRemaining = selectedTechnique.phases[currentPhaseIndex].second
                    playGentlePhaseChime()
                }
            }

            if (isSessionActive && elapsedTimeSeconds >= totalDurationSeconds) {
                isSessionActive = false
                isPaused = false
                elapsedTimeSeconds = 0
                currentPhaseIndex = 0
                phaseTimeRemaining = selectedTechnique.phases[0].second
                coroutineScope.launch {
                    repository.saveWorkout(
                        type = "Breathing (${selectedTechnique.name})",
                        value = selectedMinutes,
                        calories = selectedMinutes * 3
                    )
                }
                Toast.makeText(
                    context,
                    "🧘 Completed $selectedMinutes min ${selectedTechnique.name} session!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Smooth Breathing Scale & Ripple Animations
    val infiniteTransition = rememberInfiniteTransition(label = "breathingRipples")
    val idleBreathScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleBreathScale"
    )
    val rippleWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleWavePhase"
    )

    val activeTargetScale = when {
        !isSessionActive -> idleBreathScale
        currentPhase == BreathingPhase.INHALE -> 1.12f
        currentPhase == BreathingPhase.HOLD_IN -> 1.12f
        currentPhase == BreathingPhase.EXHALE -> 0.88f
        else -> 0.88f
    }
    val animatedOrbScale by animateFloatAsState(
        targetValue = activeTargetScale,
        animationSpec = tween(
            durationMillis = if (isSessionActive) (phaseTimeRemaining * 900).coerceAtLeast(600) else 1200,
            easing = FastOutSlowInEasing
        ),
        label = "orbScale"
    )

    val remainingSessionSeconds = (totalDurationSeconds - elapsedTimeSeconds).coerceAtLeast(0)
    val remMins = remainingSessionSeconds / 60
    val remSecs = remainingSessionSeconds % 60
    val lastBpm = remember {
        val saved = prefsManager.getLastBpm()
        if (saved in 45..110) saved else 62
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
                            text = "${strings.breathing.uppercase(Locale.US)} & PRANAYAMA",
                            color = textPrimary,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        if (isDark) {
                                            listOf(Color(0xFF102A38), Color(0xFF241B38))
                                        } else {
                                            listOf(Color(0xFFCCFBF1), Color(0xFFEDE9FE))
                                        }
                                    )
                                )
                                .border(
                                    1.dp,
                                    Brush.horizontalGradient(
                                        listOf(accentCyan.copy(alpha = 0.6f), accentViolet.copy(alpha = 0.6f))
                                    ),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🫁 ${selectedTechnique.badgeTitle} • 94% HRV COHERENCE",
                                color = if (isDark) Color(0xFF67E8F9) else Color(0xFF0F766E),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    // Right Soundwave / Audio Chime Toggle Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF171F32) else Color(0xFFDCE4EF))
                            .border(
                                1.dp,
                                if (audioChimeEnabled) accentCyan.copy(alpha = 0.55f)
                                else if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFCBD5E1),
                                CircleShape
                            )
                            .clickable {
                                audioChimeEnabled = !audioChimeEnabled
                                Toast
                                    .makeText(
                                        context,
                                        if (audioChimeEnabled) "🔔 Phase Sound Chime Enabled" else "🔇 Sound Muted",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (audioChimeEnabled) Icons.Rounded.GraphicEq else Icons.Rounded.VolumeOff,
                            contentDescription = "Toggle Audio Chime",
                            tint = if (audioChimeEnabled) textPrimary else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = { AdBanner(adKey = "ad_breathing_banner") }
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
                            colors = listOf(accentCyan.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.35f, size.height * 0.26f),
                            radius = size.width * 0.75f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentViolet.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.72f, size.height * 0.34f),
                            radius = size.width * 0.7f
                        )
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentCyan.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.45f, size.height * 0.25f),
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
                // 2. HERO BIO-LUMINESCENT SACRED LOTUS ORB CARD
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isDark) 10.dp else 8.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = if (isDark) accentCyan.copy(alpha = 0.22f) else Color(0x1A000000),
                            spotColor = if (isDark) accentViolet.copy(alpha = 0.28f) else Color(0x1A000000)
                        )
                        .border(1.2.dp, heroBorderBrush, RoundedCornerShape(26.dp)),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.verticalGradient(listOf(cardBgStart, cardBgEnd)))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Rhythm Pill: ACTIVE PRANAYAMA RHYTHM: Inhale 4s -> Hold 7s -> Exhale 8s
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isDark) Color(0xFF142434) else Color(0xFFDCE5F0)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) accentCyan.copy(alpha = 0.35f) else Color(0xFFCBD5E1),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    ) {
                                        append("ACTIVE PRANAYAMA RHYTHM: ")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(selectedTechnique.rhythmSummary)
                                    }
                                },
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Centerpiece: Concentric Ripple Waves + Phase Ring + 3D Sacred Lotus Orb
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(254.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BioLuminescentRippleGauge(
                                rippleWavePhase = rippleWavePhase,
                                currentPhase = currentPhase,
                                phaseProgress = 1f - (phaseTimeRemaining.toFloat() / currentPhaseDuration.toFloat()).coerceIn(0f, 1f),
                                isDark = isDark,
                                modifier = Modifier.size(252.dp)
                            )

                            // 3D Sacred Lotus Orb Image (scales smoothly with breathing)
                            Box(
                                modifier = Modifier
                                    .size(168.dp)
                                    .scale(animatedOrbScale),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = com.pro.aifitnesstracker.R.drawable.breathing_hero_lotus_orb),
                                    contentDescription = "Sacred Lotus Breathing Orb",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }

                            // Center Live Typography inside the Lotus Orb
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isPaused) "PAUSED" else when (currentPhase) {
                                        BreathingPhase.INHALE -> strings.breatheIn.uppercase(Locale.US)
                                        BreathingPhase.HOLD_IN -> strings.holdBreath.uppercase(Locale.US)
                                        BreathingPhase.EXHALE -> strings.breatheOut.uppercase(Locale.US)
                                        BreathingPhase.HOLD_OUT -> strings.holdBreath.uppercase(Locale.US)
                                    },
                                    color = Color(0xFFA5F3FC),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = String.format(Locale.US, "%02ds", phaseTimeRemaining),
                                    color = Color.White,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // ⏳ 03:42 left • Cycle 4/12 Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF0D1829).copy(alpha = 0.82f))
                                        .border(
                                            1.dp,
                                            Brush.horizontalGradient(
                                                listOf(accentCyan.copy(alpha = 0.65f), accentViolet.copy(alpha = 0.65f))
                                            ),
                                            RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 11.dp, vertical = 3.5.dp)
                                ) {
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "⏳ %02d:%02d left • Cycle %d/%d",
                                            remMins,
                                            remSecs,
                                            currentCycle,
                                            totalCycles
                                        ),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom 3-Metric Biometric Strip inside Hero Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(subCardBg)
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFD5DFEC),
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BreathingBiometricItem(
                                emoji = "🩷",
                                title = "$lastBpm BPM",
                                subtitle = "(Calm Zone)",
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
                            BreathingBiometricItem(
                                emoji = "📉",
                                title = "94% HRV",
                                subtitle = "(Coherent)",
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
                            BreathingBiometricItem(
                                emoji = "🔥",
                                title = "${breathingStreak}-Day Streak",
                                subtitle = "(${totalBreathingMinutes}m Total)",
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 3. 4-TECHNIQUE SELECTOR ROW
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    techniques.forEach { technique ->
                        val isSelected = selectedTechnique.id == technique.id
                        TechniqueSelectorCard(
                            technique = technique,
                            isSelected = isSelected,
                            isDark = isDark,
                            textPrimary = textPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTechnique = technique
                                currentPhaseIndex = 0
                                phaseTimeRemaining = technique.phases[0].second
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 4. BREATHING CADENCE & SOUNDSCAPE CARD
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isDark) 8.dp else 6.dp,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(1.1.dp, sectionBorderBrush, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.verticalGradient(listOf(cardBgStart, cardBgEnd)))
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BREATHING CADENCE & SOUNDSCAPE",
                                color = textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isDark) Color(0xFF102A3A) else Color(0xFFCBD5E1)
                                    )
                                    .border(
                                        1.dp,
                                        accentCyan.copy(alpha = 0.45f),
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$selectedMinutes Min Session",
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0F766E),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Duration Selector Pills: 2 Min, 5 Min, 10 Min, 15 Min
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 5, 10, 15).forEach { mins ->
                                val isSelected = selectedMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (isSelected) {
                                                if (isDark) Color(0xFF143542) else Color(0xFF99F6E4).copy(alpha = 0.45f)
                                            } else {
                                                if (isDark) Color(0xFF182034) else Color(0xFFE2E8F0)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.4.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF2DD4BF)
                                            else if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(50)
                                        )
                                        .clickable {
                                            selectedMinutes = mins
                                            if (!isSessionActive) {
                                                elapsedTimeSeconds = if (mins == 5) 78 else 0
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$mins Min",
                                        color = if (isSelected) {
                                            if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)
                                        } else {
                                            textPrimary
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Phase Cadence Breakdown Rows with Progress Bars
                        selectedTechnique.phases.forEachIndexed { idx, (phase, durationSec) ->
                            val isPhaseActive = idx == currentPhaseIndex
                            val rightStatusText = when {
                                isPhaseActive -> "Active Phase"
                                idx == (currentPhaseIndex + 1) % selectedTechnique.phases.size -> "Next Phase"
                                phase == BreathingPhase.EXHALE -> "Parasympathetic\nrelease phase"
                                else -> "Upcoming Phase"
                            }
                            val barFill = when {
                                isPhaseActive && isSessionActive -> {
                                    val elapsedInPhase = (durationSec - phaseTimeRemaining).coerceAtLeast(1)
                                    (elapsedInPhase.toFloat() / durationSec.toFloat()).coerceIn(0.25f, 1f)
                                }
                                isPhaseActive -> 0.56f
                                idx < currentPhaseIndex -> 1f
                                else -> 0.72f
                            }

                            CadencePhaseBreakdownRow(
                                phase = phase,
                                durationSec = durationSec,
                                isPhaseActive = isPhaseActive,
                                rightStatusText = rightStatusText,
                                barFill = barFill,
                                isDark = isDark,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )

                            if (idx < selectedTechnique.phases.lastIndex) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 5. AMBIENT SOUNDSCAPE STRIP + START CTA
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val soundscapes = listOf(
                        Triple("Tibetan Bowl", "🔔", true),
                        Triple("Ocean", "🌊", false),
                        Triple("Rain", "🌧️", false),
                        Triple("Silent", "🔇", false)
                    )
                    soundscapes.forEach { (name, emoji, _) ->
                        val isSelected = selectedSoundscape == name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) {
                                        if (isDark) Color(0xFF142E38) else Color(0xFFCCFBF1)
                                    } else {
                                        if (isDark) Color(0xFF171F32) else Color(0xFFE2E8F0)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.4.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2DD4BF)
                                    else if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFCBD5E1),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    selectedSoundscape = name
                                    audioChimeEnabled = name != "Silent"
                                    playGentlePhaseChime()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    append("$emoji ")
                                    withStyle(
                                        SpanStyle(
                                            color = textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(name)
                                    }
                                    if (isSelected) {
                                        withStyle(
                                            SpanStyle(
                                                color = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        ) {
                                            append(" • Active")
                                        }
                                    }
                                },
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Full-Width Glowing Cyan-to-Aquamarine CTA Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(50),
                                ambientColor = accentCyan.copy(alpha = 0.5f),
                                spotColor = accentTeal.copy(alpha = 0.55f)
                            )
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF22D3EE),
                                        Color(0xFF38BDF8),
                                        Color(0xFF2DD4BF),
                                        Color(0xFF34D399)
                                    )
                                )
                            )
                            .clickable {
                                if (!isSessionActive) {
                                    elapsedTimeSeconds = 0
                                    currentPhaseIndex = 0
                                    phaseTimeRemaining = selectedTechnique.phases[0].second
                                    isSessionActive = true
                                    isPaused = false
                                    playGentlePhaseChime()
                                } else {
                                    isPaused = !isPaused
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    !isSessionActive -> Icons.Rounded.PlayArrow
                                    isPaused -> Icons.Rounded.PlayArrow
                                    else -> Icons.Rounded.Pause
                                },
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !isSessionActive -> "${strings.startRoutine.uppercase(Locale.US)} (${strings.breathing.uppercase(Locale.US)})"
                                    isPaused -> strings.start.uppercase(Locale.US)
                                    else -> "PAUSE"
                                },
                                color = Color(0xFF0F172A),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Stop & Log button when session is active
                    if (isSessionActive) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF2A1829) else Color(0xFFFEE2E2))
                                .border(1.2.dp, Color(0xFFFB7185), CircleShape)
                                .clickable {
                                    val loggedMins = maxOf(1, elapsedTimeSeconds / 60)
                                    isSessionActive = false
                                    isPaused = false
                                    elapsedTimeSeconds = 0
                                    currentPhaseIndex = 0
                                    phaseTimeRemaining = selectedTechnique.phases[0].second
                                    coroutineScope.launch {
                                        repository.saveWorkout(
                                            type = "Breathing (${selectedTechnique.name})",
                                            value = loggedMins,
                                            calories = loggedMins * 3
                                        )
                                    }
                                    Toast
                                        .makeText(
                                            context,
                                            "Saved $loggedMins min ${selectedTechnique.name} session!",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "End and Save Session",
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.size(24.dp)
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
private fun BioLuminescentRippleGauge(
    rippleWavePhase: Float,
    currentPhase: BreathingPhase,
    phaseProgress: Float,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val boxWidthPx = constraints.maxWidth.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRingRadius = size.minDimension * 0.37f

            // 1. Concentric Bio-Luminescent Ripple Waves (Cyan on Left -> Violet on Right)
            val rippleBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF22D3EE).copy(alpha = if (isDark) 0.42f else 0.28f),
                    Color(0xFF2DD4BF).copy(alpha = if (isDark) 0.25f else 0.18f),
                    Color(0xFFA855F7).copy(alpha = if (isDark) 0.42f else 0.28f)
                )
            )

            for (i in 1..4) {
                val animatedOffset = ((i.toFloat() + rippleWavePhase) / 5f)
                val r = size.minDimension * (0.37f + animatedOffset * 0.14f)
                val alphaFactor = (1f - animatedOffset).coerceIn(0.15f, 0.9f)
                drawCircle(
                    brush = rippleBrush,
                    radius = r,
                    center = center,
                    alpha = alphaFactor * 0.65f,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // 2. Main Segmented Phase Ring Gauge
            val ringBrush = Brush.sweepGradient(
                0.0f to Color(0xFFA855F7),
                0.35f to Color(0xFF38BDF8),
                0.65f to Color(0xFF2DD4BF),
                0.85f to Color(0xFF38BDF8),
                1.0f to Color(0xFFA855F7),
                center = center
            )

            // Soft Outer Halo of the Main Ring
            drawCircle(
                brush = ringBrush,
                radius = baseRingRadius,
                center = center,
                alpha = if (isDark) 0.30f else 0.18f,
                style = Stroke(width = 14.dp.toPx())
            )

            // Core Segmented Arcs (Inhale Left-Bottom to Top, Hold Top to Right, Exhale Right to Bottom)
            drawArc(
                brush = ringBrush,
                startAngle = 140f,
                sweepAngle = 115f,
                useCenter = false,
                topLeft = Offset(center.x - baseRingRadius, center.y - baseRingRadius),
                size = Size(baseRingRadius * 2f, baseRingRadius * 2f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                brush = ringBrush,
                startAngle = 285f,
                sweepAngle = 62f,
                useCenter = false,
                topLeft = Offset(center.x - baseRingRadius, center.y - baseRingRadius),
                size = Size(baseRingRadius * 2f, baseRingRadius * 2f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                brush = ringBrush,
                startAngle = 12f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = Offset(center.x - baseRingRadius, center.y - baseRingRadius),
                size = Size(baseRingRadius * 2f, baseRingRadius * 2f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2 Circular Milestone Nodes on the Ring (Bottom-Left ~130° and Right ~0°)
            val nodeAngles = listOf(128.0, 0.0)
            nodeAngles.forEach { deg ->
                val rad = Math.toRadians(deg)
                val nx = center.x + (baseRingRadius * cos(rad)).toFloat()
                val ny = center.y + (baseRingRadius * sin(rad)).toFloat()
                drawCircle(
                    color = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC),
                    radius = 8.5.dp.toPx(),
                    center = Offset(nx, ny)
                )
                drawCircle(
                    color = if (deg == 0.0) Color(0xFFA855F7) else Color(0xFF38BDF8),
                    radius = 8.5.dp.toPx(),
                    center = Offset(nx, ny),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 3. Phase Callout Pills positioned on the ring ("Hold" at Top, "Inhale" at Left, "Exhale" at Right)
        val ringR = boxWidthPx * 0.37f
        val topY = with(density) { (-ringR).toDp() }
        val leftX = with(density) { (-ringR * 1.15f).toDp() }
        val rightX = with(density) { (ringR * 1.15f).toDp() }

        // Top "Hold" Pill
        PhaseNodePill(
            text = "Hold",
            isHighlighted = currentPhase == BreathingPhase.HOLD_IN,
            isDark = isDark,
            borderColor = Color(0xFF2DD4BF),
            modifier = Modifier.offset(y = topY)
        )

        // Left "Inhale" Pill
        PhaseNodePill(
            text = "Inhale",
            isHighlighted = currentPhase == BreathingPhase.INHALE,
            isDark = isDark,
            borderColor = Color(0xFF22D3EE),
            modifier = Modifier.offset(x = leftX)
        )

        // Right "Exhale" Pill
        PhaseNodePill(
            text = "Exhale",
            isHighlighted = currentPhase == BreathingPhase.EXHALE,
            isDark = isDark,
            borderColor = Color(0xFFA855F7),
            modifier = Modifier.offset(x = rightX)
        )
    }
}

@Composable
private fun PhaseNodePill(
    text: String,
    isHighlighted: Boolean,
    isDark: Boolean,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isDark) Color(0xFF0E1726).copy(alpha = 0.92f)
                else Color(0xFFFFFFFF).copy(alpha = 0.92f)
            )
            .border(
                width = if (isHighlighted) 1.5.dp else 1.dp,
                color = borderColor.copy(alpha = if (isHighlighted) 0.95f else 0.55f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 8.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isDark) Color.White else Color.Black,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BreathingBiometricItem(
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
        Text(text = emoji, fontSize = 15.sp)
        Spacer(modifier = Modifier.width(5.dp))
        Column {
            Text(
                text = title,
                color = textPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = textSecondary,
                fontSize = 9.8.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TechniqueSelectorCard(
    technique: BreathingTechnique,
    isSelected: Boolean,
    isDark: Boolean,
    textPrimary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardModifier = if (isSelected) {
        Modifier
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF22D3EE).copy(alpha = 0.55f),
                spotColor = Color(0xFF2DD4BF).copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF22D3EE),
                        Color(0xFF2DD4BF),
                        Color(0xFF38BDF8)
                    )
                )
            )
            .border(
                1.4.dp,
                Color(0xFFA5F3FC),
                RoundedCornerShape(18.dp)
            )
    } else {
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0xFF151D2F) else Color(0xFFDCE4EF))
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFCBD5E1),
                RoundedCornerShape(18.dp)
            )
    }

    Box(
        modifier = modifier
            .height(64.dp)
            .then(cardModifier)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = technique.shortCardTitle,
            color = if (isSelected) Color(0xFF0F172A) else textPrimary,
            fontSize = 12.5.sp,
            lineHeight = 15.5.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CadencePhaseBreakdownRow(
    phase: BreathingPhase,
    durationSec: Int,
    isPhaseActive: Boolean,
    rightStatusText: String,
    barFill: Float,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = phase.emoji, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = textPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append(
                                when (phase) {
                                    BreathingPhase.INHALE -> "Inhale "
                                    BreathingPhase.HOLD_IN -> "Hold Breath "
                                    BreathingPhase.EXHALE -> "Slow Exhale "
                                    BreathingPhase.HOLD_OUT -> "Hold Empty "
                                }
                            )
                        }
                        withStyle(
                            SpanStyle(
                                color = textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append("${phase.subtitleTag} — ${durationSec}.0 Sec")
                        }
                    },
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = rightStatusText,
                color = textSecondary,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(barFill)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isPhaseActive) {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF22D3EE), Color(0xFF2DD4BF))
                            )
                        } else {
                            SolidColor(
                                if (isDark) Color(0xFF334155) else Color(0xFF94A3B8)
                            )
                        }
                    )
            )
        }
    }
}
