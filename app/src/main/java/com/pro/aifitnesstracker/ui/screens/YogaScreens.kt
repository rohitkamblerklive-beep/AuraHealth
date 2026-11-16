package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import android.media.MediaPlayer
import java.util.UUID
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.pro.aifitnesstracker.R
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.media.AudioManager

// Helper function to return category-specific glowing color scheme
fun getCategoryColor(category: String): Color {
    return when (category) {
        "Mental Calm" -> Color(0xFFB39DDB)      // Soft Purple / Lavender
        "Healing & Therapy" -> Color(0xFF4DB6AC) // Teal
        "Energy & Vitality" -> Color(0xFFFFB74D)  // Orange / Amber
        "Digestion & Detox" -> Color(0xFF81C784)  // Green
        "Brain & Focus" -> Color(0xFF64B5F6)      // Blue / Sky
        "Heart & Circulation" -> Color(0xFFE57373) // Rose
        else -> Color(0xFF9FA8DA)
    }
}

data class MudraJourney(
    val id: String,
    val title: String,
    val description: String,
    val categoryFilter: String,
    val accentColor: Color
)

val presetJourneys = listOf(
    MudraJourney(
        id = "mental_calm",
        title = "Mental Calm & Anti-Stress",
        description = "Calm your overactive mind, improve focus, and restore inner peace over 21 days.",
        categoryFilter = "Mental Calm",
        accentColor = Color(0xFFB39DDB)
    ),
    MudraJourney(
        id = "energy_boost",
        title = "Energy & Vitality Booster",
        description = "Overcome chronic fatigue, boost immunity, and generate internal dynamic energy.",
        categoryFilter = "Energy & Vitality",
        accentColor = Color(0xFFFFB74D)
    ),
    MudraJourney(
        id = "healing_detox",
        title = "Therapeutic Healing & Detox",
        description = "Stimulate digestion, clear blockages, and target physiological recovery.",
        categoryFilter = "Healing & Therapy",
        accentColor = Color(0xFF4DB6AC)
    )
)

fun getElementsForMudra(iconCode: String): List<String> {
    return when (iconCode) {
        "thumb_index", "thumb_index_down", "thumb_over_index", "thumb_index_curled" -> listOf("fire", "air")
        "thumb_middle", "thumb_over_middle", "thumb_three_fingers" -> listOf("fire", "space")
        "thumb_ring", "thumb_over_ring" -> listOf("fire", "earth")
        "thumb_pinky" -> listOf("fire", "water")
        
        "thumb_middle_ring" -> listOf("fire", "space", "earth")
        "thumb_ring_pinky" -> listOf("fire", "earth", "water")
        "thumb_index_middle" -> listOf("fire", "air", "space")
        
        "thumb_middle_ring_pinky" -> listOf("fire", "space", "earth", "water")
        "all_touching_thumb", "all_fingers_touching", "fingertips_touching" -> listOf("fire", "air", "space", "earth", "water")
        
        "palms_joined", "palms_stacked", "palms_stacked_flat", "palms_stacked_heart", "lotus_shape", "womb_shape" -> listOf("fire", "air", "space", "earth", "water")
        else -> listOf("fire", "air", "space", "earth", "water")
    }
}

data class BreathingPattern(
    val name: String,
    val description: String,
    val inhale: Int,
    val holdIn: Int,
    val exhale: Int,
    val holdOut: Int
)

val presetBreathingPatterns = listOf(
    BreathingPattern("Sama Vritti (Box)", "Equal duration for balance and focus.", 4, 4, 4, 4),
    BreathingPattern("4-7-8 Technique", "Deep relaxation, cures anxiety.", 4, 7, 8, 0),
    BreathingPattern("Visama Vritti (1:4:2)", "Directs prana flow and lung capacity.", 4, 16, 8, 0),
    BreathingPattern("Puraka Recaka", "Simple equal breath for beginners.", 5, 0, 5, 0),
    BreathingPattern("Custom Breathing", "Configure your own duration parameters.", 4, 2, 4, 2)
)

data class AmbientSound(
    val name: String,
    val description: String,
    val url: String
)

val presetAmbientSounds = listOf(
    AmbientSound("None", "Silence for pure focus", ""),
    AmbientSound("Tibetan Bowl", "Soothing singing bowls & bells", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
    AmbientSound("Forest River", "Gentle stream and forest birds", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
    AmbientSound("Deep Space", "Astral ambient pad for deep focus", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
    AmbientSound("Zen Garden", "Traditional flute and soft wind chimes", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3")
)

fun getMudraForJourneyDay(journey: MudraJourney, dayIndex: Int, mudrasList: List<YogaMudra>): YogaMudra {
    val filtered = mudrasList.filter { 
        it.category == journey.categoryFilter || 
        (journey.id == "mental_calm" && it.category == "Brain & Focus") || 
        (journey.id == "energy_boost" && it.category == "Heart & Circulation") ||
        (journey.id == "healing_detox" && it.category == "Digestion & Detox")
    }
    val list = if (filtered.isEmpty()) mudrasList else filtered
    return list[dayIndex % list.size]
}


// Trigger sound chime on complete
fun playTibetanBell(context: android.content.Context) {
    try {
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, notificationUri)
        if (ringtone != null) {
            ringtone.play()
        } else {
            val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneG.startTone(ToneGenerator.TONE_CDMA_PIP, 500)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun YogaMenuScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var mainTab by remember { mutableIntStateOf(0) } // 0 = Asanas & Guided Flows, 1 = Mudras

    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF3F6FB)
    val cardBg = if (isDark) Color(0xFF101726) else Color(0xFFE6ECF5)
    val cardBorderColor = if (isDark) Color(0xFF26334D) else Color(0xFFBCC6D8)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
    ) {
        // ── Top App Bar + Centered "Mindful Flow • Guided Asanas" Pill ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // < Back Button
                IconButton(
                    onClick = {
                        if (mainTab == 1) mainTab = 0 else onBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }

                // Centered Title
                Text(
                    text = "YOGA & MEDITATION",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )

                // Circular Sound / Tibetan Chime Button
                Surface(
                    onClick = { playTibetanBell(context) },
                    shape = CircleShape,
                    color = cardBg,
                    border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.45f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "Ambient Chime",
                            tint = Color(0xFF2DD4BF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Glowing "Mindful Flow • Guided Asanas" Subtitle Pill (Tapping toggles Asanas / Mudras)
            val pillBorderBrush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF2DD4BF), Color(0xFF06B6D4), Color(0xFF818CF8))
            )
            val pillBgBrush = Brush.horizontalGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF0A2732), Color(0xFF14233C))
                } else {
                    listOf(Color(0xFF123E4F), Color(0xFF24375E))
                }
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(pillBgBrush)
                    .border(1.2.dp, pillBorderBrush, RoundedCornerShape(50))
                    .clickable { mainTab = if (mainTab == 0) 1 else 0 }
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (mainTab == 0) "Mindful Flow • Guided Asanas" else "Sacred Hand Mudras • Tap for Asanas",
                    color = Color(0xFF99F6E4),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            }
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            androidx.compose.animation.AnimatedContent(
                targetState = mainTab,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    else
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                },
                label = "yoga_main_tab"
            ) { tab ->
                when (tab) {
                    0 -> YogaAsanaSection(onOpenMudras = { mainTab = 1 })
                    else -> YogaMudrasTab()
                }
            }
        }

        AdBanner(adKey = "ad_yoga_banner")
    }
}

