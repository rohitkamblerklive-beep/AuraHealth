package com.pro.aifitnesstracker.ui.screens

import com.pro.aifitnesstracker.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@Composable
fun LanguageSelectionScreen(onLanguageSelected: (String) -> Unit) {
    val s = LocalAppStrings.current
    val languages = listOf(
        Pair("en", "🇺🇸 English"),
        Pair("hi", "🇮🇳 Hindi (हिंदी)"),
        Pair("mr", "🇮🇳 Marathi (मराठी)"),
        Pair("gu", "🇮🇳 Gujarati (ગુજરાતી)"),
        Pair("ta", "🇮🇳 Tamil (தமிழ்)"),
        Pair("te", "🇮🇳 Telugu (తెలుగు)"),
        Pair("kn", "🇮🇳 Kannada (ಕನ್ನಡ)"),
        Pair("bn", "🇮🇳 Bengali (বাংলা)"),
        Pair("pa", "🇮🇳 Punjabi (ਪੰਜਾਬੀ)"),
        Pair("es", "🇪🇸 Spanish (Español)"),
        Pair("fr", "🇫🇷 French (Français)"),
        Pair("de", "🇩🇪 German (Deutsch)"),
        Pair("it", "🇮🇹 Italian (Italiano)"),
        Pair("pt", "🇵🇹 Portuguese (Português)"),
        Pair("zh", "🇨🇳 Chinese (中文)"),
        Pair("ja", "🇯🇵 Japanese (日本語)"),
        Pair("ar", "🇸🇦 Arabic (العربية)"),
        Pair("ru", "🇷🇺 Russian (Русский)")
    )
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val topBarBg = if (isDark) Color(0xFF080B14) else Color(0xFFEEF2F6)
    val canvasBgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF080B14), Color(0xFF0D121F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEEF2F6), Color(0xFFE2E8F0)))
    }
    val cardColor = if (isDark) Color(0xFF131A2A) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF1F293D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color.Black

    Scaffold(
        containerColor = topBarBg,
        topBar = {
            Surface(
                color = topBarBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = s.chooseLanguage,
                        color = textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select your preferred language",
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(canvasBgGradient)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            languages.forEach { (code, name) ->
                Surface(
                    onClick = { onLanguageSelected(code) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
                    color = cardColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            color = textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

