package com.pro.aifitnesstracker

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import java.util.Date

/**
 * AppOpenAdManager manages loading and showing App Open Ads on background-to-foreground transitions.
 * Integrates hybrid time-based cooldown and foreground resume thresholds to prevent user irritation.
 * Uses ActivityLifecycleCallbacks directly without needing external ProcessLifecycleOwner dependency.
 */
class AppOpenAdManager(private val app: Application) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null
    private var loadTime: Long = 0

    // Capping Metrics
    private var startedActivityCount = 0
    private var resumeCount = 0
    private var lastAppOpenShownTime = 0L

    companion object {
        private const val TAG = "AppOpenAdManager"
        private const val FOUR_HOURS_MS = 3600000L * 4
        private const val DEFAULT_RESUME_THRESHOLD = 2 // Require at least 2 app resumes before showing
        private const val DEFAULT_COOLDOWN_MS = 14400000L // 4 hours default cooldown
    }

    init {
        app.registerActivityLifecycleCallbacks(this)
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && (Date().time - loadTime) < FOUR_HOURS_MS
    }

    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return
        if (!AdConfigManager.shouldShowAds()) return

        val adUnitId = AdConfigManager.getAdUnitId(AdConfigManager.KEY_AD_APP_OPEN)
        if (adUnitId.isNullOrEmpty()) {
            Log.d(TAG, "App open ad unit ID not configured, skipping load")
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            app,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open Ad loaded successfully")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                    isLoadingAd = false
                }
            }
        )
    }

    private fun canShowAppOpenAd(): Boolean {
        if (!AdConfigManager.shouldShowAds()) return false
        val now = System.currentTimeMillis()

        // 1. Time Cooldown Gate
        val cooldownMs = try {
            val configSeconds = FirebaseRemoteConfig.getInstance().getLong("ad_app_open_cooldown_seconds")
            if (configSeconds > 0) configSeconds * 1000L else DEFAULT_COOLDOWN_MS
        } catch (e: Exception) {
            DEFAULT_COOLDOWN_MS
        }

        if (lastAppOpenShownTime > 0 && (now - lastAppOpenShownTime) < cooldownMs) {
            val remainMin = (cooldownMs - (now - lastAppOpenShownTime)) / 60000
            Log.d(TAG, "App Open Ad skipped: cooldown active ($remainMin minutes left)")
            return false
        }

        // 2. Resume / Click threshold
        val threshold = try {
            val configThreshold = FirebaseRemoteConfig.getInstance().getLong("ad_app_open_resume_threshold")
            if (configThreshold > 0) configThreshold.toInt() else DEFAULT_RESUME_THRESHOLD
        } catch (e: Exception) {
            DEFAULT_RESUME_THRESHOLD
        }

        if (resumeCount < threshold) {
            Log.d(TAG, "App Open Ad skipped: resume threshold not met ($resumeCount < $threshold)")
            return false
        }

        return true
    }

    private fun showAdIfAvailable(activity: Activity) {
        if (isShowingAd) return

        if (!canShowAppOpenAd()) {
            if (!isAdAvailable()) loadAd()
            return
        }

        if (!isAdAvailable()) {
            loadAd()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open Ad dismissed")
                appOpenAd = null
                isShowingAd = false
                lastAppOpenShownTime = System.currentTimeMillis()
                resumeCount = 0
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "App Open Ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open Ad displayed")
                isShowingAd = true
            }
        }

        appOpenAd?.show(activity)
    }

    // Activity Lifecycle Callbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (startedActivityCount == 0) {
            // App transitioning from background to foreground
            resumeCount++
            Log.d(TAG, "App foregrounded, resumeCount: $resumeCount")
            showAdIfAvailable(activity)
        }
        startedActivityCount++
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
