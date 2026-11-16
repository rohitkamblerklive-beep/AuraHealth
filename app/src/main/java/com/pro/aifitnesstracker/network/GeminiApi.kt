package com.pro.aifitnesstracker.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.pro.aifitnesstracker.ui.screens.NutrientInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object GeminiApi {

    private const val API_KEY = "AIzaSyBD7Rn5NdSCJrFk6af1TP60eBgV_iT4Z84"
    private const val TAG     = "GeminiApi"

    // Models tried in order — first available one wins
    private val MODELS = listOf(
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-flash-lite-latest",
        "gemini-2.0-flash-lite",
        "gemini-2.0-flash"
    )

    private fun endpointFor(model: String) =
        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$API_KEY"

    // ─── Public entry point ───────────────────────────────────────────────────
    @Throws(Exception::class)
    fun analyzeFood(bitmap: Bitmap): NutrientInfo {
        val b64Image = bitmapToBase64(bitmap)
        val requestJson = buildRequestJson(b64Image)
        val bodyBytes   = requestJson.toString().toByteArray(Charsets.UTF_8)

        // Try each model; skip on 404/429, throw only if all fail
        var lastError = "No models available. Try again later."
        for (model in MODELS) {
            Log.d(TAG, "Trying model: $model")
            val result = tryModel(model, bodyBytes)
            when {
                result.isSuccess -> return result.getOrThrow()
                result.exceptionOrNull()?.message?.startsWith("SKIP") == true -> {
                    // 404 or 429 → try next model
                    lastError = result.exceptionOrNull()!!.message!!.removePrefix("SKIP:")
                    Log.w(TAG, "Skipping $model: $lastError")
                    continue
                }
                else -> throw result.exceptionOrNull()!! // real error — propagate
            }
        }

        // All models exhausted
        throw Exception("Rate limit hit on all models.\nPlease wait 1 minute and try again.\nOr try again tomorrow if daily quota is exceeded.")
    }

    // ─── Text Generation Entry Point ─────────────────────────────────────────
    @Throws(Exception::class)
    fun generateText(prompt: String): String {
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }
        val bodyBytes = requestJson.toString().toByteArray(Charsets.UTF_8)
        for (model in MODELS) {
            try {
                val conn = (URL(endpointFor(model)).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput        = true
                    connectTimeout  = 15_000
                    readTimeout     = 20_000
                }
                conn.outputStream.use { it.write(bodyBytes) }
                val code = conn.responseCode
                if (code == 200) {
                    val raw = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val respObj = JSONObject(raw)
                    val candidates = respObj.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""
                        if (text.isNotBlank()) return text.trim()
                    }
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "generateText error on $model: ${e.message}")
            }
        }
        throw Exception("All Gemini models exhausted for text generation.")
    }

    // ─── Try a single model ───────────────────────────────────────────────────
    private fun tryModel(model: String, bodyBytes: ByteArray): Result<NutrientInfo> {
        return try {
            val conn = (URL(endpointFor(model)).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput        = true
                connectTimeout  = 20_000
                readTimeout     = 30_000
            }
            conn.outputStream.use { it.write(bodyBytes) }

            val code = conn.responseCode
            Log.d(TAG, "$model response: $code")

            if (code == 200) {
                val raw = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                Result.success(parseGeminiResponse(raw))
            } else {
                val errBody = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                conn.disconnect()
                Log.w(TAG, "$model error $code: ${errBody.take(200)}")
                when (code) {
                    404  -> Result.failure(Exception("SKIP:Model $model not found"))
                    429  -> Result.failure(Exception("SKIP:Rate limit on $model"))
                    400  -> Result.failure(Exception("Bad request — try a clearer photo of food."))
                    401  -> Result.failure(Exception("Invalid API key. Go to aistudio.google.com to get a valid key."))
                    403  -> Result.failure(Exception("API key permission denied. Check your Google Cloud project."))
                    500, 503 -> Result.failure(Exception("SKIP:Server error on $model"))
                    else -> Result.failure(Exception("API error $code. Check internet and try again."))
                }
            }
        } catch (e: Exception) {
            if (e.message?.startsWith("SKIP") == true || e.message?.startsWith("Bad") == true
                || e.message?.startsWith("Invalid") == true || e.message?.startsWith("API key") == true) {
                Result.failure(e)
            } else {
                Result.failure(Exception("SKIP:Network error — ${e.message}"))
            }
        }
    }

    // ─── Build Gemini request JSON ────────────────────────────────────────────
    private fun buildRequestJson(b64Image: String): JSONObject {
        val prompt = """
            You are a nutritionist AI. Analyze this food image carefully.
            Return ONLY a valid JSON object — no markdown, no explanation, no code fences.
            Use this exact structure:
            {
              "foodName": "Full name of the food",
              "servingSize": "Estimated serving (e.g. 1 cup, 200g)",
              "calories": 350,
              "protein": 12.5,
              "carbs": 45.0,
              "fat": 10.0,
              "fiber": 3.5,
              "sugar": 8.0,
              "healthScore": 72,
              "tags": ["Vegetarian", "High Protein", "Low Fat"]
            }
            Rules:
            - healthScore: 0 (very unhealthy) to 100 (very healthy)
            - All numeric values must be realistic for a typical serving
            - tags: 2-4 relevant food property tags
        """.trimIndent()

        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", b64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 600)
                put("topP", 0.8)
            })
            put("safetySettings", JSONArray().apply {
                listOf(
                    "HARM_CATEGORY_HARASSMENT",
                    "HARM_CATEGORY_HATE_SPEECH",
                    "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                    "HARM_CATEGORY_DANGEROUS_CONTENT"
                ).forEach { cat ->
                    put(JSONObject().apply {
                        put("category", cat)
                        put("threshold", "BLOCK_NONE")
                    })
                }
            })
        }
    }

    // ─── Parse Gemini response ────────────────────────────────────────────────
    private fun parseGeminiResponse(raw: String): NutrientInfo {
        Log.d(TAG, "Parsing: ${raw.take(300)}")
        val respObj    = JSONObject(raw)
        val candidates = respObj.optJSONArray("candidates")
            ?: throw Exception("Empty response from Gemini. Try again.")

        val candidate = candidates.getJSONObject(0)
        if (candidate.optString("finishReason") == "SAFETY") {
            throw Exception("Photo blocked by safety filter. Try a clearer food photo.")
        }

        val text = candidate
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(text)
        val tagsArray = json.optJSONArray("tags")
        val tags = if (tagsArray != null) List(tagsArray.length()) { i -> tagsArray.getString(i) }
                   else emptyList()

        return NutrientInfo(
            foodName    = json.optString("foodName",    "Unknown Food"),
            servingSize = json.optString("servingSize", "1 serving"),
            calories    = json.optInt   ("calories",    0),
            protein     = json.optDouble("protein",     0.0).toFloat(),
            carbs       = json.optDouble("carbs",       0.0).toFloat(),
            fat         = json.optDouble("fat",         0.0).toFloat(),
            fiber       = json.optDouble("fiber",       0.0).toFloat(),
            sugar       = json.optDouble("sugar",       0.0).toFloat(),
            healthScore = json.optInt   ("healthScore", 50),
            tags        = tags,
            source      = "Gemini AI"
        )
    }

    // ─── Encode bitmap → base64 JPEG ─────────────────────────────────────────
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = scaleBitmap(bitmap, 768)
        val baos   = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxPx: Int): Bitmap {
        val w = bitmap.width; val h = bitmap.height
        if (w <= maxPx && h <= maxPx) return bitmap
        val scale = maxPx.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}
