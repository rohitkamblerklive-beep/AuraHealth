package com.pro.aifitnesstracker.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import com.pro.aifitnesstracker.MainActivity
import com.pro.aifitnesstracker.SharedPrefsManager

class FitnessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FitnessWidget()
}

class FitnessWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Use standard preferences context for the widget
        val prefs = SharedPrefsManager(context.createDeviceProtectedStorageContext())
        
        provideContent {
            val steps = prefs.getStepsToday()
            val stepGoal = prefs.getStepGoal()
            val water = prefs.getWaterGlassesToday()
            
            val bg = ColorProvider(Color(0xFF0F172A))
            val surface = ColorProvider(Color(0xFF1E293B))
            val accent = ColorProvider(Color(0xFF9FA8DA))
            val textPrimary = ColorProvider(Color.White)
            val textSecondary = ColorProvider(Color(0xFFB0BEC5))
            
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AuraHealth",
                    style = TextStyle(color = accent, fontWeight = FontWeight.Bold)
                )
                
                Spacer(modifier = GlanceModifier.height(12.dp))
                
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Steps Card
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(surface)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👣 Steps", style = TextStyle(color = textSecondary))
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "$steps",
                            style = TextStyle(color = textPrimary, fontWeight = FontWeight.Bold)
                        )
                        Text("Goal: $stepGoal", style = TextStyle(color = accent))
                    }
                    
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    
                    // Water Card
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(surface)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💧 Water", style = TextStyle(color = textSecondary))
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "$water Glasses",
                            style = TextStyle(color = textPrimary, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
