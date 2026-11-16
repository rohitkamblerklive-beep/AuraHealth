package com.pro.aifitnesstracker

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object AdConfigManager {
    private const val TAG = "AdConfigManager"
    private var prefsManager: SharedPrefsManager? = null
    private var isDebug = false

    // Default values
    private const val DEFAULT_ADS_ENABLED = true
    private const val DEFAULT_ADS_PREMIUM_FREE = true

    // Hybrid Capping Constants & Defaults
    const val DEFAULT_CLICK_THRESHOLD = 4L
    const val DEFAULT_COOLDOWN_SECONDS = 240L // 4 minutes
    const val DEFAULT_MAX_ADS_PER_SESSION = 3L
    const val DEFAULT_GRACE_PERIOD_SECONDS = 60L // 1 minute

    // AdMob Official Production App ID: ca-app-pub-5924279266454689~XXXXXXXXXX

    // Remote Config Keys for Banners
    const val KEY_AD_ACHIEVEMENT_BANNER = "ad_achievements_banner"
    const val KEY_AD_ACTIVITY_HISTORY_BANNER = "ad_activity_history_banner"
    const val KEY_AD_AI_COACH_BANNER = "ad_ai_coach_banner"
    const val KEY_AD_AI_MEAL_BANNER = "ad_ai_meal_banner"
    const val KEY_AD_AI_WORKOUT_BANNER = "ad_ai_workout_banner"
    const val KEY_AD_MEAL_PLANNER_BANNER = "ad_meal_planner_banner"
    const val KEY_AD_WORKOUT_GENERATOR_BANNER = "ad_workout_generator_banner"
    const val KEY_AD_ALGORITHM_LAB_BANNER = "ad_algorithm_lab_banner"
    const val KEY_AD_ANALYTICS_BANNER = "ad_analytics_banner"
    const val KEY_AD_BARCODE_SCANNER_BANNER = "ad_barcode_scanner_banner"
    const val KEY_AD_BLOOD_PRESSURE_BANNER = "ad_blood_pressure_banner"
    const val KEY_AD_BODY_VISUALIZER_BANNER = "ad_body_visualizer_banner"
    const val KEY_AD_BREATHING_BANNER = "ad_breathing_banner"
    const val KEY_AD_DASHBOARD_BANNER = "ad_dashboard_banner"
    const val KEY_AD_DIET_BANNER = "ad_diet_banner"
    const val KEY_AD_FASTING_BANNER = "ad_fasting_banner"
    const val KEY_AD_HEART_RATE_BANNER = "ad_heart_rate_banner"
    const val KEY_AD_INSTRUCTIONS_BANNER = "ad_instructions_banner"
    const val KEY_AD_NUTRITION_BANNER = "ad_nutrition_banner"
    const val KEY_AD_PERFORMANCE_BANNER = "ad_performance_banner"
    const val KEY_AD_PROFILE_BANNER = "ad_profile_banner"
    const val KEY_AD_RUN_TRACKER_BANNER = "ad_run_tracker_banner"
    const val KEY_AD_SETTINGS_BANNER = "ad_settings_banner"
    const val KEY_AD_SLEEP_BANNER = "ad_sleep_banner"
    const val KEY_AD_STEPS_BANNER = "ad_steps_banner"
    const val KEY_AD_WATER_BANNER = "ad_water_banner"
    const val KEY_AD_WEIGHT_BANNER = "ad_weight_banner"
    const val KEY_AD_WORKOUT_BANNER = "ad_workout_banner"
    const val KEY_AD_YOGA_BANNER = "ad_yoga_banner"

    // Remote Config Keys for Native Ads
    const val KEY_AD_ACHIEVEMENTS_NATIVE = "ad_achievements_native"
    const val KEY_AD_AI_FOOD_SCANNER_NATIVE = "ad_ai_food_scanner_native"
    const val KEY_AD_AI_MEAL_PLANNER_NATIVE = "ad_ai_meal_planner_native"
    const val KEY_AD_DIET_NATIVE = "ad_diet_native"
    const val KEY_AD_NUTRITION_NATIVE = "ad_nutrition_native"

    // Remote Config Keys for App Open Ads
    const val KEY_AD_APP_OPEN = "ad_app_open"

    // Remote Config Keys for Interstitials
    const val KEY_AD_CHALLENGE_INTERSTITIAL = "ad_challenge_interstitial"
    const val KEY_AD_DASHBOARD_INTERSTITIAL = "ad_dashboard_interstitial"
    const val KEY_AD_WORKOUT_INTERSTITIAL = "ad_workout_interstitial"

    fun initialize(context: Context, prefs: SharedPrefsManager) {
        this.prefsManager = prefs
        this.isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // Initialize Mobile Ads SDK
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "MobileAds initialized: ${status.adapterStatusMap}")
        }

        // Initialize Firebase Remote Config
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (this.isDebug) 0 else 3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set local default values
        val defaults = mutableMapOf<String, Any>()
        defaults["ads_enabled"] = DEFAULT_ADS_ENABLED
        defaults["ads_premium_free"] = DEFAULT_ADS_PREMIUM_FREE
        defaults["ad_click_threshold"] = DEFAULT_CLICK_THRESHOLD
        defaults["ad_cooldown_seconds"] = DEFAULT_COOLDOWN_SECONDS
        defaults["ad_max_per_session"] = DEFAULT_MAX_ADS_PER_SESSION
        defaults["ad_grace_period_seconds"] = DEFAULT_GRACE_PERIOD_SECONDS

        // AdMob Unit IDs default to empty string.
        // All IDs are controlled purely via Firebase Remote Config or our upcoming clean Ad Strategy Plan.
        // Banners
        defaults[KEY_AD_ACHIEVEMENT_BANNER] = ""
        defaults[KEY_AD_ACTIVITY_HISTORY_BANNER] = ""
        defaults[KEY_AD_AI_COACH_BANNER] = ""
        defaults[KEY_AD_AI_MEAL_BANNER] = ""
        defaults[KEY_AD_AI_WORKOUT_BANNER] = ""
        defaults[KEY_AD_MEAL_PLANNER_BANNER] = ""
        defaults[KEY_AD_WORKOUT_GENERATOR_BANNER] = ""
        defaults[KEY_AD_ALGORITHM_LAB_BANNER] = ""
        defaults[KEY_AD_ANALYTICS_BANNER] = ""
        defaults[KEY_AD_BARCODE_SCANNER_BANNER] = ""
        defaults[KEY_AD_BLOOD_PRESSURE_BANNER] = ""
        defaults[KEY_AD_BODY_VISUALIZER_BANNER] = ""
        defaults[KEY_AD_BREATHING_BANNER] = ""
        defaults[KEY_AD_DASHBOARD_BANNER] = ""
        defaults[KEY_AD_DIET_BANNER] = ""
        defaults[KEY_AD_FASTING_BANNER] = ""
        defaults[KEY_AD_HEART_RATE_BANNER] = ""
        defaults[KEY_AD_INSTRUCTIONS_BANNER] = ""
        defaults[KEY_AD_NUTRITION_BANNER] = ""
        defaults[KEY_AD_PERFORMANCE_BANNER] = ""
        defaults[KEY_AD_PROFILE_BANNER] = ""
        defaults[KEY_AD_RUN_TRACKER_BANNER] = ""
        defaults[KEY_AD_SETTINGS_BANNER] = ""
        defaults[KEY_AD_SLEEP_BANNER] = ""
        defaults[KEY_AD_STEPS_BANNER] = ""
        defaults[KEY_AD_WATER_BANNER] = ""
        defaults[KEY_AD_WEIGHT_BANNER] = ""
        defaults[KEY_AD_WORKOUT_BANNER] = ""
        defaults[KEY_AD_YOGA_BANNER] = ""

        // Native Ads
        defaults[KEY_AD_ACHIEVEMENTS_NATIVE] = ""
        defaults[KEY_AD_AI_FOOD_SCANNER_NATIVE] = ""
        defaults[KEY_AD_AI_MEAL_PLANNER_NATIVE] = ""
        defaults[KEY_AD_DIET_NATIVE] = ""
        defaults[KEY_AD_NUTRITION_NATIVE] = ""

        // App Open Ads
        defaults[KEY_AD_APP_OPEN] = ""

        // Interstitials
        defaults[KEY_AD_CHALLENGE_INTERSTITIAL] = ""
        defaults[KEY_AD_DASHBOARD_INTERSTITIAL] = ""
        defaults[KEY_AD_WORKOUT_INTERSTITIAL] = ""

        remoteConfig.setDefaultsAsync(defaults)
        
        // Fetch and activate config
        fetchAndActivate()
    }

    fun fetchAndActivate() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Config fetch and activate SUCCESS")
                } else {
                    Log.w(TAG, "Config fetch FAILED")
                }
            }
    }

    fun shouldShowAds(): Boolean {
        val prefs = prefsManager ?: return false
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        
        val adsEnabled = remoteConfig.getBoolean("ads_enabled")
        val adsPremiumFree = remoteConfig.getBoolean("ads_premium_free")
        val isPremium = prefs.isPremiumUser()

        Log.d(TAG, "shouldShowAds: adsEnabled=$adsEnabled, adsPremiumFree=$adsPremiumFree, isPremium=$isPremium")

        if (!adsEnabled) return false
        if (adsPremiumFree && isPremium) return false
        return true
    }

    fun getAdUnitId(key: String): String? {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val adId = remoteConfig.getString(key)
        return if (adId.isNullOrEmpty()) null else adId
    }

    fun getInterstitialClickThreshold(): Long {
        return try {
            val v = FirebaseRemoteConfig.getInstance().getLong("ad_click_threshold")
            if (v > 0) v else DEFAULT_CLICK_THRESHOLD
        } catch (e: Exception) {
            DEFAULT_CLICK_THRESHOLD
        }
    }

    fun getInterstitialCooldownMs(): Long {
        val seconds = try {
            val v = FirebaseRemoteConfig.getInstance().getLong("ad_cooldown_seconds")
            if (v > 0) v else DEFAULT_COOLDOWN_SECONDS
        } catch (e: Exception) {
            DEFAULT_COOLDOWN_SECONDS
        }
        return seconds * 1000L
    }

    fun getMaxInterstitialPerSession(): Long {
        return try {
            val v = FirebaseRemoteConfig.getInstance().getLong("ad_max_per_session")
            if (v > 0) v else DEFAULT_MAX_ADS_PER_SESSION
        } catch (e: Exception) {
            DEFAULT_MAX_ADS_PER_SESSION
        }
    }

    fun getInterstitialGracePeriodMs(): Long {
        val seconds = try {
            val v = FirebaseRemoteConfig.getInstance().getLong("ad_grace_period_seconds")
            if (v > 0) v else DEFAULT_GRACE_PERIOD_SECONDS
        } catch (e: Exception) {
            DEFAULT_GRACE_PERIOD_SECONDS
        }
        return seconds * 1000L
    }
}
