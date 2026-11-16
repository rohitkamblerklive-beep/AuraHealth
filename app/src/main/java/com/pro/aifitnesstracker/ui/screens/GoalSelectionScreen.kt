package com.pro.aifitnesstracker.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.LocalAppStrings

@Composable
fun GoalSelectionScreen(onContinue: () -> Unit) {
    val strings = LocalAppStrings.current
    val goals = listOf(
        Pair(Icons.AutoMirrored.Filled.DirectionsRun, strings.goalActivity),
        Pair(Icons.Default.MonitorWeight, strings.goalWeight),
        Pair(Icons.Default.Favorite, strings.goalHeart),
        Pair(Icons.Default.TrackChanges, strings.goalHabits),
        Pair(Icons.Default.SelfImprovement, strings.goalStress)
    )
    var selectedGoals by remember { mutableStateOf(setOf<String>()) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Surface(
                color = backgroundColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onContinue) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    TextButton(onClick = onContinue) {
                        Text(
                            text = strings.skip,
                            color = primaryColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = cardColor,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "CONTINUE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = strings.goalsTitle,
                color = textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 36.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = strings.goalsSubtitle,
                color = textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                goals.forEach { (icon, title) ->
                    val isSelected = selectedGoals.contains(title)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                selectedGoals = if (isSelected) selectedGoals - title else selectedGoals + title
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) primaryColor.copy(alpha = 0.12f) else cardColor,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) primaryColor else borderColor
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) primaryColor.copy(alpha = 0.2f) else surfaceAltColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) primaryColor else textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            Text(
                                text = title,
                                color = if (isSelected) textPrimary else textSecondary,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
