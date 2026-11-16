package com.pro.aifitnesstracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pro.aifitnesstracker.LocalAppStrings
import com.pro.aifitnesstracker.SharedPrefsManager

data class BadgeData(
    val id: String,
    val name: String,
    val desc: String,
    val color: Color,
    val category: String = "General",
    val xp: Int = 150
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    prefsManager: SharedPrefsManager,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val unlockedBadges = prefsManager.getUnlockedBadges()

    val allBadges = listOf(
        BadgeData("10K_STEPS", "10K Warrior", "Walk 10,000 steps in a day", Color(0xFFFFD700), "Activity", 200),
        BadgeData("20K_STEPS", "Marathoner", "Walk 20,000 steps in a day", Color(0xFFC0C0C0), "Activity", 300),
        BadgeData("WATER_HERO", "Hydration Hero", "Drink 8 glasses of water", Color(0xFF38BDF8), "Hydration", 150),
        BadgeData("STREAK_3", "Consistency King", "Maintain a 3-day streak", Color(0xFFF97316), "Activity", 250),
        BadgeData("SQUAT_MASTER", "Leg Day Champ", "Complete 50 squats in one session", Color(0xFF84CC16), "Activity", 180),
        BadgeData("HEART_HEALTHY", "Heart Monitor", "Measure heart rate 5 times", Color(0xFFF43F5E), "Activity", 150),
        BadgeData("MUDRA_JOURNEY_START", "Mudra Novice", "Start your first Yoga Mudra", Color(0xFFA78BFA), "Yoga", 120),
        BadgeData("MUDRA_ROUTINE_BUILDER", "Mudra Builder", "Practice 3 or more Mudras", Color(0xFF06B6D4), "Yoga", 180),
        BadgeData("MUDRA_ZEN_MASTER", "Zen Master", "Complete full yoga sessions", Color(0xFFFBBF24), "Yoga", 250),
        BadgeData("MUDRA_JOURNEY_COMPLETE", "Yoga Champion", "Complete all mudra sessions", Color(0xFF10B981), "Yoga", 350)
    )

    val unlockedCount = allBadges.count { unlockedBadges.contains(it.id) }
    val totalXp = allBadges.filter { unlockedBadges.contains(it.id) }.sumOf { it.xp }
    val athleteLevel = 1 + (totalXp / 300)

    val nextBadgeToUnlock = allBadges.firstOrNull { !unlockedBadges.contains(it.id) }

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedBadgeForDetail by remember { mutableStateOf<BadgeData?>(null) }

    val filteredBadges = remember(selectedFilter) {
        if (selectedFilter == "All") allBadges else allBadges.filter { it.category == selectedFilter }
    }

    // Color Theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF080B14) else Color(0xFFF4F7FB)
    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    val goldAccent = Color(0xFFF59E0B)
    val purpleAccent = Color(0xFFA855F7)
    val emeraldAccent = Color(0xFF10B981)
    val cyanAccent = Color(0xFF06B6D4)

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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    // Centered Title
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TROPHY CABINET",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(goldAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HALL OF FAME • MEDALS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = goldAccent,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Athlete Level & XP Pill
                    Surface(
                        color = cardColor,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, purpleAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "LEVEL $athleteLevel",
                                color = purpleAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$totalXp XP",
                                color = textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = { AdBanner(adKey = "ad_achievements_banner") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            // Hero Trophy Progress Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = cardColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glowing Trophy Graphic Box
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(goldAccent.copy(alpha = 0.25f), surfaceAltColor)
                                    )
                                )
                                .border(1.5.dp, goldAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = goldAccent,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MEDAL COLLECTION",
                                    color = textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                val percent = if (allBadges.isNotEmpty()) (unlockedCount * 100) / allBadges.size else 0
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(emeraldAccent.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$percent% DONE",
                                        color = emeraldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "$unlockedCount of ${allBadges.size} Medals Earned",
                                color = textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(Modifier.height(8.dp))

                            // Horizontal Gradient Progress Bar
                            val progress = if (allBadges.isNotEmpty()) unlockedCount.toFloat() / allBadges.size.toFloat() else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(surfaceAltColor)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress.coerceIn(0.04f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(goldAccent, Color(0xFFFBBF24), emeraldAccent)
                                            )
                                        )
                                )
                            }
                        }
                    }

                    // Next Milestone Teaser Card
                    if (nextBadgeToUnlock != null) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = surfaceAltColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(goldAccent.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = goldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "NEXT TARGET: ${nextBadgeToUnlock.name}",
                                        color = textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${nextBadgeToUnlock.desc} • +${nextBadgeToUnlock.xp} XP Bonus",
                                        color = textSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterCategories = listOf(
                    "All" to "All Medals (${allBadges.size})",
                    "Activity" to "Activity (${allBadges.count { it.category == "Activity" }})",
                    "Hydration" to "Hydration (${allBadges.count { it.category == "Hydration" }})",
                    "Yoga" to "Yoga Zen (${allBadges.count { it.category == "Yoga" }})"
                )

                filterCategories.forEach { (catKey, catLabel) ->
                    val isSelected = selectedFilter == catKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) goldAccent else cardColor
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) goldAccent else borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilter = catKey }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = catLabel,
                            color = if (isSelected) Color(0xFF0F172A) else textSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 3D Interactive Medal Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredBadges) { badge ->
                    val isUnlocked = unlockedBadges.contains(badge.id)
                    BadgeItem(
                        badge = badge,
                        isUnlocked = isUnlocked,
                        isDark = isDark,
                        onClick = { selectedBadgeForDetail = badge }
                    )
                }
            }
        }
    }

    // Detail Dialog Overlay
    selectedBadgeForDetail?.let { badge ->
        BadgeDetailDialog(
            badge = badge,
            isUnlocked = unlockedBadges.contains(badge.id),
            isDark = isDark,
            onDismiss = { selectedBadgeForDetail = null }
        )
    }
}

