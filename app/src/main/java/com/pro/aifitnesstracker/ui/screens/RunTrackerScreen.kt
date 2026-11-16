package com.pro.aifitnesstracker.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.pro.aifitnesstracker.AiVoiceCoachManager
import com.pro.aifitnesstracker.HealthRepository
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.VoiceCoachPersona
import com.pro.aifitnesstracker.services.LocationTrackingService
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RunTrackerScreen(
    repository: HealthRepository,
    onSpeak: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val coachManager = remember { AiVoiceCoachManager.getInstance(context) }

    val permissionsList = remember {
        val list = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }
    val permissionState = rememberMultiplePermissionsState(permissions = permissionsList)

    var isRunning by remember { mutableStateOf(LocationTrackingService.isServiceRunning) }
    var isWalkingMode by remember { mutableStateOf(false) }
    var isAudioCoachEnabled by remember { mutableStateOf(coachManager.isEnabled()) }
    var activePersona by remember { mutableStateOf(coachManager.getCurrentPersona()) }
    var showCoachSelector by remember { mutableStateOf(false) }

    var lastSpokenDistanceKm by remember { mutableIntStateOf(0) }
    var lastSpokenTimeMin by remember { mutableIntStateOf(0) }

    val routePoints = remember { mutableStateListOf<Pair<Double, Double>>() }

    val infiniteTransition = rememberInfiniteTransition(label = "gpsPulse")
    val gpsPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Service updates
    var serviceDuration by remember { mutableIntStateOf(0) }
    var serviceDistanceMeters by remember { mutableFloatStateOf(0f) }
    var serviceSteps by remember { mutableIntStateOf(0) }

    // Offset accumulations for paused runs
    var offsetDuration by remember { mutableIntStateOf(0) }
    var offsetDistanceKm by remember { mutableFloatStateOf(0f) }
    var offsetSteps by remember { mutableIntStateOf(0) }

    val timeElapsed = offsetDuration + serviceDuration
    val distance = offsetDistanceKm + (serviceDistanceMeters / 1000f)
    val steps = offsetSteps + serviceSteps
    val caloriesBurned = if (isWalkingMode) (timeElapsed * 0.09f).toInt() else (timeElapsed * 0.17f).toInt()
    val cadenceSpm = if (timeElapsed > 5) ((steps / (timeElapsed / 60.0)).toInt()).coerceIn(0, 240) else 0

    // Listen for Location updates broadcasted by LocationTrackingService
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == LocationTrackingService.ACTION_BROADCAST) {
                    serviceDuration = intent.getIntExtra(LocationTrackingService.EXTRA_DURATION_SECONDS, 0)
                    serviceDistanceMeters = intent.getFloatExtra(LocationTrackingService.EXTRA_DISTANCE_METERS, 0f)
                    serviceSteps = intent.getIntExtra(LocationTrackingService.EXTRA_STEPS, 0)

                    val lat = intent.getDoubleExtra(LocationTrackingService.EXTRA_LATITUDE, 0.0)
                    val lng = intent.getDoubleExtra(LocationTrackingService.EXTRA_LONGITUDE, 0.0)
                    if (lat != 0.0 && lng != 0.0) {
                        val lastPoint = routePoints.lastOrNull()
                        if (lastPoint == null || kotlin.math.abs(lastPoint.first - lat) > 0.00003 || kotlin.math.abs(lastPoint.second - lng) > 0.00003) {
                            routePoints.add(Pair(lat, lng))
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(LocationTrackingService.ACTION_BROADCAST)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    val hours = timeElapsed / 3600
    val minutes = (timeElapsed % 3600) / 60
    val seconds = timeElapsed % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    val paceFormatted = if (distance > 0f) {
        val paceSecs = (timeElapsed / distance).toInt()
        String.format(Locale.US, "%d:%02d", paceSecs / 60, paceSecs % 60)
    } else "0:00"

    // Audio Coach Milestones
    LaunchedEffect(timeElapsed) {
        if (isRunning && isAudioCoachEnabled) {
            val currentDistanceInt = distance.toInt()
            if (currentDistanceInt > lastSpokenDistanceKm && currentDistanceInt > 0) {
                lastSpokenDistanceKm = currentDistanceInt
                coachManager.onRunSplitDistance(currentDistanceInt, paceFormatted, isWalkingMode)
            }

            val currentMin = timeElapsed / 60
            if (currentMin > 0 && currentMin % 5 == 0 && currentMin > lastSpokenTimeMin) {
                lastSpokenTimeMin = currentMin
                coachManager.onRunTimeElapsed(currentMin)
            }
        }
    }

    if (showCoachSelector) {
        CoachPersonaDialog(
            coachManager = coachManager,
            onDismiss = { showCoachSelector = false },
            onPersonaSelected = { persona ->
                activePersona = persona
            }
        )
    }

    // Unified Design System Colors
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF080B14) else Color(0xFFF8FAFC)
    val cardColor = if (isDark) Color(0xFF101726) else Color.White
    val surfaceAltColor = if (isDark) Color(0xFF161E2E) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF26334D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val limeAccent = Color(0xFF84CC16)
    val limeDark = Color(0xFF65A30D)
    val cyanAccent = Color(0xFF06B6D4)
    val amberAccent = Color(0xFFF59E0B)
    val crimsonAccent = Color(0xFFF43F5E)
    val emeraldAccent = Color(0xFF10B981)

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Circular Back Button
                    Surface(
                        shape = CircleShape,
                        color = cardColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        shadowElevation = if (isDark) 2.dp else 1.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Center Title & Status Badge (Weighted to prevent any collision)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isWalkingMode) "GPS WALK TRACKER" else "GPS RUN TRACKER",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = textPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isRunning) crimsonAccent.copy(alpha = if (isDark) 0.18f else 0.12f)
                            else emeraldAccent.copy(alpha = if (isDark) 0.18f else 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isRunning) crimsonAccent.copy(alpha = 0.5f) else emeraldAccent.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isRunning) crimsonAccent else emeraldAccent)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isRunning) "RECORDING GPS" else "GPS SATELLITE READY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRunning) crimsonAccent else (if (isDark) emeraldAccent else Color(0xFF15803D)),
                                    letterSpacing = 0.4.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Right Actions: Coach persona & Audio toggle (Compact & Unclipped)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Coach Persona Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, amberAccent.copy(alpha = 0.5f)),
                            shadowElevation = if (isDark) 2.dp else 1.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCoachSelector = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(activePersona.avatarEmoji, fontSize = 13.sp)
                                Text(
                                    text = activePersona.title.split(" ").first(),
                                    color = if (isDark) amberAccent else Color(0xFFB45309),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Voice Toggle Icon
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                            shadowElevation = if (isDark) 2.dp else 1.dp,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    isAudioCoachEnabled = !isAudioCoachEnabled
                                    coachManager.setEnabled(isAudioCoachEnabled)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isAudioCoachEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Toggle Audio Coach",
                                    tint = if (isAudioCoachEnabled) cyanAccent else textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // FIX CRITICAL OVERLAPPING: Use Surface with navigationBarsPadding() so buttons sit cleanly above Android nav bar!
            Surface(
                color = cardColor,
                shadowElevation = 16.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // CRITICAL FIX: Eliminates Android system navigation overlap!
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isRunning && timeElapsed == 0) {
                        // Large High-Visibility START Button
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = limeAccent,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    if (permissionState.allPermissionsGranted) {
                                        isRunning = true
                                        coachManager.onRunStart(isWalkingMode)
                                        val intent = Intent(context, LocationTrackingService::class.java).apply {
                                            action = LocationTrackingService.ACTION_START
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(intent)
                                        } else {
                                            context.startService(intent)
                                        }
                                    } else {
                                        permissionState.launchMultiplePermissionRequest()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Workout",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isWalkingMode) "START OUTDOOR WALK" else "START OUTDOOR RUN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else {
                        // Active Session Controls: PAUSE / RESUME + STOP & SAVE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRunning) {
                                // Pause Button
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = amberAccent.copy(alpha = if (isDark) 0.18f else 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, amberAccent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            isRunning = false
                                            coachManager.onRunPause()
                                            offsetDuration += serviceDuration
                                            offsetDistanceKm += (serviceDistanceMeters / 1000f)
                                            offsetSteps += serviceSteps
                                            serviceDuration = 0
                                            serviceDistanceMeters = 0f
                                            serviceSteps = 0
                                            val intent = Intent(context, LocationTrackingService::class.java).apply {
                                                action = LocationTrackingService.ACTION_STOP
                                            }
                                            context.stopService(intent)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Pause,
                                            contentDescription = "Pause",
                                            tint = if (isDark) amberAccent else Color(0xFFB45309),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "PAUSE",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isDark) amberAccent else Color(0xFFB45309)
                                        )
                                    }
                                }
                            } else {
                                // Resume Button
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = limeAccent,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (permissionState.allPermissionsGranted) {
                                                isRunning = true
                                                coachManager.onRunResume()
                                                val intent = Intent(context, LocationTrackingService::class.java).apply {
                                                    action = LocationTrackingService.ACTION_START
                                                }
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    context.startForegroundService(intent)
                                                } else {
                                                    context.startService(intent)
                                                }
                                            } else {
                                                permissionState.launchMultiplePermissionRequest()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Resume",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "RESUME",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }

                            // Finish & Save Button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = crimsonAccent.copy(alpha = if (isDark) 0.18f else 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, crimsonAccent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (timeElapsed > 5) {
                                            coachManager.onRunFinish(distance, (timeElapsed / 60).coerceAtLeast(1), caloriesBurned)
                                            scope.launch {
                                                val workoutType = if (isWalkingMode) "Walking" else "Running"
                                                repository.saveWorkout(workoutType, (timeElapsed / 60).coerceAtLeast(1), caloriesBurned)
                                            }
                                        }
                                        val intent = Intent(context, LocationTrackingService::class.java).apply {
                                            action = LocationTrackingService.ACTION_STOP
                                        }
                                        context.stopService(intent)

                                        isRunning = false
                                        routePoints.clear()
                                        lastSpokenDistanceKm = 0
                                        lastSpokenTimeMin = 0
                                        serviceDuration = 0
                                        serviceDistanceMeters = 0f
                                        serviceSteps = 0
                                        offsetDuration = 0
                                        offsetDistanceKm = 0f
                                        offsetSteps = 0
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = crimsonAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "FINISH",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = crimsonAccent
                                    )
                                }
                            }
                        }
                    }

                    AdBanner(adKey = "ad_run_tracker_banner")
                }
            }
        }
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Mode Selector Switch (Only before starting)
            if (!isRunning && timeElapsed == 0) {
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    shadowElevation = if (isDark) 2.dp else 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        // Run Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (!isWalkingMode) limeAccent else Color.Transparent
                                )
                                .clickable { isWalkingMode = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsRun,
                                    contentDescription = null,
                                    tint = if (!isWalkingMode) Color(0xFF0F172A) else textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Outdoor Run",
                                    color = if (!isWalkingMode) Color(0xFF0F172A) else textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Walk Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isWalkingMode) cyanAccent else Color.Transparent
                                )
                                .clickable { isWalkingMode = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = null,
                                    tint = if (isWalkingMode) Color(0xFF0F172A) else textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Outdoor Walk",
                                    color = if (isWalkingMode) Color(0xFF0F172A) else textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Hero Telemetry Gauge Card (Clean, Modern & Perfectly Proportioned)
            Surface(
                color = cardColor,
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shadowElevation = if (isDark) 4.dp else 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glowing arc progress track
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 11.dp.toPx()
                            val arcRadius = (size.minDimension - strokeW) / 2
                            val centerPt = Offset(size.width / 2, size.height / 2)
                            val arcTopLeft = Offset(centerPt.x - arcRadius, centerPt.y - arcRadius)
                            val arcSize = Size(arcRadius * 2, arcRadius * 2)

                            // Background Track
                            drawArc(
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )

                            // Active Progress Arc
                            val progressFactor = (distance % 1.0f).coerceIn(0f, 1f)
                            val activeSweep = (270f * (if (distance > 0f) progressFactor else 0.05f)).coerceIn(10f, 270f)

                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        limeAccent,
                                        cyanAccent,
                                        limeAccent
                                    )
                                ),
                                startAngle = 135f,
                                sweepAngle = activeSweep,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }

                        // Interior Info
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "TOTAL DURATION",
                                color = textSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = timeFormatted,
                                color = textPrimary,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) surfaceAltColor else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.2f", distance),
                                        color = if (isDark) limeAccent else limeDark,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "KM",
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4 Real Telemetry Metrics (Clean 2x2 Grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pace
                TelemetryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "PACE",
                    value = paceFormatted,
                    unit = "/km",
                    icon = Icons.Default.Speed,
                    accentColor = cyanAccent,
                    isDark = isDark,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                // Calories
                TelemetryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "BURNED",
                    value = "$caloriesBurned",
                    unit = "kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = amberAccent,
                    isDark = isDark,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Steps
                TelemetryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "STEPS",
                    value = "$steps",
                    unit = "total",
                    icon = Icons.Default.DirectionsWalk,
                    accentColor = limeAccent,
                    isDark = isDark,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                // Cadence (Real steps / minutes)
                TelemetryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "CADENCE",
                    value = "$cadenceSpm",
                    unit = "spm",
                    icon = Icons.Default.FlashOn,
                    accentColor = Color(0xFFA855F7),
                    isDark = isDark,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            // Dynamic GPS Route Map / Radar Card
            Surface(
                color = cardColor,
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shadowElevation = if (isDark) 3.dp else 1.5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(surfaceAltColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (routePoints.size >= 2) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            val minLat = routePoints.minOf { it.first }
                            val maxLat = routePoints.maxOf { it.first }
                            val minLng = routePoints.minOf { it.second }
                            val maxLng = routePoints.maxOf { it.second }

                            val latRange = (maxLat - minLat).coerceAtLeast(0.00010)
                            val lngRange = (maxLng - minLng).coerceAtLeast(0.00010)

                            val mappedPoints = routePoints.map { (lat, lng) ->
                                val normX = ((lng - minLng) / lngRange).toFloat()
                                val normY = (1f - ((lat - minLat) / latRange).toFloat())
                                Offset(normX * size.width, normY * size.height)
                            }

                            // Polyline shadow / glow
                            for (i in 0 until mappedPoints.size - 1) {
                                drawLine(
                                    color = limeAccent.copy(alpha = 0.25f),
                                    start = mappedPoints[i],
                                    end = mappedPoints[i + 1],
                                    strokeWidth = 8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = limeAccent,
                                    start = mappedPoints[i],
                                    end = mappedPoints[i + 1],
                                    strokeWidth = 4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }

                            // Start point (Emerald badge)
                            drawCircle(
                                color = emeraldAccent,
                                radius = 7.dp.toPx(),
                                center = mappedPoints.first()
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5.dp.toPx(),
                                center = mappedPoints.first()
                            )

                            // Current point (Pulsing marker)
                            val currentPos = mappedPoints.last()
                            drawCircle(
                                color = limeAccent.copy(alpha = 0.25f * gpsPulseScale),
                                radius = 16.dp.toPx() * gpsPulseScale,
                                center = currentPos
                            )
                            drawCircle(
                                color = limeAccent,
                                radius = 7.dp.toPx(),
                                center = currentPos
                            )
                            drawCircle(
                                color = Color(0xFF0F172A),
                                radius = 3.dp.toPx(),
                                center = currentPos
                            )
                        }

                        // Route overlay badges
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = cardColor.copy(alpha = 0.92f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(emeraldAccent, CircleShape)
                                    )
                                    Text(
                                        "Start Point",
                                        color = textPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = cardColor.copy(alpha = 0.92f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Text(
                                    text = "🛰️ ${routePoints.size} GPS Points",
                                    color = if (isDark) limeAccent else limeDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    } else {
                        // High-tech Satellite Radar Canvas
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerPt = Offset(size.width / 2, size.height / 2)
                            val gridSpacing = 36.dp.toPx()

                            // Grid lines
                            var x = 0f
                            while (x < size.width) {
                                drawLine(borderColor.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                                x += gridSpacing
                            }
                            var y = 0f
                            while (y < size.height) {
                                drawLine(borderColor.copy(alpha = 0.4f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                                y += gridSpacing
                            }

                            // Radar concentric rings
                            drawCircle(limeAccent.copy(alpha = 0.15f), 45.dp.toPx(), centerPt, style = Stroke(1.5.dp.toPx()))
                            drawCircle(cyanAccent.copy(alpha = 0.10f), 80.dp.toPx(), centerPt, style = Stroke(1.5.dp.toPx()))
                            drawCircle(limeAccent.copy(alpha = 0.20f * gpsPulseScale), 28.dp.toPx() * gpsPulseScale, centerPt)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isWalkingMode) Icons.Default.DirectionsWalk else Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = if (isDark) limeAccent else limeDark,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = if (isRunning) "🛰️ Live GPS Active • Recording Path..." else "🛰️ GPS Satellite Ready",
                                color = if (isRunning) (if (isDark) limeAccent else limeDark) else textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRunning) "Walk or run outdoors to record polyline" else "Tap start below to begin outdoor session",
                                color = textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Real Km Split Milestones Card (When active / completed)
            if (distance >= 1.0f) {
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    shadowElevation = if (isDark) 3.dp else 1.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "KM SPLITS",
                                color = textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Avg: $paceFormatted/km",
                                color = cyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        val totalKm = distance.toInt()
                        for (km in 1..totalKm.coerceAtMost(5)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(limeAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$km",
                                            color = if (isDark) limeAccent else limeDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        "Kilometer $km",
                                        color = textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    paceFormatted,
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom clearance spacer so content is never obscured by the bottom bar
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun TelemetryStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        color = cardColor,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDark) 2.dp else 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    letterSpacing = 0.8.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
