package com.pro.aifitnesstracker.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pro.aifitnesstracker.SharedPrefsManager
import java.io.InputStream
import org.json.JSONArray
import java.util.Calendar

class YogaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = SharedPrefsManager(context)
        val activeJourneyId = prefs.getActiveJourneyId()
        
        val zenQuotes = listOf(
            "Quiet the mind and the soul will speak.",
            "Conscious breathing is my anchor.",
            "Within you, there is a stillness and a sanctuary.",
            "Mindfulness isn't difficult, we just need to remember.",
            "Do not dwell in the past, concentrate on the present.",
            "The present moment is filled with joy.",
            "Rule your mind or it will rule you.",
            "When the mind is calm, everything becomes clear."
        )
        
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val todayQuote = zenQuotes[dayOfYear % zenQuotes.size]

        var title = "Daily Zen Quote"
        var subtitle = todayQuote
        var accentColor = Color(0xFF9FA8DA)

        if (activeJourneyId.isNotEmpty()) {
            val progressDay = prefs.getJourneyProgressDay(activeJourneyId)
            val journeyName = when (activeJourneyId) {
                "mental_calm" -> "Mental Calm"
                "energy_boost" -> "Energy Booster"
                else -> "Healing & Detox"
            }
            accentColor = when (activeJourneyId) {
                "mental_calm" -> Color(0xFFB39DDB)
                "energy_boost" -> Color(0xFFFFB74D)
                else -> Color(0xFF4DB6AC)
            }
            title = "$journeyName • Day ${progressDay + 1}"

            try {
                val inputStream: InputStream = context.assets.open("yoga_mudras.json")
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                val jsonString = String(buffer, Charsets.UTF_8)
                val jsonArray = JSONArray(jsonString)

                val filterCategory = when (activeJourneyId) {
                    "mental_calm" -> "Mental Calm"
                    "energy_boost" -> "Energy & Vitality"
                    else -> "Healing & Therapy"
                }

                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val cat = obj.getString("category")
                    if (cat == filterCategory || 
                        (activeJourneyId == "mental_calm" && cat == "Brain & Focus") ||
                        (activeJourneyId == "energy_boost" && cat == "Heart & Circulation") ||
                        (activeJourneyId == "healing_detox" && cat == "Digestion & Detox")) {
                        list.add(obj.getString("name"))
                    }
                }
                if (list.isNotEmpty()) {
                    val mudraName = list[progressDay % list.size]
                    subtitle = "Meditation: $mudraName"
                } else {
                    subtitle = "Today's Yoga Mudra Challenge"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                subtitle = "Today's Yoga Mudra Challenge"
            }
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "YOGA MINDFULNESS",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF9FA8DA)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = title.uppercase(),
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = subtitle,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}
