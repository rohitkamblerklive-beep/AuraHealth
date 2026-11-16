package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietTrackerScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    prefsManager: SharedPrefsManager,
    repository: HealthRepository
) {
    val strings = LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.background
    val accentColor = MaterialTheme.colorScheme.primary

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = remember { sdf.format(Date()) }
    val displayDate = remember {
        val sdfDate = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        "Today, " + sdfDate.format(Date())
    }

    // Get last 7 days range
    val cal7ago = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
    }
    val weekStart = remember { sdf.format(cal7ago.time) }

    val loggedMeals by repository.getMealsByDate(today).collectAsState(initial = emptyList())
    val weekMeals by repository.getMealsForDateRange(weekStart, today).collectAsState(initial = emptyList())

    val weight = prefsManager.getWeight()
    val userWeight = if (weight > 0) weight else 70f
    val targetCals = prefsManager.getCalorieGoal()

    val proteinTarget = (userWeight * 1.8f).toInt().coerceAtLeast(50)
    val fatTarget = (userWeight * 0.8f).toInt().coerceAtLeast(30)
    val carbTarget = ((targetCals - (proteinTarget * 4 + fatTarget * 9)) / 4).coerceAtLeast(100)

    val totalCalsConsumed = loggedMeals.sumOf { it.calories }
    val totalProteinConsumed = loggedMeals.sumOf { it.protein }
    val totalCarbsConsumed = loggedMeals.sumOf { it.carbs }
    val totalFatsConsumed = loggedMeals.sumOf { it.fats }

    // Build 7-day calorie data
    val dailyCalData = remember(weekMeals) {
        val map = mutableMapOf<String, Int>()
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        repeat(7) {
            val d = sdf.format(c.time)
            map[d] = 0
            c.add(Calendar.DAY_OF_YEAR, 1)
        }
        weekMeals.forEach { m -> map[m.date] = (map[m.date] ?: 0) + m.calories }
        map.entries.sortedBy { it.key }.map { Pair(it.key, it.value) }
    }

    // Diet score (0-100): percentage of macro goals hit
    val dietScore = remember(totalCalsConsumed, totalProteinConsumed, totalCarbsConsumed, totalFatsConsumed) {
        val cScore = if (targetCals > 0) (totalCalsConsumed.toFloat() / targetCals).coerceIn(0f, 1f) else 0f
        val pScore = if (proteinTarget > 0) (totalProteinConsumed.toFloat() / proteinTarget).coerceIn(0f, 1f) else 0f
        val carbScore = if (carbTarget > 0) (totalCarbsConsumed.toFloat() / carbTarget).coerceIn(0f, 1f) else 0f
        val fScore = if (fatTarget > 0) (totalFatsConsumed.toFloat() / fatTarget).coerceIn(0f, 1f) else 0f
        ((cScore * 0.4f + pScore * 0.3f + carbScore * 0.2f + fScore * 0.1f) * 100).toInt()
    }

    // Smart nutrition insight
    val nutritionInsight = remember(totalCalsConsumed, totalProteinConsumed) {
        val calDiff = targetCals - totalCalsConsumed
        val proteinDiff = proteinTarget - totalProteinConsumed
        when {
            totalCalsConsumed == 0 -> "🌅 Start your day right! Log your first meal to track progress."
            calDiff > 800 -> "🍽️ You still have $calDiff kcal left. Add a protein-rich meal to fuel your day."
            calDiff < -200 -> "⚠️ You've exceeded your calorie goal by ${-calDiff} kcal. Consider lighter snacks."
            proteinDiff > 30 -> "💪 Protein deficit of ${proteinDiff}g. A chicken breast or protein shake will help!"
            totalProteinConsumed >= proteinTarget -> "✅ Protein goal achieved! Great job hitting your macro targets today."
            calDiff in 0..300 -> "🎯 Almost at your calorie goal! You're on track for a perfect nutrition day."
            else -> "📊 Macros looking balanced. Keep logging meals for accurate tracking."
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var dialogMealType by remember { mutableStateOf("Breakfast") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }

    var isScannerPinned by remember { mutableStateOf(prefsManager.isFoodScannerDashboardEnabled()) }
    var isPlannerPinned by remember { mutableStateOf(prefsManager.isMealPlannerDashboardEnabled()) }
    var isYogaPinned by remember { mutableStateOf(prefsManager.isYogaDashboardEnabled()) }
    var isAlgoLabPinned by remember { mutableStateOf(prefsManager.isAlgorithmLabDashboardEnabled()) }

    var waterGlasses by remember { mutableIntStateOf(prefsManager.getWaterGlassesToday()) }
    val waterGoal = prefsManager.getWaterGoal()

    if (showBarcodeScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { barcode ->
                scannedBarcode = barcode
                showBarcodeScanner = false
                showAddDialog = true
            },
            onCancel = {
                showBarcodeScanner = false
                showAddDialog = true
            }
        )
        return
    }

    if (showAddDialog) {
        AddMealDialog(
            defaultMealType = dialogMealType,
            initialBarcode = scannedBarcode,
            onDismiss = { 
                showAddDialog = false
                scannedBarcode = null
            },
            onAdd = { meal ->
                coroutineScope.launch {
                    repository.addMeal(meal.copy(date = today))
                }
                showAddDialog = false
                scannedBarcode = null
            },
            onScanBarcodeClick = {
                showAddDialog = false
                showBarcodeScanner = true
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            strings.nutrition.uppercase(Locale.US),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 1.5.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.12f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    displayDate,
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            dialogMealType = "Snack"
                            showBarcodeScanner = true
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { onNavigate("CALORIE_GOAL") },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Calorie Settings",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        bottomBar = {
            AdBanner(adKey = "ad_diet_banner")
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Macro Summary (existing enhanced) ──────────────────────────────
            val stepsToday = prefsManager.getStepsToday()
            val burnedCals = (stepsToday * 0.04f).toInt()
            
            item {
                MacroSummaryCard(
                    consumed = totalCalsConsumed,
                    burned = burnedCals,
                    total = targetCals,
                    proteinConsumed = totalProteinConsumed,
                    proteinTarget = proteinTarget,
                    carbsConsumed = totalCarbsConsumed,
                    carbsTarget = carbTarget,
                    fatsConsumed = totalFatsConsumed,
                    fatsTarget = fatTarget,
                    accentColor = accentColor
                )
            }

            // ── Diet Score + Macro Donut ────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Diet Score Card
                    DietScoreCard(
                        score = dietScore,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    // Macro Donut
                    MacroDonutCard(
                        protein = totalProteinConsumed,
                        carbs = totalCarbsConsumed,
                        fats = totalFatsConsumed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Smart Nutrition Insight ────────────────────────────────────────
            item {
                NutritionInsightCard(
                    insight = nutritionInsight,
                    accentColor = accentColor
                )
            }

            // ── Smart Nutrition Suite ───────────────────────────────────────────
            item {
                SectionTitle("SMART NUTRITION SUITE", accentColor)
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Food Scanner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .padding(8.dp)
                    ) {
                        AiFoodScannerBannerCard(onClick = { onNavigate("AI_FOOD_SCANNER") })
                        Spacer(Modifier.height(4.dp))
                        DashboardVisibilityToggle(
                            label = "Food Scanner",
                            checked = isScannerPinned,
                            onCheckedChange = { pinned ->
                                isScannerPinned = pinned
                                prefsManager.setFoodScannerDashboardEnabled(pinned)
                            },
                            accentColor = accentColor
                        )
                    }

                    // 2. Meal Planner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .padding(8.dp)
                    ) {
                        AiMealPlannerBannerCard(onClick = { onNavigate("AI_MEAL_PLANNER") })
                        Spacer(Modifier.height(4.dp))
                        DashboardVisibilityToggle(
                            label = "Meal Planner",
                            checked = isPlannerPinned,
                            onCheckedChange = { pinned ->
                                isPlannerPinned = pinned
                                prefsManager.setMealPlannerDashboardEnabled(pinned)
                            },
                            accentColor = accentColor
                        )
                    }

                    // 3. Yoga Guide
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .padding(8.dp)
                    ) {
                        AiYogaBannerCard(onClick = { onNavigate("YOGA") })
                        Spacer(Modifier.height(4.dp))
                        DashboardVisibilityToggle(
                            label = "Yoga Guide",
                            checked = isYogaPinned,
                            onCheckedChange = { pinned ->
                                isYogaPinned = pinned
                                prefsManager.setYogaDashboardEnabled(pinned)
                            },
                            accentColor = accentColor
                        )
                    }

                    // 4. Health Lab
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .padding(8.dp)
                    ) {
                        AiAlgorithmBannerCard(onClick = { onNavigate("ALGORITHM_LAB") })
                        Spacer(Modifier.height(4.dp))
                        DashboardVisibilityToggle(
                            label = "Health Lab",
                            checked = isAlgoLabPinned,
                            onCheckedChange = { pinned ->
                                isAlgoLabPinned = pinned
                                prefsManager.setAlgorithmLabDashboardEnabled(pinned)
                            },
                            accentColor = accentColor
                        )
                    }
                }
            }

            // ── 7-Day Calorie Trend ────────────────────────────────────────────
            item {
                SectionTitle("7-DAY CALORIE TREND", accentColor)
            }
            item {
                CalorieTrendChart(
                    dailyData = dailyCalData,
                    goalCals = targetCals,
                    accentColor = accentColor
                )
            }



            // ── Daily Meals ────────────────────────────────────────────────────
            item {
                SectionTitle("DAILY MEALS", accentColor)
            }

            // Breakfast Section
            item {
                val breakfastList = loggedMeals.filter { it.mealType.equals("Breakfast", ignoreCase = true) }
                MealCategoryCard(
                    title = "Breakfast",
                    icon = Icons.Rounded.FreeBreakfast,
                    meals = breakfastList,
                    color = accentColor,
                    onAddClick = {
                        dialogMealType = "Breakfast"
                        showAddDialog = true
                    },
                    onDeleteClick = { meal ->
                        coroutineScope.launch { repository.deleteMeal(meal) }
                    }
                )
            }

            // Lunch Section
            item {
                val lunchList = loggedMeals.filter { it.mealType.equals("Lunch", ignoreCase = true) }
                MealCategoryCard(
                    title = "Lunch",
                    icon = Icons.Rounded.LunchDining,
                    meals = lunchList,
                    color = accentColor,
                    onAddClick = {
                        dialogMealType = "Lunch"
                        showAddDialog = true
                    },
                    onDeleteClick = { meal ->
                        coroutineScope.launch { repository.deleteMeal(meal) }
                    }
                )
            }

            // Snack Section
            item {
                val snackList = loggedMeals.filter { it.mealType.equals("Snack", ignoreCase = true) }
                MealCategoryCard(
                    title = "Snack",
                    icon = Icons.Rounded.Fastfood,
                    meals = snackList,
                    color = accentColor,
                    onAddClick = {
                        dialogMealType = "Snack"
                        showAddDialog = true
                    },
                    onDeleteClick = { meal ->
                        coroutineScope.launch { repository.deleteMeal(meal) }
                    }
                )
            }

            // Dinner Section
            item {
                val dinnerList = loggedMeals.filter { it.mealType.equals("Dinner", ignoreCase = true) }
                MealCategoryCard(
                    title = "Dinner",
                    icon = Icons.Rounded.DinnerDining,
                    meals = dinnerList,
                    color = accentColor,
                    onAddClick = {
                        dialogMealType = "Dinner"
                        showAddDialog = true
                    },
                    onDeleteClick = { meal ->
                        coroutineScope.launch { repository.deleteMeal(meal) }
                    }
                )
            }

            item {
                SectionTitle("QUICK LOGGING", accentColor)
            }

            item {
                WaterLogCard(
                    glasses = waterGlasses,
                    goal = waterGoal,
                    onAddWater = {
                        coroutineScope.launch {
                            val nextGlasses = waterGlasses + 1
                            repository.updateWater(today, nextGlasses)
                            waterGlasses = nextGlasses
                        }
                    }
                )
            }

            // ── Weekly Nutrition Summary ───────────────────────────────────────
            item {
                SectionTitle("WEEKLY SUMMARY", accentColor)
            }
            item {
                WeeklyNutritionSummary(
                    weekMeals = weekMeals,
                    targetCals = targetCals,
                    accentColor = accentColor
                )
            }

            // Native Ad Placement (Clean bottom flow placement)
            item {
                Spacer(Modifier.height(8.dp))
                AdNative(
                    adKey = "ad_diet_native",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ─── Diet Score Card ──────────────────────────────────────────────────────────

@Composable
fun DietScoreCard(score: Int, accentColor: Color, modifier: Modifier = Modifier) {
    val animScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label = "diet_score"
    )
    val scoreColor = accentColor
    val scoreLabel = when {
        score >= 80 -> "Excellent"
        score >= 55 -> "Good"
        score >= 30 -> "Fair"
        else -> "Start Eating!"
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Diet Score", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val inset = stroke / 2
                    drawArc(
                        color = trackColor,
                        startAngle = -90f, sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(scoreColor.copy(alpha = 0.6f), scoreColor)),
                        startAngle = -90f,
                        sweepAngle = (animScore / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${animScore.toInt()}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("/100", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                scoreLabel,
                color = scoreColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Macro Donut Chart ────────────────────────────────────────────────────────

@Composable
fun MacroDonutCard(protein: Int, carbs: Int, fats: Int, modifier: Modifier = Modifier) {
    val total = (protein * 4 + carbs * 4 + fats * 9).toFloat().coerceAtLeast(1f)
    val proteinPct = (protein * 4 / total)
    val carbsPct = (carbs * 4 / total)
    val fatsPct = (fats * 9 / total)

    val animProgress by animateFloatAsState(
        targetValue = if (total > 1f) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "donut_anim"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val proteinColor = primaryColor
    val carbsColor = primaryColor.copy(alpha = 0.65f)
    val fatsColor = primaryColor.copy(alpha = 0.35f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Macro Split", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    val inset = stroke / 2
                    val rect = Size(size.width - stroke, size.height - stroke)

                    if (total <= 1f) {
                        drawArc(
                            color = trackColor,
                            startAngle = -90f, sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Butt),
                            topLeft = Offset(inset, inset), size = rect
                        )
                    } else {
                        var startAngle = -90f
                        listOf(
                            proteinPct to proteinColor,
                            carbsPct to carbsColor,
                            fatsPct to fatsColor
                        ).forEach { (pct, color) ->
                            val sweep = pct * 360f * animProgress
                            if (sweep > 0f) {
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                                    topLeft = Offset(inset, inset), size = rect
                                )
                                startAngle += sweep
                            }
                        }
                    }
                }
                Text(
                    if (total > 1f) "${(proteinPct * 100).toInt()}P" else "—",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "P" to proteinColor,
                    "C" to carbsColor,
                    "F" to fatsColor
                ).forEach { (label, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ─── Nutrition Insight Card ───────────────────────────────────────────────────

@Composable
fun NutritionInsightCard(insight: String, accentColor: Color) {
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }
    val glowColor = if (isDark) Color(0xFF00E5FF) else accentColor

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = glowColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, glowColor.copy(alpha = 0.25f)),
        shadowElevation = if (isDark) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(glowColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = glowColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SMART NUTRITION INSIGHT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = glowColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    insight,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── 7-Day Calorie Trend Chart ────────────────────────────────────────────────

@Composable
fun CalorieTrendChart(
    dailyData: List<Pair<String, Int>>,
    goalCals: Int,
    accentColor: Color
) {
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label = "trend_anim"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dayLabels = remember(dailyData) {
        val dayNames = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
        dailyData.map { (dateStr, _) ->
            try {
                val d = sdf.parse(dateStr)
                val cal = Calendar.getInstance().apply { if (d != null) time = d }
                dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            } catch (e: Exception) { "?" }
        }
    }

    val maxCal = (dailyData.maxOfOrNull { it.second } ?: 0).coerceAtLeast(goalCals)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Calorie Trend", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(trackColor, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Goal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.size(8.dp).background(accentColor, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Intake", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val barCount = dailyData.size
                if (barCount == 0) return@Canvas

                val groupWidth = chartWidth / barCount
                val barWidth = groupWidth * 0.45f
                val gap = (groupWidth - barWidth) / 2f

                // Draw goal line
                val goalY = chartHeight - (goalCals.toFloat() / maxCal.toFloat()) * chartHeight * animProgress
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, goalY),
                    end = Offset(chartWidth, goalY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                // Draw bars
                dailyData.forEachIndexed { i, (_, cals) ->
                    val barH = (cals.toFloat() / maxCal.toFloat()) * chartHeight * animProgress
                    val x = i * groupWidth + gap
                    val top = chartHeight - barH
                    val color = accentColor
                    // Bar background (empty)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                    // Filled bar
                    if (barH > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(color, color.copy(alpha = 0.6f)),
                                startY = top,
                                endY = chartHeight
                            ),
                            topLeft = Offset(x, top),
                            size = Size(barWidth, barH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day labels + calorie values
            Row(modifier = Modifier.fillMaxWidth()) {
                dailyData.forEachIndexed { i, (_, cals) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${if (cals > 0) cals else "-"}",
                            color = if (cals >= goalCals) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            dayLabels.getOrElse(i) { "?" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Weekly Nutrition Summary ─────────────────────────────────────────────────

@Composable
fun WeeklyNutritionSummary(
    weekMeals: List<MealRecord>,
    targetCals: Int,
    accentColor: Color
) {
    val totalWeekCals = weekMeals.sumOf { it.calories }
    val avgDailyCals = totalWeekCals / 7
    val totalProtein = weekMeals.sumOf { it.protein }
    val totalCarbs = weekMeals.sumOf { it.carbs }
    val daysLogged = weekMeals.map { it.date }.distinct().size

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BarChart, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("This Week", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        "$daysLogged/7 days",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeeklyStat("Total Cals", "${totalWeekCals}", "kcal", accentColor)
                WeeklyStat("Daily Avg", "$avgDailyCals", "kcal/day", accentColor)
                WeeklyStat("Protein", "${totalProtein}g", "total", accentColor)
                WeeklyStat("Carbs", "${totalCarbs}g", "total", accentColor)
            }

            if (weekMeals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                val avgVsGoal = avgDailyCals - targetCals
                val weekInsight = when {
                    daysLogged == 0 -> "No meals logged this week. Start tracking for insights!"
                    avgVsGoal > 200 -> "📈 Weekly avg is ${avgVsGoal} kcal over goal. Consider lighter meals."
                    avgVsGoal < -400 -> "📉 Weekly avg is ${-avgVsGoal} kcal below goal. Ensure adequate nutrition."
                    else -> "✅ Weekly average of $avgDailyCals kcal is close to your $targetCals goal. Great consistency!"
                }
                Text(weekInsight, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
fun WeeklyStat(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
    }
}

// ─── Existing composables (updated for Light/Dark Mode) ──────────────────────

@Composable
fun MacroSummaryCard(
    consumed: Int,
    burned: Int,
    total: Int,
    proteinConsumed: Int,
    proteinTarget: Int,
    carbsConsumed: Int,
    carbsTarget: Int,
    fatsConsumed: Int,
    fatsTarget: Int,
    accentColor: Color
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val proteinGradient = if (isDark) {
        listOf(Color(0xFF00E5FF), Color(0xFF0091FF))
    } else {
        listOf(Color(0xFF0284C7), Color(0xFF2563EB))
    }
    val carbsGradient = if (isDark) {
        listOf(Color(0xFFFFB300), Color(0xFFFF8F00))
    } else {
        listOf(Color(0xFFF59E0B), Color(0xFFD97706))
    }
    val fatsGradient = if (isDark) {
        listOf(Color(0xFFFF5252), Color(0xFFFF1744))
    } else {
        listOf(Color(0xFFE11D48), Color(0xFFBE123C))
    }

    val calProgress = if (total > 0) (consumed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val animProgress by animateFloatAsState(
        targetValue = calProgress,
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label = "cal_progress"
    )

    val remaining = (total - consumed).coerceAtLeast(0)
    val isOver = consumed > total

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isDark) 0.dp else 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isDark) {
                        Brush.verticalGradient(listOf(Color(0xFF131D2A), Color(0xFF0B1320)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
                    }
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = if (isDark) 0.2f else 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "DAILY CALORIE BUDGET",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color.Black
                        )
                        Text(
                            "Calories & Macro Tracking",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯", fontSize = 11.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$total kcal",
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Hero Radial Arc Gauge (Futuristic Speedo Arc)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 230.dp, height = 160.dp)
            ) {
                val trackColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
                val gradientColors = if (isDark) {
                    listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFD600), Color(0xFFFF3D00))
                } else {
                    listOf(Color(0xFFEA580C), Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0xFFEA580C))
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val arcPadding = strokeWidth / 2f + 4.dp.toPx()
                    val arcSize = Size(size.width - arcPadding * 2, size.height * 1.6f - arcPadding * 2)
                    val topLeft = Offset(arcPadding, arcPadding)
                    val startAngle = 145f
                    val totalSweep = 250f

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = startAngle,
                        sweepAngle = totalSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Foreground animated gradient arc
                    if (animProgress > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = gradientColors,
                                center = Offset(size.width / 2f, size.height * 0.82f)
                            ),
                            startAngle = startAngle,
                            sweepAngle = totalSweep * animProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Glowing needle tip dot
                        val angleRad = Math.toRadians((startAngle + totalSweep * animProgress).toDouble())
                        val cx = size.width / 2f
                        val cy = size.height * 0.82f
                        val rx = arcSize.width / 2f
                        val ry = arcSize.height / 2f
                        val dotX = cx + (rx * Math.cos(angleRad)).toFloat()
                        val dotY = cy + (ry * Math.sin(angleRad)).toFloat()

                        drawCircle(
                            color = Color(0xFFFF9100).copy(alpha = 0.45f),
                            radius = 9.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.5.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 18.dp)
                ) {
                    Text(
                        text = if (isOver) "+${consumed - total}" else "$remaining",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        color = if (isOver) Color(0xFFFF453A) else if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = if (isOver) "KCAL OVER GOAL" else "KCAL REMAINING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = if (isOver) Color(0xFFFF453A) else if (isDark) Color(0xFF94A3B8) else Color.Black
                    )
                    Spacer(Modifier.height(5.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOver) Color(0xFFFF453A).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isOver) "Exceeded Goal" else "${(animProgress * 100).toInt()}% of Goal",
                            color = if (isOver) Color(0xFFFF453A) else Color(0xFF10B981),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Calorie Budget Equation Row (Food, Burned, Net)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Food Eaten Chip
                BudgetStatChip(
                    icon = "🍏",
                    label = "Food",
                    value = "$consumed",
                    unit = "kcal",
                    color = Color(0xFFF59E0B),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                // Burned Chip
                BudgetStatChip(
                    icon = "🔥",
                    label = "Burned",
                    value = "$burned",
                    unit = "kcal",
                    color = accentColor,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                // Net Chip
                BudgetStatChip(
                    icon = "⚡",
                    label = "Net",
                    value = "${consumed - burned}",
                    unit = "kcal",
                    color = if (isDark) Color(0xFF00E5FF) else Color(0xFF0284C7),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(18.dp))

            // 3 Modern Macro Progress Capsules
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModernMacroCapsule(
                    label = "Protein",
                    icon = "🥩",
                    consumed = proteinConsumed,
                    target = proteinTarget,
                    gradientColors = proteinGradient,
                    isDark = isDark
                )
                ModernMacroCapsule(
                    label = "Carbs",
                    icon = "🌾",
                    consumed = carbsConsumed,
                    target = carbsTarget,
                    gradientColors = carbsGradient,
                    isDark = isDark
                )
                ModernMacroCapsule(
                    label = "Fats",
                    icon = "🥑",
                    consumed = fatsConsumed,
                    target = fatsTarget,
                    gradientColors = fatsGradient,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
fun BudgetStatChip(
    icon: String,
    label: String,
    value: String,
    unit: String,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF16202E) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF94A3B8) else Color.Black
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    unit,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
fun ModernMacroCapsule(
    label: String,
    icon: String,
    consumed: Int,
    target: Int,
    gradientColors: List<Color>,
    isDark: Boolean
) {
    val progress = if (target > 0) (consumed.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val animProg by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "macro_anim_$label"
    )
    val pct = (progress * 100).toInt()
    val primaryColor = gradientColors.first()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF131D2A) else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (isDark) primaryColor.copy(alpha = 0.22f) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${consumed}g",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        " / ${target}g",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF94A3B8) else Color.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = primaryColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                    ) {
                        Text(
                            "$pct%",
                            color = primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Capsule Progress Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                        RoundedCornerShape(4.dp)
                    )
            ) {
                if (animProg > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animProg)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(gradientColors),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MealCategoryCard(
    title: String,
    icon: ImageVector,
    meals: List<MealRecord>,
    color: Color,
    onAddClick: () -> Unit,
    onDeleteClick: (MealRecord) -> Unit
) {
    val totalCals = meals.sumOf { it.calories }
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }

    val badgeColor = when (title.lowercase()) {
        "breakfast" -> Color(0xFFF59E0B)
        "lunch" -> Color(0xFF10B981)
        "snack" -> Color(0xFF8B5CF6)
        "dinner" -> Color(0xFF3B82F6)
        else -> color
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shadowElevation = if (isDark) 0.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(22.dp))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (totalCals > 0) {
                        Text("$totalCals kcal", color = badgeColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Not logged yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }

                if (totalCals > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "${meals.size} items",
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(badgeColor, CircleShape)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Meal", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            if (meals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    meals.forEach { meal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(meal.foodName, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("P: ${meal.protein}g", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    Text("C: ${meal.carbs}g", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    Text("F: ${meal.fats}g", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${meal.calories} kcal", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDeleteClick(meal) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tap + to log $title",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
fun WaterLogCard(glasses: Int, goal: Int, onAddWater: () -> Unit) {
    val progress = if (goal > 0) (glasses.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    val waterColor = Color(0xFF0284C7)
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, waterColor.copy(alpha = 0.25f)),
        shadowElevation = if (isDark) 0.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(waterColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, waterColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = waterColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Water Hydration", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("$glasses / $goal glasses", color = waterColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${glasses * 250} ml logged today", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = waterColor,
                        trackColor = waterColor.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(waterColor, CircleShape)
                    .clickable { onAddWater() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Water", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealDialog(
    defaultMealType: String,
    initialBarcode: String?,
    onDismiss: () -> Unit,
    onAdd: (MealRecord) -> Unit,
    onScanBarcodeClick: () -> Unit
) {
    var mealType by remember { mutableStateOf(defaultMealType) }
    var foodName by remember { mutableStateOf("") }
    var caloriesStr by remember { mutableStateOf("") }
    var proteinStr by remember { mutableStateOf("") }
    var carbsStr by remember { mutableStateOf("") }
    var fatsStr by remember { mutableStateOf("") }
    val primaryColor = MaterialTheme.colorScheme.primary

    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val presets = listOf(
        QuickFoodPreset("Eggs (2 boiled)", 155, 13, 1, 11),
        QuickFoodPreset("Oatmeal & Milk", 310, 11, 54, 6),
        QuickFoodPreset("Chicken breast (150g)", 250, 46, 0, 6),
        QuickFoodPreset("Brown Rice (1 cup)", 216, 5, 45, 2),
        QuickFoodPreset("Protein Shake", 180, 25, 3, 2),
        QuickFoodPreset("Mixed Almonds (30g)", 170, 6, 6, 15),
        QuickFoodPreset("Apple with Peanut Butter", 250, 7, 28, 16),
        QuickFoodPreset("Paneer Salad", 280, 18, 8, 20)
    )

    fun searchFoodOnline(query: String) {
        if (query.isBlank()) return
        isSearching = true
        searchError = null
        coroutineScope.launch {
            try {
                val response = com.pro.aifitnesstracker.data.network.FoodNetworkManager.api.searchFood(query)
                val product = response.products?.firstOrNull { it.product_name != null && it.nutriments?.energy_kcal_100g != null }
                if (product != null) {
                    foodName = product.product_name ?: query
                    val nut = product.nutriments
                    caloriesStr = (nut?.energy_kcal_100g?.toInt() ?: 0).toString()
                    proteinStr = (nut?.proteins_100g?.toInt() ?: 0).toString()
                    carbsStr = (nut?.carbohydrates_100g?.toInt() ?: 0).toString()
                    fatsStr = (nut?.fat_100g?.toInt() ?: 0).toString()
                } else {
                    searchError = "Not found. Please enter manually."
                }
            } catch (e: Exception) {
                searchError = "Offline or error. Enter manually."
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(initialBarcode) {
        if (initialBarcode != null) {
            isSearching = true
            searchError = null
            coroutineScope.launch {
                try {
                    val response = com.pro.aifitnesstracker.data.network.FoodNetworkManager.api.searchByBarcode(initialBarcode)
                    val product = response.product
                    if (product != null && product.product_name != null) {
                        foodName = product.product_name
                        val nut = product.nutriments
                        caloriesStr = (nut?.energy_kcal_100g?.toInt() ?: 0).toString()
                        proteinStr = (nut?.proteins_100g?.toInt() ?: 0).toString()
                        carbsStr = (nut?.carbohydrates_100g?.toInt() ?: 0).toString()
                        fatsStr = (nut?.fat_100g?.toInt() ?: 0).toString()
                    } else {
                        searchError = "Barcode not found. Enter manually."
                    }
                } catch (e: Exception) {
                    searchError = "Offline or error. Enter manually."
                } finally {
                    isSearching = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Log a Meal",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf("Breakfast", "Lunch", "Dinner", "Snack")
                    types.forEach { type ->
                        val isSel = mealType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) primaryColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { mealType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                type,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text("Quick Presets", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.take(4).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    foodName = preset.name
                                    caloriesStr = preset.calories.toString()
                                    proteinStr = preset.protein.toString()
                                    carbsStr = preset.carbs.toString()
                                    fatsStr = preset.fats.toString()
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset.name.substringBefore(" "), color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.drop(4).take(4).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    foodName = preset.name
                                    caloriesStr = preset.calories.toString()
                                    proteinStr = preset.protein.toString()
                                    carbsStr = preset.carbs.toString()
                                    fatsStr = preset.fats.toString()
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset.name.substringBefore(" "), color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Food Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(50.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .clickable { onScanBarcodeClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = "Scan Barcode", tint = primaryColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(50.dp)
                            .background(primaryColor, RoundedCornerShape(12.dp))
                            .clickable { searchFoodOnline(foodName) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Search, contentDescription = "Search Online", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                
                if (searchError != null) {
                    Text(searchError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                }

                OutlinedTextField(
                    value = caloriesStr,
                    onValueChange = { caloriesStr = it },
                    label = { Text("Calories (kcal per 100g/serving)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proteinStr,
                        onValueChange = { proteinStr = it },
                        label = { Text("Protein (g)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbsStr,
                        onValueChange = { carbsStr = it },
                        label = { Text("Carbs (g)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fatsStr,
                        onValueChange = { fatsStr = it },
                        label = { Text("Fats (g)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cals = caloriesStr.toIntOrNull() ?: 0
                    val prot = proteinStr.toIntOrNull() ?: 0
                    val carb = carbsStr.toIntOrNull() ?: 0
                    val fat = fatsStr.toIntOrNull() ?: 0

                    if (foodName.isNotEmpty() && cals > 0) {
                        onAdd(
                            MealRecord(
                                date = "",
                                mealType = mealType,
                                foodName = foodName,
                                calories = cals,
                                protein = prot,
                                carbs = carb,
                                fats = fat
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("ADD", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}


data class QuickFoodPreset(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int
)

@Composable
fun MacroStat(name: String, consumed: String, total: String, color: Color, progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(consumed, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(total, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}

@Composable
fun SectionTitle(title: String, accentColor: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = accentColor,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun DashboardVisibilityToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Dashboard,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Show on Dashboard",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun DashboardVisibilityToggleMini(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Rounded.Dashboard,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Show $label",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.5f)
            ),
            modifier = Modifier.scale(0.7f)
        )
    }
}




