package com.pro.aifitnesstracker.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pro.aifitnesstracker.MainActivity
import com.pro.aifitnesstracker.R
import com.pro.aifitnesstracker.SharedPrefsManager

class FitnessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FitnessWidget()
}

/**
 * 4x2 Wide Health & Yoga Dashboard Home Screen Widget for AuraHealth.
 * Features:
 * - App's official logo + Branding
 * - Left Panel: Real-time Steps progress ratio, Active Calories, and Water glasses
 * - Right Panel: Daily Yoga recommendation with 1-tap "START" session launcher
 */
class FitnessWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = SharedPrefsManager(context.createDeviceProtectedStorageContext())
        
        val steps = prefs.getStepsToday()
        val stepGoal = if (prefs.getStepGoal() > 0) prefs.getStepGoal() else 10000
        val calories = (steps * 0.045f).toInt()
        val water = prefs.getWaterGlassesToday()
        val activeJourneyId = prefs.getActiveJourneyId()
        
        val yogaTitle = when (activeJourneyId) {
            "mental_calm" -> "Mental Calm"
            "energy_boost" -> "Energy Booster"
            "healing_detox" -> "Healing & Detox"
            else -> "Morning Flow"
        }
        val yogaDuration = "15m"

        provideContent {
            GlanceTheme {
                val bg = ColorProvider(Color(0xFF0F172A))          // Slate-900 dark background
                val cardSurface = ColorProvider(Color(0xFF1E293B)) // Slate-800 card surface
                val emeraldAccent = ColorProvider(Color(0xFF10B981))// Fresh emerald green
                val textPrimary = ColorProvider(Color.White)
                val textSecondary = ColorProvider(Color(0xFF94A3B8))

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(bg)
                        .cornerRadius(24.dp)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        
                        // Header Bar: Official App Logo + App Title
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.app_logo),
                                contentDescription = "AuraHealth Logo",
                                modifier = GlanceModifier
                                    .size(22.dp)
                                    .cornerRadius(6.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = "AuraHealth Dashboard",
                                style = TextStyle(
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Dual-Panel Split View (Left: Health Vitals, Right: Yoga Session)
                        Row(
                            modifier = GlanceModifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            
                            // ── LEFT PANEL: Health & Activity ──────────────────
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                                    .background(cardSurface)
                                    .cornerRadius(16.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$steps",
                                        style = TextStyle(
                                            color = emeraldAccent,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = " / $stepGoal",
                                        style = TextStyle(
                                            color = textSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                
                                Text(
                                    text = "STEPS TODAY",
                                    style = TextStyle(
                                        color = textSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = GlanceModifier.height(6.dp))

                                // Metrics Row (Calories & Water)
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔥 ${calories} kcal",
                                        style = TextStyle(
                                            color = textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.width(10.dp))
                                    Text(
                                        text = "💧 $water glasses",
                                        style = TextStyle(
                                            color = textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = GlanceModifier.width(10.dp))

                            // ── RIGHT PANEL: Daily Yoga Recommendation ─────────
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                                    .background(cardSurface)
                                    .cornerRadius(16.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🧘 DAILY YOGA",
                                    style = TextStyle(
                                        color = textSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = "$yogaTitle • $yogaDuration",
                                    style = TextStyle(
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = GlanceModifier.height(8.dp))

                                // "START" Action Pill
                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .background(emeraldAccent)
                                        .cornerRadius(12.dp)
                                        .padding(vertical = 5.dp)
                                        .clickable(actionStartActivity<MainActivity>()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "▶ START SESSION",
                                        style = TextStyle(
                                            color = ColorProvider(Color.Black),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
