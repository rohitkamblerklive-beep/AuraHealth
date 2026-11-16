package com.pro.aifitnesstracker.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.pro.aifitnesstracker.AdConfigManager

private const val TAG = "AdComponents"

@Composable
fun AdBanner(
    adKey: String,
    modifier: Modifier = Modifier,
    applyNavigationBarsPadding: Boolean = true
) {
    if (!AdConfigManager.shouldShowAds()) {
        return
    }

    val context = LocalContext.current
    val adUnitId = AdConfigManager.getAdUnitId(adKey) ?: return

    Box(
        modifier = modifier
            .then(if (applyNavigationBarsPadding) Modifier.navigationBarsPadding() else Modifier)
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    setAdUnitId(adUnitId)
                    adListener = object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e(TAG, "Banner load failed: ${error.message} (Key: $adKey, ID: $adUnitId)")
                        }
                        override fun onAdLoaded() {
                            Log.d(TAG, "Banner loaded successfully (Key: $adKey)")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { adView ->
                // Optionally update state if needed
            }
        )
    }
}

object AdInterstitialHelper {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    // Session & Hybrid Tracking
    private val sessionStartTime = System.currentTimeMillis()
    private var lastAdShownTimestamp = 0L
    private var actionClickCount = 0
    private var sessionAdsShownCount = 0

    fun recordActionClick(context: Context, prefetchKey: String = "ad_dashboard_interstitial") {
        actionClickCount++
        Log.d(TAG, "Action click recorded: currentCount=$actionClickCount")
        // Preload ad when user is halfway to threshold
        if (interstitialAd == null && !isLoading && actionClickCount >= 2) {
            loadInterstitial(context, prefetchKey)
        }
    }

    fun canShowInterstitial(): Boolean {
        if (!AdConfigManager.shouldShowAds()) {
            Log.d(TAG, "canShowInterstitial: Ads are disabled or user is premium")
            return false
        }

        val maxSessionAds = AdConfigManager.getMaxInterstitialPerSession()
        if (sessionAdsShownCount >= maxSessionAds) {
            Log.d(TAG, "canShowInterstitial: Session limit reached ($sessionAdsShownCount >= $maxSessionAds)")
            return false
        }

        val currentTime = System.currentTimeMillis()
        val gracePeriodMs = AdConfigManager.getInterstitialGracePeriodMs()
        if ((currentTime - sessionStartTime) < gracePeriodMs) {
            val remain = (gracePeriodMs - (currentTime - sessionStartTime)) / 1000
            Log.d(TAG, "canShowInterstitial: In grace period ($remain seconds remaining)")
            return false
        }

        val cooldownMs = AdConfigManager.getInterstitialCooldownMs()
        if (lastAdShownTimestamp > 0 && (currentTime - lastAdShownTimestamp) < cooldownMs) {
            val remain = (cooldownMs - (currentTime - lastAdShownTimestamp)) / 1000
            Log.d(TAG, "canShowInterstitial: In cooldown ($remain seconds remaining)")
            return false
        }

        val clickThreshold = AdConfigManager.getInterstitialClickThreshold()
        if (actionClickCount < clickThreshold) {
            Log.d(TAG, "canShowInterstitial: Click threshold not met ($actionClickCount < $clickThreshold)")
            return false
        }

        return true
    }

    fun loadInterstitial(context: Context, adKey: String) {
        if (!AdConfigManager.shouldShowAds()) return
        if (interstitialAd != null || isLoading) return

        val adUnitId = AdConfigManager.getAdUnitId(adKey) ?: return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial load failed: ${adError.message} (Key: $adKey, ID: $adUnitId)")
                    interstitialAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded successfully (Key: $adKey)")
                    interstitialAd = ad
                    isLoading = false
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismiss: () -> Unit) {
        // Automatically record this action click
        actionClickCount++
        Log.d(TAG, "showInterstitial triggered, actionClickCount=$actionClickCount")

        if (!canShowInterstitial()) {
            Log.d(TAG, "Skipping interstitial ad due to click/cooldown gating")
            // Try background preload for the next time threshold is met
            if (interstitialAd == null && !isLoading) {
                loadInterstitial(activity, "ad_dashboard_interstitial")
            }
            onDismiss()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Conditions met but ad not ready; loading now")
            loadInterstitial(activity, "ad_dashboard_interstitial")
            onDismiss()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                onAdFinished()
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                interstitialAd = null
                onDismiss()
            }
        }
        ad.show(activity)
    }

    private fun onAdFinished() {
        interstitialAd = null
        lastAdShownTimestamp = System.currentTimeMillis()
        actionClickCount = 0 // Reset counter for the next cycle
        sessionAdsShownCount++
        Log.d(TAG, "Ad finished: resets click counter, sessionAdsShown=$sessionAdsShownCount")
    }
}

@Composable
fun AdNative(
    adKey: String,
    modifier: Modifier = Modifier
) {
    if (!AdConfigManager.shouldShowAds()) {
        return
    }

    val context = LocalContext.current
    val adUnitId = AdConfigManager.getAdUnitId(adKey) ?: return
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad : NativeAd ->
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: ${error.message} (Key: $adKey, ID: $adUnitId)")
                    adLoadFailed = true
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    val currentNativeAd = nativeAd

    if (adLoadFailed) {
        // Fallback to banner ad if native fails
        AdBanner(adKey = adKey, modifier = modifier)
    } else if (currentNativeAd != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                factory = { ctx ->
                    val nativeAdView = NativeAdView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val container = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // Header Row with Ad badge and Headline
                    val headerRow = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val adBadge = TextView(ctx).apply {
                        text = "AD"
                        textSize = 10f
                        setTextColor(android.graphics.Color.WHITE)
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setPadding(12, 4, 12, 4)
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 8f
                            setColor(android.graphics.Color.parseColor("#4F46E5"))
                        }
                    }
                    headerRow.addView(adBadge)

                    val headlineView = TextView(ctx).apply {
                        text = currentNativeAd.headline ?: "Sponsored"
                        textSize = 15f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.DKGRAY)
                        setPadding(16, 0, 0, 0)
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    headerRow.addView(headlineView)
                    nativeAdView.headlineView = headlineView
                    container.addView(headerRow)

                    // Body
                    val bodyText = currentNativeAd.body
                    if (!bodyText.isNullOrEmpty()) {
                        val bodyView = TextView(ctx).apply {
                            text = bodyText
                            textSize = 13f
                            setTextColor(android.graphics.Color.GRAY)
                            setPadding(0, 12, 0, 12)
                            maxLines = 2
                            ellipsize = android.text.TextUtils.TruncateAt.END
                        }
                        nativeAdView.bodyView = bodyView
                        container.addView(bodyView)
                    }

                    // Call To Action Button
                    val ctaButton = Button(ctx).apply {
                        text = currentNativeAd.callToAction ?: "Learn More"
                        textSize = 13f
                        setTextColor(android.graphics.Color.WHITE)
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 16f
                            setColor(android.graphics.Color.parseColor("#4F46E5"))
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = 8
                        }
                    }
                    nativeAdView.callToActionView = ctaButton
                    container.addView(ctaButton)

                    nativeAdView.addView(container)
                    nativeAdView.setNativeAd(currentNativeAd)
                    nativeAdView
                },
                update = { view ->
                    view.setNativeAd(currentNativeAd)
                }
            )
        }
    } else {
        // loading state placeholder
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sponsored Content loading...",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
