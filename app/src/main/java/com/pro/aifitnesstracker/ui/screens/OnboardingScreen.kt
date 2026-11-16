package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.LocalAppStrings

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val BG: Color
    @Composable get() = MaterialTheme.colorScheme.background
private val ACCENT: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val SURFACE: Color
    @Composable get() = MaterialTheme.colorScheme.surface
private val TEXT_SECONDARY: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val emoji: String,
    val accentColor: Color,
    val index: Int
)

val pages = listOf(
    OnboardingPage(
        title = "Activity & Steps",
        subtitle = "Track your movement",
        description = "Automatically monitor your daily step count, distance traveled, and active calories. Keep moving to complete your daily goals and build a healthy streak.",
        emoji = "🏃",
        accentColor = Color(0xFF9FA8DA),
        index = 0
    ),
    OnboardingPage(
        title = "Yoga & Posture Guide",
        subtitle = "Balance body & mind",
        description = "Access personalized yoga flows, step-by-step tutorials on yoga mudras, and breathing exercises designed to improve posture, flexibility, and mindfulness.",
        emoji = "🧘",
        accentColor = Color(0xFF81C784),
        index = 1
    ),
    OnboardingPage(
        title = "Smart Diet & Macros",
        subtitle = "Nutrition made simple",
        description = "Scan meals instantly using your camera to log calories and macronutrients. Receive smart meal plans and dietary tips tailored specifically to your body's needs.",
        emoji = "🍎",
        accentColor = Color(0xFFFFB74D),
        index = 2
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val strings = LocalAppStrings.current
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize().background(BG)) {

        // Animated Background Gradient Orbs
        val orbColor = primaryColor

        val infiniteTransition = rememberInfiniteTransition(label = "orb")
        val orbOffset by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 30f,
            animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "orbY"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(orbColor.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.2f - orbOffset),
                    radius = size.width * 0.5f
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.8f, size.height * 0.2f - orbOffset)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(orbColor.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.75f + orbOffset),
                    radius = size.width * 0.4f
                ),
                radius = size.width * 0.4f,
                center = Offset(size.width * 0.1f, size.height * 0.75f + orbOffset)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Skip button top right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = strings.skip,
                    color = TEXT_SECONDARY,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SURFACE.copy(alpha = 0.8f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onFinish() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { position ->
                PagerScreen(page = pages[position].copy(accentColor = primaryColor))
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { i ->
                        val isSelected = pagerState.currentPage == i
                        val color      = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                        val width      = if (isSelected) 28.dp else 8.dp
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                if (pagerState.currentPage == pages.size - 1) {
                    Button(
                        onClick = onFinish,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Text("${strings.start} 🚀", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.confirm, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PagerScreen(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Illustration
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            page.accentColor.copy(alpha = 0.2f),
                            SURFACE.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            when (page.index) {
                0 -> StepsIllustration(page.accentColor)
                1 -> YogaIllustration(page.accentColor)
                2 -> FoodScanIllustration(page.accentColor)
            }

            // Floating and swaying emoji on top
            val infiniteTransition = rememberInfiniteTransition(label = "emoji_float")
            val floatY by infiniteTransition.animateFloat(
                initialValue = -8f, targetValue = 8f,
                animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "fy"
            )
            val rotateAngle by infiniteTransition.animateFloat(
                initialValue = -10f, targetValue = 10f,
                animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "rot"
            )
            Text(
                text = page.emoji,
                fontSize = 72.sp,
                modifier = Modifier
                    .offset(y = floatY.dp)
                    .graphicsLayer(rotationZ = rotateAngle)
            )
        }

        Spacer(Modifier.height(40.dp))

        // Accent badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(page.accentColor.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = page.subtitle,
                color = page.accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = page.title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = page.description,
            color = TEXT_SECONDARY,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )
    }
}

// ─────────────────────────────────────────────
// Illustrations
// ─────────────────────────────────────────────

@Composable
fun StepsIllustration(accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "steps")
    
    // Step bounce
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -15f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce"
    )
    
    // Step angle rotation
    val angle by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "angle"
    )
    
    // Fading footprints
    val fadeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "fade"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        
        // 1. Draw glowing circular tracking ring
        drawCircle(
            color = accent.copy(alpha = 0.1f),
            radius = w * 0.42f,
            center = Offset(cx, cy)
        )
        
        drawArc(
            color = accent,
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.42f, cy - h * 0.42f),
            size = Size(w * 0.84f, h * 0.84f),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
        
        // 2. Draw footprint/footstep symbols
        // Left Footprint
        drawRoundRect(
            color = accent.copy(alpha = 0.2f * fadeAlpha),
            topLeft = Offset(cx - 30f, cy + 20f + bounceY),
            size = Size(18f, 32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
        )
        drawCircle(
            color = accent.copy(alpha = 0.3f * fadeAlpha),
            radius = 4f,
            center = Offset(cx - 21f, cy + 12f + bounceY)
        )
        
        // Right Footprint
        drawRoundRect(
            color = accent.copy(alpha = 0.4f * (1f - fadeAlpha)),
            topLeft = Offset(cx + 12f, cy - 25f - bounceY),
            size = Size(18f, 32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
        )
        drawCircle(
            color = accent.copy(alpha = 0.5f * (1f - fadeAlpha)),
            radius = 4f,
            center = Offset(cx + 21f, cy - 33f - bounceY)
        )
        
        // 3. Draw a bouncing neon shoe silhouette or dynamic energy lines
        translate(left = 0f, top = bounceY) {
            rotate(degrees = angle, pivot = Offset(cx, cy)) {
                // A sleek athletic shoe sole shape
                val shoePath = Path().apply {
                    moveTo(cx - 45f, cy - 5f)
                    quadraticTo(cx - 35f, cy - 25f, cx + 5f, cy - 25f)
                    quadraticTo(cx + 45f, cy - 10f, cx + 55f, cy + 10f)
                    lineTo(cx - 35f, cy + 10f)
                    quadraticTo(cx - 45f, cy + 8f, cx - 45f, cy - 5f)
                }
                drawPath(
                    path = shoePath,
                    color = accent.copy(alpha = 0.8f)
                )
                
                // Laces & details
                drawLine(
                    color = Color.White,
                    start = Offset(cx - 5f, cy - 15f),
                    end = Offset(cx + 15f, cy - 5f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cx - 10f, cy - 10f),
                    end = Offset(cx + 10f, cy),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun YogaIllustration(accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "yoga")
    
    // Breathing expansion (0.8f to 1.15f)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    
    // Halo rotate
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "halo"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        
        // 1. Draw expanding breathing waves (underlay)
        drawCircle(
            color = accent.copy(alpha = 0.08f),
            radius = w * 0.45f * scale,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = accent.copy(alpha = 0.12f),
            radius = w * 0.35f * scale,
            center = Offset(cx, cy)
        )
        
        // 2. Rotating lotus/halo rings
        rotate(degrees = rotation, pivot = Offset(cx, cy)) {
            drawCircle(
                color = accent.copy(alpha = 0.25f),
                radius = w * 0.28f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f))
            )
        }
        
        // 3. Meditating Yoga Silhouette
        val headRadius = w * 0.05f
        val headCenter = Offset(cx, cy - h * 0.18f)
        
        // Head
        drawCircle(
            color = Color.White,
            radius = headRadius,
            center = headCenter
        )
        
        // Spine / Torso / Legs
        val shoulderY = headCenter.y + headRadius + 5f
        val hipsY = cy + h * 0.08f
        
        // Draw torso
        drawLine(
            color = Color.White,
            start = Offset(cx, shoulderY),
            end = Offset(cx, hipsY),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        
        // Crossed legs path
        val legsPath = Path().apply {
            moveTo(cx - w * 0.25f, cy + h * 0.12f)
            quadraticTo(cx - w * 0.18f, cy + h * 0.02f, cx, hipsY)
            quadraticTo(cx + w * 0.18f, cy + h * 0.02f, cx + w * 0.25f, cy + h * 0.12f)
            lineTo(cx - w * 0.25f, cy + h * 0.12f)
        }
        drawPath(
            path = legsPath,
            color = accent.copy(alpha = 0.7f)
        )
        
        // Arms raised in mudra / prayer position
        val leftElbow = Offset(cx - w * 0.14f, shoulderY + h * 0.04f)
        val rightElbow = Offset(cx + w * 0.14f, shoulderY + h * 0.04f)
        val handsJoin = Offset(cx, shoulderY - 8f)
        
        drawLine(Color.White, Offset(cx, shoulderY), leftElbow, 5f, StrokeCap.Round)
        drawLine(Color.White, leftElbow, handsJoin, 5f, StrokeCap.Round)
        
        drawLine(Color.White, Offset(cx, shoulderY), rightElbow, 5f, StrokeCap.Round)
        drawLine(Color.White, rightElbow, handsJoin, 5f, StrokeCap.Round)
        
        // Small glowing energy center (Heart Chakra)
        drawCircle(
            color = accent,
            radius = 5f * scale,
            center = Offset(cx, (shoulderY + hipsY) / 2f)
        )
    }
}

@Composable
fun WelcomeIllustration(accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "ecg")
    
    // Heartbeat pulse animation (simulates double beat)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.95f at 0
                1.08f at 150
                0.97f at 300
                1.06f at 450
                0.95f at 600
                0.95f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    
    // Sweep phase for ECG line (from 0.0 to 1.0)
    val sweepPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "sweep"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        
        // 1. Draw glowing outer tech rings
        val outerRadius = w * 0.42f * pulseScale
        drawCircle(
            color = accent.copy(alpha = 0.15f),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
        )
        drawCircle(
            color = accent.copy(alpha = 0.05f),
            radius = w * 0.35f,
            center = center
        )

        // 2. Define ECG points
        val startX = w * 0.1f
        val endX = w * 0.9f
        val midY = h / 2f
        
        val points = listOf(
            Offset(startX, midY),
            Offset(w * 0.32f, midY),
            Offset(w * 0.36f, midY - 14f),
            Offset(w * 0.40f, midY + 14f),
            Offset(w * 0.44f, midY),
            Offset(w * 0.48f, midY),
            Offset(w * 0.52f, midY + 22f),   // Q
            Offset(w * 0.56f, midY - 65f),   // R (spike)
            Offset(w * 0.60f, midY + 45f),   // S (dip)
            Offset(w * 0.64f, midY),         // base
            Offset(w * 0.68f, midY - 10f),   // T (bump)
            Offset(w * 0.72f, midY),         // base
            Offset(endX, midY)
        )

        // Draw background static path (dim)
        val bgPath = Path()
        bgPath.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            bgPath.lineTo(points[i].x, points[i].y)
        }
        drawPath(
            path = bgPath,
            color = accent.copy(alpha = 0.15f),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // Interpolate Y for current X sweep position
        val currentX = startX + (endX - startX) * sweepPhase
        fun getYForX(x: Float): Float {
            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                if (x >= p1.x && x <= p2.x) {
                    val ratio = (x - p1.x) / (p2.x - p1.x)
                    return p1.y + (p2.y - p1.y) * ratio
                }
            }
            return midY
        }
        val currentY = getYForX(currentX)

        // Draw active sweeping path (colored and glowing)
        val activePath = Path()
        activePath.moveTo(startX, midY)
        for (p in points) {
            if (p.x <= currentX) {
                activePath.lineTo(p.x, p.y)
            } else {
                activePath.lineTo(currentX, currentY)
                break
            }
        }
        drawPath(
            path = activePath,
            color = accent,
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        // Draw sweep head dot (glowing lead)
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = Offset(currentX, currentY)
        )
        drawCircle(
            color = accent.copy(alpha = 0.4f),
            radius = 14f,
            center = Offset(currentX, currentY)
        )
    }
}

