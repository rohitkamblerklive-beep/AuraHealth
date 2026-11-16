package com.pro.aifitnesstracker.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pro.aifitnesstracker.*
import com.pro.aifitnesstracker.data.water.*
import com.pro.aifitnesstracker.utils.WaterSoundGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

enum class WaterTrackerTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Tracker", Icons.Rounded.WaterDrop),
    HISTORY("History", Icons.Rounded.History),
    ANALYTICS("Trends", Icons.Rounded.BarChart),
    CONTAINERS("Bottles", Icons.Rounded.LocalDrink),
    ACHIEVEMENTS("Badges", Icons.Rounded.EmojiEvents)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterTrackingScreen(
    prefsManager: SharedPrefsManager,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    onWaterLogged: (Int) -> Unit = {},
    repository: HealthRepository,
    onShowNotification: (AppNotificationData) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val waterRepo = remember { WaterTrackerRepository.getInstance(context) }

    // Tab state
    var selectedTab by remember { mutableStateOf(WaterTrackerTab.DASHBOARD) }

    // Date & Formats
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFmt = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val todayStr = remember { sdf.format(Date()) }
    var viewingDate by remember { mutableStateOf(todayStr) }

    // Reactive data from Room
    val todayEntries by waterRepo.getEntriesForDate(todayStr).collectAsState(initial = emptyList())
    val viewingEntries by waterRepo.getEntriesForDate(viewingDate).collectAsState(initial = emptyList())
    val customContainers by waterRepo.getAllContainers().collectAsState(initial = emptyList())

    // Ensure default containers exist
    LaunchedEffect(Unit) {
        waterRepo.ensureDefaultContainers()
        // Ensure reminder is scheduled if enabled
        WaterReminderScheduler.schedule(context)
    }

    // Units & Settings state
    var measurementUnit by remember { mutableStateOf(prefsManager.getWaterUnit()) }
    var currentGoalMl by remember { mutableIntStateOf(prefsManager.getWaterGoalMl()) }

    // Modals
    var showCustomAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showGoalCalculatorDialog by remember { mutableStateOf(false) }
    var showAddContainerDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<WaterIntakeEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<WaterIntakeEntry?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Celebration state
    var showGoalMetBanner by remember { mutableStateOf(false) }

    // Totals calculation
    val todayTotalMl = remember(todayEntries) { todayEntries.sumOf { it.amountMl } }
    val todayPlainWaterMl = remember(todayEntries) {
        todayEntries.filter { it.drinkType == DrinkType.WATER.code }.sumOf { it.amountMl }
    }
    val todayRemainingMl = (currentGoalMl - todayTotalMl).coerceAtLeast(0)
    val todayProgressFraction = (todayTotalMl.toFloat() / currentGoalMl.toFloat()).coerceIn(0f, 1.5f)
    val todayProgressPct = ((todayTotalMl.toFloat() / currentGoalMl.toFloat()) * 100).roundToInt()
    val totalDrinksCount = todayEntries.size

    // Selected drink type for quick add
    var selectedDrinkType by remember { mutableStateOf(DrinkType.WATER) }

    // Theme palette
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }
    val screenBg = if (isDark) Color(0xFF070D18) else Color(0xFFF1F5F9)
    val cardBg = if (isDark) Color(0xFF0F1C2E) else Color(0xFFFFFFFF)
    val cardBorder = BorderStroke(1.dp, if (isDark) Color(0xFF1E3A5F) else Color(0xFFE2E8F0))
    val skyCyan = Color(0xFF06B6D4)
    val skyBlue = Color(0xFF38BDF8)
    val deepBlue = Color(0xFF0284C7)
    val emerald = Color(0xFF10B981)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    // Check goal celebration
    LaunchedEffect(todayTotalMl, currentGoalMl) {
        if (todayTotalMl >= currentGoalMl && currentGoalMl > 0) {
            val celebrated = prefsManager.getWaterCelebrationDate()
            if (celebrated != todayStr) {
                showGoalMetBanner = true
                prefsManager.saveWaterCelebrationDate(todayStr)
                onWaterLogged(todayTotalMl / 250)
            }
        }
    }

    // Quick add action handler
    val onQuickAdd: (Int, String) -> Unit = { amountMl: Int, containerName: String ->
        scope.launch {
            try {
                waterRepo.addWaterEntry(
                    amountMl = amountMl,
                    drinkType = selectedDrinkType.code,
                    containerName = containerName
                )
                WaterSoundGenerator.playSound(context)
                snackbarMessage = "+${waterRepo.formatVolume(amountMl, measurementUnit)} ${selectedDrinkType.displayName} logged! 💧"
            } catch (e: Exception) {
                snackbarMessage = e.message ?: "Failed to log drink"
            }
        }
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            Surface(
                color = cardBg,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            ) {
                Column(
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
                    // Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    // Title
                    Text(
                        text = strings.waterTracker.uppercase(Locale.US),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Actions: Unit toggle + Settings
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Quick Unit Toggle Chip (ml / fl oz)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val nextUnit = if (measurementUnit == "ml") "oz" else "ml"
                                    measurementUnit = nextUnit
                                    prefsManager.setWaterUnit(nextUnit)
                                    snackbarMessage = "Unit switched to ${if (nextUnit == "oz") "Fluid Ounces (fl oz)" else "Milliliters (ml)"}"
                                }
                        ) {
                            Text(
                                text = measurementUnit.uppercase(),
                                color = skyBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Settings Icon Button
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Hydration Settings",
                                tint = textPrimary
                            )
                        }
                    }
                }
            }
        }
    },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WaterTrackerTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val itemColor = if (isDark) Color.White else Color.Black
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedTab = tab },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) (if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f))
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = itemColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = when (tab) {
                                        WaterTrackerTab.DASHBOARD -> strings.waterTracker.take(8)
                                        WaterTrackerTab.HISTORY -> strings.history
                                        WaterTrackerTab.ANALYTICS -> strings.analytics
                                        WaterTrackerTab.CONTAINERS -> tab.title
                                        WaterTrackerTab.ACHIEVEMENTS -> strings.achievements.take(8)
                                    },
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    color = itemColor,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                WaterTrackerTab.DASHBOARD -> {
                    DashboardTabContent(
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        skyBlue = skyBlue,
                        deepBlue = deepBlue,
                        emerald = emerald,
                        measurementUnit = measurementUnit,
                        waterRepo = waterRepo,
                        todayTotalMl = todayTotalMl,
                        todayPlainWaterMl = todayPlainWaterMl,
                        goalMl = currentGoalMl,
                        remainingMl = todayRemainingMl,
                        progressFraction = todayProgressFraction,
                        progressPct = todayProgressPct,
                        totalDrinksCount = totalDrinksCount,
                        selectedDrinkType = selectedDrinkType,
                        onDrinkTypeSelected = { selectedDrinkType = it },
                        customContainers = customContainers,
                        onQuickAdd = onQuickAdd,
                        onCustomAmountClick = { showCustomAddDialog = true },
                        drinkEntries = todayEntries,
                        onEditEntry = { editingEntry = it },
                        onDeleteEntry = { deletingEntry = it },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
                WaterTrackerTab.HISTORY -> {
                    HistoryTabContent(
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        skyBlue = skyBlue,
                        measurementUnit = measurementUnit,
                        waterRepo = waterRepo,
                        goalMl = currentGoalMl,
                        viewingDate = viewingDate,
                        onDateSelected = { viewingDate = it },
                        entries = viewingEntries,
                        onEditEntry = { editingEntry = it },
                        onDeleteEntry = { deletingEntry = it }
                    )
                }
                WaterTrackerTab.ANALYTICS -> {
                    AnalyticsTabContent(
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        skyBlue = skyBlue,
                        measurementUnit = measurementUnit,
                        waterRepo = waterRepo,
                        goalMl = currentGoalMl
                    )
                }
                WaterTrackerTab.CONTAINERS -> {
                    ContainersTabContent(
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        skyBlue = skyBlue,
                        measurementUnit = measurementUnit,
                        waterRepo = waterRepo,
                        containers = customContainers,
                        onAddContainerClick = { showAddContainerDialog = true },
                        onQuickAdd = onQuickAdd
                    )
                }
                WaterTrackerTab.ACHIEVEMENTS -> {
                    AchievementsTabContent(
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        skyBlue = skyBlue,
                        prefsManager = prefsManager,
                        waterRepo = waterRepo,
                        todayTotalMl = todayTotalMl,
                        goalMl = currentGoalMl
                    )
                }
            }

            // Snackbar banner at bottom
            AnimatedVisibility(
                visible = snackbarMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, skyBlue.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = snackbarMessage ?: "", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }
                }
                LaunchedEffect(snackbarMessage) {
                    kotlinx.coroutines.delay(2600)
                    snackbarMessage = null
                }
            }

            // Goal Reached Celebration Modal
            if (showGoalMetBanner) {
                GoalAchievedDialog(
                    todayTotalMl = todayTotalMl,
                    goalMl = currentGoalMl,
                    unit = measurementUnit,
                    waterRepo = waterRepo,
                    onDismiss = { showGoalMetBanner = false }
                )
            }

            // Custom Amount Dialog
            if (showCustomAddDialog) {
                CustomWaterAddDialog(
                    initialDrinkType = selectedDrinkType,
                    unit = measurementUnit,
                    waterRepo = waterRepo,
                    onDismiss = { showCustomAddDialog = false },
                    onConfirm = { amountMl, drinkType, containerName, note ->
                        showCustomAddDialog = false
                        scope.launch {
                            try {
                                waterRepo.addWaterEntry(
                                    amountMl = amountMl,
                                    drinkType = drinkType.code,
                                    containerName = containerName,
                                    note = note
                                )
                                WaterSoundGenerator.playSound(context)
                                snackbarMessage = "+${waterRepo.formatVolume(amountMl, measurementUnit)} ${drinkType.displayName} logged! 💧"
                            } catch (e: Exception) {
                                snackbarMessage = e.message ?: "Failed to log drink"
                            }
                        }
                    }
                )
            }

            // Edit Entry Dialog
            editingEntry?.let { entry ->
                EditWaterEntryDialog(
                    entry = entry,
                    unit = measurementUnit,
                    waterRepo = waterRepo,
                    onDismiss = { editingEntry = null },
                    onSave = { updated ->
                        editingEntry = null
                        scope.launch {
                            try {
                                waterRepo.updateWaterEntry(updated)
                                snackbarMessage = "Entry updated successfully! 💧"
                            } catch (e: Exception) {
                                snackbarMessage = e.message ?: "Failed to update entry"
                            }
                        }
                    }
                )
            }

            // Delete Confirmation Dialog
            deletingEntry?.let { entry ->
                AlertDialog(
                    onDismissRequest = { deletingEntry = null },
                    title = { Text("Delete Drink Entry?") },
                    text = {
                        Text("Are you sure you want to delete ${waterRepo.formatVolume(entry.amountMl, measurementUnit)} of ${DrinkType.fromCode(entry.drinkType).displayName}? This will update your daily total and statistics.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val toDelete = entry
                                deletingEntry = null
                                scope.launch {
                                    waterRepo.deleteWaterEntry(toDelete)
                                    snackbarMessage = "Entry deleted! Statistics recalculated."
                                }
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deletingEntry = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Hydration Settings Dialog
            if (showSettingsDialog) {
                WaterSettingsDialog(
                    prefsManager = prefsManager,
                    currentGoalMl = currentGoalMl,
                    unit = measurementUnit,
                    waterRepo = waterRepo,
                    onDismiss = { showSettingsDialog = false },
                    onGoalChanged = { newGoal ->
                        currentGoalMl = newGoal
                        waterRepo.saveDailyGoalMl(newGoal)
                        snackbarMessage = "Daily goal set to ${waterRepo.formatVolume(newGoal, measurementUnit)}"
                    },
                    onUnitChanged = { newUnit ->
                        measurementUnit = newUnit
                        prefsManager.setWaterUnit(newUnit)
                    },
                    onOpenCalculator = {
                        showSettingsDialog = false
                        showGoalCalculatorDialog = true
                    }
                )
            }

            // Personalized Goal Calculator Wizard Dialog
            if (showGoalCalculatorDialog) {
                GoalCalculatorWizardDialog(
                    prefsManager = prefsManager,
                    waterRepo = waterRepo,
                    unit = measurementUnit,
                    onDismiss = { showGoalCalculatorDialog = false },
                    onApplyGoal = { calculatedGoal ->
                        currentGoalMl = calculatedGoal
                        waterRepo.saveDailyGoalMl(calculatedGoal)
                        showGoalCalculatorDialog = false
                        snackbarMessage = "Personalized goal set to ${waterRepo.formatVolume(calculatedGoal, measurementUnit)}! 🎯"
                    }
                )
            }

            // Add Custom Container Dialog
            if (showAddContainerDialog) {
                AddCustomContainerDialog(
                    unit = measurementUnit,
                    waterRepo = waterRepo,
                    onDismiss = { showAddContainerDialog = false },
                    onSave = { name, amountMl, icon ->
                        showAddContainerDialog = false
                        scope.launch {
                            try {
                                waterRepo.addCustomContainer(name, amountMl, icon)
                                snackbarMessage = "Container \"$name\" saved! 🧴"
                            } catch (e: Exception) {
                                snackbarMessage = e.message ?: "Failed to save container"
                            }
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. DASHBOARD TAB CONTENT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardTabContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    skyBlue: Color,
    deepBlue: Color,
    emerald: Color,
    measurementUnit: String,
    waterRepo: WaterTrackerRepository,
    todayTotalMl: Int,
    todayPlainWaterMl: Int,
    goalMl: Int,
    remainingMl: Int,
    progressFraction: Float,
    progressPct: Int,
    totalDrinksCount: Int,
    selectedDrinkType: DrinkType,
    onDrinkTypeSelected: (DrinkType) -> Unit,
    customContainers: List<CustomWaterContainer>,
    onQuickAdd: (Int, String) -> Unit,
    onCustomAmountClick: () -> Unit,
    drinkEntries: List<WaterIntakeEntry>,
    onEditEntry: (WaterIntakeEntry) -> Unit,
    onDeleteEntry: (WaterIntakeEntry) -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val nextReminderStr = remember(context) {
        WaterReminderScheduler.getNextReminderTimeString(context)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Animated Circular Water Wave Progress Indicator ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(205.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularWaterWaveCanvas(
                            fillFraction = progressFraction.coerceIn(0f, 1f),
                            waveColor = Color(0xFF0284C7),
                            highlightColor = Color(0xFF38BDF8),
                            isDark = isDark
                        )

                        // Center stats typography
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$progressPct%",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = if (progressFraction > 0.45f) Color.White else textPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${waterRepo.formatVolume(todayTotalMl, measurementUnit)} / ${waterRepo.formatVolume(goalMl, measurementUnit)}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (progressFraction > 0.45f) Color(0xFFE0F2FE) else textSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (progressFraction > 0.45f) Color(0x66000000) else (if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = if (todayTotalMl >= goalMl) "🌟 GOAL MET!" else "${waterRepo.formatVolume(remainingMl, measurementUnit)} left",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (todayTotalMl >= goalMl) Color(0xFF10B981) else (if (progressFraction > 0.45f) Color(0xFFBAE6FD) else deepBlue),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 2. Stat Pillars (Consumed, Remaining, Drinks, Next Reminder) ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPillarCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    label = "Consumed",
                    value = waterRepo.formatVolume(todayTotalMl, measurementUnit),
                    icon = "💧",
                    color = skyBlue
                )
                StatPillarCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    label = "Remaining",
                    value = waterRepo.formatVolume(remainingMl, measurementUnit),
                    icon = "⏳",
                    color = if (remainingMl == 0) emerald else Color(0xFFF59E0B)
                )
                StatPillarCard(
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    border = cardBorder,
                    label = "Drinks",
                    value = "$totalDrinksCount",
                    icon = "🥛",
                    color = Color(0xFF818CF8)
                )
            }
        }

        // Next Reminder Pill banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF0E2238) else Color(0xFFE0F2FE),
                border = BorderStroke(1.dp, skyBlue.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("⏰", fontSize = 18.sp)
                        Column {
                            Text("Next Reminder", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(nextReminderStr, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    TextButton(onClick = onOpenSettings) {
                        Text("Schedule", color = skyBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── 3. Drink Type Selector Chips ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT BEVERAGE",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    if (selectedDrinkType != DrinkType.WATER) {
                        Text(
                            text = "${(selectedDrinkType.hydrationFactor * 100).toInt()}% Hydration",
                            color = Color(selectedDrinkType.colorHex),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(DrinkType.entries) { type ->
                        val isSelected = selectedDrinkType == type
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) Color(type.colorHex) else (if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0)),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color.White.copy(alpha = 0.5f) else Color.Transparent
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onDrinkTypeSelected(type) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(type.emoji, fontSize = 14.sp)
                                Text(
                                    text = type.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) Color.Black else textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 4. Quick Add Containers Grid ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "QUICK ADD WATER",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                // Standard Presets + Custom Button
                val presets = listOf(
                    Triple("Small Glass", 150, "🥛"),
                    Triple("Regular Glass", 250, "🥛"),
                    Triple("Large Glass", 350, "🥛"),
                    Triple("Bottle", 500, "🧴"),
                    Triple("Large Bottle", 750, "🧴")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.take(3).forEach { (label, ml, emoji) ->
                        QuickAddContainerButton(
                            modifier = Modifier.weight(1f),
                            cardBg = cardBg,
                            border = cardBorder,
                            label = label,
                            amountFormatted = waterRepo.formatVolume(ml, measurementUnit),
                            emoji = emoji,
                            skyBlue = skyBlue,
                            onClick = { onQuickAdd(ml, label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.drop(3).forEach { (label, ml, emoji) ->
                        QuickAddContainerButton(
                            modifier = Modifier.weight(1f),
                            cardBg = cardBg,
                            border = cardBorder,
                            label = label,
                            amountFormatted = waterRepo.formatVolume(ml, measurementUnit),
                            emoji = emoji,
                            skyBlue = skyBlue,
                            onClick = { onQuickAdd(ml, label) }
                        )
                    }

                    // + Custom Amount Card Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onCustomAmountClick() },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) Color(0xFF132A44) else Color(0xFFE0F2FE),
                        border = BorderStroke(1.5.dp, skyBlue)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("✨", fontSize = 18.sp)
                            Spacer(Modifier.height(2.dp))
                            Text("Custom", color = skyBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("Any size", color = textSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // ── 5. Today's Drink Logs Timeline ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S DRINK LOGS",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$totalDrinksCount recorded",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (drinkEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💧", fontSize = 42.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "No Water Logged Yet Today",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap any cup or bottle above to add your first drink and get hydrated!",
                            fontSize = 12.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(drinkEntries, key = { it.id }) { entry ->
                DrinkEntryRowCard(
                    entry = entry,
                    unit = measurementUnit,
                    waterRepo = waterRepo,
                    cardBg = cardBg,
                    border = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onEdit = { onEditEntry(entry) },
                    onDelete = { onDeleteEntry(entry) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. HISTORY TAB CONTENT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryTabContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    skyBlue: Color,
    measurementUnit: String,
    waterRepo: WaterTrackerRepository,
    goalMl: Int,
    viewingDate: String,
    onDateSelected: (String) -> Unit,
    entries: List<WaterIntakeEntry>,
    onEditEntry: (WaterIntakeEntry) -> Unit,
    onDeleteEntry: (WaterIntakeEntry) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFmt = remember { SimpleDateFormat("EEE, dd MMMM yyyy", Locale.getDefault()) }

    val totalMlForDate = remember(entries) { entries.sumOf { it.amountMl } }
    val isGoalMet = totalMlForDate >= goalMl && goalMl > 0
    val progressPct = ((totalMlForDate.toFloat() / goalMl.toFloat()) * 100).roundToInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Date Selector Bar
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous Day
                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                time = sdf.parse(viewingDate) ?: Date()
                                add(Calendar.DAY_OF_YEAR, -1)
                            }
                            onDateSelected(sdf.format(cal.time))
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous Day", tint = textPrimary)
                    }

                    // Date Label & Picker
                    val parsedDate = sdf.parse(viewingDate) ?: Date()
                    val dateLabel = displayFmt.format(parsedDate)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val cal = Calendar.getInstance().apply { time = parsedDate }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val c = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth)
                                        }
                                        onDateSelected(sdf.format(c.time))
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (viewingDate == waterRepo.todayDateString()) "TODAY" else dateLabel.split(",").first().uppercase(),
                            color = skyBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = dateLabel,
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Next Day
                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                time = sdf.parse(viewingDate) ?: Date()
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                            onDateSelected(sdf.format(cal.time))
                        }
                    ) {
                        Icon(Icons.Rounded.ArrowForward, contentDescription = "Next Day", tint = textPrimary)
                    }
                }
            }
        }

        // Selected Date Summary Banner
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = waterRepo.formatVolume(totalMlForDate, measurementUnit),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = skyBlue
                    )
                    Text(
                        text = "Goal: ${waterRepo.formatVolume(goalMl, measurementUnit)} ($progressPct%)",
                        color = textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isGoalMet) Color(0xFF10B981).copy(alpha = 0.2f) else (if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = if (isGoalMet) "🌟 Daily Goal Achieved!" else "${waterRepo.formatVolume((goalMl - totalMlForDate).coerceAtLeast(0), measurementUnit)} Remaining",
                            color = if (isGoalMet) Color(0xFF10B981) else textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "LOGGED DRINKS (${entries.size})",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        if (entries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📭", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No entries recorded on this day.", fontSize = 14.sp, color = textSecondary)
                    }
                }
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                DrinkEntryRowCard(
                    entry = entry,
                    unit = measurementUnit,
                    waterRepo = waterRepo,
                    cardBg = cardBg,
                    border = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onEdit = { onEditEntry(entry) },
                    onDelete = { onDeleteEntry(entry) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. ANALYTICS TAB CONTENT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsTabContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    skyBlue: Color,
    measurementUnit: String,
    waterRepo: WaterTrackerRepository,
    goalMl: Int
) {
    var timeframe by remember { mutableStateOf("WEEKLY") } // DAILY, WEEKLY, MONTHLY
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Load past 7 days records
    val cal7 = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
    }
    val startDate7 = remember { sdf.format(cal7.time) }
    val todayDate = remember { waterRepo.todayDateString() }

    val weeklyEntries by waterRepo.getEntriesBetweenDates(startDate7, todayDate).collectAsState(initial = emptyList())

    // Weekly calculations
    val dayFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val weeklyDayStats = remember(weeklyEntries, todayDate) {
        (6 downTo 0).map { daysAgo ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val dStr = sdf.format(c.time)
            val dLabel = dayFmt.format(c.time)
            val sum = weeklyEntries.filter { it.date == dStr }.sumOf { it.amountMl }
            Triple(dLabel, sum, dStr == todayDate)
        }
    }

    val weeklyTotalMl = remember(weeklyDayStats) { weeklyDayStats.sumOf { it.second } }
    val weeklyAvgDailyMl = (weeklyTotalMl / 7)
    val bestDay = remember(weeklyDayStats) { weeklyDayStats.maxByOrNull { it.second } ?: Triple("-", 0, false) }
    val lowestDay = remember(weeklyDayStats) { weeklyDayStats.minByOrNull { it.second } ?: Triple("-", 0, false) }
    val daysGoalAchieved = remember(weeklyDayStats, goalMl) { weeklyDayStats.count { it.second >= goalMl } }
    val consistencyRatePct = ((daysGoalAchieved / 7f) * 100).roundToInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeframe Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF132034) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                listOf("DAILY", "WEEKLY", "MONTHLY").forEach { tf ->
                    val isSel = timeframe == tf
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) skyBlue else Color.Transparent)
                            .clickable { timeframe = tf }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                            color = if (isSel) Color.Black else textSecondary
                        )
                    }
                }
            }
        }

        when (timeframe) {
            "WEEKLY" -> {
                // Mon to Sun Bar Chart Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = cardBorder
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("7-DAY HYDRATION TREND", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    Text("Daily Volume (ml)", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Goal: ${waterRepo.formatVolume(goalMl, measurementUnit)}", color = skyBlue, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.height(18.dp))

                            // Interactive Bar Chart Canvas
                            val maxBar = maxOf(goalMl * 1.25f, (weeklyDayStats.maxOfOrNull { it.second } ?: 1).toFloat()).coerceAtLeast(1000f)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                weeklyDayStats.forEach { (label, ml, isToday) ->
                                    val barFraction = (ml / maxBar).coerceIn(0.04f, 1f)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (ml > 0) "${(ml / 100) * 100}" else "0",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isToday) skyBlue else textSecondary
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(22.dp)
                                                .fillMaxHeight(barFraction)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(
                                                    if (ml >= goalMl) Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))
                                                    else Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF475569)))
                                                )
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = label.take(3),
                                            fontSize = 10.sp,
                                            fontWeight = if (isToday) FontWeight.Black else FontWeight.SemiBold,
                                            color = if (isToday) skyBlue else textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Metric Cards: Best Day, Lowest Day, Average Daily
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatPillarCard(
                            modifier = Modifier.weight(1f),
                            cardBg = cardBg,
                            border = cardBorder,
                            label = "Daily Average",
                            value = waterRepo.formatVolume(weeklyAvgDailyMl, measurementUnit),
                            icon = "📊",
                            color = skyBlue
                        )
                        StatPillarCard(
                            modifier = Modifier.weight(1f),
                            cardBg = cardBg,
                            border = cardBorder,
                            label = "Best Day",
                            value = "${bestDay.first} (${waterRepo.formatVolume(bestDay.second, measurementUnit)})",
                            icon = "🏆",
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Consistency & Completion Rate Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = cardBorder
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Goal Achievement Rate", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)
                                Text("$daysGoalAchieved of 7 Days ($consistencyRatePct%)", fontWeight = FontWeight.Black, color = skyBlue, fontSize = 13.sp)
                            }
                            LinearProgressIndicator(
                                progress = { (daysGoalAchieved / 7f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = Color(0xFF10B981),
                                trackColor = if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            "DAILY" -> {
                // Drink Type Breakdown
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = cardBorder
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("BEVERAGE BREAKDOWN (TODAY)", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black)

                            val typeMap = remember(weeklyEntries, todayDate) {
                                val todayList = weeklyEntries.filter { it.date == todayDate }
                                DrinkType.entries.map { dt ->
                                    val total = todayList.filter { it.drinkType == dt.code }.sumOf { it.amountMl }
                                    Pair(dt, total)
                                }.filter { it.second > 0 }
                            }

                            if (typeMap.isEmpty()) {
                                Text("No drinks logged today yet to calculate breakdown.", color = textSecondary, fontSize = 12.sp)
                            } else {
                                typeMap.forEach { (type, ml) ->
                                    val pct = ((ml.toFloat() / (weeklyDayStats.lastOrNull()?.second ?: 1).toFloat()) * 100).roundToInt()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(type.emoji, fontSize = 16.sp)
                                            Text(type.displayName, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("${waterRepo.formatVolume(ml, measurementUnit)} ($pct%)", color = skyBlue, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "MONTHLY" -> {
                // Monthly Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = cardBorder
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("30-DAY HYDRATION OVERVIEW", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text(
                                text = "Keep logging daily to build an unbreakable hydration streak and track deep monthly cellular recovery.",
                                color = textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. CONTAINERS TAB CONTENT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContainersTabContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    skyBlue: Color,
    measurementUnit: String,
    waterRepo: WaterTrackerRepository,
    containers: List<CustomWaterContainer>,
    onAddContainerClick: () -> Unit,
    onQuickAdd: (Int, String) -> Unit
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner with Create Container CTA
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Custom Drinking Vessels", color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Save your favorite bottles, shakers & mugs for 1-tap logging.",
                            color = textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Button(
                        onClick = onAddContainerClick,
                        colors = ButtonDefaults.buttonColors(containerColor = skyBlue, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ New", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "SAVED CONTAINERS (${containers.size})",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        items(containers, key = { it.id }) { container ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF16253B) else Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(container.icon, fontSize = 22.sp)
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = container.name,
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = waterRepo.formatVolume(container.amountMl, measurementUnit),
                                color = skyBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Quick 1-tap Drink
                        Button(
                            onClick = { onQuickAdd(container.amountMl, container.name) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF16253B) else Color(0xFFE0F2FE), contentColor = skyBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Drink 💧", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (!container.isDefault) {
                            IconButton(
                                onClick = { scope.launch { waterRepo.deleteCustomContainer(container) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. ACHIEVEMENTS TAB CONTENT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AchievementsTabContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: BorderStroke,
    skyBlue: Color,
    prefsManager: SharedPrefsManager,
    waterRepo: WaterTrackerRepository,
    todayTotalMl: Int,
    goalMl: Int
) {
    val currentStreak = prefsManager.getWaterStreak()
    val bestStreak = prefsManager.getWaterBestStreak()

    val achievements = listOf(
        AchievementItem("FIRST_DROP", "First Drop", "Complete your first water entry", "💧", prefsManager.isWaterAchievementUnlocked("FIRST_DROP"), 1f),
        AchievementItem("STARTER_50", "Hydration Starter", "Reach 50% of your daily goal", "⚡", prefsManager.isWaterAchievementUnlocked("STARTER_50"), (todayTotalMl / (goalMl * 0.5f)).coerceIn(0f, 1f)),
        AchievementItem("HERO_100", "Hydration Hero", "Reach 100% of your daily goal", "🏆", prefsManager.isWaterAchievementUnlocked("HERO_100"), (todayTotalMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)),
        AchievementItem("STREAK_3", "3-Day Streak", "Achieve your goal for 3 consecutive days", "🔥", prefsManager.isWaterAchievementUnlocked("STREAK_3"), (currentStreak / 3f).coerceIn(0f, 1f)),
        AchievementItem("STREAK_7", "7-Day Streak", "Achieve your goal for 7 consecutive days", "🛡️", prefsManager.isWaterAchievementUnlocked("STREAK_7"), (currentStreak / 7f).coerceIn(0f, 1f)),
        AchievementItem("STREAK_30", "Hydration Master", "Achieve your goal for 30 consecutive days", "👑", prefsManager.isWaterAchievementUnlocked("STREAK_30"), (currentStreak / 30f).coerceIn(0f, 1f))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Streak Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🔥", fontSize = 24.sp)
                            Text("$currentStreak Days", fontSize = 26.sp, fontWeight = FontWeight.Black, color = textPrimary)
                        }
                        Text("Current Streak", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("👑", fontSize = 20.sp)
                            Text("$bestStreak Days", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF59E0B))
                        }
                        Text("Personal Record", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "HYDRATION MILESTONES (${achievements.count { it.isUnlocked }}/${achievements.size} UNLOCKED)",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        items(achievements, key = { it.id }) { item ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                border = if (item.isUnlocked) BorderStroke(1.dp, skyBlue.copy(alpha = 0.5f)) else cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (item.isUnlocked) Color(0xFF0284C7).copy(alpha = 0.2f) else (if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.icon, fontSize = 24.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.title, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (item.isUnlocked) Color(0xFF10B981).copy(alpha = 0.2f) else (if (isDark) Color(0xFF16253B) else Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = if (item.isUnlocked) "UNLOCKED" else "LOCKED",
                                    color = if (item.isUnlocked) Color(0xFF10B981) else textSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(item.description, color = textSecondary, fontSize = 11.5.sp)
                    }
                }
            }
        }
    }
}

private data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progress: Float
)

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS & CANVAS WAVE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CircularWaterWaveCanvas(
    fillFraction: Float,
    waveColor: Color,
    highlightColor: Color,
    isDark: Boolean
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val animatedFill by animateFloatAsState(
        targetValue = fillFraction,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "fill"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val diameter = size.minDimension
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Background Circle
        drawCircle(
            color = if (isDark) Color(0xFF0F2038) else Color(0xFFE0F2FE),
            radius = radius,
            center = center
        )

        // Clip circle path for wave
        val circlePath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
        }

        clipPath(circlePath) {
            val baseWaterY = size.height * (1f - animatedFill)
            val waveHeight = (size.height * 0.04f).coerceAtMost(20f)

            // Wave Path
            val wavePath = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, baseWaterY)
                val step = 10f
                var x = 0f
                while (x <= size.width) {
                    val y = baseWaterY + waveHeight * sin((x / size.width * 2 * PI + wavePhase).toFloat())
                    lineTo(x, y)
                    x += step
                }
                lineTo(size.width, size.height)
                close()
            }

            // Draw wave gradient
            drawPath(
                path = wavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(highlightColor, waveColor, Color(0xFF0C4A6E)),
                    startY = baseWaterY - 10f,
                    endY = size.height
                )
            )
        }

        // Circular glow ring outline
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(highlightColor, waveColor, highlightColor)
            ),
            radius = radius - 2f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun StatPillarCard(
    modifier: Modifier = Modifier,
    cardBg: Color,
    border: BorderStroke,
    label: String,
    value: String,
    icon: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickAddContainerButton(
    modifier: Modifier = Modifier,
    cardBg: Color,
    border: BorderStroke,
    label: String,
    amountFormatted: String,
    emoji: String,
    skyBlue: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        shadowElevation = 4.dp,
        border = border
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(3.dp))
            Text(amountFormatted, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = skyBlue)
            Text(label, fontSize = 9.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DrinkEntryRowCard(
    entry: WaterIntakeEntry,
    unit: String,
    waterRepo: WaterTrackerRepository,
    cardBg: Color,
    border: BorderStroke,
    textPrimary: Color,
    textSecondary: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val drinkType = DrinkType.fromCode(entry.drinkType)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(drinkType.colorHex).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(drinkType.emoji, fontSize = 18.sp)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = waterRepo.formatVolume(entry.amountMl, unit),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                        Text(
                            text = "• ${entry.containerName}",
                            fontSize = 12.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(entry.time, fontSize = 11.sp, color = textSecondary)
                        if (entry.note.isNotBlank()) {
                            Text("— \"${entry.note}\"", fontSize = 11.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = textSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOGS: CUSTOM ADD, EDIT, SETTINGS & GOAL CALCULATOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CustomWaterAddDialog(
    initialDrinkType: DrinkType,
    unit: String,
    waterRepo: WaterTrackerRepository,
    onDismiss: () -> Unit,
    onConfirm: (Int, DrinkType, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("250") }
    var selectedType by remember { mutableStateOf(initialDrinkType) }
    var containerName by remember { mutableStateOf("Glass") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Log Custom Drink 💧", fontSize = 18.sp, fontWeight = FontWeight.Black)

                // Amount Text Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Volume ($unit)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Beverage Type Dropdown / Chips
                Text("Beverage Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DrinkType.entries) { type ->
                        val isSel = selectedType == type
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedType = type },
                            label = { Text("${type.emoji} ${type.displayName}", fontSize = 11.sp) }
                        )
                    }
                }

                // Optional Container Name
                OutlinedTextField(
                    value = containerName,
                    onValueChange = { containerName = it },
                    label = { Text("Container (e.g. Shaker, Mug)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional Note (e.g. Post-Run)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rawAmount = amountText.toIntOrNull() ?: 0
                            if (rawAmount <= 0) {
                                errorMessage = "Please enter an amount greater than 0"
                                return@Button
                            }
                            val finalMl = if (unit == "oz") waterRepo.ozToMl(rawAmount.toFloat()) else rawAmount
                            onConfirm(finalMl, selectedType, containerName.ifBlank { "Glass" }, note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Log Drink", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditWaterEntryDialog(
    entry: WaterIntakeEntry,
    unit: String,
    waterRepo: WaterTrackerRepository,
    onDismiss: () -> Unit,
    onSave: (WaterIntakeEntry) -> Unit
) {
    val initialAmount = if (unit == "oz") waterRepo.mlToOz(entry.amountMl).roundToInt().toString() else entry.amountMl.toString()
    var amountText by remember { mutableStateOf(initialAmount) }
    var selectedType by remember { mutableStateOf(DrinkType.fromCode(entry.drinkType)) }
    var containerName by remember { mutableStateOf(entry.containerName) }
    var note by remember { mutableStateOf(entry.note) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Edit Drink Entry", fontSize = 18.sp, fontWeight = FontWeight.Black)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Volume ($unit)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Drink Type", fontSize = 12.sp, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DrinkType.entries) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text("${type.emoji} ${type.displayName}", fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = containerName,
                    onValueChange = { containerName = it },
                    label = { Text("Container") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val raw = amountText.toIntOrNull() ?: 0
                            if (raw <= 0) {
                                errorMessage = "Amount must be greater than 0"
                                return@Button
                            }
                            val finalMl = if (unit == "oz") waterRepo.ozToMl(raw.toFloat()) else raw
                            onSave(
                                entry.copy(
                                    amountMl = finalMl,
                                    drinkType = selectedType.code,
                                    containerName = containerName,
                                    note = note,
                                    hydrationFactor = selectedType.hydrationFactor
                                )
                            )
                        }
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterSettingsDialog(
    prefsManager: SharedPrefsManager,
    currentGoalMl: Int,
    unit: String,
    waterRepo: WaterTrackerRepository,
    onDismiss: () -> Unit,
    onGoalChanged: (Int) -> Unit,
    onUnitChanged: (String) -> Unit,
    onOpenCalculator: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var reminderEnabled by remember { mutableStateOf(prefsManager.isWaterReminderEnabled()) }
    var reminderInterval by remember { mutableIntStateOf(prefsManager.getWaterReminderIntervalMinutes()) }
    var soundEnabled by remember { mutableStateOf(prefsManager.isWaterReminderSound()) }
    var dndEnabled by remember { mutableStateOf(prefsManager.isWaterDndDuringSleep()) }

    var goalMlState by remember { mutableIntStateOf(currentGoalMl) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("Hydration Settings ⚙️", fontSize = 19.sp, fontWeight = FontWeight.Black)
                }

                // Daily Goal Adjuster
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Daily Goal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { if (goalMlState > 500) goalMlState -= 100 }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease Goal")
                            }
                            Text(
                                text = waterRepo.formatVolume(goalMlState, unit),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0284C7)
                            )
                            IconButton(onClick = { if (goalMlState < 6000) goalMlState += 100 }) {
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase Goal")
                            }
                        }

                        // Calculator Wizard button
                        OutlinedButton(
                            onClick = onOpenCalculator,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🎯 Calculate Personalized Goal")
                        }
                    }
                }

                // Measurement Unit
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Measurement Unit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = unit == "ml",
                                onClick = { onUnitChanged("ml") },
                                label = { Text("ml") }
                            )
                            FilterChip(
                                selected = unit == "oz",
                                onClick = { onUnitChanged("oz") },
                                label = { Text("fl oz") }
                            )
                        }
                    }
                }

                // Reminder Toggles
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hydration Reminders", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = {
                                    reminderEnabled = it
                                    prefsManager.setWaterReminderEnabled(it)
                                    if (it) WaterReminderScheduler.schedule(context) else WaterReminderScheduler.cancel(context)
                                }
                            )
                        }

                        if (reminderEnabled) {
                            // Reminder interval selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Interval", fontSize = 12.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(30, 45, 60, 90, 120).forEach { mins ->
                                        FilterChip(
                                            selected = reminderInterval == mins,
                                            onClick = {
                                                reminderInterval = mins
                                                prefsManager.setWaterReminderIntervalMinutes(mins)
                                                WaterReminderScheduler.schedule(context)
                                            },
                                            label = { Text("${mins}m", fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }

                            // Do not disturb during sleep switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pause during sleep (DND)", fontSize = 12.sp)
                                Switch(
                                    checked = dndEnabled,
                                    onCheckedChange = {
                                        dndEnabled = it
                                        prefsManager.setWaterDndDuringSleep(it)
                                    }
                                )
                            }

                            // Notification sound switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Play sound on reminder", fontSize = 12.sp)
                                Switch(
                                    checked = soundEnabled,
                                    onCheckedChange = {
                                        soundEnabled = it
                                        prefsManager.setWaterReminderSound(it)
                                    }
                                )
                            }

                            // Test notification button
                            TextButton(
                                onClick = { WaterReminderScheduler.sendTestNotification(context) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🔔 Send Test Reminder Now")
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onGoalChanged(goalMlState)
                                onDismiss()
                            }
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCalculatorWizardDialog(
    prefsManager: SharedPrefsManager,
    waterRepo: WaterTrackerRepository,
    unit: String,
    onDismiss: () -> Unit,
    onApplyGoal: (Int) -> Unit
) {
    var weightText by remember { mutableStateOf(prefsManager.getWaterProfileWeight().toInt().toString()) }
    var activityLevel by remember { mutableStateOf(prefsManager.getWaterProfileActivity()) }
    var workoutMins by remember { mutableIntStateOf(prefsManager.getWaterProfileWorkoutMins()) }
    var gender by remember { mutableStateOf(prefsManager.getWaterProfileGender()) }

    val calculatedMl = remember(weightText, activityLevel, workoutMins, gender) {
        val w = weightText.toFloatOrNull() ?: 70f
        waterRepo.calculateSuggestedGoal(w, activityLevel, workoutMins, gender)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("Personalized Hydration Estimate 🎯", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = "Estimate your baseline hydration needs based on your body weight, daily movement, and athletic workouts.",
                        fontSize = 11.5.sp,
                        color = Color.Gray
                    )
                }

                // Weight Input
                item {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { c -> c.isDigit() } },
                        label = { Text("Body Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Activity Level Chips
                item {
                    Text("Daily Activity Level", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Sedentary", "Light", "Moderate", "High", "Athlete")) { level ->
                            FilterChip(
                                selected = activityLevel == level,
                                onClick = { activityLevel = level },
                                label = { Text(level, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Workout Duration Slider
                item {
                    Text("Daily Workout: $workoutMins min/day", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = workoutMins.toFloat(),
                        onValueChange = { workoutMins = it.roundToInt() },
                        valueRange = 0f..120f,
                        steps = 7
                    )
                }

                // Result Card
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF0284C7))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SUGGESTED DAILY GOAL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF0284C7))
                            Text(
                                text = waterRepo.formatVolume(calculatedMl, unit),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0284C7)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Note: This is an estimated wellness guideline, not a medical prescription. Adjust according to your personal comfort.",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val w = weightText.toFloatOrNull() ?: 70f
                                prefsManager.saveWaterProfile(w, activityLevel, workoutMins, gender)
                                onApplyGoal(calculatedMl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("Apply Goal", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomContainerDialog(
    unit: String,
    waterRepo: WaterTrackerRepository,
    onDismiss: () -> Unit,
    onSave: (String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("300") }
    var selectedEmoji by remember { mutableStateOf("🥛") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val emojis = listOf("🥛", "🧴", "🥤", "☕", "🧃", "🥥", "🍶", "🍵")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Custom Container 🧴", fontSize = 18.sp, fontWeight = FontWeight.Black)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vessel Name (e.g. Gym Shaker)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Volume ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon / Emoji", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojis.forEach { emoji ->
                        val isSel = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSel) Color(0xFF0284C7).copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (isSel) Color(0xFF0284C7) else Color.LightGray, CircleShape)
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val raw = amountText.toIntOrNull() ?: 0
                            if (name.isBlank()) {
                                errorMsg = "Please enter a container name"
                                return@Button
                            }
                            if (raw <= 0) {
                                errorMsg = "Volume must be greater than 0"
                                return@Button
                            }
                            val finalMl = if (unit == "oz") waterRepo.ozToMl(raw.toFloat()) else raw
                            onSave(name, finalMl, selectedEmoji)
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalAchievedDialog(
    todayTotalMl: Int,
    goalMl: Int,
    unit: String,
    waterRepo: WaterTrackerRepository,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("🎉", fontSize = 48.sp)
                Text(
                    text = "Hydration Goal Achieved!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981)
                )
                Text(
                    text = "Fantastic job! You've logged ${waterRepo.formatVolume(todayTotalMl, unit)} of your ${waterRepo.formatVolume(goalMl, unit)} target today. Your cells and muscles are thoroughly replenished!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
