package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import com.pro.aifitnesstracker.utils.BatteryOptimizationHelper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import java.util.*

private fun getLocalizedDayName(day: String, strings: AppStrings): String {
    return when (day) {
        "Sun" -> strings.sun
        "Mon" -> strings.mon
        "Tue" -> strings.tue
        "Wed" -> strings.wed
        "Thu" -> strings.thu
        "Fri" -> strings.fri
        "Sat" -> strings.sat
        else -> day
    }
}

@Composable
fun ReminderScreen(
    onBack: () -> Unit, 
    prefsManager: SharedPrefsManager,
    onScheduleReminders: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var isEnabled by remember { mutableStateOf(prefsManager.isDailyReminderEnabled()) }
    var isWaterEnabled by remember { mutableStateOf(prefsManager.isWaterReminderEnabled()) }
    var time by remember { mutableStateOf(prefsManager.getReminderTime()) }
    var repeatDays by remember { mutableStateOf(prefsManager.getReminderDays()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDaysDialog by remember { mutableStateOf(false) }
    var waterInterval by remember { mutableStateOf(prefsManager.getWaterReminderInterval()) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (showIntervalDialog) {
        SettingsListDialog(
            title = "Water Reminder Interval",
            options = listOf("1 Hour", "2 Hours", "3 Hours", "4 Hours"),
            onDismiss = { showIntervalDialog = false },
            onSelect = { option ->
                waterInterval = option.replace(" Hour", "").replace("s", "").toInt()
                showIntervalDialog = false
            }
        )
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance()
        android.app.TimePickerDialog(context, { _, hour, minute ->
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            time = String.format("%02d:%02d %s", displayHour, minute, amPm)
            showTimePicker = false
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        showTimePicker = false
    }

    val localizedRepeatDays = remember(repeatDays, strings) {
        if (repeatDays.isBlank()) ""
        else repeatDays.split(", ").map { getLocalizedDayName(it, strings) }.joinToString(", ")
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    if (showDaysDialog) {
        val allDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val selectedDaysList = remember { repeatDays.split(", ").toMutableStateList() }
        AlertDialog(
            onDismissRequest = { showDaysDialog = false },
            title = { Text(strings.selectDays, color = MaterialTheme.colorScheme.onSurface) },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column {
                    allDays.forEach { day ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { if (selectedDaysList.contains(day)) selectedDaysList.remove(day) else selectedDaysList.add(day) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selectedDaysList.contains(day), onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = primaryColor))
                            Spacer(Modifier.width(8.dp))
                            Text(getLocalizedDayName(day, strings), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { repeatDays = allDays.filter { selectedDaysList.contains(it) }.joinToString(", "); showDaysDialog = false }) { Text(strings.ok, color = primaryColor) } },
            dismissButton = { TextButton(onClick = { showDaysDialog = false }) { Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
    }

    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
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
    val cardBg = if (isDarkTheme) Color(0xFF141C2E) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF24324D) else Color(0xFFE2E8F0)
    val topBarBg = if (isDarkTheme) Color(0xFF080B14) else Color(0xFFEEF2F6)

    Scaffold(
        containerColor = if (isDarkTheme) Color(0xFF080B14) else Color(0xFFEEF2F6),
        topBar = {
            Surface(
                color = topBarBg,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription=strings.back, tint=primaryColor) }
                        Column {
                            Text(strings.reminderTitle, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("SMART NUDGES • WORKMANAGER", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f))
                            .border(1.dp, primaryColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEnabled || isWaterEnabled) "ACTIVE" else "MUTED",
                            color = primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
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
                .background(canvasBgGradient)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.dailyReportReminder, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it; prefsManager.setDailyReminderEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = primaryColor))
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.smartWaterReminders, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(if (waterInterval == 1) "Get notified every hour" else "Get notified every $waterInterval hours", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                        Switch(checked = isWaterEnabled, onCheckedChange = { isWaterEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = primaryColor))
                    }
                    if (isWaterEnabled) {
                        HorizontalDivider(color = cardBorder, modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showIntervalDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reminder Interval", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(if (waterInterval == 1) "1 Hour" else "$waterInterval Hours", color = primaryColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(strings.time.uppercase(java.util.Locale.getDefault()), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(time, color = primaryColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Change", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                    HorizontalDivider(color = cardBorder, modifier = Modifier.padding(vertical = 12.dp))
                    Text(strings.repeat.uppercase(java.util.Locale.getDefault()), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().clickable { showDaysDialog = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(localizedRepeatDays, color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Select Days", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Battery Optimization & Background Execution Card
            val isXiaomiOrVivo = remember { BatteryOptimizationHelper.isXiaomiOrMiui() }
            val isIgnoringBattery = remember { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context) }
            val warnColor = Color(0xFFFFB300)
            val successColor = Color(0xFF4CAF50)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(
                        BorderStroke(1.dp, if (!isIgnoringBattery || isXiaomiOrVivo) warnColor.copy(alpha = 0.6f) else cardBorder),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isIgnoringBattery) successColor.copy(alpha = 0.15f) else warnColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIgnoringBattery) Icons.Rounded.CheckCircle else Icons.Rounded.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = if (isIgnoringBattery) successColor else warnColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Reliable Background Alerts",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isIgnoringBattery && !isXiaomiOrVivo) "Active • Battery Whitelisted" else "Background Restrictions Active",
                                    color = if (isIgnoringBattery && !isXiaomiOrVivo) successColor else warnColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (isXiaomiOrVivo)
                            "Xiaomi / MIUI, Vivo and Oppo phones kill background reminders aggressively. Please enable 'Autostart' and set Battery to 'No restrictions' so hydration & workout alerts are never delayed."
                        else
                            "Allow background execution to ensure smart health nudges and water reminders are delivered on schedule even when the phone is sleeping.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { BatteryOptimizationHelper.openOptimizationSettings(context) },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(21.dp),
                        border = BorderStroke(1.dp, if (!isIgnoringBattery || isXiaomiOrVivo) warnColor else primaryColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (!isIgnoringBattery || isXiaomiOrVivo) warnColor else primaryColor)
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isXiaomiOrVivo) "Open Autostart & Battery Settings" else "Disable Battery Optimization",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = {
                    val request = androidx.work.OneTimeWorkRequestBuilder<DailyReminderWorker>()
                        .setInputData(androidx.work.Data.Builder().putBoolean("is_test", true).build())
                        .build()
                    WorkManager.getInstance(context).enqueue(request)
                    android.widget.Toast.makeText(context, strings.aiReminderTriggered, android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, cardBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
            ) {
                Text(strings.testAiReminder, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    prefsManager.setDailyReminderEnabled(isEnabled)
                    prefsManager.setReminderTime(time)
                    prefsManager.setReminderDays(repeatDays)
                    prefsManager.setWaterReminderEnabled(isWaterEnabled)
                    prefsManager.setWaterReminderInterval(waterInterval)
                    
                    onScheduleReminders()
                    
                    if (isEnabled) {
                        android.widget.Toast.makeText(context, strings.reminderScheduledAt.format(time), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(strings.save.uppercase(java.util.Locale.getDefault()), color = MaterialTheme.colorScheme.onPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(110.dp))
        }
    }
}