@Composable
fun FoodScanIllustration(accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "food")
    
    // Scan line moves up and down
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0.18f, targetValue = 0.82f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "scan"
    )

    // Floating particles (macros)
    val floatOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float1"
    )
    val floatOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float2"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val w = size.width
        val h = size.height
        
        val pad = w * 0.15f
        val cornerLen = 25f
        val stroke = 6f

        // 1. Draw Viewfinder Corners
        // Top Left
        drawLine(accent, Offset(pad, pad), Offset(pad + cornerLen, pad), stroke, StrokeCap.Round)
        drawLine(accent, Offset(pad, pad), Offset(pad, pad + cornerLen), stroke, StrokeCap.Round)
        // Top Right
        drawLine(accent, Offset(w - pad, pad), Offset(w - pad - cornerLen, pad), stroke, StrokeCap.Round)
        drawLine(accent, Offset(w - pad, pad), Offset(w - pad, pad + cornerLen), stroke, StrokeCap.Round)
        // Bottom Left
        drawLine(accent, Offset(pad, h - pad), Offset(pad + cornerLen, h - pad), stroke, StrokeCap.Round)
        drawLine(accent, Offset(pad, h - pad), Offset(pad, h - pad - cornerLen), stroke, StrokeCap.Round)
        // Bottom Right
        drawLine(accent, Offset(w - pad, h - pad), Offset(w - pad - cornerLen, h - pad), stroke, StrokeCap.Round)
        drawLine(accent, Offset(w - pad, h - pad), Offset(w - pad, h - pad - cornerLen), stroke, StrokeCap.Round)

        // 2. Draw Apple / Food Silhouette in the Center
        val appleCenter = Offset(w / 2f, h / 2f + 10f)
        val appleRadius = w * 0.20f
        
        // Left lobe of apple
        drawCircle(
            color = Color(0xFFFF5252).copy(alpha = 0.45f),
            radius = appleRadius,
            center = Offset(appleCenter.x - appleRadius * 0.22f, appleCenter.y)
        )
        // Right lobe of apple
        drawCircle(
            color = Color(0xFFFF5252).copy(alpha = 0.45f),
            radius = appleRadius,
            center = Offset(appleCenter.x + appleRadius * 0.22f, appleCenter.y)
        )
        
        // Stem
        drawLine(
            color = Color(0xFF8D6E63),
            start = Offset(appleCenter.x, appleCenter.y - appleRadius * 0.85f),
            end = Offset(appleCenter.x + 8f, appleCenter.y - appleRadius * 1.35f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )

        // Leaf
        val leafPath = Path().apply {
            moveTo(appleCenter.x + 4f, appleCenter.y - appleRadius * 1.25f)
            quadraticTo(
                appleCenter.x + 22f, appleCenter.y - appleRadius * 1.55f,
                appleCenter.x + 36f, appleCenter.y - appleRadius * 1.6f
            )
            quadraticTo(
                appleCenter.x + 18f, appleCenter.y - appleRadius * 1.3f,
                appleCenter.x + 4f, appleCenter.y - appleRadius * 1.25f
            )
        }
        drawPath(leafPath, Color(0xFF81C784))

        // 3. Draw Scan laser line
        val lineY = pad + (h - 2 * pad) * scanY
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, accent, Color.Transparent)),
            start = Offset(pad - 10f, lineY),
            end = Offset(w - pad + 10f, lineY),
            strokeWidth = 5f
        )
        // Laser glow projection
        drawRect(
            brush = Brush.verticalGradient(
                listOf(accent.copy(alpha = 0.12f), Color.Transparent)
            ),
            topLeft = Offset(pad, lineY - 20f * (1f - scanY)),
            size = Size(w - 2 * pad, 20f)
        )

        // 4. Draw nutrient macro labels floating around
        // Carb dot (orange)
        drawCircle(Color(0xFFFFB74D), radius = 6f, center = Offset(w * 0.22f, h * 0.32f + floatOffset1))
        // Protein dot (blue)
        drawCircle(Color(0xFF64B5F6), radius = 8f, center = Offset(w * 0.76f, h * 0.38f + floatOffset2))
        // Fat dot (red)
        drawCircle(Color(0xFFE57373), radius = 5f, center = Offset(w * 0.74f, h * 0.62f + floatOffset1))
    }
}

