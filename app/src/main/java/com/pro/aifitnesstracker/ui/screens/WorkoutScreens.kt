package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*
import com.pro.aifitnesstracker.LocalAppStrings
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

// ─── Workout Library Screen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLibraryScreen(
    onBack: () -> Unit,
    onStartWorkout: (WorkoutData) -> Unit
) {
    val strings = LocalAppStrings.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val accentColor = MaterialTheme.colorScheme.primary

    val workouts = listOf(
        WorkoutData(
            id = "1",
            name = "Full Body Burner",
            icon = Icons.Rounded.FitnessCenter,
            durationMins = 20,
            kcal = 250,
            category = "Strength",
            instructions = listOf(
                "Jumping Jacks - 1 min",
                "Pushups - 3 sets of 15",
                "Bodyweight Squats - 3 sets of 20",
                "Plank - 1 min hold"
            ),
            color = Color(0xFFFF9800)
        ),
        WorkoutData(
            id = "2",
            name = "Core Crusher",
            icon = Icons.Rounded.WorkspacePremium,
            durationMins = 12,
            kcal = 140,
            category = "Core",
            instructions = listOf(
                "Crunches - 3 sets of 25",
                "Bicycle Crunches - 1 min",
                "Russian Twists - 3 sets of 20",
                "Plank - 1 min hold"
            ),
            color = Color(0xFFE91E63)
        ),
        WorkoutData(
            id = "3",
            name = "Cardio HIIT Blast",
            icon = Icons.Rounded.DirectionsRun,
            durationMins = 15,
            kcal = 210,
            category = "Cardio",
            instructions = listOf(
                "High Knees - 45 sec",
                "Mountain Climbers - 45 sec",
                "Burpees - 10 reps",
                "Rest - 15 sec",
                "Repeat 4 times"
            ),
            color = Color(0xFF4CAF50)
        ),
        WorkoutData(
            id = "4",
            name = "Upper Body Power",
            icon = Icons.Rounded.FitnessCenter,
            durationMins = 25,
            kcal = 280,
            category = "Strength",
            instructions = listOf(
                "Push-ups - 4 sets of 20",
                "Dumbbell Rows - 3 sets of 12",
                "Shoulder Press - 3 sets of 15",
                "Tricep Dips - 3 sets of 15",
                "Bicep Curls - 3 sets of 12"
            ),
            color = Color(0xFFFF9800)
        ),
        WorkoutData(
            id = "5",
            name = "Yoga Flow",
            icon = Icons.Rounded.SelfImprovement,
            durationMins = 20,
            kcal = 120,
            category = "Flexibility",
            instructions = listOf(
                "Sun Salutation - 5 rounds",
                "Warrior I & II - 2 min each",
                "Downward Dog - 1 min",
                "Child's Pose - 2 min",
                "Savasana - 3 min"
            ),
            color = MaterialTheme.colorScheme.primary
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        strings.workoutArchitect.uppercase(Locale.US),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Select a Routine",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(workouts) { workout ->
                WorkoutItemCard(workout = workout, onStart = { onStartWorkout(workout) })
            }
        }
    }
}

// ─── Workout Item Card ────────────────────────────────────────────────────────