@Composable
fun ElementalRadarChart(
    firePercent: Float,
    airPercent: Float,
    spacePercent: Float,
    earthPercent: Float,
    waterPercent: Float,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val axesColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    val pointCenterColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = size.width.coerceAtMost(size.height) / 2f * 0.8f

        val numVertices = 5
        val angleStep = (2 * Math.PI / numVertices).toFloat()
        val startAngle = (-Math.PI / 2).toFloat()

        // 1. Draw concentric pentagons (grid background)
        val gridLevels = 5
        for (level in 1..gridLevels) {
            val radius = maxRadius * (level.toFloat() / gridLevels)
            val path = androidx.compose.ui.graphics.Path()
            for (i in 0 until numVertices) {
                val angle = startAngle + i * angleStep
                val vx = centerX + radius * Math.cos(angle.toDouble()).toFloat()
                val vy = centerY + radius * Math.sin(angle.toDouble()).toFloat()
                if (i == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
            }
            path.close()
            drawPath(
                path = path,
                color = gridColor,
                style = Stroke(width = 2f)
            )
        }

        // 2. Draw 5 axes lines from center to vertices
        for (i in 0 until numVertices) {
            val angle = startAngle + i * angleStep
            val vx = centerX + maxRadius * Math.cos(angle.toDouble()).toFloat()
            val vy = centerY + maxRadius * Math.sin(angle.toDouble()).toFloat()
            drawLine(
                color = axesColor,
                start = Offset(centerX, centerY),
                end = Offset(vx, vy),
                strokeWidth = 2f
            )
        }

        // 3. Draw user's active aura shape (polygon)
        val percentages = listOf(firePercent, airPercent, spacePercent, earthPercent, waterPercent)
        val auraPath = androidx.compose.ui.graphics.Path()
        val points = mutableListOf<Offset>()
        for (i in 0 until numVertices) {
            val pct = percentages[i].coerceIn(0f, 100f) / 100f
            val scale = if (pct == 0f) 0.15f else pct
            val radius = maxRadius * scale
            val angle = startAngle + i * angleStep
            val px = centerX + radius * Math.cos(angle.toDouble()).toFloat()
            val py = centerY + radius * Math.sin(angle.toDouble()).toFloat()
            points.add(Offset(px, py))
            if (i == 0) auraPath.moveTo(px, py) else auraPath.lineTo(px, py)
        }
        auraPath.close()

        drawPath(
            path = auraPath,
            color = accentColor.copy(alpha = 0.25f)
        )
        drawPath(
            path = auraPath,
            color = accentColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        points.forEach { pt ->
            drawCircle(
                color = pointCenterColor,
                radius = 6f,
                center = pt
            )
            drawCircle(
                color = accentColor,
                radius = 12f,
                center = pt,
                style = Stroke(width = 3f)
            )
        }
    }
}

@Composable
fun ElementalAnalyticsScreen(prefsManager: SharedPrefsManager) {
    val strings = LocalAppStrings.current
    val fireTime = prefsManager.getMudraMeditationTime("fire")
    val airTime = prefsManager.getMudraMeditationTime("air")
    val spaceTime = prefsManager.getMudraMeditationTime("space")
    val earthTime = prefsManager.getMudraMeditationTime("earth")
    val waterTime = prefsManager.getMudraMeditationTime("water")

    val totalSeconds = fireTime + airTime + spaceTime + earthTime + waterTime
    val totalMins = (totalSeconds / 60).toInt()
    
    val firePercent = if (totalSeconds == 0f) 20f else (fireTime / totalSeconds * 100f)
    val airPercent = if (totalSeconds == 0f) 20f else (airTime / totalSeconds * 100f)
    val spacePercent = if (totalSeconds == 0f) 20f else (spaceTime / totalSeconds * 100f)
    val earthPercent = if (totalSeconds == 0f) 20f else (earthTime / totalSeconds * 100f)
    val waterPercent = if (totalSeconds == 0f) 20f else (waterTime / totalSeconds * 100f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            strings.history.uppercase() + " & " + strings.performance.uppercase(),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.totalMinutes.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalMins " + strings.minutesLabel, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.activeStreak.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${prefsManager.getCurrentStreak()} " + strings.daysLabel, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    strings.ayurvedicElementalBalance.uppercase(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    strings.showingEnergyAlignment,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ElementalRadarChart(
                        firePercent = firePercent,
                        airPercent = airPercent,
                        spacePercent = spacePercent,
                        earthPercent = earthPercent,
                        waterPercent = waterPercent,
                        modifier = Modifier.fillMaxSize(),
                        accentColor = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "🔥 " + strings.elementFire.uppercase(),
                        color = Color(0xFFFFB74D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    Text(
                        "💨 " + strings.elementAir.uppercase(),
                        color = Color(0xFF4DB6AC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(top = 10.dp)
                    )
                    Text(
                        "🌌 " + strings.elementSpace.uppercase(),
                        color = Color(0xFF64B5F6),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 12.dp)
                    )
                    Text(
                        "🌿 " + strings.elementEarth.uppercase(),
                        color = Color(0xFF81C784),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 12.dp)
                    )
                    Text(
                        "💧 " + strings.elementWater.uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterStart).padding(top = 10.dp)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    strings.fiveBiologicalCircuits.uppercase(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                val elementsList = listOf(
                    Triple("🔥 " + strings.agniFireThumb, firePercent, Color(0xFFFFB74D)),
                    Triple("💨 " + strings.vayuAirIndex, airPercent, Color(0xFF4DB6AC)),
                    Triple("🌌 " + strings.akashaSpaceMiddle, spacePercent, Color(0xFF64B5F6)),
                    Triple("🌿 " + strings.prithviEarthRing, earthPercent, Color(0xFF81C784)),
                    Triple("💧 " + strings.varunaWaterPinky, waterPercent, MaterialTheme.colorScheme.primary)
                )

                elementsList.forEach { (name, percent, color) ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(String.format("%.1f%%", percent), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(percent / 100f)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JourneyRoadmapScreen(
    prefsManager: SharedPrefsManager,
    mudrasList: List<YogaMudra>,
    onSelectMudra: (YogaMudra) -> Unit,
    onUnlockBadge: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val activeJourneyId = remember { mutableStateOf(prefsManager.getActiveJourneyId()) }
    val progressDay = remember { mutableIntStateOf(0) }
    
    LaunchedEffect(activeJourneyId.value) {
        if (activeJourneyId.value.isNotEmpty()) {
            progressDay.intValue = prefsManager.getJourneyProgressDay(activeJourneyId.value)
        }
    }

    if (activeJourneyId.value.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                strings.choose21DayJourney.uppercase(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            presetJourneys.forEach { rawJourney ->
                val journey = getLocalizedJourney(rawJourney, strings)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            prefsManager.setActiveJourneyId(journey.id)
                            activeJourneyId.value = journey.id
                            prefsManager.checkAndUnlockBadge("MUDRA_JOURNEY_START", onUnlockBadge)
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(journey.accentColor.copy(alpha = 0.05f), Color.Transparent)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SelfImprovement,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            journey.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            journey.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                strings.startJourney.uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        val activeJourney = remember(activeJourneyId.value, strings) {
            getLocalizedJourney(presetJourneys.first { it.id == activeJourneyId.value }, strings)
        }
        val pct = (progressDay.intValue * 100 / 21)

        Column(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                activeJourney.title.uppercase(),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                strings.dayOf21Completed.format(progressDay.intValue),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        
                        TextButton(
                            onClick = {
                                prefsManager.setActiveJourneyId("")
                                activeJourneyId.value = ""
                            }
                        ) {
                            Text(strings.resetJourney, color = Color.Red.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(pct.toFloat() / 100f)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        "$pct% " + strings.completedLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(21) { dayIndex ->
                    val isCompleted = dayIndex < progressDay.intValue
                    val isCurrent = dayIndex == progressDay.intValue
                    val isLocked = dayIndex > progressDay.intValue
                    
                    val dayMudra = remember(dayIndex) {
                        getMudraForJourneyDay(activeJourney, dayIndex, mudrasList)
                    }
                    
                    val targetDurationMins = when {
                        dayIndex < 7 -> 5
                        dayIndex < 14 -> 10
                        else -> 15
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLocked) {
                                onSelectMudra(dayMudra)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .alpha(if (isLocked) 0.4f else 1.0f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = strings.completedLabel,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else if (isLocked) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = strings.lockedLabel,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        "${dayIndex + 1}",
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${strings.day.uppercase()} ${dayIndex + 1}",
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    dayMudra.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    dayMudra.translation,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "⏱️ " + strings.minsDurationFormat.format(targetDurationMins).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZenQuoteCard(onDismiss: () -> Unit) {
    val zenQuotes = remember {
        listOf(
            "Quiet the mind and the soul will speak." to "Ma Jaya Sati Bhagavati",
            "Feelings come and go like clouds in a windy sky. Conscious breathing is my anchor." to "Thich Nhat Hanh",
            "Within you, there is a stillness and a sanctuary to which you can retreat at any time." to "Hermann Hesse",
            "Mindfulness isn't difficult, we just need to remember to do it." to "Sharon Salzberg",
            "Do not dwell in the past, do not dream of the future, concentrate the mind on the present moment." to "Buddha",
            "The present moment is filled with joy and happiness. If you are attentive, you will see it." to "Thich Nhat Hanh",
            "Rule your mind or it will rule you." to "Horace",
            "The mind is like water. When it's calm, everything becomes clear." to "Prasad Dev"
        )
    }

    val todayQuote = remember {
        val calendar = java.util.Calendar.getInstance()
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        zenQuotes[dayOfYear % zenQuotes.size]
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.SelfImprovement,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "DAILY ZEN WISDOM",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "“${todayQuote.first}”",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "— ${todayQuote.second}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun YogaMudrasTab() {
    val context = LocalContext.current
    val repository = remember(LocalAppStrings.current) { YogaMudraRepository(context) }
    val englishStepsMap = remember { loadEnglishMudras(context) }
    val prefsManager = remember { SharedPrefsManager(context) }
    var activeUnlockedBadgeId by remember { mutableStateOf<String?>(null) }
    val onUnlockBadge = { badgeId: String -> activeUnlockedBadgeId = badgeId }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedElement by remember { mutableStateOf("All") }
    var selectedFinger by remember { mutableStateOf("All") }
    var activeMudra by remember { mutableStateOf<YogaMudra?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showTimerOverlay by remember { mutableStateOf(false) }
    var activeRoutine by remember { mutableStateOf<CustomMudraRoutine?>(null) }
    var showRoutineRunner by remember { mutableStateOf(false) }
    var showZenQuote by remember { mutableStateOf(true) }

    var activeSubTab by remember { mutableStateOf(0) }
    val strings = LocalAppStrings.current
    val subTabs = remember(strings) {
        listOf(
            strings.mudraExplore.uppercase(),
            strings.mudraDailyJourney.uppercase(),
            strings.mudraMyRoutines.uppercase(),
            strings.mudraMyAnalytics.uppercase()
        )
    }

    val categories = listOf("All", "Mental Calm", "Healing & Therapy", "Energy & Vitality", "Digestion & Detox", "Brain & Focus", "Heart & Circulation")
    val elements = listOf("All", "Fire", "Air", "Space", "Earth", "Water")
    val fingers = listOf("All", "Thumb", "Index", "Middle", "Ring", "Pinky")

    val mudrasList = remember(searchQuery, selectedCategory, selectedElement, selectedFinger) {
        var filtered = repository.getAllMudras()
        
        // 1. Category Filter
        if (selectedCategory != "All") {
            filtered = filtered.filter { it.category == selectedCategory }
        }
        
        // 2. Element Filter
        if (selectedElement != "All") {
            val elementLower = selectedElement.lowercase()
            filtered = filtered.filter { mudra ->
                val mudraElements = getElementsForMudra(mudra.icon)
                mudraElements.contains(elementLower)
            }
        }
        
        // 3. Finger Filter
        if (selectedFinger != "All") {
            val fingerLower = selectedFinger.lowercase()
            filtered = filtered.filter { mudra ->
                val targetElement = when (fingerLower) {
                    "thumb" -> "fire"
                    "index" -> "air"
                    "middle" -> "space"
                    "ring" -> "earth"
                    "pinky" -> "water"
                    else -> ""
                }
                if (targetElement.isNotEmpty()) {
                    val mudraElements = getElementsForMudra(mudra.icon)
                    mudraElements.contains(targetElement)
                } else true
            }
        }
        
        // 4. Search Query Filter
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.benefits.lowercase().contains(query) ||
                it.translation.lowercase().contains(query)
            }
        }
        
        filtered
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Segmented control for Sub-Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                subTabs.forEachIndexed { index, title ->
                    val isSelected = activeSubTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activeSubTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (activeSubTab == 0) {
                if (showZenQuote) {
                    Dialog(onDismissRequest = { showZenQuote = false }) {
                        ZenQuoteCard(onDismiss = { showZenQuote = false })
                    }
                }

                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(strings.mudraSearchPlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                // Category filter chips
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏷️", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    categories.forEach { category ->
                        val displayName = getLocalizedCategoryName(category, strings)
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                displayName,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Element filter chips
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌀", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    elements.forEach { elem ->
                        val isSelected = selectedElement == elem
                        val displayName = getLocalizedElementName(elem, strings)
                        val emoji = when (elem) {
                            "Fire" -> "🔥 "
                            "Air" -> "💨 "
                            "Space" -> "🌌 "
                            "Earth" -> "🌿 "
                            "Water" -> "💧 "
                            else -> ""
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedElement = elem }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "$emoji$displayName",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Finger filter chips
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👋", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    fingers.forEach { finger ->
                        val isSelected = selectedFinger == finger
                        val displayName = getLocalizedFingerName(finger, strings)
                        val emoji = when (finger) {
                            "Thumb" -> "👍 "
                            "Index" -> "☝️ "
                            "Middle" -> "🖕 "
                            "Ring" -> "💍 "
                            "Pinky" -> "🤙 "
                            else -> ""
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedFinger = finger }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "$emoji$displayName",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Results count and Reset Filters action
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.mudrasAvailable.format(mudrasList.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    
                    if (selectedCategory != "All" || selectedElement != "All" || selectedFinger != "All" || searchQuery.isNotEmpty()) {
                        Text(
                            text = strings.resetFilters,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                selectedCategory = "All"
                                selectedElement = "All"
                                selectedFinger = "All"
                                searchQuery = ""
                            }
                        )
                    }
                }

                // Grid displaying cards
                if (mudrasList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(strings.noMudrasFound, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mudrasList) { mudra ->
                            MudraGridCard(
                                mudra = mudra,
                                onClick = {
                                    activeMudra = mudra
                                    showDetailSheet = true
                                }
                            )
                        }
                    }
                }
            } else if (activeSubTab == 1) {
                JourneyRoadmapScreen(
                    prefsManager = prefsManager,
                    mudrasList = repository.getAllMudras(),
                    onSelectMudra = { mudra ->
                        activeMudra = mudra
                        showDetailSheet = true
                    },
                    onUnlockBadge = onUnlockBadge
                )
            } else if (activeSubTab == 2) {
                MyRoutinesScreen(
                    prefsManager = prefsManager,
                    repository = repository,
                    onSelectMudra = { mudra ->
                        activeMudra = mudra
                        showDetailSheet = true
                    },
                    onStartRoutine = { routine ->
                        activeRoutine = routine
                        showRoutineRunner = true
                    },
                    onUnlockBadge = onUnlockBadge
                )
            } else {
                ElementalAnalyticsScreen(prefsManager = prefsManager)
            }
        }

        // Custom Bottom Sheet Detail Modal overlay
        AnimatedVisibility(
            visible = showDetailSheet && activeMudra != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            activeMudra?.let { mudra ->
                MudraDetailOverlay(
                    mudra = mudra,
                    onClose = { showDetailSheet = false },
                    englishStepsMap = englishStepsMap,
                    onStartTimer = {
                        showDetailSheet = false
                        showTimerOverlay = true
                    }
                )
            }
        }

        // Meditation Timer Screen Overlay
        AnimatedVisibility(
            visible = showTimerOverlay && activeMudra != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            activeMudra?.let { mudra ->
                MeditationTimerOverlay(
                    mudra = mudra,
                    onClose = { showTimerOverlay = false },
                    onUnlockBadge = onUnlockBadge
                )
            }
        }

        // Custom Routine Session Runner Overlay
        AnimatedVisibility(
            visible = showRoutineRunner && activeRoutine != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            activeRoutine?.let { routine ->
                RoutineRunnerOverlay(
                    routine = routine,
                    mudrasList = repository.getAllMudras(),
                    englishStepsMap = englishStepsMap,
                    onClose = { showRoutineRunner = false },
                    onUnlockBadge = onUnlockBadge
                )
            }
        }

        if (activeUnlockedBadgeId != null) {
            BadgeUnlockedDialog(
                badgeId = activeUnlockedBadgeId!!,
                prefsManager = prefsManager,
                onClose = { activeUnlockedBadgeId = null }
            )
        }
    }
}

@Composable
fun MudraGridCard(mudra: YogaMudra, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Gradient dynamic initial avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        mudra.name.take(2).uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
                
                // Small Category pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        mudra.category.uppercase().substringBefore(" &"),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                mudra.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                mudra.translation,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                mudra.benefits,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun DynamicHandVisualizer(iconCode: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val scaleX = w / 200f
        val scaleY = h / 200f

        if (iconCode == "palms_joined") {
            val leftPalm = androidx.compose.ui.graphics.Path().apply {
                moveTo(80f * scaleX, 170f * scaleY)
                quadraticTo(50f * scaleX, 110f * scaleY, 95f * scaleX, 50f * scaleY)
                quadraticTo(100f * scaleX, 100f * scaleY, 95f * scaleX, 170f * scaleY)
                close()
            }
            val rightPalm = androidx.compose.ui.graphics.Path().apply {
                moveTo(120f * scaleX, 170f * scaleY)
                quadraticTo(150f * scaleX, 110f * scaleY, 105f * scaleX, 50f * scaleY)
                quadraticTo(100f * scaleX, 100f * scaleY, 105f * scaleX, 170f * scaleY)
                close()
            }
            drawPath(leftPalm, color, style = Stroke(width = 4f * scaleX))
            drawPath(rightPalm, color, style = Stroke(width = 4f * scaleX))
        } else if (iconCode == "palms_stacked" || iconCode == "palms_stacked_flat" || iconCode == "palms_stacked_heart") {
            drawRoundRect(
                color = color,
                topLeft = Offset(40f * scaleX, 110f * scaleY),
                size = androidx.compose.ui.geometry.Size(120f * scaleX, 40f * scaleY),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f * scaleX, 20f * scaleY),
                style = Stroke(width = 4f * scaleX)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(50f * scaleX, 85f * scaleY),
                size = androidx.compose.ui.geometry.Size(100f * scaleX, 35f * scaleY),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(17f * scaleX, 17f * scaleY),
                style = Stroke(width = 4f * scaleX)
            )
        } else {
            val wristLeft = Offset(80f * scaleX, 180f * scaleY)
            val wristRight = Offset(120f * scaleX, 180f * scaleY)

            val thumbBase = Offset(65f * scaleX, 140f * scaleY)
            val indexBase = Offset(75f * scaleX, 100f * scaleY)
            val middleBase = Offset(100f * scaleX, 95f * scaleY)
            val ringBase = Offset(125f * scaleX, 100f * scaleY)
            val pinkyBase = Offset(135f * scaleX, 115f * scaleY)

            var thumbTip = Offset(40f * scaleX, 115f * scaleY)
            var indexTip = Offset(75f * scaleX, 40f * scaleY)
            var middleTip = Offset(100f * scaleX, 30f * scaleY)
            var ringTip = Offset(125f * scaleX, 40f * scaleY)
            var pinkyTip = Offset(145f * scaleX, 60f * scaleY)

            var thumbJoint = Offset(50f * scaleX, 130f * scaleY)
            var indexJoint = Offset(75f * scaleX, 70f * scaleY)
            var middleJoint = Offset(100f * scaleX, 60f * scaleY)
            var ringJoint = Offset(125f * scaleX, 70f * scaleY)
            var pinkyJoint = Offset(140f * scaleX, 85f * scaleY)

            when (iconCode) {
                "thumb_index", "thumb_index_down" -> {
                    val touch = Offset(65f * scaleX, 85f * scaleY)
                    thumbTip = touch
                    indexTip = touch
                    thumbJoint = Offset(50f * scaleX, 110f * scaleY)
                    indexJoint = Offset(70f * scaleX, 90f * scaleY)
                }
                "thumb_middle" -> {
                    val touch = Offset(85f * scaleX, 80f * scaleY)
                    thumbTip = touch
                    middleTip = touch
                    thumbJoint = Offset(55f * scaleX, 115f * scaleY)
                    middleJoint = Offset(90f * scaleX, 85f * scaleY)
                }
                "thumb_ring" -> {
                    val touch = Offset(105f * scaleX, 85f * scaleY)
                    thumbTip = touch
                    ringTip = touch
                    thumbJoint = Offset(70f * scaleX, 120f * scaleY)
                    ringJoint = Offset(115f * scaleX, 95f * scaleY)
                }
                "thumb_pinky" -> {
                    val touch = Offset(115f * scaleX, 100f * scaleY)
                    thumbTip = touch
                    pinkyTip = touch
                    thumbJoint = Offset(80f * scaleX, 125f * scaleY)
                    pinkyJoint = Offset(125f * scaleX, 110f * scaleY)
                }
                "thumb_ring_pinky" -> {
                    val touch = Offset(100f * scaleX, 95f * scaleY)
                    thumbTip = touch
                    ringTip = touch
                    pinkyTip = touch
                    thumbJoint = Offset(75f * scaleX, 120f * scaleY)
                    ringJoint = Offset(115f * scaleX, 100f * scaleY)
                    pinkyJoint = Offset(125f * scaleX, 105f * scaleY)
                }
                "thumb_middle_ring" -> {
                    val touch = Offset(90f * scaleX, 90f * scaleY)
                    thumbTip = touch
                    middleTip = touch
                    ringTip = touch
                    thumbJoint = Offset(70f * scaleX, 120f * scaleY)
                    middleJoint = Offset(95f * scaleX, 95f * scaleY)
                    ringJoint = Offset(110f * scaleX, 95f * scaleY)
                }
                "thumb_middle_ring_pinky" -> {
                    val touch = Offset(105f * scaleX, 100f * scaleY)
                    thumbTip = touch
                    middleTip = touch
                    ringTip = touch
                    pinkyTip = touch
                    thumbJoint = Offset(80f * scaleX, 125f * scaleY)
                }
                "thumb_over_index" -> {
                    val fold = Offset(65f * scaleX, 125f * scaleY)
                    indexTip = fold
                    indexJoint = Offset(60f * scaleX, 105f * scaleY)
                    thumbTip = Offset(75f * scaleX, 115f * scaleY)
                    thumbJoint = Offset(65f * scaleX, 125f * scaleY)
                }
                "thumb_over_middle" -> {
                    val fold = Offset(80f * scaleX, 120f * scaleY)
                    middleTip = fold
                    middleJoint = Offset(90f * scaleX, 105f * scaleY)
                    thumbTip = Offset(85f * scaleX, 110f * scaleY)
                }
                "thumb_over_ring" -> {
                    val fold = Offset(100f * scaleX, 120f * scaleY)
                    ringTip = fold
                    ringJoint = Offset(115f * scaleX, 105f * scaleY)
                    thumbTip = Offset(95f * scaleX, 110f * scaleY)
                }
                "thumb_index_curled" -> {
                    val curl = Offset(80f * scaleX, 120f * scaleY)
                    indexTip = curl
                    indexJoint = Offset(70f * scaleX, 105f * scaleY)
                    thumbTip = curl
                    thumbJoint = Offset(60f * scaleX, 130f * scaleY)
                }
                "fist", "thumb_over_ring_fist", "thumb_inside_fist", "thumb_inside_pinky_fist", "wrists_crossed_fists" -> {
                    val curl = Offset(100f * scaleX, 125f * scaleY)
                    indexTip = curl
                    middleTip = curl
                    ringTip = curl
                    pinkyTip = curl
                    thumbTip = Offset(90f * scaleX, 135f * scaleY)
                    
                    indexJoint = Offset(80f * scaleX, 115f * scaleY)
                    middleJoint = Offset(100f * scaleX, 110f * scaleY)
                    ringJoint = Offset(120f * scaleX, 115f * scaleY)
                    pinkyJoint = Offset(125f * scaleX, 120f * scaleY)
                    thumbJoint = Offset(75f * scaleX, 135f * scaleY)
                }
            }

            val handOutline = androidx.compose.ui.graphics.Path().apply {
                moveTo(wristLeft.x, wristLeft.y)
                lineTo(thumbBase.x, thumbBase.y)
                
                quadraticTo(thumbJoint.x, thumbJoint.y, thumbTip.x, thumbTip.y)
                quadraticTo(thumbJoint.x + 5f * scaleX, thumbJoint.y + 5f * scaleY, thumbBase.x + 10f * scaleX, thumbBase.y - 15f * scaleY)

                lineTo(indexBase.x, indexBase.y)
                quadraticTo(indexJoint.x, indexJoint.y, indexTip.x, indexTip.y)
                quadraticTo(indexJoint.x + 6f * scaleX, indexJoint.y, indexBase.x + 10f * scaleX, indexBase.y)

                lineTo(middleBase.x, middleBase.y)
                quadraticTo(middleJoint.x, middleJoint.y, middleTip.x, middleTip.y)
                quadraticTo(middleJoint.x + 6f * scaleX, middleJoint.y, middleBase.x + 10f * scaleX, middleBase.y)

                lineTo(ringBase.x, ringBase.y)
                quadraticTo(ringJoint.x, ringJoint.y, ringTip.x, ringTip.y)
                quadraticTo(ringJoint.x + 6f * scaleX, ringJoint.y, ringBase.x + 10f * scaleX, ringBase.y)

                lineTo(pinkyBase.x, pinkyBase.y)
                quadraticTo(pinkyJoint.x, pinkyJoint.y, pinkyTip.x, pinkyTip.y)
                quadraticTo(pinkyJoint.x + 6f * scaleX, pinkyJoint.y, pinkyBase.x + 8f * scaleX, pinkyBase.y + 5f * scaleY)

                lineTo(wristRight.x, wristRight.y)
                lineTo(wristLeft.x, wristLeft.y)
            }

            drawPath(
                path = handOutline,
                color = color,
                style = Stroke(width = 4f * scaleX, cap = StrokeCap.Round)
            )
        }

        // Draw pulsing touch point overlay
        val touchOffset = when (iconCode) {
            "thumb_index", "thumb_index_down" -> Offset(65f, 85f)
            "thumb_middle" -> Offset(85f, 80f)
            "thumb_ring" -> Offset(105f, 85f)
            "thumb_pinky" -> Offset(115f, 100f)
            "thumb_ring_pinky" -> Offset(100f, 95f)
            "thumb_middle_ring" -> Offset(90f, 90f)
            "thumb_middle_ring_pinky" -> Offset(105f, 100f)
            "thumb_over_index" -> Offset(65f, 125f)
            "thumb_over_middle" -> Offset(80f, 120f)
            "thumb_over_ring" -> Offset(100f, 120f)
            "thumb_index_curled" -> Offset(80f, 120f)
            "fist", "thumb_over_ring_fist", "thumb_inside_fist", "thumb_inside_pinky_fist", "wrists_crossed_fists" -> Offset(100f, 125f)
            "palms_joined" -> Offset(100f, 110f)
            "palms_stacked", "palms_stacked_flat", "palms_stacked_heart" -> Offset(100f, 130f)
            else -> Offset(100f, 100f)
        }

        val cx = touchOffset.x * scaleX
        val cy = touchOffset.y * scaleY

        drawCircle(
            color = color.copy(alpha = (1.0f - pulseAlpha).coerceIn(0f, 1f)),
            radius = 16f * scaleX * pulseScale,
            center = Offset(cx, cy),
            style = Stroke(width = 3f * scaleX)
        )
        drawCircle(
            color = color,
            radius = 5f * scaleX,
            center = Offset(cx, cy)
        )
    }
}

@Composable
fun MudraStepImageVisualizer(
    focus: String,
    stepText: String,
    categoryColor: Color,
    coverImageRes: Int,
    iconCode: String,
    activeStepIndex: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stepPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stepPulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stepPulseAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (focus == "hand_gesture") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                DynamicHandVisualizer(
                    iconCode = iconCode,
                    modifier = Modifier.size(110.dp),
                    color = categoryColor
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "FINGER GESTURE",
                        color = categoryColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Image(
                painter = painterResource(id = coverImageRes),
                contentDescription = "Yoga step posture",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                alpha = if (activeStepIndex == -1) 1.0f else 0.7f
            )

            if (activeStepIndex >= 0) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    when (focus) {
                        "posture" -> {
                            val startY = size.height * 0.38f
                            val endY = size.height * 0.75f
                            val spineX = size.width * 0.5f
                            
                            drawLine(
                                color = categoryColor.copy(alpha = pulseAlpha * 0.3f),
                                start = Offset(spineX, startY),
                                end = Offset(spineX, endY),
                                strokeWidth = 20f * pulseScale,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = categoryColor,
                                start = Offset(spineX, startY),
                                end = Offset(spineX, endY),
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )
                        }
                        "hand_place" -> {
                            val isLap = stepText.lowercase().contains("lap") || stepText.lowercase().contains("abdomen")
                            if (isLap) {
                                val lapX = size.width * 0.5f
                                val lapY = size.height * 0.72f
                                drawCircle(
                                    color = categoryColor.copy(alpha = 1.0f - pulseAlpha),
                                    radius = 28f * pulseScale,
                                    center = Offset(lapX, lapY),
                                    style = Stroke(width = 3f)
                                )
                                drawCircle(
                                    color = categoryColor.copy(alpha = pulseAlpha),
                                    radius = 8f,
                                    center = Offset(lapX, lapY)
                                )
                            } else {
                                val leftKneeX = size.width * 0.34f
                                val rightKneeX = size.width * 0.66f
                                val kneeY = size.height * 0.72f
                                
                                drawCircle(
                                    color = categoryColor.copy(alpha = 1.0f - pulseAlpha),
                                    radius = 24f * pulseScale,
                                    center = Offset(leftKneeX, kneeY),
                                    style = Stroke(width = 3f)
                                )
                                drawCircle(
                                    color = categoryColor.copy(alpha = pulseAlpha),
                                    radius = 8f,
                                    center = Offset(leftKneeX, kneeY)
                                )
                                
                                drawCircle(
                                    color = categoryColor.copy(alpha = 1.0f - pulseAlpha),
                                    radius = 24f * pulseScale,
                                    center = Offset(rightKneeX, kneeY),
                                    style = Stroke(width = 3f)
                                )
                                drawCircle(
                                    color = categoryColor.copy(alpha = pulseAlpha),
                                    radius = 8f,
                                    center = Offset(rightKneeX, kneeY)
                                )
                            }
                        }
                        "breathing" -> {
                            val chestX = size.width * 0.5f
                            val chestY = size.height * 0.48f
                            
                            drawCircle(
                                color = categoryColor.copy(alpha = (1.0f - pulseScale / 1.3f).coerceIn(0f, 1f)),
                                radius = 40f * pulseScale,
                                center = Offset(chestX, chestY),
                                style = Stroke(width = 3f)
                            )
                            drawCircle(
                                color = categoryColor.copy(alpha = (1.0f - (pulseScale + 0.3f) % 1.3f / 1.3f).coerceIn(0f, 1f)),
                                radius = 40f * ((pulseScale + 0.3f) % 1.3f),
                                center = Offset(chestX, chestY),
                                style = Stroke(width = 2.5f)
                            )
                            drawCircle(
                                color = categoryColor.copy(alpha = 0.8f),
                                radius = 10f,
                                center = Offset(chestX, chestY)
                            )
                        }
                        "face_focus" -> {
                            val headX = size.width * 0.5f
                            val headY = size.height * 0.23f
                            
                            drawCircle(
                                color = categoryColor.copy(alpha = 1.0f - pulseAlpha),
                                radius = 20f * pulseScale,
                                center = Offset(headX, headY),
                                style = Stroke(width = 3f)
                            )
                            drawCircle(
                                color = categoryColor.copy(alpha = pulseAlpha),
                                radius = 7f,
                                center = Offset(headX, headY)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val label = when (focus) {
                        "posture" -> "SPINE POSTURE"
                        "hand_place" -> "HAND PLACEMENT"
                        "breathing" -> "DEEP BREATHING"
                        "face_focus" -> "MIND FOCUS"
                        else -> "BODY POSE"
                    }
                    Text(
                        label,
                        color = categoryColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MudraStepMiniVisualizer(
    focus: String,
    stepText: String,
    categoryColor: Color,
    coverImageRes: Int,
    iconCode: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "miniPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miniPulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miniPulseAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (focus == "hand_gesture") {
            DynamicHandVisualizer(
                iconCode = iconCode,
                modifier = Modifier.size(32.dp).align(Alignment.Center),
                color = categoryColor
            )
        } else {
            Image(
                painter = painterResource(id = coverImageRes),
                contentDescription = "Mini step posture",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.6f
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                when (focus) {
                    "posture" -> {
                        val startY = size.height * 0.38f
                        val endY = size.height * 0.75f
                        val spineX = size.width * 0.5f
                        
                        drawLine(
                            color = categoryColor,
                            start = Offset(spineX, startY),
                            end = Offset(spineX, endY),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                    "hand_place" -> {
                        val isLap = stepText.lowercase().contains("lap") || stepText.lowercase().contains("abdomen")
                        if (isLap) {
                            val lapX = size.width * 0.5f
                            val lapY = size.height * 0.72f
                            drawCircle(
                                color = categoryColor,
                                radius = 4f,
                                center = Offset(lapX, lapY)
                            )
                        } else {
                            val leftKneeX = size.width * 0.34f
                            val rightKneeX = size.width * 0.66f
                            val kneeY = size.height * 0.72f
                            
                            drawCircle(
                                color = categoryColor,
                                radius = 3f,
                                center = Offset(leftKneeX, kneeY)
                            )
                            drawCircle(
                                color = categoryColor,
                                radius = 3f,
                                center = Offset(rightKneeX, kneeY)
                            )
                        }
                    }
                    "breathing" -> {
                        val chestX = size.width * 0.5f
                        val chestY = size.height * 0.48f
                        
                        drawCircle(
                            color = categoryColor.copy(alpha = 0.5f),
                            radius = 10f * pulseScale,
                            center = Offset(chestX, chestY),
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = categoryColor,
                            radius = 3.5f,
                            center = Offset(chestX, chestY)
                        )
                    }
                    "face_focus" -> {
                        val headX = size.width * 0.5f
                        val headY = size.height * 0.23f
                        
                        drawCircle(
                            color = categoryColor,
                            radius = 3f,
                            center = Offset(headX, headY)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MudraDetailOverlay(
    mudra: YogaMudra,
    onClose: () -> Unit,
    englishStepsMap: Map<String, List<String>>,
    onStartTimer: () -> Unit
) {
    val categoryColor = getCategoryColor(mudra.category)
    var activeStepIndex by remember { mutableStateOf(-1) }
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val englishSteps = remember(mudra.id, englishStepsMap) { englishStepsMap[mudra.id] ?: mudra.steps }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                onClick = onClose,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.9f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .clickable(
                    enabled = true,
                    onClick = {},
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp, 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        mudra.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        mudra.translation,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic
                    )
                }

                val prefsManager = remember { SharedPrefsManager(context) }
                var isFavorite by remember { mutableStateOf(prefsManager.isMudraFavorite(mudra.id)) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val added = prefsManager.toggleFavoriteMudra(mudra.id)
                            isFavorite = added
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                val coverImageRes = remember(mudra.category) {
                    when (mudra.category) {
                        "Mental Calm", "Brain & Focus" -> com.pro.aifitnesstracker.R.drawable.yoga_girl_calm
                        "Energy & Vitality" -> com.pro.aifitnesstracker.R.drawable.yoga_girl_energy
                        else -> com.pro.aifitnesstracker.R.drawable.yoga_girl_healing
                    }
                }

                val activeStepFocus = remember(activeStepIndex, englishSteps) {
                    if (activeStepIndex >= 0 && activeStepIndex < englishSteps.size) {
                        val text = englishSteps[activeStepIndex].lowercase()
                        when {
                            text.contains("sit") || text.contains("posture") || text.contains("spine") || 
                            text.contains("back") || text.contains("shoulders") || text.contains("padmasana") || 
                            text.contains("sukhasana") || text.contains("vajrasana") || text.contains("pose") -> "posture"
                            
                            text.contains("knees") || text.contains("thighs") || text.contains("lap") || 
                            text.contains("place your hands") || text.contains("rest your hands") -> "hand_place"
                            
                            text.contains("breath") || text.contains("inhale") || text.contains("exhale") || 
                            text.contains("chest") || text.contains("abdomen") || text.contains("lungs") || 
                            text.contains("stomach") || text.contains("deeply") -> "breathing"
                            
                            text.contains("eyes") || text.contains("eyebrows") || text.contains("gaze") || 
                            text.contains("head") || text.contains("forehead") || text.contains("third eye") || 
                            text.contains("look") || text.contains("close") -> "face_focus"
                            
                            else -> "hand_gesture"
                        }
                    } else "none"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
                        MudraStepImageVisualizer(
                            focus = activeStepFocus,
                            stepText = if (activeStepIndex >= 0) mudra.steps[activeStepIndex] else "",
                            categoryColor = MaterialTheme.colorScheme.primary,
                            coverImageRes = coverImageRes,
                            iconCode = mudra.icon,
                            activeStepIndex = activeStepIndex
                        )

                        if (activeStepIndex >= 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 48.dp, vertical = 12.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${strings.stepFormat.substringBefore("%s").format(activeStepIndex + 1).replace(": ", "").replace(":", "").trim()}: ${mudra.steps[activeStepIndex]}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                        )
                                    )
                            )
                        }

                        IconButton(
                            onClick = {
                                activeStepIndex = if (activeStepIndex == -1) englishSteps.size - 1 else activeStepIndex - 1
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 4.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                activeStepIndex = if (activeStepIndex == englishSteps.size - 1) -1 else activeStepIndex + 1
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in -1 until mudra.steps.size) {
                                Box(
                                    modifier = Modifier
                                        .size(if (activeStepIndex == i) 8.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(if (activeStepIndex == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            mudra.category,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "⏱️ ${mudra.duration}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                MudraElementView(iconCode = mudra.icon)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    strings.physiologicalBenefits.uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        mudra.benefits,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    strings.practiceSteps.uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                mudra.steps.forEachIndexed { index, step ->
                    val isFocused = activeStepIndex == index
                    val englishStepText = englishSteps.getOrNull(index) ?: step
                    val stepFocus = remember(englishStepText) {
                        val text = englishStepText.lowercase()
                        when {
                            text.contains("sit") || text.contains("posture") || text.contains("spine") || 
                            text.contains("back") || text.contains("shoulders") || text.contains("padmasana") || 
                            text.contains("sukhasana") || text.contains("vajrasana") || text.contains("pose") -> "posture"
                            
                            text.contains("knees") || text.contains("thighs") || text.contains("lap") || 
                            text.contains("place your hands") || text.contains("rest your hands") -> "hand_place"
                            
                            text.contains("breath") || text.contains("inhale") || text.contains("exhale") || 
                            text.contains("chest") || text.contains("abdomen") || text.contains("lungs") || 
                            text.contains("stomach") || text.contains("deeply") -> "breathing"
                            
                            text.contains("eyes") || text.contains("eyebrows") || text.contains("gaze") || 
                            text.contains("head") || text.contains("forehead") || text.contains("third eye") || 
                            text.contains("look") || text.contains("close") -> "face_focus"
                            
                            else -> "hand_gesture"
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { activeStepIndex = index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.width(60.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MudraStepMiniVisualizer(
                                        focus = stepFocus,
                                        stepText = step,
                                        categoryColor = MaterialTheme.colorScheme.primary,
                                        coverImageRes = coverImageRes,
                                        iconCode = mudra.icon
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "${strings.stepFormat.substringBefore("%s").format(index + 1).replace(": ", "").replace(":", "").trim()}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Text(
                                text = step,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            Button(
                onClick = onStartTimer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        strings.startMeditation.uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MudraElementView(iconCode: String) {
    val strings = LocalAppStrings.current
    val elements = remember(strings) {
        listOf(
            Triple(strings.elementFire.uppercase(), strings.fingerThumb, Color(0xFFFF5252)),
            Triple(strings.elementAir.uppercase(), strings.fingerIndex, Color(0xFF40C4FF)),
            Triple(strings.elementSpace.uppercase(), strings.fingerMiddle, Color(0xFFE040FB)),
            Triple(strings.elementEarth.uppercase(), strings.fingerRing, Color(0xFFFFD740)),
            Triple(strings.elementWater.uppercase(), strings.fingerPinky, Color(0xFF64FFDA))
        )
    }

    val touchingIndexes = remember(iconCode) {
        when (iconCode) {
            "thumb_index" -> listOf(0, 1)
            "thumb_index_down" -> listOf(0, 1)
            "thumb_middle" -> listOf(0, 2)
            "thumb_ring" -> listOf(0, 3)
            "thumb_pinky" -> listOf(0, 4)
            "thumb_ring_pinky" -> listOf(0, 3, 4)
            "thumb_middle_ring" -> listOf(0, 2, 3)
            "thumb_middle_ring_pinky" -> listOf(0, 2, 3, 4)
            "thumb_over_index" -> listOf(0, 1)
            "thumb_over_ring" -> listOf(0, 3)
            "thumb_over_middle" -> listOf(0, 2)
            "thumb_three_fingers" -> listOf(0, 1, 2, 3)
            "thumb_index_curled" -> listOf(0, 1)
            "thumb_inside_fist" -> listOf(0, 1, 2, 3, 4)
            "thumb_inside_pinky_fist" -> listOf(0, 4)
            "all_fingers_touching" -> listOf(0, 1, 2, 3, 4)
            "fingertips_touching" -> listOf(0, 1, 2, 3, 4)
            "all_touching_thumb" -> listOf(0, 1, 2, 3, 4)
            "pushan_hand_shapes" -> listOf(0, 1, 2, 3)
            else -> emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            strings.fiveElementsBalancingGuide.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        val canvasLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        val unhighlightedDotColor = MaterialTheme.colorScheme.outlineVariant

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spacing = width / 6

                if (touchingIndexes.size > 1) {
                    val firstIdx = touchingIndexes.first()
                    val firstX = spacing * (firstIdx + 1)
                    val firstY = height / 2

                    touchingIndexes.drop(1).forEach { otherIdx ->
                        val otherX = spacing * (otherIdx + 1)
                        val otherY = height / 2
                        val controlX = (firstX + otherX) / 2
                        val controlY = (height / 2) - 40f

                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(firstX, firstY)
                            quadraticTo(controlX, controlY, otherX, otherY)
                        }

                        drawPath(
                            path = path,
                            color = canvasLineColor,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )
                    }
                }

                elements.forEachIndexed { index, (elemName, fingerName, elemColor) ->
                    val x = spacing * (index + 1)
                    val y = height / 2
                    val isHighlighted = touchingIndexes.contains(index)

                    if (isHighlighted) {
                        drawCircle(
                            color = elemColor.copy(alpha = 0.3f),
                            radius = 22f,
                            center = Offset(x, y)
                        )
                    }

                    drawCircle(
                        color = if (isHighlighted) elemColor else unhighlightedDotColor,
                        radius = 12f,
                        center = Offset(x, y)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            elements.forEachIndexed { index, (elemName, fingerName, elemColor) ->
                val isHighlighted = touchingIndexes.contains(index)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        elemName,
                        color = if (isHighlighted) elemColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        fingerName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MeditationTimerOverlay(
    mudra: YogaMudra,
    onClose: () -> Unit,
    onUnlockBadge: (String) -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { SharedPrefsManager(context) }
    val repository = remember(LocalAppStrings.current) { YogaMudraRepository(context) }
    var initialDurationMinutes by remember { mutableStateOf(5) }
    var remainingSeconds by remember { mutableStateOf(300) }
    var timerRunning by remember { mutableStateOf(false) }
    var totalElapsedSeconds by remember { mutableStateOf(0) }

    val strings = LocalAppStrings.current
    val patterns = remember(strings) {
        listOf(
            BreathingPattern(strings.breathSamaVrittiTitle, strings.breathSamaVrittiDesc, 4, 4, 4, 4),
            BreathingPattern(strings.breath478Title, strings.breath478Desc, 4, 7, 8, 0),
            BreathingPattern(strings.breathVisamaVrittiTitle, strings.breathVisamaVrittiDesc, 4, 16, 8, 0),
            BreathingPattern(strings.breathPurakaRecakaTitle, strings.breathPurakaRecakaDesc, 5, 0, 5, 0),
            BreathingPattern(strings.breathCustomTitle, strings.breathCustomDesc, 4, 2, 4, 2)
        )
    }
    var selectedPattern by remember { mutableStateOf(patterns[0]) }

    var customInhale by remember { mutableIntStateOf(4) }
    var customHoldIn by remember { mutableIntStateOf(4) }
    var customExhale by remember { mutableIntStateOf(4) }
    var customHoldOut by remember { mutableIntStateOf(4) }

    var scaleTarget by remember { mutableStateOf(1.0f) }

    val activeIn = if (selectedPattern.name == strings.breathCustomTitle) customInhale else selectedPattern.inhale
    val activeHIn = if (selectedPattern.name == "Custom Breathing") customHoldIn else selectedPattern.holdIn
    val activeEx = if (selectedPattern.name == "Custom Breathing") customExhale else selectedPattern.exhale
    val activeHOut = if (selectedPattern.name == "Custom Breathing") customHoldOut else selectedPattern.holdOut

    var breathingState by remember { mutableStateOf("Inhale") }
    val categoryColor = MaterialTheme.colorScheme.primary

    val ambientSounds = remember(strings) {
        listOf(
            AmbientSound(strings.soundNoneTitle, strings.soundNoneDesc, ""),
            AmbientSound(strings.soundTibetanBowlTitle, strings.soundTibetanBowlDesc, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
            AmbientSound(strings.soundForestRiverTitle, strings.soundForestRiverDesc, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
            AmbientSound(strings.soundDeepSpaceTitle, strings.soundDeepSpaceDesc, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
            AmbientSound(strings.soundZenGardenTitle, strings.soundZenGardenDesc, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3")
        )
    }
    var selectedAmbient by remember { mutableStateOf(ambientSounds[0]) }
    val ambientVolume = remember { mutableFloatStateOf(0.5f) }
    val mediaPlayer = remember { android.media.MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(timerRunning, selectedAmbient) {
        try {
            if (timerRunning && selectedAmbient.url.isNotEmpty()) {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(selectedAmbient.url)
                mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.setVolume(ambientVolume.floatValue, ambientVolume.floatValue)
                    mp.start()
                }
            } else {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(ambientVolume.floatValue) {
        try {
            mediaPlayer.setVolume(ambientVolume.floatValue, ambientVolume.floatValue)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val pulseScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = tween(
            durationMillis = when (breathingState) {
                "Inhale" -> activeIn * 1000
                "Exhale" -> activeEx * 1000
                else -> 2000
            },
            easing = LinearEasing
        ),
        label = "pulse"
    )

    fun logMeditationProgress() {
        if (totalElapsedSeconds > 0) {
            val elements = getElementsForMudra(mudra.icon)
            elements.forEach { elem ->
                prefsManager.incrementMudraMeditationTime(elem, totalElapsedSeconds.toFloat())
            }

            val durationSeconds = totalElapsedSeconds
            val totalSeconds = prefsManager.getTotalYogaMeditationTime() + durationSeconds
            prefsManager.incrementTotalYogaMeditationTime(durationSeconds.toFloat())
            if (totalSeconds >= 100 * 60) {
                prefsManager.checkAndUnlockBadge("MUDRA_ZEN_MASTER", onUnlockBadge)
            }

            val activeJourneyId = prefsManager.getActiveJourneyId()
            if (activeJourneyId.isNotEmpty()) {
                val progressDay = prefsManager.getJourneyProgressDay(activeJourneyId)
                val activeJourney = presetJourneys.firstOrNull { it.id == activeJourneyId }?.let { getLocalizedJourney(it, strings) }
                if (activeJourney != null) {
                    val currentDayMudra = getMudraForJourneyDay(activeJourney, progressDay, repository.getAllMudras())
                    if (mudra.id == currentDayMudra.id) {
                        val targetDurationMins = when {
                            progressDay < 7 -> 5
                            progressDay < 14 -> 10
                            else -> 15
                        }
                        if (totalElapsedSeconds >= targetDurationMins * 60 || totalElapsedSeconds >= 15) {
                            val lastCompletedTime = prefsManager.getJourneyLastCompletedTime(activeJourneyId)
                            val isSameDay = android.text.format.DateUtils.isToday(lastCompletedTime)
                            if (!isSameDay) {
                                prefsManager.incrementJourneyProgressDay(activeJourneyId)
                                prefsManager.setJourneyLastCompletedTime(activeJourneyId, System.currentTimeMillis())
                                if (progressDay == 20) {
                                    prefsManager.checkAndUnlockBadge("MUDRA_JOURNEY_COMPLETE", onUnlockBadge)
                                }
                            }
                        }
                    }
                }
            }
            totalElapsedSeconds = 0
        }
    }

    LaunchedEffect(timerRunning, selectedPattern, customInhale, customHoldIn, customExhale, customHoldOut) {
        if (timerRunning) {
            val inh = if (selectedPattern.name == "Custom Breathing") customInhale else selectedPattern.inhale
            val hldIn = if (selectedPattern.name == "Custom Breathing") customHoldIn else selectedPattern.holdIn
            val exh = if (selectedPattern.name == "Custom Breathing") customExhale else selectedPattern.exhale
            val hldOut = if (selectedPattern.name == "Custom Breathing") customHoldOut else selectedPattern.holdOut

            while (remainingSeconds > 0) {
                if (inh > 0) {
                    breathingState = "Inhale"
                    scaleTarget = 1.4f
                    delay(inh * 1000L)
                    if (!timerRunning) break
                }

                if (hldIn > 0) {
                    breathingState = "Hold"
                    scaleTarget = 1.4f
                    delay(hldIn * 1000L)
                    if (!timerRunning) break
                }

                if (exh > 0) {
                    breathingState = "Exhale"
                    scaleTarget = 1.0f
                    delay(exh * 1000L)
                    if (!timerRunning) break
                }

                if (hldOut > 0) {
                    breathingState = "Hold"
                    scaleTarget = 1.0f
                    delay(hldOut * 1000L)
                }
            }
        } else {
            breathingState = "Ready"
            scaleTarget = 1.0f
        }
    }

    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (remainingSeconds > 0) {
                delay(1000)
                if (timerRunning) {
                    remainingSeconds--
                    totalElapsedSeconds++
                }
            }
            if (remainingSeconds == 0) {
                playTibetanBell(context)
                logMeditationProgress()
                timerRunning = false
            }
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeStr = String.format("%02d:%02d", minutes, seconds)

    val timerBgCircleColor = MaterialTheme.colorScheme.surfaceVariant
    val timerArcColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    logMeditationProgress()
                    onClose()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    strings.meditationTimer.uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    mudra.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    mudra.translation,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = timerBgCircleColor,
                        radius = size.minDimension / 2 - 10f,
                        style = Stroke(width = 8f)
                    )
                }

                val progress = remainingSeconds.toFloat() / (initialDurationMinutes * 60).toFloat()
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = timerArcColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )
                }

                val scale = if (timerRunning) pulseScale else 1.0f

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (timerRunning) {
                            when (breathingState) {
                                "Inhale" -> strings.inhaleLabel.uppercase()
                                "Exhale" -> strings.exhaleLabel.uppercase()
                                "Hold" -> strings.holdInLabel.uppercase()
                                else -> breathingState.uppercase()
                            }
                        } else strings.ready.uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            timeStr,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!timerRunning && remainingSeconds == initialDurationMinutes * 60) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    // 1. Duration selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf(2, 5, 10, 15).forEach { mins ->
                            val isSelected = initialDurationMinutes == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        initialDurationMinutes = mins
                                        remainingSeconds = mins * 60
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "${mins}M",
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // 2. Breathing Pattern chips
                    Text(strings.breathingTechnique.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        patterns.forEach { pattern ->
                            val isSelected = selectedPattern.name == pattern.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        selectedPattern = pattern
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    pattern.name.substringBefore(" (").substringBefore(" Tech"),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // 3. Custom breathing controls (if Custom selected)
                    if (selectedPattern.name == "Custom Breathing") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CustomBreathItem(strings.inhaleLabel, customInhale, MaterialTheme.colorScheme.primary) { customInhale = it.coerceIn(2, 10) }
                                    CustomBreathItem(strings.holdInLabel, customHoldIn, MaterialTheme.colorScheme.primary) { customHoldIn = it.coerceIn(0, 20) }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CustomBreathItem(strings.exhaleLabel, customExhale, MaterialTheme.colorScheme.primary) { customExhale = it.coerceIn(2, 10) }
                                    CustomBreathItem(strings.holdOutLabel, customHoldOut, MaterialTheme.colorScheme.primary) { customHoldOut = it.coerceIn(0, 10) }
                                }
                            }
                        }
                    } else {
                        // Short description of selected pattern
                        Text(
                            text = "${selectedPattern.description} (${selectedPattern.inhale}s ${strings.inhaleLabel} : ${selectedPattern.holdIn}s ${strings.holdInLabel} : ${selectedPattern.exhale}s ${strings.exhaleLabel} : ${selectedPattern.holdOut}s ${strings.holdOutLabel})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4. Ambient Soundscape chips
                    Text(strings.meditationMusic.uppercase() + " (AMBIENT)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ambientSounds.forEach { sound ->
                            val isSelected = selectedAmbient.name == sound.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        selectedAmbient = sound
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    sound.name,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // 5. Volume Slider (only if an ambient sound is selected)
                    if (selectedAmbient.url.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            Text("🔊 ${strings.volume}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = ambientVolume.floatValue,
                                onValueChange = { ambientVolume.floatValue = it },
                                valueRange = 0.0f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(ambientVolume.floatValue * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${selectedPattern.name} " + strings.activeLabel + if (selectedAmbient.url.isNotEmpty()) " • 🎵 ${selectedAmbient.name}" else "",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${strings.inhaleLabel}: ${activeIn}s • ${strings.holdInLabel}: ${activeHIn}s • ${strings.exhaleLabel}: ${activeEx}s • ${strings.holdOutLabel}: ${activeHOut}s",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        timerRunning = false
                        remainingSeconds = initialDurationMinutes * 60
                        scaleTarget = 1.0f
                        breathingState = "Ready"
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { timerRunning = !timerRunning },
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = { playTibetanBell(context) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = "Bell Test", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun CustomBreathItem(label: String, value: Int, tint: Color, onValueChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${value}s", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        
        IconButton(
            onClick = { onValueChange(value - 1) },
            modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Text("-", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        IconButton(
            onClick = { onValueChange(value + 1) },
            modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Text("+", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

// Helper functions for Yoga Screen localization
fun getLocalizedCategoryName(category: String, strings: AppStrings): String {
    return when (category) {
        "All" -> strings.catAll
        "Mental Calm" -> strings.catMentalCalm
        "Healing & Therapy" -> strings.catHealingTherapy
        "Energy & Vitality" -> strings.catEnergyVitality
        "Digestion & Detox" -> strings.catDigestionDetox
        "Brain & Focus" -> strings.catBrainFocus
        "Heart & Circulation" -> strings.catHeartCirculation
        else -> category
    }
}

fun getLocalizedElementName(element: String, strings: AppStrings): String {
    return when (element) {
        "All" -> strings.catAll
        "Fire" -> strings.elementFire
        "Air" -> strings.elementAir
        "Space" -> strings.elementSpace
        "Earth" -> strings.elementEarth
        "Water" -> strings.elementWater
        else -> element
    }
}

fun getLocalizedFingerName(finger: String, strings: AppStrings): String {
    return when (finger) {
        "All" -> strings.catAll
        "Thumb" -> strings.fingerThumb
        "Index" -> strings.fingerIndex
        "Middle" -> strings.fingerMiddle
        "Ring" -> strings.fingerRing
        "Pinky" -> strings.fingerPinky
        else -> finger
    }
}

fun getLocalizedJourney(journey: MudraJourney, strings: AppStrings): MudraJourney {
    return when (journey.id) {
        "mental_calm" -> journey.copy(
            title = strings.journeyMentalCalmTitle,
            description = strings.journeyMentalCalmDesc
        )
        "energy_boost" -> journey.copy(
            title = strings.journeyEnergyBoostTitle,
            description = strings.journeyEnergyBoostDesc
        )
        "healing_detox" -> journey.copy(
            title = strings.journeyHealingDetoxTitle,
            description = strings.journeyHealingDetoxDesc
        )
        else -> journey
    }
}

fun loadEnglishMudras(context: android.content.Context): Map<String, List<String>> {
    return try {
        val jsonString = context.assets.open("yoga_mudras.json").reader().use { it.readText() }
        val jsonArray = org.json.JSONArray(jsonString)
        val map = mutableMapOf<String, List<String>>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val id = obj.getString("id")
            val stepsArray = obj.getJSONArray("steps")
            val stepsList = mutableListOf<String>()
            for (j in 0 until stepsArray.length()) {
                stepsList.add(stepsArray.getString(j))
            }
            map[id] = stepsList
        }
        map
    } catch (e: Exception) {
        emptyMap()
    }
}

// AI Poses camera components removed to simplify Yoga to Mudras only

@Composable
fun MyRoutinesScreen(
    prefsManager: SharedPrefsManager,
    repository: YogaMudraRepository,
    onSelectMudra: (YogaMudra) -> Unit,
    onStartRoutine: (CustomMudraRoutine) -> Unit,
    onUnlockBadge: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var routines by remember { mutableStateOf(prefsManager.getCustomRoutines()) }
    var favorites by remember { mutableStateOf(repository.getAllMudras().filter { prefsManager.isMudraFavorite(it.id) }) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showCreateDialog) {
        routines = prefsManager.getCustomRoutines()
        favorites = repository.getAllMudras().filter { prefsManager.isMudraFavorite(it.id) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = strings.favMudrasSection,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (favorites.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = strings.noFavMudrasYet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(favorites) { mudra ->
                        MudraFavChip(mudra = mudra, onClick = { onSelectMudra(mudra) })
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.customRoutinesSection,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Routine", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            if (routines.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = strings.noCustomRoutinesYet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                routines.forEach { routine ->
                    RoutineCard(
                        routine = routine,
                        mudrasList = repository.getAllMudras(),
                        onStart = { onStartRoutine(routine) },
                        onDelete = {
                            prefsManager.deleteCustomRoutine(routine.id)
                            routines = prefsManager.getCustomRoutines()
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        if (showCreateDialog) {
            CreateRoutineDialog(
                allMudras = repository.getAllMudras(),
                onDismiss = { showCreateDialog = false },
                onSave = { name, selectedIds, duration ->
                    val newRoutine = CustomMudraRoutine(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        mudraIds = selectedIds,
                        durationPerMudraMinutes = duration
                    )
                    val currentList = prefsManager.getCustomRoutines().toMutableList()
                    currentList.add(newRoutine)
                    prefsManager.saveCustomRoutines(currentList)
                    if (currentList.size >= 5) {
                        prefsManager.checkAndUnlockBadge("MUDRA_ROUTINE_BUILDER", onUnlockBadge)
                    }
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun MudraFavChip(mudra: YogaMudra, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val coverImageRes = remember(mudra.category) {
                when (mudra.category) {
                    "Mental Calm", "Brain & Focus" -> com.pro.aifitnesstracker.R.drawable.yoga_girl_calm
                    "Energy & Vitality" -> com.pro.aifitnesstracker.R.drawable.yoga_girl_energy
                    else -> com.pro.aifitnesstracker.R.drawable.yoga_girl_healing
                }
            }
            Image(
                painter = painterResource(id = coverImageRes),
                contentDescription = mudra.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mudra.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mudra.category,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RoutineCard(
    routine: CustomMudraRoutine,
    mudrasList: List<YogaMudra>,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val names = routine.mudraIds.mapNotNull { mid -> mudrasList.firstOrNull { it.id == mid }?.name }
                val namesText = if (names.size <= 3) names.joinToString(", ") else names.take(3).joinToString(", ") + "..."
                Text(
                    text = namesText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${routine.mudraIds.size} Mudras • ${routine.durationPerMudraMinutes * routine.mudraIds.size} ${strings.minutesLabel}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onStart,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Routine", tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Routine", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteRoutineConfirm, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(strings.deleteRoutineMsg, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text(strings.delete, color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineDialog(
    allMudras: List<YogaMudra>,
    onDismiss: () -> Unit,
    onSave: (String, List<String>, Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(5) }
    val selectedIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.createRoutineTitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(strings.routineNamePlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${strings.durationPerMudraLabel}: $duration ${strings.minutesLabel}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 1f..15f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = strings.selectMudrasTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allMudras) { mudra ->
                            val isChecked = selectedIds.contains(mudra.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedIds.remove(mudra.id)
                                        else selectedIds.add(mudra.id)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked == true) selectedIds.add(mudra.id)
                                        else selectedIds.remove(mudra.id)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(mudra.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(mudra.category, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && selectedIds.isNotEmpty(),
                onClick = { onSave(name.trim(), selectedIds.toList(), duration) }
            ) {
                Text(strings.save, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun RoutineRunnerOverlay(
    routine: CustomMudraRoutine,
    mudrasList: List<YogaMudra>,
    englishStepsMap: Map<String, List<String>>,
    onClose: () -> Unit,
    onUnlockBadge: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val prefsManager = remember { SharedPrefsManager(context) }

    val routineMudras = remember(routine.mudraIds, mudrasList) {
        routine.mudraIds.mapNotNull { mid -> mudrasList.firstOrNull { it.id == mid } }
    }

    if (routineMudras.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    var activeIndex by remember { mutableStateOf(0) }
    val activeMudra = routineMudras[activeIndex]

    var remainingSeconds by remember { mutableStateOf(routine.durationPerMudraMinutes * 60) }
    var timerRunning by remember { mutableStateOf(true) }
    var showIntermission by remember { mutableStateOf(false) }
    var intermissionSeconds by remember { mutableStateOf(5) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    val ambientSounds = remember(strings) {
        listOf(
            AmbientSound(strings.soundNoneTitle, strings.soundNoneDesc, ""),
            AmbientSound(strings.soundTibetanBowlTitle, strings.soundTibetanBowlDesc, "https://assets.mixkit.co/active_storage/sfx/2568/2568-84.wav"),
            AmbientSound(strings.soundForestRiverTitle, strings.soundForestRiverDesc, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
            AmbientSound(strings.soundDeepSpaceTitle, strings.soundDeepSpaceDesc, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
            AmbientSound(strings.soundZenGardenTitle, strings.soundZenGardenDesc, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3")
        )
    }
    var selectedSound by remember { mutableStateOf(ambientSounds[1]) }
    var volume by remember { mutableStateOf(0.5f) }

    val mediaPlayer = remember { MediaPlayer() }
    
    DisposableEffect(selectedSound, timerRunning, showIntermission) {
        if (timerRunning && selectedSound.url.isNotEmpty() && !showIntermission) {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(selectedSound.url)
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mediaPlayer.setVolume(volume, volume)
                mediaPlayer.isLooping = true
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { mp ->
                    if (timerRunning && !showIntermission) mp.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
        }
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
        }
    }

    LaunchedEffect(volume) {
        try {
            mediaPlayer.setVolume(volume, volume)
        } catch (e: Exception) {}
    }

    LaunchedEffect(timerRunning, showIntermission, activeIndex) {
        while (timerRunning) {
            delay(1000L)
            if (showIntermission) {
                if (intermissionSeconds > 1) {
                    intermissionSeconds--
                } else {
                    showIntermission = false
                    remainingSeconds = routine.durationPerMudraMinutes * 60
                }
            } else {
                if (remainingSeconds > 1) {
                    remainingSeconds--
                } else {
                    val elements = getElementsForMudra(activeMudra.icon)
                    elements.forEach { elem ->
                        prefsManager.incrementMudraMeditationTime(elem, (routine.durationPerMudraMinutes * 60).toFloat())
                    }
                    val durationSeconds = routine.durationPerMudraMinutes * 60
                    val totalSeconds = prefsManager.getTotalYogaMeditationTime() + durationSeconds
                    prefsManager.incrementTotalYogaMeditationTime(durationSeconds.toFloat())
                    if (totalSeconds >= 100 * 60) {
                        prefsManager.checkAndUnlockBadge("MUDRA_ZEN_MASTER", onUnlockBadge)
                    }

                    try {
                        val chimePlayer = MediaPlayer.create(context, com.pro.aifitnesstracker.R.raw.tibetan_bell)
                        chimePlayer?.start()
                        chimePlayer?.setOnCompletionListener { it.release() }
                    } catch (e: Exception) {}

                    if (activeIndex < routineMudras.size - 1) {
                        activeIndex++
                        intermissionSeconds = 5
                        showIntermission = true
                    } else {
                        timerRunning = false
                        showCompletionDialog = true
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.release()
            } catch (e: Exception) {}
        }
    }

    val runnerCircleTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val runnerCircleArcColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                }
                
                Text(
                    text = routine.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${activeIndex + 1}/${routineMudras.size}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showIntermission) {
                val nextMudra = routineMudras.getOrNull(activeIndex)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = strings.transitionNextMudra.format(nextMudra?.name ?: ""),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "$intermissionSeconds",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = activeMudra.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = activeMudra.translation,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(220.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = runnerCircleTrackColor,
                                radius = size.minDimension / 2 - 10f,
                                style = Stroke(width = 8f)
                            )
                        }

                        val progress = remainingSeconds.toFloat() / (routine.durationPerMudraMinutes * 60).toFloat()
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = runnerCircleArcColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = 8f, cap = StrokeCap.Round)
                            )
                        }

                        val mins = remainingSeconds / 60
                        val secs = remainingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        DynamicHandVisualizer(
                            iconCode = activeMudra.icon,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ambientSounds.forEach { sound ->
                        val isSelected = selectedSound == sound
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedSound = sound }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sound.name,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (selectedSound.url.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎵", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { timerRunning = !timerRunning },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (timerRunning) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = {
                showCompletionDialog = false
                onClose()
            },
            title = { Text(strings.routineCompleted, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text(strings.routineCompletedMsg, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCompletionDialog = false
                        onClose()
                    }
                ) {
                    Text(strings.ok, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun BadgeUnlockedDialog(
    badgeId: String,
    prefsManager: SharedPrefsManager,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    val allBadges = listOf(
        BadgeData("MUDRA_JOURNEY_START", strings.badgeMudraJourneyStartTitle, strings.badgeMudraJourneyStartDesc, Color(0xFFA78BFA)),
        BadgeData("MUDRA_ROUTINE_BUILDER", strings.badgeMudraRoutineBuilderTitle, strings.badgeMudraRoutineBuilderDesc, Color(0xFF38BDF8)),
        BadgeData("MUDRA_ZEN_MASTER", strings.badgeMudraZenMasterTitle, strings.badgeMudraZenMasterDesc, Color(0xFFFBBF24)),
        BadgeData("MUDRA_JOURNEY_COMPLETE", strings.badgeMudraJourneyCompleteTitle, strings.badgeMudraJourneyCompleteDesc, Color(0xFF34D399))
    )
    val badge = allBadges.firstOrNull { it.id == badgeId } ?: return

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.badgeUnlockedCongratulate.uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.badgeUnlockedSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    badge.color.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(badge.color, badge.color.copy(alpha = 0.3f), badge.color)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = badge.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = badge.desc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(text = strings.badgeUnlockedClose, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

