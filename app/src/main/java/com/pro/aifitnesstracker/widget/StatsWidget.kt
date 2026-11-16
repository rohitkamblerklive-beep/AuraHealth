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

class StatsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = SharedPrefsManager(context)
        val steps = prefs.getStepsToday()
        val calories = (steps * 0.04f).toInt()

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .padding(16.dp)
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PREMIUM STATS",
                            style = TextStyle(color = ColorProvider(Color(0xFF9FA8DA)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.height(16.dp))
                        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$steps",
                                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "STEPS",
                                    style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp)
                                )
                            }
                            Spacer(modifier = GlanceModifier.width(32.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${calories}k",
                                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "CALS",
                                    style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