@Composable
fun WorkoutItemCard(workout: WorkoutData, onStart: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(workout.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(workout.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${workout.category} • ${workout.durationMins} mins • ${workout.kcal} kcal",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Exercise count pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        "${workout.instructions.size} exercises",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            IconButton(
                onClick = onStart,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Start Workout", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

// ─── Workout Session Screen (Enhanced) ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    workout: WorkoutData?,
    repository: HealthRepository,
    onFinish: () -> Unit
) {
    val strings = LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val accentColor = workout?.color ?: MaterialTheme.colorScheme.primary

    var secondsElapsed by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(true) }
    var showSummary by remember { mutableStateOf(false) }

    // Exercise checklist state
    val exerciseList = workout?.instructions ?: emptyList()
    val checkedExercises = remember { mutableStateListOf(*Array(exerciseList.size) { false }) }
    val completedCount = checkedExercises.count { it }

    val context = LocalContext.current
    val coachManager = remember { AiVoiceCoachManager.getInstance(context) }
    var activePersona by remember { mutableStateOf(coachManager.getCurrentPersona()) }
    var showCoachSelector by remember { mutableStateOf(false) }

    var isListening by remember { mutableStateOf(false) }
    var voiceStatusText by remember { mutableStateOf("Tap mic or say commands: Pause, Resume, Next, Finish") }

    // Start workout audio coaching
    LaunchedEffect(Unit) {
        delay(600)
        coachManager.onWorkoutStart(
            workout?.name ?: "Workout",
            exerciseList.firstOrNull() ?: "Exercise"
        )
    }

    // Handle voice commands helper
    fun handleVoiceCommand(command: String) {
        val cmd = command.lowercase(Locale.ROOT).trim()
        when {
            cmd.contains("pause") || cmd.contains("stop") || cmd.contains("wait") -> {
                isTimerRunning = false
                voiceStatusText = "Workout Paused ⏸️"
                coachManager.onWorkoutPause()
            }
            cmd.contains("resume") || cmd.contains("start") || cmd.contains("play") || cmd.contains("go") -> {
                isTimerRunning = true
                voiceStatusText = "Workout Resumed ▶️"
                coachManager.onWorkoutResume()
            }
            cmd.contains("next") || cmd.contains("check") || cmd.contains("done") || cmd.contains("complete") -> {
                // Find first unchecked exercise
                val nextUncheckedIndex = checkedExercises.indexOfFirst { !it }
                if (nextUncheckedIndex != -1) {
                    checkedExercises[nextUncheckedIndex] = true
                    val nextExName = exerciseList.getOrNull(nextUncheckedIndex + 1)
                    val remaining = exerciseList.size - checkedExercises.count { it }
                    if (nextExName != null) {
                        voiceStatusText = "Checked: ${exerciseList[nextUncheckedIndex]} ✅"
                    } else {
                        voiceStatusText = "Completed all exercises! 🎉"
                    }
                    coachManager.onExerciseCompleted(nextExName, remaining)
                } else {
                    voiceStatusText = "All exercises are already checked!"
                    coachManager.speak("All exercises are already completed! Say finish to review.")
                }
            }
            cmd.contains("finish") || cmd.contains("end") || cmd.contains("save") -> {
                isTimerRunning = false
                showSummary = true
                voiceStatusText = "Workout finished! 🏆"
                coachManager.speak("Workout finished! Let's check your results.")
            }
            else -> {
                voiceStatusText = "Command not recognized: '$command'"
                coachManager.speak("Sorry, I didn't get that. Try saying next or pause.")
            }
        }
    }

    // Speech Recognizer setup
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    DisposableEffect(Unit) {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                voiceStatusText = "Listening for command..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                isListening = false
                voiceStatusText = "Listening stopped."
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                handleVoiceCommand(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        recognizer.setRecognitionListener(listener)
        speechRecognizer = recognizer
        onDispose {
            recognizer.destroy()
        }
    }

    // Toggle voice recognition
    fun toggleSpeechListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            voiceStatusText = "Listening stopped."
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                speechRecognizer?.startListening(intent)
                isListening = true
                voiceStatusText = "Listening..."
            } catch (e: Exception) {
                voiceStatusText = "Voice recognition failed: ${e.message}"
            }
        }
    }

    // Target duration in seconds
    val targetSeconds = (workout?.durationMins ?: 30) * 60
    val timerProgress = (secondsElapsed.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)

    // Live calorie burn (linear estimate)
    val liveCals = remember(secondsElapsed) {
        val kcalPerSecond = (workout?.kcal ?: 200).toFloat() / targetSeconds.toFloat()
        (kcalPerSecond * secondsElapsed).toInt()
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (true) {
                delay(1000)
                secondsElapsed++
            }
        }
    }

    // Animated ring progress
    val animatedProgress by animateFloatAsState(
        targetValue = timerProgress,
        animationSpec = tween(500),
        label = "ring_progress"
    )

    if (showSummary) {
        WorkoutSummaryDialog(
            workout = workout,
            secondsElapsed = secondsElapsed,
            caloriesBurned = liveCals,
            completedExercises = completedCount,
            totalExercises = exerciseList.size,
            accentColor = accentColor,
            onDone = {
                coroutineScope.launch {
                    repository.saveWorkout(
                        type = workout?.name ?: "Workout",
                        value = secondsElapsed / 60,
                        calories = liveCals
                    )
                    onFinish()
                }
            }
        )
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        workout?.name ?: "WORKOUT",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        isTimerRunning = false
                        showSummary = true
                    }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Finish",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable { showCoachSelector = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(activePersona.avatarEmoji, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activePersona.title.split(" ").first(),
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Animated Timer Ring ──────────────────────────────────────────
            item {
                val trackRingColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)),
                            RoundedCornerShape(28.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Timer Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(180.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 14.dp.toPx()
                                val inset = stroke / 2
                                val arcSize = Size(size.width - stroke, size.height - stroke)

                                // Track ring
                                drawArc(
                                    color = trackRingColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                                    topLeft = Offset(inset, inset),
                                    size = arcSize
                                )
                                // Progress ring
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            accentColor.copy(alpha = 0.5f),
                                            accentColor
                                        )
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = animatedProgress * 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                                    topLeft = Offset(inset, inset),
                                    size = arcSize
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val minutes = secondsElapsed / 60
                                val seconds = secondsElapsed % 60
                                Text(
                                    String.format("%02d:%02d", minutes, seconds),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    "ACTIVE",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Live stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LiveStatChip(
                                icon = Icons.Rounded.LocalFireDepartment,
                                value = "$liveCals",
                                label = "kcal",
                                color = MaterialTheme.colorScheme.primary
                            )
                            LiveStatChip(
                                icon = Icons.Rounded.CheckCircle,
                                value = "$completedCount/${exerciseList.size}",
                                label = "done",
                                color = MaterialTheme.colorScheme.primary
                            )
                            LiveStatChip(
                                icon = Icons.Rounded.Schedule,
                                value = "${workout?.durationMins ?: 30}",
                                label = "min goal",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress bar under ring
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Progress", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text(
                                    "${(animatedProgress * 100).toInt()}%",
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = accentColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // ── AI Voice Assistant Card ───────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isListening) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isListening) Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
                        else SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pulsating/Glowing Mic Button
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isListening) {
                                            Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), Color.Transparent))
                                        } else {
                                            Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent))
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable { toggleSpeechListening() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = "Microphone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${activePersona.avatarEmoji} ${activePersona.title}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { showCoachSelector = true }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Change", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = voiceStatusText,
                                    color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(14.dp))
                        
                        // Suggestion Chips / Instructions Row
                        Text(
                            text = "Say commands like: 'Pause', 'Resume', 'Next', 'Finish'",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // ── Exercise Checklist ───────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardColor, RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "EXERCISES",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "$completedCount of ${exerciseList.size} completed",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Completion progress bar
                        val exerciseProgress = if (exerciseList.isNotEmpty())
                            completedCount.toFloat() / exerciseList.size.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { exerciseProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        exerciseList.forEachIndexed { index, exercise ->
                            val isChecked = checkedExercises.getOrElse(index) { false }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        if (index < checkedExercises.size) {
                                            val wasChecked = checkedExercises[index]
                                            checkedExercises[index] = !wasChecked
                                            if (!wasChecked) {
                                                val nextExName = exerciseList.getOrNull(index + 1)
                                                val remaining = exerciseList.size - (completedCount + 1)
                                                coachManager.onExerciseCompleted(nextExName, remaining)
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Check circle
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            if (isChecked) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isChecked) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChecked) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Text(
                                            "${index + 1}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    exercise,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (index < exerciseList.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Control Buttons ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { 
                            isTimerRunning = !isTimerRunning
                            if (!isTimerRunning) coachManager.onWorkoutPause() else coachManager.onWorkoutResume()
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            if (isTimerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isTimerRunning) "PAUSE" else "RESUME",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            isTimerRunning = false
                            showSummary = true
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FINISH", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

// ─── Live Stat Chip ───────────────────────────────────────────────────────────

@Composable
fun LiveStatChip(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

// ─── Post-Workout Summary Dialog ─────────────────────────────────────────────

@Composable
fun WorkoutSummaryDialog(
    workout: WorkoutData?,
    secondsElapsed: Int,
    caloriesBurned: Int,
    completedExercises: Int,
    totalExercises: Int,
    accentColor: Color,
    onDone: () -> Unit
) {
    val minutes = secondsElapsed / 60
    val seconds = secondsElapsed % 60
    val completionPct = if (totalExercises > 0) (completedExercises * 100 / totalExercises) else 0

    val animRing by animateFloatAsState(
        targetValue = completionPct / 100f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "summary_ring"
    )

    Dialog(onDismissRequest = {}) {
        val summaryTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)),
                    RoundedCornerShape(28.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Trophy icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Workout Complete!",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    workout?.name ?: "Great session",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Completion ring
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 10.dp.toPx()
                        val inset = stroke / 2
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(
                            color = summaryTrackColor,
                            startAngle = -90f, sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                            topLeft = Offset(inset, inset), size = arcSize
                        )
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = animRing * 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                            topLeft = Offset(inset, inset), size = arcSize
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$completionPct%", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("done", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryStatItem(
                        value = String.format("%02d:%02d", minutes, seconds),
                        label = "Duration",
                        color = MaterialTheme.colorScheme.primary
                    )
                    SummaryStatItem(
                        value = "$caloriesBurned",
                        label = "Calories",
                        color = MaterialTheme.colorScheme.primary
                    )
                    SummaryStatItem(
                        value = "$completedExercises/$totalExercises",
                        label = "Exercises",
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Motivational message
                val msg = when {
                    completionPct == 100 -> "🔥 Perfect session! Every exercise crushed!"
                    completionPct >= 75 -> "💪 Excellent effort! Almost a perfect run."
                    completionPct >= 50 -> "👍 Solid workout! Keep building momentum."
                    else -> "✅ Every rep counts! Come back stronger tomorrow."
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Home, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE & FINISH", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// ─── Summary Stat Item ────────────────────────────────────────────────────────

@Composable
fun SummaryStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