@Composable
fun BadgeItem(
    badge: BadgeData,
    isUnlocked: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current.density
    val infiniteTransition = rememberInfiniteTransition(label = "floating")

    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    val badgeColor = badge.color
    val emeraldAccent = Color(0xFF10B981)

    val floatX by infiniteTransition.animateFloat(
        initialValue = if (isUnlocked) -4f else -1.5f,
        targetValue = if (isUnlocked) 4f else 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isUnlocked) 2800 else 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatX"
    )

    val floatY by infiniteTransition.animateFloat(
        initialValue = if (isUnlocked) -3f else -1f,
        targetValue = if (isUnlocked) 3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isUnlocked) 2200 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    var touchX by remember { mutableStateOf(0f) }
    var touchY by remember { mutableStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }

    val tiltMax = if (isUnlocked) 24f else 12f

    val tiltX by animateFloatAsState(
        targetValue = if (isPressed) -touchY * tiltMax else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tiltX"
    )
    val tiltY by animateFloatAsState(
        targetValue = if (isPressed) touchX * tiltMax else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tiltY"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val animatedShadow by animateFloatAsState(
        targetValue = if (isPressed) 16f else 4f,
        label = "shadow"
    )

    val emblemTranslateX by animateFloatAsState(
        targetValue = if (isPressed) touchX * (if (isUnlocked) 18f else 8f) * density else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "emblemTranslateX"
    )
    val emblemTranslateY by animateFloatAsState(
        targetValue = if (isPressed) touchY * (if (isUnlocked) 18f else 8f) * density else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "emblemTranslateY"
    )
    val totalEmblemX = emblemTranslateX + (floatY * density * (if (isUnlocked) 1.2f else 0.5f))
    val totalEmblemY = emblemTranslateY - (floatX * density * (if (isUnlocked) 1.2f else 0.5f))

    Card(
        modifier = Modifier
            .height(210.dp)
            .graphicsLayer {
                rotationX = tiltX + floatX
                rotationY = tiltY + floatY
                scaleX = scale
                scaleY = scale
                cameraDistance = 8f
                shadowElevation = animatedShadow
            }
            .clickable { onClick() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w > 0 && h > 0) {
                            touchX = ((down.position.x / w) * 2f - 1f).coerceIn(-1f, 1f)
                            touchY = ((down.position.y / h) * 2f - 1f).coerceIn(-1f, 1f)
                        }
                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }
                            if (!anyPressed) break
                            val currentPointer = event.changes.firstOrNull { it.pressed }
                            if (currentPointer != null) {
                                if (w > 0 && h > 0) {
                                    touchX = ((currentPointer.position.x / w) * 2f - 1f).coerceIn(-1f, 1f)
                                    touchY = ((currentPointer.position.y / h) * 2f - 1f).coerceIn(-1f, 1f)
                                }
                            }
                        }
                        isPressed = false
                    }
                }
            },
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            if (isUnlocked) 1.5.dp else 1.dp,
            if (isUnlocked) Brush.verticalGradient(
                colors = listOf(badgeColor, badgeColor.copy(alpha = 0.25f))
            ) else SolidColor(borderColor)
        ),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val bgBgColor = cardColor
            val bgAccentColor = if (isUnlocked) badgeColor.copy(alpha = 0.12f) else surfaceAltColor.copy(alpha = 0.6f)
            val bgEndColor = cardColor

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val width = size.width
                        val height = size.height
                        val shiftX = (tiltY / tiltMax) * 0.18f * width
                        val shiftY = -(tiltX / tiltMax) * 0.18f * height

                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(bgBgColor, bgAccentColor, bgEndColor),
                                start = Offset(0f, 0f),
                                end = Offset(width + shiftX, height + shiftY)
                            ),
                            cornerRadius = CornerRadius(22.dp.toPx())
                        )
                    }
            )

            // Neon glowing aura behind emblem
            if (isUnlocked) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .align(Alignment.Center)
                        .offset(y = (-20).dp)
                        .background(
                            Brush.radialGradient(
                                listOf(badgeColor.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
            }

            // XP / Locked Pill top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                if (isUnlocked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.18f))
                            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${badge.xp} XP",
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(surfaceAltColor)
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LOCKED",
                            color = textSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // 3D Metallic Coin Emblem
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .graphicsLayer {
                            translationX = totalEmblemX
                            translationY = totalEmblemY
                        }
                        .background(
                            brush = if (isUnlocked) {
                                Brush.radialGradient(
                                    colors = listOf(
                                        badgeColor.copy(alpha = 0.35f),
                                        badgeColor.copy(alpha = 0.08f)
                                    )
                                )
                            } else {
                                Brush.radialGradient(
                                    colors = listOf(
                                        textSecondary.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            },
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            brush = if (isUnlocked) {
                                Brush.sweepGradient(
                                    colors = listOf(badgeColor, badgeColor.copy(alpha = 0.4f), badgeColor)
                                )
                            } else {
                                SolidColor(borderColor)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = when (badge.category) {
                        "Activity" -> Icons.Default.FitnessCenter
                        "Hydration" -> Icons.Default.WaterDrop
                        "Yoga" -> Icons.Default.SelfImprovement
                        else -> Icons.Default.EmojiEvents
                    }

                    Icon(
                        imageVector = if (isUnlocked) iconVector else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) badgeColor else textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Name & Desc
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = badge.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isUnlocked) textPrimary else textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = badge.desc,
                        fontSize = 10.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge at bottom of card
                if (isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(emeraldAccent.copy(alpha = 0.15f))
                            .border(1.dp, emeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "UNLOCKED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = emeraldAccent,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaceAltColor)
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PROGRESS PENDING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeDetailDialog(
    badge: BadgeData,
    isUnlocked: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val cardColor = if (isDark) Color(0xFF111726) else Color(0xFFFFFFFF)
    val surfaceAltColor = if (isDark) Color(0xFF171F32) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF24304A) else Color(0xFFDEE6F2)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    val badgeColor = badge.color
    val emeraldAccent = Color(0xFF10B981)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = cardColor,
            border = BorderStroke(
                1.5.dp,
                if (isUnlocked) Brush.verticalGradient(listOf(badgeColor, badgeColor.copy(alpha = 0.2f)))
                else SolidColor(borderColor)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3D Coin preview
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                if (isUnlocked) listOf(badgeColor.copy(alpha = 0.3f), Color.Transparent)
                                else listOf(textSecondary.copy(alpha = 0.1f), Color.Transparent)
                            )
                        )
                        .border(
                            2.5.dp,
                            if (isUnlocked) badgeColor else borderColor,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = when (badge.category) {
                        "Activity" -> Icons.Default.FitnessCenter
                        "Hydration" -> Icons.Default.WaterDrop
                        "Yoga" -> Icons.Default.SelfImprovement
                        else -> Icons.Default.EmojiEvents
                    }

                    Icon(
                        imageVector = if (isUnlocked) iconVector else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) badgeColor else textSecondary,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = badge.name,
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = badge.desc,
                    color = textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(16.dp))

                // XP Reward & Category Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+${badge.xp} ATHLETE XP",
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaceAltColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badge.category.uppercase(),
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Tips to Unlock Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = surfaceAltColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isUnlocked) emeraldAccent else badgeColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isUnlocked) "ACHIEVEMENT UNLOCKED" else "HOW TO UNLOCK",
                                color = if (isUnlocked) emeraldAccent else badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = when (badge.id) {
                                "10K_STEPS" -> "Walk 10,000 steps in a single day."
                                "20K_STEPS" -> "Walk 20,000 steps in a single day."
                                "WATER_HERO" -> "Log 8 glasses of water in the Water Tracker."
                                "STREAK_3" -> "Complete targets 3 days in a row."
                                "SQUAT_MASTER" -> "Log and complete 50 squats in Workout Planner."
                                "HEART_HEALTHY" -> "Perform heart rate scan 5 times."
                                "MUDRA_JOURNEY_START" -> "Start practicing your first Yoga Mudra."
                                "MUDRA_ROUTINE_BUILDER" -> "Add and practice at least 3 Mudras."
                                "MUDRA_ZEN_MASTER" -> "Complete full yoga sessions to earn Zen level."
                                "MUDRA_JOURNEY_COMPLETE" -> "Complete all mudra sessions in the Yoga catalog."
                                else -> "Complete goal targets for this medal."
                            },
                            color = textPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUnlocked) badgeColor else surfaceAltColor,
                        contentColor = if (isUnlocked) Color(0xFF0F172A) else textPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isUnlocked) "Claim & Return" else "Got It",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
