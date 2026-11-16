package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import com.pro.aifitnesstracker.LocalAppStrings

// ── Asset Image Loader ────────────────────────────────────────────────────
@Composable
fun rememberAsanaImage(asanaId: String, stepIndex: Int? = null, stepText: String = ""): ImageBitmap? {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    return remember(asanaId, stepIndex, stepText) {
        val extensions = listOf("jpg", "jpeg", "webp", "png")
        var bitmap: android.graphics.Bitmap? = null

        val fallbackMap = mapOf(
            "cat_cow" to "bhujangasana",
            "viparita_karani" to "balasana",
            "savasana" to "balasana",
            "paschimottanasana" to "navasana",
            "vasisthasana" to "plank",
            "ustrasana" to "bhujangasana",
            "ardha_chandrasana" to "trikonasana",
            "garudasana" to "vrksasana",
            "dhanurasana" to "bhujangasana",
            "chakrasana" to "setu_bandha",
            "sirsasana" to "adho_mukha",
            "sarvangasana" to "setu_bandha",
            "bakasana" to "plank",
            "mayurasana" to "plank",
            "padmasana" to "balasana",
            "vrschikasana" to "bhujangasana",
            "tittibhasana" to "navasana"
        )
        
        val targetId = if (context.assets.list("yoga_poses")?.any { it.startsWith(asanaId) } == true) {
            asanaId
        } else {
            fallbackMap[asanaId] ?: asanaId
        }

        // 1. Try explicit step-specific image first (e.g. tadasana_step_1.jpg, tadasana_step_2.jpg... tadasana_step_6.jpg)
        if (stepIndex != null) {
            val stepNumber = stepIndex + 1
            for (ext in extensions) {
                try {
                    val inputStream = context.assets.open("yoga_poses/${targetId}_step_${stepNumber}.$ext")
                    bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (bitmap != null) break
                } catch (_: Exception) {}
            }
            if (bitmap != null) return@remember bitmap.asImageBitmap()
        }

        // 2. Try action pose image if available
        if (stepIndex != null && stepIndex > 0) {
            val txt = stepText.lowercase()
            val stepActionPose = when {
                txt.contains("tabletop") || txt.contains("all fours") -> "adho_mukha_step_1"
                txt.contains("flat") || txt.contains("belly") || txt.contains("stomach") || txt.contains("prone") -> "bhujangasana_step_1"
                txt.contains("kneel") -> "balasana_step_1"
                else -> null
            }
            if (stepActionPose != null) {
                for (ext in extensions) {
                    try {
                        val inputStream = context.assets.open("yoga_poses/$stepActionPose.$ext")
                        bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        if (bitmap != null) break
                    } catch (_: Exception) {}
                }
                if (bitmap != null) return@remember bitmap.asImageBitmap()
            }
            // For remaining steps 2..6 without dedicated step image, return null to show distinct Step Action Focus Card instead of repeating main pose photo
            return@remember null
        }

        // 3. For main pose card or step 1, load main target pose image
        for (ext in extensions) {
            try {
                val inputStream = context.assets.open("yoga_poses/$targetId.$ext")
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (bitmap != null) break
            } catch (_: Exception) {}
        }
        
        bitmap?.asImageBitmap()
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────
fun getAsanaCategoryColor(cat: String): Color = when (cat) {
    ASANA_CAT_MORNING     -> Color(0xFFFFB74D)
    ASANA_CAT_POWER       -> Color(0xFFE57373)
    ASANA_CAT_FLEXIBILITY -> Color(0xFF64B5F6)
    ASANA_CAT_SLEEP       -> Color(0xFF4DB6AC)
    ASANA_CAT_STRESS      -> Color(0xFFB39DDB)
    ASANA_CAT_CORE        -> Color(0xFFFF8A65)
    ASANA_CAT_THERAPY     -> Color(0xFF81C784)
    else                  -> Color(0xFF9FA8DA)
}

fun getLevelColor(level: String): Color = when (level.lowercase()) {
    "beginner", "नवशिक्या", "શરૂઆત", "தொடக்கநிலை", "ప్రారంభకుడు", "ಆರಂಭಿಕ", "শিক্ষানবিস", "ਸ਼ੁਰੂਆਤੀ", "principiante", "débutant", "anfänger", "iniciante", "новичок", "مبتدئ", "初级", "初級" -> Color(0xFF66BB6A)
    "intermediate", "मध्यवर्ती", "મધ્યમ", "நடுத்தர", "మధ్యస్థం", "ಮಧ್ಯಮ", "মধ্যবর্তী", "ਦਰਮਿਆਨਾ", "intermedio", "intermédiaire", "mittelstufe", "средний", "متوسط", "中级", "中級" -> Color(0xFFFFB74D)
    "advanced", "प्रगत", "ઉન્નત", "மேம்பட்ட", "అధునాతన", "ಸುಧಾರಿತ", "উন্নত", "ਉੱਚ ਪੱਧਰੀ", "avanzado", "avancé", "fortgeschritten", "avançado", "продвинутый", "متقدم", "高级", "上級" -> Color(0xFFE57373)
    else -> Color(0xFF9FA8DA)
}

private fun formatHoldTime(seconds: Int): String = when {
    seconds >= 60 -> "${seconds / 60} min"
    else          -> "$seconds sec"
}

// SharedPreferences helpers for program progress
private fun getProgramDay(ctx: Context, programId: String): Int =
    ctx.getSharedPreferences("yoga_programs", Context.MODE_PRIVATE)
        .getInt("day_$programId", 0)

private fun setProgramDay(ctx: Context, programId: String, day: Int) =
    ctx.getSharedPreferences("yoga_programs", Context.MODE_PRIVATE).edit()
        .putInt("day_$programId", day).apply()

// ── Internal Nav State ────────────────────────────────────────────────────
private sealed class AsanaNav {
    object Library  : AsanaNav()
    object Programs : AsanaNav()
    data class Detail(val asana: YogaAsana)                           : AsanaNav()
    data class Session(val poses: List<YogaAsana>, val title: String) : AsanaNav()
}

// ── Root Entry Point ──────────────────────────────────────────────────────
@Composable
fun YogaAsanaSection(
    onOpenMudras: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    var nav by remember { mutableStateOf<AsanaNav>(AsanaNav.Library) }

    AnimatedContent(
        targetState = nav,
        transitionSpec = {
            if (targetState is AsanaNav.Library || targetState is AsanaNav.Programs)
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            else
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
        },
        label = "asana_nav"
    ) { state ->
        when (state) {
            is AsanaNav.Library  -> YogaAsanaLibraryScreen(
                onSelectAsana   = { nav = AsanaNav.Detail(it) },
                onGoPrograms    = { nav = AsanaNav.Programs },
                onStartGuidedFlow = { poses, title ->
                    if (poses.isNotEmpty()) {
                        nav = AsanaNav.Session(poses, title)
                    }
                },
                onOpenMudras = onOpenMudras
            )
            is AsanaNav.Programs -> YogaProgramsScreen(
                onBack          = { nav = AsanaNav.Library },
                onStartSession  = { poses, title -> nav = AsanaNav.Session(poses, title) }
            )
            is AsanaNav.Detail   -> YogaAsanaPoseDetailScreen(
                asana           = state.asana,
                onBack          = { nav = AsanaNav.Library },
                onStartSession  = { nav = AsanaNav.Session(listOf(state.asana), state.asana.name) }
            )
            is AsanaNav.Session  -> YogaGuidedSessionScreen(
                poses           = state.poses,
                sessionTitle    = state.title,
                onFinish        = { nav = AsanaNav.Library }
            )
        }
    }
}

private data class GuidedYogaFlowSpec(
    val title: String,
    val subtitle: String,
    val categoryTag: String,
    val posesSummary: List<Pair<String, String>>,
    val asanaIds: List<String>
)

// ── 1. Asana & Guided Flows Studio Screen (Exact Mockup Match) ────────────
@Composable
fun YogaAsanaLibraryScreen(
    onSelectAsana: (YogaAsana) -> Unit,
    onGoPrograms:  () -> Unit,
    onStartGuidedFlow: (List<YogaAsana>, String) -> Unit = { _, _ -> },
    onOpenMudras: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF3F6FB)
    val cardBg = if (isDark) Color(0xFF101726) else Color(0xFFE6ECF5)
    val chipBg = if (isDark) Color(0xFF151E30) else Color(0xFFDCE4F0)
    val cardBorderColor = if (isDark) Color(0xFF26334D) else Color(0xFFBCC6D8)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val filterTabs = listOf(strings.allFlows, strings.flexibility, strings.stressRelief, strings.bedtimeYoga, "🤲 Mudras")
    var selectedTab by remember { mutableStateOf(strings.allFlows) }
    var searchQuery by remember { mutableStateOf("") }

    val guidedFlows = remember {
        listOf(
            GuidedYogaFlowSpec(
                title = "Full Body Flexibility Flow",
                subtitle = "15 min • Beginner • 8 Poses",
                categoryTag = strings.flexibility,
                posesSummary = listOf(
                    "Downward Dog (Adho Mukha)" to "60s",
                    "Cobra Pose (Bhujangasana)" to "45s",
                    "Warrior II (Virabhadrasana)" to "60s"
                ),
                asanaIds = listOf("adho_mukha", "bhujangasana", "virabhadrasana_2", "trikonasana", "balasana")
            ),
            GuidedYogaFlowSpec(
                title = "Deep Sleep & Pranayama Breath",
                subtitle = "12 min • Relaxation • Ambient Sound",
                categoryTag = strings.bedtimeYoga,
                posesSummary = listOf(
                    "Child's Pose (Balasana)" to "90s",
                    "Legs Up the Wall (Viparita Karani)" to "120s",
                    "Corpse Pose (Savasana)" to "180s"
                ),
                asanaIds = listOf("balasana", "viparita_karani", "savasana")
            ),
            GuidedYogaFlowSpec(
                title = "Mindful Stress Relief & Calm",
                subtitle = "18 min • All Levels • 6 Poses",
                categoryTag = strings.stressRelief,
                posesSummary = listOf(
                    "Seated Lotus (Padmasana)" to "90s",
                    "Cat-Cow Stretch (Marjaryasana)" to "60s",
                    "Bridge Pose (Setu Bandha)" to "60s"
                ),
                asanaIds = listOf("padmasana", "cat_cow", "setu_bandha", "balasana")
            )
        )
    }

    val visibleFlows = remember(selectedTab) {
        if (selectedTab == strings.allFlows) guidedFlows
        else guidedFlows.filter { it.categoryTag.equals(selectedTab, ignoreCase = true) }.ifEmpty { guidedFlows }
    }

    val filteredAsanas = remember(selectedTab, searchQuery) {
        allYogaAsanas.filter { a ->
            val matchesTab = when (selectedTab) {
                strings.flexibility -> a.category == ASANA_CAT_FLEXIBILITY || a.category == ASANA_CAT_MORNING
                strings.stressRelief -> a.category == ASANA_CAT_STRESS || a.category == ASANA_CAT_THERAPY
                strings.bedtimeYoga -> a.category == ASANA_CAT_SLEEP || a.category == ASANA_CAT_STRESS
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                a.name.contains(searchQuery, true) ||
                a.sanskritName.contains(searchQuery, true) ||
                a.targetBodyPart.contains(searchQuery, true)
            matchesTab && matchesQuery
        }
    }

    val resolvePoses: (List<String>) -> List<YogaAsana> = { ids ->
        val matched = ids.mapNotNull { id -> allYogaAsanas.find { it.id == id } }
        matched.ifEmpty { allYogaAsanas.take(5) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // ── 1. Hero "Sun Salutation (Surya Namaskar)" Card with 3D Lotus Yogi ──
        item {
            val heroBgBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF281C46),
                    Color(0xFF1A2842),
                    Color(0xFF123644)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(900f, 400f)
            )
            val heroBorderBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFC084FC),
                    Color(0xFF818CF8),
                    Color(0xFF2DD4BF)
                )
            )
            val startHeroBtnBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFC084FC),
                    Color(0xFF67E8F9),
                    Color(0xFF5EEAD4)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0xFF2DD4BF),
                        spotColor = Color(0xFFA855F7)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(heroBgBrush)
                    .border(1.5.dp, heroBorderBrush, RoundedCornerShape(24.dp))
                    .clickable {
                        val sunPoses = resolvePoses(listOf("tadasana", "uttanasana", "plank", "bhujangasana", "adho_mukha"))
                        onStartGuidedFlow(sunPoses, "Sun Salutation (Surya Namaskar)")
                    }
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1.25f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Sun Salutation (Surya Namaskar)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            lineHeight = 23.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metadata Pill: "20 min • Flexibility & Calm • 120 Kcal"
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF0F172A).copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                            Text(
                                text = "20 min • Flexibility & Calm • 120 Kcal",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Glowing "▶ Start Guided Flow" Pill CTA
                        Box(
                            modifier = Modifier
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = Color(0xFF5EEAD4),
                                    spotColor = Color(0xFFC084FC)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(startHeroBtnBrush)
                                .clickable {
                                    val sunPoses = resolvePoses(listOf("tadasana", "uttanasana", "plank", "bhujangasana", "adho_mukha"))
                                    onStartGuidedFlow(sunPoses, "Sun Salutation (Surya Namaskar)")
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Start Guided Flow",
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Right: 3D Glowing Holographic Lotus Yogi Artwork
                    Image(
                        painter = painterResource(id = com.pro.aifitnesstracker.R.drawable.yoga_hero_lotus_yogi),
                        contentDescription = "3D Meditating Lotus Yogi",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(width = 124.dp, height = 138.dp)
                    )
                }
            }
        }

        // ── 2. 3-Column Mindful Stats Bar (🧘 6 Sessions | 🕒 140 Mins | ✨ 92% Calm) ──
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 15.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    YogaMindfulStatItem(
                        emoji = "🧘",
                        value = "6",
                        label = "Sessions",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(36.dp),
                        color = cardBorderColor.copy(alpha = 0.6f)
                    )
                    YogaMindfulStatItem(
                        emoji = "🕒",
                        value = "140",
                        label = "Mins",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(36.dp),
                        color = cardBorderColor.copy(alpha = 0.6f)
                    )
                    YogaMindfulStatItem(
                        emoji = "✨",
                        value = "92%",
                        label = "Calm",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── 3. Interactive Category Filter Pills Row ──
        item {
            val activePillBrush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF5EEAD4), Color(0xFF67E8F9), Color(0xFFA78BFA))
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filterTabs) { tab ->
                    val isSelected = selectedTab == tab
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = Color(0xFF2DD4BF),
                                    spotColor = Color(0xFFA855F7)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(activePillBrush)
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = Color(0xFF0F172A),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Surface(
                            onClick = {
                                if (tab.contains("Mudras")) {
                                    onOpenMudras()
                                } else {
                                    selectedTab = tab
                                }
                            },
                            shape = RoundedCornerShape(50),
                            color = chipBg,
                            border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.8f))
                        ) {
                            Text(
                                text = tab,
                                color = textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── 4. Guided Yoga & Asana Flow Cards (Exact Mockup Design) ──
        items(visibleFlows, key = { it.title }) { flow ->
            val flowBorderBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2DD4BF).copy(alpha = 0.65f),
                    cardBorderColor,
                    Color(0xFFA855F7).copy(alpha = 0.65f)
                )
            )
            val startBtnBgBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF0F3B4C),
                    Color(0xFF1F2B52),
                    Color(0xFF2E2252)
                )
            )
            val startBtnBorderBrush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF2DD4BF), Color(0xFFA78BFA))
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = BorderStroke(1.3.dp, flowBorderBrush),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 7.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = flow.title,
                                color = textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 25.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = flow.subtitle,
                                color = textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Glowing "▶ Start" Pill Button
                        Box(
                            modifier = Modifier
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = Color(0xFF2DD4BF),
                                    spotColor = Color(0xFFA855F7)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(startBtnBgBrush)
                                .border(1.4.dp, startBtnBorderBrush, RoundedCornerShape(50))
                                .clickable {
                                    onStartGuidedFlow(resolvePoses(flow.asanaIds), flow.title)
                                }
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Start Flow",
                                    tint = Color(0xFF5EEAD4),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Start",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = cardBorderColor.copy(alpha = 0.55f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Numbered Pose List with Hold Duration on Right
                    flow.posesSummary.forEachIndexed { index, (poseName, holdTime) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    color = textSecondary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    text = poseName,
                                    color = textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = holdTime,
                                color = textSecondary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // ── 5. Individual Asanas Library Header + Multi-Day Programs Button ──
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Explore All 30 Asanas",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${filteredAsanas.size} step-by-step poses available",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    onClick = onGoPrograms,
                    shape = RoundedCornerShape(50),
                    color = chipBg,
                    border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF2DD4BF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Programs",
                            color = textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Individual Asana Cards
        items(filteredAsanas, key = { it.id }) { asana ->
            Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp)) {
                AsanaCard(asana = asana, onClick = { onSelectAsana(asana) })
            }
        }
    }
}

