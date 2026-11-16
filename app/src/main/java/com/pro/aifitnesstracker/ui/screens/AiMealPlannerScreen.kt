package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.LocalAppStrings

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

// ─── Data Models ─────────────────────────────────────────────────────────────

data class MealItem(
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val emoji: String
)

data class DayMealPlan(
    val day: String,
    val breakfast: MealItem,
    val lunch: MealItem,
    val dinner: MealItem,
    val snack: MealItem,
    val totalCalories: Int
)

data class WeeklyMealPlan(
    val goal: String,
    val targetCalories: Int,
    val dietType: String,
    val days: List<DayMealPlan>
)

// ─── Gemini Meal Plan API ────────────────────────────────────────────────────

object MealPlanApi {

    private val MODELS = listOf(
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-flash-lite-latest",
        "gemini-2.0-flash-lite",
        "gemini-2.0-flash"
    )
    private const val API_KEY = "AIzaSyBD7Rn5NdSCJrFk6af1TP60eBgV_iT4Z84"
    private const val TAG = "MealPlanApi"

    @Throws(Exception::class)
    fun generatePlan(
        goal: String,
        weightKg: Int,
        heightCm: Int,
        ageYears: Int,
        dietType: String,
        targetCalories: Int
    ): WeeklyMealPlan {
        val prompt = buildPrompt(goal, weightKg, heightCm, ageYears, dietType, targetCalories)
        val bodyBytes = buildRequestBody(prompt)

        var lastError = "All models unavailable"
        for (model in MODELS) {
            try {
                val url = java.net.URL(
                    "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$API_KEY"
                )
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 25_000
                    readTimeout = 40_000
                }
                conn.outputStream.use { it.write(bodyBytes) }
                val code = conn.responseCode
                Log.d(TAG, "$model → $code")

                if (code == 200) {
                    val raw = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    return parseResponse(raw, goal, targetCalories, dietType)
                } else {
                    conn.disconnect()
                    when (code) {
                        404, 429, 503 -> {
                            lastError = "Model $model not available ($code)"
                            continue
                        }
                        401 -> throw Exception("Invalid API key.")
                        403 -> throw Exception("API key permission denied.")
                        else -> throw Exception("API error $code")
                    }
                }
            } catch (e: Exception) {
                if (e.message?.contains("API key") == true || e.message?.contains("permission") == true) throw e
                lastError = e.message ?: "Unknown error"
                continue
            }
        }
        throw Exception("Rate limit hit. Please wait 1 minute and try again.\n$lastError")
    }

    private fun buildPrompt(
        goal: String, weight: Int, height: Int, age: Int, diet: String, calories: Int
    ): String = """
        You are a professional nutritionist. Create a 7-day meal plan as JSON.
        
        User Profile:
        - Goal: $goal
        - Weight: ${weight}kg, Height: ${height}cm, Age: $age years
        - Diet: $diet
        - Target calories: $calories kcal/day
        
        Return ONLY this JSON, no markdown, no explanation:
        {
          "days": [
            {
              "day": "Monday",
              "breakfast": {"name": "Oatmeal with banana and almonds", "calories": 380, "protein": 14, "carbs": 62, "fat": 8, "emoji": "🥣"},
              "lunch": {"name": "Paneer tikka salad with quinoa", "calories": 520, "protein": 32, "carbs": 45, "fat": 18, "emoji": "🥗"},
              "dinner": {"name": "Dal tadka with brown rice and sabzi", "calories": 540, "protein": 22, "carbs": 82, "fat": 10, "emoji": "🍛"},
              "snack": {"name": "Roasted chana and mixed nuts", "calories": 180, "protein": 8, "carbs": 16, "fat": 9, "emoji": "🥜"},
              "totalCalories": 1620
            }
          ]
        }
        
        Rules:
        - All 7 days: Monday through Sunday
        - Diet type is $diet — strictly follow this
        - Meals should be practical Indian/common foods
        - totalCalories = sum of all 4 meals
        - Vary meals each day, no repetition
        - Use realistic, achievable foods
    """.trimIndent()

    private fun buildRequestBody(prompt: String): ByteArray {
        val json = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("maxOutputTokens", 3000)
            })
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    private fun parseResponse(raw: String, goal: String, targetCal: Int, dietType: String): WeeklyMealPlan {
        val respObj = JSONObject(raw)
        val text = respObj
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        val json = JSONObject(text)
        val daysArr = json.getJSONArray("days")
        val days = mutableListOf<DayMealPlan>()

        for (i in 0 until daysArr.length()) {
            val d = daysArr.getJSONObject(i)
            days.add(
                DayMealPlan(
                    day = d.optString("day", "Day ${i + 1}"),
                    breakfast = parseMeal(d.getJSONObject("breakfast")),
                    lunch = parseMeal(d.getJSONObject("lunch")),
                    dinner = parseMeal(d.getJSONObject("dinner")),
                    snack = parseMeal(d.getJSONObject("snack")),
                    totalCalories = d.optInt("totalCalories", 0)
                )
            )
        }
        return WeeklyMealPlan(goal, targetCal, dietType, days)
    }

    private fun parseMeal(obj: JSONObject) = MealItem(
        name = obj.optString("name", ""),
        calories = obj.optInt("calories", 0),
        protein = obj.optDouble("protein", 0.0).toFloat(),
        carbs = obj.optDouble("carbs", 0.0).toFloat(),
        fat = obj.optDouble("fat", 0.0).toFloat(),
        emoji = obj.optString("emoji", "🍽️")
    )
}