@Composable
fun WorkoutIllustration(accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "workout")
    
    // Squat progress (0 to 1)
    val squatProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "squat"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val w = size.width
        val h = size.height
        
        val cx = w / 2f
        val cy = h / 2f
        
        val groundY = h * 0.85f
        val squatDepth = h * 0.16f * squatProgress
        
        // Feet (fixed)
        val leftFoot = Offset(cx - w * 0.14f, groundY)
        val rightFoot = Offset(cx + w * 0.12f, groundY)
        
        // Hips (moves down)
        val hips = Offset(cx - w * 0.04f * squatProgress, groundY - h * 0.36f + squatDepth)
        
        // Knees (bends outward/down)
        val leftKnee = Offset(cx - w * 0.22f - w * 0.04f * squatProgress, groundY - h * 0.18f + squatDepth * 0.5f)
        val rightKnee = Offset(cx + w * 0.18f + w * 0.04f * squatProgress, groundY - h * 0.18f + squatDepth * 0.5f)
        
        // Shoulders (moves slightly forward/down)
        val shoulder = Offset(cx - w * 0.02f * squatProgress, hips.y - h * 0.20f)
        
        // Head (above shoulder)
        val head = Offset(shoulder.x - w * 0.02f * squatProgress, shoulder.y - h * 0.08f)
        
        // Hands (extended balance forward)
        val leftHand = Offset(cx - w * 0.24f - w * 0.04f * squatProgress, shoulder.y + h * 0.04f)
        val rightHand = Offset(cx - w * 0.21f - w * 0.04f * squatProgress, shoulder.y + h * 0.06f)

        // 1. Tech grid guide lines
        drawLine(
            color = accent.copy(alpha = 0.05f),
            start = Offset(cx, 0f), end = Offset(cx, h),
            strokeWidth = 2f
        )
        drawLine(
            color = accent.copy(alpha = 0.05f),
            start = Offset(0f, cy), end = Offset(w, cy),
            strokeWidth = 2f
        )
        
        // 2. Ground line
        drawLine(
            color = Color.White.copy(alpha = 0.1f),
            start = Offset(w * 0.1f, groundY),
            end = Offset(w * 0.9f, groundY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // 3. Draw bones (skeleton connections)
        val boneWidth = 6f
        
        // Spine
        drawLine(accent, shoulder, hips, boneWidth, StrokeCap.Round)
        // Left Leg: Hip -> Knee -> Foot
        drawLine(accent, hips, leftKnee, boneWidth, StrokeCap.Round)
        drawLine(accent, leftKnee, leftFoot, boneWidth, StrokeCap.Round)
        // Right Leg: Hip -> Knee -> Foot
        drawLine(accent, hips, rightKnee, boneWidth, StrokeCap.Round)
        drawLine(accent, rightKnee, rightFoot, boneWidth, StrokeCap.Round)
        // Arms: Shoulder -> Hands
        drawLine(accent, shoulder, leftHand, boneWidth, StrokeCap.Round)
        drawLine(accent, shoulder, rightHand, boneWidth, StrokeCap.Round)

        // 4. Draw Joint Nodes (Glowing Keypoints)
        val nodeColor = Color(0xFF64B5F6) // neon cyan
        val nodeRadius = 6f
        
        drawCircle(nodeColor, radius = nodeRadius, center = hips)
        drawCircle(nodeColor, radius = nodeRadius, center = leftKnee)
        drawCircle(nodeColor, radius = nodeRadius, center = rightKnee)
        drawCircle(nodeColor, radius = nodeRadius, center = leftFoot)
        drawCircle(nodeColor, radius = nodeRadius, center = rightFoot)
        drawCircle(nodeColor, radius = nodeRadius, center = shoulder)
        drawCircle(nodeColor, radius = nodeRadius, center = leftHand)
        drawCircle(nodeColor, radius = nodeRadius, center = rightHand)
        
        // Head
        drawCircle(
            color = accent,
            radius = h * 0.05f,
            center = head
        )

        // 5. Draw target confirmation circular guide ring (changes color on depth completion)
        val feedbackColor = if (squatProgress > 0.8f) Color(0xFF81C784) else accent
        drawArc(
            color = feedbackColor.copy(alpha = 0.2f),
            startAngle = -90f,
            sweepAngle = 360f * squatProgress,
            useCenter = false,
            topLeft = Offset(cx - w * 0.38f, cy - h * 0.38f),
            size = Size(w * 0.76f, h * 0.76f),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}

// Keep CameraIllustration, ShoeIllustration, ChartIllustration for backward compat
@Composable
fun CameraIllustration() {
    val accentColor = ACCENT
    val surfaceColor = SURFACE
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = ""
    )
    Canvas(modifier = Modifier.size(150.dp)) {
        val cornerLength = 30f; val stroke = 8f
        drawLine(accentColor, Offset(0f, 0f), Offset(cornerLength, 0f), stroke)
        drawLine(accentColor, Offset(0f, 0f), Offset(0f, cornerLength), stroke)
        drawLine(accentColor, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), stroke)
        drawLine(accentColor, Offset(size.width, 0f), Offset(size.width, cornerLength), stroke)
        drawLine(accentColor, Offset(0f, size.height), Offset(cornerLength, size.height), stroke)
        drawLine(accentColor, Offset(0f, size.height), Offset(0f, size.height - cornerLength), stroke)
        drawLine(accentColor, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), stroke)
        drawLine(accentColor, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), stroke)
        drawCircle(color = surfaceColor, radius = size.width / 3f, center = center)
        drawCircle(color = accentColor, radius = (size.width / 4f) * scale, center = center, style = Stroke(width = 6f))
        drawCircle(color = Color(0xFFFF5252), radius = 8f * scale, center = center)
    }
}

