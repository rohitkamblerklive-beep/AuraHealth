package com.pro.aifitnesstracker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class WorkoutData(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val durationMins: Int,
    val kcal: Int,
    val category: String,
    val instructions: List<String>,
    val color: Color
)
