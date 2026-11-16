package com.pro.aifitnesstracker

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"

    fun initAndFetch(prefs: SharedPrefsManager) {
        try {
            val remoteConfig = Firebase.remoteConfig
            
            // Set config settings. Minimum fetch interval is set to 0 for debugging/live updates.
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0
            }
            remoteConfig.setConfigSettingsAsync(configSettings)

            // Set default parameter values
            val defaults = mapOf(
                "premium_themes_list" to "SleepOptimizer,CyberpunkNeon,GoldElite,MidnightDark",
                "default_premium_theme" to "CyberpunkNeon",
                "premium_font_family" to "Default",
                "enable_premium_animations" to true,
                "premium_branding_title" to "AuraHealth Pro"
            )
            remoteConfig.setDefaultsAsync(defaults)

            // Fetch and activate
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val themes = remoteConfig.getString("premium_themes_list")
                        val defaultTheme = remoteConfig.getString("default_premium_theme")
                        val font = remoteConfig.getString("premium_font_family")
                        val anims = remoteConfig.getBoolean("enable_premium_animations")
                        val branding = remoteConfig.getString("premium_branding_title")
                        
                        prefs.savePremiumThemesList(themes)
                        prefs.saveRemoteDefaultPremiumTheme(defaultTheme)
                        prefs.savePremiumFontFamily(font)
                        prefs.savePremiumAnimationsEnabled(anims)
                        prefs.savePremiumBrandingTitle(branding)
                        
                        Log.d(TAG, "Fetched Remote Config: themes=$themes, default=$defaultTheme, font=$font, anims=$anims, branding=$branding")
                    } else {
                        Log.e(TAG, "Fetch failed: ${task.exception?.message}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "RemoteConfig initialization error: ${e.message}")
        }
    }
}
