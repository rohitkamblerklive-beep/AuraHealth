package com.pro.aifitnesstracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.pro.aifitnesstracker.utils.BatteryOptimizationHelper

// Screen imports
import com.pro.aifitnesstracker.ui.screens.*
import com.pro.aifitnesstracker.utils.MotivationalQuotes

class MainActivity : ComponentActivity() {

    private var pendingNavigationRoute by mutableStateOf<String?>(null)

    private var tts: TextToSpeech? = null
    private lateinit var prefsManager: SharedPrefsManager
    private lateinit var viewModel: MainViewModel

    private val stepUpdateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            val steps = intent?.getIntExtra("steps", 0) ?: 0
            if (::viewModel.isInitialized) {
                viewModel.updateSteps(steps)
                saveCurrentDayStats()
                checkStepAchievements(steps)
            }
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        setupUI()
        startStepCounterIfPermitted(permissions)
        scheduleSmartReminders()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.navigationBarColor = AndroidColor.TRANSPARENT
        super.onCreate(savedInstanceState)
        updateRecentsTaskDescription()
        
        val initialNavTarget = intent?.getStringExtra("navigate_to") 
            ?: intent?.getStringExtra("NAVIGATE_TO") 
            ?: intent?.data?.getQueryParameter("screen")
        if (!initialNavTarget.isNullOrBlank()) {
            pendingNavigationRoute = initialNavTarget
        }

        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("FITSPHERE_FATAL", "FATAL CRASH on ${thread.name}: ${throwable.message}", throwable)
            try {
                val crashFile = java.io.File(getExternalFilesDir(null), "last_crash.txt")
                crashFile.writeText("Thread: ${thread.name}\nException: ${throwable.javaClass.name}\nMessage: ${throwable.message}\n" + throwable.stackTraceToString())
            } catch (_: Exception) {}
            prevHandler?.uncaughtException(thread, throwable)
        }

        prefsManager = SharedPrefsManager(this)
        RemoteConfigManager.initAndFetch(prefsManager)
        AdConfigManager.initialize(this, prefsManager)
        NotificationHelper(this).createNotificationChannel()
        com.pro.aifitnesstracker.utils.SmartAlarmManager.scheduleGoalReminder(this)
        FeatureRecommendationReceiver.scheduleRecommendationAlarm(this)
        
        val database = AppDatabase.getDatabase(this)
        val healthDao = database.healthDao()
        val repository = HealthRepository(healthDao, prefsManager)
        
        val factory = MainViewModelFactory(repository, prefsManager)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        viewModel.updateSteps(prefsManager.getStepsToday())

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setTTSLanguage(prefsManager.getLanguage())
            }
        }

        val startupPermissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startupPermissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startupPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stepUpdateReceiver, android.content.IntentFilter("com.pro.aifitnesstracker.STEP_UPDATE"), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stepUpdateReceiver, android.content.IntentFilter("com.pro.aifitnesstracker.STEP_UPDATE"))
        }

        // Sync FCM token if logged in
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (token != null) {
                        prefsManager.saveFcmToken(token)
                        FirebaseSyncManager.syncFcmTokenToCloud(firebaseUser.uid, token)
                    }
                }
            }
        }

        setupUI() // Always set content, setupUI will handle screen states

        val hasActivityPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else true

        if (hasActivityPermission) {
            startStepCounterService()
        }

        if (allPermissionsGranted(startupPermissions)) {
            scheduleSmartReminders()
        } else if (startupPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(startupPermissions.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        updateRecentsTaskDescription()

        val hasActivityPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else true

        if (hasActivityPermission) {
            startStepCounterService()
        }
        if (::viewModel.isInitialized && ::prefsManager.isInitialized) {
            viewModel.updateSteps(prefsManager.getStepsToday())
        }
    }

    private fun checkStepAchievements(steps: Int) {
        if (steps >= 1) prefsManager.unlockBadge("FIRST_STEP")
        if (steps >= 1000) prefsManager.unlockBadge("STEP_1K")
    }

    private fun saveCurrentDayStats() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val steps = viewModel.stepsTodayState.intValue
        prefsManager.saveDailyStats(today, steps, prefsManager.getTotalSquats())
        
        // Synchronize steps to Room Database and Cloud
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                viewModel.repository.updateSteps(today, steps)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setTTSLanguage(langCode: String) {
        val locale = if (langCode == "hi") Locale.Builder().setLanguage("hi").setRegion("IN").build() else Locale.Builder().setLanguage(langCode).build()
        tts?.language = locale
    }

    private fun scheduleSmartReminders() {
        val workManager = WorkManager.getInstance(this)

        if (prefsManager.isDailyReminderEnabled()) {
            val timeString = prefsManager.getReminderTime() // Format: "hh:mm AM/PM"
            var targetHour = 20
            var targetMin = 0
            try {
                val parts = timeString.split(" ")
                val timeParts = parts[0].split(":")
                var hour = timeParts[0].toInt()
                val min = timeParts[1].toInt()
                val amPm = parts.getOrNull(1) ?: "PM"
                
                if (amPm.uppercase() == "PM" && hour < 12) hour += 12
                if (amPm.uppercase() == "AM" && hour == 12) hour = 0
                targetHour = hour
                targetMin = min
            } catch (e: Exception) {
                targetHour = 20
                targetMin = 0
            }

            // Daily Health Reminder (Once a day)
            val dailyRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
                24, TimeUnit.HOURS
            ).setInitialDelay(calculateInitialDelay(targetHour, targetMin), TimeUnit.MINUTES)
                .addTag("daily_reminder")
                .build()

            workManager.enqueueUniquePeriodicWork(
                "daily_health_reminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyRequest
            )
        } else {
            workManager.cancelUniqueWork("daily_health_reminder")
        }

        if (prefsManager.isWaterReminderEnabled()) {
            val intervalHours = prefsManager.getWaterReminderInterval()
            val waterRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            ).addTag("water_reminder")
                .setInputData(androidx.work.Data.Builder().putString("type", "water").build())
                .build()

            workManager.enqueueUniquePeriodicWork(
                "periodic_water_reminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                waterRequest
            )
        } else {
            workManager.cancelUniqueWork("periodic_water_reminder")
        }
    }

    private fun startStepCounterIfPermitted(permissions: Map<String, Boolean>) {
        val physicalActivityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
        } else true

        if (physicalActivityGranted) {
            startStepCounterService()
        }
    }

    private fun startStepCounterService() {
        try {
            val intent = Intent(this, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {}
    }

    private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, targetMinute)
        calendar.set(Calendar.SECOND, 0)
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return (calendar.timeInMillis - now) / (1000 * 60) // Return minutes
    }

    private fun allPermissionsGranted(permissions: List<String>) = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }



    private fun setupUI() {
        setContent {
            var currentThemeName by remember { mutableStateOf(prefsManager.getAppTheme()) }
            var currentFontFamily by remember { mutableStateOf(prefsManager.getPremiumFontFamily()) }
            com.pro.aifitnesstracker.ui.theme.AIFitnessTrackerTheme(themeName = currentThemeName, fontFamilyName = currentFontFamily) {
                val startRoute = remember {
                    if (!prefsManager.isProfileSetupDone()) "PROFILE_SETUP"
                    else "STARTUP_QUOTE"
                }

                val navController = rememberNavController()
                var currentStrings by remember { mutableStateOf(getLanguageStrings(prefsManager.getLanguage())) }

                CompositionLocalProvider(LocalAppStrings provides currentStrings) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    var isDashboardExitOverlayVisible by remember { mutableStateOf(false) }
                    var isDashboardMounted by remember { mutableStateOf(false) }

                    val insetsController = remember(window) { WindowCompat.getInsetsController(window, window.decorView) }

                    // Notification Deep Linking Auto-Navigator
                    LaunchedEffect(pendingNavigationRoute, currentRoute) {
                        val target = pendingNavigationRoute
                        val bypassScreens = listOf("SPLASH", "STARTUP_QUOTE", "PERMISSION_DENIED", "PROFILE_SETUP")
                        if (!target.isNullOrBlank() && currentRoute != null && currentRoute !in bypassScreens) {
                            pendingNavigationRoute = null
                            try {
                                navController.navigate(target) {
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Deep link failed for target: $target", e)
                            }
                        }
                    }

                    LaunchedEffect(currentThemeName, isDashboardExitOverlayVisible, currentRoute) {
                        val isQuoteOrSplash = currentRoute in listOf("STARTUP_QUOTE", "SPLASH")
                        val isDark = currentThemeName != "LightClean" && currentThemeName != "LightMode" && currentThemeName != "Light"
                        insetsController.isAppearanceLightStatusBars = if (isQuoteOrSplash) false else !isDark
                        insetsController.isAppearanceLightNavigationBars = if (isQuoteOrSplash) false else !isDark

                        if (isDashboardExitOverlayVisible) {
                            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            insetsController.show(WindowInsetsCompat.Type.navigationBars())
                        }
                    }

                    LaunchedEffect(currentRoute) {
                        if (currentRoute != "DASHBOARD") {
                            isDashboardExitOverlayVisible = false
                        }
                    }

                    val bottomBarRoutes = setOf(
                        "DASHBOARD",
                        "WORKOUT_PLANNER",
                        "WORKOUT_LIBRARY",
                        "AI_COACH",
                        "AI_WORKOUT_GENERATOR",
                        "PERFORMANCE",
                        "ANALYTICS",
                        "PROFILE"
                    )
                    val showBottomBar = currentRoute in bottomBarRoutes &&
                            !isDashboardExitOverlayVisible &&
                            (currentRoute != "DASHBOARD" || isDashboardMounted)
                    val isQuoteOrSplash = currentRoute in listOf("STARTUP_QUOTE", "SPLASH")

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = if (isQuoteOrSplash) Color.Black else MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            if (showBottomBar) {
                                BottomNavigationBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { screen ->
                                        if (currentRoute != screen) {
                                            navController.navigate(screen) {
                                                popUpTo("DASHBOARD") {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
                        ) {
                            NavHost(navController = navController, startDestination = "SPLASH") {
                            composable(
                                "SPLASH",
                                exitTransition = { ExitTransition.None },
                                popExitTransition = { ExitTransition.None }
                            ) {
                                SplashScreen(onNavigate = { navController.navigate(startRoute) { popUpTo("SPLASH") { inclusive = true } } })
                            }
                            composable(
                                "STARTUP_QUOTE",
                                enterTransition = { EnterTransition.None },
                                exitTransition = { ExitTransition.None },
                                popEnterTransition = { EnterTransition.None },
                                popExitTransition = { ExitTransition.None }
                            ) {
                                StartupQuoteOverlay(
                                    quote = remember { MotivationalQuotes.getQuoteForToday() },
                                    langCode = remember { prefsManager.getLanguage() },
                                    journeyDay = prefsManager.getAppJourneyDay(),
                                    onDismiss = {
                                        navController.navigate("DASHBOARD") {
                                            popUpTo("STARTUP_QUOTE") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("PERMISSION_DENIED") { 
                                PermissionRequiredScreen(onRetry = {
                                    val activityPerms = mutableListOf<String>()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) activityPerms.add(Manifest.permission.ACTIVITY_RECOGNITION)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) activityPerms.add(Manifest.permission.POST_NOTIFICATIONS)
                                    if (allPermissionsGranted(activityPerms)) {
                                        startStepCounterService()
                                        val dest = if (!prefsManager.isProfileSetupDone()) "PROFILE_SETUP" else "STARTUP_QUOTE"
                                        navController.navigate(dest) { popUpTo("PERMISSION_DENIED") { inclusive = true } }
                                    } else {
                                        requestPermissionsLauncher.launch(activityPerms.toTypedArray())
                                    }
                                })
                            }
                            composable("ONBOARDING") { 
                                OnboardingScreen(onFinish = {
                                    prefsManager.setFeaturesSeen()
                                    val dest = if (!prefsManager.isProfileSetupDone()) "PROFILE_SETUP" else "STARTUP_QUOTE"
                                    navController.navigate(dest) { popUpTo("ONBOARDING") { inclusive = true } }
                                })
                            }
                            composable("PROFILE_SETUP") {
                                ProfileSetupWizardScreen(
                                    prefsManager = prefsManager,
                                    onLanguageChanged = { code ->
                                        currentStrings = getLanguageStrings(code)
                                        setTTSLanguage(code)
                                    },
                                    onFinish = {
                                        navController.navigate("DASHBOARD") { popUpTo("PROFILE_SETUP") { inclusive = true } }
                                    }
                                )
                            }
                            composable("LANGUAGE") { 
                                LanguageSelectionScreen(onLanguageSelected = { code ->
                                    prefsManager.saveLanguage(code)
                                    currentStrings = getLanguageStrings(code)
                                    setTTSLanguage(code)
                                    navController.navigate("GOAL_SELECTION") { popUpTo("LANGUAGE") { inclusive = true } }
                                })
                            }
                            composable("GOAL_SELECTION") { 
                                GoalSelectionScreen(onContinue = {
                                    prefsManager.setGoalsSelected()
                                    navController.navigate("DASHBOARD") { popUpTo("GOAL_SELECTION") { inclusive = true } }
                                })
                            }
                            composable(
                                "DASHBOARD",
                                enterTransition = {
                                    if (initialState.destination.route in listOf("STARTUP_QUOTE", "SPLASH")) {
                                        EnterTransition.None
                                    } else {
                                        fadeIn(animationSpec = tween(200))
                                    }
                                }
                            ) { 
                                LaunchedEffect(Unit) {
                                    isDashboardMounted = true
                                }
                                DashboardScreen(
                                    prefsManager = prefsManager,
                                    stepsToday = viewModel.stepsTodayState.intValue,
                                    onNavigate = { screen -> navController.navigate(screen) },
                                    repository = viewModel.repository,
                                    onExitOverlayVisible = { visible ->
                                        isDashboardExitOverlayVisible = visible
                                    }
                                )
                            }
                            composable("DIET_TRACKER") { 
                                DietTrackerScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { navController.navigate(it) },
                                    prefsManager = prefsManager,
                                    repository = viewModel.repository
                                )
                            }
                            composable("WORKOUT_PLANNER") { 
                                WorkoutPlannerScreen(
                                    onBack = { navController.popBackStack() },
                                    repository = viewModel.repository,
                                    prefsManager = prefsManager,
                                    onStartWorkout = { workout ->
                                        viewModel.activeWorkout.value = workout
                                        navController.navigate("WORKOUT_SESSION")
                                    },
                                    onNavigateToAiGenerator = { navController.navigate("AI_WORKOUT_GENERATOR") },
                                    onNavigate = { navController.navigate(it) }
                                )
                            }
                            composable("ANALYTICS") { 
                                AnalyticsScreen(
                                    prefsManager = prefsManager,
                                    stepsToday = viewModel.stepsTodayState.intValue,
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { navController.navigate(it) },
                                    repository = viewModel.repository
                                )
                            }
                            composable("ACHIEVEMENTS") { AchievementScreen(prefsManager = prefsManager, onBack = { navController.popBackStack() }) }
                            composable("HEART_RATE") { 
                                prefsManager.markFeatureUsed("heart_rate")
                                HeartRateMonitorScreen(
                                    prefsManager = prefsManager,
                                    repository = viewModel.repository,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("HEART_RATE_DETAIL") { 
                                prefsManager.markFeatureUsed("heart_rate")
                                val lastBpm = prefsManager.getLastBpm()
                                if (lastBpm > 0) {
                                    val hrv = prefsManager.getLastHrv()
                                    val stress = prefsManager.getLastStress()
                                    val energy = prefsManager.getLastEnergy()
                                    HeartRateMonitorScreen(
                                        prefsManager = prefsManager,
                                        repository = viewModel.repository,
                                        initialResult = HeartRateResult(lastBpm, hrv, stress, energy),
                                        onBack = { navController.popBackStack() }
                                    )
                                } else {
                                    navController.navigate("HEART_RATE") { popUpTo("HEART_RATE_DETAIL") { inclusive = true } }
                                }
                            }
                            composable("STEP_DETAIL") { 
                                StepDetailScreen(
                                    steps = viewModel.stepsTodayState.intValue,
                                    goal = prefsManager.getStepGoal(),
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { navController.navigate(it) },
                                    prefsManager = prefsManager
                                )
                            }
                            composable("STEP_GOAL") { StepGoalScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager) }
                            composable("WATER") { 
                                WaterTrackingScreen(
                                    prefsManager = prefsManager,
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { navController.navigate(it) },
                                    onWaterLogged = { glasses -> if (glasses >= 8) prefsManager.unlockBadge("WATER_HERO") },
                                    repository = viewModel.repository,
                                    onShowNotification = { viewModel.appNotification.value = it }
                                )
                            }
                            composable("WATER_GOAL") { WaterGoalScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager) }
                            composable("SLEEP_GOAL") { SleepGoalScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager) }
                            composable("CALORIE_GOAL") { CalorieGoalScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager) }
                            composable("NUTRITION") { 
                                prefsManager.markFeatureUsed("diet")
                                DietTrackerScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { navController.navigate(it) },
                                    prefsManager = prefsManager,
                                    repository = viewModel.repository
                                )
                            }
                            composable("SLEEP") { 
                                prefsManager.markFeatureUsed("sleep")
                                SleepOptimizerScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }, prefsManager = prefsManager, repository = viewModel.repository) 
                            }
                            composable("WORKOUT_LIBRARY") { 
                                prefsManager.markFeatureUsed("workout")
                                WorkoutLibraryScreen(
                                    onBack = { navController.popBackStack() },
                                    onStartWorkout = { workout ->
                                        viewModel.activeWorkout.value = workout
                                        navController.navigate("WORKOUT_SESSION")
                                    }
                                )
                            }
                            composable("AI_WORKOUT_GENERATOR") {
                                prefsManager.markFeatureUsed("workout")
                                AiWorkoutGeneratorScreen(
                                    onBack = { navController.popBackStack() },
                                    repository = viewModel.repository
                                )
                            }
                            composable("AI_COACH") {
                                prefsManager.markFeatureUsed("workout")
                                AiWorkoutGeneratorScreen(
                                    onBack = { navController.popBackStack() },
                                    repository = viewModel.repository
                                )
                            }
                            composable("FASTING_TRACKER") {
                                prefsManager.markFeatureUsed("fasting")
                                FastingTrackerScreen(
                                    onBack = { navController.popBackStack() },
                                    prefsManager = prefsManager
                                )
                            }
                            composable("WORKOUT_SESSION") { 
                                WorkoutSessionScreen(
                                    workout = viewModel.activeWorkout.value,
                                    repository = viewModel.repository,
                                    onFinish = { navController.popBackStack() }
                                )
                            }
                            composable("BODY_VISUALIZER") { BodyVisualizerScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager) }
                            composable("MUSCLE_HEATMAP") { MuscleHeatmapScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager, repository = viewModel.repository) }

                            composable("PROFILE") { 
                                ProfileScreen(
                                    prefsManager = prefsManager, 
                                    repository = viewModel.repository,
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { navController.navigate(it) },
                                    onLanguageChange = { code ->
                                        currentStrings = getLanguageStrings(code)
                                    },
                                    onThemeChange = { theme ->
                                        currentThemeName = theme
                                        prefsManager.saveAppTheme(theme)
                                    },
                                    onScheduleReminders = { scheduleSmartReminders() }
                                ) 
                            }
                            composable("YOGA") { 
                                prefsManager.markFeatureUsed("yoga")
                                YogaHubScreen(onBack = { navController.popBackStack() }) 
                            }
                            composable("CHALLENGE") { ChallengeScreen(prefsManager = prefsManager, onBack = { navController.popBackStack() }, onStartChallengeDay = { day, target -> viewModel.activeChallengeDay.intValue = day; viewModel.activeChallengeTarget.intValue = target; navController.navigate("CAMERA") }) }
                            composable("CAMERA") { 
                                CameraWorkoutScreen(
                                    challengeTarget = viewModel.activeChallengeTarget.intValue, 
                                    repository = viewModel.repository, 
                                    onFinishClicked = { repsDone ->
                                        prefsManager.addTotalSquats(repsDone)
                                        if (prefsManager.getTotalSquats() >= 100) prefsManager.unlockBadge("SQUAT_MASTER")
                                        prefsManager.checkAndSaveBestSession(repsDone)
                                        saveCurrentDayStats()
                                        if (viewModel.activeChallengeDay.intValue != -1 && viewModel.activeChallengeTarget.intValue != -1 && repsDone >= viewModel.activeChallengeTarget.intValue) prefsManager.completeChallengeDay(viewModel.activeChallengeDay.intValue)
                                        viewModel.activeChallengeDay.intValue = -1; viewModel.activeChallengeTarget.intValue = -1; navController.popBackStack()
                                    },
                                    onSpeak = { text -> tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) }
                                )
                            }
                            composable("BREATHING") { 
                                prefsManager.markFeatureUsed("breathing")
                                BreathingScreen(
                                    onBack = { navController.popBackStack() },
                                    repository = viewModel.repository
                                )
                            }
                            composable("LUNG_TEST") { LungTestScreen(onBack = { navController.popBackStack() }) }
                            composable("INJURY_RISK") { InjuryRiskScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager, stepsToday = viewModel.stepsTodayState.intValue) }
                            composable("ALGORITHM_LAB") { AlgorithmLabScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager, onNavigate = { navController.navigate(it) }) }
                            composable("RUN_TRACKER") { 
                                prefsManager.markFeatureUsed("run")
                                RunTrackerScreen(
                                    repository = viewModel.repository,
                                    onSpeak = { text -> tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) },
                                    onBack = { navController.popBackStack() }
                                ) 
                            }
                            composable("BP_RECORD") { BloodPressureRecordScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager, repository = viewModel.repository) }
                            composable("BP_DETAIL") { BloodPressureDetailScreen(prefsManager = prefsManager, repository = viewModel.repository, onBack = { navController.popBackStack() }, onLogNew = { navController.navigate("BP_RECORD") }) }
                            composable("WEIGHT_GOAL") { WeightGoalScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager) }
                            composable("WEIGHT_DETAIL") { WeightDetailScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }, prefsManager = prefsManager) }
                            composable("PERFORMANCE") { PerformanceTrendsScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager, onNavigate = { navController.navigate(it) }, repository = viewModel.repository) }
                            composable("AI_INTELLIGENCE_HUB") {
                                prefsManager.markFeatureUsed("workout")
                                com.pro.aifitnesstracker.ui.screens.AiIntelligenceHubScreen(
                                    onBack = { navController.popBackStack() },
                                    prefsManager = prefsManager,
                                    healthDao = viewModel.repository.getHealthDao()
                                )
                            }
                            composable("VISUALIZATIONS") { VisualizationsScreen(prefsManager = prefsManager, onBack = { navController.popBackStack() }) }
                            composable("STEP_REPORT") { StepReportDetailScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }, prefsManager = prefsManager) }
                            composable("CALORIE_REPORT") { CalorieReportDetailScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }, prefsManager = prefsManager, repository = viewModel.repository) }
                            composable("DISTANCE_REPORT") { DistanceReportDetailScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }, prefsManager = prefsManager) }
                            composable("TIME_REPORT") { TimeReportDetailScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }, prefsManager = prefsManager) }
                            composable("HYDRATION_REPORT") { HydrationReportDetailScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }, prefsManager = prefsManager) }
                            composable("ADD_WIDGET") { AddWidgetScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager) }
                            composable("SETTINGS") { SettingsScreen(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }, onLanguageChange = { code ->
                                currentStrings = getLanguageStrings(code)
                            }, onThemeChange = { theme ->
                                currentThemeName = theme
                                prefsManager.saveAppTheme(theme)
                            }, onScheduleReminders = { scheduleSmartReminders() }, prefsManager = prefsManager, repository = viewModel.repository) }
                            composable("INSTRUCTIONS") { InstructionsScreen(onBack = { navController.popBackStack() }) }
                            composable("MORE_SETTINGS") { MoreSettingsScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager) }
                            composable("REMINDER_SETTINGS") { ReminderScreen(onBack = { navController.popBackStack() }, prefsManager = prefsManager, onScheduleReminders = { scheduleSmartReminders() }) }
                            composable("ACTIVITY_HISTORY") { ActivityHistoryScreen(onBack = { navController.popBackStack() }, repository = viewModel.repository) }
                            composable("PRIVACY_POLICY") { PrivacyPolicyScreen(onBack = { navController.popBackStack() }) }
                            composable("AI_FOOD_SCANNER") {
                                prefsManager.markFeatureUsed("diet")
                                AiFoodScannerScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("AI_MEAL_PLANNER") {
                                prefsManager.markFeatureUsed("diet")
                                AiMealPlannerScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("PREMIUM") {
                                PremiumScreen(
                                    prefsManager = prefsManager,
                                    onBack = { navController.popBackStack() },
                                    onPurchase = { plan ->
                                        // In production: launch real billing flow
                                        // For now, activate premium locally (test mode)
                                        PremiumManager.activate(prefsManager, plan)
                                        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                        if (userId != null) {
                                            val expiry = prefsManager.getPremiumExpiryDate()
                                            FirebaseSyncManager.syncPremiumStatusToCloud(userId, true, expiry)
                                            // Upload existing local database records to cloud database upon premium upgrade!
                                            val appDatabase = AppDatabase.getDatabase(this@MainActivity)
                                            FirebaseSyncManager.uploadFullBackup(userId, appDatabase)
                                        }
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }


                        // --- XIAOMI / MIUI & BATTERY OPTIMIZATION PROMPT ---
                        var showBatteryOptDialog by remember { 
                            mutableStateOf(BatteryOptimizationHelper.shouldShowOptimizationPrompt(this@MainActivity))
                        }
                        if (showBatteryOptDialog && isDashboardMounted) {
                            AlertDialog(
                                onDismissRequest = { 
                                    showBatteryOptDialog = false 
                                    BatteryOptimizationHelper.dismissPrompt(this@MainActivity)
                                },
                                title = {
                                    Text(
                                        text = if (BatteryOptimizationHelper.isXiaomiOrMiui()) "Xiaomi / MIUI Step Tracking" else "Background Step Tracking",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Text(
                                        text = if (BatteryOptimizationHelper.isXiaomiOrMiui()) {
                                            "MIUI / Xiaomi battery saver can pause step tracking when your screen is locked.\n\nTo ensure steps count 24/7:\n1. Enable 'Autostart'\n2. Set Battery saver to 'No restrictions'"
                                        } else {
                                            "To ensure accurate step counting 24/7 when your screen is locked, please allow AuraHealth to run without battery restrictions."
                                        }
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showBatteryOptDialog = false
                                            BatteryOptimizationHelper.dismissPrompt(this@MainActivity)
                                            BatteryOptimizationHelper.openOptimizationSettings(this@MainActivity)
                                        }
                                    ) {
                                        Text("Enable")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            showBatteryOptDialog = false
                                            BatteryOptimizationHelper.dismissPrompt(this@MainActivity)
                                        }
                                    ) {
                                        Text("Later")
                                    }
                                }
                            )
                        }

                        // --- GLOBAL ANIMATED NOTIFICATION OVERLAY ---
                        viewModel.appNotification.value?.let { data ->
                            AnimatedNotificationOverlay(
                                data = data,
                                onDismiss = { viewModel.appNotification.value = null }
                            )
                        }
                    }
                }
            }
            }
        }
    }

        override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navTarget = intent.getStringExtra("navigate_to") 
            ?: intent.getStringExtra("NAVIGATE_TO") 
            ?: intent.data?.getQueryParameter("screen")
        if (!navTarget.isNullOrBlank()) {
            pendingNavigationRoute = navTarget
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        try {
            AiVoiceCoachManager.getInstance(this).shutdown()
        } catch (e: Exception) {}
        try {
            unregisterReceiver(stepUpdateReceiver)
        } catch (e: Exception) {}
        super.onDestroy()
    }

    private fun updateRecentsTaskDescription() {
        try {
            val src = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.app_logo)
            if (src != null) {
                val size = src.width.coerceAtMost(src.height)
                val circularBm = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas2 = android.graphics.Canvas(circularBm)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                paint.color = android.graphics.Color.WHITE
                canvas2.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                val scaledSrc = android.graphics.Bitmap.createScaledBitmap(src, size, size, true)
                canvas2.drawBitmap(scaledSrc, 0f, 0f, paint)
                paint.xfermode = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val taskDesc = android.app.ActivityManager.TaskDescription.Builder()
                        .setLabel(getString(R.string.app_name))
                        .setIcon(R.drawable.app_logo)
                        .setPrimaryColor(android.graphics.Color.WHITE)
                        .build()
                    setTaskDescription(taskDesc)
                } else {
                    @Suppress("DEPRECATION")
                    val taskDesc = android.app.ActivityManager.TaskDescription(
                        getString(R.string.app_name),
                        circularBm,
                        android.graphics.Color.WHITE
                    )
                    @Suppress("DEPRECATION")
                    setTaskDescription(taskDesc)
                }
            }
        } catch (_: Exception) {}
    }
}

@Composable
fun AnimatedNotificationOverlay(data: AppNotificationData, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(data) {
        visible = true
        delay(3500) // Show for 3.5 seconds
        visible = false
        delay(500) // Wait for exit animation
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(data.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(data.icon, fontSize = 28.sp)

                    // Pulse Effect
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )
                    Box(modifier = Modifier.size(50.dp).scale(scale).border(1.dp, data.color.copy(alpha = 0.2f), CircleShape))
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(data.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(data.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 18.sp)
                }
            }
        }
    }



}
