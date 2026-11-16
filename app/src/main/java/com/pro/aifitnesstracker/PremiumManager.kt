package com.pro.aifitnesstracker

import android.content.Context

object PremiumManager {

    const val PLAN_MONTHLY = "premium_monthly"
    const val PLAN_YEARLY = "premium_yearly"
    const val PLAN_LIFETIME = "premium_lifetime"

    // Premium features list
    val PREMIUM_FEATURES = listOf(
        "Unlimited AI Coach messages",
        "Unlimited AI Meal Plans",
        "Unlimited AI Workout Generation",
        "Body Visualizer Access",
        "Advanced Analytics & Reports",
        "Priority Cloud Backup",
        "Ad-Free Experience",
        "Unlimited Home Widgets",
        "Custom Goals & Targets",
        "Export Data (CSV/PDF)"
    )

    val PLAN_PRICES = mapOf(
        PLAN_MONTHLY to "₹199/month",
        PLAN_YEARLY to "₹999/year",
        PLAN_LIFETIME to "₹1,999 one-time"
    )

    val PLAN_SAVINGS = mapOf(
        PLAN_MONTHLY to "",
        PLAN_YEARLY to "Save 58%",
        PLAN_LIFETIME to "Best Value"
    )

    fun isPremium(prefs: SharedPrefsManager): Boolean = prefs.isPremiumUser()

    fun activate(prefs: SharedPrefsManager, plan: String) {
        val expiry = when (plan) {
            PLAN_MONTHLY -> System.currentTimeMillis() + 30L * 24 * 3600 * 1000
            PLAN_YEARLY -> System.currentTimeMillis() + 365L * 24 * 3600 * 1000
            PLAN_LIFETIME -> -1L // never expires
            else -> 0L
        }
        prefs.setPremiumUser(true, expiry)
        prefs.setPremiumPlan(plan)
    }

    fun deactivate(prefs: SharedPrefsManager) {
        prefs.setPremiumUser(false, 0L)
        prefs.setPremiumPlan("")
    }

    // Check if AI Coach message limit reached (free: 10/day)
    fun canUseAiCoach(prefs: SharedPrefsManager): Boolean {
        if (isPremium(prefs)) return true
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        if (prefs.getLastAiCoachDate() != today) {
            prefs.resetAiCoachDailyCount()
            prefs.setLastAiCoachDate(today)
        }
        return prefs.getAiCoachMessageCount() < 10
    }

    fun onAiCoachMessageSent(prefs: SharedPrefsManager) {
        if (!isPremium(prefs)) prefs.incrementAiCoachMessageCount()
    }
}