@Composable
private fun YogaMindfulStatItem(
    emoji: String,
    value: String,
    label: String,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 17.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = value,
                color = textPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── 1-Column Asana Row Card ───────────────────────────────────────────────
@Composable
fun AsanaCard(asana: YogaAsana, onClick: () -> Unit) {
    val strings = LocalAppStrings.current
    val catColor  = getAsanaCategoryColor(asana.category)
    val lvlColor  = getLevelColor(asana.level)
    var pressed   by remember { mutableStateOf(false) }
    val scale     by animateFloatAsState(if (pressed) 0.98f else 1f, label = "card_scale")
    val poseImage = rememberAsanaImage(asana.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                pressed = true
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Studio Photo Thumbnail Box
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (poseImage != null) {
                    Image(
                        bitmap = poseImage,
                        contentDescription = asana.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Text(asana.emoji, fontSize = 38.sp)
                }
            }

            Spacer(Modifier.width(14.dp))

            // Center Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        asana.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    // Level badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(lvlColor.copy(0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(asana.level, color = lvlColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Text(
                    asana.sanskritName,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Hold time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(formatHoldTime(asana.holdSeconds), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    // Calories
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("${(asana.caloriesPerMin * asana.holdSeconds / 60f).toInt().coerceAtLeast(2)} kcal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Target Body Part Tag
                Text(
                    "🎯 ${asana.targetBodyPart}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right Arrow Indicator Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Guidance",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── 2. Pose Detail Screen (100% Real Human Studio Photo — ZERO SCROLL VIEW) ─────
@Composable
fun YogaAsanaPoseDetailScreen(
    asana: YogaAsana,
    onBack: () -> Unit,
    onStartSession: () -> Unit
) {
    val strings = LocalAppStrings.current
    val catColor = getAsanaCategoryColor(asana.category)
    val lvlColor = getLevelColor(asana.level)

    var currentStepIndex by remember { mutableIntStateOf(0) }

    val totalSteps = asana.steps.size.coerceAtLeast(1)
    val stepText = asana.steps.getOrElse(currentStepIndex) { "" }
    val posePhoto = rememberAsanaImage(asana.id, currentStepIndex, stepText)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(asana.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(asana.sanskritName, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(lvlColor.copy(0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(asana.level, color = lvlColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(4.dp))

        // 2. MAIN HERO REAL HUMAN STUDIO PHOTO DISPLAY AREA (100% CLEAN & ZERO OVERLAP)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (posePhoto != null) {
                Image(
                    bitmap = posePhoto,
                    contentDescription = "${asana.name} Step ${currentStepIndex + 1} Studio Photo Guide",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            } else {
                // Distinct Step Action Focus Card for steps without dedicated photo
                val textLower = stepText.lowercase()
                val stepActionIcon = when {
                    textLower.contains("feet") || textLower.contains("heel") || textLower.contains("ankle") || textLower.contains("toes") -> "🦶"
                    textLower.contains("knee") || textLower.contains("thigh") || textLower.contains("leg") || textLower.contains("hip") -> "🦵"
                    textLower.contains("core") || textLower.contains("spine") || textLower.contains("back") || textLower.contains("chest") || textLower.contains("belly") -> "🔥"
                    textLower.contains("arm") || textLower.contains("shoulder") || textLower.contains("hand") || textLower.contains("head") || textLower.contains("crown") -> "🙆"
                    else -> asana.emoji
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stepActionIcon, fontSize = 64.sp)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "STEP ${currentStepIndex + 1} OF ${asana.steps.size}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        val textLower = stepText.lowercase()
        val focusTag = remember(stepText) {
            when {
                textLower.contains("feet") || textLower.contains("heel") || textLower.contains("ankle") || textLower.contains("toes") || textLower.contains("floor") || textLower.contains("mat") ->
                    "🦶 FEET GROUNDING: Press feet firmly into mat"
                textLower.contains("knee") || textLower.contains("thigh") || textLower.contains("leg") || textLower.contains("hip") ->
                    "🦵 LEG ALIGNMENT: Engage thighs & lift kneecaps"
                textLower.contains("core") || textLower.contains("spine") || textLower.contains("back") || textLower.contains("chest") || textLower.contains("belly") || textLower.contains("torso") ->
                    "🔥 CORE STABILITY: Engage core & lengthen spine"
                textLower.contains("arm") || textLower.contains("shoulder") || textLower.contains("hand") || textLower.contains("head") || textLower.contains("crown") || textLower.contains("palms") ->
                    "🙆 UPPER BODY: Roll shoulders & extend arms"
                else ->
                    "✨ POSE FORM: Maintain steady alignment & breath"
            }
        }

        Spacer(Modifier.height(8.dp))

        // 3. STEP-BY-STEP GUIDANCE CARD (Compact, Clean & Fit)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Step Selection Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    asana.steps.forEachIndexed { index, _ ->
                        val isSelected = currentStepIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { currentStepIndex = index }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Step ${index + 1}",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Action Focus Tag Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        focusTag,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Current Step Instruction Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${currentStepIndex + 1}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stepText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Step Navigation Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentStepIndex > 0) currentStepIndex-- },
                        enabled = currentStepIndex > 0,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            tint = if (currentStepIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Prev Step",
                            color = if (currentStepIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "Step ${currentStepIndex + 1} of $totalSteps",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )

                    TextButton(
                        onClick = { if (currentStepIndex < totalSteps - 1) currentStepIndex++ },
                        enabled = currentStepIndex < totalSteps - 1,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Next Step",
                            color = if (currentStepIndex < totalSteps - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = if (currentStepIndex < totalSteps - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 4. QUICK STATS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatChip(icon = Icons.Default.Timer, label = formatHoldTime(asana.holdSeconds), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            StatChip(icon = Icons.Rounded.FitnessCenter, label = asana.targetBodyPart, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            StatChip(icon = Icons.Default.LocalFireDepartment, label = "${(asana.caloriesPerMin * asana.holdSeconds / 60f).toInt().coerceAtLeast(2)} kcal", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        // 5. BOTTOM ACTION BUTTON — Full Width Start Guided Session (Pushed safely above Android System Nav Bar)
        Button(
            onClick = onStartSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(6.dp))
            Text(
                "Start Guided Pose Session",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionCard(title: String, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}

// ── 3. Guided Session Screen ──────────────────────────────────────────────
@Composable
fun YogaGuidedSessionScreen(
    poses: List<YogaAsana>,
    sessionTitle: String,
    onFinish: () -> Unit
) {
    val strings = LocalAppStrings.current
    if (poses.isEmpty()) { onFinish(); return }

    var activeIndex   by remember { mutableIntStateOf(0) }
    var remaining     by remember { mutableIntStateOf(poses.first().holdSeconds) }
    var isPaused      by remember { mutableStateOf(false) }
    var isResting     by remember { mutableStateOf(false) }
    var restRemaining by remember { mutableIntStateOf(10) }
    var isFinished    by remember { mutableStateOf(false) }

    val activePose    = poses.getOrNull(activeIndex)
    val catColor      = activePose?.let { getAsanaCategoryColor(it.category) } ?: Color(0xFF9FA8DA)

    // Animated progress
    val totalTime = activePose?.holdSeconds ?: 1
    val progress  by animateFloatAsState(
        targetValue = if (isResting) restRemaining / 10f else remaining.toFloat() / totalTime,
        animationSpec = tween(500), label = "progress"
    )

    // Timer
    LaunchedEffect(activeIndex, isPaused, isResting) {
        while (true) {
            delay(1000)
            if (!isPaused && !isFinished) {
                if (isResting) {
                    if (restRemaining > 0) {
                        restRemaining--
                    } else {
                        isResting = false
                        restRemaining = 10
                        remaining = poses[activeIndex].holdSeconds
                    }
                } else {
                    if (remaining > 0) {
                        remaining--
                    } else {
                        // Move to next pose or rest
                        if (activeIndex < poses.size - 1) {
                            isResting = true
                            restRemaining = 10
                        } else {
                            isFinished = true
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(isResting) {
        if (!isResting && activeIndex < poses.size - 1 && restRemaining == 10) {
            // Check if we were just resting
        }
    }

    // Automatically save yoga session & calories burned to HealthRepository for Main Dashboard live sync
    val context = LocalContext.current
    LaunchedEffect(isFinished) {
        if (isFinished) {
            withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val prefs = SharedPrefsManager(context)
                    val repository = HealthRepository(db.healthDao(), prefs)

                    val totalSecs = poses.sumOf { it.holdSeconds }
                    val totalCals = poses.sumOf { (it.caloriesPerMin * it.holdSeconds / 60).toInt() }.coerceAtLeast(5)

                    // 1. Save to Yoga History DB
                    repository.saveYogaSession(
                        pose = sessionTitle,
                        duration = totalSecs,
                        accuracy = 95
                    )

                    // 2. Save to Workout History DB (so calories & active time update on Main Dashboard)
                    repository.saveWorkout(
                        type = "Yoga: $sessionTitle",
                        value = (totalSecs / 60).coerceAtLeast(1),
                        calories = totalCals
                    )
                } catch (_: Exception) {}
            }
        }
    }

    // Session complete screen
    if (isFinished) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text("🎉", fontSize = 72.sp)
                Text("Session Complete!", color = MaterialTheme.colorScheme.onBackground, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text(
                    "You completed $sessionTitle\n${poses.size} pose${if (poses.size > 1) "s" else ""}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
                )

                val totalSecs = poses.sumOf { it.holdSeconds }
                val totalCals = poses.sumOf { (it.caloriesPerMin * it.holdSeconds / 60).toInt() }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SessionStatBox("⏱️", "${totalSecs / 60} min", "Duration")
                    SessionStatBox("🔥", "$totalCals kcal", "Burned")
                    SessionStatBox("🧘", "${poses.size}", "Poses")
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onFinish,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Back to Library", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
        return
    }

    val timerTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val timerSweepColor = MaterialTheme.colorScheme.primary

    // Active session UI
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFinish) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit", tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(sessionTitle, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Pose ${activeIndex + 1} of ${poses.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }

        // Progress indicators (dots)
        val activeDotColor = MaterialTheme.colorScheme.primary
        val inactiveDotColor = MaterialTheme.colorScheme.surfaceVariant
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            poses.forEachIndexed { i, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                i < activeIndex  -> activeDotColor
                                i == activeIndex -> activeDotColor.copy(0.6f)
                                else             -> inactiveDotColor
                            }
                        )
                )
            }
        }

        // Rest screen
        if (isResting) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("😮‍💨", fontSize = 64.sp)
                    Text("REST", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("$restRemaining sec", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    val nextPose = poses.getOrNull(activeIndex + 1)
                    if (nextPose != null) {
                        Text("Next: ${nextPose.name}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Text(nextPose.emoji, fontSize = 32.sp)
                    }
                }
            }
        } else {
            // Pose display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(activePose?.emoji ?: "", fontSize = 80.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(activePose?.name ?: "", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(activePose?.sanskritName ?: "", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }

            // Timer ring
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
                    val stroke = 12f
                    val sweepAngle = progress * 360f
                    drawCircle(color = timerTrackColor, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                    drawArc(
                        color = timerSweepColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$remaining",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("sec", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            // Current pose hint
            activePose?.steps?.firstOrNull()?.let { hint ->
                Text(
                    hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.weight(0.05f))
        }

        // Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip back
            IconButton(
                onClick = {
                    if (activeIndex > 0) {
                        activeIndex--
                        remaining = poses[activeIndex].holdSeconds
                        isResting = false
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }

            // Pause / Play
            IconButton(
                onClick = { isPaused = !isPaused },
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Play" else "Pause",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Skip forward
            IconButton(
                onClick = {
                    if (activeIndex < poses.size - 1) {
                        activeIndex++
                        remaining = poses[activeIndex].holdSeconds
                        isResting = false
                    } else {
                        isFinished = true
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun SessionStatBox(emoji: String, value: String, label: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

// ── 4. Programs Screen ────────────────────────────────────────────────────
@Composable
fun YogaProgramsScreen(
    onBack: () -> Unit,
    onStartSession: (List<YogaAsana>, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                "YOGA PROGRAMS",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(allYogaPrograms) { program ->
                YogaProgramCard(
                    program = program,
                    context = context,
                    onStart = {
                        val dayIndex = getProgramDay(context, program.id)
                        val poseId  = program.poseIds.getOrElse(dayIndex % program.poseIds.size) { program.poseIds.first() }
                        val pose    = allYogaAsanas.firstOrNull { it.id == poseId }
                        if (pose != null) {
                            setProgramDay(context, program.id, (dayIndex + 1).coerceAtMost(program.durationDays))
                            onStartSession(listOf(pose), "${program.name} — Day ${dayIndex + 1}")
                        }
                    },
                    onReset = {
                        setProgramDay(context, program.id, 0)
                    }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun YogaProgramCard(
    program: YogaProgram,
    context: Context,
    onStart: () -> Unit,
    onReset: () -> Unit
) {
    val color    = Color(program.colorHex)
    var dayIndex by remember { mutableIntStateOf(getProgramDay(context, program.id)) }
    val pct      = if (program.durationDays > 0) dayIndex * 100 / program.durationDays else 0
    val isComplete = dayIndex >= program.durationDays

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(color.copy(0.07f), Color.Transparent)))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(color.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(program.emoji, fontSize = 26.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(program.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(getLevelColor(program.level).copy(0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(program.level, color = getLevelColor(program.level), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${program.durationDays} days", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
                if (dayIndex > 0) {
                    TextButton(onClick = {
                        onReset()
                        dayIndex = 0
                    }) {
                        Text("Reset", color = Color.Red.copy(0.7f), fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(program.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp)

            Spacer(Modifier.height(14.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Day $dayIndex / ${program.durationDays}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text("$pct%", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(pct / 100f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (isComplete) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF66BB6A).copy(0.15f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✅ Program Complete! Tap Reset to restart.", color = Color(0xFF66BB6A), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (dayIndex == 0) "Start Program" else "Continue — Day ${dayIndex + 1}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ── Step-by-Step Guidance Card ───────────────────────────────────────────────
@Composable
fun YogaStepByStepCharacterGuide(
    asana: YogaAsana,
    catColor: Color
) {
    val strings = LocalAppStrings.current
    val totalSteps = asana.steps.size.coerceAtLeast(1)
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val stepText = asana.steps.getOrElse(currentStepIndex) { "" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🧘 STEP-BY-STEP GUIDANCE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    "Step ${currentStepIndex + 1} of $totalSteps",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Step Selection Pills / Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                asana.steps.forEachIndexed { index, _ ->
                    val isSelected = currentStepIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { currentStepIndex = index }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Step ${index + 1}",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Real Human Studio Photo Display Area (Full Image Uncropped per step)
            val posePhoto = rememberAsanaImage(asana.id, currentStepIndex, stepText)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (posePhoto != null) {
                    Image(
                        bitmap = posePhoto,
                        contentDescription = "${asana.name} Step ${currentStepIndex + 1} Studio Photo Guide",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(asana.emoji, fontSize = 70.sp)
                    }
                }

                // Pose Step Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (currentStepIndex) {
                                0 -> "📍 STEP 1: SETUP POSITION"
                                1 -> "⚡ STEP 2: FORM & ALIGNMENT"
                                else -> "🧘 STEP ${currentStepIndex + 1}: HOLD & BREATHING"
                            },
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Dynamic Action/Body Part Alignment Focus Badge for exact step text visual guidance
                val textLower = stepText.lowercase()
                val (focusTag, focusAlign) = remember(stepText) {
                    when {
                        textLower.contains("feet") || textLower.contains("heel") || textLower.contains("ankle") || textLower.contains("toes") || textLower.contains("floor") || textLower.contains("mat") ->
                            "🦶 FOCUS: Feet Grounding & Balance" to Alignment.BottomStart
                        textLower.contains("knee") || textLower.contains("thigh") || textLower.contains("leg") || textLower.contains("hip") ->
                            "🦵 FOCUS: Leg & Hip Alignment" to Alignment.BottomEnd
                        textLower.contains("core") || textLower.contains("spine") || textLower.contains("back") || textLower.contains("chest") || textLower.contains("belly") || textLower.contains("torso") ->
                            "🔥 FOCUS: Core & Spine Engagement" to Alignment.CenterStart
                        textLower.contains("arm") || textLower.contains("shoulder") || textLower.contains("hand") || textLower.contains("head") || textLower.contains("crown") || textLower.contains("palms") ->
                            "🙆 FOCUS: Arms & Upper Body Extension" to Alignment.TopEnd
                        else ->
                            "✨ FOCUS: Body Alignment" to Alignment.BottomCenter
                    }
                }

                Box(
                    modifier = Modifier
                        .align(focusAlign)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            focusTag,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Current Step Instructions Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${currentStepIndex + 1}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stepText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Navigation buttons (Previous / Next Step)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { if (currentStepIndex > 0) currentStepIndex-- },
                    enabled = currentStepIndex > 0
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Step",
                        tint = if (currentStepIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Previous",
                        color = if (currentStepIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = { if (currentStepIndex < totalSteps - 1) currentStepIndex++ },
                    enabled = currentStepIndex < totalSteps - 1
                ) {
                    Text(
                        "Next Step",
                        color = if (currentStepIndex < totalSteps - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Step",
                        tint = if (currentStepIndex < totalSteps - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

