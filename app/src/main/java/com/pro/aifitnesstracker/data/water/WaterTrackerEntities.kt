package com.pro.aifitnesstracker.data.water

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Individual drink / water intake entry.
 * Canonical storage unit is milliliters (ml).
 */
@Entity(tableName = "water_intake_entries")
data class WaterIntakeEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMl: Int,
    val drinkType: String = "WATER", // WATER, MILK, COCONUT_WATER, JUICE, TEA, COFFEE, OTHER
    val date: String, // Format: yyyy-MM-dd
    val time: String, // Format: hh:mm a
    val timestamp: Long = System.currentTimeMillis(),
    val containerName: String = "Glass",
    val note: String = "",
    val hydrationFactor: Float = 1.0f // Multiplier for effective hydration
)

/**
 * Custom drinking container saved by the user.
 */
@Entity(tableName = "custom_water_containers")
data class CustomWaterContainer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amountMl: Int,
    val icon: String = "🥛",
    val isDefault: Boolean = false
)

/**
 * Supported Drink Types with their metadata and hydration factors.
 */
enum class DrinkType(
    val code: String,
    val displayName: String,
    val emoji: String,
    val hydrationFactor: Float,
    val colorHex: Long
) {
    WATER("WATER", "Plain Water", "💧", 1.0f, 0xFF38BDF8),
    COCONUT_WATER("COCONUT_WATER", "Coconut Water", "🥥", 1.0f, 0xFF10B981),
    MILK("MILK", "Milk", "🥛", 0.9f, 0xFFF8FAFC),
    JUICE("JUICE", "Fruit Juice", "🧃", 0.85f, 0xFFF59E0B),
    TEA("TEA", "Herbal / Green Tea", "🍵", 0.9f, 0xFF84CC16),
    COFFEE("COFFEE", "Coffee", "☕", 0.8f, 0xFFB45309),
    OTHER("OTHER", "Other Beverage", "🥤", 0.85f, 0xFF818CF8);

    companion object {
        fun fromCode(code: String): DrinkType =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: WATER
    }
}
