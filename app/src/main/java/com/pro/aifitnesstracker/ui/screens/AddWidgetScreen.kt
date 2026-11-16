package com.pro.aifitnesstracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWidgetScreen(
    prefsManager: SharedPrefsManager,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val topBarBg = if (isDark) Color(0xFF080B14) else Color(0xFFEEF2F6)
    val canvasBgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF080B14), Color(0xFF0D121F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEEF2F6), Color(0xFFE2E8F0)))
    }
    val cardBg = if (isDark) Color(0xFF131A2A) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF1F293D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    var smallStepWidget by remember { mutableStateOf(prefsManager.isSmallStepWidgetAdded()) }
    var mediumStepWidget by remember { mutableStateOf(prefsManager.isMediumStepWidgetAdded()) }
    var waterWidget by remember { mutableStateOf(prefsManager.isWaterWidgetAdded()) }

    Scaffold(
        containerColor = topBarBg,
        topBar = {
            Surface(
                color = topBarBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = strings.addWidget,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = textPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(canvasBgGradient)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Instructions Box
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "How to add widgets to your home screen:",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Go to your home screen.\n2. Long press on any empty space.\n3. Tap on 'Widgets' or 'Add Widget'.\n4. Find AIFitnessTracker and drag the widget onto your screen.",
                            color = textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Available Widgets",
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Small Step Widget Configure
            WidgetConfigureCard(
                title = "Small Step Counter (1x1)",
                description = "Shows your current daily step count progress in a compact layout.",
                isEnabled = smallStepWidget,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onCheckedChange = { checked ->
                    smallStepWidget = checked
                    prefsManager.setSmallStepWidgetAdded(checked)
                }
            )

            // Medium Step Widget Configure
            WidgetConfigureCard(
                title = "Detailed Step Counter (2x2)",
                description = "Shows active steps count, distance, and calorie progress graph.",
                isEnabled = mediumStepWidget,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onCheckedChange = { checked ->
                    mediumStepWidget = checked
                    prefsManager.setMediumStepWidgetAdded(checked)
                }
            )

            // Water Tracker Widget Configure
            WidgetConfigureCard(
                title = "Quick Hydration Tracker (2x1)",
                description = "Quick-log glasses of water consumed directly from the home screen.",
                isEnabled = waterWidget,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onCheckedChange = { checked ->
                    waterWidget = checked
                    prefsManager.setWaterWidgetAdded(checked)
                }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun WidgetConfigureCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = textSecondary,
                        uncheckedTrackColor = cardBorder
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                color = textSecondary,
                fontSize = 13.sp,
                lineHeight = 16.sp
            )
        }
    }
}

