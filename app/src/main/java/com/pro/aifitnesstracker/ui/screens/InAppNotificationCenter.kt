package com.pro.aifitnesstracker.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager
import com.pro.aifitnesstracker.utils.BatteryOptimizationHelper
import com.pro.aifitnesstracker.utils.MotivationalQuotes

@Composable
fun InAppNotificationCenter(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    prefsManager: SharedPrefsManager
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Theme palette
    val dialogBg = if (isDark) Color(0xFF0C101B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF141C2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF23304B) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val dividerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val brandCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0284C7)

    val steps = prefsManager.getStepsToday()
    val stepGoal = prefsManager.getStepGoal().coerceAtLeast(1000)
    val water = prefsManager.getWaterGlassesToday()
    val waterGoal = prefsManager.getWaterGoal().coerceAtLeast(6)
    val sleep = prefsManager.getSleepHoursToday()
    val sleepGoal = prefsManager.getSleepGoal().coerceAtLeast(6f)

    val isStepDone = steps >= stepGoal
    val isWaterDone = water >= waterGoal

    val todayQuote = MotivationalQuotes.getQuoteForToday()
    val langCode = prefsManager.getLanguage()
    val quoteText = when (langCode) {
        "hi" -> todayQuote.hindi
        "mr" -> todayQuote.marathi
        else -> todayQuote.english
    }

    val isXiaomiOrVivo = remember { BatteryOptimizationHelper.isXiaomiOrMiui() }
    val needsBatteryOpt = remember { !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                // 1. Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(brandCyan.copy(alpha = if (isDark) 0.16f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = brandCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Notification Center",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = brandCyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "DAILY BRIEF",
                                        color = brandCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Smart nudges, daily targets & insights",
                                fontSize = 11.5.sp,
                                color = textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 2. Mini Health Target Summary Strip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Steps Quick Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.DirectionsWalk,
                                contentDescription = null,
                                tint = if (isStepDone) Color(0xFF10B981) else brandCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "$steps / $stepGoal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = if (isStepDone) "Step Goal Met" else "Steps Today",
                                    fontSize = 10.sp,
                                    color = if (isStepDone) Color(0xFF10B981) else textSecondary
                                )
                            }
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(dividerColor))

                        // Water Quick Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                tint = if (isWaterDone) Color(0xFF10B981) else Color(0xFF0EA5E9),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "$water / $waterGoal Glasses",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = if (isWaterDone) "Hydrated" else "Hydration",
                                    fontSize = 10.sp,
                                    color = if (isWaterDone) Color(0xFF10B981) else textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 3. Scrollable Notification Cards
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Battery Alert
                    if (needsBatteryOpt || isXiaomiOrVivo) {
                        item {
                            NotificationAlertCard(
                                icon = Icons.Rounded.BatteryChargingFull,
                                iconColor = Color(0xFFF59E0B),
                                badgeText = "SYSTEM ALERT",
                                title = "Allow Reliable Reminders",
                                description = if (isXiaomiOrVivo)
                                    "MIUI & background managers may kill notifications. Enable Autostart to get reminders on time."
                                else
                                    "Disable battery optimization so water and workout nudges arrive without delay.",
                                actionText = "Open Settings",
                                isDark = isDark,
                                onAction = {
                                    BatteryOptimizationHelper.openOptimizationSettings(context)
                                }
                            )
                        }
                    }

                    // Hydration Alert
                    item {
                        val waterLeft = (waterGoal - water).coerceAtLeast(0)
                        NotificationAlertCard(
                            icon = Icons.Rounded.WaterDrop,
                            iconColor = Color(0xFF0EA5E9),
                            badgeText = if (isWaterDone) "GOAL COMPLETED" else "HYDRATION REMINDER",
                            title = if (isWaterDone) "Water Target Achieved! 💧" else "Drink Water Nudge",
                            description = if (isWaterDone)
                                "Great work! You reached your $waterGoal glasses target. Keep hydrating throughout the day."
                            else
                                "You've logged $water of $waterGoal glasses. Just $waterLeft more glasses to complete today's quota!",
                            actionText = "Log Water",
                            isDark = isDark,
                            onAction = {
                                onDismiss()
                                onNavigate("WATER")
                            }
                        )
                    }

                    // Step Tracker Alert
                    item {
                        val stepsLeft = (stepGoal - steps).coerceAtLeast(0)
                        NotificationAlertCard(
                            icon = Icons.Rounded.DirectionsWalk,
                            iconColor = Color(0xFF10B981),
                            badgeText = if (isStepDone) "MILESTONE REACHED" else "ACTIVITY GOAL",
                            title = if (isStepDone) "Step Goal Crushed! 🏃" else "Daily Movement Progress",
                            description = if (isStepDone)
                                "Awesome! You completed $steps of $stepGoal steps today. Keep the streak burning!"
                            else
                                "You have logged $steps steps. $stepsLeft steps remaining to reach your $stepGoal step milestone.",
                            actionText = "View Pedometer",
                            isDark = isDark,
                            onAction = {
                                onDismiss()
                                onNavigate("STEP_DETAIL")
                            }
                        )
                    }

                    // Daily Wisdom Card
                    item {
                        NotificationAlertCard(
                            icon = Icons.Rounded.FormatQuote,
                            iconColor = Color(0xFF8B5CF6),
                            badgeText = "DAILY MOTIVATION",
                            title = "Mindset Boost",
                            description = "\"$quoteText\"",
                            actionText = "Share Motivation",
                            isDark = isDark,
                            onAction = {
                                try {
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, "\"$quoteText\"\n\n- via AI Fitness Tracker")
                                        type = "text/plain"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Motivation"))
                                } catch (_: Exception) {}
                            }
                        )
                    }

                    // Yoga & Mindfulness
                    item {
                        NotificationAlertCard(
                            icon = Icons.Rounded.SelfImprovement,
                            iconColor = Color(0xFF14B8A6),
                            badgeText = "WELLNESS GUIDE",
                            title = "Relax & Recharge Yoga",
                            description = "Take 10 minutes for gentle breathing and Sanskrit asanas to release muscle tension.",
                            actionText = "Start Yoga Flow",
                            isDark = isDark,
                            onAction = {
                                onDismiss()
                                onNavigate("YOGA")
                            }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 4. Bottom Dismiss Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandCyan,
                        contentColor = if (isDark) Color(0xFF04101F) else Color.White
                    )
                ) {
                    Text(
                        text = "Close & Keep Moving",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationAlertCard(
    icon: ImageVector,
    iconColor: Color,
    badgeText: String,
    title: String,
    description: String,
    actionText: String,
    isDark: Boolean,
    onAction: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF141C2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF24324D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onAction() },
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Icon + Badge (Proper horizontal layout, NEVER wraps vertically)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = if (isDark) 0.18f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconColor.copy(alpha = if (isDark) 0.18f else 0.10f),
                        border = BorderStroke(0.5.dp, iconColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = badgeText,
                            color = iconColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title (Full width available now)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.5.sp,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))

            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.5.sp,
                color = textSecondary,
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    color = iconColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
