package com.pro.aifitnesstracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.aifitnesstracker.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ProThemeChip(
    val code: String,
    val label: String,
    val color: Color
)

private data class ProComparisonRow(
    val feature: String,
    val freeText: String,
    val proText: String,
    val proColor: Color
)

private data class ProPlanOption(
    val planId: String,
    val title: String,
    val priceMain: String,
    val unitLabel: String,
    val subtitle: String,
    val topBadge: String,
    val accent: Color,
    val isPopular: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    prefsManager: SharedPrefsManager,
    onBack: () -> Unit,
    onPurchase: (plan: String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // 100% Unified Design System: Exactly matches Dashboard, WorkoutPlanner, and HealthVitals
    val screenBg = if (isDark) Color(0xFF080B14) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF101726) else Color.White
    val cardBgAlt = if (isDark) Color(0xFF161E2E) else Color(0xFFF8FAFC)
    val subBoxBg = if (isDark) Color(0xFF1B2438) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF26334D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val textMuted = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val sectionAccent = Color(0xFF3B82F6)

    val gold = Color(0xFFF59E0B)
    val goldDeep = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    val cyan = Color(0xFF06B6D4)
    val cyanLight = if (isDark) Color(0xFF22D3EE) else Color(0xFF0284C7)
    val emerald = Color(0xFF10B981)
    val emeraldLight = if (isDark) Color(0xFF34D399) else Color(0xFF15803D)
    val purple = Color(0xFF8B5CF6)
    val purpleLight = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
    val crimson = Color(0xFFF43F5E)

    var isPremium by remember { mutableStateOf(PremiumManager.isPremium(prefsManager)) }
    var selectedPlan by remember {
        mutableStateOf(
            prefsManager.getPremiumPlan().ifBlank { PremiumManager.PLAN_YEARLY }
        )
    }
    var selectedThemeCode by remember {
        mutableStateOf(prefsManager.getAppTheme().ifBlank { "GoldElite" })
    }

    val themeChips = remember {
        listOf(
            ProThemeChip("GoldElite", "Gold Elite", Color(0xFFF59E0B)),
            ProThemeChip("CyberpunkNeon", "Cyberpunk Neon", Color(0xFFEC4899)),
            ProThemeChip("SleepOptimizer", "Midnight Neon", Color(0xFF06B6D4)),
            ProThemeChip("MidnightDark", "Obsidian Emerald", Color(0xFF10B981)),
            ProThemeChip("LightClean", "Clean Daylight", Color(0xFF3B82F6))
        )
    }

    val comparisonRows = remember(isDark) {
        listOf(
            ProComparisonRow("AI Coach & Live Chat", "10 msgs/day", "UNLIMITED 24/7", goldDeep),
            ProComparisonRow("Food Scanner & Meal Plans", "Basic Macros", "FULL CHEF RECIPES", emeraldLight),
            ProComparisonRow("3D Muscle Recovery Lab", "Preview Only", "FULL 360° ECORCHE", cyanLight),
            ProComparisonRow("Biometrics (BP, Heart, Sleep)", "7-Day History", "90-DAY + PDF EXPORT", purpleLight),
            ProComparisonRow("Cloud Auto-Backup & Sync", "Manual Only", "AUTO-SYNC + WIDGETS", goldDeep),
            ProComparisonRow("Ad-Free Experience & Themes", "Contains Ads", "100% AD-FREE + 5 THEMES", emeraldLight)
        )
    }

    val planOptions = remember {
        listOf(
            ProPlanOption(
                planId = PremiumManager.PLAN_YEARLY,
                title = "YEARLY ELITE",
                priceMain = "₹999",
                unitLabel = "/ year",
                subtitle = "Billed annually • Just ₹83/month",
                topBadge = "MOST POPULAR • SAVE 58%",
                accent = gold,
                isPopular = true
            ),
            ProPlanOption(
                planId = PremiumManager.PLAN_MONTHLY,
                title = "MONTHLY PRO",
                priceMain = "₹199",
                unitLabel = "/ month",
                subtitle = "Flexible monthly billing, cancel anytime",
                topBadge = "FLEXIBLE",
                accent = cyan,
                isPopular = false
            ),
            ProPlanOption(
                planId = PremiumManager.PLAN_LIFETIME,
                title = "LIFETIME VIP",
                priceMain = "₹1,999",
                unitLabel = "one-time",
                subtitle = "Pay once, own AuraHealth Elite Pro forever",
                topBadge = "BEST VALUE • FOREVER PASS",
                accent = purple,
                isPopular = false
            )
        )
    }

    val selectedPlanPriceBanner = when (selectedPlan) {
        PremiumManager.PLAN_MONTHLY -> "₹199 / MONTH"
        PremiumManager.PLAN_LIFETIME -> "₹1,999 LIFETIME"
        else -> "₹999 / YEAR"
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            Surface(
                color = screenBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Circular Back Button matching WorkoutPlanner & Profile
                    Surface(
                        shape = CircleShape,
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        shadowElevation = if (isDark) 2.dp else 1.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Center: Authoritative Title & Clean Subtitle (No squeezed oval pill!)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FITSPHERE ELITE PRO",
                            color = textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPremium) "VIP Active • All Pro Features Unlocked" else "Unlock All Pro Features • Ad-Free",
                            color = if (isPremium) emeraldLight else textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    // Right Restore Pill Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        shadowElevation = if (isDark) 2.dp else 1.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val restored = PremiumManager.isPremium(prefsManager)
                                isPremium = restored
                                Toast.makeText(
                                    context,
                                    if (restored) "Elite Pro Membership Restored!" else "No previous purchase found on this account.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    ) {
                        Text(
                            text = "RESTORE",
                            color = goldDeep,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                .background(screenBg)
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==========================================
            // 1. HERO SHOWCASE BANNER (EXECUTIVE GRADIENT)
            // ==========================================
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = if (isDark) 8.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E1B4B),
                                    Color(0xFF311042)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = gold.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "👑", fontSize = 12.sp)
                                    Text(
                                        text = if (isPremium) "VIP PASS ACTIVE" else "ALL-ACCESS PRO TIER",
                                        color = Color(0xFFFDE68A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = emerald.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, emerald.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "SAVE 58% TODAY",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Unlock Peak Performance",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        )

                        Text(
                            text = "Unlimited Personal AI Coach, 3D Muscle Ecorche, Food Scanner, 90-Day Advanced Biometrics & 100% Ad-Free Experience.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        // 3 Key Pill Highlights
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("⚡ 24/7 AI Coach", "🧬 3D Body Lab", "🚫 0 Ads").forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = tag,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. EXCLUSIVE PRO CAPABILITIES (2x2 STANDALONE GRID)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = strings.exclusiveCapabilities.uppercase(), accentColor = sectionAccent, textPrimary = textPrimary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StandaloneFeatureCard(
                        icon = "🤖",
                        title = "Personal AI Coach",
                        subtitle = "24/7 Workout & Diet Plans",
                        badge = "UNLIMITED",
                        accentColor = gold,
                        isDark = isDark,
                        cardBg = cardBg,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    StandaloneFeatureCard(
                        icon = "🧬",
                        title = "3D Muscle Lab",
                        subtitle = "Full 360° Anatomical Ecorche",
                        badge = "PRO ONLY",
                        accentColor = cyan,
                        isDark = isDark,
                        cardBg = cardBg,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StandaloneFeatureCard(
                        icon = "🎨",
                        title = "Pro Theme Studio",
                        subtitle = "5 Exclusive Neon Palettes",
                        badge = "UNLOCKED",
                        accentColor = purple,
                        isDark = isDark,
                        cardBg = cardBg,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    StandaloneFeatureCard(
                        icon = "⚡",
                        title = "Zero Ads",
                        subtitle = "100% Pure Focused UI",
                        badge = "AD-FREE",
                        accentColor = emerald,
                        isDark = isDark,
                        cardBg = cardBg,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ==========================================
            // 3. PRO THEME STUDIO (STANDALONE CARD)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "PRO THEME STUDIO (INSTANT PREVIEW)", accentColor = purple, textPrimary = textPrimary)

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    shadowElevation = if (isDark) 4.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        themeChips.forEach { chip ->
                            val isSelected = selectedThemeCode == chip.code
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    if (isDark) chip.color.copy(alpha = 0.22f) else chip.color.copy(alpha = 0.12f)
                                } else subBoxBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) chip.color else borderColor
                                ),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.clickable {
                                    selectedThemeCode = chip.code
                                    prefsManager.saveAppTheme(chip.code)
                                    Toast.makeText(context, "${chip.label} Theme Activated!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(chip.color)
                                    )
                                    Text(
                                        text = chip.label,
                                        color = if (isSelected) textPrimary else textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. FREE VS ELITE PRO COMPARISON TABLE
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "FEATURE COMPARISON", accentColor = gold, textPrimary = textPrimary)

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    shadowElevation = if (isDark) 4.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header aligned with columns
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BENEFIT",
                                color = textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "FREE",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(76.dp)
                            )
                            Text(
                                text = "ELITE PRO",
                                color = goldDeep,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(108.dp)
                            )
                        }

                        comparisonRows.forEach { row ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) cardBgAlt else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                shadowElevation = 0.5.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = row.feature,
                                        color = textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = row.freeText,
                                        color = textMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(76.dp)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) gold.copy(alpha = 0.16f) else Color(0xFFFEF3C7),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isDark) gold.copy(alpha = 0.5f) else Color(0xFFFDE68A)
                                        ),
                                        modifier = Modifier.width(108.dp)
                                    ) {
                                        Text(
                                            text = row.proText,
                                            color = if (isDark) row.proColor else Color(0xFFB45309),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 5. SELECT MEMBERSHIP PLAN (VERTICAL LUXURY STACK)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = strings.chooseMembershipPlan.uppercase(), accentColor = emerald, textPrimary = textPrimary)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    planOptions.forEach { option ->
                        val isSelected = selectedPlan == option.planId

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) {
                                if (isDark) option.accent.copy(alpha = 0.14f) else Color(0xFFFFFBEB)
                            } else cardBg,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) option.accent else borderColor
                            ),
                            shadowElevation = if (isSelected) {
                                if (isDark) 6.dp else 3.5.dp
                            } else 1.5.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlan = option.planId }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top Row: Radio + Title + Price
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Radio Check Circle
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) option.accent else Color.Transparent,
                                        border = androidx.compose.foundation.BorderStroke(
                                            2.dp,
                                            if (isSelected) option.accent else (if (isDark) borderColor else Color(0xFFCBD5E1))
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        if (isSelected) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = if (isDark) Color(0xFF0A0E17) else Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Title & Subtitle
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.title,
                                            color = textPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = option.subtitle,
                                            color = textSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }

                                    // Price Column
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = option.priceMain,
                                            color = if (isSelected) {
                                                if (isDark) option.accent else goldDeep
                                            } else textPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = option.unitLabel,
                                            color = textSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Bottom Badge Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) {
                                            if (isDark) option.accent.copy(alpha = 0.20f) else Color(0xFFFEF3C7)
                                        } else {
                                            option.accent.copy(alpha = if (isDark) 0.16f else 0.10f)
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isDark) option.accent.copy(alpha = 0.6f)
                                            else if (isSelected) Color(0xFFFDE68A) else borderColor
                                        )
                                    ) {
                                        Text(
                                            text = option.topBadge,
                                            color = if (isDark) option.accent else (if (isSelected) Color(0xFFB45309) else option.accent),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Text(
                                        text = if (isSelected) "✓ PLAN SELECTED" else "TAP TO SELECT",
                                        color = if (isSelected) {
                                            if (isDark) option.accent else goldDeep
                                        } else textMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Active VIP Banner if already Premium
            if (isPremium) {
                val expiry = prefsManager.getPremiumExpiryDate()
                val expiryLabel = when {
                    expiry == -1L -> "Lifetime VIP Access Active"
                    expiry > 0L -> "Valid until ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expiry))}"
                    else -> "Active VIP Membership"
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) emerald.copy(alpha = 0.16f) else Color(0xFFDCFCE7),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) emerald.copy(alpha = 0.6f) else Color(0xFFBBF7D0)
                    ),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ELITE PRO MEMBERSHIP ACTIVE",
                                color = if (isDark) emeraldLight else Color(0xFF15803D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = expiryLabel,
                                color = textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = crimson.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, crimson.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                PremiumManager.deactivate(prefsManager)
                                isPremium = false
                                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                if (uid.isNotEmpty()) {
                                    FirebaseSyncManager.syncPremiumStatusToCloud(uid, false, 0L)
                                }
                                Toast.makeText(context, "Premium Deactivated (Test Mode)", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "DEACTIVATE",
                                color = crimson,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 6. PRIMARY ACTION CTA BUTTON
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) gold else Color.Transparent,
                shadowElevation = if (isDark) 6.dp else 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDark) Brush.linearGradient(listOf(gold, gold))
                        else Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                    )
                    .clickable {
                        onPurchase(selectedPlan)
                        isPremium = true
                        Toast.makeText(context, "AuraHealth Elite Pro Unlocked!", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isPremium) "SWITCH / EXTEND PLAN  —  $selectedPlanPriceBanner"
                        else "UNLOCK ELITE PRO NOW  —  $selectedPlanPriceBanner",
                        color = if (isDark) Color(0xFF0A0E17) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Footer Guarantee & Restore Row
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🔒 100% Secure Google Play Billing • Instant Activation • Cancel Anytime",
                    color = textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Already upgraded? Tap Restore Purchases • Terms & Privacy",
                    color = cyanLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        val restored = PremiumManager.isPremium(prefsManager)
                        isPremium = restored
                        Toast.makeText(
                            context,
                            if (restored) "Elite Pro Membership Restored!" else "Tap Unlock Elite Pro to activate instantly.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    accentColor: Color,
    textPrimary: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Text(
            text = title,
            color = textPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun StandaloneFeatureCard(
    icon: String,
    title: String,
    subtitle: String,
    badge: String,
    accentColor: Color,
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDark) 3.dp else 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Soft pastel squircle badge matching Health Vitals
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = icon, fontSize = 16.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = if (isDark) 0.18f else 0.10f)
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = textSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 14.sp,
                    maxLines = 2
                )
            }
        }
    }
}
