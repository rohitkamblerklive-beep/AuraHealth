package com.pro.aifitnesstracker.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {
    private const val TAG = "BatteryOptHelper"
    private const val PREF_PROMPT_SHOWN = "pref_battery_opt_prompt_dismissed"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else {
            true
        }
    }

    fun isXiaomiOrMiui(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
    }

    fun shouldShowOptimizationPrompt(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_battery_prefs", Context.MODE_PRIVATE)
        val dismissed = prefs.getBoolean(PREF_PROMPT_SHOWN, false)
        if (dismissed) return false

        // Prompt if battery optimization is active (not whitelisted)
        return !isIgnoringBatteryOptimizations(context) || isXiaomiOrMiui()
    }

    fun dismissPrompt(context: Context) {
        val prefs = context.getSharedPreferences("app_battery_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_PROMPT_SHOWN, true).apply()
    }

    fun openOptimizationSettings(context: Context) {
        try {
            // First try direct battery optimization intent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations(context)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Standard ignore battery intent failed, trying OEM intents: ${e.message}")
        }

        // Try Xiaomi / MIUI Autostart screen
        if (isXiaomiOrMiui()) {
            val xiaomiIntents = listOf(
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")),
                Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").putExtra("package_name", context.packageName)
            )
            for (intent in xiaomiIntents) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {}
            }
        }

        // Fallback to app details settings
        try {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (e: Exception) {
            Log.e(TAG, "All optimization intents failed", e)
        }
    }
}
