package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.LocalAppStrings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val strings = LocalAppStrings.current
    val uriHandler = LocalUriHandler.current

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val topBarBg = if (isDark) Color(0xFF080B14) else Color(0xFFEEF2F6)
    val canvasBgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF080B14), Color(0xFF0D121F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEEF2F6), Color(0xFFE2E8F0)))
    }
    val cardColor   = if (isDark) Color(0xFF131A2A) else Color(0xFFFFFFFF)
    val cardBorder  = if (isDark) Color(0xFF1F293D) else Color(0xFFE2E8F0)
    val accentColor = MaterialTheme.colorScheme.primary
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecond  = if (isDark) Color(0xFF94A3B8) else Color.Black

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
                    title = { Text(strings.privacyPolicy, color = textPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = accentColor)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(canvasBgGradient)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("AuraHealth: AI Coach & Tracker", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                        Text(strings.privacyPolicy, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text("Effective Date: June 20, 2026  |  Last Updated: June 20, 2026", fontSize = 12.sp, color = textSecond)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Your privacy is important to us. This policy explains how we collect, use, store, and protect your data when you use AuraHealth: AI Coach & Tracker.",
                            fontSize = 14.sp, color = textSecond, lineHeight = 22.sp
                        )
                    }
                }
            }

            item {
                PolicySection(icon = Icons.Rounded.Dataset, title = "1. Information We Collect", accentColor = accentColor, cardColor = cardColor, cardBorder = cardBorder, textPrimary = textPrimary) {
                    PolicyBullet("Account data (email) via Firebase Authentication", textSecond)
                    PolicyBullet("Profile info: name, age, weight, height, fitness goals", textSecond)
                    PolicyBullet("Health data: steps, heart rate, sleep, calories (via Android Health Connect)", textSecond)
                    PolicyBullet("Meal logs, water intake records, workout sessions", textSecond)
                    PolicyBullet("AI Coach chat messages (processed by Gemini AI)", textSecond)
                    PolicyBullet("Anonymised usage analytics via Firebase Analytics", textSecond)
                    PolicyBullet("FCM device token for push notifications", textSecond)
                }
            }

            item {
                PolicySection(icon = Icons.Rounded.Security, title = "2. App Permissions", accentColor = accentColor, cardColor = cardColor, cardBorder = cardBorder, textPrimary = textPrimary) {
                    PermissionRow("Camera", "AI-powered food scanning / meal recognition", textSecond, textPrimary)
                    PermissionRow("Microphone", "Voice input for AI Coach", textSecond, textPrimary)
                    PermissionRow("Activity Recognition", "Step counting & movement detection", textSecond, textPrimary)
                    PermissionRow("Notifications", "Daily reminders, water alerts, smart alarm", textSecond, textPrimary)
                    PermissionRow("Health Connect", "Steps, heart rate, sleep, calories", textSecond, textPrimary)
                    PermissionRow("Alarms & Scheduling", "Precise smart alarm timing", textSecond, textPrimary)
                    PermissionRow("Boot Completed", "Restart background services after reboot", textSecond, textPrimary)
                    PermissionRow("Internet", "Sync data to Firebase cloud", textSecond, textPrimary)
                }
            }

            item {
                PolicySection(icon = Icons.Rounded.Lock, title = "3. Data Storage & Security", accentColor = accentColor, cardColor = cardColor, cardBorder = cardBorder, textPrimary = textPrimary) {
                    PolicyBullet("Settings & preferences stored locally via Android SharedPreferences", textSecond)
                    PolicyBullet("Health records stored locally in a SQLite database (Room)", textSecond)
                    PolicyBullet("Cloud sync to Firebase Realtime Database & Firestore under your user ID", textSecond)
                    PolicyBullet("All network traffic uses HTTPS/TLS encryption", textSecond)
                    PolicyBullet("Firebase Security Rules ensure only you can access your own data", textSecond)
                    PolicyBullet("Camera images for food scanning are NOT stored on our servers", textSecond)
                }
            }

            item {
                PolicySection(icon = Icons.Rounded.SmartToy, title = "4. AI Features & Third-Party AI", accentColor = accentColor, cardColor = cardColor, cardBorder = cardBorder, textPrimary = textPrimary) {
                    PolicyBullet("AI Coach uses Google Gemini AI — your messages are processed by Google", textSecond)
                    PolicyBullet("Food scanner uses Google ML Kit for on-device image analysis", textSecond)
                    PolicyBullet("Do not share sensitive medical or financial info in AI chat", textSecond)
                    PolicyBullet("Gemini AI terms apply: ai.google.dev/gemini-api/terms", textSecond)
                }
            }

            item {
                PolicySection(icon = Icons.Rounded.Extension, title = "5. Third-Party Services", accentColor = accentColor, cardColor = cardColor, cardBorder = cardBorder, textPrimary = textPrimary) {
                    ServiceRow("Firebase Auth", "Account login", textSecond, textPrimary)
                    ServiceRow("Firebase Realtime DB / Firestore", "Cloud data storage", textSecond, textPrimary)
                    ServiceRow("Firebase Analytics", "Usage analytics (anonymised)", textSecond, textPrimary)
                    ServiceRow("Firebase Messaging (FCM)", "Push notifications", textSecond, textPrimary)
                    ServiceRow("Firebase Remote Config", "App configuration", textSecond, textPrimary)
                    ServiceRow("Google ML Kit", "Pose detection, barcode scan, image labeling", textSecond, textPrimary)
                    ServiceRow("Gemini AI API", "AI fitness coach & food analysis", textSecond, textPrimary)
                    ServiceRow("Android Health Connect", "Health data integration", textSecond, textPrimary)
                }
            }

            item {
                PolicySection(icon = Icons.Rounded.Share, title = "6. Data Sharing", accentColor = accentColor, cardColor = cardColor, cardBorder = cardBorder, textPrimary = textPrimary) {
                    PolicyBullet("We do NOT sell your personal data", textSecond)
                    PolicyBullet("Data shared only with Firebase / Google for app operation", textSecond)
                    PolicyBullet("Data may be disclosed if required by law", textSecond)
                    PolicyBullet("Your health data is NEVER shared with insurers or advertisers", textSecond)
                }
            }

            item {
                PolicySection(icon = Icons.Rounded.VerifiedUser, title = "7. Your Rights & Controls", accentColor = accentColor, cardColor = cardColor, cardBorder = cardBorder, textPrimary = textPrimary) {
                    PolicyBullet("Access: Request a copy of your personal data", textSecond)
                    PolicyBullet("Correction: Update profile data directly in the app", textSecond)
                    PolicyBullet("Deletion: Delete account in Settings -> Delete Account", textSecond)
                    PolicyBullet("Revoke any device permission in Android Settings at any time", textSecond)
                    PolicyBullet("Manage Health Connect data in the Health Connect app", textSecond)
                    PolicyBullet("Disable notifications in Settings -> Notifications", textSecond)
                }
            }

            item {
                PolicySection(icon = Icons.Rounded.ChildCare, title = "8. Children's Privacy", accentColor = accentColor, cardColor = cardColor, cardBorder = cardBorder, textPrimary = textPrimary) {
                    PolicyBullet("This app is NOT intended for children under 13 years of age", textSecond)
                    PolicyBullet("We do not knowingly collect data from children", textSecond)
                    PolicyBullet("Contact us if you believe a child's data was collected", textSecond)
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.Mail, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Contact Us", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "For questions or data requests:\nrohitkamble.developer@gmail.com",
                            fontSize = 13.sp, color = textSecond, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { uriHandler.openUri("https://aurahealth-policy.web.app/privacy.html") },
                            border = BorderStroke(1.dp, accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("View Online Policy", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PolicySection(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    cardColor: Color,
    cardBorder: Color,
    textPrimary: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = accentColor.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PolicyBullet(text: String, textColor: Color) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("• ", color = textColor, fontSize = 13.sp)
        Text(text, color = textColor, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun PermissionRow(permission: String, reason: String, textColor: Color, textPrimary: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(permission, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.38f))
        Text(reason, color = textColor, fontSize = 12.sp, modifier = Modifier.weight(0.62f), lineHeight = 18.sp)
    }
}

@Composable
private fun ServiceRow(service: String, purpose: String, textColor: Color, textPrimary: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(service, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.45f))
        Text(purpose, color = textColor, fontSize = 12.sp, modifier = Modifier.weight(0.55f), lineHeight = 18.sp)
    }
}
