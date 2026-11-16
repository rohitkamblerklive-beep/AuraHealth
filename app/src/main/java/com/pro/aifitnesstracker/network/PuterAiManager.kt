package com.pro.aifitnesstracker.network

import android.util.Log
import com.pro.aifitnesstracker.SharedPrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CoachAdviceResult(
    val headline: String,
    val bulletItems: List<String>,
    val engineBadge: String
)

object PuterAiManager {

    private const val TAG = "PuterAiManager"
    private const val PUTER_OPENAI_ENDPOINT = "https://api.puter.com/puterai/openai/v1/chat/completions"

    /**
     * Queries Puter (OpenAI GPT-4o / Claude 3.5), with automatic seamless fallback
     * to Gemini 2.5 Flash, and lastly to local smart fitness heuristics.
     */
    suspend fun askCoach(
        userPrompt: String,
        userContextSummary: String,
        prefsManager: SharedPrefsManager
    ): CoachAdviceResult = withContext(Dispatchers.IO) {
        val puterToken = prefsManager.getPuterToken().trim()
        val preferredEngine = prefsManager.getSelectedAiEngine() // "PUTER_GPT4O", "PUTER_CLAUDE", "GEMINI_FLASH"

        val systemPrompt = """
            You are AuraHealth Pro AI Coach, an elite personal trainer and sports dietician.
            Athlete Context: $userContextSummary.
            User Query: "$userPrompt"
            
            RULES:
            1. First line MUST be a punchy, motivating headline summarizing the direct answer.
            2. Followed by 2 or 3 numbered bullet points (e.g. "1) 🥤 ...", "2) 🏋️ ...") with exact sets/reps, macros, or recovery steps.
            3. If user query is in Hindi or Hinglish, answer in friendly motivating Hinglish. If in English, answer in English.
            4. Keep it concise, actionable, and under 120 words.
        """.trimIndent()

        // 1. If Puter Token is available, try Puter GPT-4o / Claude
        if (puterToken.isNotEmpty()) {
            val modelName = if (preferredEngine == "PUTER_CLAUDE") "anthropic/claude-3-5-sonnet" else "openai/gpt-4o"
            val puterResult = tryPuterAi(systemPrompt, userPrompt, puterToken, modelName)
            if (puterResult != null) {
                val (head, bullets) = parseAiResponse(puterResult)
                val badgeName = if (preferredEngine == "PUTER_CLAUDE") "Puter Claude 3.5" else "Puter GPT-4o"
                return@withContext CoachAdviceResult(head, bullets, badgeName)
            }
        }

        // 2. Seamless Fallback: Google Gemini 2.5 Flash (Keyless for the user, highly reliable)
        try {
            val fullPrompt = "$systemPrompt\n\nAthlete asks: $userPrompt"
            val geminiRaw = GeminiApi.generateText(fullPrompt)
            val (head, bullets) = parseAiResponse(geminiRaw)
            return@withContext CoachAdviceResult(head, bullets, "Gemini 2.5 Flash")
        } catch (e: Exception) {
            Log.w(TAG, "Gemini fallback failed: ${e.message}")
        }

        // 3. Offline Heuristic Fallback (Never show empty or crashed state)
        val fallback = generateSmartFallback(userPrompt, userContextSummary)
        CoachAdviceResult(fallback.first, fallback.second, "Smart Offline Coach")
    }

