package com.pro.aifitnesstracker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val isTest = inputData.getBoolean("is_test", false)
        val prefs = SharedPrefsManager(applicationContext)
        if (!isTest && !prefs.isDailyReminderEnabled()) {
            return Result.success()
        }

        val helper = NotificationHelper(applicationContext)
        helper.createNotificationChannel()
        
        val steps = prefs.getStepsToday()
        val stepGoal = prefs.getStepGoal()

        // If the daily step goal is already met/completed, do NOT send the daily reminder (unless test)!
        if (!isTest && steps >= stepGoal) {
            return Result.success()
        }

        val water = prefs.getWaterGlassesToday()
        val waterGoal = prefs.getWaterGoal()
        val sleep = prefs.getSleepHoursToday()
        val sleepGoal = prefs.getSleepGoal()

        val stepProgress = steps.toFloat() / stepGoal.coerceAtLeast(1)
        val waterProgress = water.toFloat() / waterGoal.coerceAtLeast(1)
        val sleepProgress = if (sleepGoal > 0f) sleep / sleepGoal else 1.0f

        val isPremium = prefs.isPremiumUser()
        val strings = getLanguageStrings(prefs.getLanguage())
        val name = prefs.getUserName().ifBlank { "Champion" }

        // Determine if any goal is set but has low progress (< 40%)
        val (title, message) = when {
            // 1. Water Nudge (Progress under 40% and lowest progress)
            waterProgress < 0.4f && (waterProgress <= stepProgress && waterProgress <= sleepProgress) -> {
                Pair(
                    strings.waterNudgeTitle,
                    strings.waterNudgeMsg.format(name, waterGoal, water)
                )
            }
            // 2. Steps Nudge (Progress under 40% and lowest progress)
            stepProgress < 0.4f && (stepProgress <= waterProgress && stepProgress <= sleepProgress) -> {
                Pair(
                    strings.stepsNudgeTitle,
                    strings.stepsNudgeMsg.format(name, stepGoal, steps)
                )
            }
            // 3. Sleep Nudge (Progress under 40% and lowest progress)
            sleepProgress < 0.4f && (sleepProgress <= waterProgress && sleepProgress <= stepProgress) -> {
                Pair(
                    strings.sleepNudgeTitle,
                    strings.sleepNudgeMsg.format(name, sleepGoal, sleep)
                )
            }
            // 4. Premium templates fallback (if progress is > 40% but incomplete, or completed)
            isPremium -> {
                val mockTemp = 30
                when {
                    waterProgress < 1.0f && (waterProgress <= stepProgress && waterProgress <= sleepProgress) -> {
                        Pair(
                            strings.premiumWaterTitle,
                            strings.premiumWaterMsg.format(name, mockTemp, waterGoal - water, waterGoal)
                        )
                    }
                    stepProgress < 1.0f && (stepProgress <= waterProgress && stepProgress <= sleepProgress) -> {
                        Pair(
                            strings.premiumStepsTitle,
                            strings.premiumStepsMsg.format(name, stepGoal - steps, stepGoal)
                        )
                    }
                    else -> {
                        val streak = prefs.getCurrentStreak()
                        Pair(
                            strings.premiumGoalMetTitle,
                            strings.premiumGoalMetMsg.format(name, streak)
                        )
                    }
                }
            }
            // 5. Free templates fallback (if progress is > 40% but incomplete, or completed)
            else -> {
                when {
                    stepProgress < 1.0f && (stepProgress <= waterProgress && stepProgress <= sleepProgress) -> {
                        Pair(
                            strings.freeStepsTitle,
                            strings.freeStepsMsg.format(steps, stepGoal - steps, stepGoal)
                        )
                    }
                    waterProgress < 1.0f && (waterProgress <= stepProgress && waterProgress <= sleepProgress) -> {
                        Pair(
                            strings.freeWaterTitle,
                            strings.freeWaterMsg.format(water, waterGoal - water, waterGoal)
                        )
                    }
                    sleepProgress < 1.0f -> {
                        Pair(
                            strings.freeSleepTitle,
                            strings.freeSleepMsg.format(sleep)
                        )
                    }
                    else -> {
                        val streak = prefs.getCurrentStreak()
                        Pair(
                            strings.freeGoalMetTitle,
                            strings.freeGoalMetMsg.format(streak)
                        )
                    }
                }
            }
        }

        val langCode = prefs.getLanguage()
        val todayQuote = com.pro.aifitnesstracker.utils.MotivationalQuotes.getQuoteForToday()
        val quoteText = when (langCode) {
            "hi" -> todayQuote.hindi
            "mr" -> todayQuote.marathi
            else -> todayQuote.english
        }
        val finalMessage = "$message\n\n💡 ${strings.todayInspirationPrefix}: “$quoteText”"

        helper.showNotification(title, finalMessage)
        
        return Result.success()
    }
}