// ─── Main Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMealPlannerScreen(onBack: () -> Unit) {
    val strings = LocalAppStrings.current

    var screen by remember { mutableStateOf("INPUT") } // INPUT | LOADING | RESULT | ERROR
    var plan by remember { mutableStateOf<WeeklyMealPlan?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Input state
    var selectedGoal by remember { mutableStateOf("Weight Loss") }
    var selectedDiet by remember { mutableStateOf("Vegetarian") }
    var weight by remember { mutableStateOf("70") }
    var height by remember { mutableStateOf("170") }
    var age by remember { mutableStateOf("25") }
    var targetCal by remember { mutableStateOf("1800") }

    val coroutineScope = rememberCoroutineScope()

    fun generatePlan() {
        screen = "LOADING"
        error = null
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val result = MealPlanApi.generatePlan(
                    goal = selectedGoal,
                    weightKg = weight.toIntOrNull() ?: 70,
                    heightCm = height.toIntOrNull() ?: 170,
                    ageYears = age.toIntOrNull() ?: 25,
                    dietType = selectedDiet,
                    targetCalories = targetCal.toIntOrNull() ?: 1800
                )
                withContext(Dispatchers.Main) {
                    plan = result
                    screen = "RESULT"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = e.message
                    screen = "ERROR"
                }
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    val emeraldAccent = Color(0xFF10B981)
    val cyanAccent = Color(0xFF06B6D4)
    val amberAccent = Color(0xFFF59E0B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (screen) {
                "INPUT" -> InputScreen(
                    selectedGoal = selectedGoal,
                    selectedDiet = selectedDiet,
                    weight = weight,
                    height = height,
                    age = age,
                    targetCal = targetCal,
                    onGoalChange = { selectedGoal = it },
                    onDietChange = { selectedDiet = it },
                    onWeightChange = { weight = it },
                    onHeightChange = { height = it },
                    onAgeChange = { age = it },
                    onCalChange = { targetCal = it },
                    onGenerate = ::generatePlan,
                    onBack = onBack,
                    isDark = isDark,
                    backgroundColor = backgroundColor,
                    cardColor = cardColor,
                    surfaceAltColor = surfaceAltColor,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    emeraldAccent = emeraldAccent,
                    amberAccent = amberAccent
                )
                "LOADING" -> LoadingScreen(
                    isDark = isDark,
                    backgroundColor = backgroundColor,
                    emeraldAccent = emeraldAccent,
                    cyanAccent = cyanAccent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                "RESULT" -> plan?.let {
                    ResultScreen(
                        plan = it,
                        onBack = onBack,
                        onRegenerate = { screen = "INPUT" },
                        isDark = isDark,
                        backgroundColor = backgroundColor,
                        cardColor = cardColor,
                        surfaceAltColor = surfaceAltColor,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        emeraldAccent = emeraldAccent,
                        cyanAccent = cyanAccent,
                        amberAccent = amberAccent
                    )
                }
                "ERROR" -> ErrorScreen(
                    error = error ?: "Unknown error",
                    onRetry = { screen = "INPUT" },
                    onBack = onBack,
                    isDark = isDark,
                    backgroundColor = backgroundColor,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    emeraldAccent = emeraldAccent
                )
            }
        }
        AdBanner(adKey = "ad_meal_planner_banner")
    }
}

// ─── Input Screen ─────────────────────────────────────────────────────────────

@Composable
private fun InputScreen(
    selectedGoal: String,
    selectedDiet: String,
    weight: String,
    height: String,
    age: String,
    targetCal: String,
    onGoalChange: (String) -> Unit,
    onDietChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onCalChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onBack: () -> Unit,
    isDark: Boolean,
    backgroundColor: Color,
    cardColor: Color,
    surfaceAltColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    emeraldAccent: Color,
    amberAccent: Color
) {
    val strings = LocalAppStrings.current
    val scroll = rememberScrollState()

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Surface(
                color = backgroundColor,
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CUSTOM MEAL PLANNER",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(emeraldAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GEMINI 2.5 • 7-DAY NUTRITION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = emeraldAccent,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Preset badge
                    Surface(
                        color = cardColor,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Text(
                            text = "CHEF RECIPES",
                            color = emeraldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Hero Showcase Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(emeraldAccent.copy(alpha = 0.25f), surfaceAltColor)
                                )
                            )
                            .border(1.5.dp, emeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = emeraldAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "SMART NUTRITION ADVISOR",
                            color = emeraldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "7-Day Precision Meal Plan",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Personalized macro balancing calibrated to your metabolism, daily activity, and culinary preferences.",
                    color = textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🎯 Smart Optimized", "📊 Macro Balanced", "🥗 Indian & Global").forEach { tag ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(surfaceAltColor)
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(tag, color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Goal Selection
        Text(
            text = "TARGET FITNESS GOAL",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(10.dp))
        val goals = listOf(
            "Weight Loss" to "⚖️",
            "Muscle Gain" to "💪",
            "Maintenance" to "🏃",
            "Healthy Eating" to "🥗"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(goals) { (goal, emoji) ->
                val selected = selectedGoal == goal
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) emeraldAccent else cardColor)
                        .border(
                            1.dp,
                            if (selected) emeraldAccent else borderColor,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onGoalChange(goal) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 22.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = goal,
                            color = if (selected) Color(0xFF0F172A) else textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Diet Type Selection
        Text(
            text = "DIETARY PREFERENCE",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(10.dp))
        val diets = listOf(
            "Vegetarian" to "🥗",
            "Non-Veg" to "🍗",
            "Vegan" to "🌱",
            "Eggetarian" to "🥚"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            diets.forEach { (diet, emoji) ->
                val selected = selectedDiet == diet
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) emeraldAccent else cardColor)
                        .border(
                            1.dp,
                            if (selected) emeraldAccent else borderColor,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onDietChange(diet) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 18.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = diet,
                            color = if (selected) Color(0xFF0F172A) else textPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Body Stats Input Cards
        Text(
            text = "ATHLETE METRICS",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InputMetricCard(
                label = "WEIGHT",
                value = weight,
                unit = "kg",
                modifier = Modifier.weight(1f),
                cardColor = cardColor,
                borderColor = borderColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accentColor = emeraldAccent,
                onChange = onWeightChange
            )
            InputMetricCard(
                label = "HEIGHT",
                value = height,
                unit = "cm",
                modifier = Modifier.weight(1f),
                cardColor = cardColor,
                borderColor = borderColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accentColor = emeraldAccent,
                onChange = onHeightChange
            )
            InputMetricCard(
                label = "AGE",
                value = age,
                unit = "yrs",
                modifier = Modifier.weight(1f),
                cardColor = cardColor,
                borderColor = borderColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accentColor = emeraldAccent,
                onChange = onAgeChange
            )
        }

        Spacer(Modifier.height(20.dp))

        // Daily Calorie Target Card
        Text(
            text = "DAILY ENERGY TARGET",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(amberAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = amberAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                OutlinedTextField(
                    value = targetCal,
                    onValueChange = { if (it.length <= 5) onCalChange(it) },
                    label = { Text("Target Calories", color = textSecondary) },
                    suffix = { Text("kcal", color = emeraldAccent, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = emeraldAccent,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Generate Button
        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = emeraldAccent,
                contentColor = Color(0xFF0F172A)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "GENERATE 7-DAY MEAL PLAN",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}
}

// ─── Loading Screen ────────────────────────────────────────────────────────────

@Composable
private fun LoadingScreen(
    isDark: Boolean,
    backgroundColor: Color,
    emeraldAccent: Color,
    cyanAccent: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "pulse"
    )
    val stepIdx by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "step"
    )
    val steps = listOf(
        "🧠 Analyzing metabolic profile...",
        "🥗 Selecting nutritious whole foods...",
        "📊 Calculating optimal macro ratio...",
        "📅 Formatting 7-day schedule...",
        "✅ Finalizing personalized plan..."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(CircleShape)
                    .background(emeraldAccent.copy(0.12f))
                    .border(
                        2.5.dp,
                        Brush.sweepGradient(listOf(emeraldAccent, cyanAccent, emeraldAccent)),
                        CircleShape
                    )
            )
            Icon(
                imageVector = Icons.Default.RestaurantMenu,
                contentDescription = null,
                tint = emeraldAccent,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(Modifier.height(36.dp))
        Text(
            text = "Chef Generating Meal Plan",
            color = textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = steps.getOrElse(stepIdx.toInt()) { steps.last() },
            label = "stepText"
        ) { step ->
            Text(
                text = step,
                color = emeraldAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { idx ->
                Box(
                    modifier = Modifier
                        .size(if (idx == stepIdx.toInt()) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (idx <= stepIdx.toInt()) emeraldAccent else textSecondary.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Gemini 2.5 is balancing your weekly nutrients...",
            color = textSecondary,
            fontSize = 11.sp
        )
    }
}

// ─── Result Screen ────────────────────────────────────────────────────────────

@Composable
private fun ResultScreen(
    plan: WeeklyMealPlan,
    onBack: () -> Unit,
    onRegenerate: () -> Unit,
    isDark: Boolean,
    backgroundColor: Color,
    cardColor: Color,
    surfaceAltColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    emeraldAccent: Color,
    cyanAccent: Color,
    amberAccent: Color
) {
    val strings = LocalAppStrings.current
    val scroll = rememberScrollState()
    var selectedDay by remember { mutableIntStateOf(0) }
    val day = plan.days.getOrNull(selectedDay) ?: return

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Surface(
                color = backgroundColor,
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "YOUR 7-DAY MEAL PLAN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${plan.goal} • ${plan.dietType}",
                            color = emeraldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Edit Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(onClick = onRegenerate)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "EDIT",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scroll)
        ) {

        Spacer(Modifier.height(8.dp))

        // Day Selector Bar (Mon to Sun)
        Text(
            text = "SELECT DAY SCHEDULE",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(plan.days) { idx, d ->
                val selected = idx == selectedDay
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) emeraldAccent else cardColor)
                        .border(
                            1.dp,
                            if (selected) emeraldAccent else borderColor,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedDay = idx }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = d.day.take(3).uppercase(Locale.US),
                            color = if (selected) Color(0xFF0F172A) else textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${d.totalCalories}",
                            color = if (selected) Color(0xFF0F172A) else textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "kcal",
                            color = if (selected) Color(0xFF0F172A).copy(alpha = 0.7f) else textSecondary.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Daily Calorie & Macro Target Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(22.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${day.day.uppercase(Locale.US)} NUTRITION TARGET",
                            color = textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${day.totalCalories} kcal",
                            color = textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    val diff = day.totalCalories - plan.targetCalories
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (kotlin.math.abs(diff) <= 150) emeraldAccent.copy(alpha = 0.15f)
                                else amberAccent.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (kotlin.math.abs(diff) <= 150) emeraldAccent.copy(alpha = 0.5f)
                                else amberAccent.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (diff >= 0) "+$diff vs target" else "$diff vs target",
                            color = if (kotlin.math.abs(diff) <= 150) emeraldAccent else amberAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 3 Macro Progress Bars: Protein, Carbs, Fats
                val mealsList = listOf(day.breakfast, day.lunch, day.dinner, day.snack)
                val totalP = mealsList.sumOf { it.protein.toDouble() }.toInt()
                val totalC = mealsList.sumOf { it.carbs.toDouble() }.toInt()
                val totalF = mealsList.sumOf { it.fat.toDouble() }.toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroProgressBar(
                        label = strings.protein.uppercase(),
                        grams = "${totalP}g",
                        color = emeraldAccent,
                        surfaceAlt = surfaceAltColor,
                        modifier = Modifier.weight(1f)
                    )
                    MacroProgressBar(
                        label = strings.carbs.uppercase(),
                        grams = "${totalC}g",
                        color = cyanAccent,
                        surfaceAlt = surfaceAltColor,
                        modifier = Modifier.weight(1f)
                    )
                    MacroProgressBar(
                        label = strings.fats.uppercase(),
                        grams = "${totalF}g",
                        color = amberAccent,
                        surfaceAlt = surfaceAltColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Categorized Meal Cards
        Text(
            text = "MEAL SCHEDULE FOR ${day.day.uppercase(Locale.US)}",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val scheduledMeals = listOf(
                MealSchedule("BREAKFAST", "08:30 AM", amberAccent, day.breakfast),
                MealSchedule("LUNCH", "01:30 PM", emeraldAccent, day.lunch),
                MealSchedule("EVENING SNACK", "05:30 PM", Color(0xFFA855F7), day.snack),
                MealSchedule("DINNER", "08:30 PM", cyanAccent, day.dinner)
            )

            scheduledMeals.forEach { sm ->
                MealCardItem(
                    schedule = sm,
                    cardColor = cardColor,
                    surfaceAltColor = surfaceAltColor,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Regenerate Button
        Button(
            onClick = onRegenerate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = surfaceAltColor,
                contentColor = emeraldAccent
            ),
            border = BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.5f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, null, tint = emeraldAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "REGENERATE NEW MEAL PLAN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}
}

private data class MealSchedule(
    val title: String,
    val time: String,
    val accentColor: Color,
    val item: MealItem
)

@Composable
private fun MealCardItem(
    schedule: MealSchedule,
    cardColor: Color,
    surfaceAltColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji Container
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(schedule.accentColor.copy(alpha = 0.22f), surfaceAltColor)
                        )
                    )
                    .border(1.5.dp, schedule.accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = schedule.item.emoji.ifEmpty { "🍽️" },
                    fontSize = 24.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = schedule.title,
                        color = schedule.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    )

                    // Time badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = schedule.time,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = schedule.item.name,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Macro Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MacroPill("P ${schedule.item.protein.toInt()}g", schedule.accentColor)
                    MacroPill("C ${schedule.item.carbs.toInt()}g", schedule.accentColor)
                    MacroPill("F ${schedule.item.fat.toInt()}g", schedule.accentColor)
                }
            }

            Spacer(Modifier.width(10.dp))

            // Calories
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${schedule.item.calories}",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "kcal",
                    color = textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MacroProgressBar(
    label: String,
    grams: String,
    color: Color,
    surfaceAlt: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceAlt)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = grams,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
private fun MacroPill(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InputMetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color,
    onChange: (String) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = textSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 4) onChange(it) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = borderColor,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = unit,
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    isDark: Boolean,
    backgroundColor: Color,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    emeraldAccent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(32.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("❌", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Plan Generation Failed",
            color = textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Text(
                text = error,
                color = textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = emeraldAccent,
                contentColor = Color(0xFF0F172A)
            )
        ) {
            Icon(Icons.Default.Refresh, null, tint = Color(0xFF0F172A))
            Spacer(Modifier.width(8.dp))
            Text("Try Again", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Text("Go Back", color = textPrimary)
        }
    }
}
