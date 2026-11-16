package com.pro.aifitnesstracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.pro.aifitnesstracker.data.water.WaterIntakeEntry
import com.pro.aifitnesstracker.data.water.CustomWaterContainer
import com.pro.aifitnesstracker.data.water.WaterTrackerDao
import com.pro.aifitnesstracker.data.step.DailyStepEntity
import com.pro.aifitnesstracker.data.step.WalkingSessionEntity
import com.pro.aifitnesstracker.data.step.StepAchievementEntity
import com.pro.aifitnesstracker.data.step.StepTrackerDao

import com.pro.aifitnesstracker.data.yoga.YogaSessionRecord
import com.pro.aifitnesstracker.data.yoga.CustomYogaRoutineEntity
import com.pro.aifitnesstracker.data.yoga.YogaFavoriteEntity
import com.pro.aifitnesstracker.data.yoga.YogaPlanProgressEntity
import com.pro.aifitnesstracker.data.yoga.YogaAchievementEntity
import com.pro.aifitnesstracker.data.yoga.YogaDao

import com.pro.aifitnesstracker.data.sleep.SleepSessionEntity
import com.pro.aifitnesstracker.data.sleep.SleepScheduleEntity
import com.pro.aifitnesstracker.data.sleep.SleepAchievementEntity
import com.pro.aifitnesstracker.data.sleep.SleepTrackerDao

@Database(
    entities = [
        HeartRateRecord::class,
        BloodPressureRecord::class,
        StepRecord::class,
        WaterRecord::class,
        MealRecord::class,
        SleepRecord::class,
        YogaRecord::class,
        WorkoutRecord::class,
        WorkoutPlanRecord::class,
        AIChatRecord::class,
        WaterIntakeEntry::class,
        CustomWaterContainer::class,
        DailyStepEntity::class,
        WalkingSessionEntity::class,
        StepAchievementEntity::class,
        YogaSessionRecord::class,
        CustomYogaRoutineEntity::class,
        YogaFavoriteEntity::class,
        YogaPlanProgressEntity::class,
        YogaAchievementEntity::class,
        SleepSessionEntity::class,
        SleepScheduleEntity::class,
        SleepAchievementEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao
    abstract fun waterTrackerDao(): WaterTrackerDao
    abstract fun stepTrackerDao(): StepTrackerDao
    abstract fun yogaDao(): YogaDao
    abstract fun sleepTrackerDao(): SleepTrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_fitness_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