    private fun tryPuterAi(
        systemPrompt: String,
        userPrompt: String,
        token: String,
        model: String
    ): String? {
        return try {
            val conn = (URL(PUTER_OPENAI_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 25_000
            }

            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 350)
            }

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val respText = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = JSONObject(respText)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val msgObj = choices.getJSONObject(0).optJSONObject("message")
                    return msgObj?.optString("content", "")?.trim()
                }
            } else {
                conn.disconnect()
                Log.w(TAG, "Puter returned HTTP $code")
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Puter error: ${e.message}")
            null
        }
    }

    private fun parseAiResponse(rawText: String): Pair<String, List<String>> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return Pair("Coach Recommendation", listOf("1) Stay hydrated and hit your daily target!"))
        }

        val headline = lines.first().removePrefix("#").removePrefix("**").removeSuffix("**").trim()
        val remaining = lines.drop(1)

        val bullets = mutableListOf<String>()
        var currentBullet = StringBuilder()

        for (line in remaining) {
            val isBulletStart = line.matches(Regex("""^(\d+[\.\)]|[-*•])\s+.*"""))
            if (isBulletStart) {
                if (currentBullet.isNotEmpty()) {
                    bullets.add(currentBullet.toString().trim())
                    currentBullet = StringBuilder()
                }
                currentBullet.append(line)
            } else {
                if (currentBullet.isNotEmpty()) {
                    currentBullet.append("\n").append(line)
                } else if (bullets.isEmpty()) {
                    // Part of headline or subhead
                    currentBullet.append(line)
                }
            }
        }
        if (currentBullet.isNotEmpty()) {
            bullets.add(currentBullet.toString().trim())
        }

        val finalBullets = if (bullets.isEmpty()) {
            listOf(
                "1) 🎯 Focus on progressive overload and consistent nutrition.",
                "2) 💧 Keep hydration high (minimum 3L today).",
                "3) 💤 Prioritize 7.5 hours of restorative sleep."
            )
        } else bullets

        return Pair(headline, finalBullets)
    }

    private fun generateSmartFallback(prompt: String, context: String): Pair<String, List<String>> {
        val lower = prompt.lowercase()
        return when {
            lower.contains("post-workout") || lower.contains("eat") || lower.contains("meal") || lower.contains("diet") || lower.contains("protein") -> {
                Pair(
                    "Smart Nutrition Protocol ($context):",
                    listOf(
                        "1) 🥤 Fast Recovery: 35g Whey/Soy Protein + 1 Banana within 45 mins",
                        "2) 🍗 Balanced Meal: Lean Protein + Sweet Potato/Rice with greens",
                        "3) 💧 Electrolytes: 500ml water with a pinch of pink salt"
                    )
                )
            }
            lower.contains("pain") || lower.contains("dard") || lower.contains("hurt") || lower.contains("injury") -> {
                Pair(
                    "Recovery & Safety Protocol:",
                    listOf(
                        "1) ⏸️ Pause Heavy Loading: Do not push through sharp joint pain.",
                        "2) 🧊 Cold/Heat Therapy: 15 mins ice for acute swelling, light mobility for stiffness.",
                        "3) 🩺 Active Rest: Focus on light walking and gentle stretching today."
                    )
                )
            }
            lower.contains("weight") || lower.contains("fat") || lower.contains("vajan") -> {
                Pair(
                    "Weight Target Acceleration Plan:",
                    listOf(
                        "1) ⚖️ 300 kcal Mild Deficit: Prioritize high satiety lean protein & fiber.",
                        "2) 🚶 Step Consistency: Aim for 10,000–12,000 brisk steps daily.",
                        "3) 💧 Zero Liquid Calories: Stick to water, green tea, and black coffee."
                    )
                )
            }
            else -> {
                Pair(
                    "Coach Guidance for Your Session:",
                    listOf(
                        "1) ⚡ Mind-Muscle Connection: Control eccentric lowering for 3 full seconds.",
                        "2) 🔥 Intensity Check: Leave 1–2 reps in reserve on your working sets.",
                        "3) 💧 Hydration Pacing: Sip water between sets to maintain peak power output."
                    )
                )
            }
        }
    }

    /**
     * Generates an Executive Briefing comparing two periods (Week-on-Week, Month-on-Month, or Day 1 vs Now)
     * using Puter Dual-AI (GPT-4o / Claude) with Gemini and local fallback.
     */
    suspend fun generateIntelligenceComparisonBriefing(
        timeframeTitle: String,
        currentSteps: Int,
        prevSteps: Int,
        currentCalories: Int,
        prevCalories: Int,
        currentWaterLiters: Float,
        prevWaterLiters: Float,
        restingHeartRate: Int,
        prefsManager: SharedPrefsManager
    ): List<String> = withContext(Dispatchers.IO) {
        val puterToken = prefsManager.getPuterToken().trim()
        val preferredEngine = prefsManager.getSelectedAiEngine()
        val stepsDeltaPct = if (prevSteps > 0) ((currentSteps - prevSteps).toFloat() / prevSteps * 100).toInt() else 0
        val calDeltaPct = if (prevCalories > 0) ((currentCalories - prevCalories).toFloat() / prevCalories * 100).toInt() else 0

        val prompt = """
            You are AuraHealth Pro AI Coach.
            Analyze this athlete's comparison for $timeframeTitle in simple, friendly, easy-to-understand English:
            - Steps: $currentSteps vs Previous: $prevSteps (Delta: $stepsDeltaPct%)
            - Calories: $currentCalories vs Previous: $prevCalories (Delta: $calDeltaPct%)
            - Water: $currentWaterLiters L vs Previous: $prevWaterLiters L
            - Resting Heart Rate: $restingHeartRate bpm

            Generate EXACTLY 2 short, motivating bullet points in simple, friendly, everyday English (no medical jargon):
            Bullet 1: Explain in simple English whether daily activity & stamina improved and why it helps their fitness.
            Bullet 2: A simple friendly comment on their heart health, recovery, or hydration.
            Do not use complex medical jargon like "autonomic tone" or clinical terms. Keep each bullet under 18 words. Return ONLY the 2 lines in English.
        """.trimIndent()

        // 1. Try Puter GPT-4o / Claude
        if (puterToken.isNotEmpty()) {
            val modelName = if (preferredEngine == "PUTER_CLAUDE") "anthropic/claude-3-5-sonnet" else "openai/gpt-4o"
            val res = tryPuterAi("You are AuraHealth Pro AI Coach.", prompt, puterToken, modelName)
            if (res != null) {
                val lines = res.split("\n").map { it.trim().removePrefix("-").removePrefix("•").removePrefix("1.").removePrefix("2.").trim() }.filter { it.isNotBlank() }
                if (lines.size >= 2) return@withContext lines.take(2)
            }
        }

        // 2. Try Gemini 2.5 Flash
        try {
            val geminiRes = GeminiApi.generateText(prompt)
            if (geminiRes.isNotBlank()) {
                val lines = geminiRes.split("\n").map { it.trim().removePrefix("-").removePrefix("•").removePrefix("1.").removePrefix("2.").trim() }.filter { it.isNotBlank() }
                if (lines.size >= 2) return@withContext lines.take(2)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini intelligence fallback: ${e.message}")
        }

        // 3. Deterministic Smart Biological Insights (Friendly & Simple English)
        val bullet1 = if (stepsDeltaPct >= 0) {
            "Daily steps and activity increased, boosting your cardiovascular stamina and energy."
        } else {
            "Activity is slightly lower. Aim for a brisk 20-minute daily walk to keep stamina steady."
        }

        val bullet2 = if (restingHeartRate in 50..72) {
            "Resting Heart Rate ($restingHeartRate bpm) is optimal — your heart is strong and recovering well."
        } else {
            "Staying hydrated ($currentWaterLiters L) and getting 7-8 hours of sleep ensures rapid body recovery."
        }

        listOf(bullet1, bullet2)
    }
}
