package com.pro.aifitnesstracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val BG @Composable get() = MaterialTheme.colorScheme.background
private val ACCENT @Composable get() = MaterialTheme.colorScheme.primary

private data class SplashFeature(
    val icon: ImageVector,
    val tint: Color,
    val name: String
)

@Composable
fun SplashScreen(onNavigate: () -> Unit) {
    val primaryColor = ACCENT
    val transition = rememberInfiniteTransition(label = "splash_transition")

    val pulseScale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_rotation"
    )

    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_glow"
    )

    // Accurate, official fitness & wellness icons matching each content feature
    val features = remember {
        listOf(
            SplashFeature(Icons.Filled.DirectionsRun, Color(0xFF06B6D4), "Running"),
            SplashFeature(Icons.Filled.SelfImprovement, Color(0xFF8B5CF6), "Yoga"),
            SplashFeature(Icons.Filled.FitnessCenter, Color(0xFFF97316), "Workout"),
            SplashFeature(Icons.Filled.Favorite, Color(0xFFEF4444), "Heart Rate"),
            SplashFeature(Icons.Filled.WaterDrop, Color(0xFF3B82F6), "Hydration"),
            SplashFeature(Icons.Filled.Bedtime, Color(0xFF6366F1), "Sleep"),
            SplashFeature(Icons.Filled.Restaurant, Color(0xFF10B981), "Diet"),
            SplashFeature(Icons.Filled.LocalFireDepartment, Color(0xFFF59E0B), "Calories")
        )
    }

    LaunchedEffect(Unit) {
        delay(3500)
        onNavigate()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(BG),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val orbitRadius = 100.dp

            Box(
                modifier = Modifier.size(270.dp),
                contentAlignment = Alignment.Center
            ) {
                // Central App Logo with ambient glow
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(125.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(ACCENT.copy(alpha = alpha * 0.45f), Color.Transparent)
                                )
                            )
                    )

                    Image(
                        painter = painterResource(id = com.pro.aifitnesstracker.R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(96.dp)
                            .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // 8 Orbiting Feature Badges revolving around main logo (Clean UI, no tracks)
                features.forEachIndexed { index, feat ->
                    val angleDegrees = rotationAngle + (index * 360f / features.size)
                    val angleRadians = Math.toRadians(angleDegrees.toDouble())
                    val offsetX = (orbitRadius.value * kotlin.math.cos(angleRadians)).dp
                    val offsetY = (orbitRadius.value * kotlin.math.sin(angleRadians)).dp

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .size(38.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, feat.tint.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = feat.icon,
                            contentDescription = feat.name,
                            tint = feat.tint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Title
            Text("AURAHEALTH", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("FITNESS COACH & TRACKER", color = ACCENT, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

            Spacer(Modifier.height(60.dp))

            // Loading Line
            Box(modifier = Modifier.width(150.dp).height(2.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(alpha).background(ACCENT))
            }
        }

        // Bottom Info
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Version 1.0.0",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Developed by Rohit",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}





