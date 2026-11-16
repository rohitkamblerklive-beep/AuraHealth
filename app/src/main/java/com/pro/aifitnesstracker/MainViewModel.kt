package com.pro.aifitnesstracker

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModel(
    val repository: HealthRepository,
    val prefsManager: SharedPrefsManager
) : ViewModel() {

    var activeChallengeDay = mutableIntStateOf(-1)
    var activeChallengeTarget = mutableIntStateOf(-1)
    var activeYogaPose = mutableStateOf(YogaAnalyzer.PoseType.NONE)
    var activeWorkout = mutableStateOf<WorkoutData?>(null)
    var appNotification = mutableStateOf<AppNotificationData?>(null)
    
    var stepsTodayState = mutableIntStateOf(0)

    fun updateSteps(steps: Int) {
        stepsTodayState.intValue = steps
    }
}

class MainViewModelFactory(
    private val repository: HealthRepository,
    private val prefsManager: SharedPrefsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, prefsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
