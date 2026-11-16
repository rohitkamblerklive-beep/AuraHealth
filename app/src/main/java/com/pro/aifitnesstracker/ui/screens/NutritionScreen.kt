package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    onBack: () -> Unit,
    prefsManager: SharedPrefsManager,
    repository: HealthRepository
) {
    val strings = LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()
    
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val backgroundColor = if (isDarkTheme) Color(0xFF080B14) else Color(0xFFEEF2F6)
    val cardColor = if (isDarkTheme) Color(0xFF141C2E) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF24324D) else Color(0xFFE2E8F0)
    val canvasBgGradient = Brush.verticalGradient(
        colors = if (isDarkTheme) listOf(
            Color(0xFF0F1523),
            Color(0xFF080B14),
            Color(0xFF05070E)
        ) else listOf(
            Color(0xFFF1F5F9),
            Color(0xFFEEF2F6),
            Color(0xFFE2E8F0)
        )
    )
    val accentColor = MaterialTheme.colorScheme.primary // Lavender accent
    
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val loggedMeals by repository.getMealsByDate(today).collectAsState(initial = emptyList())

    val weight = prefsManager.getWeight()
    val height = prefsManager.getHeight()
    val age = prefsManager.getAge()
    val gender = prefsManager.getGender()

    // Estimate TDEE
    val tdee = if (weight > 0 && height > 0 && age > 0) {
        val bmr = if (gender.lowercase() == "female") {
            (10 * weight) + (6.25f * height) - (5 * age) - 161
        } else {
            (10 * weight) + (6.25f * height) - (5 * age) + 5
        }
        (bmr * 1.375f).toInt() // Active baseline
    } else {
        2000
    }

    // Set targets based on weight goal
    val selectedGoal = "LOSE" // Simplified or default
    val targetCals = when (selectedGoal) {
        "LOSE" -> tdee - 500
        "GAIN" -> tdee + 400
        else -> tdee
    }

    val proteinTarget = (weight * 1.8f).toInt().coerceAtLeast(50)
    val fatTarget = (weight * 0.8f).toInt().coerceAtLeast(30)
    val carbTarget = ((targetCals - (proteinTarget * 4 + fatTarget * 9)) / 4).coerceAtLeast(100)

    val totalCalsConsumed = loggedMeals.sumOf { it.calories }
    val totalProteinConsumed = loggedMeals.sumOf { it.protein }
    val totalCarbsConsumed = loggedMeals.sumOf { it.carbs }
    val totalFatsConsumed = loggedMeals.sumOf { it.fats }

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddMealDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { meal ->
                coroutineScope.launch {
                    repository.addMeal(meal.copy(date = today))
                }
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        strings.nutrition.uppercase(Locale.US),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        bottomBar = {
            AdBanner(adKey = "ad_nutrition_banner")
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(canvasBgGradient),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular Calorie Indicator Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 12.dp
                            )
                            CircularProgressIndicator(
                                progress = { (totalCalsConsumed.toFloat() / targetCals.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                color = accentColor,
                                strokeWidth = 12.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val remaining = (targetCals - totalCalsConsumed).coerceAtLeast(0)
                                Text("$remaining", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                                Text("KCAL LEFT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Summary info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Budget", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text("$targetCals kcal", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Consumed", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text("$totalCalsConsumed kcal", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // Macro Progress section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MacroGauge("Protein", "${totalProteinConsumed}g", "${proteinTarget}g", accentColor, (totalProteinConsumed.toFloat() / proteinTarget.toFloat()).coerceIn(0f, 1f))
                        MacroGauge("Carbs", "${totalCarbsConsumed}g", "${carbTarget}g", accentColor, (totalCarbsConsumed.toFloat() / carbTarget.toFloat()).coerceIn(0f, 1f))
                        MacroGauge("Fats", "${totalFatsConsumed}g", "${fatTarget}g", accentColor, (totalFatsConsumed.toFloat() / fatTarget.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }

            // Native Ad Card
            item {
                AdNative(
                    adKey = "ad_nutrition_native",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Logged Meals Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TODAY'S LOG", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.logMeal.uppercase(Locale.US), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Logged Meals list
            if (loggedMeals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No meals logged today yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            } else {
                items(loggedMeals) { meal ->
                    MealItemCard(meal = meal, onDelete = {
                        coroutineScope.launch {
                            repository.deleteMeal(meal)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun MacroGauge(label: String, value: String, target: String, color: Color, progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 4.dp
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 4.dp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(target, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}

@Composable
fun MealItemCard(meal: MealRecord, onDelete: () -> Unit) {
    val mealColor = MaterialTheme.colorScheme.primary
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(mealColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(meal.mealType.lowercase()) {
                        "breakfast" -> Icons.Rounded.FreeBreakfast
                        "lunch" -> Icons.Rounded.LunchDining
                        "snack" -> Icons.Rounded.Fastfood
                        else -> Icons.Rounded.DinnerDining
                    },
                    contentDescription = null,
                    tint = mealColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.foodName, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${meal.mealType} • P: ${meal.protein}g C: ${meal.carbs}g F: ${meal.fats}g", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("${meal.calories} kcal", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp).padding(top = 4.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddMealDialog(onDismiss: () -> Unit, onAdd: (MealRecord) -> Unit) {
    var mealType by remember { mutableStateOf("Breakfast") }
    var foodName by remember { mutableStateOf("") }
    var caloriesStr by remember { mutableStateOf("") }
    var proteinStr by remember { mutableStateOf("") }
    var carbsStr by remember { mutableStateOf("") }
    var fatsStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Log a Meal", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Meal Type selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val types = listOf("Breakfast", "Lunch", "Dinner", "Snack")
                    types.forEach { type ->
                        val isSel = mealType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { mealType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(type, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = caloriesStr,
                    onValueChange = { caloriesStr = it },
                    label = { Text("Calories (kcal)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
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
                            focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
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
                            focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
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
                            focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
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