@Composable
fun ShoeIllustration() {
    val accentColor = ACCENT
    val surfaceColor = SURFACE
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = ""
    )
    Canvas(modifier = Modifier.size(150.dp)) {
        translate(left = 0f, top = rotation) {
            drawRoundRect(
                color = surfaceColor,
                topLeft = Offset(size.width * 0.2f, size.height * 0.2f),
                size = Size(size.width * 0.4f, size.height * 0.6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f, 40f)
            )
            drawCircle(color = accentColor, radius = 15f, center = Offset(size.width * 0.3f, size.height * 0.15f))
            drawCircle(color = accentColor, radius = 10f, center = Offset(size.width * 0.45f, size.height * 0.12f))
            drawCircle(color = accentColor, radius = 8f, center = Offset(size.width * 0.55f, size.height * 0.18f))
        }
    }
}

@Composable
fun ChartIllustration() {
    val accentColor = ACCENT
    val surfaceColor = SURFACE
    val textSecondaryColor = TEXT_SECONDARY
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = ""
    )
    Canvas(modifier = Modifier.size(150.dp)) {
        val barWidth = 24f; val spacing = 20f; val startX = 10f
        val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f)
        drawLine(textSecondaryColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 4f)
        drawLine(textSecondaryColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 4f)
        heights.forEachIndexed { index, ht ->
            val actualHeight = size.height * ht * (if (index == 3) heightScale else 1f)
            val left = startX + (index * (barWidth + spacing))
            val top = size.height - actualHeight
            drawRoundRect(
                color = if (index == 3) accentColor else surfaceColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, actualHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
        }
    }
}
