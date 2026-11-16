package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoreSettingsScreen(onBack: () -> Unit, prefsManager: SharedPrefsManager) {
    val strings = LocalAppStrings.current
    var weight by remember { mutableStateOf(prefsManager.getWeight().toInt().toString() + " lbs") }
    var height by remember { mutableStateOf(prefsManager.getHeightStr()) }
    var gender by remember { mutableStateOf(prefsManager.getGender()) }
    var age by remember { mutableStateOf(prefsManager.getAge().toString()) }
    var firstDay by remember { mutableStateOf(prefsManager.getFirstDayOfWeek()) }
    var sensitivity by remember { mutableStateOf(prefsManager.getSensitivity()) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showHeightDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showAgeDialog by remember { mutableStateOf(false) }
    var showFirstDayDialog by remember { mutableStateOf(false) }
    var showSensitivityDialog by remember { mutableStateOf(false) }

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

    if (showWeightDialog) SettingsListDialog("Weight", (80..300 step 5).map { "$it lbs" }, onDismiss = { showWeightDialog = false }) { weight = it; prefsManager.setWeight(it.replace(" lbs", "").toInt()); showWeightDialog = false }
    if (showHeightDialog) SettingsListDialog("Height", listOf("4 ft 10 in", "5 ft 0 in", "5 ft 2 in", "5 ft 4 in", "5 ft 6 in", "5 ft 8 in", "5 ft 10 in", "6 ft 0 in"), onDismiss = { showHeightDialog = false }) { height = it; prefsManager.setHeightStr(it); showHeightDialog = false }
    if (showGenderDialog) SettingsListDialog("Gender", listOf("Male", "Female", "Other"), onDismiss = { showGenderDialog = false }) { gender = it; prefsManager.setGender(it); showGenderDialog = false }
    if (showAgeDialog) SettingsListDialog("Age", (15..80).map { it.toString() }, onDismiss = { showAgeDialog = false }) { age = it; prefsManager.setAge(it.toInt()); showAgeDialog = false }
    if (showFirstDayDialog) SettingsListDialog("First day of week", listOf("Sunday", "Monday"), onDismiss = { showFirstDayDialog = false }) { firstDay = it; prefsManager.setFirstDayOfWeek(it); showFirstDayDialog = false }
    if (showSensitivityDialog) SettingsListDialog("Step Counter Sensitivity", listOf("Low", "Medium", "High"), onDismiss = { showSensitivityDialog = false }) { sensitivity = it; prefsManager.setSensitivity(it); showSensitivityDialog = false }

    Scaffold(
        containerColor = if (isDarkTheme) Color(0xFF080B14) else Color(0xFFEEF2F6),
        topBar = {
            Surface(
                color = topBarBg,
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription="Back", tint=MaterialTheme.colorScheme.primary) }
                    Text(strings.more, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
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
            Text(
                "PERSONAL INFORMATION",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    MoreSettingItem(strings.weight, weight, "Calories calculation needs it") { showWeightDialog = true }
                    HorizontalDivider(color = cardBorder, modifier = Modifier.padding(vertical = 12.dp))
                    MoreSettingItem("Height / Step length", height, "Distance & speed calculation needs it") { showHeightDialog = true }
                    HorizontalDivider(color = cardBorder, modifier = Modifier.padding(vertical = 12.dp))
                    MoreSettingItem("Gender", gender, "Stride length calculation needs it") { showGenderDialog = true }
                    HorizontalDivider(color = cardBorder, modifier = Modifier.padding(vertical = 12.dp))
                    MoreSettingItem("Age", age, "Calories & stride length calculation needs it") { showAgeDialog = true }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "SENSORS & TRACKING",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) { 
                Column {
                    MoreSettingItem("Step Counter Sensitivity", sensitivity, "Filters out vehicle vibrations (Low/Medium/High)") { showSensitivityDialog = true }
                    HorizontalDivider(color = cardBorder, modifier = Modifier.padding(vertical = 12.dp))
                    MoreSettingItem("First day of week", firstDay, "Start day for weekly step & calorie analytics") { showFirstDayDialog = true } 
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun MoreSettingItem(title: String, value: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(" ", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        }
    }
}

@Composable
fun SettingsListDialog(
    title: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    Text(
                        text = option,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}


