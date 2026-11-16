package com.pro.aifitnesstracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.LocalAppStrings

private data class InstructionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(onBack: () -> Unit) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var expandedItem by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Theme tokens aligned with AuraHealth Design System
    val screenBg = if (isDark) Color(0xFF090D16) else Color(0xFFF6F8FC)
    val cardBg = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentCyan = Color(0xFF00E5FF)
    val accentEmerald = Color(0xFF10B981)

    val items = remember {
        listOf(
            // Sensor & Tracking
            InstructionItem(
                title = "How does step counting work?",
                description = "Simply keep your phone in your pocket or hand while walking. AuraHealth leverages your phone's hardware step sensor and accelerometer with real-time digital filtering to track every step with precision.",
                icon = Icons.Rounded.DirectionsWalk,
                category = "SENSORS"
            ),
            InstructionItem(
                title = "Ensuring high accuracy",
                description = "For best results, keep your phone in your pocket or an armband. When loose inside a backpack or purse, secondary swinging can introduce slight variance.",
                icon = Icons.Rounded.GpsFixed,
                category = "SENSORS"
            ),
            InstructionItem(
                title = "Counting when shaking phone?",
                description = "Our proprietary cadence filter analyzes rhythmic frequency and acceleration thresholds. Random shakes, fidgeting, or rapid desk bumps are discarded.",
                icon = Icons.Rounded.Vibration,
                category = "SENSORS"
            ),
            InstructionItem(
                title = "Counting when driving or riding?",
                description = "AuraHealth continuously checks movement speed and vibration signatures to automatically pause step recording while you are in a moving car, bus, or train.",
                icon = Icons.Rounded.DirectionsCar,
                category = "SENSORS"
            ),
            // Battery & Performance
            InstructionItem(
                title = "Prevent background sleep & kill",
                description = "Some manufacturers (Xiaomi, Samsung, OnePlus, Huawei) aggressively freeze background apps. Please disable 'Battery Optimization' and enable 'Autostart' or 'Background Activity' for AuraHealth.",
                icon = Icons.Rounded.BatteryChargingFull,
                category = "BATTERY"
            ),
            InstructionItem(
                title = "Ultra-low battery consumption",
                description = "AuraHealth relies on native hardware sensor batching instead of continuous GPS where possible, consuming less than 1.5% to 2% battery over a full 24-hour day.",
                icon = Icons.Rounded.EnergySavingsLeaf,
                category = "BATTERY"
            ),
            // Customization & Units
            InstructionItem(
                title = "Switch distance units (km / miles)",
                description = "Head to Settings > Distance Unit to toggle between Metric (km, kg) and Imperial (miles, lbs) systems. Your past history recalculates automatically.",
                icon = Icons.Rounded.Straighten,
                category = "SETTINGS"
            ),
            InstructionItem(
                title = "How are calories computed?",
                description = "Active burn is calculated using your weight, height, step pace, and estimated stride length based on validated metabolic equivalent of task (MET) health formulas.",
                icon = Icons.Rounded.LocalFireDepartment,
                category = "SETTINGS"
            ),
            InstructionItem(
                title = "Adjusting your daily step goal",
                description = "Tap on the Step Goal card in Settings to adjust your daily target from 3,000 to 20,000+ steps. Celebrate milestone achievements every time you hit your target!",
                icon = Icons.Rounded.EmojiEvents,
                category = "SETTINGS"
            )
        )
    }

    val categories = listOf("ALL", "SENSORS", "BATTERY", "SETTINGS")
    val filteredItems = if (selectedCategory == "ALL") items else items.filter { it.category == selectedCategory }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.instructions,
                            color = textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Sensor guide, tips & calibration",
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBg
                )
            )
        },
        bottomBar = {
            AdBanner(adKey = "ad_instructions_banner")
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Guide Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(accentCyan.copy(alpha = 0.4f), accentEmerald.copy(alpha = 0.2f))))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(accentCyan.copy(alpha = 0.2f), accentEmerald.copy(alpha = 0.2f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = null,
                                tint = accentCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AuraHealth Support Guide",
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Everything you need to know about tracking, sensor accuracy, and battery conservation.",
                                color = textSecondary,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Category Filter Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val pillBg = if (isSelected) {
                            Brush.horizontalGradient(listOf(accentCyan, Color(0xFF00B0FF)))
                        } else {
                            Brush.linearGradient(listOf(cardBg, cardBg))
                        }
                        val pillTextColor = if (isSelected) Color(0xFF080B14) else textSecondary

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(pillBg)
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else cardBorder,
                                    RoundedCornerShape(50)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                color = pillTextColor,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Accordion Items
            itemsIndexed(filteredItems) { _, item ->
                val isExpanded = expandedItem == item.title
                val rotationAngle by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = tween(250),
                    label = "chevron_rotation"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            expandedItem = if (isExpanded) null else item.title
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(
                        1.dp,
                        if (isExpanded) accentCyan.copy(alpha = 0.5f) else cardBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isExpanded) accentCyan.copy(alpha = 0.15f) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (isExpanded) accentCyan else textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = item.title,
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = if (isExpanded) accentCyan else textSecondary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(rotationAngle)
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp, start = 48.dp, end = 4.dp)) {
                                HorizontalDivider(
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                Text(
                                    text = item.description,
                                    color = textSecondary,
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Contact Support Footer Card
            item {
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Still have questions?",
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Our fitness and wellness engineering team is here to assist you anytime.",
                            color = textSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@aurahealth.app")).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, "AuraHealth Support & Inquiries")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No email client found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentCyan,
                                contentColor = Color(0xFF080B14)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MailOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Contact AuraHealth Support",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
