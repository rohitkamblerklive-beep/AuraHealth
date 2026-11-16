package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HeartRateMonitorScreen(
    prefsManager: SharedPrefsManager,
    repository: HealthRepository,
    initialResult: HeartRateResult? = null,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val isDark = MaterialTheme.colorScheme.surface.let {
        (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f
    }

    var progress by remember { mutableFloatStateOf(0f) }
    var isMeasuring by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<HeartRateResult?>(initialResult) }
    var statusMessage by remember {
        mutableStateOf("Place your fingertip gently over the rear camera lens & flash")
    }
    val scope = rememberCoroutineScope()

    // Collect authenticated Room + Cloud synced history
    val history by repository.getHeartRateHistory().collectAsState(initial = emptyList())

    // Resolve current displayed metrics from latest scan, history, or saved prefs
    val activeResult = remember(result, history) {
        result ?: history.firstOrNull()?.let {
            HeartRateResult(it.bpm, it.hrv, it.stress, it.energy)
        } ?: if (prefsManager.getLastBpm() > 0) {
            HeartRateResult(
                prefsManager.getLastBpm(),
                prefsManager.getLastHrv(),
                prefsManager.getLastStress(),
                prefsManager.getLastEnergy()
            )
        } else {
            HeartRateResult(72, 58, "Low", 85)
        }
    }

    val displayBpm = activeResult.bpm
    val displayHrv = activeResult.hrv
    val displayStress = activeResult.stress
    val displayEnergy = activeResult.energy

    // Executive Senior Palette: Solid black for Light Mode text, rich tones for Dark Mode
    val screenBgColor = if (isDark) Color(0xFF090B14) else Color(0xFFFFF6F7)
    val cardBgColor = if (isDark) Color(0xFF121626) else Color(0xFFFFFFFF)
    val subBoxBgColor = if (isDark) Color(0xFF1B2136) else Color(0xFFF9FAFB)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val rosePrimary = Color(0xFFF43F5E)
    val pinkAccent = Color(0xFFEC4899)
    val orangeAccent = Color(0xFFFB923C)
    val emeraldColor = Color(0xFF10B981)
    val cyanColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val amberColor = Color(0xFFF59E0B)

    val cardBorder = BorderStroke(
        1.2.dp,
        if (isDark) Color(0xFF1E263D) else Color(0xFFE2E8F0)
    )

    val zoneLabel = when {
        isMeasuring -> "SCANNING..."
        displayBpm < 96 -> "★ RESTING ZONE"
        displayBpm < 131 -> "🔥 FAT BURN"
        displayBpm < 161 -> "⚡ CARDIO"
        else -> "🚀 PEAK"
    }

    val zoneShortName = when {
        displayBpm < 96 -> "Resting Zone"
        displayBpm < 131 -> "Fat Burn Zone"
        displayBpm < 161 -> "Cardio Zone"
        else -> "Peak Zone"
    }

    // Heartbeat & ECG animations
    val infiniteTransition = rememberInfiniteTransition(label = "hr_infinite")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isMeasuring) 1.25f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isMeasuring) 420 else 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_scale"
    )

    val ecgPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isMeasuring) 1400 else 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ecg_phase"
    )

    val dialProgress = if (isMeasuring) {
        progress.coerceIn(0.05f, 1f)
    } else {
        ((displayBpm - 40f) / 140f).coerceIn(0.15f, 0.92f)
    }
    val animDial by animateFloatAsState(
        targetValue = dialProgress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "dial_anim"
    )

    Scaffold(
        containerColor = screenBgColor,
        topBar = {
            // Dedicated Unclipped Custom Top Bar with Status Bars Padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBgColor)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Elevated Circular Back Button
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = cardBgColor,
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E263D) else Color(0xFFE2E8F0)),
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Symmetrical Center Title & Live Sensor Capsule
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "CARDIO & BIOMETRICS LAB",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = rosePrimary,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "HEART RATE MONITOR",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        // Clean, Unclipped Sensor Capsule
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = rosePrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, rosePrimary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(rosePrimary, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isMeasuring) {
                                        "Optical PPG • Scanning ${(progress * 100).toInt()}%"
                                    } else {
                                        "Optical PPG Sensor • Ready"
                                    },
                                    color = rosePrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Right: Glowing Heart Avatar Badge
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                ambientColor = rosePrimary.copy(alpha = 0.35f),
                                spotColor = rosePrimary.copy(alpha = 0.35f)
                            )
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0xFF1E2436) else Color(0xFFFFECEF)
                            )
                            .border(1.2.dp, rosePrimary.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = rosePrimary,
                            modifier = Modifier.size(20.dp).scale(heartScale)
                        )
                    }
                }
            }
        },
        bottomBar = { AdBanner(adKey = "ad_heart_rate_banner") }
    ) { padding ->
        // Keep CameraX PreviewView alive when permission is granted so flash PPG measurement works seamlessly
        if (cameraPermissionState.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier.size(1.dp)
            ) { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (isMeasuring) {
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    Executors.newSingleThreadExecutor(),
                                    HeartRateAnalyzer(
                                        onProgressUpdate = { p -> progress = p },
                                        onFinish = { r ->
                                            result = r
                                            isMeasuring = false
                                            progress = 0f
                                            statusMessage = "Measurement Complete • Saved to Cloud"
                                            scope.launch {
                                                repository.saveHeartRate(r.bpm, r.hrv, r.stress, r.energy)
                                            }
                                        },
                                        onStateChanged = { msg ->
                                            statusMessage = msg
                                        }
                                    )
                                )
                            }
                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                            camera.cameraControl.enableTorch(true)
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(context))
                } else {
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProvider.unbindAll()
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(screenBgColor),
            contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. HERO PPG PULSE DIAL & LIVE ECG WAVEFORM ────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.2.dp, rosePrimary.copy(alpha = 0.25f)),
                    shadowElevation = if (isDark) 0.dp else 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(rosePrimary, CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "LIVE PULSE & ECG ANALYSIS",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = textSecondary
                                )
                            }

                            // Zone Badge with proper padding & no awkward line wrap
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = emeraldColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, emeraldColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = zoneLabel,
                                    color = emeraldColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Central Glowing Heart Rate Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(196.dp)
                        ) {
                            val trackColor = if (isDark) Color(0xFF1F273D) else Color(0xFFFFE4E6)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 14.dp.toPx()
                                val arcSize = Size(size.width - stroke, size.height - stroke)
                                val topLeft = Offset(stroke / 2f, stroke / 2f)

                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )

                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(rosePrimary, pinkAccent, orangeAccent, rosePrimary)
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = 360f * animDial,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = rosePrimary,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .scale(heartScale)
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = if (isMeasuring) "${(progress * 100).toInt()}%" else "$displayBpm",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = textPrimary
                                    )
                                    if (!isMeasuring) {
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "BPM",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = rosePrimary,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = (if (isDark) Color(0xFF1E263D) else Color(0xFFFFECEF)),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = (if (isMeasuring) "Hold Finger Steady" else zoneShortName).uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp,
                                        color = if (isMeasuring) rosePrimary else textSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Live Animated ECG Waveform Strip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(subBoxBgColor)
                                .border(1.dp, rosePrimary.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val midY = h / 2f
                                val cycleWidth = w / 3f
                                val shiftX = -ecgPhase * cycleWidth

                                val path = Path()
                                var curX = shiftX
                                path.moveTo(curX, midY)
                                while (curX < w + cycleWidth) {
                                    path.lineTo(curX + cycleWidth * 0.30f, midY)
                                    path.lineTo(curX + cycleWidth * 0.38f, midY - h * 0.22f)
                                    path.lineTo(curX + cycleWidth * 0.45f, midY + h * 0.25f)
                                    path.lineTo(curX + cycleWidth * 0.52f, midY - h * 0.80f)
                                    path.lineTo(curX + cycleWidth * 0.60f, midY + h * 0.70f)
                                    path.lineTo(curX + cycleWidth * 0.68f, midY - h * 0.15f)
                                    path.lineTo(curX + cycleWidth * 0.76f, midY)
                                    path.lineTo(curX + cycleWidth, midY)
                                    curX += cycleWidth
                                }

                                drawPath(
                                    path = path,
                                    color = rosePrimary,
                                    style = Stroke(
                                        width = 2.4.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = statusMessage,
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.height(14.dp))

                        // Start / Stop / Permission Action Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = rosePrimary.copy(alpha = 0.4f),
                                    spotColor = rosePrimary.copy(alpha = 0.4f)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFE11D48), Color(0xFFBE185D))
                                    )
                                )
                                .clickable {
                                    if (!cameraPermissionState.status.isGranted) {
                                        cameraPermissionState.launchPermissionRequest()
                                    } else {
                                        if (isMeasuring) {
                                            isMeasuring = false
                                            progress = 0f
                                            statusMessage = "Scan paused. Tap below to start again."
                                        } else {
                                            progress = 0f
                                            isMeasuring = true
                                            statusMessage = "Scanning pulse... Keep finger gently on lens & flash"
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    !cameraPermissionState.status.isGranted -> "🔓 GRANT CAMERA PERMISSION TO SCAN"
                                    isMeasuring -> "⏹ STOP CAMERA PPG SCAN"
                                    result != null -> "📸 RETAKE CAMERA PPG SCAN"
                                    else -> "📸 START CAMERA PPG SCAN"
                                },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            // ── 2. 3-COLUMN BIOMETRIC METRICS (HRV, STRESS, ENERGY) ───────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // HRV Index
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardBgColor,
                        border = cardBorder,
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(cyanColor.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📈", fontSize = 15.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "HRV INDEX",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$displayHrv ms",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = cyanColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = emeraldColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = if (displayHrv >= 45) "Balanced" else "Recovering",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = emeraldColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Stress Level
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardBgColor,
                        border = cardBorder,
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(emeraldColor.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🧘", fontSize = 15.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "STRESS LEVEL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            val stressColor = if (displayStress.equals("High", true)) rosePrimary else emeraldColor
                            Text(
                                text = displayStress,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = stressColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = stressColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = if (displayStress.equals("Low", true)) "Relaxed" else if (displayStress.equals("High", true)) "Elevated" else "Moderate",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = stressColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Body Energy
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardBgColor,
                        border = cardBorder,
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(amberColor.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 15.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "BODY ENERGY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$displayEnergy%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = amberColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = amberColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = if (displayEnergy >= 70) "Optimal" else "Advised Rest",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = amberColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. HEART RATE TRAINING ZONES ──────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = cardBgColor,
                    border = cardBorder,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CARDIO HEART RATE ZONES",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = textPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = rosePrimary.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, rosePrimary.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "$displayBpm BPM",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = rosePrimary,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Multi-zone bar with indicator
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(subBoxBgColor),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
                                        .background(cyanColor)
                                )
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(emeraldColor))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(amberColor))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                                        .background(rosePrimary)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZoneBadgeBox(
                                label = "Resting / Light",
                                range = "50–95 BPM",
                                color = cyanColor,
                                modifier = Modifier.weight(1f)
                            )
                            ZoneBadgeBox(
                                label = "Fat Burn",
                                range = "96–130 BPM",
                                color = emeraldColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZoneBadgeBox(
                                label = "Cardio Aerobic",
                                range = "131–160 BPM",
                                color = amberColor,
                                modifier = Modifier.weight(1f)
                            )
                            ZoneBadgeBox(
                                label = "Peak Effort",
                                range = "161+ BPM",
                                color = rosePrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── 4. RECENT MEASUREMENT HISTORY ─────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = cardBgColor,
                    border = cardBorder,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENT MEASUREMENTS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = textPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = rosePrimary.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text = "Synced with Cloud",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = rosePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        val dateFmt = remember { SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()) }

                        if (history.isEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Row 1: Current / Resting Check
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(subBoxBgColor)
                                        .border(0.8.dp, if (isDark) Color(0xFF1E263D) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(rosePrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$displayBpm",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = rosePrimary
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Resting Check • $displayStress",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Today, 09:15 AM • HRV ${displayHrv}ms",
                                                fontSize = 11.sp,
                                                color = textSecondary
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = emeraldColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "$displayEnergy% Energy",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = emeraldColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Row 2: Post-Workout Baseline Reference
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(subBoxBgColor)
                                        .border(0.8.dp, if (isDark) Color(0xFF1E263D) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(amberColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "118",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = amberColor
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Post-Workout • Moderate",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Yesterday, 06:40 PM • HRV 42ms",
                                                fontSize = 11.sp,
                                                color = textSecondary
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = amberColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "72% Energy",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = amberColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                history.take(6).forEach { item ->
                                    val itemZoneColor = when {
                                        item.bpm < 96 -> emeraldColor
                                        item.bpm < 131 -> amberColor
                                        else -> rosePrimary
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(subBoxBgColor)
                                            .border(0.8.dp, if (isDark) Color(0xFF1E263D) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                .background(itemZoneColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${item.bpm}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = itemZoneColor
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${if (item.bpm < 96) "Resting Check" else "Active Check"} • ${item.stress}",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = "${dateFmt.format(Date(item.timestamp))} • HRV ${item.hrv}ms",
                                                    fontSize = 11.sp,
                                                    color = textSecondary
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = itemZoneColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${item.energy}% Energy",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = itemZoneColor,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
}

@Composable
private fun ZoneBadgeBox(
    label: String,
    range: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = range,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
